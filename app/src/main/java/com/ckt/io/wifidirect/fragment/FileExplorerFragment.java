package com.ckt.io.wifidirect.fragment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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
import com.ckt.io.wifidirect.MainActivity.OnSendFileListChangeListener;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.utils.DrawableLoaderUtils;
import com.ckt.io.wifidirect.utils.LogUtils;
import com.ckt.io.wifidirect.utils.SdcardUtils;


@SuppressLint("ValidFragment")
public class FileExplorerFragment extends Fragment implements
		OnItemClickListener, OnSendFileListChangeListener {

	private ArrayList<State> stateList = new ArrayList<State>();
	private State nowState;

	private ViewGroup lin_no_file;
	private ListView listView;
	private TextView txt_dir_Path; //显示当前的文件夹路径的
	private File mDir;

	private File externalSDFile;
	private File innerSdFile;

	private DrawableLoaderUtils drawableLoaderUtils;

	private boolean isListViewScrolling = false;

	private Handler handler = new Handler();

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
		if(state.isAutoUpdateChildFiles) {//更新mdir下的childs(如果不更新,需要自己手动设置filelist 和 checklist)--->目前只有家目录是自定义的childs,不会在这里自动更新childs
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
			state.checkList = new ArrayList<>();
			for(int i=0; i<state.list.size(); i++) {
				state.checkList.add(false);
			}
		}
		//根据文件发送列表来更新checklist
		MainActivity activity = (MainActivity) getActivity();
		updateCheckList(activity.getSendFiles(), state.list, state.checkList);
		adapter.setData(state.list, state.checkList);//reset the data and notifidatasetchanged
		listView.setSelectionFromTop(state.pos, state.top);
		LogUtils.i(WifiP2pHelper.TAG, "updateViews pos="+state.pos+" top="+state.top);
		if (state.list.size() != 0) {
			lin_no_file.setVisibility(View.GONE);
		} else {
			lin_no_file.setVisibility(View.VISIBLE);
		}
		//更新当前文件夹路径
		ArrayList<String> dirs = new ArrayList<>();
		File f = state.dir;
		Log.d(WifiP2pHelper.TAG, "f="+f);
		while (f!=null) {
			if(externalSDFile!=null) {
				if(f.getPath().equals(externalSDFile.getPath())) {
					dirs.add(getResources().getString(R.string.external_sdcard));
					break;
				}
			}
			if(innerSdFile != null) {
				if(f.getPath().equals(innerSdFile.getPath())) {
					dirs.add(getResources().getString(R.string.inner_sdcard));
					break;
				}
			}
			File root = new File("/");
			if(f.getPath().equals(root.getPath())) {
				dirs.add(getResources().getString(R.string.root_dir));
				break;
			}
			else {
				dirs.add(f.getName());
				f = f.getParentFile();
			}
		}
		StringBuffer buf = new StringBuffer();
		for(int i=dirs.size()-1; i>=0; i--) {
			if(i==0) {
				buf.append(dirs.get(i));
			}else {
				buf.append(dirs.get(i)+" > ");
			}
		}
		txt_dir_Path.setText(buf.toString());
		//加载listview Item图片-->只有像apk, 图片等文件会加载
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				loadListViewItemDrawalbe();
			}
		}, 500);//延迟500ms后开始加载可见的listItem的图片(延迟500ms,是为了等待listview重新加载完成, 不然获取到第一个可视位置不对)
		nowState = state;
	}

	public boolean back() {
		Log.d(WifiP2pHelper.TAG, "FileExplorerFragment-->back-->stateList.size() =" + stateList.size());
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
		boolean isFirstCrateView = false;
		if(mDir == null) { //首次加载
			externalSDFile = SdcardUtils.getExternalSDcardFile(getActivity());
			innerSdFile = SdcardUtils.getInnerSDcardFile(getActivity());
			mDir = externalSDFile;
			if(mDir == null) {
				mDir = innerSdFile;
			}
			mDir = new File(getResources().getString(R.string.home_dir));
			final ArrayList<File> list = new ArrayList<>();
			final ArrayList<Boolean> checkList= new ArrayList<>();
			//依次向家目录下添加:  ①内置sdcard ②外置sdcard ③接收文件夹
			if(innerSdFile != null) {
				list.add(innerSdFile);
				checkList.add(false);
			}
			if(externalSDFile != null) {
				list.add(externalSDFile);
				checkList.add(false);
			}
			final File receiveFileSaveDir = ((MainActivity)getActivity()).getWifiP2pHelper().getReceivedFileDirPath();
			MainActivity activity = (MainActivity) getActivity();
			activity.requestPermission(receiveFileSaveDir.hashCode() + MainActivity.REQUEST_CODE_WRITE_EXTERNAL,
					Manifest.permission.WRITE_EXTERNAL_STORAGE,
					new Runnable() {
						@Override
						public void run() {
							LogUtils.i(WifiP2pHelper.TAG, "gain the permission WRITE_EXTERNAL_STORAGE");
							receiveFileSaveDir.mkdir();
							list.add(receiveFileSaveDir);
							checkList.add(false);
						}
					},null);
			isFirstCrateView = true;
			nowState = new State(mDir, 0, 0, list, checkList, false);
		}

		View view = inflater.inflate(R.layout.fragment_file_explorer,
				container, false);
		lin_no_file = (ViewGroup) view.findViewById(R.id.lin_no_file);
		listView = (ListView) view.findViewById(R.id.listview);
		txt_dir_Path = (TextView) view.findViewById(R.id.txt_dir_path);
		listView.setAdapter(new MyListViewAdapter());
		drawableLoaderUtils = DrawableLoaderUtils.getInstance((DrawableLoaderUtils.OnLoadFinishedListener) listView.getAdapter()); //获取图片加载器实例对象
		listView.setOnItemClickListener(this);
		listView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
					LogUtils.i(WifiP2pHelper.TAG, "listview stop");
					isListViewScrolling = false;
					MyListViewAdapter adapter = (MyListViewAdapter) listView.getAdapter();
					adapter.notifyDataSetChanged();
					//加载需要加载图片的一个文件
					loadListViewItemDrawalbe();
				} else {
					LogUtils.i(WifiP2pHelper.TAG, "listview start scrolling");
					isListViewScrolling = true;
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
		});
		if(isFirstCrateView) {
			updateView(nowState);
		}else {
			updateView(nowState);
		}
		return view;
	}

	private void loadListViewItemDrawalbe() {
		MyListViewAdapter adapter = (MyListViewAdapter) listView.getAdapter();
		int start = listView.getFirstVisiblePosition();
		int end = listView.getLastVisiblePosition();
		if(end >= adapter.getList().size()) {
			end = adapter.getList().size()-1;
		}
		LogUtils.i(WifiP2pHelper.TAG, "loadListViewItemDrawalbe: start="+start+" End="+end);
		if(end>=start) {
			for(int i=start; i<=end; i++) {
				File f = adapter.getList().get(i);
				if(DrawableLoaderUtils.isNeedToLoadDrawable(f.getPath())) {
					drawableLoaderUtils.load(getContext(), f.getPath());
				}
			}
		}
	}

	private void updateCheckList(ArrayList<String> sendFiles, ArrayList<File> fileList, ArrayList<Boolean> checkList) {
		String data = sendFiles.toString();
		for(int i=0; i<fileList.size(); i++) {
			File f = fileList.get(i);
			String temp = f.getPath();
			if(f.isFile() && data.contains(temp)) {
				checkList.set(i, true);
			}else {
				checkList.set(i, false);
			}
		}
	}

	@Override
	public void onSendFileListChange(ArrayList<String> sendFiles, int num) {
		if(listView != null && listView.getAdapter() != null) {
			MyListViewAdapter adapter = (MyListViewAdapter) listView.getAdapter();
			ArrayList<Boolean> checkList = adapter.getmCheckBoxList();
			ArrayList<File> fileList = adapter.getList();
			updateCheckList(sendFiles, fileList, checkList);
			adapter.notifyDataSetInvalidated();
		}
	}

	/**
	 * 保存浏览一个目录时的状态
	 */
	class State {
		public State(File dir, int pos, int top, ArrayList<File> list, ArrayList<Boolean> checkList) {
			this(dir, pos, top, list, checkList, true);
		}
		public State(File dir, int pos, int top, ArrayList<File> list, ArrayList<Boolean> checkList, boolean isAutoUpdateChildFiles) {
			this.pos = pos;
			this.dir = dir;
			this.top = top;
			this.checkList = checkList;
			this.list = list;
			this.isAutoUpdateChildFiles = isAutoUpdateChildFiles;
		}
		private int pos;
		private File dir;
		private int top; // listview中第一个view的top
		private ArrayList<File> list;
		private ArrayList<Boolean> checkList;
		private boolean isAutoUpdateChildFiles;
	}

	class MyListViewAdapter extends BaseAdapter implements DrawableLoaderUtils.OnLoadFinishedListener{
		private ArrayList<Boolean> mCheckBoxList = new ArrayList<>();
		private ArrayList<File> list = new ArrayList<>();
		public void setData(ArrayList<File> list, ArrayList<Boolean> checkList) {
			this.list = list;
			this.mCheckBoxList = checkList;
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
				if(DrawableLoaderUtils.isNeedToLoadDrawable(tempFile.getPath())) {//需要显示加载图片
					Object object = drawableLoaderUtils.get(tempFile.getPath());
					if(object instanceof Drawable) {
						viewHolder.img_icon.setImageDrawable((Drawable) object);
					}else if(object instanceof Bitmap) {
						viewHolder.img_icon.setImageBitmap((Bitmap) object);
					}else { //图片为空-->加载默认的图片
						viewHolder.img_icon.setImageResource(R.drawable.file_icon);
					}
				}else {//其他文件-->加载默认图片
					viewHolder.img_icon.setImageResource(R.drawable.file_icon);
				}
			}
			//sdcard目录,特别处理:
			if(innerSdFile != null) {
				if(tempFile.getPath().equals(innerSdFile.getPath())) {
					viewHolder.img_icon.setImageResource(R.drawable.sdcard_icon);
					viewHolder.txt_title.setText(getResources().getString(R.string.inner_sdcard));
				}
			}
			if(externalSDFile != null) {
				if(tempFile.getPath().equals(externalSDFile.getPath())) {
					viewHolder.img_icon.setImageResource(R.drawable.sdcard_icon);
					viewHolder.txt_title.setText(getResources().getString(R.string.external_sdcard));
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

		//加载完一个文件的图片后,的回调
		@Override
		public void onLoadOneFinished(String path, Object obj, boolean isAllFinished) {
			LogUtils.i(WifiP2pHelper.TAG, "LOAD_ONE_DRAWABLE_FINISHED");
			if(!isListViewScrolling) {
				MyListViewAdapter adapter = (MyListViewAdapter) listView.getAdapter();
				adapter.notifyDataSetChanged();
			}
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
			nowState.pos = listView.getFirstVisiblePosition();
			nowState.top = top;
//			State state = new State(mDir, listView.getFirstVisiblePosition(), top, adapter.getList(), adapter.getmCheckBoxList());
			stateList.add(nowState); //save old state
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
