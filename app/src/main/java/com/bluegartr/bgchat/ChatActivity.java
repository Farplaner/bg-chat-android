package com.bluegartr.bgchat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.bluegartr.bgchat.model.BGChatPreferences;
import com.bluegartr.bgchat.model.ChatCommand;
import com.bluegartr.bgchat.model.ChatUser;
import com.bluegartr.bgchat.service.ChatService;
import com.bluegartr.bgchat.utils.ChatMessageUtils;
import com.bluegartr.bgchat.utils.FetchUsernameColors;
import com.bluegartr.bgchat.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ChatActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private static final int PREFERENCES_SETTINGS = 1;

    private ChatService chatService;
    private boolean serviceBound = false;

    private BGChatPreferences preferences;
    private boolean autoScroll = true;

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private TextView chatTextView;
    private EditText chatEditText;

    private EditText userText;
    private EditText passText;
    private CheckBox rememberMeCheckBox;

    private AlertDialog loginDialog;
    private AlertDialog alertDialog;
    private ProgressDialog progress;

    public ChatService getChatService() {
        return chatService;
    }

    private void updateChatLog() {
        chatTextView.beginBatchEdit();
        chatTextView.setText("");

        Queue<String> chatLog = chatService.getChatLog();
        for (String msg : chatLog) {
            if (!TextUtils.isEmpty(chatTextView.getText())) {
                chatTextView.append("\n");
            }
            chatTextView.append(Html.fromHtml(msg, new HtmlResourceImageGetter(), null));
        }
        chatTextView.endBatchEdit();
    }

    public void updateUsers(List<ChatUser> users) {
        final List<ChatUser> userList = users;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNavigationDrawerFragment.updateUsers(userList);
            }
        });
    }

    public void onError(String error) {
        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        final CharSequence msg = error;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progress.dismiss();

                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Error");
                builder.setMessage(msg);
                builder.setCancelable(false);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                        alertDialog = null;
                        returnToLoginActivity();
                    }
                });

                alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }

    public void onLoginSuccess() {
        Log.d("BG Chat", "Login successful");

        preferences.setUsername(chatService.getUsername());
        preferences.setPassword(chatService.getPassword());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progress.dismiss();
            }
        });
    }

    private void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    private void autoScroll() {
        if (autoScroll) {
            final int scrollAmount = chatTextView.getLayout().getLineTop(chatTextView.getLineCount()) - chatTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                chatTextView.scrollTo(0, scrollAmount);
            else
                chatTextView.scrollTo(0, 0);
        }
    }

    private void fillLoginDialog() {
        if (!TextUtils.isEmpty(preferences.getUsername())) {
            userText.setText(preferences.getUsername());
        }
        if (!TextUtils.isEmpty(preferences.getPassword())) {
            passText.setText(preferences.getPassword());
        }
        rememberMeCheckBox.setChecked(preferences.isRememberLogin());
    }

    private void returnToLoginActivity() {
        if (alertDialog == null) {
            chatService.showLoginNotification();

            fillLoginDialog();
            loginDialog.show();
        }
    }

    private void onMessageSend(String msg) {
        ChatCommand cmd = ChatCommand.parseCommand(msg);

        if (TextUtils.isEmpty(cmd.getCommand())) {
            chatService.sendMessage(msg.toString());
            return;
        }

        if (cmd.getCommand().equals(ChatCommand.HELP_COMMAND)) {
            chatService.addToChatLog(ChatMessageUtils.getLogMessageHtml("/help - not sure what you're doing here"));
            chatService.addToChatLog(ChatMessageUtils.getLogMessageHtml("/topic - view the current topic"));
            chatService.addToChatLog(ChatMessageUtils.getLogMessageHtml("/who /w - list the users online"));
            chatService.addToChatLog(ChatMessageUtils.getLogMessageHtml("/random - roll a dice"));

            if (chatService.isMod()) {
                chatService.addToChatLog(ChatMessageUtils.getLogMessageHtml("/topic [new topic name] - set a new topic"));
                chatService.addToChatLog(ChatMessageUtils.getLogMessageHtml("/kick [user name] - kick a user"));
                chatService.addToChatLog(ChatMessageUtils.getLogMessageHtml("/adm [message] - send a secret message to all the mods/admins online"));
            }
            updateChatLog();
            return;
        }

        if (cmd.getCommand().equals(ChatCommand.TOPIC_COMMAND)) {
            if (TextUtils.isEmpty(cmd.getParam()) || !chatService.isMod()) {
                chatService.printTopic(chatService.getCurrentRoom());
            } else {
                String topic = cmd.getParam();
                chatService.makeNewTopic(topic);
            }
            return;
        }

        if (cmd.getCommand().equals(ChatCommand.WHOIS_COMMAND)) {
            chatService.printUsers(chatService.getChatUsers());
            return;
        }

        if (cmd.getCommand().equals(ChatCommand.RANDOM_COMMAND)) {
            chatService.random();
            return;
        }

        if (chatService.isMod()) {
            if (cmd.getCommand().equals(ChatCommand.ADMIN_COMMAND) && !TextUtils.isEmpty(cmd.getParam())) {
                chatService.adminMessage(cmd.getParam(), getString(R.string.chat_admin_room));
                return;
            }

            if (cmd.getCommand().equals(ChatCommand.KICK_COMMAND) && !TextUtils.isEmpty(cmd.getParam())) {
                chatService.kickUser(cmd.getParam());
                return;
            }
        }

        chatService.addToChatLog(ChatMessageUtils.getLogMessageHtml("Unrecognized command. See /help for more info."));
        updateChatLog();
        return;
    }

    private void getPreferences() {
        preferences = new BGChatPreferences(this);

        if (preferences.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (chatService != null) {
            chatService.setPreferences(preferences);
        }

        if (preferences.isRememberLogin() && chatService != null && chatService.isConnected()) {
            preferences.setUsername(chatService.getUsername());
            preferences.setPassword(chatService.getPassword());
        }
    }

    public void startChatService() {
        Log.d("BG Chat", "Starting Chat Service...");
        Intent i = new Intent(this, ChatService.class);
        startService(i);

        // Bind to ChatService
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void stopChatService() {
        Log.d("BG Chat", "Stopping Chat Service...");
        Intent i = new Intent(this, ChatService.class);
        stopService(i);

        // Unbind from the service
        if (serviceBound) {
            unbindService(mConnection);
            serviceBound = false;
        }
    }

    private void onServiceConnected() {
        chatService.setPreferences(preferences);

        // If connecting
        if (chatService.isConnecting()) {
            showProgress();

            updateChatLog();
            return;
        }

        // Check to see if chat is connected
        if (!chatService.isConnected()) {
            if (alertDialog == null) {
                Log.d("BG Chat", "Chat Service not connected to BGChat... showing Login Dialog");
                fillLoginDialog();
                loginDialog.show();
            }
            return;
        }

        updateUsers(chatService.getChatUsers());
        updateChatLog();
    }

    private void createLoginDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View loginView = inflater.inflate(R.layout.login_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(loginView);

        userText = (EditText) loginView.findViewById(R.id.login_name);
        passText = (EditText) loginView.findViewById(R.id.login_password);
        passText.setTypeface(Typeface.DEFAULT);
        passText.setTransformationMethod(new PasswordTransformationMethod());

        rememberMeCheckBox = (CheckBox) loginView.findViewById(R.id.checkbox_remember_me);

        builder.setTitle("Login to BG Chat");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (!NetworkUtils.isConnected(ChatActivity.this)) {
                    onError("No network available");
                    return;
                }

                if (TextUtils.isEmpty(userText.getText())) {
                    userText.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(passText.getText())) {
                    passText.requestFocus();
                    return;
                }

                String server = getString(R.string.chat_server);
                int port = getResources().getInteger(R.integer.chat_server_port);
                String username = userText.getText().toString();
                String password = passText.getText().toString();
                String zone = getString(R.string.chat_zone);

                boolean rememberLogin = rememberMeCheckBox.isChecked();
                preferences.setRememberLogin(rememberLogin);

                showProgress();

                if (chatService.isConnected()) {
                    chatService.login(username, password, zone);
                } else {
                    chatService.connect(server, port, username, password, zone);
                }
            }
        });

        loginDialog = builder.create();
    }

    private void showProgress() {
        progress.setMessage(getString(R.string.logging_in_text));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("BG Chat", "BG Chat - onCreate");

        // Load Content View
        setContentView(R.layout.activity_chat);

        // Load Username Colors
        new FetchUsernameColors().execute();

        // Setup Slideout Drawer
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Create Progress Dialog
        progress = new ProgressDialog(this);
        progress.setCanceledOnTouchOutside(false);

        // Create Login Dialog if not already created
        createLoginDialog();

        // Setup TextView
        chatTextView = (TextView)findViewById(R.id.chatTextView);
        chatTextView.setMovementMethod(LinkMovementMethod.getInstance());
        chatTextView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Log.d("BG Chat", "Layout Changed... Autoscrolling");
                autoScroll();
            }
        });

        // Setup TextBox
        chatEditText = (EditText)findViewById(R.id.chatEditText);
        chatEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent evt) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    CharSequence msg = textView.getText();

                    if (!TextUtils.isEmpty(msg)) {
                        onMessageSend(msg.toString());
                        textView.setText("");
                    }
                }
                return false;
            }
        });

        // Moves up text box when you're typing so it won't be hidden by the on screen keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("BG Chat", "BG Chat - onResume");

        // Check for preference changes
        getPreferences();

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ChatService.UPDATE_CHATLOG);
        filter.addAction(ChatService.UPDATE_USERS);
        filter.addAction(ChatService.CONNECTING);
        filter.addAction(ChatService.LOGIN_SUCCESS);
        filter.addAction(ChatService.WRITE_ERROR_MESSAGE);
        registerReceiver(receiver, filter);

        // Check to see if service is up
        if (chatService == null) {
            Log.d("BG Chat", "Chat Service is not running");
            startChatService();
        } else {
            updateChatLog();
            updateUsers(chatService.getChatUsers());

            if (chatService.isConnecting()) {
                showProgress();
                return;
            }

            if (chatService.getError() != null) {
                onError(chatService.getError());
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopChatService();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, PREFERENCES_SETTINGS);
            return true;
        }
        if (id == R.id.action_autoscroll) {
            autoScroll = !item.isChecked();
            Log.d("BG Chat", "Auto Scroll: " + autoScroll);
            item.setChecked(autoScroll);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setTitle("Confirm");
        alertbox.setMessage("Disconnect from BG Chat?");

        alertbox.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        Log.d("BG Chat", "Quitting BG Chat...");
                        dialog.dismiss();

                        chatService.disconnect();

                        finish();
                    }
                }
        );

        alertbox.setNeutralButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                    }
                }
        );
        alertbox.show();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
            return rootView;
        }
    }

    public class HtmlResourceImageGetter implements Html.ImageGetter {
        @Override
        public Drawable getDrawable(String source) {
            int resourceId = getResources().getIdentifier(source, "drawable",getPackageName());
            Drawable drawable = getResources().getDrawable(resourceId);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            return drawable;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d("BG Chat", "Chat Service Connected");
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            chatService = binder.getService();
            ChatActivity.this.onServiceConnected();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("BG Chat", "Chat Service Disconnected");
            serviceBound = false;
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.d("BG Chat", "Broadcast Receiver - Received: " + intent.getAction());

            if (intent.getAction().equals(ChatService.UPDATE_CHATLOG)) {
                updateChatLog();
                return;
            }

            if (intent.getAction().equals(ChatService.UPDATE_USERS)) {
                ArrayList<ChatUser> chatUsers = intent.getParcelableArrayListExtra("users");
                updateUsers(chatUsers);
                return;
            }

            if (intent.getAction().equals(ChatService.CONNECTING)) {
                showProgress();
                return;
            }

            if (intent.getAction().equals(ChatService.LOGIN_SUCCESS)) {
                onLoginSuccess();
                return;
            }

            if (intent.getAction().equals(ChatService.WRITE_ERROR_MESSAGE)) {
                String msg = intent.getStringExtra("msg");

                onError(msg);
                return;
            }
        }
    };
}
