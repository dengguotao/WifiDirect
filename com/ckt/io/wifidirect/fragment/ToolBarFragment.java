package com.easy.transfer.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;

import com.ckt.io.transfer.MainActivity;
import com.ckt.io.transfer.R;
import com.easy.transfer.p2p.WifiP2pHelper;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by ckt on 2/28/16.
 */
public class ToolBarFragment extends Fragment {
    private Toolbar toolbar;
    MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.toolbar_layout, container, false);
        activity = (MainActivity) getActivity();
        toolbar = (Toolbar) view.findViewById(R.id.id_toolbar_layout);
        activity.setSupportActionBar(toolbar);
        return view;
    }
}
