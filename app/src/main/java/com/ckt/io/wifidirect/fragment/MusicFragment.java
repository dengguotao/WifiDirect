package com.ckt.io.wifidirect.fragment;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyListViewAdapter;
import com.ckt.io.wifidirect.utils.AudioUtils;
import com.ckt.io.wifidirect.utils.Song;

import java.util.ArrayList;


/**
 * Created by ckt on 2/29/16.
 */
public class MusicFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private ArrayList<Song> songList;
    private ArrayList<String> nameList;
    private ArrayList<Drawable> iconList;
    private ArrayList<Boolean> checkBoxList;
    private ListView listView;
    private TextView refresh;
    MyListViewAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.music_layout, container, false);
        nameList = new ArrayList<>();
        iconList = new ArrayList<>();
        checkBoxList = new ArrayList<>();
        listView = (ListView) view.findViewById(R.id.id_adapter_list_view);
        refresh = (TextView) view.findViewById(R.id.id_music_refresh);
        songList = AudioUtils.getAllSongs(getActivity());

        for (Song song : songList) {
            nameList.add(song.getTitle());
            iconList.add(new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.icon)));
            checkBoxList.add(false);
        }
        adapter = new MyListViewAdapter(getActivity(), nameList, iconList, checkBoxList);
        listView.setAdapter(adapter);
        refresh.setOnClickListener(this);
        listView.setOnItemClickListener(this);
        return view;
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
}
