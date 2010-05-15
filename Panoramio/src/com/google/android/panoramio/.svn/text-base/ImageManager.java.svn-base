/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.panoramio;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;

/**
 * This class is responsible for downloading and parsing the search results for
 * a particular area. All of the work is done on a separate thread, and progress
 * is reported back through the DataSetObserver set in
 * {@link #addObserver(DataSetObserver). State is held in memory by in memory
 * maintained by a single instance of the ImageManager class.
 */
public class ImageManager {
   private static final String TAG = "Panoramio";
    
    /**
     * Base URL for Panoramio's web API
     */
    private static final String THUMBNAIL_URL = "//www.panoramio.com/map/get_panoramas.php?order=popularity&set=public&from=0&to=20&miny=%f&minx=%f&maxy=%f&maxx=%f&size=thumbnail";

    /**
     * Used to post results back to the UI thread
     */
    private Handler mHandler = new Handler();

    /**
     * Holds the single instance of a ImageManager that is shared by the process.
     */
    private static ImageManager sInstance;
    
    /**
     * Holds the images and related data that have been downloaded
     */
    private ArrayList<PanoramioItem> mImages = new ArrayList<PanoramioItem>();
    
    /**
     * Observers interested in changes to the current search results
     */
    private ArrayList<WeakReference<DataSetObserver>> mObservers = 
            new ArrayList<WeakReference<DataSetObserver>>();
    
    /**
     * True if we are in the process of loading
     */
    private boolean mLoading;
    
    private Context mContext;
    
    /**
     * Key for an Intent extra. The value is the zoom level selected by the user.
     */
    public static final String ZOOM_EXTRA = "zoom";

    /**
     * Key for an Intent extra. The value is the latitude of the center of the search
     * area chosen by the user.
     */
    public static final String LATITUDE_E6_EXTRA = "latitudeE6";

    /**
     * Key for an Intent extra. The value is the latitude of the center of the search
     * area chosen by the user.
     */
    public static final String LONGITUDE_E6_EXTRA = "longitudeE6";
    

    /**
     * Key for an Intent extra. The value is an item to display
     */
    public static final String PANORAMIO_ITEM_EXTRA = "item";
    
    public static ImageManager getInstance(Context c) {
        if (sInstance == null) {
            sInstance = new ImageManager(c.getApplicationContext());
        }
        return sInstance;
    }
    
    private ImageManager(Context c) {
        mContext = c;
    }
    
    /**
     * @return True if we are still loading content
     */
    public boolean isLoading() {
        return mLoading;
    }
    
    /**
     * Clear all downloaded content
     */
    public void clear() {
        mImages.clear();   
        notifyObservers();
    }
    
    /**
     * Add an item to and notify observers of the change.
     * @param item The item to add
     */
    private void add(PanoramioItem item) {
        mImages.add(item);   
        notifyObservers();
    }
    
    /**
     * @return The number of items displayed so far
     */
    public int size() {
        return mImages.size();
    }

    /**
     * Gets the item at the specified position
     */
    public PanoramioItem get(int position) {
        return mImages.get(position);
    }
    
    /**
     * Adds an observer to be notified when the set of items held by this ImageManager changes.
     */
    public void addObserver(DataSetObserver observer) {
        WeakReference<DataSetObserver> obs = new WeakReference<DataSetObserver>(observer);
        mObservers.add(obs);
    }
    
    /**
     * Load a new set of search results for the specified area.
     * 
     * @param minLong The minimum longitude for the search area
     * @param maxLong The maximum longitude for the search area
     * @param minLat The minimum latitude for the search area
     * @param maxLat The minimum latitude for the search area
     */
    public void load(float minLong, float maxLong, float minLat, float maxLat) {
        mLoading = true;
        new NetworkThread(minLong, maxLong, minLat, maxLat).start();
    }
    
    /**
     * Called when something changes in our data set. Cleans up any weak references that
     * are no longer valid along the way.
     */
    private void notifyObservers() {
        final ArrayList<WeakReference<DataSetObserver>> observers = mObservers;
        final int count = observers.size();
        for (int i = count - 1; i >= 0; i--) {
            WeakReference<DataSetObserver> weak = observers.get(i);
            DataSetObserver obs = weak.get();
            if (obs != null) {
                obs.onChanged();
            } else {
                observers.remove(i);
            }
        }
        
    }
    
    /**
     * This thread does the actual work of downloading and parsing data.
     *
     */
    private class NetworkThread extends Thread {

        private float mMinLong;
        private float mMaxLong;
        private float mMinLat;
        private float mMaxLat;

        public NetworkThread(float minLong, float maxLong, float minLat, float maxLat) {
            mMinLong = minLong;
            mMaxLong = maxLong;
            mMinLat = minLat;
            mMaxLat = maxLat;
        }

        @Override
        public void run() {
            
            String url = THUMBNAIL_URL;
            url = String.format(url, mMinLat, mMinLong, mMaxLat, mMaxLong);
            try {
                URI uri = new URI("http", url, null);
                HttpGet get = new HttpGet(uri);
                
                HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(get);
                HttpEntity entity = response.getEntity();
                String str = convertStreamToString(entity.getContent());
                JSONObject json = new JSONObject(str);
                parse(json);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        
        private void parse(JSONObject json) {
            try {
                JSONArray array = json.getJSONArray("photos");
                int count = array.length();
                for (int i = 0; i < count; i++) {
                    JSONObject obj = array.getJSONObject(i);

                    long id = obj.getLong("photo_id");
                    String title = obj.getString("photo_title");
                    String owner = obj.getString("owner_name");
                    String thumb = obj.getString("photo_file_url");
                    String ownerUrl = obj.getString("owner_url");
                    String photoUrl = obj.getString("photo_url");
                    double latitude = obj.getDouble("latitude");
                    double longitude = obj.getDouble("longitude");
                    Bitmap b = BitmapUtils.loadBitmap(thumb);
                    if (title == null) {
                        title = mContext.getString(R.string.untitled);
                    }

                    final PanoramioItem item = new PanoramioItem(id, thumb, b,
                            (int) (latitude * Panoramio.MILLION),
                            (int) (longitude * Panoramio.MILLION), title, owner,
                            ownerUrl, photoUrl);
                    final boolean done = i == count - 1;
                    mHandler.post(new Runnable() {
                        public void run() {
                            
                            sInstance.mLoading = !done;
                            sInstance.add(item);
                        }
                    });
                }
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }

        private String convertStreamToString(InputStream is) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8*1024);
            StringBuilder sb = new StringBuilder();
     
            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
     
            return sb.toString();
        }

    }
    
}
