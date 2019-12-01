package com.bluegartr.bgchat.model;

import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Daniel on 8/2/14.
 */
public class ChatCommand {
    public static String HELP_COMMAND = "/help";
    public static String TOPIC_COMMAND = "/topic";
    public static String WHOIS_COMMAND = "/whois";
    public static String RANDOM_COMMAND = "/random";
    public static String ADMIN_COMMAND = "/admin";
    public static String KICK_COMMAND = "/kick";
    public static String UNKNOWN_COMMAND = "/";

    private String command, param;

    public static ChatCommand parseCommand(String message) {
        return new ChatCommand(message);
    }

    public ChatCommand(String message) {
        if (TextUtils.isEmpty(message)) return;

        if (!message.startsWith("/")) return;

        String firstWord = getFirstWord(message);
        if (TextUtils.isEmpty(firstWord)) return;

        Log.d("BG Chat", "Command: " + firstWord);
        if (HELP_COMMAND.startsWith(firstWord)) {
            command = HELP_COMMAND;
        } else if (TOPIC_COMMAND.startsWith(firstWord)) {
            command = TOPIC_COMMAND;
            param = getParam(message);
        } else if (WHOIS_COMMAND.startsWith(firstWord)) {
            command = WHOIS_COMMAND;
        } else if (RANDOM_COMMAND.startsWith(firstWord)) {
            command = RANDOM_COMMAND;
        } else if (ADMIN_COMMAND.startsWith(firstWord)) {
            command = ADMIN_COMMAND;
            param = getParam(message);
        } else if (KICK_COMMAND.startsWith(firstWord)) {
            command = KICK_COMMAND;
            param = getParam(message);
        } else {
            command = UNKNOWN_COMMAND;
        }
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    private String getFirstWord(String message) {
        Pattern pattern = Pattern.compile("([^\\s]+)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String getParam(String message) {
        Pattern pattern = Pattern.compile("([^\\s]+)([\\s]+)(.*$)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(3);
        }

        return null;
    }
}
