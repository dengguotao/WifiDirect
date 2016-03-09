package com.ckt.io.wifidirect.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.utils.SdcardUtils;


public class WifiP2pHelper extends BroadcastReceiver implements PeerListListener, ConnectionInfoListener {
    public static final String TAG = "WifiDirect";
    public static final int MAX_FILENAME_LEN = 32;
    public static final int MAX_FILESIZE_LEN = 16;
    public static final int SOCKET_PORT = 8989;
    public static final int SOCKET_TIMEOUT = 5000;

    public static final int WIFIP2P_DEVICE_DISCOVERING = 99; //正在发现设备
    public static final int WIFIP2P_DEVICE_LIST_CHANGED = 100;//可以设备列表更新
    public static final int WIFIP2P_DEVICE_CONNECTED_SUCCESS = 101;//连接设备成功
    public static final int WIFIP2P_DEVICE_DISCONNECTED = 102;//链接断开
    private WifiP2pManager manager;
    private Channel channel;
    private ArrayList<WifiP2pDevice> deviceList;
    private WifiP2pInfo connectInfo;
    private boolean isConnected = false;

    // 作为服务socket用来接收对方发送的文件
    private ServerSocket serverSocket;
    private InetAddress clientAddress;  //客户端的地址

    private MainActivity activity;
    private Handler handler;

    public WifiP2pHelper(MainActivity activity, Handler handler) {
        this.activity = activity;
        this.handler = handler;
        manager = (WifiP2pManager) activity
                .getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(activity, activity.getMainLooper(), null);
        deviceList = new ArrayList<WifiP2pDevice>();
    }

