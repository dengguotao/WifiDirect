package com.ckt.io.wifidirect.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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


public class WifiP2pHelper extends BroadcastReceiver implements PeerListListener, ConnectionInfoListener{
	public static final String TAG = "WifiDirect";
	public static final int MAX_FILENAME_LEN = 32;
	public static final int MAX_FILESIZE_LEN = 16;
	public static final int SOCKET_PORT = 8989;
	public static final int SOCKET_TIMEOUT = 5000;

	public static final int WIFIP2P_DEVICE_LIST_CHANGED = 100;
	public static final int WIFIP2P_DEVICE_CONNECTED_SUCCESS = 101;
	private WifiP2pManager manager;
	private Channel channel;
	private ArrayList<WifiP2pDevice> deviceList;
	private WifiP2pInfo connectInfo;
	private boolean isConnected = false;

	// 作为服务端时,独有的2个socket
	private ServerSocket serverSocket;
	private Socket serverSocket_the_connected_clientSocket;
	// 作为客户端时,独有的1一个socket
	private Socket clientSocket_to_connect_server;

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
		if(isConnected) return;
		manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

			@Override
			public void onSuccess() {
			}

			@Override
			public void onFailure(int reasonCode) {
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
				for (int i = 0; i < fl.size(); i++) {
					File send = fl.get(i);
					sendFile(send);
				}
			}
		}.start();
	}
	private void sendFile(File f) {
		InputStream inputstream = null;
		OutputStream out = null;
		try {
			inputstream = new FileInputStream(f);
			if (connectInfo.groupFormed && connectInfo.isGroupOwner) { // server--->sendFile
				out = serverSocket_the_connected_clientSocket.getOutputStream();
			} else if (connectInfo.groupFormed) {// client--->sendFile
				out = clientSocket_to_connect_server.getOutputStream();
			} else {
				inputstream.close();
				return;
			}
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
			Log.d(TAG, "send fileSize:"+size+ " - "+fileSize);
			byte sizeBuf[] = new byte[MAX_FILESIZE_LEN];
			byte sizeTemp [] = fileSize.getBytes();
			for(int i=0; i<sizeTemp.length; i++) {
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

		} catch (IOException e) {
			try {
				out.close();
			} catch (Exception e1) {}
		}
	}
	
	// 接收文件-->单独的新线程里面运行
	public void receviceFiles() {
		new Thread() {
			@Override
			public void run() {
				receviceFile();
			}
		}.start();
	}
	private void receviceFile() {
		InputStream inputstream = null;
		OutputStream out = null;
		try {
			if (connectInfo.groupFormed && connectInfo.isGroupOwner) { // server--->receviceFile
				inputstream = serverSocket_the_connected_clientSocket
						.getInputStream();
			} else if (connectInfo.groupFormed) {// client--->receviceFile
				inputstream = clientSocket_to_connect_server.getInputStream();
			} else {
				return;
			}
			File sdcard = SdcardUtils.getUseableSdcardFile(activity, true);
			if(sdcard == null || !sdcard.canWrite()) return;
			
			while(true) {
				long size = 0; //fileSize--->使用long类型,因为大文件(G)来说,大小用字节表示的话会超出int的表示范围
				//1.get the file name
	            byte buffer [] = new byte[MAX_FILENAME_LEN];
	            int receveCount = inputstream.read(buffer);
//				if(receveCount==-1) {
//					continue;
//				}
	            String name = new String(buffer, 0, receveCount);
	            int validLen = name.indexOf("\0");
	            name = name.substring(0, validLen);
	            Log.d(TAG, "recevice File->"+name);
	            if(name == null || "".equals(name)) {//recevice file name failed, so return null now!
	                return;
	            }
	            //2.get the file size
	            byte buffer2 [] = new byte[MAX_FILESIZE_LEN];
	            receveCount = inputstream.read(buffer2);
	            String fileSize = new String(buffer2, 0, receveCount);
	            validLen = fileSize.indexOf("\0");
	            fileSize = fileSize.substring(0, validLen);
	            Log.d(TAG, "recevice fileSize:"+fileSize);
	            size = Long.valueOf(fileSize); 
	            //3.create the new file
	            File f = new File(sdcard, activity.getPackageName()+File.separator+name);
				if(!f.getParentFile().exists()) {
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
		        	if(leftSize<1024) {
		        		byte temp [] = new byte[(int) leftSize];
		        		len = inputstream.read(temp);
		        		if(len == -1)	break;
		        		out.write(temp, 0, len);
		        		leftSize -= len;
		                receivedSize += len;
		        		break;
		        	}else {
		        		len = inputstream.read(buf);
		        		if(len == -1)	break;
		                out.write(buf, 0, len);
		                leftSize -= len;
		                receivedSize += len;
		        	}
	            }
		        out.close();
			}
			
		} catch (IOException e) {
		}finally {
			try {
				inputstream.close();
			} catch (Exception e1) {}
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
		new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(isServer()) { //server
					try {
						serverSocket = new ServerSocket(SOCKET_PORT);
						serverSocket_the_connected_clientSocket = serverSocket.accept();
						Log.d(TAG, "server has accept client!!!");
						handler.sendEmptyMessage(0);
						isConnected = true;
						
					} catch (IOException e) {
						try {
							serverSocket.close();
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						// TODO Auto-generated catch block
						Log.d(TAG, "server accept error!!!");
						e.printStackTrace();
					}
				}else if(isClient()) { //client
					clientSocket_to_connect_server = new Socket();
					try {
						clientSocket_to_connect_server.bind(null);
						clientSocket_to_connect_server.connect((new InetSocketAddress(connectInfo.groupOwnerAddress, SOCKET_PORT)), SOCKET_TIMEOUT);
						Log.d(TAG, "client has connected to server!!!");
						handler.sendEmptyMessage(1);
					} catch (IOException e) {
						try {
							clientSocket_to_connect_server.close();
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						Log.d(TAG, "client connected error!!!");
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else {
					return;
				}
				//start receviceFile
				receviceFiles();
			}
		}.start();
		
		handler.sendEmptyMessage(WIFIP2P_DEVICE_CONNECTED_SUCCESS);
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
		Log.d(TAG, "WifiP2pHelper-->connectDevice()");
		try {
			this.serverSocket.close();
		}catch(Exception e){}
		try {
			this.serverSocket_the_connected_clientSocket.close();
		}catch(Exception e){}
		try{
			this.clientSocket_to_connect_server.close();
		}catch(Exception e){}
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
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
//            DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
//                    .findFragmentById(R.id.frag_list);
//            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
//                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }
	}

}
