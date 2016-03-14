package com.ckt.io.wifidirect.fragment;

import android.app.Notification;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyListViewAdapter;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.utils.AudioUtils;
import com.ckt.io.wifidirect.utils.Song;

import java.util.ArrayList;


/**
 * Created by ckt on 2/29/16.
 */
public class MusicFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener, MainActivity.OnSendFileListChangeListener {
    private ArrayList<Song> songList = new ArrayList<>();
    private ArrayList<String> nameList = new ArrayList<>();
    private ArrayList<Drawable> iconList = new ArrayList<>();
    private ArrayList<Boolean> checkBoxList = new ArrayList<>();
    private ListView listView;
    private TextView refresh;
    MyListViewAdapter adapter;

    //用来还原listview的位置
    private int listViewState_pos = 0;
    private int listViewState_top = 0;

    public static final int LOAD_DATA_FINISHED=0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_DATA_FINISHED:
                    listView.setAdapter(adapter);
                    listView.setSelectionFromTop(listViewState_pos, listViewState_top);
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(WifiP2pHelper.TAG, "MusicFragment-->onCreateView()");
        View view = inflater.inflate(R.layout.music_layout, container, false);
        listView = (ListView) view.findViewById(R.id.id_adapter_list_view);
        refresh = (TextView) view.findViewById(R.id.id_music_refresh);
        songList = AudioUtils.getAllSongs(getActivity());
        if(adapter == null) {
            loadData();
        }else {
            handler.sendEmptyMessage(LOAD_DATA_FINISHED);
        }
        refresh.setOnClickListener(this);
        listView.setOnItemClickListener(this);
        return view;
    }

    public void loadData() {
        new Thread() {
            @Override
            public void run() {
                for (Song song : songList) {
                    nameList.add(song.getTitle());
                    iconList.add(new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.icon)));
                    checkBoxList.add(false);
                }
                adapter = new MyListViewAdapter(getActivity(), nameList, iconList, checkBoxList);
                handler.sendEmptyMessage(LOAD_DATA_FINISHED);
            }
        }.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(WifiP2pHelper.TAG, "MusicFragment-->onPause");
        listViewState_pos = listView.getFirstVisiblePosition();
        View child = listView.getChildAt(0);
        listViewState_top = child!=null? child.getTop() : 0;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_music_refresh:
                MediaScannerConnection.scanFile(getActivity(), new String[]{Environment
                        .getExternalStorageDirectory().getAbsolutePath()}, null, null);
                if (songList != null) {
                    songList.clear();
                    nameList.clear();
                    iconList.clear();
                }
                songList = AudioUtils.getAllSongs(getActivity());
                for (Song song : songList) {
                    nameList.add(song.getTitle());
                    iconList.add(new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.icon)));
                }
                adapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        checkBoxList = adapter.getmCheckBoxList();
        checkBoxList.set(position, !checkBoxList.get(position));
        MainActivity activity = (MainActivity) getActivity();
        if(checkBoxList.get(position)) {//checked--->add to sendfile-list
            activity.addFileToSendFileList(songList.get(position).getFileUrl());
        }else {//unchecked--->remove from sendfile-list
            activity.removeFileFromSendFileList(songList.get(position).getFileUrl());
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
                String temp = songList.get(i).getFileUrl();
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
