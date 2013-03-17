package com.derekquam.FRIG;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
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
	private static File mCacheDir;
	

	private static Uri getOutputPictureFileUri(String teamName) {
		File cacheDir = new File(Environment.getDownloadCacheDirectory(), "FRIG");
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), "FRIG");

		if (!mediaStorageDir.exists()){
			if (!mediaStorageDir.mkdir()){
				Log.d(TAG, "failed to create directory");
				return null;
			}
		}

		return Uri.fromFile(new File(mediaStorageDir.getPath() + File.separator +
				teamName + ".jpg"));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.team_new);
		mCacheDir = getCacheDir();

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

		mGallery = (GridView)findViewById(R.id.teamPics);
		mGallery.setClickable(true);
		mGallery.setAdapter(new FRIGImageAdapter(this, mGallery, team));
		mGallery.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(arg0.getContext());                      
				dlgAlert.setTitle("Default"); 
				dlgAlert.setMessage("Make this the default image?"); 
				dlgAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						SetDefault task = new SetDefault();
						task.execute(team, );
						dialog.dismiss();
					}
				});
				dlgAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
				dlgAlert.setCancelable(true);
				dlgAlert.create().show();
				return true;
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

	private void pictureUploaded() {
		((FRIGImageAdapter) mGallery.getAdapter()).refresh();
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
				mUploader = new UploadPictureTask();
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
			File lHashFile = new File(new File(Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_PICTURES), "FRIG"),
					AeSimpleSHA1.SHA1(imagePath) + ".jpg");
			lOriginal.renameTo(lHashFile);
			scaledBitmap = null;
			return lHashFile.getAbsolutePath();
		} catch (FileNotFoundException ex) {
			Log.d(TAG, "ResizePicture", ex);
		} catch (IOException ex) {
			Log.d(TAG, "ResizePicture", ex);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			outputStream.writeBytes("Content-Disposition: form-data; name=\"team\"" + lineEnd + lineEnd);
			outputStream.writeBytes(team + lineEnd + twoHyphens + boundary + lineEnd);
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
	
	public class UploadPictureTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			// iterate over all images ...
			Boolean lReturn = false;
			for (String i : params) {
				if(isCancelled()) return false;
				if (UploadPicture(i)) {
					lReturn = true;
				}
			}
			return lReturn;
		}
		
		protected void onPostExecute(Boolean results) {
			if (results) {
				pictureUploaded();
			}
		}
		
	}
	
	public class SetDefault extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			if (params.length != 2) {
				return null;
			}
			makeDefault(params[0], params[1]);
			return null;
		}
		
		private Boolean makeDefault(String team, String image) {
			try {
				Authenticator.setDefault(new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication("FRIGApp","correcthorsebatterystaple".toCharArray());
					}
				});
				URL url = new URL("http://www.derekquam.com/frig/set_default.php?team=" + team + "&image=" + image);
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
				return true;
			}
			catch (Exception ex) {
				Log.e("FRIG", "makeDefault", ex);
			}
			return false;
		}
		
	}
	
	public static class AeSimpleSHA1 {
		private static String convertToHex(byte[] data) {
			StringBuilder buf = new StringBuilder();
			for (byte b : data) {
				int halfbyte = (b >>> 4) & 0x0F;
				int two_halfs = 0;
				do {
					buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
					halfbyte = b & 0x0F;
				} while (two_halfs++ < 1);
			}
			return buf.toString();
		}

		public static String SHA1(String filepath) throws NoSuchAlgorithmException, IOException {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			FileInputStream in = new FileInputStream(filepath);
			byte[] bytes = new byte[8192];
			int byteCount;
			int total = 0;
			while ((byteCount = in.read(bytes)) > 0) {
			    total += byteCount;
			    sha1.update(bytes, 0, byteCount);
			    Log.d("sha", "processed " + total);
			}
			byte[] sha1hash = sha1.digest();
			return convertToHex(sha1hash);
		}
	}

}