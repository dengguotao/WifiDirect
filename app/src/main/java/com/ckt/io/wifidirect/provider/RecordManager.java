package com.ckt.io.wifidirect.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.ckt.io.wifidirect.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by admin on 2016/3/24.
 */
public class RecordManager implements Record.OnStateChangeListener{
    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;
    public static RecordManager manager;

    private ArrayList<Record> recordArrayList = new ArrayList<>();
    private ArrayList<OnRecordsChangedListener> listenerArrayList = new ArrayList<>();
    private HashMap<Record, Uri> uris = new HashMap<>(); //保存每个record对应的URI
    private Context context;

    public static RecordManager getInstance(Context context) {
        if(manager == null) {
            manager = new RecordManager(context);
        }
        return manager;
    }

    public RecordManager(Context context) {
        this.context = context;
    }

    //新增正在发送的记录
    public void addNewSendingRecord(ArrayList<File> list) {
        ArrayList<Record> changedRecords = new ArrayList<>();
        for(int i=0; i<list.size(); i++) {
            File f = list.get(i);
            Record record = new Record(
                    f.getPath(),
                    f.length(),
                    0,
                    Record.STATE_WAIT_FOR_TRANSPORT,
                    Record.DIRECTION_OUT);
            this.add(record, false);
            changedRecords.add(record);
        }
        //监听回调
        for(int i=0; i<listenerArrayList.size(); i++) {
            OnRecordsChangedListener listener = listenerArrayList.get(i);
            if(listener != null) {
                listener.onRecordListChanged(ACTION_ADD, changedRecords);
            }
        }
    }

    //新增正在接收的记录
    public Record addNewRecevingRecord(File f) {
        Record record = new Record(f.getPath(),
                f.length(),
                0,
                Record.STATE_TRANSPORTING,
                Record.DIRECTION_IN);
        this.add(record);
        return record;
    }

    //添加监听
    public void addOnRecordsChangedListener(OnRecordsChangedListener listener) {
        if(!listenerArrayList.contains(listener)) {
            listenerArrayList.add(listener);
        }
    }

    //移除监听
    public void removeOnRecordsChangedListener(OnRecordsChangedListener listener) {
        listenerArrayList.remove(listener);
    }

    //新增record
    public void add(Record record) {
        add(record, true);
    }

    private void add(Record record, boolean isCallListener) {
        if(!recordArrayList.contains(record)) {
            record.setListener(this);
            recordArrayList.add(record);
            //回调监听
            if(isCallListener) {
                for(int i=0; i<listenerArrayList.size(); i++) {
                    OnRecordsChangedListener listener = listenerArrayList.get(i);
                    if(listener != null) {
                        ArrayList<Record> changedRecords = new ArrayList<>();
                        changedRecords.add(0, record);
                        listener.onRecordListChanged(ACTION_ADD, changedRecords);
                    }
                }
            }
        }
    }



    //移除record
    public void remove(Record record) {
        if(recordArrayList.remove(record)) {
            //回调监听
            for(int i=0; i<listenerArrayList.size(); i++) {
                OnRecordsChangedListener listener = listenerArrayList.get(i);
                if(listener!=null) {
                    ArrayList<Record> changedRecords = new ArrayList<>();
                    changedRecords.add(record);
                    listener.onRecordListChanged(ACTION_REMOVE, changedRecords);
                }
            }
        }
    }

    //获取 状态为state的 record列表
    public ArrayList<Record> getRecords(int state) {
        ArrayList<Record> ret = new ArrayList<>();
        for(int i=0; i<recordArrayList.size(); i++) {
            Record record = recordArrayList.get(i);
            if(record.getState() == state) {
                ret.add(record);
            }
        }
        return ret;
    }

    public Record findRecord(String path, int state, boolean isSend) {
        ArrayList<Record> recordList = getRecords(state);
        for(int i=0; i<recordList.size(); i++) {
            Record record = recordList.get(i);
            if(record.getTransport_direction() == Record.DIRECTION_OUT && record.getPath().equals(path)) {
                return record;
            }
        }
        return null;
    }

    //数据库操作
    private void addToDB(Record record) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.InstanceColumns.NAME, record.getName());

        Uri uri = resolver.insert(Constants.InstanceColumns.CONTENT_URI, contentValues);
        this.uris.put(record, uri);
    }

    //record 状态改变
    @Override
    public void onStateChanged(Record record, int state_old, int state_new) {
        //将record位置调到最新
        recordArrayList.remove(record);
        recordArrayList.add(0, record);
        //回调监听
        for(int i=0; i<listenerArrayList.size(); i++) {
            OnRecordsChangedListener listener = listenerArrayList.get(i);
            if(listener!=null) {
                listener.onRecordChanged(record, state_old, state_new);
            }
        }
    }

    //记录发生改变的监听
    public interface OnRecordsChangedListener {
        public abstract void onRecordListChanged(int action, ArrayList<Record> changedRecordList);
        public abstract void onRecordChanged(Record record, int state_old, int state_new);
    }
}
