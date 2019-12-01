package com.bluegartr.bgchat.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by tyang on 8/29/2014.
 */
public class WifiLockUtils {
    private static PowerManager powerManager;
    private static PowerManager.WakeLock wakeLock;
    private static WifiManager.WifiLock wifiLock;

    public static void lockWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            if (powerManager == null) {
                powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            }

            if (wakeLock == null) {
                Log.d("BG Chat", "Acquiring Wake Lock...");
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BG Chat");
                wakeLock.acquire();
            }

            if (wifiLock == null) {
                Log.d("BG Chat", "Acquiring Wifi Lock...");
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "BG Chat");
                wifiLock.acquire();
            }
        }
    }

    public static void unlockWifi() {
        if (wifiLock != null) {
            Log.d("BG Chat", "Releasing Wifi Lock...");
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
            wifiLock = null;
        }

        if (wakeLock != null) {
            Log.d("BG Chat", "Releasing Wake Lock...");
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            wakeLock = null;
        }
    }
}
