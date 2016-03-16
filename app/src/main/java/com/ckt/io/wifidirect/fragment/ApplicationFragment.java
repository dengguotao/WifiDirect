package com.ckt.io.wifidirect.fragment;

import android.annotation.TargetApi;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.TextView;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyGridViewAdapter;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.utils.DrawableLoaderUtils;
import com.ckt.io.wifidirect.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by ckt on 2/29/16.
 */
public class ApplicationFragment extends Fragment implements AdapterView.OnItemClickListener,
        MainActivity.OnSendFileListChangeListener,
        DrawableLoaderUtils.OnLoadFinishedListener {
    private MyGridViewAdapter adapter;
    private GridView gridView;
    private PackageManager manager;
    private TextView applicationNumber;
    List<PackageInfo> packageInfoList = new ArrayList<>();
    List<PackageInfo> apps = new ArrayList<>();
    ArrayList<String> mNameList = new ArrayList<>();
    ArrayList<String> mPathList = new ArrayList<>();
    ArrayList<Object> mIconList = new ArrayList<>();
    ArrayList<Boolean> mCheckBoxList = new ArrayList<>();

    private DrawableLoaderUtils drawableLoaderUtils; //用来异步加载图片

    //用来还原gridview的位置
    private int gridViewState_pos = 0;

    public static final int LOAD_DATA_FINISHED = 0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_DATA_FINISHED:
                    gridView.setAdapter(adapter);
                    gridView.setSelection(gridViewState_pos);
                    applicationNumber.setText(getResources().getString(R.string.installed_apps) + "(" + mNameList.size() + ")");
                    if (drawableLoaderUtils != null) {
                        for (int i = 0; i < mNameList.size(); i++) {
                            //启动异步任务加载图片,加载完成一个图片后会调用onLoadOneFinished
                            drawableLoaderUtils.load(getContext(), mPathList.get(i));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(WifiP2pHelper.TAG, "ApplicationFragment-->onCreateView");
        View view = inflater.inflate(R.layout.application_layout, container, false);
        gridView = (GridView) view.findViewById(R.id.id_grid_view);
        applicationNumber = (TextView) view.findViewById(R.id.id_application_number);
        manager = getActivity().getPackageManager();
        if (adapter == null) {//first loaded data
            LogUtils.i(WifiP2pHelper.TAG, "ApplicationFragment first oncreateView()");
            drawableLoaderUtils = DrawableLoaderUtils.getInstance(this);
            loadData();
        } else {
            handler.sendEmptyMessage(LOAD_DATA_FINISHED);
        }
        gridView.setOnItemClickListener(this);
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) { //stop
                    gridView.setTag(true);
                    ((BaseAdapter) gridView.getAdapter()).notifyDataSetChanged();
                } else { //scrolling
                    gridView.setTag(false);
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });
        return view;
    }

    public void loadData() {
        new Thread() {
            @Override
            public void run() {
                packageInfoList = manager.getInstalledPackages(0);
                for (int i = 0; i < packageInfoList.size(); i++) {
                    PackageInfo packageInfo = packageInfoList.get(i);
                    if ((packageInfo.applicationInfo.flags & packageInfo.applicationInfo.FLAG_SYSTEM) <= 0) {
                        //第三方应用
                        apps.add(packageInfo);
                        mNameList.add(manager.getApplicationLabel(packageInfo.applicationInfo).toString());
//                        mIconList.add(manager.getApplicationIcon(packageInfo.applicationInfo));
                        mPathList.add(packageInfo.applicationInfo.sourceDir);
                        mIconList.add(R.drawable.apk_icon);//先默认显示apk_icon
                        mCheckBoxList.add(false);
                    } else {
                    }
                }
                adapter = new MyGridViewAdapter(getActivity(), mNameList, mIconList, mCheckBoxList, 60);
                handler.sendEmptyMessage(LOAD_DATA_FINISHED);
            }
        }.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        gridViewState_pos = gridView.getFirstVisiblePosition();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mCheckBoxList = adapter.getmCheckBoxList();
        mCheckBoxList.set(position, !mCheckBoxList.get(position));
        MainActivity activity = (MainActivity) getActivity();
        if (mCheckBoxList.get(position)) {//checked--->add to sendfile-list
            activity.addFileToSendFileList(mPathList.get(position));
        } else {//unchecked--->remove from sendfile-list
            activity.removeFileFromSendFileList(mPathList.get(position));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSendFileListChange(ArrayList<String> sendFiles, int num) {
        if (adapter != null) {
            ArrayList<Boolean> checkList = adapter.getmCheckBoxList();
            String data = sendFiles.toString();
            Log.d(WifiP2pHelper.TAG, data);
            for (int i = 0; i < checkList.size(); i++) {
                String temp = mPathList.get(i);
                if (data.contains(temp)) {
                    checkList.set(i, true);
                } else {
                    checkList.set(i, false);
                }
            }
            adapter.notifyDataSetChanged();
        }
    }

    //加载完一张图片的回调
    @Override
    public void onLoadOneFinished(String path, Object obj, boolean isAllFinished) {
        int index = mPathList.indexOf(path);
        if (index >= 0) {
            mIconList.set(index, obj);
        }
        if (gridView.getTag() == null || !(boolean) gridView.getTag()) { //gridview没有滑动
            if(index == mPathList.size()/2 || isAllFinished) {
                ((BaseAdapter) (gridView.getAdapter())).notifyDataSetChanged();
            }
        }
    }
}
