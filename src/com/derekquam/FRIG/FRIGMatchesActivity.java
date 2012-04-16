package com.derekquam.FRIG;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class FRIGMatchesActivity extends ListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String[] lMatches = new String[] { "Match 1", "Match 2", "Match 3", "Match 4", 
				"Match 5", "Match 6", "Match 7", "Match 8", "Match 9"};
		ArrayAdapter<String> lAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, lMatches);
		setListAdapter(lAdapter);
	}
		
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String lItem = (String) getListAdapter().getItem(position);
		Toast.makeText(this, lItem + " selected", Toast.LENGTH_LONG).show();
	}
}
