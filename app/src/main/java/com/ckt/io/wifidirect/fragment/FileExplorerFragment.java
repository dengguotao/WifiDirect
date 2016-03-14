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
import android.widget.CheckBox;
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

	private boolean isListViewScrolling = false;
	private boolean isUpdateListViewAfterStopScrolling = false;

	private Handler handler = new Handler();
	public FileExplorerFragment(File dir) {
		this.mDir = dir;
		if (this.mDir == null) {
			mDir = new File("/");
		}
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
		mDir = state.dir;
		MyListViewAdapter adapter = (MyListViewAdapter) listView.getAdapter();
		if(state.list == null) { //a new state
			state.list = new ArrayList<File>();
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
				state.list.addAll(sort(tempFolderList));
				state.list.addAll(sort(tempFileList));
			}
			adapter.setList(state.list);
		}else {
			//reset the listview pos
			adapter.setData(state.list, state.checkList);
		}
		listView.setSelectionFromTop(state.pos, state.top);

		if (state.list.size() != 0) {
			lin_no_file.setVisibility(View.GONE);
		} else {
			lin_no_file.setVisibility(View.VISIBLE);
		}
	}

	public boolean back() {
		Log.d(WifiP2pHelper.TAG, "FileExplorerFragment-->back-->stateList.size() ="+stateList.size());
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
		listView.setAdapter(new MyListViewAdapter(null));
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
		updateView(new State(mDir,0,0,null,null));
		return view;
	}

	/**
	 * 保存浏览一个目录时的状态
	 */
	class State {
		public State(File dir, int pos, int top, ArrayList<File> list, ArrayList<Boolean> checkList) {
			this.pos = pos;
			this.dir = dir;
			this.top = top;
			this.checkList = checkList;
			this.list = list;
		}
		private int pos;
		private File dir;
		private int top; // listview中第一个view的top
		private ArrayList<File> list;
		private ArrayList<Boolean> checkList;
	}

	class MyListViewAdapter extends BaseAdapter {
		private ArrayList<Boolean> mCheckBoxList = new ArrayList<>();
		private ArrayList<File> list = new ArrayList<>();
		public MyListViewAdapter(ArrayList<File> list) {
			setList(list);
		}
		public void setList(ArrayList<File> list) {
			this.list = list;
			mCheckBoxList = new ArrayList<>();
			if(list != null) {
				for(int i=0; i<list.size(); i++) {
					mCheckBoxList.add(false);
				}
			}
			notifyDataSetChanged();
		}
		public void setData(ArrayList<File> list, ArrayList<Boolean> checkList) {
			this.list = list;
			if(checkList == null) {
				setList(list);
			}else {
				this.mCheckBoxList = checkList;
			}
			notifyDataSetChanged();
		}
		public boolean isChecked(int pos) {
			if(pos<0 || pos >= mCheckBoxList.size()) {
				return false;
			}
			return mCheckBoxList.get(pos);
		}
		public void clearChecked() {
			for(int i=0; i<mCheckBoxList.size(); i++) {
				mCheckBoxList.set(i, false);
			}
		}
		public void toggleChecked(int pos) {
			mCheckBoxList.set(pos, !mCheckBoxList.get(pos));
		}

		class ViewHolder {
			ImageView img_icon;
			TextView txt_title;
			TextView txt_info;
			CheckBox checkBox;
		}
		public ArrayList<Boolean> getmCheckBoxList() {
			return  this.mCheckBoxList;
		}
		public ArrayList<File> getList() {
			return this.list;
		}
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return this.list == null ? 0 : this.list.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return this.list == null ? null :list.get(position);
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
				holder.checkBox = (CheckBox) convertView.findViewById(R.id.list_item_checkbox);
				convertView.setTag(holder);
			}
			ViewHolder viewHolder = (ViewHolder) convertView.getTag();
			File tempFile = list.get(position);
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
			//设置是否选中
			if(mCheckBoxList.get(position)) { //checked
				viewHolder.checkBox.setVisibility(View.VISIBLE);
				viewHolder.checkBox.setChecked(true);
			}else {//unchecked
				viewHolder.checkBox.setVisibility(View.GONE);
				viewHolder.checkBox.setChecked(false);
			}
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		Log.d(WifiP2pHelper.TAG, "FileExplore-->onItemClick()");
		MyListViewAdapter adapter = (MyListViewAdapter) listView.getAdapter();
		File f = adapter.getList().get(position);
		if (f.isDirectory()) {
			final View v = listView.getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			State state = new State(mDir, listView.getFirstVisiblePosition(), top, adapter.getList(), adapter.getmCheckBoxList());
			stateList.add(state); //save old state
			updateView(new State(f, 0, 0, null, null)); //update new state
		}else { //click a file ---> add to sendFile-list
			MainActivity activity = (MainActivity) getActivity();
			adapter.toggleChecked(position);
			adapter.notifyDataSetInvalidated();
			if(adapter.isChecked(position)) {//checked-->add to sendfile-list
				activity.addFileToSendFileList(f.getPath());
			}else { //unchecked--->remove from sendfile-list
				activity.removeFileFromSendFileList(f.getPath());
			}
		}
		int len = f.getName().getBytes().length;
		Integer x = 500;
		Log.d(WifiP2pHelper.TAG, "fileName len(byte) = "+len + "  test:"+x.byteValue());
	}
}
