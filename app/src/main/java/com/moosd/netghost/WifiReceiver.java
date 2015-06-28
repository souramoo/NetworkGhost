package com.moosd.netghost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class WifiReceiver extends BroadcastReceiver {
    private final String TAG = "WifiReceiver";
    public static int state = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_UNKNOWN);
        String wifiStateText = "No State";

        String action = intent.getAction();

        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            WifiManager manager = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);
            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            NetworkInfo.State state = networkInfo.getState();

            if (state == NetworkInfo.State.DISCONNECTED) {
                if (manager.isWifiEnabled()) {

                    doUpdate(context);
                    wifiStateText = "WIFI_STATE_DISCONNECTED";
                }
            }
        }

        switch (wifiState) {
            case WifiManager.WIFI_STATE_DISABLING:
                wifiStateText = "WIFI_STATE_DISABLING";
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                wifiStateText = "WIFI_STATE_DISABLED";
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                wifiStateText = "WIFI_STATE_ENABLING";

                break;
            case WifiManager.WIFI_STATE_ENABLED:
                wifiStateText = "WIFI_STATE_ENABLED";
                if (state == 1) state--;
                else
                    doUpdate(context);
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
                wifiStateText = "WIFI_STATE_UNKNOWN";
                break;
            default:
                break;
        }

        System.out.println("WIFI_recv: " + wifiStateText);
    }

    public void doUpdate(Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                "settings", 0);
        if (settings.getBoolean("spoofenabled", false)) {
            String nhost = "";
            if (settings.getBoolean("hostrandomise", false)) {
                nhost = Util.randomHostname();
                settings.edit().putString("hostset", nhost).commit();
            } else {
                nhost = settings.getString("hostset", "");
            }
            Util.setHost(nhost);

            String rmac = "";
            if (settings.getBoolean("macrandomise", false)) {
                rmac = Util.randomMAC();
                settings.edit().putString("macset", rmac).commit();
            } else {
                rmac = settings.getString("macset", Util.randomMAC());
            }
            state = 1;
            Util.setMAC(rmac, context);
        }
    }

}