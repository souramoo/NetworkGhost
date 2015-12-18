package com.moosd.netghost;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;

import android.provider.ContactsContract;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * Created by souradip on 23/06/15.
 */
public class Util {

    public static void askToInstall(final Context ctx, final boolean update) {
        new AlertDialog.Builder(ctx)
                .setTitle("Install")
                .setMessage("To set this up I need to modify your system partition. This might brick your device (but shouldnt). You cool with that?")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performInstall(ctx);
                        if (update) {
                            if (Util.isInstalled()) {
                                MainActivity.me.settings.edit().putBoolean("spoofenabled", true).commit();
                            }
                            MainActivity.me.updateUX();
                        }
                    }
                })
                .create()
                .show();
    }

    public static void askToInstall(final Context ctx) {
        askToInstall(ctx, false);
    }

    public static void uninstall(Context ctx) {
        // show loading menu
        ProgressDialog dialog = ProgressDialog.show(ctx, "Uninstalling",
                "Remounting system...", true);
        dialog.show();
        // remount system
        final String[] result = {""};
        Command command = new Command(0, "mount -o rw,remount /system") {
            @Override
            public void output(int id, String line) {
            }
        };
        runCmd(command);

        // check if success
        command = new Command(0, "mount|grep system") {
            @Override
            public void output(int id, String line) {
                if(line.contains("rw")) result[0] = "yes";
            }
        };
        runCmd(command);
        if(!result[0].equals("yes")) {
            dialog.hide();
            new AlertDialog.Builder(ctx).setTitle("Error").setMessage("Error remounting system. Sorry! Nothing was touched.").setPositiveButton("OK", null).create().show();
            return;
        }
        result[0] = "";

        dialog.setMessage("Moving wpa_supplicant back");
        // move wpa_supplicant
        command = new Command(0, "mv /system/bin/wpa_supplicant_real /system/bin/wpa_supplicant") {
            @Override
            public void output(int id, String line) {
            }
        };
        runCmd(command);

        // check if success
        command = new Command(0, "ls /system/bin/wpa_*") {
            @Override
            public void output(int id, String line) {
                if(line.contains("_real")) result[0] = "yes";
            }
        };
        runCmd(command);
        if(result[0].equals("yes")) {
            dialog.hide();
            new AlertDialog.Builder(ctx).setTitle("Error").setMessage("Error moving wpa_supplicant back. Sorry! Failed.").setPositiveButton("OK", null).create().show();
            return;
        }
        result[0] = "";

        dialog.hide();
        new AlertDialog.Builder(ctx).setTitle("Success").setMessage("Uninstalled code successfully").setPositiveButton("OK", null).create().show();
    }

    public static void performInstall(Context ctx) {
        // show loading menu
        ProgressDialog dialog = ProgressDialog.show(ctx, "Installing",
                "Remounting system...", true);
        dialog.show();
        // remount system
        final String[] result = {""};
        Command command = new Command(0, "mount -o rw,remount /system") {
            @Override
            public void output(int id, String line) {
            }
        };
        runCmd(command);

        // check if success
        command = new Command(0, "mount|grep system") {
            @Override
            public void output(int id, String line) {
                if(line.contains("rw")) result[0] = "yes";
            }
        };
        runCmd(command);
        if(!result[0].equals("yes")) {
            dialog.hide();
            new AlertDialog.Builder(ctx).setTitle("Error").setMessage("Error remounting system. Sorry! Nothing was touched.").setPositiveButton("OK", null).create().show();
            return;
        }
        result[0] = "";

        dialog.setMessage("Moving wpa_supplicant");
        // move wpa_supplicant
        command = new Command(0, "mv /system/bin/wpa_supplicant /system/bin/wpa_supplicant_real") {
            @Override
            public void output(int id, String line) {
            }
        };
        runCmd(command);

        // check if success
        command = new Command(0, "ls /system/bin/wpa_*") {
            @Override
            public void output(int id, String line) {
                if(line.contains("_real")) result[0] = "yes";
            }
        };
        runCmd(command);
        if(!result[0].equals("yes")) {
            dialog.hide();
            new AlertDialog.Builder(ctx).setTitle("Error").setMessage("Error moving wpa_supplicant. Sorry! Nothing was touched.").setPositiveButton("OK", null).create().show();
            return;
        }
        result[0] = "";

        // inject our script
        dialog.setMessage("Injecting our mac changing code");

        command = new Command(0, "echo '#!/system/xbin/bash|/system/xbin/busybox ifconfig wlan0 up hw ether $(cat /dev/mac)|/system/bin/wpa_supplicant_real $@' | sed 's/|/\\n/g' > /system/bin/wpa_supplicant", "busybox chmod +x /system/bin/wpa_supplicant") {
            @Override
            public void output(int id, String line) {
            }
        };
        runCmd(command);
        // check if success

        if(isInstalled()) {
            dialog.hide();
            new AlertDialog.Builder(ctx).setTitle("Success").setMessage("Installed code successfully").setPositiveButton("OK", null).create().show();
        } else {
            //dialog.hide();
            //new AlertDialog.Builder(ctx).setTitle("Error").setMessage("Error inserting our wpa_supplicant. Will try to revert now.").setPositiveButton("OK", null).create().show();
            dialog.setMessage("Error inserting out wpa_supplicant. Trying to revert...");
            command = new Command(0, "mv /system/bin/wpa_supplicant_real /system/bin/wpa_supplicant") {
                @Override
                public void output(int id, String line) {
                }
            };
            runCmd(command);
            command = new Command(0, "ls /system/bin/wpa_*") {
                @Override
                public void output(int id, String line) {
                    if(line.contains("_real")) result[0] = "yes";
                }
            };
            runCmd(command);
            if(result[0].equals("yes")) {
                new AlertDialog.Builder(ctx).setTitle("Error").setMessage("Error reverting wpa_supplicant! System in inconsistent state.").setPositiveButton("OK", null).create().show();
            } else {
                new AlertDialog.Builder(ctx).setTitle("Error").setMessage("Reverted successfully.").setPositiveButton("OK", null).create().show();
            }
            dialog.hide();
        }
    }

    public static boolean isInstalled() {
        try {
            byte[] buffer = new byte[4];
            InputStream is = new FileInputStream("/system/bin/wpa_supplicant");
            if (is.read(buffer) != buffer.length) {
            }
            is.close();
            if (new String(buffer).equals("#!/s"))
                return true;
            else return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

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
        if (!isInstalled()) {
            askToInstall(context);
            return;
        }
        runCmd(new CommandCapture(0, "setenforce 0"));
        try {
            Thread.sleep(200);
        } catch (Exception e) {
        }

        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled(false);
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }

        System.out.println("setting MAC: " + rmac);
        //runCmd(new CommandCapture(0, "busybox ifconfig wlan0 hw ether " + rmac));
        runCmd(new CommandCapture(0, "echo \"" + rmac + "\" > /dev/mac"));
        try {
            Thread.sleep(200);
        } catch (Exception e) {
        }

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
                    System.out.println("MAC - " + split[1]);
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
