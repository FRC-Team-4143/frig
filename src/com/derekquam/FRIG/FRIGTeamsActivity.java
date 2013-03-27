package com.derekquam.FRIG;

import com.derekquam.FRIG.FRIGImageAdapter.Image;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class FRIGTeamsActivity extends Activity {
	static final int DEFAULT_PICTURE = 0;
	private GridView mGridview;
	private int mRegion;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.teams);

		// Set up gridview of images
		if (savedInstanceState != null && savedInstanceState.containsKey("region")) {
			mRegion = savedInstanceState.getInt("region");
		} else {
			mRegion = 4;
		}
		mGridview = (GridView)findViewById(R.id.gridview);
		mGridview.setAdapter(new FRIGImageAdapter(this, mGridview, mRegion + 1));
		mGridview.setOnItemClickListener(new OnItemClickListener() {  
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,  
					long id) {
				Image item = (Image)parent.getAdapter().getItem(position);
				Intent lIntent = new Intent(FRIGTeamsActivity.this, FRIGTeamActivity.class);
				lIntent.putExtra("Team", item.caption);
				startActivityForResult(lIntent, DEFAULT_PICTURE);
			}
		});
		
		mGridview.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				String toastText = ((Image)parent.getAdapter().getItem(position)).teamName;
				if (toastText.trim() == "") {
					toastText = "Unknown";
				}
				Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
				return true;
			}
			
		});

		// Set up action bar menu for region selection
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		OnNavigationListener mOnNavigationListener = new OnNavigationListener() {
			// Get the same strings provided for the drop-down's ArrayAdapter

			@Override
			public boolean onNavigationItemSelected(int position, long itemId) {
				mRegion = position;
				((FRIGImageAdapter) mGridview.getAdapter()).setRegion(position + 1);
				return true;
			}
		};
		SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.regions,
				android.R.layout.simple_spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, mOnNavigationListener);
		actionBar.setSelectedNavigationItem(mRegion);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.refresh:
	        	((FRIGImageAdapter) mGridview.getAdapter()).refresh(); 
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("region", mRegion);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (requestCode == DEFAULT_PICTURE) {
			if (resultCode == RESULT_OK) {
				((FRIGImageAdapter) mGridview.getAdapter()).refresh();
			}
		}
	}
}