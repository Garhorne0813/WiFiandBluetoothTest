package com.garhorne.wifiandbluetoothtest;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class PermissionHelper {

    private Activity mActivity;
    private PermissionInterface mPermissionInterface;
    private String Permission;
    private int callBallCode;

    public PermissionHelper(Activity activity,PermissionInterface permissionInterface){
        mActivity = activity;
        mPermissionInterface = permissionInterface;
    }

    public static boolean hasPermission(Context context,String permission){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    public void requestPermissions(String permission,int callBackCode){
        this.Permission = permission;
        this.callBallCode = callBackCode;
        if (hasPermission(mActivity,permission)){
            mPermissionInterface.requestPermissionSuccess(callBackCode);
        } else {
            ActivityCompat.requestPermissions(mActivity,new String[]{permission},callBackCode);
        }
    }

    public void requestPermissionResult(int requestCode,String[] permission,int[] grantResults){
        if (requestCode == callBallCode){
            for (int result:grantResults){
                if (result == PackageManager.PERMISSION_GRANTED){
                    mPermissionInterface.requestPermissionSuccess(callBallCode);
                }else {
                    mPermissionInterface.requestPermissionFailure(callBallCode);
                }
            }
        }
    }
}
