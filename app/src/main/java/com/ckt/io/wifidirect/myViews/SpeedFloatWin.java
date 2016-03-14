package com.ckt.io.wifidirect.myViews;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.utils.BitmapUtils;


/**
 * Created by admin on 2015/11/11.
 */
public class SpeedFloatWin {

    static boolean isFloatViewAdded = false;
    static boolean isFirstShow = true;
    static int x;
    static int y;
    static View view;

    public static void show(final Context context) {
        if (isFirstShow) { //read the stored position
            SharedPreferences mySharedPreferences = context.getSharedPreferences(
                    "WifiDirect", Activity.MODE_PRIVATE);
            try {
                x = mySharedPreferences.getInt("x", 0);
                y = mySharedPreferences.getInt("y", 0);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
                x = y = 0;
            }
            isFirstShow = false;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        final WindowManager wm = (WindowManager) context.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        // set window type
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            /*
             * if params.type = WindowManager.LayoutParams.TYPE_PHONE; 那么优先级会降低一些,
             * 即拉下通知栏不可见
             */
        params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        // 设置Window flag
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            /*
             * 下面的flags属性的效果形同“锁定”。 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
             * wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL |
             * LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
             */
        // 设置悬浮窗的长得宽
        params.width = (int) BitmapUtils.dipTopx(context, 80);
        params.height = (int) BitmapUtils.dipTopx(context, 60);
        params.x = x;
        params.y = y;
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.float_win_layout, null);
        // 设置悬浮窗的Touch监听
        v.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY;
            int paramX, paramY;

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        paramX = params.x;
                        paramY = params.y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        params.x = paramX + dx;
                        params.y = paramY + dy;
                        // 更新悬浮窗位置
                        wm.updateViewLayout(v, params);
                        x = params.x;
                        y = params.y;
                        break;
                    case MotionEvent.ACTION_UP:

                        break;
                }
                return false;
            }
        });

        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

            }
        });
        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                return true;
            }
        });

        //防止添加多个悬浮窗口
        if (!isFloatViewAdded) {
            wm.addView(v, params);
            view = v;
            isFloatViewAdded = true;
        }
    }

    public static void hide(Context context) {
        final WindowManager wm = (WindowManager) context.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        if (isFloatViewAdded && view != null) {
            wm.removeViewImmediate(view);
        }
        isFloatViewAdded = false;

        //保存悬浮窗口的位置
        SharedPreferences mySharedPreferences = context.getSharedPreferences(
                "WifiDirect", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putInt("x", x);
        editor.putInt("y", y);
        editor.commit();
    }

    public static void updateSpeed(String sendSpeed, String receviceSpeed) {
        if (view == null) return;
        TextView txt_sendSpeed = (TextView) view.findViewById(R.id.txt_speed_send);
        txt_sendSpeed.setText(sendSpeed);
        TextView txt_receiveSpeed = (TextView) view.findViewById(R.id.txt_speed_recevice);
        txt_receiveSpeed.setText(receviceSpeed);
        //wm.updateViewLayout(v, params);
        view.invalidate();
    }
}
