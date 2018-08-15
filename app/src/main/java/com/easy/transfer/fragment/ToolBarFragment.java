package com.easy.transfer.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;

import com.easy.transfer.MainActivity;
import com.easy.transfer.R;

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
