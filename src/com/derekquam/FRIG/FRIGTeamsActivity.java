package com.derekquam.FRIG;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FRIGTeamsActivity extends ListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String[] lTeams = new String[] { "1111", "2222", "3333", "4444", "5555", "6666",
				"7777", "8888", "9999"};
		ArrayAdapter<String> lAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, lTeams);
		setListAdapter(lAdapter);
	}
		
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String lItem = (String) getListAdapter().getItem(position);
		Intent lIntent = new Intent(FRIGTeamsActivity.this, FRIGTeamActivity.class);
		lIntent.putExtra("Team", lItem);
	    startActivity(lIntent);
	}
}