package com.easy.transfer;

import android.app.Application;
import android.content.Intent;

import com.easy.transfer.p2p.WifiP2pState;

/**
 * Created by guotao.deng on 2016/7/28.
 */
public class WifiDirectApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WifiP2pState.getInstance(getApplicationContext());
        Intent intent = new Intent(this.getApplicationContext(), WifiDirectService.class);
        startService(intent);
    }

}
