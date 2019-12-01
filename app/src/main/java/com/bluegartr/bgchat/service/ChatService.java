package com.bluegartr.bgchat.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;

import com.bluegartr.bgchat.ChatActivity;
import com.bluegartr.bgchat.R;
import com.bluegartr.bgchat.model.BGChatPreferences;
import com.bluegartr.bgchat.model.ChatUser;
import com.bluegartr.bgchat.model.UserComparator;
import com.bluegartr.bgchat.utils.ChatMessageUtils;
import com.bluegartr.bgchat.utils.ChatUtils;
import com.bluegartr.bgchat.utils.NetworkUtils;
import com.bluegartr.bgchat.utils.SoundEngine;
import com.bluegartr.bgchat.utils.WifiLockUtils;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import sfs2x.client.SmartFox;
import sfs2x.client.core.BaseEvent;
import sfs2x.client.core.IEventListener;
import sfs2x.client.core.SFSEvent;
import sfs2x.client.entities.Room;
import sfs2x.client.entities.User;
import sfs2x.client.requests.ExtensionRequest;
import sfs2x.client.requests.JoinRoomRequest;
import sfs2x.client.requests.LoginRequest;
import sfs2x.client.requests.PublicMessageRequest;

/**
 * Created by Daniel on 8/4/14.
 */
public class ChatService extends Service implements IEventListener {
    public static final String UPDATE_CHATLOG = "com.bluegartr.bgchat.ChatService.updateChatlog";
    public static final String UPDATE_USERS = "com.bluegartr.bgchat.ChatService.updateUsers";
    public static final String CONNECTING = "com.bluegartr.bgchat.ChatService.connecting";
    public static final String LOGIN_SUCCESS = "com.bluegartr.bgchat.ChatService.onLoginSuccess";
    public static final String WRITE_ERROR_MESSAGE = "com.bluegartr.bgchat.ChatService.writeErrorMessage";

    public static final int MAX_CHATLOG_LINES = 100;
    public static final int MAX_RETRY_COUNT = 5;

    private final IBinder mBinder = new LocalBinder();

    private boolean connecting = false;
    private boolean pinging = false;
    private String error;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;

    private SoundEngine soundEngine;

    private UserComparator comparator;
    private SmartFox sfs;
    private Room currentRoom;

    private BGChatPreferences preferences;
    private String server;
    private int port;
    private String username;
    private String password;
    private String zone;
    private String room;

    private int retryCount = 0;

    private ArrayBlockingQueue<String> chatLog = new ArrayBlockingQueue<String>(MAX_CHATLOG_LINES);

