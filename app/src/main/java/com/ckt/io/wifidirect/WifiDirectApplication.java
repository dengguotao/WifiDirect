package com.ckt.io.wifidirect;

import android.app.Application;
import android.content.Intent;

import com.ckt.io.wifidirect.p2p.WiFiP2pState;

/**
 * Created by guotao.deng on 2016/7/28.
 */
public class WifiDirectApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WiFiP2pState.getInstance(getApplicationContext());
        Intent intent = new Intent(this.getApplicationContext(), WifiDirectService.class);
        startService(intent);
    }

}
