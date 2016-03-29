package com.ckt.io.wifidirect.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.ckt.io.wifidirect.Constants;
import com.ckt.io.wifidirect.utils.FileResLoaderUtils;

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
            synchronized (RecordManager.class) {
                if(manager == null) {
                    manager = new RecordManager(context);
                    //从数据库中读取数据
                    manager.readDB();
                }
            }
        }
        return manager;
    }

    public RecordManager(Context context) {
        this.context = context;
    }

    //新增正在发送的记录
    public void addNewSendingRecord(ArrayList<File> list, String mac) {
        ArrayList<Record> changedRecords = new ArrayList<>();
        for(int i=0; i<list.size(); i++) {
            File f = list.get(i);
            String name = FileResLoaderUtils.getFileName(f.getPath());
            Record record = new Record(
                    name,
                    f.getPath(),
                    f.length(),
                    0,
                    Record.STATE_WAIT_FOR_TRANSPORT,
                    Record.DIRECTION_OUT,
                    mac);
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
    public Record addNewRecevingRecord(File f, String name, String mac) {
        Record record = new Record(
                name,
                f.getPath(),
                f.length(),
                0,
                Record.STATE_TRANSPORTING,
                Record.DIRECTION_IN,
                mac);
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
            recordArrayList.add(0, record);
            //添加记录到数据库
            addToDB(record);
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
            //从数据库中删除
            deleteDB(record);
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

    //清除所有记录
    public void clearAllRecord() {
        while (recordArrayList.size() != 0) {
            Record record = recordArrayList.get(0);
            remove(record);
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
            if(record.isSend() == isSend && record.getPath().equals(path)) {
                return record;
            }
        }
        return null;
    }

    //数据库操作
    private void addToDB(Record record) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        /*public static final String ID = "_id";
        public static final String NAME = "_name";
        public static final String LENGTH = "_length";
        public static final String TRANSFER_LENGTH = "_tlength";
        public static final String STATE = "_state";
        public static final String TRANSFER_DIRECTION = "_direction";
        public static final String TRANSFER_MAC = "_mac";*/
        contentValues.put(Constants.InstanceColumns.ID, record.getId());
        contentValues.put(Constants.InstanceColumns.NAME, record.getName());
        contentValues.put(Constants.InstanceColumns.PATH, record.getPath());
        contentValues.put(Constants.InstanceColumns.LENGTH, record.getLength());
        contentValues.put(Constants.InstanceColumns.TRANSFER_LENGTH, record.getTransported_len());
        contentValues.put(Constants.InstanceColumns.STATE, record.getState());
        contentValues.put(Constants.InstanceColumns.TRANSFER_DIRECTION, record.getTransport_direction());
        contentValues.put(Constants.InstanceColumns.TRANSFER_MAC, record.getMac());
        Uri uri = resolver.insert(Constants.InstanceColumns.CONTENT_URI, contentValues);
        this.uris.put(record, uri);
    }

    //更新数据库中的记录
    private void updateDB(Record record) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.InstanceColumns.ID, record.getId());
        contentValues.put(Constants.InstanceColumns.NAME, record.getName());
        contentValues.put(Constants.InstanceColumns.PATH, record.getPath());
        contentValues.put(Constants.InstanceColumns.LENGTH, record.getLength());
        contentValues.put(Constants.InstanceColumns.TRANSFER_LENGTH, record.getTransported_len());
        contentValues.put(Constants.InstanceColumns.STATE, record.getState());
        contentValues.put(Constants.InstanceColumns.TRANSFER_DIRECTION, record.getTransport_direction());
        contentValues.put(Constants.InstanceColumns.TRANSFER_MAC, record.getMac());
        Uri uri = this.uris.get(record);
        if(uri != null) {
            resolver.update(uri, contentValues, null, null);
        }
    }

    //从数据库读取数据
    private void readDB() {
        this.recordArrayList.clear();
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(
                Constants.InstanceColumns.CONTENT_URI,
                new String[]{
                        Constants.InstanceColumns.ID,
                        Constants.InstanceColumns.NAME,
                        Constants.InstanceColumns.PATH,
                        Constants.InstanceColumns.LENGTH,
                        Constants.InstanceColumns.TRANSFER_LENGTH,
                        Constants.InstanceColumns.STATE,
                        Constants.InstanceColumns.TRANSFER_DIRECTION,
                        Constants.InstanceColumns.TRANSFER_MAC},
                null,
                null,
                null);
        if(cursor.moveToFirst()) {
            do {
                Record record = new Record(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        cursor.getLong(4),
                        cursor.getInt(5),
                        cursor.getInt(6),
                        cursor.getString(7)
                );
                this.recordArrayList.add(record);
            }while (cursor.moveToNext());
        }
    }

    //删除一条数据库记录
    private void deleteDB(Record record) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = this.uris.get(record);
        if(uri != null) {
            resolver.delete(uri, null, null);
        }
    }

    //record 状态改变
    @Override
    public void onStateChanged(Record record, int state_old, int state_new) {
        //将record位置调到最新
        recordArrayList.remove(record);
        recordArrayList.add(0, record);
        //更新数据库
        updateDB(record);
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
