package com.ckt.io.wifidirect.p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.ckt.io.wifidirect.Constants;
import com.ckt.io.wifidirect.utils.LogUtils;
import com.ckt.io.wifidirect.utils.NetworksUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by admin on 2016/7/28.
 */
public class WifiP2pState extends BroadcastReceiver implements
        WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.PeerListListener, WifiP2pManager.GroupInfoListener {
    public static final String TAG = "WiFiP2pState";
    private Context context;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    private ArrayList<WifiP2pDevice> deviceList;

    private ConnectedDeviceInfo connectedDeviceInfo;

    public WifiTransferManager wifiTransferManager;

    private static WifiP2pState instance = null;

    private WifiP2pDevice mThisDevice;

    private List<OnConnectStateChangeListener> listeners = new ArrayList<>();
    private List<OnP2pChangeListener> onP2pChangeListenerList = new ArrayList<>();

    private boolean mPendingDiscover;

    public static WifiP2pState getInstance(Context context) {
        if (instance == null) {
            instance = new WifiP2pState(context);
        }
        return instance;
    }

    private WifiP2pState(Context context) {
        this.context = context;
        manager = (WifiP2pManager) context
                .getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, context.getMainLooper(), null);
        deviceList = new ArrayList<WifiP2pDevice>();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(this, intentFilter);
    }

    private boolean isWifiOn() {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    private void toggleWifi(boolean isOpen) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(isOpen);
    }

    public void discoverDevice() {
        LogUtils.d(TAG, "discoverDevice()");
        if (!isWifiOn()) {
            toggleWifi(true);
            mPendingDiscover = true;
        } else {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int reasonCode) {
                    LogUtils.d(TAG, "WifiP2pHelper-->discoverDevice failed   reasonCode=" + reasonCode);
                }
            });
        }
    }

    public void connectDevice(WifiP2pDevice device, WifiP2pManager.ActionListener listener) {
        LogUtils.d(TAG, "connect device " + device.deviceAddress);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        manager.connect(channel, config, listener);
    }

    public void registerOnConnectChangeListener(OnConnectStateChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void registerOnP2pChangeListenerListener(OnP2pChangeListener listener) {
        if (listener != null && !onP2pChangeListenerList.contains(listener)) {
            onP2pChangeListenerList.add(listener);
        }
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
        } else {
            if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                if (manager == null) {
                    manager = (WifiP2pManager) context
                            .getSystemService(Context.WIFI_P2P_SERVICE);
                    channel = manager.initialize(context, context.getMainLooper(), null);
                }

                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    // we are connected with the other device, request connection info
                    //to find group owner IP
                    connectedDeviceInfo = new ConnectedDeviceInfo();
                    manager.requestGroupInfo(channel, this);
                    Log.d(TAG, "device Connected!!--->requestConnectionInfo()");
                } else {
                    // It's a disconnect
                    connectedDeviceInfo = null;

                    for (OnConnectStateChangeListener listener : listeners) {
                        listener.onDisConnected();
                    }

                    Log.d(TAG, "device disconnected!!)");
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                mThisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                if (isWifiOn() && mPendingDiscover) {
                    mPendingDiscover = false;
                    manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure(int reason) {
                            LogUtils.e(TAG, "discover fail");
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        LogUtils.d(TAG, "onConnectionInfoAvailable  isGroupOwner:" + info.isGroupOwner);
        connectedDeviceInfo.connectInfo = info;
        if (!info.isGroupOwner) { //the client
            connectedDeviceInfo.connectedDeviceAddr = info.groupOwnerAddress;
        }

        if (info.isGroupOwner) { //groupOwner
            /*connectedDeviceInfo.connectedDeviceAddr =
                    NetworksUtils.getPeerIp(connectedDeviceInfo.group);*/
            int retryTime = 50;
            String ip = null;
            while (retryTime >= 0) {
                for (WifiP2pDevice device : connectedDeviceInfo.group.getClientList()) {
                    ip = NetworksUtils.getPeerIP(device.deviceAddress);
                }
                if (ip != null) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryTime--;
            }
            if (ip != null) {
                try {
                    connectedDeviceInfo.connectedDeviceAddr = InetAddress.getByName(ip);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            } else {
                LogUtils.d(TAG, "GroupOwner get client ip failed!");
                return;
            }
            LogUtils.d(TAG, "getPeerIp-------->" + connectedDeviceInfo.connectedDeviceAddr);
        } else {//client
            if (connectedDeviceInfo.connectInfo != null) {
                connectedDeviceInfo.connectedDeviceAddr = connectedDeviceInfo.connectInfo.groupOwnerAddress;
            }
        }

        for (OnConnectStateChangeListener listener : listeners) {
            listener.onConnected(connectedDeviceInfo);
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
//        deviceList = (ArrayList<WifiP2pDevice>) peers.getDeviceList();
        for (OnP2pChangeListener l : onP2pChangeListenerList) {
            l.onPeersAvailable(peers);
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        LogUtils.d(TAG, "onGroupInfoAvailable--->" + group);
        if (group == null) return;
        connectedDeviceInfo.group = group;
        if (group.isGroupOwner()) {
            for (WifiP2pDevice device : group.getClientList()) {
                LogUtils.d(TAG, "clientList--->" + device);
                if (device.status == WifiP2pDevice.CONNECTED) {
                    connectedDeviceInfo.connectedDevice = device;
                    break;
                }
            }
        } else {
            connectedDeviceInfo.connectedDevice = group.getOwner();
        }


        manager.requestConnectionInfo(channel, this);

        for (OnP2pChangeListener l : onP2pChangeListenerList) {
            l.onGroupInfoAvailable(group);
        }

        /*Object obj = context.getSystemService("network_management");
        try {
            Method method = obj.getClass().getMethod("getRoutes");
            RouteInfo ret [] = (RouteInfo[]) method.invoke(obj, group.getInterface());
            LogUtils.d(TAG, "routeInfo:" + ret);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    private static byte[] getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
                            return inetAddress.getAddress();
                        }
                        //return inetAddress.getHostAddress().toString(); // Galaxy Nexus returns IPv6
                    }
                }
            }
        } catch (SocketException ex) {
            //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        } catch (NullPointerException ex) {
            //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        }
        return null;
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
                LogUtils.d(TAG, "getInterfaceAddress--> addr=" + addr.toString());
//                return (Inet4Address)addr;
            }
        }
        return null;
    }

    public ConnectedDeviceInfo getConnectedDeviceInfo() {
        return connectedDeviceInfo;
    }

    public boolean isConnected() {
        return connectedDeviceInfo != null && connectedDeviceInfo.connectedDevice != null;
    }

    public WifiP2pDevice getThisDevice() {
        return mThisDevice;
    }

    private static void destory() {
        try {
            instance.context.unregisterReceiver(instance);
        } catch (Exception e) {
        }
    }

    public static void relase() {
        destory();
        instance = null;
    }

    public class ConnectedDeviceInfo {
        public WifiP2pInfo connectInfo;
        public WifiP2pDevice connectedDevice;
        public InetAddress connectedDeviceAddr;
        public WifiP2pGroup group;
    }

    public interface OnConnectStateChangeListener {
        public void onConnected(ConnectedDeviceInfo connectedDeviceInfo);

        public void onDisConnected();
    }

    public interface OnP2pChangeListener {
        public void onPeersAvailable(WifiP2pDeviceList peers);

        public void onGroupInfoAvailable(WifiP2pGroup group);
    }
}
