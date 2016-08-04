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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by admin on 2016/7/28.
 */
public class WifiP2pState extends BroadcastReceiver implements
        WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.PeerListListener, WifiP2pManager.GroupInfoListener{
    public static final String TAG = "WiFiP2pState";
    private Context context;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    private ArrayList<WifiP2pDevice> deviceList;

    private ConnectedDeviceInfo connectedDeviceInfo;

    public WifiTransferManager wifiTransferManager;

    private boolean sendClientIpThreadRunning = false;

    private static WifiP2pState instance = null;
    public static WifiP2pState getInstance(Context context) {
        if(instance == null) {
            instance = new WifiP2pState(context);
        }
        return instance;
    }

    private ServerSocket serverSocket;

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
        LogUtils.d(TAG, "onConnectionInfoAvailable  isGroupOwner:"+info.isGroupOwner);
        connectedDeviceInfo.connectInfo = info;
        if(!info.isGroupOwner) { //the client
            connectedDeviceInfo.connectedDeviceAddr = info.groupOwnerAddress;
        }

        if (info.isGroupOwner) { //groupOwner
            connectedDeviceInfo.connectedDeviceAddr = null;
        } else {//client
            if (connectedDeviceInfo.connectInfo != null) {
                connectedDeviceInfo.connectedDeviceAddr = connectedDeviceInfo.connectInfo.groupOwnerAddress;
            }
        }

        wifiTransferManager = new WifiTransferManager(context,
                connectedDeviceInfo.connectedDeviceAddr,
                8080,
                new WifiTransferManager.FileSendStateListener() {
                    @Override
                    public void onStart(int id, String path, long transferedSize) {

                    }

                    @Override
                    public void onUpdate(ArrayList<WifiTransferManager.DataTranferTask> taskList) {
                        for(WifiTransferManager.DataTranferTask task : taskList) {
                            LogUtils.d(TAG, "update Speed:"+task.toString());
                        }
                    }

                    @Override
                    public void onFinished(int id, String path, long transferedSize, boolean ret) {

                    }
                },
                new WifiTransferManager.FileReceiveStateListener() {
                    @Override
                    public void onStart(String path, long transferedSize, long size) {

                    }

                    @Override
                    public void onUpdate(ArrayList<WifiTransferManager.DataTranferTask> taskList) {
                        for(WifiTransferManager.DataTranferTask task:taskList) {
                            LogUtils.d(TAG, "update Speed:"+task.toString());
                        }
                    }

                    @Override
                    public void onFinished(String path, long transferedSize, long size, boolean ret) {

                    }
                },
                new WifiTransferManager.OnGetClientIpListener() {
                    @Override
                    public void onGetClientIp(InetAddress address) {
                        LogUtils.d(TAG, "Group owner get the client addr:" + address.toString());
                        if (connectedDeviceInfo != null) {
                            connectedDeviceInfo.connectedDeviceAddr = address;
                        }
                    }
                },
                new WifiTransferManager.OnSendClientIpResponseListener() {
                    @Override
                    public void onSendClientIpResponse(boolean ret) {
                        sendClientIpThreadRunning = false;
                    }
                });
        /*
        * [five] test
        * */
        new Thread() {
            @Override
            public void run() {
                serverSocket = null;
                try {
                    serverSocket = new ServerSocket(8080);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int reTryTime = 0;
                while(connectedDeviceInfo != null) {
                    try {
                        Socket client = serverSocket.accept();
                        LogUtils.d(TAG, "a new connection!!!");
                        wifiTransferManager.receive(client);
                    } catch (Exception e) {
                        e.printStackTrace();
                        reTryTime ++;
                        if(reTryTime > 5) break;
                        try {
                            if(serverSocket != null) {
                                serverSocket.close();
                            }
                        } catch (Exception e1) {}
                        if(serverSocket == null) {
                            try {
                                serverSocket = new ServerSocket(8080);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                    try {
                        sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        if(!connectedDeviceInfo.connectInfo.isGroupOwner) {

            new Thread() {
                @Override
                public void run() {
                    sendClientIpThreadRunning = true;
                    while (sendClientIpThreadRunning) {
                        wifiTransferManager.sendClientIp();
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
//        deviceList = (ArrayList<WifiP2pDevice>) peers.getDeviceList();
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if(group == null || !isConnected()) return;
        connectedDeviceInfo.group = group;
        for(WifiP2pDevice device : group.getClientList()) {
            if(device.status == WifiP2pDevice.CONNECTED) {
                connectedDeviceInfo.connectedDevice = device;
                break;
            }
        }

        manager.requestConnectionInfo(channel, this);
    }

    private static byte[] getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
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
            if (addr instanceof Inet4Address ) {
                LogUtils.d(TAG, "getInterfaceAddress--> addr="+addr.toString());
//                return (Inet4Address)addr;
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

    private static void destory() {
        try {
            instance.context.unregisterReceiver(instance);
        }catch (Exception e) {}
    }

    public static void relase() {
        destory();
        try {
            instance.serverSocket.close();
        } catch (Exception e) {}
        instance = null;

    }

    class ConnectedDeviceInfo {
        public WifiP2pInfo connectInfo;
        public WifiP2pDevice connectedDevice;
        public InetAddress connectedDeviceAddr;
        public WifiP2pGroup group;
    }
}
