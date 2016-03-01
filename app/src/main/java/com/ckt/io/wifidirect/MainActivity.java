package com.ckt.io.wifidirect;

import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.ckt.io.wifidirect.fragment.ContentFragment;
import com.ckt.io.wifidirect.fragment.DeviceChooseFragment;
import com.ckt.io.wifidirect.fragment.FileExplorerFragment;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;

public class MainActivity extends ActionBarActivity {
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    protected Toolbar toolbar;

    private ContentFragment contentfragment;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pHelper wifiP2pHelper;
    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch(msg.what) {
                case WifiP2pHelper.WIFIP2P_DEVICE_LIST_CHANGED:
                    DeviceChooseFragment fragment = contentfragment.getmDeviceChooseFragment();
                    fragment.updateDeviceList(wifiP2pHelper.getDeviceList());
                    break;
                case WifiP2pHelper.WIFIP2P_DEVICE_CONNECTED_SUCCESS:
                    break;
                case 0:
                    break;
                case 1:
                    break;
            }
        };
    };
    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiP2pHelper = new WifiP2pHelper(this, this.handler);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        contentfragment = (ContentFragment) getSupportFragmentManager().findFragmentById(R.id.id_content);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.id_drawer_layout);
        // 實作 drawer toggle 並放入 toolbar
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wifiP2pHelper, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(wifiP2pHelper);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wifiP2pHelper.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        FileExplorerFragment fileExplorerFragment = contentfragment.getFileExplorerFragment();
        if(fileExplorerFragment != null && fileExplorerFragment.back()) {
            return ;
        }
        super.onBackPressed();
    }

    public WifiP2pHelper getWifiP2pHelper() {
        return wifiP2pHelper;
    }
}
