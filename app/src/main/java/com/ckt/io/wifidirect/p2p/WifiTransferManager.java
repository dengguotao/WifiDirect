package com.ckt.io.wifidirect.p2p;

import android.content.Context;
import android.nfc.Tag;
import android.os.Looper;

import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.utils.ApkUtils;
import com.ckt.io.wifidirect.utils.DataTypeUtils;
import com.ckt.io.wifidirect.utils.LogUtils;
import com.ckt.io.wifidirect.utils.SdcardUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Handler;

/**
 * Created by admin on 2016/7/29.
 */
public class WifiTransferManager {

    public static final String TAG = "WifiTransferManager";

    public static String TEMP_FILE_SUFFIX = ".tmp";

    public static final int SOCKET_TIMEOUT = 5000;
    public static final int TASK_TIMEOUT = 3000;
    public static final int MAX_SEND_TASK = 3;

    public static final byte MSG_REQEUST_FILE_INFO = 0;
    public static final byte MSG_RESPONSE_FILE_INFO = 1;
    public static final byte MSG_SEND_FILE = 2;
    public static final byte MSG_SEND_CLIENT_IP = 10;
    public static final byte MSG_RESPONSE_SEND_CLIENT_IP = 11;

    public static final String PARAM_TRANSFERED_LEN = "transferedLen";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_PATH = "path";
    public static final String PARAM_FILE_NAME = "name";
    public static final String PARAM_IS_HAS_FILE_EXTRA = "hasFileExtra";
    public static final String PARAM_ID = "id";
    public static final String PARAM_RET = "ret";

    private File receiveFileDir;

    private Context context;
    private InetAddress peerAddr;
    private int peerPort;


    private HashMap<Integer, Runnable> timeoutRunnableMap = new HashMap<>();
    private HashMap<Integer, String> sendMap = new HashMap<>();
    private ArrayList<Integer> sendList = new ArrayList<>();

    private SendTaskThread sendTaskHandleThread;

    private int nowSendTaskNum = 0;

    private android.os.Handler handler;

    private FileSendStateListener fileSendStateListener;
    private FileReceiveStateListener fileReceiveStateListener;

    private OnSendClientIpResponseListener onSendClientIpResponseListener;
    private OnGetClientIpListener onGetClientIpListener;

