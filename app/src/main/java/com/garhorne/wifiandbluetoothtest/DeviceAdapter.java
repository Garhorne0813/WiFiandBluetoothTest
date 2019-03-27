package com.garhorne.wifiandbluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class DeviceAdapter extends ArrayAdapter {

    private int resourceid;

    public DeviceAdapter(@NonNull Context context, int resource, @NonNull List<BluetoothDevice> list) {
        super(context, resource, list);
        resourceid = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //DeviceBean deviceBean = (DeviceBean) getItem(position);
        BluetoothDevice device = (BluetoothDevice)getItem(position);
        View view;
        ViewHolder viewHolder;
        if (convertView == null){
            view = LayoutInflater.from(getContext()).inflate(resourceid,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.deviceName = (TextView)view.findViewById(R.id.device_name);
            viewHolder.deviceAddress = (TextView)view.findViewById(R.id.device_address);
            viewHolder.deviceState = (TextView)view.findViewById(R.id.device_state);
            view.setTag(viewHolder);
        }else {
            view = convertView;
            viewHolder = (ViewHolder)view.getTag();
        }
        if (device != null) {
            if (device.getName() == null){
                viewHolder.deviceName.setText("null");
            }else {
                viewHolder.deviceName.setText(device.getName());
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED){
                viewHolder.deviceState.setText("已连接");
            }else {
                viewHolder.deviceState.setText("");
            }
        }
        return view;
    }

    class ViewHolder{
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceState;
    }
}
