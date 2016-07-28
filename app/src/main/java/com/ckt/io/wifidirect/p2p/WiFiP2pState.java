package com.ckt.io.wifidirect.p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.ckt.io.wifidirect.utils.LogUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by admin on 2016/7/28.
 */
public class WiFiP2pState extends BroadcastReceiver implements
        WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.PeerListListener, WifiP2pManager.GroupInfoListener{
    public static final String TAG = "WiFiP2pState";
    private Context context;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    private ArrayList<WifiP2pDevice> deviceList;

    private ConnectedDeviceInfo connectedDeviceInfo;


    private static WiFiP2pState instance = null;
    public static WiFiP2pState getInstance(Context context) {
        if(instance == null) {
            instance = new WiFiP2pState(context);
        }
        return instance;
    }

    private WiFiP2pState(Context context) {
        this.context = context;
        manager = (WifiP2pManager) context
                .getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, context.getMainLooper(), null);
        deviceList = new ArrayList<WifiP2pDevice>();

        //×¢²á¼àÌý
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {//wifi-p2p on
                // Wifi-p2p mode is enabled

            } else { //wifi-p2p off

            }
            Log.d(TAG, "P2P state changed - " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                manager.requestPeers(channel, this);
            }
            Log.d(TAG, "P2P peers changed");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection info
                //to find group owner IP
                connectedDeviceInfo = new ConnectedDeviceInfo();
                manager.requestConnectionInfo(channel, this);
                manager.requestGroupInfo(channel, this);
                Log.d(TAG, "device Connected!!--->requestConnectionInfo()");
            } else {
                // It's a disconnect
                connectedDeviceInfo = null;
                Log.d(TAG, "device disconnected!!)");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {

        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        connectedDeviceInfo.connectInfo = info;
        if(!info.isGroupOwner) { //the client
            connectedDeviceInfo.connectedDeviceAddr = info.groupOwnerAddress;
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
//        deviceList = (ArrayList<WifiP2pDevice>) peers.getDeviceList();
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if(group == null || !isConnected()) return;
        for(WifiP2pDevice device : group.getClientList()) {
            if(device.status == WifiP2pDevice.CONNECTED) {
                connectedDeviceInfo.connectedDevice = device;
                break;
            }
        }

        if (group.isGroupOwner()) { //groupOwner
            connectedDeviceInfo.connectedDeviceAddr = getInterfaceAddress(group);
            LogUtils.d(TAG, "Connected device ip:" + connectedDeviceInfo.connectedDeviceAddr.toString());
        } else {//client
            if (connectedDeviceInfo.connectInfo != null) {
                connectedDeviceInfo.connectedDeviceAddr = connectedDeviceInfo.connectInfo.groupOwnerAddress;
            }
        }
    }

    private static Inet4Address getInterfaceAddress(WifiP2pGroup info) {
        NetworkInterface iface;
        try {
            iface = NetworkInterface.getByName(info.getInterface());
        } catch (SocketException ex) {
            return null;
        }

        Enumeration<InetAddress> addrs = iface.getInetAddresses();
        while (addrs.hasMoreElements()) {
            InetAddress addr = addrs.nextElement();
            if (addr instanceof Inet4Address) {
                return (Inet4Address)addr;
            }
        }
        return null;
    }

    public ConnectedDeviceInfo getConnectedDeviceInfo() {
        return connectedDeviceInfo;
    }

    public boolean isConnected() {
        return connectedDeviceInfo != null;
    }

    public void destory() {
        context.unregisterReceiver(this);
    }

    class ConnectedDeviceInfo {
        public WifiP2pInfo connectInfo;
        public WifiP2pDevice connectedDevice;
        public InetAddress connectedDeviceAddr;
    }
}