    public WifiTransferManager(Context context,
                               InetAddress addr,
                               int port,
                               FileSendStateListener fileSendStateListener,
                               FileReceiveStateListener fileReceiveStateListener,
                               OnGetClientIpListener onGetClientIpListener,
                               OnSendClientIpResponseListener onSendClientIpResponseListener) {
        if(context == null) {
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.context = context;
        this.peerAddr = addr;
        this.peerPort = port;
        File sdcard = SdcardUtils.getInnerSDcardFile(context);
        if(sdcard != null) {
            receiveFileDir = new File(sdcard, context.getString(R.string.received_file_dir));
        }
        handler = new android.os.Handler(Looper.getMainLooper());
        this.fileSendStateListener= fileSendStateListener;
        this.fileReceiveStateListener = fileReceiveStateListener;
        this.onGetClientIpListener = onGetClientIpListener;
        this.onSendClientIpResponseListener = onSendClientIpResponseListener;
    }

    public boolean sendFile(int id, String path) {
        File f = new File(path);
        if(id < 0 || path == null || "".endsWith(path) || !f.exists() || !f.isFile()) {
            return false;
        }

        //save the id and path.
        sendMap.put(id, path);
        //add to sendList
        sendList.add(id);

        //start sendTaskHandleThread if need
        startHandleSendTaskThread();

        return true;
    }

    public void sendClientIp() {
        Socket s = new Socket();
        try {
            s.bind(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("ip", s.getLocalAddress().toString());
        doSend(MSG_SEND_CLIENT_IP, map, null);
    }

    private boolean doSend(byte msgType, HashMap<String, String> paramMap, String extraFile) {
        LogUtils.d(TAG, "doSend  msgType="+msgType + " paramMap="+paramMap.toString() + "extraFile="+extraFile + " peerIp:"+peerAddr);
        boolean ret = true;
        //handle param
        boolean hasExtraFile = false;
        File f = null;
        if(extraFile != null ) {
            f = new File(extraFile);
            if(f.exists() && f.isFile() && !paramMap.containsKey(PARAM_IS_HAS_FILE_EXTRA)) {
                paramMap.put(PARAM_IS_HAS_FILE_EXTRA, String.valueOf(true));
                paramMap.put(PARAM_SIZE, String.valueOf(f.length()));
            }
            hasExtraFile = true;
        }
        String paramStr = DataTypeUtils.toJsonStr(paramMap);
        Socket socket = new Socket();
        OutputStream out = null;
        InputStream in = null;
        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(peerAddr, peerPort)), SOCKET_TIMEOUT);
            out = socket.getOutputStream();
            //step 1: send msgType
            out.write(msgType);
            //step 2: send param-len
            byte paramBuf [] = paramStr.getBytes();
            out.write(DataTypeUtils.intToBytes2(paramBuf.length));
            LogUtils.d(TAG, "do send msg param-len=" + paramBuf.length + "  paramStr:"+paramStr);
            //step 3: send param
            out.write(paramBuf);
            //step 4: send extra file if it has
            if(hasExtraFile) {
                long transferedSize = 0;
                try {//if paramMap.get return a null, Long.valueOf(..) will be fatal.
                    transferedSize = Long.valueOf(paramMap.get(PARAM_TRANSFERED_LEN));
                }catch (Exception e){}
                if(transferedSize >= f.length()) {
                    transferedSize = 0;
                }
                in = new FileInputStream(f);
                //skip transfered content
                in.skip(transferedSize);
                byte buf [] = new byte [2048];
                int len = 0;
                while((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                LogUtils.d(TAG, "do send file successed! ret=");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret = false;
        } finally {
            try {
                out.close();
            } catch (Exception e) {}
            try {
                in.close();
            } catch (Exception e) {}
            try {
                socket.close();
            } catch (Exception e) {}
        }
        if(msgType == MSG_SEND_FILE) {
            int id = -1;
            try {
                id = Integer.valueOf(paramMap.get(PARAM_ID));
            }catch (Exception e){}
            onSendFileFinished(id, ret);
        }
        return ret;
    }

    public void receive(final Socket socket) {
        new Thread() {
            @Override
            public void run() {
                doReceive(socket);
            }
        }.start();
    }

    private boolean doReceive(Socket socket) {
        if (socket == null) return false;
        boolean ret = true;
        InputStream inputstream = null;
        OutputStream out = null;
        int len = 0;
        byte msgType = -1;
        File f = null;
        try {
            inputstream = socket.getInputStream();
            //step 1: recevice the msg_type
            byte msg_type[] = new byte[1];//the buffer to recevice the msg_type
            len = inputstream.read(msg_type);
            if (len <= 0) {
                LogUtils.d(TAG, "receive msg type failed!!!");
                throw new Exception();
            }
            msgType = msg_type[0];
            LogUtils.d(TAG, "do receive: msg type = " + msgType);
            //step 2: recevice the param len
            byte param_len_buf[] = new byte[4]; //the buffer to recevie the param_len
            len = inputstream.read(param_len_buf);
            if (len <= 0) {
                LogUtils.d(TAG, "receive msg param-len failed!!!");
                throw new Exception();
            }
            int paramLen = DataTypeUtils.byteToInt2(param_len_buf);
            LogUtils.d(TAG, "do receive msg param-len="+paramLen);
            //step 3: receive the param_str and parse it
            byte param_buf[] = new byte[1024];
            int left = paramLen;
            StringBuffer sb = new StringBuffer();
            while(true) {
                if(left >= 1024) {
                    len = inputstream.read(param_buf);
                    if(len == -1) throw new Exception();
                    sb.append(new String(param_buf));
                    left -= len;
                }else {
                    byte tempBuf [] = new byte [left];
                    len = inputstream.read((tempBuf));
                    if(len == -1) throw new Exception();
                    sb.append(new String(tempBuf));
                    break;
                }
            }
            LogUtils.d(TAG, "do receive: paramStr="+sb.toString());
            HashMap<String, String> paramMap = DataTypeUtils.toHashmap(sb.toString());
            //step 4: handle the recevied msg
            switch (msg_type[0]) {
                case MSG_REQEUST_FILE_INFO: //another device request the file info
                    onFileInfoRequest(paramMap);
                    break;
                case MSG_RESPONSE_FILE_INFO://another device response our reqeust for file info
                    onRequestFileInfoResponsed((HashMap<String, String>) paramMap.clone());
                    break;
                case MSG_SEND_FILE://another device send a file to here.
                    //do step 5
                    break;
                case MSG_SEND_CLIENT_IP:
                    onGetClientIP(socket.getInetAddress());
                    break;
                case MSG_RESPONSE_SEND_CLIENT_IP:
                    onSendClientIpResponse(paramMap);
                    break;
            }
            //step 5:recevice extra info and save to file if it has.
            boolean isHasFileExtra = false;
            try {
                isHasFileExtra = Boolean.valueOf(paramMap.get(PARAM_IS_HAS_FILE_EXTRA));
            } catch (Exception e) {}
            if (isHasFileExtra) {
                //get important params
                String name = paramMap.get(PARAM_FILE_NAME);
                long size = 0;
                long transferedLen = 0;
                try {
                    size = Long.valueOf(paramMap.get(PARAM_SIZE));
                }catch (Exception e) {}
                try {
                    transferedLen = Long.valueOf(paramMap.get(PARAM_TRANSFERED_LEN));
                }catch (Exception e) {}

                if (name == null || size == 0) {
                    LogUtils.d(TAG, "receive file content: miss important params!!!");
                    throw new Exception();
                }
                f = getTempFile(name);
                LogUtils.d(TAG, "do receive file:"+f.getPath());
                onReciveFileStarted(f.getPath());
                File parent = f.getParentFile();
                if(!parent.exists()) {
                    parent.mkdirs();
                }
                if(transferedLen == 0) {//new file
                    f.createNewFile();
                    out = new FileOutputStream(f);
                }else {
                    if(!f.exists()) {
                        LogUtils.d(TAG, "recevie file [" + f.getPath() + "] failed: the tmp file does't exist in breakpoint-resume mode" );
                        throw new Exception();
                    }else if(f.length() != transferedLen) {
                        LogUtils.d(TAG, "recevie file [" + f.getPath() + "] failed: the tmp file size done't match the transfered size");
                        throw new Exception();
                    }
                    out = new FileOutputStream(f, true);//true--->append file
                }
                byte buf [] = new byte[2048];
                long receivedSize = transferedLen;
                while((len = inputstream.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    receivedSize += len;
                }
                LogUtils.d(TAG, "do receive file while end  receivedSize="+receivedSize+ " szie="+size);
                if(receivedSize == size) {
                    //remove the ".tmp" suffix
                    String savedPath = f.getPath();
                    File newFile = new File(savedPath.substring(0, savedPath.length()-TEMP_FILE_SUFFIX.length()));
                    newFile.delete();
                    f.renameTo(newFile);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
            ret = false;
        } finally {
            try {
                inputstream.close();
            } catch (Exception e){}
            try {
                out.close();
            } catch (Exception e) {}
            try {
                socket.close();
            } catch (Exception e) {}
        }
        if(msgType == MSG_SEND_FILE && f != null) {
            onReceiveFileFinished(f.getPath(), ret);
        }
        return ret;
    }

    public void onSendClientIpResponse(HashMap<String, String> map) {
        boolean ret = false;
        try {
            ret= Boolean.valueOf(map.get(PARAM_RET));
        }catch (Exception e) {}
        if(onSendClientIpResponseListener != null) {
            onSendClientIpResponseListener.onSendClientIpResponse(ret);
        }
    }

    public void onGetClientIP(InetAddress address) {
        peerAddr = address;
        if(onGetClientIpListener != null) {
            onGetClientIpListener.onGetClientIp(address);
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("test", "test");
        doSend(MSG_RESPONSE_SEND_CLIENT_IP, map, null);

        startHandleSendTaskThread();
    }

    public void onSendFileStarted(int id) {
        nowSendTaskNum ++;
        LogUtils.d(TAG, "onSendFileStarted --> id="+id);
    }

    public void onSendFileFinished(int id, boolean ret) {
        sendMap.remove(id);
        sendList.remove((Object)id);
        nowSendTaskNum --;
        startHandleSendTaskThread();
    }

    public void onReciveFileStarted(String path) {
        LogUtils.d(TAG, "receive File started: "+path);
    }

    public void onReceiveFileFinished(String path, boolean ret) {
        LogUtils.d(TAG, "receive File finished: "+path + " ret:" +ret);
    }

    /*
    ** handle request
     */
    public void onFileInfoRequest(HashMap<String, String> map) {
        String name = map.get(PARAM_FILE_NAME);
        if(name == null) {
            LogUtils.d(TAG, "received a request of file info, but file name is null, so ignore the receive!!!!");
            return ;
        }
        File f = getTempFile(name);
        long transferedLen = 0;
        if(f.exists()) {
            transferedLen = f.length();
        }
        map.put(PARAM_TRANSFERED_LEN, String.valueOf(transferedLen));
        //response it
        doSend(MSG_RESPONSE_FILE_INFO, map, null);
    }

    /*
    *
    * */
    public void onRequestFileInfoResponsed(HashMap<String, String> map) {
        int id = -1;
        try {
            id = Integer.valueOf(map.get(PARAM_ID));
        }catch (Exception e){}
        if(id < 0) {
            LogUtils.d(TAG, "received a request_file_info response, but the id is invalide, so ignore the receive!!!!");
            return ;
        }

        //remove timeout timer.
        Runnable timeoutRunnalbe = timeoutRunnableMap.get(id);
        if(timeoutRunnalbe != null) {
            handler.removeCallbacks(timeoutRunnalbe);
        }

        //do send file now
        doSend(MSG_SEND_FILE, map, sendMap.get(id));
    }

    public File getTempFile(String name) {
        return new File(receiveFileDir, name + TEMP_FILE_SUFFIX);
    }

    private void startTaskTimeoutTimer(int id) {
        Runnable runnable = new SendTaskTimeOutRunnable(id);
        timeoutRunnableMap.put(id, runnable);
        handler.postDelayed(runnable, TASK_TIMEOUT);
    }

    private void startHandleSendTaskThread() {
        if(sendTaskHandleThread != null && sendTaskHandleThread.isRunning) {
            //the thread is running now, do nothing
        }else {
            sendTaskHandleThread = new SendTaskThread();
            sendTaskHandleThread.start();
        }
    }

    class SendTaskThread extends Thread {
        boolean isRunning = false;
        @Override
        public void run() {
            isRunning = true;
            while(sendList.size() > 0 && nowSendTaskNum < MAX_SEND_TASK && peerAddr!=null) {
                int id = sendList.get(0);
                sendList.remove((Object)id);
                String path = sendMap.get(id);
                if(path == null) {
                    LogUtils.d(TAG, "send failed because path==null id="+id);
                }else {
                    File f = new File(path);
                    String name = f.getName();
                    if(name.toString().endsWith(".apk")) {
                        name = ApkUtils.getApkLable(context, f.getPath()) + ".apk";
                    }
                    final HashMap<String, String> paramMap = new HashMap<>();
                    paramMap.put(PARAM_ID, String.valueOf(id));
                    paramMap.put(PARAM_FILE_NAME, name);
                    //send request to get the file state in another device.
                    onSendFileStarted(id);
                    new Thread() {
                        @Override
                        public void run() {
                            doSend(MSG_REQEUST_FILE_INFO, paramMap, null);

                        }
                    }.start();
                }

                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isRunning = false;
        }
    }

    /*
    * task timeout runnable
    * */
    class SendTaskTimeOutRunnable implements Runnable {

        public int timeoutTaskId;
        public SendTaskTimeOutRunnable(int id) {
            timeoutTaskId = id;
        }

        @Override
        public void run() {
            LogUtils.d(TAG, "SendTask tiemout, id="+timeoutTaskId + "path="+sendMap.get(timeoutTaskId));
            onSendFileFinished(timeoutTaskId, false);
        }
    }

    public interface FileSendStateListener {
        public void onStart(int id, String path);
        public void onFinished(int id, String path, boolean ret);
    }

    public interface FileReceiveStateListener {
        public void onStart(int id, String path);
        public void onFinished(int id, String path, boolean ret);
    }

    public interface OnGetClientIpListener {
        public void onGetClientIp(InetAddress address);
    }

    public interface OnSendClientIpResponseListener {
        public void onSendClientIpResponse(boolean ret);
    }
}
