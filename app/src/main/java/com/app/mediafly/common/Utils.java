 package com.app.mediafly.common;

 import android.Manifest;
 import android.app.Activity;
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.content.pm.PackageManager;
 import android.net.ConnectivityManager;
 import android.net.NetworkInfo;
 import android.os.Build;
 import android.provider.Settings;
 import android.util.Log;
 import android.view.Window;
 import android.view.WindowManager;

 import androidx.core.app.ActivityCompat;
 import androidx.core.content.ContextCompat;

 import com.app.mediafly.R;

 public class Utils {
     public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

     public static void setStatusBarColor(Activity c) {
         if (Build.VERSION.SDK_INT >= 21) {
             Window window = c.getWindow();
             window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
             window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
             window.setStatusBarColor(c.getResources().getColor(R.color.white));
         }
     }

     public static boolean isNetworkAvailable(Context context) {
         if (context != null) {
             ConnectivityManager connMgr = (ConnectivityManager) context
                     .getSystemService(Context.CONNECTIVITY_SERVICE);
             NetworkInfo wifiNetwork = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
             if (wifiNetwork != null && wifiNetwork.isConnected()) {
                 return true;
             }
             NetworkInfo mobileNetwork = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
             if (mobileNetwork != null && mobileNetwork.isConnected()) {
                 return true;
             }
         }
         return false;
     }

     public static void setStringPreference(Context cxt, String key, String value, String prefName) {
         SharedPreferences.Editor editor = getPrefsEditor(cxt, prefName);
         editor.putString(key, value);
         editor.apply();
     }

     private static SharedPreferences.Editor getPrefsEditor(Context context, String pref) {
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

     /* public static boolean isValidEmail(String email) {
         return !TextUtils.isEmpty(email) && android.util.Patterns
                 .EMAIL_ADDRESS.matcher(email).matches();
     }*/

     public static String getAndroidId(Context context) {
         String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

         Log.d("Android", "Android ID : " + android_id);

         return android_id;
     }

     public static boolean checkForLocationPermission(final Context context, int reqCode) {
         int currentAPIVersion = Build.VERSION.SDK_INT;
         if (currentAPIVersion >= Build.VERSION_CODES.M) {
             if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                     && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                 if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_FINE_LOCATION)
                         && ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_COARSE_LOCATION)) {

                     ActivityCompat.requestPermissions((Activity) context,
                             new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                     Manifest.permission.ACCESS_COARSE_LOCATION}, reqCode);

                 } else {
                     ActivityCompat.requestPermissions((Activity) context,
                             new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                     Manifest.permission.ACCESS_COARSE_LOCATION}, reqCode);
                 }
                 return false;
             } else {
                 return true;
             }
         } else {
             return true;
         }
     }

     public static boolean checkPermission(final Context context) {
         int currentAPIVersion = Build.VERSION.SDK_INT;
         if (currentAPIVersion >= Build.VERSION_CODES.M) {
             if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                     && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                     &&  ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                 if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_EXTERNAL_STORAGE)
                         && ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                         && ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.CAMERA)
                 ) {

                     ActivityCompat.requestPermissions((Activity) context,
                             new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA},
                             103);

                 } else {
                     ActivityCompat.requestPermissions((Activity) context,
                             new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                     Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA}, 103);
                 }
                 return false;
             } else {
                 return true;
             }
         } else {
             return true;
         }
     }

     public static boolean isNullOrEmpty(String pStr) {
         return pStr == null || pStr.trim().length() == 0 || pStr.trim().equalsIgnoreCase("null");
     }

     /*public static void showSnackBar(final View layout, final Context context, final String msg,
                                     final int length, final int color) {
         Snackbar snackbar = Snackbar.make(layout, msg, length);
         snackbar.getView().setBackgroundColor(color);
         snackbar.show();
     }*/

     /*public boolean checkForLocationPermission(final Context context) {
         int currentAPIVersion = Build.VERSION.SDK_INT;
         if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
             if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                 if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_FINE_LOCATION)) {

                     ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                             105);

                 } else {
                     ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                             105);
                 }
                 return false;
             } else {
                 return true;
             }
         } else {
             return true;
         }
     }*/
 }
