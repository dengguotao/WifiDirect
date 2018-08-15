package com.easy.transfer.fragment;

import android.content.Context;
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
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.ckt.io.transfer.MainActivity;
import com.ckt.io.transfer.R;
import com.easy.transfer.adapter.MyExpandableListViewAdapter;
import com.easy.transfer.p2p.WifiP2pHelper;
import com.easy.transfer.provider.Record;
import com.easy.transfer.provider.RecordManager;
import com.easy.transfer.utils.FileResLoaderUtils;
import com.easy.transfer.utils.FileTypeUtils;
import com.easy.transfer.utils.LogUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by admin on 2016/3/16.
 */
public class HistoryFragment extends Fragment implements
        RecordManager.OnRecordsChangedListener, FileResLoaderUtils.OnLoadFinishedListener{

    public static final int RECEVING_GROUP = R.string.group_recevieing_task;
    public static final int SENDING_GROUP = R.string.group_sending_task;
    public static final int FINISHED_GROUP = R.string.group_finished_task;
    public static final int FAILED_GROUP = R.string.group_failed_task;
    public static final int PAUSED_GROUP = R.string.group_paused_task;
    static final int groups [] = new int[]{SENDING_GROUP, RECEVING_GROUP, PAUSED_GROUP, FAILED_GROUP, FINISHED_GROUP};

    private ExpandableListView expandableListView;
    private MyExpandableListViewAdapter adapter;

    private FileResLoaderUtils drawLoader;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtils.i(WifiP2pHelper.TAG, "HistoryFragment onCreateView");
        View view = inflater.inflate(R.layout.history_fragment, container, false);
        expandableListView = (ExpandableListView) view.findViewById(R.id.expand_listview);
        boolean isFirstOnCrate = false;
        if(adapter == null) {
            //��ӷ���,������ʱ�����ÿ�����������item
            ArrayList<Integer> groupIds = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();
            ArrayList<ArrayList<Record>> records = new ArrayList<>();
            for(int i=0; i<groups.length; i++) {
                ArrayList<Record> recordList = new ArrayList<>();
//                recordList.addAll(getGroupRecordFromRecordManager(groups[i]));
                groupIds.add(groups[i]);
                names.add(getResources().getString(groups[i]));
                records.add(recordList);
            }

            adapter = new MyExpandableListViewAdapter(getContext(), groupIds, names, records);
            RecordManager.getInstance(getContext()).addOnRecordsChangedListener(this);//ע�����
            isFirstOnCrate = true;
            drawLoader = FileResLoaderUtils.getInstance(this);
            loadDrawable();
        }
        expandableListView.setChildDivider(getContext().getResources().getDrawable(R.drawable.expandablelistview_child_divider));
        expandableListView.setAdapter(adapter);
        expandableListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) { //stop
                    expandableListView.setTag(false);
                    adapter.notifyDataSetChanged();
                } else { //scrolling
                    expandableListView.setTag(true);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        //Ĭ��չ�� ���ڷ��� �� ���ڽ��յ�  ����
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

    //����ÿ������
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

    //չ����Ӧ�ķ���
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
        return adapter.getGroupById(groupId);
    }

    public void loadDrawable() {
        if(getContext()!=null) {
            RecordManager recordManager = RecordManager.getInstance(getContext());
            for(int i=0; i<recordManager.getAllRecord().size(); i++) {
                Record record = recordManager.getAllRecord().get(i);
                drawLoader.load(getContext(), record.getPath());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LogUtils.i(WifiP2pHelper.TAG, "HistoryFragment onDestroyView");
    }

    //************������һЩ�ص�**************************************************************
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
        if(state_new == Record.STATE_FINISHED && getContext() != null) {
            drawLoader.load(getContext(), record.getPath());
        }
        updateExpandableListView();
        LogUtils.i(WifiP2pHelper.TAG, "onRecordChanged");
    }

    @Override
    public void onRecordDataChanged(Record record) {
        Object object = expandableListView.getTag();
        if(object == null || !(boolean)(object)) { //expandableListViewû�л���
            adapter.notifyDataSetChanged();
        }
    }

    //�������һ��ͼƬ�Ļص�
    @Override
    public void onLoadOneFinished(String path, Object obj, boolean isAllFinished) {
        Object object = expandableListView.getTag();
        if(object == null || !(boolean)(object)) { //expandableListViewû�л���
            adapter.notifyDataSetChanged();
        }
    }
}
