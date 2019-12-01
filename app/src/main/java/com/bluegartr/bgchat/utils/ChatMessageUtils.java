package com.bluegartr.bgchat.utils;

import android.text.TextUtils;

/**
 * Created by Daniel on 9/6/14.
 */
public class ChatMessageUtils {
    public static String getChatMessageHtml(long timestamp, String sender, String message, boolean isAdmin) {
        return "[" + ChatUtils.getTimestamp(timestamp)
                + "] " + (isAdmin ? TextUtils.htmlEncode("<adm> ") : "") + ChatUtils.getColoredName(sender) + ": "
                + ChatUtils.linkifyString(message);
    }

    public static String getSystemMessageHtml(long timestamp, String message, boolean isAdmin) {
        return "[" + ChatUtils.getTimestamp(timestamp)
                + "] " + (isAdmin ? "<adm> " : "") + ChatUtils.linkifyString(message);
    }

    public static String getLogMessageHtml(String message) {
        return ChatUtils.linkifyString(message);
    }
}
