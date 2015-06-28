package com.moosd.netghost;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * Created by souradip on 23/06/15.
 */
public class Util {


    public static void runCmd(Command command) {
        boolean finished = false;
        try {
            Command com = RootTools.getShell(true).add(command);
            while (!finished) {
                try {
                    com.waitForFinish(100);
                    finished = true;
                } catch (InterruptedException e) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static String randomMAC() {
        Random r = new Random();
        StringBuffer sb = new StringBuffer("e8:99:c4:");
        while (sb.length() < 17) {
            sb.append(Integer.toHexString(r.nextInt()).substring(0, 1));
            sb.append(Integer.toHexString(r.nextInt()).substring(0, 1));
            sb.append(":");
        }

        return sb.toString().substring(0, 17);
    }

    public static String randomHostname() {
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while (sb.length() < 16) {
            sb.append(Integer.toHexString(r.nextInt()).substring(0, 1));
        }

        return "android-" + sb.toString().substring(0, 16);
    }

    public static void setMAC(String rmac, Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled(false);
        try {
            Thread.sleep(1000);
        } catch(Exception e){}

        System.out.println("setting MAC: " + rmac);
        runCmd(new CommandCapture(0, "busybox ifconfig wlan0 hw ether " + rmac));
        try {
            Thread.sleep(200);
        } catch(Exception e){}

        wifi.setWifiEnabled(true);
    }

    public static void setHost(String host) {
        runCmd(new CommandCapture(0, "setprop net.hostname " + host));
    }

    public static String getMAC() {
        final String[] result = {"deadbebe"};
        Command command = new Command(0, "busybox ip addr show dev wlan0") {
            @Override
            public void output(int id, String line) {
                if (line.contains("link/ether")) {
                    String[] split = line.trim().split(" ");
                    System.out.println("MAC - "+split[1]);
                    result[0] = split[1];
                }
            }
        };
        runCmd(command);
        return result[0];
    }

    public static String getHost() {
        final String[] result = {"deadbebe"};
        Command command = new Command(0, "getprop net.hostname") {
            @Override
            public void output(int id, String line) {
                result[0] = line;
            }
        };
        runCmd(command);
        return result[0];
    }
}
