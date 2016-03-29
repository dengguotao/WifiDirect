package com.ckt.io.wifidirect.fragment;

import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyExpandableListViewAdapter;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.provider.Record;
import com.ckt.io.wifidirect.provider.RecordManager;
import com.ckt.io.wifidirect.utils.FileTypeUtils;
import com.ckt.io.wifidirect.utils.LogUtils;

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
        boolean isFirstOnCrate = false;
        if(adapter == null) {
            //添加分组,但是暂时不添加每个分组里面的item
            ArrayList<String> names = new ArrayList<>();
            ArrayList<ArrayList<Record>> records = new ArrayList<>();
            for(int i=0; i<groups.length; i++) {
                ArrayList<Record> recordList = new ArrayList<>();
//                recordList.addAll(getGroupRecordFromRecordManager(groups[i]));
                names.add(getResources().getString(groups[i]));
                records.add(recordList);
            }
            adapter = new MyExpandableListViewAdapter(getContext(), names, records);
            RecordManager.getInstance(getContext()).addOnRecordsChangedListener(this);//注册监听
            isFirstOnCrate = true;
        }
        expandableListView.setChildDivider(getContext().getResources().getDrawable(R.drawable.expandablelistview_child_divider));
        expandableListView.setAdapter(adapter);
        //默认展开 正在发送 和 正在接收的  分组
        if(isFirstOnCrate) {
            expandGroup(SENDING_GROUP);
            expandGroup(RECEVING_GROUP);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateExpandableListView();
    }

    //更新每个分组
    public void updateExpandableListView() {
        if(adapter == null) return;
        for(int i=0; i<adapter.getGroupList().size(); i++) {
            MyExpandableListViewAdapter.ExpandableListViewGroup group = adapter.getGroupList().get(i);
            group.getRecordList().clear();
            group.getRecordList().addAll(getGroupRecordFromRecordManager(groups[i]));
        }
        adapter.notifyDataSetChanged();
    }

    public ArrayList<Record> getGroupRecordFromRecordManager(int id) {
        RecordManager manager = RecordManager.getInstance(getContext());
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

    //展开对应的分组
    public void expandGroup(int nameStrId) {
        for(int i=0; i<adapter.getGroupList().size(); i++) {
            String s = getResources().getString(nameStrId);
            MyExpandableListViewAdapter.ExpandableListViewGroup group = adapter.getGroupList().get(i);
            if(group.getName().equals(s)) {
                expandableListView.expandGroup(i, true);
                break;
            }
        }
    }

    public MyExpandableListViewAdapter.ExpandableListViewGroup getOwnerGroup(Record record) {
        int groupId = -1;
        switch (record.getState()) {
            case Record.STATE_TRANSPORTING:
            case Record.STATE_WAIT_FOR_TRANSPORT:
                if (record.isSend()) {
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
        if(adapter == null) return;
        if(action == RecordManager.ACTION_ADD) {
           for(int i=0; i<changedRecordList.size(); i++) {
               Record record = changedRecordList.get(i);
               int state = record.getState();
               if(state == Record.STATE_WAIT_FOR_TRANSPORT || state == Record.STATE_TRANSPORTING) {
                   MyExpandableListViewAdapter.ExpandableListViewGroup group = getOwnerGroup(record);
                   if(this.isResumed()) {
                       expandableListView.expandGroup(adapter.getGroupPostion(group), true);
                   }
               }
           }
        }

        updateExpandableListView();
        LogUtils.i(WifiP2pHelper.TAG, "onRecordListChanged");
    }

    @Override
    public void onRecordChanged(Record record, int state_old, int state_new) {
        updateExpandableListView();
        LogUtils.i(WifiP2pHelper.TAG, "onRecordChanged");
    }
}
