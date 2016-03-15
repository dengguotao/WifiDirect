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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.utils.ApkUtils;
import com.ckt.io.wifidirect.utils.DataTypeUtils;
import com.ckt.io.wifidirect.utils.LogUtils;
import com.ckt.io.wifidirect.utils.SdcardUtils;


public class WifiP2pHelper extends BroadcastReceiver implements PeerListListener, ConnectionInfoListener {
    public static final String TAG = "WifiDirect";
    public static final int SOCKET_PORT = 8989;
    public static final int SOCKET_TIMEOUT = 5000;

    public static final int WIFIP2P_DEVICE_DISCOVERING = 99; //正在发现设备
    public static final int WIFIP2P_DEVICE_LIST_CHANGED = 100;//可以设备列表更新
    public static final int WIFIP2P_DEVICE_CONNECTED_SUCCESS = 101;//连接设备成功
    public static final int WIFIP2P_DEVICE_DISCONNECTED = 102;//链接断开


    public static final int WIFIP2P_SENDFILELIST_CHANGED = 120;//文件发送列表改变
    public static final int WIFIP2P_SEND_ONE_FILE_SUCCESSFULLY = 121; //成功发送一个文件
    public static final int WIFIP2P_SEND_ONE_FILE_FAILURE = 122; //成功发送一个失败
    public static final int WIFIP2P_RECEIVE_ONE_FILE_SUCCESSFULLY = 123; //成功接收一个文件
    public static final int WIFIP2P_RECEIVE_ONE_FILE_FAILURE = 124; //接收一个文件失败
    public static final int WIFIP2P_BEGIN_SEND_FILE = 125; //开始发送文件
    public static final int WIFIP2P_BEGIN_RECEIVE_FILE = 126; //开始接收文件
    private WifiP2pManager manager;
    private Channel channel;
    private ArrayList<WifiP2pDevice> deviceList;
    private WifiP2pInfo connectInfo;
    private boolean isConnected = false;

    // 作为服务socket用来接收对方发送的文件
    private ServerSocket serverSocket;
    private InetAddress clientAddress;  //客户端的地址
    private String currentMAC;
    private String currentConnectMAC;

    private MainActivity activity;
    private Handler handler;

    private int mSendCount;
    private int mReceviceCount;

    private ArrayList<File> sendFileList;
    private FileReceiveAsyncTask fileReceiveAsyncTask;
    private FileSendAsyncTask fileSendAsyncTask;

    public WifiP2pHelper(MainActivity activity, Handler handler) {
        this.activity = activity;
        this.handler = handler;
        manager = (WifiP2pManager) activity
                .getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(activity, activity.getMainLooper(), null);
        deviceList = new ArrayList<WifiP2pDevice>();
        sendFileList = new ArrayList<>();
        updateWifiMac();
    }

