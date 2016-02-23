package com.ckt.io.fragment;

import java.io.File;
import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.ckt.io.R;

public class FileExplorerFragment extends Fragment{
	private File mDir;
	private ArrayList<File> fileList;
	public FileExplorerFragment(File dir) {
		this.mDir = dir;
		if(this.mDir == null) {
			mDir = new File("/");
		}
		this.fileList = new ArrayList<>();
		if(this.mDir.canRead()) {
			
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			 ViewGroup container,  Bundle savedInstanceState) {

		return inflater.inflate(R.layout.fragment_file_explorer, container, false);
	}
	
	class MyListViewAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
