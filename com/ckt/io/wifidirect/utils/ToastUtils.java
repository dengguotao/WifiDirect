package com.easy.transfer.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import com.ckt.io.transfer.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by admin on 2016/3/7.
 */
public class ToastUtils {
    private static Toast mToast;
    public static void toast(Context context, int msg_id) {
        if(mToast == null) {
            mToast = Toast.makeText(context, msg_id, Toast.LENGTH_SHORT);
        }else {
            mToast.setText(context.getResources().getString(msg_id));
        }
        mToast.show();
    }

    public static void toast(Context context, String msg) {
        if(mToast == null) {
            mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        }else {
            mToast.setText(msg);
        }
        mToast.show();
    }
}
