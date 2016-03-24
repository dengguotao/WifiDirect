package com.ckt.io.wifidirect.fragment;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyExpandableListViewAdapter;
import com.ckt.io.wifidirect.provider.Record;
import com.ckt.io.wifidirect.provider.RecordManager;
import com.ckt.io.wifidirect.utils.FileTypeUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by admin on 2016/3/16.
 */
public class HistoryFragment extends Fragment implements RecordManager.OnRecordsChangedListener{

    public static final int RECEVING_GROUP = R.string.group_recevieing_task;
    public static final int SENDING_GROUP = R.string.group_sending_task;
    public static final int FINISHED_GROUP = R.string.group_finished_task;
    public static final int FAILED_GROUP = R.string.group_failed_task;
    public static final int PAUSED_GROUP = R.string.group_paused_task;
    static final int groups [] = new int[]{SENDING_GROUP, RECEVING_GROUP, PAUSED_GROUP, FAILED_GROUP, FINISHED_GROUP};

    private ExpandableListView expandableListView;
    private MyExpandableListViewAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history_fragment, container, false);
        expandableListView = (ExpandableListView) view.findViewById(R.id.expand_listview);
        if(adapter == null) {
            //添加分组,但是暂时不添加每个分组里面的item
            ArrayList<String> names = new ArrayList<>();
            ArrayList<ArrayList<Record>> records = new ArrayList<>();
            for(int i=0; i<groups.length; i++) {
                ArrayList<Record> recordList = new ArrayList<>();
                recordList.addAll(getGroupRecordFromRecordManager(groups[i]));
                names.add(getResources().getString(groups[i]));
                records.add(recordList);
            }
            adapter = new MyExpandableListViewAdapter(getContext(), names, records);
            RecordManager.getInstance().addOnRecordsChangedListener(this);//注册监听
        }
        expandableListView.setChildDivider(getContext().getResources().getDrawable(R.drawable.expandablelistview_child_divider));
        expandableListView.setAdapter(adapter);
        return view;
    }

    public ArrayList<Record> getGroupRecordFromRecordManager(int id) {
        RecordManager manager = RecordManager.getInstance();
        ArrayList<Record> ret = null;
        if(id == SENDING_GROUP) {
            ret = new ArrayList<>();
            ArrayList<Record> temp = manager.getRecords(Record.STATE_TRANSPORTING);
            for(int i=0; i<temp.size(); i++) {
                Record record = temp.get(i);
                if(record.isSend()) {
                    ret.add(record);
                }
            }
            temp = manager.getRecords(Record.STATE_WAIT_FOR_TRANSPORT);
            for(int i=0; i<temp.size(); i++) {
                Record record = temp.get(i);
                if(record.isSend()) {
                    ret.add(record);
                }
            }
        }else if(id == RECEVING_GROUP) {
            ret = new ArrayList<>();
            ArrayList<Record> temp = manager.getRecords(Record.STATE_TRANSPORTING);
            for(int i=0; i<temp.size(); i++) {
                Record record = temp.get(i);
                if(!record.isSend()) {
                    ret.add(record);
                }
            }
            temp = manager.getRecords(Record.STATE_WAIT_FOR_TRANSPORT);
            for(int i=0; i<temp.size(); i++) {
                Record record = temp.get(i);
                if(!record.isSend()) {
                    ret.add(record);
                }
            }
        }else if(id == PAUSED_GROUP) {
            ret = manager.getRecords(Record.STATE_PAUSED);
        }else if(id == FAILED_GROUP) {
            ret = manager.getRecords(Record.STATE_FAILED);
        }else if(id == FINISHED_GROUP) {
            ret = manager.getRecords(Record.STATE_FINISHED);
        }
        return ret;
    }

    //获取expandablelistview对应id的分组
    public MyExpandableListViewAdapter.ExpandableListViewGroup findGroup(int id) {
        return adapter.getGroupByName(getContext().getResources().getString(id));
    }

    //从adapter里面查找对应的发送记录
    public Record findRecord(int groupId, String recordPath) {
        MyExpandableListViewAdapter.ExpandableListViewGroup group = findGroup(groupId);
        if(group == null) return null;
        ArrayList<Record> records = group.getRecordList();
        for(int i=0; i<records.size(); i++) {
            Record temp = records.get(i);
            if(temp.equals(recordPath)) {
                return temp;
            }
        }
        return null;
    }

    public Record findRecord(MyExpandableListViewAdapter.ExpandableListViewGroup group, String recordPath) {
        if(group == null) return null;
        ArrayList<Record> records = group.getRecordList();
        for(int i=0; i<records.size(); i++) {
            Record temp = records.get(i);
            if(temp.equals(recordPath)) {
                return temp;
            }
        }
        return null;
    }

    public void updateSendRecordState(MyExpandableListViewAdapter.ExpandableListViewGroup group, Record record, int state) {
        if(group == null || record == null) return;
        MyExpandableListViewAdapter.ExpandableListViewGroup tempGroup = null;
        switch (state) {
            case Record.STATE_FAILED:
                tempGroup = findGroup(FAILED_GROUP);
                break;
            case Record.STATE_FINISHED:
                tempGroup = findGroup(FINISHED_GROUP);
                break;
            case Record.STATE_WAIT_FOR_TRANSPORT:
                tempGroup = findGroup(PAUSED_GROUP);
                break;
            case Record.STATE_TRANSPORTING:
                tempGroup = findGroup(SENDING_GROUP);
                break;
            default:

                break;
        }
        //如果record没有在 group 组里面
        //①从原来的组里面移除   ②新增到已失败里面
        if(tempGroup != null && group != tempGroup) {
            tempGroup.getRecordList().add(0, record);
            group.getRecordList().remove(record);
        }else {
            record.setState(state);
        }
        adapter.notifyDataSetChanged();
    }

    public MyExpandableListViewAdapter.ExpandableListViewGroup getOwnerGroup(Record record) {
        int groupId = -1;
        switch (record.getState()) {
            case Record.STATE_TRANSPORTING:
            case Record.STATE_WAIT_FOR_TRANSPORT:
                if (record.getTransport_direction() == Record.TRANSPORT_DERICTION_SEND) {
                    groupId = SENDING_GROUP;
                } else {
                    groupId = RECEVING_GROUP;
                }
                break;
            case Record.STATE_FAILED:
                groupId = FAILED_GROUP;
                break;
            case Record.STATE_FINISHED:
                groupId = FINISHED_GROUP;
                break;
            case Record.STATE_PAUSED:
                groupId = PAUSED_GROUP;
                break;
        }
        return adapter.getGroupByName(getResources().getString(groupId));
    }

    //************下面是一些回调**************************************************************
    @Override
    public void onRecordListChanged(int action, ArrayList<Record> changedRecordList) {
        //获取 "接收中" 分组--->包含了正在发送和等待发送的
        for(int i=0; i<changedRecordList.size(); i++) {
            Record record = changedRecordList.get(i);
            MyExpandableListViewAdapter.ExpandableListViewGroup group = getOwnerGroup(record);
            if(action == RecordManager.ACTION_ADD) {
                group.getRecordList().add(record);
            }else if(action == RecordManager.ACTION_REMOVE){
                group.getRecordList().remove(record);
            }
            expandableListView.expandGroup(adapter.getGroupPostion(group), true);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRecordChanged(Record record, int state_old, int state_new) {

    }
}
