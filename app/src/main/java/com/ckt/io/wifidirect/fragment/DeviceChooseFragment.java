package com.ckt.io.wifidirect.fragment;

import java.util.ArrayList;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;


public class DeviceChooseFragment extends Fragment{
	private TextView txt_connected_info;
	private ListView listView;
	private ArrayList<WifiP2pDevice> deviceList = new ArrayList<WifiP2pDevice>();
	private WifiP2pHelper wifiP2pHelper;
	public DeviceChooseFragment() {
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			 ViewGroup container,  Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_device_choose, container, false);
		listView = (ListView) view.findViewById(R.id.listview);
		txt_connected_info = (TextView) view.findViewById(R.id.txt_connected_info);
		listView.setAdapter(new MyListViewAdapter());
		wifiP2pHelper = ((MainActivity)getActivity()).getWifiP2pHelper();
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				WifiP2pDevice device = deviceList.get(position);
				wifiP2pHelper.connectDevice(device);
			}
		});
		return view;
	}
	
	public void updateDeviceList(ArrayList<WifiP2pDevice> deviceList) {
		this.deviceList.clear();
		this.deviceList.addAll(deviceList);
		((BaseAdapter)this.listView.getAdapter()).notifyDataSetChanged();
	}
	public void updateConnectedInfo(boolean isServer) {
		if(isServer) {
			txt_connected_info.setText("server");
		}else {
			txt_connected_info.setText("client");
		}
	}
	
	class MyListViewAdapter extends BaseAdapter {

		class ViewHolder {
			TextView txt_device_name;
		}
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return deviceList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return deviceList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if(convertView == null) {
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_device_choose_listview_item, null);
				ViewHolder holder = new ViewHolder();
				holder.txt_device_name = (TextView) convertView.findViewById(R.id.txt_device_name);
				convertView.setTag(holder);
			}
			ViewHolder viewHolder = (ViewHolder) convertView.getTag();
			WifiP2pDevice device = deviceList.get(position);
			viewHolder.txt_device_name.setText(device.deviceName);
			return convertView;
		}
	}
}
