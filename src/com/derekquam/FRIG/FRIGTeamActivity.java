package com.derekquam.FRIG;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.derekquam.FRIG.FRIGImageAdapter.Image;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class FRIGTeamActivity extends Activity {
	private static final String TAG = "FRIGTeamActivity";
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int REPLACE_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	private Uri fileUri;
	private String team;
	private static int nextPic = 1;
	private static int picNo = 1;
	private static int width = 800;
	private static int quality = 70;
	private UploadPictureTask mUploader;
	private GridView mGallery;
	

	private static Uri getOutputPictureFileUri(String teamName) {
		File cacheDir = new File(Environment.getDownloadCacheDirectory(), "FRIG");
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), "FRIG");

		if (!cacheDir.exists()){
			if (!cacheDir.mkdirs()){
				Log.d(TAG, "failed to create directory");
				return null;
			}
		}

		return Uri.fromFile(new File(cacheDir.getPath() + File.separator +
				teamName + ".jpg"));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.team_new);

		Bundle lExtras = getIntent().getExtras();
		if (lExtras == null) {
			return;
		}
		
		mUploader = new UploadPictureTask();

		// Get data via the key
		team = lExtras.getString("Team");
		if (team != null) {
			TextView lblTeam = (TextView)findViewById(R.id.txtTeamID);
			lblTeam.setText("Team " + team);
		}

		mGallery = (GridView)findViewById(R.id.teamPics);
		mGallery.setAdapter(new FRIGImageAdapter(this, mGallery, team));
		mGallery.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(arg0.getContext());                      
				dlgAlert.setTitle("Default"); 
				dlgAlert.setMessage("Make this the default image?"); 
				dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						finish(); 
					}
				});
				dlgAlert.setCancelable(true);
				dlgAlert.create().show();
				return false;
			}
		});

		
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
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE ||
				requestCode == REPLACE_IMAGE_ACTIVITY_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
					picNo = mGallery.getAdapter().getCount() + 1;
				}
				String outPath = ResizePicture(fileUri.getPath(), width);
				mUploader.execute(outPath);
			} else if (resultCode == RESULT_CANCELED) {
				// User cancelled the image capture
			} else {
				// Image capture failed, advise user
			}
		}
	}

	protected String ResizePicture(String imagePath, int desiredWidth) {
		// Get the source image's dimensions
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imagePath, options);

		int srcWidth = options.outWidth;
		int srcHeight = options.outHeight;

		// Only scale if the source is big enough. This code is just trying to fit a image into a certain width.
		if(desiredWidth > srcWidth)
			desiredWidth = srcWidth;

		// Calculate the correct inSampleSize/scale value. This helps reduce memory use. It should be a power of 2
		// from: http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue/823966#823966
		int inSampleSize = 1;
		while(srcWidth / 2 > desiredWidth) {
			srcWidth /= 2;
			srcHeight /= 2;
			inSampleSize *= 2;
		}

		float desiredScale = (float) desiredWidth / srcWidth;

		// Decode with inSampleSize
		options.inJustDecodeBounds = false;
		options.inDither = false;
		options.inSampleSize = inSampleSize;
		options.inScaled = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap sampledSrcBitmap = BitmapFactory.decodeFile(imagePath, options);

		// Resize
		Matrix matrix = new Matrix();
		matrix.postScale(desiredScale, desiredScale);
		Bitmap scaledBitmap = Bitmap.createBitmap(sampledSrcBitmap, 0, 0, sampledSrcBitmap.getWidth(), sampledSrcBitmap.getHeight(), matrix, true);
		sampledSrcBitmap = null;

		// Save
		FileOutputStream out;
		try {
			out = new FileOutputStream(imagePath);
			scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
			out.close();
			File lOriginal = new File(imagePath);
			File lHashFile = new File(new File(Environment.getDownloadCacheDirectory(), "FRIG"),
					getSha1(imagePath));
			lOriginal.renameTo(lHashFile);
			scaledBitmap = null;
			return lHashFile.getAbsolutePath();
		} catch (FileNotFoundException ex) {
			Log.d(TAG, "ResizePicture", ex);
		} catch (IOException ex) {
			Log.d(TAG, "ResizePicture", ex);
		}
		return "";
	}

	protected boolean UploadPicture(String imagePath) {
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;

		String urlServer = "http://www.derekquam.com/handle_upload.php";
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary =  "*****";

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1*1024*1024;

		try
		{
			FileInputStream fileInputStream = new FileInputStream(new File(imagePath) );

			URL url = new URL(urlServer);
			connection = (HttpURLConnection) url.openConnection();

			// Allow Inputs & Outputs
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			// Enable POST method
			connection.setRequestMethod("POST");

			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

			outputStream = new DataOutputStream( connection.getOutputStream() );
			outputStream.writeBytes(twoHyphens + boundary + lineEnd);
			outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + imagePath +"\"" + lineEnd);
			outputStream.writeBytes(lineEnd);

			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];

			// Read file
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);

			while (bytesRead > 0)
			{
				outputStream.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}

			outputStream.writeBytes(lineEnd);
			outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			// Responses from the server (code and message)
			int serverResponseCode = connection.getResponseCode();
			String serverResponseMessage = connection.getResponseMessage();
			InputStream in = new BufferedInputStream(connection.getInputStream());

			Toast.makeText(this, serverResponseMessage, Toast.LENGTH_LONG).show();

			fileInputStream.close();
			outputStream.flush();
			outputStream.close();
			
			if (serverResponseCode == 200) {
				return true;
			}
		}
		catch (Exception ex)
		{
			Log.d(TAG, "UploadPicture", ex);
			return false;
		}
		return false;
	}
	
	public class UploadPictureTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			// iterate over all images ...
			for (String i : params) {
				if(isCancelled()) return null;
				UploadPicture(i);
			}
			return null;
		}
		
	}
	
	private String getSha1(String file) throws IOException {
		FileInputStream in;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "";
		}
		MessageDigest digester;
		try {
			digester = MessageDigest.getInstance("sha1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return "";
		}
		byte[] bytes = new byte[8192];
		int byteCount;
		int total = 0;
		try {
			while ((byteCount = in.read(bytes)) > 0) {
			    total += byteCount;
			    digester.update(bytes, 0, byteCount);
			    Log.d("sha", "processed " + total);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		in.close();
		return digester.toString();
	}
}