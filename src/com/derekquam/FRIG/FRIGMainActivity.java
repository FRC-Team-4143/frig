package com.derekquam.FRIG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class FRIGMainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button btnTeams = (Button)findViewById(R.id.btnTeam);
        btnTeams.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View xView) {
        		Intent lIntent = new Intent(FRIGMainActivity.this, FRIGTeamsActivity.class);
        	    startActivity(lIntent);
        	}
        });
        
        Button btnMatches = (Button)findViewById(R.id.btnMatches);
        btnMatches.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View xView) {
        		Intent lIntent = new Intent(FRIGMainActivity.this, FRIGMatchesActivity.class);
        	    startActivity(lIntent);
        	}
        });
        
        Button btnAdmin =  (Button)findViewById(R.id.btnAdmin);
        btnAdmin.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View xView) {
        		
        	}
        });
    }
}