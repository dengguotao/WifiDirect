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
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.TextView;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyGridViewAdapter;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by ckt on 2/29/16.
 */
public class ApplicationFragment extends Fragment implements AdapterView.OnItemClickListener, MainActivity.OnSendFileListChangeListener {
    private MyGridViewAdapter adapter;
    private GridView gridView;
    private PackageManager manager;
    private TextView applicationNumber;
    List<PackageInfo> packageInfoList = new ArrayList<>();
    List<PackageInfo> apps = new ArrayList<>();
    ArrayList<String> mNameList = new ArrayList<>();
    ArrayList<String> mPathList = new ArrayList<>();
    ArrayList<Drawable> mIconList = new ArrayList<>();
    ArrayList<Boolean> mCheckBoxList = new ArrayList<>();

    //用来还原gridview的位置
    private int gridViewState_pos = 0;

    public static final int LOAD_DATA_FINISHED=0;

    private Handler handler = new Handler() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_DATA_FINISHED:
                    gridView.setAdapter(adapter);
                    gridView.setSelection(gridViewState_pos);
                    applicationNumber.setText("已安装应用" + "(" + mNameList.size() + ")");
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
        if(adapter == null) {//first loaded data
            loadData();
        }else {
            handler.sendEmptyMessage(LOAD_DATA_FINISHED);
        }
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
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
                        mIconList.add(manager.getApplicationIcon(packageInfo.applicationInfo));
                        mPathList.add(packageInfo.applicationInfo.sourceDir);
//                Log.i(WifiP2pHelper.TAG, "packageInfo.applicationInfo.sourceDir=" + packageInfo.applicationInfo.sourceDir);
//                Log.i(WifiP2pHelper.TAG, "packageInfo.applicationInfo.publicSourceDir="+packageInfo.applicationInfo.publicSourceDir);
                        mCheckBoxList.add(false);
                    } else {
                        //系统应用
                    }
                }
                adapter = new MyGridViewAdapter(getActivity(), mNameList, mIconList, mCheckBoxList);
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
        if(mCheckBoxList.get(position)) {//checked--->add to sendfile-list
            activity.addFileToSendFileList(mPathList.get(position));
        }else {//unchecked--->remove from sendfile-list
            activity.removeFileFromSendFileList(mPathList.get(position));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSendFileListChange(ArrayList<String> sendFiles, int num) {
        if(adapter!=null) {
            ArrayList<Boolean> checkList = adapter.getmCheckBoxList();
            String data = sendFiles.toString();
            Log.d(WifiP2pHelper.TAG, data);
            for(int i=0; i<checkList.size(); i++) {
                String temp = mPathList.get(i);
                if(data.contains(temp)) {
                    checkList.set(i, true);
                }else {
                    checkList.set(i, false);
                }
            }
            adapter.notifyDataSetChanged();
        }
    }
}
