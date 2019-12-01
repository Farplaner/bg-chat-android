package com.bluegartr.bgchat.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.bluegartr.bgchat.R;
import com.bluegartr.bgchat.utils.EncryptionUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tyang on 8/29/2014.
 */
public class BGChatPreferences {
    private Context context;

    private boolean keepScreenOn;
    private boolean autoReconnect;
    private boolean rememberLogin;
    private List<String> notifyWords;
    private boolean vibrate;
    private boolean shoryuken;
    private boolean dingOnMessage;
    private boolean keepWifiOn;

    private String secretKey;
    private String username;
    private String password;

    public BGChatPreferences(Context context) {
        this.context = context;

        update();
    }

    public void update() {
        Log.d("BG Chat", "Updating preferences...");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        keepScreenOn = sharedPrefs.getBoolean("prefKeepScreenOn", false);
        autoReconnect = sharedPrefs.getBoolean("prefAutoReconnect", false);
        rememberLogin = sharedPrefs.getBoolean("prefRememberLogin", false);
        String prefNotifyWords = sharedPrefs.getString("prefNotifyWords", "");
        vibrate = sharedPrefs.getBoolean("prefVibrate", true);
        shoryuken = sharedPrefs.getBoolean("prefSound", true);
        dingOnMessage = sharedPrefs.getBoolean("prefDingOnMessage", false);
        keepWifiOn = sharedPrefs.getBoolean("prefKeepWifiOn", false);

        if (!TextUtils.isEmpty(prefNotifyWords)) {
            String[] words = prefNotifyWords.split(",");
            notifyWords = Arrays.asList(words);
        } else {
            notifyWords = null;
        }

        if (rememberLogin) {
            secretKey = getSecretKey(sharedPrefs);
            username = sharedPrefs.getString(context.getString(R.string.pref_username_key), null);
            String prefPassword = sharedPrefs.getString(context.getString(R.string.pref_password_key), null);
            if (TextUtils.isEmpty(prefPassword)) {
                password = null;
            } else {
                password = EncryptionUtils.decipher(secretKey, prefPassword);
            }
        } else {
            setUsername(null);
            setPassword(null);
        }
    }

    private String getSecretKey(SharedPreferences sharedPrefs) {
        String secretKey = sharedPrefs.getString("prefSecretKey", null);
        if (secretKey == null) {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            SecureRandom random = new SecureRandom();

            secretKey = new BigInteger(130, random).toString(32);
            editor.putString("prefSecretKey", secretKey);
            editor.commit();
        }

        return secretKey;
    }

    public boolean isKeepScreenOn() {
        return keepScreenOn;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public boolean isRememberLogin() {
        return rememberLogin;
    }

    public List<String> getNotifyWords() {
        return notifyWords;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public boolean isShoryuken() {
        return shoryuken;
    }

    public boolean isDingOnMessage() {
        return dingOnMessage;
    }

    public boolean isKeepWifiOn() {
        return keepWifiOn;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            Log.d("BG Chat", "Deleting username...");
        } else {
            Log.d("BG Chat", "Saving username: " + username);
        }
        this.username = username;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (TextUtils.isEmpty(username)) {
            editor.remove(context.getString(R.string.pref_username_key));
        } else {
            editor.putString(context.getString(R.string.pref_username_key), username);
        }
        editor.commit();
    }

    public void setPassword(String password) {
        if (TextUtils.isEmpty(password)) {
            Log.d("BG Chat", "Deleting password...");
        } else {
            Log.d("BG Chat", "Saving password: " + password.replaceAll(".", "*"));
        }
        this.password = password;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        secretKey = getSecretKey(sharedPrefs);

        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (TextUtils.isEmpty(password)) {
            editor.remove(context.getString(R.string.pref_password_key));
        } else {
            String encryptedPass = EncryptionUtils.cipher(secretKey, password);
            editor.putString(context.getString(R.string.pref_password_key), encryptedPass);
        }
        editor.commit();
    }

    public void setRememberLogin(boolean rememberLogin) {
        this.rememberLogin = rememberLogin;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        Log.d("BG Chat", "Setting remember login: " + rememberLogin);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(context.getString(R.string.pref_remember_login_key), rememberLogin);
        editor.commit();
    }
}
