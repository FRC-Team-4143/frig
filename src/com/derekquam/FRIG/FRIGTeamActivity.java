package com.derekquam.FRIG;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.derekquam.FRIG.FRIGTeamActivity.ImageDownloader.BitmapDownloaderTask;

public class FRIGTeamActivity extends Activity {
	private static final String TAG = "FRIGTeamActivity";
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int REPLACE_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	private Uri fileUri;
	private String team;
	private static int nextPic = 1;
	private static int picNo = 1;
	private static int width = 800;
	private static int quality = 80;

	private static boolean cancelPotentialDownload(String url, ImageView imageView) {
		BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

		if (bitmapDownloaderTask != null) {
			String bitmapUrl = bitmapDownloaderTask.url;
			if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
				bitmapDownloaderTask.cancel(true);
			} else {
				// The same URL is already being downloaded.
				return false;
			}
		}
		return true;
	}

	private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
		if (imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof DownloadedDrawable) {
				DownloadedDrawable downloadedDrawable = (DownloadedDrawable)drawable;
				return downloadedDrawable.getBitmapDownloaderTask();
			}
		}
		return null;
	}

	private static Uri getOutputPictureFileUri(String teamName) {
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), "FRIG");

		if (!mediaStorageDir.exists()){
			if (!mediaStorageDir.mkdirs()){
				Log.d(TAG, "failed to create directory");
				return null;
			}
		}

		return Uri.fromFile(new File(mediaStorageDir.getPath() + File.separator +
				teamName + ".jpg"));
	}

	private String[] GetCurrentPictures(String team) {
		try {
			URL url = new URL("http://www.derekquam.com/frig/TeamPics.php?team=" + team);
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
			nextPic = lRet.length + 1;
			bis.close();
			is.close();
			return lRet;
		}
		catch (Exception ex)
		{
			Log.d(TAG, "GetCurrentPictures", ex);
		}
		return null;
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
		
		final EditText txtWidth = (EditText)findViewById(R.id.txtWidth);
		final SeekBar seekQuality = (SeekBar)findViewById(R.id.seekQuality);

		final Gallery gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setAdapter(new ImageAdapter(this));
		gallery.setCallbackDuringFling(true);
		gallery.setOnItemSelectedListener(new OnItemSelectedListener() {
			TextView lblFileName = (TextView)findViewById(R.id.txtPicNo);
			
			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {
				String labelName = (position + 1) + " of " + adapter.getCount();
				String fileName = (String)adapter.getItemAtPosition(position);
				lblFileName.setText(labelName);
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapter) {
				lblFileName.setText("");
			}
        });
		
		gallery.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				// TODO Auto-generated method stub
				return false;
			}
		});

		
		Button lCamera = (Button)findViewById(R.id.btnTakePic);
		lCamera.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View xView) {
				width = Integer.parseInt(txtWidth.getText().toString());
				quality = seekQuality.getProgress();
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				fileUri = getOutputPictureFileUri(team); 
				intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

				// start the image capture Intent
				startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
			}
		});
		
		Button lReplace = (Button)findViewById(R.id.btnReplace);
		lReplace.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View xView) {
				width = Integer.parseInt(txtWidth.getText().toString());
				quality = seekQuality.getProgress();
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				fileUri = getOutputPictureFileUri(team); 
				intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
				picNo = gallery.getLastVisiblePosition() + 1;

				// start the image capture Intent
				startActivityForResult(intent, REPLACE_IMAGE_ACTIVITY_REQUEST_CODE);
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
					picNo = nextPic;
				}
				String outPath = fileUri.getPath().replace(".jpg", "_small-" + picNo + ".jpg");
				ResizePicture(fileUri.getPath(), width, outPath);
				if (UploadPicture(outPath) && requestCode ==  CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
					nextPic++;
				}
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

	protected void ResizePicture(String imagePath, int desiredWidth, String imageOutPath) {
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
			out = new FileOutputStream(imageOutPath);
			scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
			scaledBitmap = null;
		} catch (FileNotFoundException ex) {
			Log.d(TAG, "ResizePicture", ex);
		}
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

	static Bitmap downloadBitmap(String url) {
		
		final AndroidHttpClient client = AndroidHttpClient.newInstance("FRIG");
		final HttpGet getRequest = new HttpGet(url);

		try {
			HttpResponse response = client.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) { 
				Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url); 
				return null;
			}

			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = null;
				try {
					inputStream = entity.getContent(); 
					final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
					return bitmap;
				} finally {
					if (inputStream != null) {
						inputStream.close();  
					}
					entity.consumeContent();
				}
			}
		} catch (Exception e) {
			// Could provide a more explicit error message for IOException or IllegalStateException
			getRequest.abort();
			Log.d("ImageDownloader", "Error while retrieving bitmap from " + url, e);
		} finally {
			if (client != null) {
				client.close();
			}
		}
		return null;
	}

	public class ImageDownloader {

		public void download(String url, ImageView imageView) {
			if (cancelPotentialDownload(url, imageView)) {
				BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
				DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
				imageView.setImageDrawable(downloadedDrawable);
				task.execute(url);
			}
		}

		class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
			private String url;
			private final WeakReference<ImageView> imageViewReference;

			public BitmapDownloaderTask(ImageView imageView) {
				imageViewReference = new WeakReference<ImageView>(imageView);
			}

			@Override
			// Actual download method, run in the task thread
			protected Bitmap doInBackground(String... params) {
				// params comes from the execute() call: params[0] is the url.
				return downloadBitmap(params[0]);
			}

			@Override
			// Once the image is downloaded, associates it to the imageView
			protected void onPostExecute(Bitmap bitmap) {
				if (isCancelled()) {
					bitmap = null;
				}

				if (imageViewReference != null) {
					ImageView imageView = imageViewReference.get();
					BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
					// Change bitmap only if this process is still associated with it
					if (this == bitmapDownloaderTask) {
						imageView.setImageBitmap(bitmap);
					}
				}
			}
		}
	}

	static class DownloadedDrawable extends ColorDrawable {
		private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

		public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
			super(Color.BLACK);
			bitmapDownloaderTaskReference =
					new WeakReference<BitmapDownloaderTask>(bitmapDownloaderTask);
		}

		public BitmapDownloaderTask getBitmapDownloaderTask() {
			return bitmapDownloaderTaskReference.get();
		}
	}

	public class ImageAdapter extends BaseAdapter {
		int mGalleryItemBackground;
		private Context mContext;

		String[] pics = GetCurrentPictures(team);

		public ImageAdapter(Context c) {
			mContext = c;
			TypedArray attr = mContext.obtainStyledAttributes(R.styleable.FRIG);
			mGalleryItemBackground = attr.getResourceId(
					R.styleable.FRIG_android_galleryItemBackground, 0);
			attr.recycle();
		}

		public int getCount() {
			return pics.length;
		}

		public Object getItem(int position) {
			return pics[position];
		}

		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView = new ImageView(mContext);

			ImageDownloader downloader = new ImageDownloader();
			downloader.download(pics[position], imageView);
			imageView.setLayoutParams(new Gallery.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 300));
			imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			imageView.setBackgroundResource(mGalleryItemBackground);

			return imageView;
		}
	}
}
