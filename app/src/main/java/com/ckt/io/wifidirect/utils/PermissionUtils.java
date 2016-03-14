package com.ckt.io.wifidirect.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by admin on 2016/3/14.
 */
public class PermissionUtils {
    //check if we have the specaial permisson on 6.0 device
    //return true-->if we have the permisson
    //or else return false.
    public static boolean checkPermissionOnAndroidM(Activity activity, String permisson, int reqeustCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(activity, permisson);
            if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                //do not have the permisson, request it now, and handle the result in activity's method
                //onRequestPermissionsResult
                ActivityCompat.requestPermissions(activity, new String[]{permisson}, reqeustCode);
                return false;
            }
        }
        return true;
    }
}
