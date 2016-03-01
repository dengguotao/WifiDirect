package com.ckt.io.wifidirect.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;

/**
 * Created by ckt on 2/28/16.
 */
public class ToolBarFragment extends Fragment {
    private Toolbar toolbar;
    MainActivity activity;
    private Toolbar.OnMenuItemClickListener onMenuItemClick = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            return true;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.toolbar_layout, container, false);
        activity = (MainActivity) getActivity();
        toolbar = (Toolbar) view.findViewById(R.id.id_toolbar_layout);
        toolbar.setTitle("Wifi Direct");
        toolbar.setSubtitle("This is a test");
        toolbar.setLogo(R.drawable.ic_launcher);
        activity.setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(onMenuItemClick);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        return view;
    }
}
