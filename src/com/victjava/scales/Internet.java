package com.victjava.scales;

import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.*;

/**
 * Управляет соединениями (Bluetooth, Wi-Fi, мобильная сеть)
 *
 * @author Kostya
 */
public class Internet {
    private final Context context;
    /**
     * Менеджер телефона
     */
    private TelephonyManager telephonyManager;
    /**
     * Слушатель менеджера телефона
     */
    private PhoneStateListener phoneStateListener;

    public static final String INTERNET_CONNECT = "internet_connect";
    public static final String INTERNET_DISCONNECT = "internet_disconnect";

    public Internet(Context c) {
        context = c;
    }

    /**
     * Сделать соединение с интернетом
     */
    public void connect() {
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onDataConnectionStateChanged(int state) {
                switch (state) {
                    case TelephonyManager.DATA_DISCONNECTED:
                        if (telephonyManager != null) {
                            turnOnDataConnection(true);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);


        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            turnOnWiFiConnection(true);
        }
        turnOnDataConnection(true);
    }

    /**
     * Выполнить отсоединение от интернета
     */
    public void disconnect() {
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            telephonyManager = null;
        }

        turnOnDataConnection(false);
        turnOnWiFiConnection(false);
    }

    /**
     * Проверяем подключение к интернету.
     *
     * @return true - есть соединение.
     */
    public static boolean isOnline() {
        try {
            Process p1 = Runtime.getRuntime().exec("ping -c 1 www.google.com");
            int returnVal = p1.waitFor();
            return returnVal == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Выполнить соединение с интернетом по wifi.
     *
     * @param on true - включить.
     */
    public void turnOnWiFiConnection(boolean on) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) {
            return;
        }
        wifi.setWifiEnabled(on);
        while (wifi.isWifiEnabled() != on) ;
    }

    /**
     * Выполнить соединение с интернетом по mobile data.
     *
     * @param on true - включить.
     */
    private boolean turnOnDataConnection(boolean on) {
        try {
            int bv = Build.VERSION.SDK_INT;
            //int bv = Build.VERSION_CODES.FROYO;
            if (bv == Build.VERSION_CODES.FROYO) { //2.2

                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                Class<?> telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                Object ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                Class<?> ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

                Method dataConnSwitchMethod = on ? ITelephonyClass.getDeclaredMethod("enableDataConnectivity") : ITelephonyClass.getDeclaredMethod("disableDataConnectivity");

                dataConnSwitchMethod.setAccessible(true);
                dataConnSwitchMethod.invoke(ITelephonyStub);
            } else if (bv <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                //log.i("App running on Ginger bread+");
                /*final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                final Class<?> conmanClass = Class.forName(conman.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(conman);
                final Class<?> iConnectivityManagerClass =  Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(iConnectivityManager, on);*/

                //Cursor cursor = context.getContentResolver().query(Settings.System.CONTENT_URI, null, null,null, null);
                /*Cursor cursor = context.getContentResolver().query(Settings.System.CONTENT_URI, null, null,null, null);

                ContentQueryMap mQueryMap = new ContentQueryMap(cursor, BaseColumns._ID, true, null);
                Map<String,ContentValues> map = mQueryMap.getRows();
                ContentValues values = map.get(Settings.Secure.DATA_ROAMING);*/

                ConnectivityManager dataManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Method setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(dataManager, on);

                // context.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));

                /*Intent intent=new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                ComponentName cn = new ComponentName("com.android.phone","com.android.phone.Settings");
                intent.setComponent(cn);
                context.startActivity(intent);*/

                //Global.getInt(context.getContentResolver(), "mobile_data");

                //((Activity) context).startActivityForResult(new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS), 0);

                //final  Intent intent=new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                //intent.addCategory(Intent.CATEGORY_LAUNCHER);
                //final ComponentName cn = new ComponentName("com.android.phone","com.android.phone.Settings");
                //intent.setComponent(cn);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //context.startActivity(intent);
            } else {
                ConnectivityManager dataManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                 /*final Method setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                //Method[] setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethods("setMobileDataEnabled");
                setMobileDataEnabledMethod.setAccessible(on);
                setMobileDataEnabledMethod.invoke(dataManager, on);*/

                Method dataMtd = null;
                try {
                    dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
                } catch (SecurityException | NoSuchMethodException e) {
                    e.printStackTrace();
                }

                assert dataMtd != null;
                dataMtd.setAccessible(true);
                try {
                    dataMtd.invoke(dataManager, on);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                /*TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                Method methodSet = Class.forName(tm.getClass().getName()).getDeclaredMethod( "setDataEnabled", Boolean.TYPE);
                methodSet.invoke(tm,on);*/

                /*TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                Method setMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
                if (null != setMobileDataEnabledMethod)      {
                    setMobileDataEnabledMethod.invoke(telephonyService, on);
                }*/

                /*Intent intent=new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                ComponentName cn = new ComponentName("com.android.phone","com.android.phone.Settings");
                intent.setComponent(cn);
                context.startActivity(intent);*/

               /* Method dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
                dataMtd.setAccessible(on);
                dataMtd.invoke((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE), on);*/

                /*Class[] cArg = new Class[2];
                cArg[0] = String.class;
                cArg[1] = Boolean.TYPE;
                Method setMobileDataEnabledMethod;

                setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", cArg);

                Object[] pArg = new Object[2];
                pArg[0] = getContext().getPackageName();
                pArg[1] = true;
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(iConnectivityManager, pArg);*/
            }
            return true;
        } catch (Exception ignored) {
            Log.e("hhh", "error turning on/off data");
            return false;
        }
    }

    /**
     * Послать ссылку в интернет.
     *
     * @param url Ссылка.
     * @return true - ответ ОК.
     */
    protected static boolean send(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();
            if (!(urlConnection instanceof HttpURLConnection)) {
                throw new IOException("URL is not an Http URL");
            }
            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            //connection.setReadTimeout(3000);
            //connection.setConnectTimeout(3000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (MalformedURLException ignored) {
            return false;
        } catch (IOException ignored) {
            return false;
        }
    }

}
