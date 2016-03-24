package com.ckt.io.wifidirect.provider;

import android.util.Log;

import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.utils.LogUtils;

import java.util.Calendar;

/**
 * every sended file has a unique record
 */
public class Record {
    public static final int TRANSPORT_DERICTION_SEND = 0; //发送
    public static final int TRANSPORT_DERICTION_RECEVING = 1; //接收

    public static final int STATE_FINISHED = 0; //完成
    public static final int STATE_FAILED = 1; //失败
    public static final int STATE_WAIT_FOR_TRANSPORT = 2; //等待
    public static final int STATE_TRANSPORTING = 3; //正在
    public static final int STATE_PAUSED = 4; //暂停

    private long id;
    private String name;
    private String path;
    private long length;
    private long transported_len;
    private int state;
    private int transport_direction;
    private boolean isSend; //true -- send  false----receive

    private OnStateChangeListener listener;

    public Record(String path, long length, long transported_len, int state, int transport_direction, boolean isSend) {
        this.path = path;
        this.length = length;
        this.transported_len = transported_len;
        this.state = state;
        this.transport_direction = transport_direction;
        this.isSend = isSend;
        //使用当前时间来作为id
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_YEAR);
        long ms = System.currentTimeMillis();
        StringBuffer buffer = new StringBuffer();
        buffer.append(day).append(ms);

        this.id = Long.valueOf(buffer.toString()) + length;
        LogUtils.i(WifiP2pHelper.TAG, "id=" + this.id);
    }

    public Record(String name, String path, long length, long transported_len, int state, int transport_direction, boolean isSend) {
        this(path, length, transported_len, state, transport_direction, isSend);
        this.name = name;
    }

    public Record(long id, String path, long length, long transported_len, int state, int transport_direction, boolean isSend) {
        this.id = id;
        this.path = path;
        this.length = length;
        this.transported_len = transported_len;
        this.state = state;
        this.transport_direction = transport_direction;
        this.isSend = isSend;
    }

    public Record(long id, String name, String path, long length, long transported_len, int state, int transport_direction, boolean isSend) {
        this.isSend = isSend;
        this.id = id;
        this.name = name;
        this.path = path;
        this.length = length;
        this.transported_len = transported_len;
        this.state = state;
        this.transport_direction = transport_direction;
    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTransported_len() {
        return transported_len;
    }

    public void setTransported_len(long transported_len) {
        this.transported_len = transported_len;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        int old = state;
        this.state = state;
        if(listener != null) {
            listener.onStateChanged(this, old, state);
        }
    }

    public int getTransport_direction() {
        return transport_direction;
    }

    public void setTransport_direction(int transport_direction) {
        this.transport_direction = transport_direction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSend() {
        return isSend;
    }

    public void setIsSend(boolean isSend) {
        this.isSend = isSend;
    }

    public OnStateChangeListener getListener() {
        return listener;
    }

    public void setListener(OnStateChangeListener listener) {
        this.listener = listener;
    }

    public interface OnStateChangeListener {
        public abstract void onStateChanged(Record record, int state_olad, int state_new);
    }

}
