package com.garhorne.wifiandbluetoothtest;

public interface PermissionInterface {

    void requestPermissionSuccess(int callBackCode);

    void requestPermissionFailure(int callBackCode);

}
