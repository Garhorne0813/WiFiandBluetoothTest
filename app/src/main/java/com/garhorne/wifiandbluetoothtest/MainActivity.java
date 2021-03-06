package com.garhorne.wifiandbluetoothtest;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements PermissionInterface, View.OnClickListener {

    private Button btn_Search;
    private Button btn_Send;
    private EditText et_input;
    private TextView textView;
    private ListView listView;

    private BluetoothAdapter mBluetoothAdapter;

    private PermissionHelper mPermissionHelper;

    private HotWiFiUtil mHotWiFiUtil;

    private static final int REQ_CODE_LOCATE = 1;
    private static final int REQ_CODE_FINE_LOCATE = 2;

    private final static String TAG = "WAB";

    private UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static BluetoothSocket socket = null;

    private List<BluetoothDevice> deviceList = new ArrayList<>();

    private DeviceAdapter deviceAdapter;

    private Set<BluetoothDevice> deviceBeanSet = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPermissionHelper = new PermissionHelper(this,this);
        mPermissionHelper.requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION, REQ_CODE_FINE_LOCATE);
        mPermissionHelper.requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, REQ_CODE_LOCATE);

        mHotWiFiUtil = new HotWiFiUtil(this);
        //mHotWiFiUtil.turnOnWifiAp();

        //turnOnHotWifi();
        initViews();
        RegReceiver();
        turnOnBluetooth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mHotWiFiUtil != null && !mHotWiFiUtil.isWifiApOpen(this)){
            mHotWiFiUtil.turnOnWifiAp();
        }
        //turnOnHotWifi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //turnOffBluetooth();
        unregisterReceiver(mReceiver);
    }

    private void initViews() {
        btn_Search = (Button)findViewById(R.id.btn_search);
        btn_Send = (Button)findViewById(R.id.btn_send);
        et_input = (EditText)findViewById(R.id.text_input);
        textView = (TextView)findViewById(R.id.textview);
        listView = (ListView)findViewById(R.id.listview);

        btn_Search.setOnClickListener(this);
        btn_Send.setOnClickListener(this);

        deviceAdapter = new DeviceAdapter(MainActivity.this,R.layout.device_item,deviceList);
        listView.setAdapter(deviceAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice device = deviceList.get(i);
                Toast.makeText(getApplicationContext(),device.getName() + ", " + device.getAddress(),Toast.LENGTH_SHORT).show();
                new Thread(new ClientThread(device)).start();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_search:
                deviceBeanSet.clear();
                deviceList.clear();
                //deviceAdapter.notifyDataSetChanged();
                mBluetoothAdapter.startDiscovery();
                break;
            case R.id.btn_send:
                String msg = et_input.getText().toString();
                if (!msg.equals("")){
                    sendMsgThroughBluetooth(msg);
                }else {
                    et_input.setError("请输入发送的字符串");
                }
                break;
            default:
                break;
        }
    }

    private void turnOnBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null){
            if (!mBluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 100);
            } else {
                mBluetoothAdapter.startDiscovery();
                //Log.d(TAG,"start discovery");
            }
        }else {
            Toast.makeText(this,"该设备不支持蓝牙",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100){
            if (resultCode == Activity.RESULT_OK){
                mBluetoothAdapter.startDiscovery();
                Log.d(TAG,"蓝牙打开成功");
            }else if (resultCode == Activity.RESULT_CANCELED){
                Log.d(TAG,"蓝牙打开失败");
            }else {
                Log.d(TAG,"蓝牙异常");
            }
        }
    }

    private void turnOffBluetooth(){
        mBluetoothAdapter.disable();
    }

    private void RegReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver,filter);
    }

    @Override
    public void requestPermissionSuccess(int callBackCode) {

    }

    @Override
    public void requestPermissionFailure(int callBackCode) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionHelper.requestPermissionResult(requestCode, permissions, grantResults);
    }

    private void sendMsgThroughBluetooth(String msg){
        Log.d(TAG,"发送的消息为：" + msg);
        if (socket != null){
            try{
                textView.setText("正在发送");
                OutputStream os = socket.getOutputStream();
                os.write(msg.getBytes());
                os.flush();
                textView.setText("消息已发送成功");
                Log.d(TAG,"消息已发送成功");
            } catch (IOException e) {
                e.printStackTrace();
                textView.setText("消息发送失败");
                Log.d(TAG,"消息发送失败");
            }
        }else {
            textView.setText("未建立连接");
            Log.d(TAG,"未建立连接");
        }
    }



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"action: " + action);
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.d(TAG,"开始扫描...");
                textView.setText("正在扫描");
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                Log.d(TAG,"发现设备");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Log.d(TAG,"发现设备");
                if (device != null){
                    String deviceName = device.getName();
                    //Log.d(TAG,deviceName!=null?deviceName:"null" + ", " + device.getAddress());
                    if (deviceName == null){
                        deviceName = "null";
                    }
                    Log.d(TAG,deviceName + ", " + device.getAddress() + ", " + device.getBondState());
                    //DeviceBean deviceBean = new DeviceBean(deviceName,device.getAddress());
                    deviceBeanSet.add(device);
                    deviceList.clear();
                    deviceList.addAll(deviceBeanSet);
                    deviceAdapter.notifyDataSetChanged();
                    //deviceList.add(deviceBean);
                    //deviceAdapter.notifyDataSetChanged();
                    try {
                        if (deviceName!= null &&(device.getName().equals("DESKTOP-GRGAC8C") || device.getName().equals("刘家瀚的 iPhone"))){
                            Log.d(TAG,"发现目标手机");
                            new Thread(new ClientThread(device)).start();
                            mBluetoothAdapter.cancelDiscovery();
                        }
                    }catch (NullPointerException e){
                        Log.d(TAG,e.toString());
                    }
                }
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                textView.setText("扫描完成");
                Log.d(TAG,"扫描结束");
            }
        }
    };

    private class ClientThread extends Thread{
        private BluetoothDevice device;

        public ClientThread(BluetoothDevice device){
            this.device = device;
        }

        @Override
        public void run() {
            try{
                socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_SECURE);
                Log.d(TAG,"连接服务端...");
                socket.connect();
                Log.d(TAG,"连接建立");
            } catch (IOException e) {
                Log.d(TAG,"连接失败");
                e.printStackTrace();
                Log.d(TAG,"重新连接");
                try {
                    Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                    socket = (BluetoothSocket) m.invoke(device, 1);
                    socket.connect();
                    Log.d(TAG,"连接建立");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e1) {
                    Log.e("TAG",e1.toString());
                    try{
                        socket.close();
                    }catch (IOException ie){
                        ie.printStackTrace();
                    }
                }
            }
        }
    }
}