    // 开始查找设备
    public void discoverDevice() {
        Log.d(TAG, "WifiP2pHelper-->discoverDevice()");
        if (!isWifiOn()) {
            toggleWifi(true);
        }
        if (isConnected) {
            Log.d(TAG, "WifiP2pHelper-->discoverDevice ended-->isConnected=true");
            return;
        }
        handler.sendEmptyMessage(WIFIP2P_DEVICE_DISCOVERING);
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "WifiP2pHelper-->discoverDevice failed   reasonCode=" + reasonCode);
            }
        });
    }

    // 连接到设备
    public void connectDevice(WifiP2pDevice device) {
        Log.d(TAG, "WifiP2pHelper-->connectDevice()");
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
            }
        });
    }

    // 发送文件-->单独的新线程里面运行
    public void sendFiles(final ArrayList<File> fl) {
        new Thread() {
            @Override
            public void run() {
                OutputStream out = null;
                Socket socket = new Socket();
                try {
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(getSendToAddress(), SOCKET_PORT)), SOCKET_TIMEOUT);
                    out = socket.getOutputStream();
                } catch (IOException e) {
                    Log.i(TAG, "sendFiles error!!");
                    e.printStackTrace();
                    return ;
                }
                for (int i = 0; i < fl.size(); i++) {
                    File send = fl.get(i);
                    Log.i(TAG, "send file:"+send.getName());
                    if (isServer()) { //server send file to client,
                        sendFile(out, send);
                    } else if (isClient()) { //client send file to server
                        sendFile(out, send);
                    } else {
                        Log.i(TAG, "unknow error when prepare send file!!!");
                    }
                }
                try {
                    out.close();
                    socket.close();
                }catch (Exception e){}
            }
        }.start();
    }

    private void sendFile(OutputStream out, File f) {
        InputStream inputstream = null;
        try {
            inputstream = new FileInputStream(f);
            String name = f.getName();
            byte nameBT[] = name.getBytes();
            //1.发送文件名
            if (nameBT.length > (MAX_FILENAME_LEN - 1)) {
                nameBT = name.substring(name.length() - 20).getBytes();
            }
            byte nameBuf[] = new byte[MAX_FILENAME_LEN];
            for (int i = 0; i < nameBT.length; i++) {
                nameBuf[i] = nameBT[i];
            }
            out.write(nameBuf, 0, MAX_FILENAME_LEN);

            //2.发送文件大小
            long size = f.length();
            String fileSize = String.valueOf(size);
            Log.d(TAG, "send fileSize:" + size + " - " + fileSize);
            byte sizeBuf[] = new byte[MAX_FILESIZE_LEN];
            byte sizeTemp[] = fileSize.getBytes();
            for (int i = 0; i < sizeTemp.length; i++) {
                sizeBuf[i] = sizeTemp[i];
            }
            out.write(sizeBuf, 0, MAX_FILESIZE_LEN);

            //3.发送文件内容
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputstream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            inputstream.close();
            Log.d(WifiP2pHelper.TAG, "send a file sucessfully!!!");
        } catch (IOException e) {

        }
    }

    // 接收文件-->单独的新线程里面运行
    public void receviceFiles(final Socket socket) {
        new Thread() {
            @Override
            public void run() {
                receviceFile(socket);
            }
        }.start();
    }

    //recevice file until the other side close-->we can recevive more than one file a connection.
    private void receviceFile(Socket socket) {
        InputStream inputstream = null;
        OutputStream out = null;
        try {
            inputstream = socket.getInputStream();
            File sdcard = SdcardUtils.getUseableSdcardFile(activity, true);
            if (sdcard == null || !sdcard.canWrite()) return;
            while (true) {
                //next a few step will recevive a file
                long size = 0; //fileSize--->使用long类型,因为大文件(G)来说,大小用字节表示的话会超出int的表示范围
                //1.get the file name
                byte buffer[] = new byte[MAX_FILENAME_LEN];
                int receveCount = inputstream.read(buffer);
                if(receveCount == -1) {//the other side closed
                    break;
                }
                String name = new String(buffer, 0, receveCount);
                int validLen = name.indexOf("\0");
                if(validLen<0) {
                    Log.i(TAG, "recevice file name---> validLen<0");
                    break;
                }
                name = name.substring(0, validLen);
                Log.d(TAG, "recevice File->" + name);
                if (name == null || "".equals(name)) {//recevice file name failed, so break now!
                    Log.i(TAG, "recevice file name---> name exception");
                    break;
                }
                //2.get the file size
                byte buffer2[] = new byte[MAX_FILESIZE_LEN];
                receveCount = inputstream.read(buffer2);
                String fileSize = new String(buffer2, 0, receveCount);
                validLen = fileSize.indexOf("\0");
                fileSize = fileSize.substring(0, validLen);
                Log.d(TAG, "recevice fileSize:" + fileSize);
                size = Long.valueOf(fileSize);
                //3.create the new file
                File f = new File(sdcard, activity.getPackageName() + File.separator + name);
                if (!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
                f.createNewFile();
                //4.get the file content
                long receivedSize = 0;
                long leftSize = size;
                out = new FileOutputStream(f);
                byte buf[] = new byte[1024];
                int len;
                while (true) {
                    if (leftSize < 1024) {
                        byte temp[] = new byte[(int) leftSize];
                        len = inputstream.read(temp);
                        if (len == -1) break;
                        out.write(temp, 0, len);
                        leftSize -= len;
                        receivedSize += len;
                        break;
                    } else {
                        len = inputstream.read(buf);
                        if (len == -1) break;
                        out.write(buf, 0, len);
                        leftSize -= len;
                        receivedSize += len;
                    }
                    if (len == -1) {
                        break;
                    }
                }
                Log.d(WifiP2pHelper.TAG, "recevice a file sucessfully!!!"+name);
                //5.close the fileOutstream
                out.close();
            }
            inputstream.close();
        } catch (IOException e) {

        } finally {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    // 监听的回调
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        // TODO Auto-generated method stub
        deviceList.clear();
        deviceList.addAll(peerList.getDeviceList());
        handler.sendEmptyMessage(WIFIP2P_DEVICE_LIST_CHANGED);
    }


    //获取到了设备连接信息-->准备用Socket通信
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onConnectionInfoAvailable()");
        connectInfo = info;
        clientAddress = null;
        new Thread() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                isConnected = true;
                try {
                    serverSocket = new ServerSocket(SOCKET_PORT);
                    if (isServer()) {//recevive the client address
                        try {
                            Socket firstClientSocket = serverSocket.accept();
                            InputStream inputStream = firstClientSocket.getInputStream();
                            byte buf[] = new byte[128];
                            int len = inputStream.read(buf);
                            String clientIp = new String(buf, 0, len);
                            Log.d(TAG, "server has receviced clientIP:" + clientIp);
                            clientAddress = firstClientSocket.getInetAddress(); //get the client addr
                            inputStream.close();
                            firstClientSocket.close();
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    while (isConnected) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            Log.d(TAG, "a new connection->" + clientSocket);
                            receviceFiles(clientSocket); //create a new thread to handle the client connect
                            try {
                                sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    try {
                        serverSocket.close();
                    } catch (Exception e1) {
                    }
                }
            }
        }.start();
        if (isClient()) { //client-->send the client ip to server
            new Thread() {
                @Override
                public void run() {
                    Socket socket = new Socket();
                    try {
                        socket.bind(null);
                        socket.connect((new InetSocketAddress(connectInfo.groupOwnerAddress, SOCKET_PORT)), SOCKET_TIMEOUT);
                        OutputStream outputStream = socket.getOutputStream();
                        String ip = socket.getLocalAddress().toString();
                        outputStream.write(ip.getBytes());
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {

                    } finally {
                        try {
                            socket.close();
                        } catch (Exception e1) {
                        }
                    }
                }
            }.start();
        }
        handler.sendEmptyMessage(WIFIP2P_DEVICE_CONNECTED_SUCCESS);//设备已连接
    }

    //get the other side addr
    public InetAddress getSendToAddress() {
        if(isServer()) {
            return clientAddress;
        }else if(isClient()) {
            return connectInfo.groupOwnerAddress;
        }else {
            return null;
        }
    }

    public boolean isServer() {
        if (connectInfo.groupFormed && connectInfo.isGroupOwner) { // server--->receviceFile
            return true;
        } else if (connectInfo.groupFormed) {// client--->receviceFile

        }
        return false;
    }

    public boolean isClient() {
        if (connectInfo.groupFormed && connectInfo.isGroupOwner) { // server--->receviceFile

        } else if (connectInfo.groupFormed) {// client--->receviceFile
            return true;
        }
        return false;
    }

    public void release() {
        Log.d(TAG, "WifiP2pHelper-->release()");
        try {
            this.serverSocket.close();
        } catch (Exception e) {
        }
        manager.removeGroup(channel, new ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
            }
        });
    }

    // 打开/关闭wifi
    public void toggleWifi(boolean isOpen) {
        WifiManager wifiManager = (WifiManager) activity
                .getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(isOpen);
    }

    // 判断wifi是否打开
    public boolean isWifiOn() {
        WifiManager wifiManager = (WifiManager) activity
                .getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    //判断是否已连接其他设备
    public boolean isConnected() {
        return this.isConnected;
    }

    // getter
    public ArrayList<WifiP2pDevice> getDeviceList() {
        return this.deviceList;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
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
                manager.requestConnectionInfo(channel, this);
                Log.d(TAG, "device Connected!!--->requestConnectionInfo()");
            } else {
                // It's a disconnect
                isConnected = false;
                Log.d(TAG, "device disconnected!!)");
                release();
                handler.sendEmptyMessage(WIFIP2P_DEVICE_DISCONNECTED); //设置断开连接
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
//            DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
//                    .findFragmentById(R.id.frag_list);
//            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
//                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }
    }

}
