package com.derekquam.FRIG;

import com.derekquam.FRIG.FRIGImageAdapter.Image;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.SpinnerAdapter;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class FRIGTeamsActivity extends Activity {
	static final int DEFAULT_PICTURE = 0;
	private GridView mGridview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.teams);

		// Set up gridview of images
		mGridview = (GridView)findViewById(R.id.gridview);
		mGridview.setAdapter(new FRIGImageAdapter(this, mGridview, 1));
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

		// Set up action bar menu for region selection
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		OnNavigationListener mOnNavigationListener = new OnNavigationListener() {
			// Get the same strings provided for the drop-down's ArrayAdapter

			@Override
			public boolean onNavigationItemSelected(int position, long itemId) {
				((FRIGImageAdapter) mGridview.getAdapter()).setRegion(position + 1);
				return true;
			}
		};
		SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.regions,
				android.R.layout.simple_spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, mOnNavigationListener);
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