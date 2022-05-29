package com.app.mediafly.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.app.mediafly.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utilities {
    public static final String LOG_TAG = Utilities.class.getSimpleName();

    public static final String CURRENT_SCREEN_PREFERENCE = "pref";

    public static int versionCode = BuildConfig.VERSION_CODE;

    public static String versionName = BuildConfig.VERSION_NAME;

    public static void setStringPreference(Context cxt,
                                           String key,
                                           String value,
                                           String prefName) {
        SharedPreferences.Editor editor = getPrefsEditor(cxt, prefName);
        editor.putString(key, value);
        editor.apply();
    }

    private static SharedPreferences.Editor getPrefsEditor(Context context,
                                                           String pref) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(pref,
                Context.MODE_PRIVATE);
        return sharedPreferences.edit();
    }

    public static String getStringPref(Context cxt,
                                       String key,
                                       String prefName) {
        SharedPreferences sharedPreferences = cxt.getSharedPreferences(prefName,
                Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, "");
    }

    public static void saveListInPreference(Context cxt,ArrayList<String>list,String prefName,String key){
        Gson gson = new Gson();
        String jsonString = gson.toJson(list);

        SharedPreferences.Editor editor = getPrefsEditor(cxt, prefName);
        editor.putString(key,jsonString);
        editor.apply();
    }

    public static ArrayList<String> getList(Context cxt, String prefName, String key){
        SharedPreferences sharedPreferences = cxt.getSharedPreferences(prefName,
                Context.MODE_PRIVATE);

        String jsonString =sharedPreferences.getString(key,"");
        Gson gson = new Gson();

        Type type = new TypeToken<ArrayList<String>>(){}.getType();
        ArrayList<String> list = gson.fromJson(jsonString,type);
        return list;
    }

    public static void saveIntPreference(Context cxt,
                                         String key,
                                         int value,
                                         String prefName) {
        SharedPreferences.Editor editor = getPrefsEditor(cxt, prefName);
        editor.putInt(key, value);
        editor.apply();
    }


    public static int getIntPreference(Context cxt,
                                       String key,
                                       String prefName) {
        SharedPreferences sharedPreferences = cxt.getSharedPreferences(prefName,
                Context.MODE_PRIVATE);
        return sharedPreferences.getInt(key, -1);
    }


    public static String getAndroidId(Context context) {
        String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Log.d("Android", "Android ID : " + android_id);

        return android_id;
    }

    /*public static void showSnackBar(final View layout,
                                    final Context context,
                                    final String msg,
                                    final int length,
                                    final int color) {
        Snackbar snackbar = Snackbar.make(layout, msg, length);
        snackbar.getView().setBackgroundColor(color);
        snackbar.show();
    }*/

    public static String getCurrentDate() {

        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date resultdate = new Date(time);

        return sdf.format(resultdate);
    }

   /* public static boolean isConnectedTo(String ssid, Context context) {
        boolean retVal = false;
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        if (wifiInfo != null) {
            String currentConnectedSSID = wifiInfo.getSSID();
            if (ssid.equalsIgnoreCase(currentConnectedSSID)) {
                Log.e("SSID", currentConnectedSSID);
                retVal = true;
            }
        }
        return retVal;
    }*/

    public static String getCurrentDatenew(){
        String date = new SimpleDateFormat("dd MMM", Locale.ENGLISH).format(new Date());
        return date;
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }
}
