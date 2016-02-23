package com.ckt.io;

import java.io.File;
import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.ckt.io.fragment.FileExplorerFragment;

public class MainActivity extends FragmentActivity  {

	private ViewPager viewPager;
	
	private ArrayList<Fragment> fragmentList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.viewPager = (ViewPager) findViewById(R.id.viewPager1);
		initViewPager();
	}
	
	public void initViewPager() {
		fragmentList = new ArrayList<Fragment>();
		fragmentList.add(new FileExplorerFragment(new File("")));
		fragmentList.add(new FileExplorerFragment(new File("")));
		fragmentList.add(new FileExplorerFragment(new File("")));
		fragmentList.add(new FileExplorerFragment(new File("")));
		viewPager.setAdapter(new MyFragmentPagerAdapter(getSupportFragmentManager(), fragmentList));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	class MyFragmentPagerAdapter extends FragmentPagerAdapter{  
	    ArrayList<Fragment> list;  
	    public MyFragmentPagerAdapter(FragmentManager fm,ArrayList<Fragment> list) {  
	        super(fm);  
	        this.list = list;      
	    }  
	      
	    @Override  
	    public int getCount() {  
	        return list.size();  
	    }  
	      
	    @Override  
	    public Fragment getItem(int arg0) {  
	        return list.get(arg0);  
	    }   
	}
}
