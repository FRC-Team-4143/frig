package com.derekquam.FRIG;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class FRIGMainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button btnTeams = (Button)findViewById(R.id.btnTeam);
        btnTeams.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View xView) {
        		
        	}
        });
        
        Button btnMatches = (Button)findViewById(R.id.btnMatches);
        btnMatches.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View xView) {
        		
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