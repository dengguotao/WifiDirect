package com.ckt.io.wifidirect.fragment;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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

import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyGridViewAdapter;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by ckt on 2/29/16.
 */
public class ApplicationFragment extends Fragment implements AdapterView.OnItemClickListener {
    private MyGridViewAdapter adapter;
    private GridView gridView;
    private PackageManager manager;
    private TextView applicationNumber;
    List<PackageInfo> packageInfoList;
    List<PackageInfo> apps;
    ArrayList<String> mNameList;
    ArrayList<Drawable> mIconList;
    ArrayList<Boolean> mCheckBoxList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.application_layout, container, false);
        manager = getActivity().getPackageManager();
        packageInfoList = manager.getInstalledPackages(0);
        apps = new ArrayList<>();
        mNameList = new ArrayList<>();
        mIconList = new ArrayList<>();
        mCheckBoxList = new ArrayList<>();
        gridView = (GridView) view.findViewById(R.id.id_grid_view);
        applicationNumber = (TextView) view.findViewById(R.id.id_application_number);

        for (int i = 0; i < packageInfoList.size(); i++) {
            PackageInfo packageInfo = packageInfoList.get(i);
            if ((packageInfo.applicationInfo.flags & packageInfo.applicationInfo.FLAG_SYSTEM) <= 0) {
                //第三方应用
                apps.add(packageInfo);
                mNameList.add(manager.getApplicationLabel(packageInfo.applicationInfo).toString());
                mIconList.add(manager.getApplicationIcon(packageInfo.applicationInfo));
                mCheckBoxList.add(false);
            } else {
            }
        }
        adapter = new MyGridViewAdapter(getActivity(), mNameList, mIconList, mCheckBoxList);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        applicationNumber.setText("已安装应用" + "(" + mNameList.size() + ")");
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ArrayList<Boolean> checkList = adapter.getmCheckBoxList();
        checkList.set(position, !checkList.get(position));
        MyGridViewAdapter.ItemViewTag viewTag = (MyGridViewAdapter.ItemViewTag) view.getTag();
        viewTag.mCheckBox.setVisibility(checkList.get(position) ? View.VISIBLE : View.GONE);
        viewTag.mCheckBox.setChecked(checkList.get(position));
    }
}
