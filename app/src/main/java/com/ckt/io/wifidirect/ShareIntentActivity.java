package com.ckt.io.wifidirect;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ckt.io.wifidirect.p2p.WifiP2pState;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ShareIntentActivity extends BaseActivity {

    public static final String TAG = "xlj";
    private WifiP2pState mWifiP2pState;
    private Context mContext;
    private WifiP2pDevice mWifiP2pDevice;
    private  Collection<WifiP2pDevice> mWifiP2pDeviceList;
    private ListView mListView;
    private ArrayAdapter<String> mWifiP2pDeviceAdapter;
    private WifiP2pState.ConnectedDeviceInfo mConnectedDeviceInfo;
    private Intent intent;
    private String action;
    private ContentResolver mContentResolver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_intent);

        mContext = getApplicationContext();
        intent = getIntent();
        action = intent.getAction();
        Log.d(TAG, "action = " + action);
        mContentResolver = mContext.getContentResolver();
        mWifiP2pState = WifiP2pState.getInstance(mContext);
        mListView = (ListView) findViewById(R.id.sharelist);
        mWifiP2pDeviceAdapter = new ArrayAdapter<String>(this,R.layout.devicelist);


        mListView.setAdapter(mWifiP2pDeviceAdapter);
        mListView.setOnItemClickListener(new OnItemClick());
        mWifiP2pState.registerOnP2pChangeListenerListener(new mOnP2pChangeListener());
        mWifiP2pState.registerOnConnectChangeListener(new mOnConnectStateChangeListener());

        if (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE)) {
            if (!mWifiP2pState.isConnected()) {
                mWifiP2pState.discoverDevice();
            } else {
                mConnectedDeviceInfo = mWifiP2pState.getConnectedDeviceInfo();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWifiP2pDeviceAdapter.clear();
    }

    private final class OnItemClick implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG,"onItemClick....   " + mWifiP2pDeviceList.isEmpty());
            if (!mWifiP2pDeviceList.isEmpty()) {
                Iterator iterator = mWifiP2pDeviceList.iterator();
                for (int i = 0; iterator.hasNext() && i <= position; i++) {
                    mWifiP2pDevice = (WifiP2pDevice)iterator.next();
                }
                Log.d(TAG,mWifiP2pDevice.toString());
                mWifiP2pState.connectDevice(mWifiP2pDevice, new mActionListener());

            }
        }
    }
    private void startShareIntent() {
        Log.d(TAG, "action = " + action);
        if (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE)) {
            final String type = intent.getType();
            final Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            CharSequence extra_text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (uri != null && type != null) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String path;
                        if (uri.getScheme() != null && uri.getScheme().toString().equals("file:")) {
                            path = uri.getPath();
                        } else {
                            String[] proj = {MediaStore.Images.Media.DATA};
                            Cursor mCursor = mContentResolver.query(uri, proj, null, null, null, null);
                            int column_index = mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                            mCursor.moveToFirst();
                            path =mCursor.getString(column_index);
                        }
                        Log.d(TAG, "path =" + path);
                        String name = path.substring(path.lastIndexOf("/") + 1, path.length());
                        TransferFileInfo mTransferFileInfo = new TransferFileInfo(-1, path, name, 0,
                                Constants.State.STATE_IDEL, 0, Constants.DIRECTION_OUT,
                                mConnectedDeviceInfo.connectedDevice.deviceAddress, mContentResolver);
                        mTransferFileInfo.insert();
                    }
                });
                thread.start();
            }
        }
    }

    private class mActionListener implements WifiP2pManager.ActionListener {
        @Override
        public void onFailure(int reason) {
            Log.d(TAG, "onFailure");
        }

        @Override
        public void onSuccess() {
            Log.d(TAG, "onSuccess");
        }
    }

    private class mOnP2pChangeListener implements WifiP2pState.OnP2pChangeListener {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            Log.d(TAG, peers.toString());
            mWifiP2pDeviceList = peers.getDeviceList();
            for (WifiP2pDevice device : mWifiP2pDeviceList) {
                String name = device.deviceName;
                mWifiP2pDeviceAdapter.add(name);
                mWifiP2pDeviceAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {

        }
    }

    private class mOnConnectStateChangeListener implements WifiP2pState.OnConnectStateChangeListener {
        @Override
        public void onConnected(WifiP2pState.ConnectedDeviceInfo connectedDeviceInfo) {
            Log.d(TAG,"onConnected");
            mConnectedDeviceInfo = connectedDeviceInfo;
            Toast.makeText(mContext, mConnectedDeviceInfo.connectedDevice.deviceName,Toast.LENGTH_SHORT).show();
            startShareIntent();
        }

        @Override
        public void onDisConnected() {
            Log.d(TAG,"onDisConnected");

        }
    }

}
