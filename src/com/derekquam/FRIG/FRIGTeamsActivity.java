package com.derekquam.FRIG;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		
		String[] lTeams = getTeams();
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

	private String[] getTeams() {
		try {
			URL url = new URL("http://www.derekquam.com/frig/TeamList.php");
			URLConnection connection = url.openConnection();
			connection.connect();
			InputStream is = connection.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is, 8 * 1024);
			byte[] contents = new byte[1024];

			int bytesRead = 0;
			String strFileContents = "";
			String strTemp = "";
			while((bytesRead = bis.read(contents)) != -1) { 
				strTemp = new String(contents, 0, bytesRead);
				strFileContents += strTemp;
			}
			Pattern pattern = Pattern.compile("team=(\\d+)");
			Matcher matcher = pattern.matcher(strFileContents);
			List<String> matches = new ArrayList<String>();
			while(matcher.find()) {
				matches.add(matcher.group(1));
			}
			String[] lRet = new String[matches.size()];
			matches.toArray(lRet);
			bis.close();
			is.close();
			return lRet;
		}
		catch (Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		return null;
	}
}