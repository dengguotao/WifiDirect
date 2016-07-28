package com.ckt.io.wifidirect;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by guotao.deng on 2016/7/28.
 */
public class WifiDirectService extends Service {

    private static final String TAG = "WifiDirectService";

    private Object mLock = new Object();

    private UpdateTask mUpdateTask;

    private ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateFromProvider();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //maybe will use, but not now.
        return null;
    }

    @Override
    public void onCreate() {
        getContentResolver().registerContentObserver(Constants.InstanceColumns.CONTENT_URI,
                true, contentObserver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(contentObserver);
    }

    private void updateFromProvider() {
        synchronized (mLock) {
            if (mUpdateTask == null) {
                mUpdateTask = new UpdateTask();
                mUpdateTask.run();
            }
        }
    }

    private class UpdateTask extends Thread {

        @Override
        public void run() {
            super.run();
        }
    }
}
