package com.moosd.netghost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ChangeReceiver extends BroadcastReceiver {
    public ChangeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences(
                "settings", 0);
        if (intent.getAction().equals("com.moosd.netghost.RANDOMISE_MAC")) {
            if (settings.getBoolean("spoofenabled", false)) {
                String rmac = Util.randomMAC();
                settings.edit().putString("macset", rmac).commit();
                WifiReceiver.state = 1;
                Util.setMAC(rmac, context);
            }
        } else if (intent.getAction().equals("com.moosd.netghost.SET_MAC_STATIC")) {
            if (settings.getBoolean("spoofenabled", false)) {
                String rmac = intent.getStringExtra("mac");
                settings.edit().putString("macset", rmac).commit();
                System.out.println("MAC static: "+rmac);
                WifiReceiver.state = 1;
                Util.setMAC(rmac, context);
            }
        } else if (intent.getAction().equals("com.moosd.netghost.SET_MAC_ONESHOT")) {
            if (settings.getBoolean("spoofenabled", false)) {
                String rmac = intent.getStringExtra("mac");
                System.out.println("MAC oneshot: "+rmac);
                WifiReceiver.state = 1;
                Util.setMAC(rmac, context);
            }
        }
    }
}
