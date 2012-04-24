package com.derekquam.FRIG;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.auth.UsernamePasswordCredentials;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FRIGTeamsActivity extends ListActivity {
	private static final String TAG = "FRIGTeamsActivity";
	
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
			Authenticator.setDefault(new Authenticator(){
			    protected PasswordAuthentication getPasswordAuthentication() {
			        return new PasswordAuthentication("FRIGApp","correcthorsebatterystaple".toCharArray());
			    }});
			URL url = new URL("http://www.derekquam.com/frig/TeamList.php?plain");
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
			String[] lRet = strFileContents.split("\\r?\\n?<br>");
			bis.close();
			is.close();
			return lRet;
		}
		catch (Exception ex)
		{
			Log.d(TAG, "getTeams", ex);
		}
		return null;
	}
}