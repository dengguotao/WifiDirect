package com.ckt.io.wifidirect;

import android.app.Application;
import android.content.Intent;

import com.ckt.io.wifidirect.p2p.WifiP2pState;
import com.ckt.io.wifidirect.p2p.WifiTransferManager;

/**
 * Created by guotao.deng on 2016/7/28.
 */
public class WifiDirectApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this.getApplicationContext(), WifiDirectService.class);
        startService(intent);
    }
}
