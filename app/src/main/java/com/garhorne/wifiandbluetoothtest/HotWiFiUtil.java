package com.garhorne.wifiandbluetoothtest;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.content.Context.WIFI_SERVICE;

public class HotWiFiUtil {

    private WifiManager mWifiManager;

    private WifiConfiguration wifiConfiguration;

    private ConnectivityManager mConnectivityManager;

    private final static String TAG = "HOTWIFITAG";

    private String ssid;
    private String key;

    private Context mContext;

    public HotWiFiUtil(Context context){
        mContext = context;
    }

    class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 2018:
                    wifiConfiguration = (WifiConfiguration)msg.obj;
                    break;
                default:
                    break;
            }
        }
    }

    private MyHandler myHandler = new MyHandler();

    public WifiConfiguration getWifiConfiguration() {
        return wifiConfiguration;
    }

    public void turnOnWifiAp(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Log.d(TAG,">=O");
            //startTethering();
            secondWay();
        }else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N){
            Log.d(TAG,"<=N");
            createAp(true);
        }else {
            Toast.makeText(mContext,"该版本尚未适配",Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isWifiApOpen(Context context) {
        try {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            //通过放射获取 getWifiApState()方法
            Method method = manager.getClass().getDeclaredMethod("getWifiApState");
            //调用getWifiApState() ，获取返回值
            int state = (int) method.invoke(manager);
            //通过放射获取 WIFI_AP的开启状态属性
            Field field = manager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED");
            //获取属性值
            int value = (int) field.get(manager);
            //判断是否开启
            if (state == value) {
                return true;
            } else {
                return false;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return false;
    }

    //7.0开启热点
    public boolean createAp(boolean isOpen) {
        Log.d(TAG,"createAp");
        StringBuffer sb = new StringBuffer();
        try {
            mWifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
            if (mWifiManager.isWifiEnabled()) {
                mWifiManager.setWifiEnabled(false);
            }
            sb.append(1);
            WifiConfiguration netConfig = new WifiConfiguration();
            netConfig.SSID = "xiaomeng";
            netConfig.preSharedKey = "11111111";
            Log.d(TAG, "WifiPresenter：createAp----->netConfig.SSID:"
                    + netConfig.SSID + ",netConfig.preSharedKey:" + netConfig.preSharedKey + ",isOpen=" + isOpen);
            netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            sb.append(2);
            if (isOpen) {
                netConfig.allowedKeyManagement.set(4);
                sb.append(3);
            } else {
                netConfig.allowedKeyManagement.set(4);
                sb.append(4);
            }
            netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            sb.append(5);

            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            sb.append(9);
            return (boolean) method.invoke(mWifiManager, netConfig, true);


        } catch (NoSuchMethodException e) {
            sb.append(10 + (e.getMessage()));
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            sb.append(11 + (e.getMessage()));
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            sb.append(12 + (e.getMessage()));
            e.printStackTrace();
        }
        //log.setText(sb.toString());

        return false;
    }

    //8.0以后的第二种方法
    public void secondWay(){
        mWifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @TargetApi(Build.VERSION_CODES.O)
                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    WifiConfiguration wifiConfiguration = reservation.getWifiConfiguration();
                    ssid = wifiConfiguration.SSID;
                    key = wifiConfiguration.preSharedKey;
                    //可以通过config拿到开启热点的账号和密码
                    myHandler.obtainMessage(2018, wifiConfiguration).sendToTarget();
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                }

            }, myHandler);
        }

    }

    //8.0以后的
    public void startTethering() {
        Log.d(TAG,"startTethering");
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mWifiManager != null) {
            int wifiState = mWifiManager.getWifiState();
            boolean isWifiEnabled = ((wifiState == WifiManager.WIFI_STATE_ENABLED) || (wifiState == WifiManager.WIFI_STATE_ENABLING));
            if (isWifiEnabled)
                mWifiManager.setWifiEnabled(false);
        }
        if (mConnectivityManager != null) {
            try {
                Field internalConnectivityManagerField = ConnectivityManager.class.getDeclaredField("mService");
                internalConnectivityManagerField.setAccessible(true);
                WifiConfiguration apConfig = new WifiConfiguration();
                apConfig.SSID = "cuieney";
                apConfig.preSharedKey = "12121212";

                StringBuffer sb = new StringBuffer();
                Class internalConnectivityManagerClass = Class.forName("android.net.IConnectivityManager");
                ResultReceiver dummyResultReceiver = new ResultReceiver(null);
                try {

                    WifiManager wifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
                    Method mMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
                    mMethod.invoke(wifiManager, apConfig);
                    Method startTetheringMethod = internalConnectivityManagerClass.getDeclaredMethod("startTethering",
                            int.class,
                            ResultReceiver.class,
                            boolean.class);

                    startTetheringMethod.invoke(internalConnectivityManagerClass,
                            0,
                            dummyResultReceiver,
                            true);
                } catch (NoSuchMethodException e) {
                    Method startTetheringMethod = internalConnectivityManagerClass.getDeclaredMethod("startTethering",
                            int.class,
                            ResultReceiver.class,
                            boolean.class,
                            String.class);

                    startTetheringMethod.invoke(internalConnectivityManagerClass,
                            0,
                            dummyResultReceiver,
                            false,
                            mContext.getPackageName());
                } catch (InvocationTargetException e) {
                    //sb.append(11 + (e.getMessage()));
                    e.printStackTrace();
                    secondWay();
                } finally {
                    //log.setText(sb.toString());
                }


            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }
}
