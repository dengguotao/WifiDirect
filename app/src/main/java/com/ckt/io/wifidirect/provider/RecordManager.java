package com.ckt.io.wifidirect.provider;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by admin on 2016/3/24.
 */
public class RecordManager implements Record.OnStateChangeListener{
    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;
    public static RecordManager manager;

    private ArrayList<Record> recordArrayList = new ArrayList<>();
    private ArrayList<OnRecordsChangedListener> listenerArrayList = new ArrayList<>();

    public static RecordManager getInstance() {
        if(manager == null) {
            manager = new RecordManager();
        }
        return manager;
    }

    public void addOnRecordsChangedListener(OnRecordsChangedListener listener) {
        if(!listenerArrayList.contains(listener)) {
            listenerArrayList.add(listener);
        }
    }

    public void removeOnRecordsChangedListener(OnRecordsChangedListener listener) {
        listenerArrayList.remove(listener);
    }

    //新增record
    public void add(Record record) {
        if(!recordArrayList.contains(record)) {
            recordArrayList.add(record);
            //回调监听
            for(int i=0; i<listenerArrayList.size(); i++) {
                OnRecordsChangedListener listener = listenerArrayList.get(i);
                if(listener != null) {
                    ArrayList<Record> changedRecords = new ArrayList<>();
                    changedRecords.add(record);
                    listener.onRecordListChanged(ACTION_ADD, changedRecords);
                }
            }
        }
    }

    //新增正在发送的记录
    public void addNewSendingRecord(ArrayList<File> list) {
        for(int i=0; i<list.size(); i++) {
            File f = list.get(i);
            Record record = new Record(
                    f.getPath(),
                    f.length(),
                    0,
                    Record.STATE_WAIT_FOR_TRANSPORT,
                    0,
                    true);
            this.add(record);
        }
    }

    //移除
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

    //record 状态改变
    @Override
    public void onStateChanged(Record record, int state_old, int state_new) {
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
