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

import org.apache.http.entity.SerializableEntity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FRIGImageAdapter extends BaseAdapter {
	// an object we'll use to keep our cache data together
	public class Image {
		String url;
		String name;
		String caption;
		Bitmap thumb;
	}

	// an array of resources we want to display
	private ArrayList<Image> mImages;
	private boolean mGetAll;
	private String mTeam;
	private int mRegion;

	// a context so we can later create a view within it
	private Context mContext;

	// the background task objects
	private LoadThumbsTask mThumbnailGen;
	private GetDataTask mDataGetter;
	private DiskLruImageCache mCache;

	// Constructor
	public FRIGImageAdapter(Context c, Object previousList, String team, int region) {
		mContext = c;

		// get our thumbnail generation task ready to execute
		mImages = new ArrayList<Image>();
		mCache = new DiskLruImageCache(c, "FRIG", 52428800, CompressFormat.JPEG, 70);
		Image defaultImage = new Image();
		defaultImage.url = "";
		defaultImage.name = "Default";
		defaultImage.caption = "Default";
		defaultImage.thumb = null;
		mImages.add(defaultImage);
		mTeam = team;
		mRegion = region;

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
			mDataGetter.execute("http://frig.marswars.org/TeamList.php?plain&region=" + region);
		} else {
			mDataGetter.execute("http://frig.marswars.org/TeamPics.php?team=" + team);
		}
	}
	
	public FRIGImageAdapter(Context c, Object previousList, String team) {
		this(c, previousList, team, -1);
	}
	
	public FRIGImageAdapter(Context c, Object previousList, int region) {
		this(c, previousList, "", region);
	}
	
	public void setRegion(int xRegion) {
		mRegion = xRegion;
		refresh();
	}
	
	public void refresh() {
		mImages.clear();
		this.notifyDataSetChanged();
		mDataGetter = new GetDataTask();
		if (mGetAll) {
			mDataGetter.execute("http://frig.marswars.org/TeamList.php?plain&region=" + mRegion);
		} else {
			mDataGetter.execute("http://frig.marswars.org/TeamPics.php?team=" + mTeam);
		}
	}

	private void dataGot(String[] data) {
		if (data != null) {
			mImages.clear();
			for(int i = 0, j = data.length; i < j; i++) {
				if (data[i].length() > 0) {
					mImages.add(new Image());
					if (mGetAll) {
						mImages.get(i).caption = data[i].substring(0, data[i].indexOf('|'));
						try {
							mImages.get(i).name = data[i].substring(data[i].lastIndexOf('|') + 1,
									data[i].indexOf("."));
						} catch (Exception ex) {
							mImages.get(i).name = "";
						}
					} else {
						mImages.get(i).caption = "";
						mImages.get(i).name = data[i].substring(data[i].lastIndexOf('/') + 1,
								data[i].indexOf('.', data[i].lastIndexOf('/')));
					}
					if (mGetAll) {
						mImages.get(i).url = "http://frig.marswars.org/images/" + mImages.get(i).caption + "/" + mImages.get(i).name + ".default.jpg"; 
					} else {
						mImages.get(i).url = data[i];
					}
					this.notifyDataSetChanged();
				}
			}
		}

		// start the background task to generate thumbs
		mThumbnailGen = new LoadThumbsTask();
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
		return mImages.get(position);
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
		ThumbnailView view;

		// pull the cached data for the image assigned to this position
		Image cached = mImages.get(position);
		int width = parent.getWidth();
		
		// can we recycle an old view?
		if(convertView == null) {
			// no view to recycle; create a new view
			if (mGetAll) {
				view = new IconView(mContext, cached);
			} else {
				view  = new ThumbnailView(mContext, cached);
			}
		} else {
			// recycle an old view (it might have old thumbs in it!)
			view = (ThumbnailView)convertView;
		}
		view.setLayoutParams(new GridView.LayoutParams(width / 2, width / 2));

		// do we have a thumb stored in cache?
		if(cached.thumb == null) {
			// no cached thumb, so let's set the view as blank
			view.setImage(R.drawable.ic_launcher);
		} else {
			// yes, cached thumb! use that image
			view.setImage(cached.thumb);
		}
		if (mGetAll) {
			((IconView) view).SetText(cached.caption);
		}
		return view;
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
	private Bitmap downloadThumb(String url) {

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
				if (i.thumb != null) continue;
				
				// check cache
				if (i.name != "") {
					if (mCache.containsKey(i.name)) {
						i.thumb = mCache.getBitmap(i.name);
					} else { // download thumb
						i.thumb = downloadThumb(i.url);
						if (i.thumb != null) {
							mCache.put(i.name, i.thumb);
						}
					}
				}

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
//				Authenticator.setDefault(new Authenticator() {
//					protected PasswordAuthentication getPasswordAuthentication() {
//						return new PasswordAuthentication("FRIGApp","correcthorsebatterystaple".toCharArray());
//					}
//				});
				URL url = new URL(address);
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
	
	class IconView extends ThumbnailView {
		
		private TextView mTextView;

		public IconView(Context context, Image image) {
			super(context, image);
			mTextView = new TextView(context);
			mTextView.setText(image.caption);
			mTextView.setTextSize(25);
			mTextView.setTextColor(Color.WHITE);
			mTextView.setBackgroundColor(0xaa888888);

			LayoutParams textLayout = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			textLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			addView(mTextView, textLayout);
			mTextView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
		}

		public void SetText(String name) {
			mTextView.setText(name);
			this.postInvalidate();
		}
	}
	
	class ThumbnailView extends RelativeLayout {
		
		private ImageView mImgView;

		public ThumbnailView(Context context, Image image) {
			super(context);
			
			mImgView = new ImageView(context);
			mImgView.setImageBitmap(image.thumb);
			mImgView.setScaleType(ScaleType.CENTER_CROP);

			addView(mImgView, 
					new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}

		public void setImage(Bitmap thumb) {
			mImgView.setScaleType(ScaleType.CENTER_CROP);
			mImgView.setImageBitmap(thumb);
			this.postInvalidate();
		}

		public void setImage(int icLauncher) {
			mImgView.setImageResource(icLauncher);
			mImgView.setScaleType(ScaleType.CENTER);
			this.postInvalidate();
		}
	}
	

}
