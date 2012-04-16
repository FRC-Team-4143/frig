package com.derekquam.FRIG;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class FRIGTeamActivity extends Activity {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private Uri fileUri;
	private String team;
	
	private static Uri getOutputPictureFileUri(String teamName) {
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "FRIG");

	    if (!mediaStorageDir.exists()){
	        if (!mediaStorageDir.mkdirs()){
	            Log.d("FRIG", "failed to create directory");
	            return null;
	        }
	    }
	    
	    return Uri.fromFile(new File(mediaStorageDir.getPath() + File.separator +
	        teamName + ".jpg"));
	}
	 
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.team);

		Bundle lExtras = getIntent().getExtras();
		if (lExtras == null) {
			return;
		}
		
		// Get data via the key
		team = lExtras.getString("Team");
		if (team != null) {
			TextView lblTeam = (TextView)findViewById(R.id.txtTeamID);
			lblTeam.setText("Team " + team);
		}
		
		Button lCamera = (Button)findViewById(R.id.btnTakePic);
		lCamera.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View xView) {
        		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        		fileUri = getOutputPictureFileUri(team); 
        	    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        	    // start the image capture Intent
        	    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        	}
        });
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// Image captured and saved to fileUri specified in the Intent
				Toast.makeText(this, "Image saved to:\n" +
						fileUri, Toast.LENGTH_LONG).show();
			} else if (resultCode == RESULT_CANCELED) {
				// User cancelled the image capture
			} else {
				// Image capture failed, advise user
			}
		}
	}
}
