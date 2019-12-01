package com.bluegartr.bgchat.utils;

import android.util.Log;

import com.bluegartr.bgchat.model.ChatUser;
import com.bluegartr.bgchat.model.UserGroup;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel on 7/28/14.
 */
public class ChatUtils {
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private static final String chatNamesUrl = "http://www.bluegartr.com/chat/names.php";

    public static final String[] nameColors = {
        "C70000", "00D2E4", "EE99F7", "FF7B94", "ADCD32", "B50000", "ED967A", "CD853F", "FF8C00", "FF635F", "008080", "CD3753", "00FF00", "FFFF00", "48A0ED", "C3CE45", "59B73C", "763CB7"
    };

    private static Map<String, String> overrideColors = null;

    public static String loadStringFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);

        String line = IOUtils.toString(new InputStreamReader(url.openStream()));

        return line;
    }

    public static Map<String, String> getOverrideColors() throws IOException {
        Log.d("BG Chat", "GetOverrideColors");
        if (overrideColors != null) {
            Log.d("BG Chat", "Override Color Size: " + overrideColors.size());
            return overrideColors;
        }

        String colorString = loadStringFromUrl(chatNamesUrl);
        Log.d("BG Chat", "Color String: " + colorString);

        overrideColors = new HashMap<String, String>();
        String[] colorLines = colorString.split("\n");
        for (String inputLine : colorLines) {
            if (inputLine.contains(";")) {
                String[] keyvalue = inputLine.split(";");
                String user = keyvalue[0];
                String colorName = keyvalue[1];
                overrideColors.put(user, colorName);
            }
        }
        Log.d("BG Chat", "Override Color Size: " + overrideColors.size());
        return overrideColors;
    }

    public static String getTimestamp(long milliseconds) {
        return timeFormat.format(new Date(milliseconds));
    }

    public static String getNameSymbol(ChatUser user) {
        int usergroup = user.getGroup();

        if (usergroup == UserGroup.ADMINISTRATOR) {
            return "@";
        }

        if (usergroup == UserGroup.MODERATOR) {
            return "+";
        }

        return "";
    }

    public static String getNameColor(String user) {
        int i = 0;

        for (int j = 0; j < user.length(); j++) {
            char c = user.charAt(j);

            i += (int)c;
        }
        return "#" + nameColors[i % nameColors.length];
    }

    public static String getColoredName(String user) {
        if (overrideColors != null && overrideColors.containsKey(user)) {
            return overrideColors.get(user);
        }

        return "<font color=" + getNameColor(user) + ">" + user + "</font>";
    }

    public static String linkifyString(String text) {
//        return text;
//        return text.replaceAll("(.*://[^<>[:space:]]+[[:alnum:]/])", "<a href=\"$1\">$1</a>");
        return text.replaceAll("(\\A|\\s)((http|https|ftp|mailto):\\S+)(\\s|\\z)", "$1<a href=\"$2\">$2</a>$4");
    }

    public static boolean containsWordIgnoreCase(String text, String word) {
        return text.toLowerCase().contains(word.toLowerCase());
//        String regex = ".*\\b" + word.toLowerCase() + "\\b.*";
//        return text.toLowerCase().matches(regex);
    }

    public static String addEmoticons(String message) {
//        message = message.replaceAll("(^|\\s)Kappa($|\\s)", " <img src=\"kappalq\"> ");

        return message;
    }
}
