package com.ckt.io.wifidirect;

import android.app.Application;
import android.content.Intent;

/**
 * Created by guotao.deng on 2016/7/28.
 */
public class WifiDirectApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WifiP2pState.newInstance(getApplicationContext());
        Intent intent = new Intent(this.getApplicationContext(), WifiDirectService.class);
        startService(intent);
    }

}