    public class LocalBinder extends Binder {
        public ChatService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ChatService.this;
        }
    }

    public boolean isConnecting() {
        return connecting;
    }

    public String getError() {
        return error;
    }

    public void setPreferences(BGChatPreferences preferences) {
        this.preferences = preferences;
    }

    public String getUsername() {
        if (sfs.getMySelf() == null) return null;
        return sfs.getMySelf().getName();
    }

    public String getPassword() {
        return password;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public boolean isMod() {
        int usergroup = sfs.getMySelf().getVariable("usergroup").getIntValue();
        return usergroup == 5 || usergroup == 6 || usergroup == 7;
    }

    public boolean isConnected() {
        return sfs.isConnected();
    }

    public List<User> getUsers() {
        if (currentRoom == null) return null;
        return currentRoom.getUserList();
    }

    public List<ChatUser> getChatUsers() {
        ArrayList<ChatUser> chatUsers = new ArrayList<ChatUser>();
        List<User> users = getUsers();
        if (users != null) {
            for (User user : users) {
                chatUsers.add(new ChatUser(user));
            }
        }
        Collections.sort(chatUsers, comparator);
        return chatUsers;
    }

    public Queue<String> getChatLog() {
        return chatLog;
    }

    public void addToChatLog(String msg) {
        try {
            if (chatLog.remainingCapacity() == 0) {
                chatLog.take();
            }
            chatLog.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startPinging() {
        pinging = true;
        if (preferences.isKeepWifiOn()) {
            WifiLockUtils.lockWifi(this);
        } else {
            WifiLockUtils.unlockWifi();
        }

        scheduledFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (pinging) {
                    Log.v("BG Chat", "Pinging server to keep connection alive...");

                    SFSObject params = new SFSObject();
                    sfs.send(new ExtensionRequest("ping", params));
                }
            }
        }, 20, 20, TimeUnit.SECONDS);
    }

    private void stopPinging() {
        pinging = false;
        WifiLockUtils.unlockWifi();

        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    public void connect(String server, int port, String username, String password, String zone) {
        this.server = server;
        this.port = port;
        this.username = username;
        this.password = password;
        this.zone = zone;

        connecting = true;
        error = null;

        sfs.connect(server, port);
    }

    public void disconnect() {
        sfs.disconnect();

        WifiLockUtils.unlockWifi();
    }

    public void login(String username, String password, String zone) {
        Log.d("BG Chat", "Logging in - username: " + username + ", password: " + password.replaceAll(".", "*") + ", zone: " + zone);
        this.username = username;
        this.password = password;
        this.zone = zone;

        connecting = true;
        error = null;

        SFSObject params = new SFSObject();
        params.putUtfString("pw", password);
        sfs.send(new LoginRequest(username, "", zone, params));
    }

    public void joinRoom(String room) {
        Log.d("BG Chat", "Joining room: " + room);
        this.room = room;
        this.currentRoom = null;

        sfs.send(new JoinRoomRequest(room));
    }

    public void sendMessage(String message) {
        sfs.send(new PublicMessageRequest(message, null, currentRoom));
    }

    public void makeNewTopic(String topic) {
        SFSObject params = new SFSObject();
        params.putUtfString("topicmsg", topic);
        sfs.send(new ExtensionRequest("topic", params));
    }

    public void printHistory() {
        ISFSObject params = new SFSObject();
        sfs.send(new ExtensionRequest("history", params));
    }

    public void random() {
        ISFSObject params = new SFSObject();
        sfs.send(new ExtensionRequest("random", params));
    }

    public void kickUser(String user) {
        ISFSObject params = new SFSObject();
        params.putUtfString("kickname", user);
        sfs.send(new ExtensionRequest("kick", params));
    }

    public void adminMessage(String message, String room) {
        Room adminRoom = sfs.getRoomByName(room);
        if (adminRoom != null) {
            sfs.send(new PublicMessageRequest(message, null, adminRoom));
        }
    }

    public void printTopic(Room room) {
        String topic = room.getVariable("topicmsg").getStringValue();
        String topicCreator = room.getVariable("topicby").getStringValue();
        long timestamp = room.getVariable("topicdate").getIntValue() * 1000l;

        String msg = "Topic By " + ChatUtils.getColoredName(topicCreator) + " - " + topic;
        writeSystemMessage(timestamp, msg, false);
    };

    public void printUsers(List<ChatUser> users) {
        StringBuffer sb = new StringBuffer();

        for (ChatUser user : users) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ChatUtils.getNameSymbol(user) + ChatUtils.getColoredName(user.getName()));
        }

        String msg = users.size() + " Users: " + sb.toString();
        writeSystemMessage(System.currentTimeMillis(), msg, false);
    }

    public void showLoginNotification() {
        connecting = false;
        error = null;

        updateNotification("BG Chat", "Touch to login", null, R.drawable.ic_logo);
    }

    public void showConnectedNotification() {
        updateNotification("BG Chat connected", "Touch to start chatting", "BG Chat connected", R.drawable.ic_connected);
    }

    private void updateUsers(List<User> users) {
        ArrayList<ChatUser> chatUsers = new ArrayList<ChatUser>();
        for (User user : users) {
            chatUsers.add(new ChatUser(user));
        }
        Collections.sort(chatUsers, comparator);

        Intent broadcast = new Intent();
        broadcast.setAction(UPDATE_USERS);
        broadcast.putParcelableArrayListExtra("users", chatUsers);
        sendBroadcast(broadcast);
    }

    private void writeSystemMessage(long timestamp, String msg, boolean isAdmin) {
        addToChatLog(ChatMessageUtils.getSystemMessageHtml(timestamp, msg, isAdmin));

        Intent broadcast = new Intent();
        broadcast.setAction(UPDATE_CHATLOG);
        sendBroadcast(broadcast);
    }

    private void writeMessage(String sender, String msg, boolean isAdmin) {
        boolean mentioned = ChatUtils.containsWordIgnoreCase(msg, username);

        if (preferences.getNotifyWords() != null && !mentioned) {
            for (String word : preferences.getNotifyWords()) {
                if (ChatUtils.containsWordIgnoreCase(msg, word)) {
                    mentioned = true;
                }
            }
        }

        if (mentioned) {
            shoryuken();
            vibrate();

            msg = "<b>" + msg + "</b>";
        }

        if (preferences.isDingOnMessage()) {
            ding();
        }

        addToChatLog(ChatMessageUtils.getChatMessageHtml(System.currentTimeMillis(), sender, msg, isAdmin));

        Intent broadcast = new Intent();
        broadcast.setAction(UPDATE_CHATLOG);
        sendBroadcast(broadcast);
    }

    private void writeMessage(String sender, String msg, boolean isAdmin, long timestamp) {
        addToChatLog(ChatMessageUtils.getChatMessageHtml(timestamp, sender, msg, isAdmin));

        Intent broadcast = new Intent();
        broadcast.setAction(UPDATE_CHATLOG);
        sendBroadcast(broadcast);
    }

    private void onError(String msg) {
        connecting = false;
        error = msg;

        retryCount = 0;
        updateNotification("BG Chat Error", msg, msg, R.drawable.ic_alert);

        Intent broadcast = new Intent();
        broadcast.setAction(WRITE_ERROR_MESSAGE);
        broadcast.putExtra("msg", msg);
        sendBroadcast(broadcast);
    }

    private void onDisconnected() {
        if (preferences.isAutoReconnect() && retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d("BG Chat", "Disconnected.  Reconnecting - Retry count: " + retryCount);
            updateNotification("BG Chat Error", "Disconnected.  Reconnecting...", "BG Chat disconnected.  Reconnecting...", R.drawable.ic_alert);

            Intent broadcast = new Intent();
            broadcast.setAction(CONNECTING);
            sendBroadcast(broadcast);

            try {
                // sleep 5 seconds
                Thread.sleep(5000l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (NetworkUtils.isConnected(this)) {
                connect(server, port, username, password, zone);
            } else {
                onDisconnected();
            }
            return;
        }

        onError("Disconnected");
    }

    private void onLoginSuccess() {
        connecting = false;

        retryCount = 0;

        Intent broadcast = new Intent();
        broadcast.setAction(LOGIN_SUCCESS);
        sendBroadcast(broadcast);

        joinRoom(getString(R.string.chat_room));
    }

    private void shoryuken() {
        if (preferences.isShoryuken()) {
            soundEngine.shoryuken(1f);
        }
    }

    private void ding() {
        if (preferences.isDingOnMessage()) {
            soundEngine.alert(1f);
        }
    }

    private void vibrate() {
        if (preferences.isVibrate()) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

//            long[] pattern = {0, 630, 100, 200, 100, 200};
            long[] pattern = {0, 200, 100, 200, 100, 200};

            // Vibrate once
            v.vibrate(pattern, -1);
        }
    }

    private void updateNotification(String title, String msg, String ticker, int icon) {
        Intent i = new Intent(this, ChatActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        Notification note = new Notification.Builder(this)
                .setContentIntent(pi)
                .setContentTitle(title)
                .setContentText(msg)
                .setSmallIcon(icon)
                .setTicker(ticker)
                .setWhen(System.currentTimeMillis())
                .build();

        note.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(1337, note);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.comparator = new UserComparator();
        this.scheduler = Executors.newScheduledThreadPool(1);

        sfs = new SmartFox();

        sfs.addEventListener(SFSEvent.CONNECTION, this);
        sfs.addEventListener(SFSEvent.CONNECTION_LOST, this);
        sfs.addEventListener(SFSEvent.LOGIN_ERROR, this);
        sfs.addEventListener(SFSEvent.LOGIN, this);
        sfs.addEventListener(SFSEvent.ROOM_JOIN_ERROR, this);
        sfs.addEventListener(SFSEvent.ROOM_JOIN, this);
        sfs.addEventListener(SFSEvent.USER_ENTER_ROOM, this);
        sfs.addEventListener(SFSEvent.USER_EXIT_ROOM, this);
        sfs.addEventListener(SFSEvent.PUBLIC_MESSAGE, this);
        sfs.addEventListener(SFSEvent.EXTENSION_RESPONSE, this);
        sfs.addEventListener(SFSEvent.ROOM_VARIABLES_UPDATE, this);
        sfs.addEventListener(SFSEvent.USER_VARIABLES_UPDATE, this);

        // Load Sounds
        soundEngine = new SoundEngine(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        WifiLockUtils.unlockWifi();

        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isConnected()) {
            showConnectedNotification();
        } else {
            showLoginNotification();
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void dispatch(BaseEvent evt) throws SFSException {
        if (evt.getType().equals(SFSEvent.CONNECTION)) {
            // Retrieve event parameters
            Map params = evt.getArguments();

            if ((Boolean)params.get("success")) {
                Log.d("BG Chat", "Connected to BG Chat");
                login(username, password, zone);
            }
            else {
                onDisconnected();
            }
            return;
        }

        if (evt.getType().equals(SFSEvent.CONNECTION_LOST)) {
            Map params = evt.getArguments();
            String reason = (String)params.get("reason");
            Log.d("BG Chat", "Connection Lost - Reason: " + reason);

            boolean kicked = false;
            boolean manual = false;

            if (reason.equals("kick")) {
                kicked = true;
            } else if (reason.equals("manual")) {
                manual = true;
            }

            stopPinging();

            if (kicked) {
                onError("You got kicked. Cry some moar!");
                return;
            }

            if (manual) {
                connecting = false;

                retryCount = 0;
                return;
            }

            onDisconnected();
            return;
        }

        if (evt.getType().equals(SFSEvent.LOGIN)) {
            Log.d("BG Chat", "Login successful");
            onLoginSuccess();
            return;
        }

        if (evt.getType().equals(SFSEvent.LOGIN_ERROR)) {
            Log.d("BG Chat", "Login error");
            onError("Login unsuccessful");
            return;
        }

        if (evt.getType().equals(SFSEvent.ROOM_JOIN)) {
            // Retrieve event parameters
            Map params = evt.getArguments();
            Room room = (Room)params.get("room");
            Log.d("BG Chat", "Joined Room: " + room.getName());
            if (!room.getName().equals(this.room)) {
                return;
            }

            Log.d("BG Chat", "Getting Users List - Room: " + room.getName());
            currentRoom = room;
            List<User> users = currentRoom.getUserList();
            updateUsers(users);

            printTopic(currentRoom);
            printUsers(getChatUsers());
            printHistory();

            showConnectedNotification();
            startPinging();
            return;
        }

        if (evt.getType().equals(SFSEvent.ROOM_JOIN_ERROR)) {
            Log.d("BG Chat", "Room join error");
            onError("Unable to join");
            return;
        }

        if (evt.getType().equals(SFSEvent.USER_ENTER_ROOM)) {
            Map params = evt.getArguments();
            Room room = (Room)params.get("room");
            User user = (User)params.get("user");

            Log.d("BG Chat", "User Entered Room: " + room.getName() + ", User: " + user.getName());
            if (room.getName().equals(this.room)) {
                Log.d("BG Chat", "Updating Users List - Room: " + room.getName());
                List<User> users = room.getUserList();
                updateUsers(users);

                String msg = "<i>" + ChatUtils.getColoredName(user.getName()) + " joined.</i>";
                writeSystemMessage(System.currentTimeMillis(), msg, false);
            }

            return;
        }

        if (evt.getType().equals(SFSEvent.USER_EXIT_ROOM)) {
            Map params = evt.getArguments();
            Room room = (Room)params.get("room");
            User user = (User)params.get("user");

            Log.d("BG Chat", "User Exited Room: " + room.getName() + ", User: " + user.getName());
            if (room.getName().equals(this.room)) {
                List<User> users = room.getUserList();

                if (!user.isItMe()) {
                    updateUsers(users);
                    String msg = "<i>" + ChatUtils.getColoredName(user.getName()) + " left.</i>";
                    writeSystemMessage(System.currentTimeMillis(), msg, false);
                }
            }

            return;
        }

        if (evt.getType().equals(SFSEvent.PUBLIC_MESSAGE)) {
            Map params = evt.getArguments();
            Room room = (Room)params.get("room");
            boolean isAdmin = false;
            if (room.getName().equals("BGAdmin")) {
                isAdmin = true;
            }

            User sender = (User)evt.getArguments().get("sender");
            String message = (String)evt.getArguments().get("message");

            message = ChatUtils.addEmoticons(TextUtils.htmlEncode(message));
            writeMessage(sender.getName(), message, isAdmin);
            return;
        }

        if (evt.getType().equals(SFSEvent.EXTENSION_RESPONSE)) {
            Map args = evt.getArguments();
            String command = (String)args.get("cmd");
            ISFSObject params = (ISFSObject)args.get("params");

            if (command.equals("newThreadEvent")) {
                String threadId = params.getUtfString("threadId");
                String threadName = params.getUtfString("threadName");

                Log.d("BG Chat", "New Thread: " + threadName);
                String msg = "<b>New Thread</b>: <a href=\"http://www.bluegartr.com/threads/" + threadId + "?goto=newpost\"><u>" + threadName + "</u></a>";
                writeSystemMessage(System.currentTimeMillis(), msg, false);
                return;
            }

            if (command.equals("newPostEvent")) {
                String postId = params.getUtfString("postId");
                String threadName = params.getUtfString("threadName");
                String threadId = params.getUtfString("threadId");
                String userName = params.getUtfString("userName");

                Log.d("BG Chat", "New Post - user: " + userName + ", thread: " + threadName);
                String msg = "<b>New Post by " + userName
                        + " in</b>: <a href=\"http://www.bluegartr.com/threads/" + threadId + "?goto=newpost\"><u>" + threadName + "</u></a>";
                writeSystemMessage(System.currentTimeMillis(), msg, false);
                return;
            }

            if (command.equals("newUserEvent")) {
                String userid = params.getUtfString("userid");
                String username = params.getUtfString("username");
                String email = params.getUtfString("email");
                String ipAddress = params.getUtfString("ipaddress");

                Log.d("BG Chat", "New User Event: " + userid + ", " + username + ", " + email + ", " + ipAddress);
                String msg = TextUtils.htmlEncode("<adm> ") + "<b>New User</b>: <a href=\"http://www.bluegartr.com/members/"
                        + userid + "\">" + username + " from " + ipAddress + " at " + email + "</a>";
                writeSystemMessage(System.currentTimeMillis(), msg, false);
                return;
            }

            if (command.equals("kickEvent")) {
                String kickFrom = params.getUtfString("kickfrom");
                String kickTo = params.getUtfString("kickto");

                Log.d("BG Chat", kickTo + " was kicked by " + kickFrom);
                String msg = ChatUtils.getColoredName(kickTo) + " was kicked by " + ChatUtils.getColoredName(kickFrom);
                writeSystemMessage(System.currentTimeMillis(), msg, false);
                return;
            }

            if (command.equals("history")) {
                String sender = params.getUtfString("username");
                String message = params.getUtfString("message");
                long timestamp = params.getInt("dateline") * 1000l;

                message = ChatUtils.addEmoticons(message);
                writeMessage(sender, message, false, timestamp);
                return;
            }

            if (command.equals("systemMsg")) {
                String msg = params.getUtfString("message");

                Log.d("BG Chat", "System Message: " + msg);
                writeSystemMessage(System.currentTimeMillis(), msg, false);
                return;
            }

            return;
        }

        if (evt.getType().equals(SFSEvent.ROOM_VARIABLES_UPDATE)) {
            Map params = evt.getArguments();
            Room room = (Room)params.get("room");

            if (room.getName().equals(this.room)) {
                List<String> changedVars = (List<String>)params.get("changedVars");
                if (changedVars.contains("topicmsg")) {
                    Log.d("BG Chat", "Topic Updated");
                    printTopic(room);
                }
            }

            return;
        }

        if (evt.getType().equals(SFSEvent.USER_VARIABLES_UPDATE)) {
            Map params = evt.getArguments();
            Room room = (Room)params.get("room");

            if (room != null && room.getName().equals(this.room)) {
                User user = (User)params.get("user");
                List<String> changedVars = (List<String>)params.get("changedVars");
                for (String var : changedVars) {
                    Log.d("BG Chat", user.getName() + "'s " + var + " has been updated");
                    String msg = "<i>" + ChatUtils.getColoredName(user.getName()) + "'s " + var + " has been updated.</i>";
                    writeSystemMessage(System.currentTimeMillis(), msg, false);
                }
            }

            return;
        }
    }
}