    private void updateWifiMac() {
        if (currentMAC == null) {
            WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
            currentMAC = wifiManager.getConnectionInfo() == null ? null : wifiManager.getConnectionInfo().getMacAddress();
        }
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

    public boolean isTranfering() {
        return ((fileReceiveAsyncTask != null && fileReceiveAsyncTask.isTranfering)
                || (fileSendAsyncTask != null && fileSendAsyncTask.isTranfering));
    }

    public int getSendCount() {
        return mSendCount;
    }

    // 发送文件
    public void sendFiles(final ArrayList<File> fl) {
        if (sendFileList.size() == 0) { //没有文件正在发送-->启动后台发送任务
            sendFileList.addAll(fl);
            fileSendAsyncTask = new FileSendAsyncTask();
            fileSendAsyncTask.execute(new File(""));
        } else { //有文件正在发送,添加到发送列表中即可
            sendFileList.addAll(fl);
        }
        handler.sendEmptyMessage(WIFIP2P_SENDFILELIST_CHANGED);
    }

    // 接收文件
    public void receviceFiles(final Socket socket) {
        fileReceiveAsyncTask = new FileReceiveAsyncTask();
        fileReceiveAsyncTask.execute(socket);
    }

    private class FileSendAsyncTask extends AsyncTask<File, Integer, Boolean> {
        private boolean isTranfering = false;

        @Override
        protected Boolean doInBackground(File... params) {
            isTranfering = true;
            while (sendFileList.size() != 0) {
                boolean isSuccessed = true;
                File f = sendFileList.get(0);
                OutputStream out = null;
                InputStream inputstream = null;
                Socket socket = new Socket();
                try {
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(getSendToAddress(), SOCKET_PORT)), SOCKET_TIMEOUT);
                    out = socket.getOutputStream();
                } catch (IOException e) {
                    Log.i(TAG, "sendFiles error!!");
                    e.printStackTrace();
                    try {
                        if (out != null) {
                            out.close();
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    isSuccessed = false;
                }
                handler.sendEmptyMessage(WIFIP2P_BEGIN_SEND_FILE);
                try {
                    inputstream = new FileInputStream(f);
                    String name = f.getName();
                    if (name.toLowerCase().endsWith(".apk")) {
                        name = ApkUtils.getApkLable(activity, f.getPath()) + ".apk";
                    }
                    name = name.replace("&", ""); //去掉文件名中的&符号
                    String info = name + "&" + String.valueOf(f.length()); //info = name&fileSize
                    byte nameBytes[] = info.getBytes();
                    //1.发送文件信息的长度(int型的长度要转化为byte[]型来发送);
                    byte nameLenByte[] = DataTypeUtils.intToBytes2(nameBytes.length);
                    out.write(nameLenByte);
                    //2.发送信息
                    out.write(nameBytes, 0, nameBytes.length);
                    //3.发送文件内容
                    byte buf[] = new byte[1024];
                    int len;
                    mSendCount = 0;
                    while ((len = inputstream.read(buf)) != -1) {
                        out.write(buf, 0, len);
                        mSendCount += len;
                    }
                    Log.d(WifiP2pHelper.TAG, "send a file sucessfully!!!");
                } catch (IOException e) {
                    isSuccessed = false;
                } finally {
                    try {
                        if (inputstream != null) {
                            inputstream.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException e) {
                    }
                }
                Message msg = new Message();
                msg.obj = f;
                if (isSuccessed) {
                    msg.what = WIFIP2P_SEND_ONE_FILE_SUCCESSFULLY;
                } else {
                    msg.what = WIFIP2P_SEND_ONE_FILE_FAILURE;
                }
                handler.sendMessage(msg);
                sendFileList.remove(0);//从发送文件列表中移除
            }
            isTranfering = false;
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
        }
    }

    private class FileReceiveAsyncTask extends AsyncTask<Socket, Integer, Boolean> {
        private boolean isTranfering = false;

        @Override
        protected Boolean doInBackground(Socket... params) {
            isTranfering = true;
            Socket socket = params[0];
            if (socket == null) return false;
            InputStream inputstream = null;
            OutputStream out = null;
            try {
                inputstream = socket.getInputStream();
                File sdcard = SdcardUtils.getUseableSdcardFile(activity, true);
                if (sdcard == null || !sdcard.canWrite()) {
                    Log.d(TAG, "没有sdcard可写");
                    return false;
                }

                //next a few step will recevive a file
                long size = 0; //fileSize--->使用long类型,因为大文件(G)来说,大小用字节表示的话会超出int的表示范围
                //1.获取文件信息长度
                byte buffer_nameLen[] = new byte[4];
                int receveCount = inputstream.read(buffer_nameLen);
                if (receveCount == -1) {//the other side closed
                    Log.d(TAG, "get file name len error");
                    throw new IOException();
                }
                int fileNameLen = DataTypeUtils.byteToInt2(buffer_nameLen);
                //2.获取文件信息
                byte buffer[] = new byte[fileNameLen];
                receveCount = inputstream.read(buffer);
                if (receveCount == -1) {//the other side closed
                    Log.d(TAG, "get file name error");
                    throw new IOException();
                }
                String info = new String(buffer, 0, receveCount);
                Log.d(TAG, "recevice File info->" + info);
                if (info == null || "".equals(info)) {//recevice file name failed, so break now!
                    Log.i(TAG, "recevice file info---> info exception");
                    throw new IOException();
                }
                String infos[] = info.split("&");
                String name = infos[0]; //文件名
                size = Long.valueOf(infos[1]);
                Log.d(TAG, "recevice fileinfo: name=" + name + " size=" + size);
                //create the new file
                File f = null;
                //防止文件名冲突
                int i = 1;
                while (true) {
                    if (i == 1) {
                        f = new File(sdcard, activity.getPackageName() + File.separator + name);
                    } else {
                        f = new File(activity.getPackageName() + File.separator + "(" + i + ")" + name);
                    }
                    i++;
                    if (!f.exists()) {
                        break;
                    }
                }
                if (!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
                f.createNewFile();
                //3.获取文件内容
                long receivedSize = 0;
                long leftSize = size;
                out = new FileOutputStream(f);
                mReceviceCount = 0;
                handler.sendEmptyMessage(1);
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
                    mReceviceCount += len;
                    if (len == -1) {
                        break;
                    }
                }
                Log.d(WifiP2pHelper.TAG, "recevice a file sucessfully!!!" + name);

            } catch (IOException e) {

            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                    if (inputstream != null) {
                        inputstream.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (Exception e) {
                }
                mReceviceCount = 0;
                handler.sendEmptyMessage(0);
            }
            isTranfering = false;
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
        }
    }


    public int getReceviceCount() {
        return mReceviceCount;
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
                            LogUtils.d(TAG, "serverSocket.accept() max=");
                            InputStream inputStream = firstClientSocket.getInputStream();
                            OutputStream outputStream = firstClientSocket.getOutputStream();
                            clientAddress = firstClientSocket.getInetAddress(); //get the client addr
                            //1.发送MAC地址给客户端
                            if(currentMAC != null) {
                                outputStream.write(currentMAC.getBytes());
                            }
                            outputStream.flush();
                            firstClientSocket.shutdownOutput();
                            //2.读取客户端的MAC地址
                            byte buf[] = new byte[128];
                            StringBuffer stringBuffer =new StringBuffer();
                            int len = 0;
                            while ((len = inputStream.read(buf)) != -1) {
                                stringBuffer.append(new String(buf, 0, len));
                            }
                            inputStream.close();
                            String clientMac = stringBuffer.toString();
                            if(clientMac != null && !clientMac.equals("")) {
                                currentConnectMAC = clientMac;
                            }
                            LogUtils.d(TAG, "server has receviced clientMAC:" + currentConnectMAC);
                            outputStream.close();
                            firstClientSocket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else if(isClient()) {
                        Socket socket = new Socket();
                        try {
                            LogUtils.d(TAG, "client start transfer MAC cuMAC: "+currentMAC);
                            socket.bind(null);
                            socket.connect((new InetSocketAddress(connectInfo.groupOwnerAddress, SOCKET_PORT)), SOCKET_TIMEOUT);
                            OutputStream outputStream = socket.getOutputStream();
                            InputStream inputStream = socket.getInputStream();
                            //1.读取服务端的MAC地址
                            byte buf[] = new byte[128];
                            StringBuffer stringBuffer =new StringBuffer();
                            int len = 0;
                            while ((len = inputStream.read(buf)) != -1) {
                                stringBuffer.append(new String(buf, 0, len));
                            }
                            String serverMac = stringBuffer.toString();
                            if(serverMac != null && !serverMac.equals("")) {
                                currentConnectMAC = serverMac;
                            }
                            //2. 发送自己的MAC地址给服务端
                            if (currentMAC != null) {
                                outputStream.write(currentMAC.getBytes());
                            }
                            outputStream.flush();
                            outputStream.close();
                            LogUtils.d(TAG, "client has receviced serverMAC:" + currentConnectMAC);
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                socket.close();
                            } catch (Exception e1) {
                            }
                        }
                    }
                    //开始等待对方连接
                    while (isConnected) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            Log.d(TAG, "a new connection->" + clientSocket);
                            receviceFiles(clientSocket); //create a new task to handle the client connect
                            try {
                                sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    try {
                        serverSocket.close();
                    } catch (Exception e1) {}
                }
            }
        }.start();
        handler.sendEmptyMessage(WIFIP2P_DEVICE_CONNECTED_SUCCESS);//设备已连接
    }

    //get the other side addr
    public InetAddress getSendToAddress() {
        if (isServer()) {
            return clientAddress;
        } else if (isClient()) {
            return connectInfo.groupOwnerAddress;
        } else {
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
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiMac();
        }
    }

}
