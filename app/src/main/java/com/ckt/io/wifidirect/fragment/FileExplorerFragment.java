package com.ckt.io.wifidirect.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.utils.ApkUtils;
import com.ckt.io.wifidirect.utils.ToastUtils;


@SuppressLint("ValidFragment")
public class FileExplorerFragment extends Fragment implements
		OnItemClickListener {

	private HashMap<String, Drawable> drawableHashMapCathe = new HashMap<>(); //cathe the loaded img
	private ArrayList<State> stateList = new ArrayList<State>();

	private ViewGroup lin_no_file;
	private ListView listView;
	private File mDir;
	private ArrayList<File> fileList;

	private boolean isListViewScrolling = false;
	private boolean isUpdateListViewAfterStopScrolling = false;

	private Handler handler = new Handler();
	public FileExplorerFragment(File dir) {
		this.mDir = dir;
		if (this.mDir == null) {
			mDir = new File("/");
		}
		this.fileList = new ArrayList<File>();
	}

	private ArrayList<File> sort(ArrayList<File> list) {
		ArrayList<File> ret = new ArrayList<File>();
		while (list.size() > 0) {
			File f = list.get(0);
			for (int j = 1; j < list.size(); j++) {
				File tmp = list.get(j);
				if (f.getName().compareTo(tmp.getName()) > 0) {
					f = tmp;
				}
			}
			list.remove(f);
			ret.add(f);
		}
		return ret;
	}

	private void updateView(State state) {
		if (state != null) {
			mDir = state.dir;
		}
		fileList.clear();
		if (mDir.canRead()) {
			ArrayList<File> tempFolderList = new ArrayList<File>(); // 暂时保存文件夹
			ArrayList<File> tempFileList = new ArrayList<File>(); // 用来暂时保存文件
			File fs[] = mDir.listFiles();
			for (File temp : fs) {
				if (temp.getName().startsWith(".")) {
					continue;
				}
				if (temp.isFile()) {
					tempFileList.add(temp);
				} else {
					tempFolderList.add(temp);
				}
			}
			fileList.addAll(sort(tempFolderList));
			fileList.addAll(sort(tempFileList));
		}
		MyListViewAdapter adapter = (MyListViewAdapter) listView
				.getAdapter();
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
		if (state != null) {
			listView.setSelectionFromTop(state.pos, state.top);
		}
		if (fileList.size() != 0) {
			lin_no_file.setVisibility(View.GONE);
		} else {
			lin_no_file.setVisibility(View.VISIBLE);
		}
	}

	public boolean back() {
		if (stateList.size() == 0) {
			return false;
		} else {
			State state = stateList.get(stateList.size() - 1);
			stateList.remove(stateList.size() - 1);
			updateView(state);
			return true;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_file_explorer,
				container, false);
		lin_no_file = (ViewGroup) view.findViewById(R.id.lin_no_file);
		listView = (ListView) view.findViewById(R.id.listview);
		listView.setAdapter(new MyListViewAdapter());
		listView.setOnItemClickListener(this);
		listView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE)  {
					isListViewScrolling = false;
					if(isUpdateListViewAfterStopScrolling) {
						((MyListViewAdapter)listView.getAdapter()).notifyDataSetChanged();
						isUpdateListViewAfterStopScrolling = false;
					}
				}else {
					isListViewScrolling = true;
				}
			}
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
		});
		updateView(null);
		return view;
	}

	/**
	 * 保存浏览一个目录时的状态
	 */
	class State {
		public State(File dir, int pos, int top) {
			this.pos = pos;
			this.dir = dir;
			this.top = top;
		}
		private int pos;
		private File dir;
		private int top; // listview中第一个view的top
	}

	class MyListViewAdapter extends BaseAdapter {

		class ViewHolder {
			ImageView img_icon;
			TextView txt_title;
			TextView txt_info;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return fileList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return fileList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if (convertView == null) {
				convertView = LayoutInflater.from(getActivity()).inflate(
						R.layout.fragment_file_explorer_listview_item, null);
				ViewHolder holder = new ViewHolder();
				holder.img_icon = (ImageView) convertView
						.findViewById(R.id.img_icon);
				holder.txt_title = (TextView) convertView
						.findViewById(R.id.txt_title);
				holder.txt_info = (TextView) convertView
						.findViewById(R.id.txt_info);
				convertView.setTag(holder);
			}
			ViewHolder viewHolder = (ViewHolder) convertView.getTag();
			File tempFile = fileList.get(position);
			viewHolder.txt_title.setText(tempFile.getName());
			if(tempFile.isDirectory()) {
				viewHolder.img_icon.setImageResource(R.drawable.folder_icon);
			}else {
				final String path = tempFile.getPath().toLowerCase();
				if(path.endsWith(".apk")) {
					if(drawableHashMapCathe.containsKey(path)) { //apk file
						Drawable drawable = drawableHashMapCathe.get(path);
						if(drawable != null) {
							viewHolder.img_icon.setImageDrawable(drawable);
						}else {
							viewHolder.img_icon.setImageResource(R.drawable.file_icon);
						}

					}else {
						handler.post(new Runnable() {
							@Override
							public void run() {
								Log.d(WifiP2pHelper.TAG, "post");
								Drawable drawable = ApkUtils.getApkIcon(getActivity(), path);
								drawableHashMapCathe.put(path, drawable);
								if(!isListViewScrolling) {//the listview is not scrolling update listview item now
									notifyDataSetChanged();
								}else { //the listview is scrolling , update listview item after it stoped.
									isUpdateListViewAfterStopScrolling = true;
								}
								Log.d(WifiP2pHelper.TAG, "post End");
							}
						});
					}
				}/*else if(path.endsWith(".png")||path.endsWith(".bmp")||
						path.endsWith(".jpg")||path.endsWith(".jpeg")) {//image file
					if(drawableHashMapCathe.containsKey(path)) {
						Drawable drawable = drawableHashMapCathe.get(path);
						if(drawable != null) {
							viewHolder.img_icon.setImageDrawable(drawable);
						}else {
							viewHolder.img_icon.setImageResource(R.drawable.file_icon);
						}
					}else {
						Bitmap bitmap = BitmapFactory.decodeFile(path);
						BitmapDrawable drawable = new BitmapDrawable(bitmap);
						drawableHashMapCathe.put(path, drawable);
						viewHolder.img_icon.setImageDrawable(drawable);
					}

				} */else { //normal file
					viewHolder.img_icon.setImageResource(R.drawable.file_icon);
				}

			}
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		Log.d(WifiP2pHelper.TAG, "FileExplore-->onItemClick()");
		File f = fileList.get(position);
		if (f.isDirectory()) {

			final View v = listView.getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			State state = new State(mDir, listView.getFirstVisiblePosition(), top);
			stateList.add(state);
			updateView(new State(f, 0, 0));
		}else { //click a file ---> add to sendFile-list
			MainActivity activity = (MainActivity) getActivity();
			if(activity.addFileToSendFileList(f.getPath())) {//add succeussed

			}else {
				ToastUtils.toast(activity, R.string.add_to_sendfilelist_erorr);
			}
		}
	}
}
