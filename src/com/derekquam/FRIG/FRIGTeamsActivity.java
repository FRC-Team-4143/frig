package com.derekquam.FRIG;

import com.derekquam.FRIG.FRIGImageAdapter.Image;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class FRIGTeamsActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.teams);
		
		GridView gridview = (GridView)findViewById(R.id.gridview);
		gridview.setAdapter(new FRIGImageAdapter(this, gridview, ""));

		gridview.setOnItemClickListener(new OnItemClickListener() {  
			@Override  
			public void onItemClick(AdapterView<?> parent, View view, int position,  
					long id) {
				Image item = (Image)parent.getAdapter().getItem(position);
				Intent lIntent = new Intent(FRIGTeamsActivity.this, FRIGTeamActivity.class);
				lIntent.putExtra("Team", item.name);
				startActivity(lIntent);
			}
		});
	}
}