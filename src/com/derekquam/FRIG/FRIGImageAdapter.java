package com.derekquam.FRIG;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class FRIGImageAdapter extends BaseAdapter {
	// an object we'll use to keep our cache data together
	public class Image {
		String url;
		String name;
		Bitmap thumb;
	}

	// an array of resources we want to display
	private ArrayList<Image> mImages;
	private boolean mGetAll;

	// a context so we can later create a view within it
	private Context mContext;

	// the background task objects
	private LoadThumbsTask mThumbnailGen;
	private GetDataTask mDataGetter;

	// Constructor
	public FRIGImageAdapter(Context c, Object previousList, String team) {

		mContext = c;

		// get our thumbnail generation task ready to execute
		mThumbnailGen = new LoadThumbsTask();
		mImages = new ArrayList<Image>();
		Image defaultImage = new Image();
		defaultImage.url = "";
		defaultImage.name = "Default";
		defaultImage.thumb = null;
		mImages.add(defaultImage);

		// we'll want to use pre-existing data, if it exists
//		if(previousList != null) {
//			mImages = (Image[]) previousList;
//
//			// continue processing remaining thumbs in the background
//			mThumbnailGen.execute(mImages);
//
//			// no more setup required in this constructor
//			return;
//		}

		// if no pre-existing data, we need to generate it from scratch.
		mGetAll = team.isEmpty();
		mDataGetter = new GetDataTask();
		if (mGetAll) {
			mDataGetter.execute("http://www.derekquam.com/frig/TeamList.php?plain");
		} else {
			mDataGetter.execute("http://www.derekquam.com/frig/TeamPics.php?team=" + team);
		}
	}
	
	private void dataGot(String[] data) {
		if (data != null) {
			mImages.clear();
			for(int i = 0, j = data.length; i < j; i++) {
				mImages.add(new Image());
				mImages.get(i).name = data[i];
				if (mGetAll) {
					mImages.get(i).url =  "http://www.derekquam.com/frig/images/" + data[i] + "-1.jpg";
				} else {
					mImages.get(i).url = data[i];
				}
			}
		}

		// start the background task to generate thumbs
		mThumbnailGen.execute(mImages.toArray(new Image[mImages.size()]));
	}


	private String[] getTeam(String team) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	/**
	 * Getter: number of items in the adapter's data set
	 */
	public int getCount() {
		return mImages.size();
	}


	@Override
	/**
	 * Getter: return URL at specified position
	 */
	public Object getItem(int position) {
		return mImages.get(position).url;
	}


	@Override
	/**
	 * Getter: return resource ID of the item at the current position
	 */
	public long getItemId(int position) {
		return position;
	}


	/**
	 * Getter: return generated data
	 * @return array of Image
	 */
	public Object getData() {
		// stop the task if it isn't finished
		if(mThumbnailGen != null && mThumbnailGen.getStatus() != AsyncTask.Status.FINISHED) {
			// cancel the task
			mThumbnailGen.cancel(true);

		}

		// return generated thumbs
		return mImages;
	}


	/**
	 * Create a new ImageView when requested, filling it with a 
	 * thumbnail or a blank image if no thumb is ready yet.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imgView;

		// pull the cached data for the image assigned to this position
		Image cached = mImages.get(position);

		// can we recycle an old view?
		if(convertView == null) {
			// no view to recycle; create a new view
			imgView = new ImageView(mContext);
			int width = parent.getWidth();
			imgView.setLayoutParams(new GridView.LayoutParams(width / 2, width / 2));
		} else {
			// recycle an old view (it might have old thumbs in it!)
			imgView = (ImageView) convertView;
		}

		// do we have a thumb stored in cache?
		if(cached.thumb == null) {
			// no cached thumb, so let's set the view as blank
			imgView.setImageResource(R.drawable.ic_launcher);		
			imgView.setScaleType(ScaleType.CENTER);
		} else {
			// yes, cached thumb! use that image
			imgView.setScaleType(ScaleType.CENTER_CROP);
			imgView.setImageBitmap(cached.thumb);
		}

		return imgView;
	}


	/**
	 * Notify the adapter that our data has changed so it can
	 * refresh the views & display any newly-generated thumbs
	 */
	private void cacheUpdated() {
		this.notifyDataSetChanged();
	}


	/**
	 * Download and return a thumb specified by url, subsampling 
	 * it to a smaller size.
	 */
	private Bitmap loadThumb(String url) {

		// the downloaded thumb (none for now!)
		Bitmap thumb = null;

		// sub-sampling options
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = 4;

		try {
			// open a connection to the URL
			// Note: pay attention to permissions in the Manifest file!
			URL u = new URL(url);
			URLConnection c = u.openConnection();
			c.connect();

			// read data
			BufferedInputStream stream = new BufferedInputStream(c.getInputStream());

			// decode the data, subsampling along the way
			thumb = BitmapFactory.decodeStream(stream, null, opts);

			// close the stream
			stream.close();

		} catch (MalformedURLException e) {
			Log.e("FRIG", "malformed url: " + url);
		} catch (IOException e) {
			Log.e("FRIG", "An error has occurred downloading the image: " + url);
		}

		// return the fetched thumb (or null, if error)
		return thumb;
	}

	// the class that will create a background thread and generate thumbs
	private class LoadThumbsTask extends AsyncTask<Image, Void, Void> {

		/**
		 * Generate thumbs for each of the Image objects in the array
		 * passed to this method. This method is run in a background task.
		 */
		@Override
		protected Void doInBackground(Image... cache) {
			// define the options for our bitmap subsampling 
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inSampleSize = 4;

			// iterate over all images ...
			for (Image i : cache) {
				// if our task has been cancelled then let's stop processing
				if(isCancelled()) return null;

				// skip a thumb if it's already been generated
				if(i.thumb != null) continue;

				// artificially cause latency!
				//SystemClock.sleep(500);

				// download and generate a thumb for this image
				i.thumb = loadThumb(i.url);

				// some unit of work has been completed, update the UI
				publishProgress();
			}

			return null;
		}


		/**
		 * Update the UI thread when requested by publishProgress()
		 */
		@Override
		protected void onProgressUpdate(Void... param) {
			cacheUpdated();
		}
	}

	private class GetDataTask extends AsyncTask<String, Void, String[]> {

		@Override
		protected String[] doInBackground(String... addresses) {
			ArrayList<String> ret = new ArrayList<String>();
			for (int index = 0; index < addresses.length; index++) {
				String[] lines = getLinesFromUrl(addresses[index]);
				if (lines != null) {
					for (int line = 0; line < lines.length; line++) {
						ret.add(lines[line]);
					}
				}
			}
			return ret.toArray(new String[ret.size()]); 
		}

		private String[] getLinesFromUrl(String address) {
			try {
				Authenticator.setDefault(new Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication("FRIGApp","correcthorsebatterystaple".toCharArray());
					}
				});
				URL url = new URL(address);//"http://www.derekquam.com/frig/TeamList.php?plain");
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
			catch (Exception ex) {
				Log.e("FRIG", "getTeams", ex);
			}
			return null;
		}
		
		protected void onPostExecute(String[] result) {
			dataGot(result);
		}
	}
}
