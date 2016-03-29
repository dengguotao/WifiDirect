package com.ckt.io.wifidirect;

import android.Manifest;
import android.app.Activity;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.ckt.io.wifidirect.fragment.HistoryFragment;
import com.ckt.io.wifidirect.myViews.DeviceConnectDialog;
import com.ckt.io.wifidirect.fragment.ContentFragment;
import com.ckt.io.wifidirect.fragment.FileExplorerFragment;
import com.ckt.io.wifidirect.myViews.SpeedFloatWin;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.provider.Record;
import com.ckt.io.wifidirect.provider.RecordManager;
import com.ckt.io.wifidirect.utils.FileResLoaderUtils;
import com.ckt.io.wifidirect.utils.LogUtils;
import com.ckt.io.wifidirect.utils.SdcardUtils;
import com.ckt.io.wifidirect.utils.ToastUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends BaseActivity {
    public static final byte REQUEST_CODE_SYSTEM_ALERT_PERMISSION = 5;
    public static final byte REQUEST_CODE_READ_EXTERNAL = 6;
    public static final byte REQUEST_CODE_WRITE_EXTERNAL = 7;

    private static final String TAG = "MainActivity";
    private DeviceConnectDialog deviceConnectDialog;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    protected Toolbar toolbar;

    private File received_file_path; //收到的文件的保存路径

    private ArrayList<String> selectFiles;  //the selected files to send

    private ContentFragment contentfragment;
    private OnSendFileListChangeListener onSendFileListChangeListener;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pHelper wifiP2pHelper;
    private boolean isTranfering;
    private int mSendCount;
    private int mReceviceCount;
    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            RecordManager recordManager = RecordManager.getInstance(getApplicationContext());
            Record record = null;
            File f = null;
            switch (msg.what) {
                case WifiP2pHelper.WIFIP2P_DEVICE_LIST_CHANGED://可用的设备列表更新
                    deviceConnectDialog.updateDeviceList(wifiP2pHelper.getDeviceList());
                    break;
                case WifiP2pHelper.WIFIP2P_DEVICE_CONNECTED_SUCCESS://设备连接成功
                    deviceConnectDialog.updateConnectedInfo(wifiP2pHelper.isServer());
                    //连接成功后关联 dialog
                    if(deviceConnectDialog.isShowing()) {
                        deviceConnectDialog.dismiss();
                    }
                    ToastUtils.toast(MainActivity.this, R.string.connect_successed);
                    break;
                case WifiP2pHelper.WIFIP2P_DEVICE_DISCONNECTED: //连接已断开
                    deviceConnectDialog.onDisconnectedInfo();
                    break;

                /////////////////////////////////////////////////////////////////////////
                case WifiP2pHelper.WIFIP2P_SENDFILELIST_ADDED: //正在发送列表新增文件
                    ArrayList<File> addedList = (ArrayList<File>) msg.obj;
                    recordManager.addNewSendingRecord(addedList, wifiP2pHelper.getCurrentConnectMAC());
                    break;
                case WifiP2pHelper.WIFIP2P_BEGIN_SEND_FILE: //开始发送一个文件
                    LogUtils.i(WifiP2pHelper.TAG, "handle--->WIFIP2P_BEGIN_SEND_FILE");
                    f = (File) msg.obj;
                    record = recordManager.findRecord(f.getPath(), Record.STATE_WAIT_FOR_TRANSPORT, true);
                    if(record == null) break;
                    record.setState(Record.STATE_TRANSPORTING);
                    ////////////////////////////////////////
                    this.sendEmptyMessageDelayed(2, 100);
                    break;
                case WifiP2pHelper.WIFIP2P_SEND_ONE_FILE_SUCCESSFULLY:  //发送完一个文件----成功
                case WifiP2pHelper.WIFIP2P_SEND_ONE_FILE_FAILURE:    // 发送完一个文件----失败
                    f = (File) msg.obj;
                    record = recordManager.findRecord(f.getPath(), Record.STATE_TRANSPORTING, true);
                    if(record == null) break;
                    if(msg.what == WifiP2pHelper.WIFIP2P_SEND_ONE_FILE_SUCCESSFULLY) {//发送成功
                        record.setState(Record.STATE_FINISHED);
                    }else { //发送失败
                        record.setState(Record.STATE_FAILED);
                    }

                    if(!wifiP2pHelper.isTranfering()) {
                        mSendCount = 0;
                        mReceviceCount = 0;
                        handler.removeMessages(2);
                        SpeedFloatWin.updateSpeed("0MB/s", "0MB/s");
                    }
                    break;

                case WifiP2pHelper.WIFIP2P_BEGIN_RECEIVE_FILE:  //开始接收文件
                    LogUtils.i(WifiP2pHelper.TAG, "handle--->WIFIP2P_BEGIN_SEND_FILE");
                    HashMap<String, Object> map = (HashMap<String, Object>) msg.obj;
                    f = (File) map.get("path");
                    String sName = (String) map.get("name");
                    recordManager.addNewRecevingRecord(f, sName, wifiP2pHelper.getCurrentConnectMAC());
                    break;
                case WifiP2pHelper.WIFIP2P_RECEIVE_ONE_FILE_SUCCESSFULLY: //接收完一个文件----成功
                case WifiP2pHelper.WIFIP2P_RECEIVE_ONE_FILE_FAILURE:  //接收完一个文件---失败
                    f = (File) msg.obj;
                    if(f == null) break;
                    record = recordManager.findRecord(f.getPath(), Record.STATE_TRANSPORTING, false);
                    if(record == null) {
                        LogUtils.i(WifiP2pHelper.TAG, "WIFIP2P_RECEIVE_ONE_FILE---->record == null 更新记录状态失败");
                        break;
                    }
                    if(msg.what == WifiP2pHelper.WIFIP2P_RECEIVE_ONE_FILE_SUCCESSFULLY) {//接收成功
                        record.setState(Record.STATE_FINISHED);
                    }else { //接受失败
                        record.setState(Record.STATE_FAILED);
                    }

                    if(!wifiP2pHelper.isTranfering()) {
                        mSendCount = 0;
                        mReceviceCount = 0;
                        handler.removeMessages(2);
                        SpeedFloatWin.updateSpeed("0MB/s", "0MB/s");
                    }
                    break;

                case 2:
                    if(wifiP2pHelper.isTranfering()) {
                        int sendCount = wifiP2pHelper.getSendCount();
                        int receviceCount = wifiP2pHelper.getReceviceCount();
                        double sendSpeed = (sendCount - mSendCount) / 1024.0 /1024;
                        double receSpeed = (receviceCount - mReceviceCount) / 1024.0 /1024;
                        mSendCount = sendCount;
                        mReceviceCount = receviceCount;
                        Log.d(TAG, "speed: " + (sendSpeed > receSpeed ? sendSpeed : receSpeed) + " MB");
                        DecimalFormat df = new DecimalFormat("0.0");
                        SpeedFloatWin.updateSpeed(df.format(sendSpeed)+"MB/s", df.format(receSpeed)+"MB/s");
                        this.sendEmptyMessageDelayed(2, 1000);
                    }else {
                        SpeedFloatWin.updateSpeed("0MB/s", "0MB/s");
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        selectFiles = new ArrayList<>();
        wifiP2pHelper = new WifiP2pHelper(this, this.handler);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        contentfragment = (ContentFragment) getSupportFragmentManager().findFragmentById(R.id.id_content);
        setOnSendFileListChangeListener(contentfragment);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.id_drawer_layout);
        // 實作 drawer toggle 並放入 toolbar
        toolbar = (Toolbar) findViewById(R.id.id_toolbar_layout);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        deviceConnectDialog = new DeviceConnectDialog(this, R.style.FullHeightDialog);

        clearSendFileList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wifiP2pHelper, intentFilter);
        //显示传输速度窗口
        /*requestPermission(this.hashCode()%200 + REQUEST_CODE_SYSTEM_ALERT_PERMISSION,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                new Runnable() {
                    @Override
                    public void run() {
                        SpeedFloatWin.show(MainActivity.this);
                    }
                },null);*/
        RecordManager manager = RecordManager.getInstance(this);
        manager.clearAllRecord();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(wifiP2pHelper);
        SpeedFloatWin.hide(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.i(WifiP2pHelper.TAG, "MainActivity onDestroy");
        wifiP2pHelper.release();
        FileResLoaderUtils.release();
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
        if (contentfragment.isNowFileExplorerFragment() && fileExplorerFragment != null && fileExplorerFragment.back()) {
            Log.d(WifiP2pHelper.TAG, "文件浏览器返回");
            return;
        }
        super.onBackPressed();
    }

    public File getReceivedFileDirPath() {
        if(received_file_path == null) {
            received_file_path = new File(SdcardUtils.getUseableSdcardFile(getApplicationContext(), false),
                    getResources().getString(R.string.received_file_dir));
        }
        return received_file_path;
    }

    //添加文件到选择列表中
    public boolean addFileToSendFileList(String path) {
        return addFileToSendFileList(path, null);
    }
    public boolean addFileToSendFileList(String path, String name) {
        if(path==null || "".equals(path)) {
            return false;
        }
        boolean isExist = false;
        for(int i=0; i<selectFiles.size(); i++) {
            String temp = selectFiles.get(i);
            if(path.equals(temp)) {
                isExist = true;
                break;
            }
        }
        if(!isExist) {
            selectFiles.add(path);
            if(this.onSendFileListChangeListener!=null) {
                this.onSendFileListChangeListener.onSendFileListChange(this.selectFiles, selectFiles.size());
            }
        }
        return !isExist;
    }
    public void removeFileFromSendFileList(ArrayList<String> list) {
        if(list == null) return;
        for(int i=0; i<list.size(); i++) {
            removeFileFromSendFileList(list.get(i));
        }
    }
    //remove a file in the sendFile-list
    public boolean removeFileFromSendFileList(String path) {
        if(path==null || "".equals(path)) {
            return false;
        }
        boolean isExist = false;
        for(int i=0; i<selectFiles.size(); i++) {
            String temp = selectFiles.get(i);
            if(path.equals(temp)) {
                selectFiles.remove(i);
                isExist = true;
                if(this.onSendFileListChangeListener!=null) {
                    this.onSendFileListChangeListener.onSendFileListChange(this.selectFiles, selectFiles.size());
                }
                break;
            }
        }
        return isExist;
    }
    //clear the sendFile-list
    public void clearSendFileList() {
        selectFiles.clear();
        if(this.onSendFileListChangeListener!=null) {
            this.onSendFileListChangeListener.onSendFileListChange(this.selectFiles, selectFiles.size());
        }
    }

    public ArrayList<String> getSendFiles() {
        return this.selectFiles;
    }
    public WifiP2pHelper getWifiP2pHelper() {
        return wifiP2pHelper;
    }

    public Handler getHandler() {
        return this.handler;
    }

    public DeviceConnectDialog getDeviceConnectDialog() {
        return this.deviceConnectDialog;
    }

    public void setOnSendFileListChangeListener(OnSendFileListChangeListener onSendFileListChangeListener) {
        this.onSendFileListChangeListener = onSendFileListChangeListener;
    }

    //选择的发送列表改变
    public interface OnSendFileListChangeListener {
        public abstract void onSendFileListChange(ArrayList<String> selectFiles, int num);
    }
}
