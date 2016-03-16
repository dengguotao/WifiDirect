package com.ckt.io.wifidirect.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.ckt.io.wifidirect.R;

/**
 * Created by admin on 2016/3/16.
 */
public class HistoryFragment extends Fragment {
    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history_fragment, container, false);
        listView = (ListView) view.findViewById(R.id.id_history_list);
        return view;
    }
}
