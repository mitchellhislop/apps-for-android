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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

/**
 * Activity which displays the list of images.
 */
public class ImageList extends ListActivity {
    
    ImageManager mImageManager;
    
    private MyDataSetObserver mObserver = new MyDataSetObserver();

    /**
     * The zoom level the user chose when picking the search area
     */
    private int mZoom;

    /**
     * The latitude of the center of the search area chosen by the user
     */
    private int mLatitudeE6;

    /**
     * The longitude of the center of the search area chosen by the user
     */
    private int mLongitudeE6;

    /**
     * Observer used to turn the progress indicator off when the {@link ImageManager} is
     * done downloading.
     */
    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            if (!mImageManager.isLoading()) {
                getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                        Window.PROGRESS_VISIBILITY_OFF);
            }
        }

        @Override
        public void onInvalidated() {
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        mImageManager = ImageManager.getInstance(this);
        ListView listView = getListView();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View footer = inflater.inflate(R.layout.list_footer, listView, false);
        listView.addFooterView(footer, null, false);
        setListAdapter(new ImageAdapter(this));

        // Theme.Light sets a background on our list.
        listView.setBackgroundDrawable(null);
        if (mImageManager.isLoading()) {
            getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                    Window.PROGRESS_VISIBILITY_ON);
            mImageManager.addObserver(mObserver);
        }
        
        // Read the user's search area from the intent
        Intent i = getIntent();
        mZoom = i.getIntExtra(ImageManager.ZOOM_EXTRA, Integer.MIN_VALUE);
        mLatitudeE6 = i.getIntExtra(ImageManager.LATITUDE_E6_EXTRA, Integer.MIN_VALUE);
        mLongitudeE6 = i.getIntExtra(ImageManager.LONGITUDE_E6_EXTRA, Integer.MIN_VALUE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        PanoramioItem item = mImageManager.get(position);   
        
        // Create an intent to show a particular item.
        // Pass the user's search area along so the next activity can use it
        Intent i = new Intent(this, ViewImage.class);
        i.putExtra(ImageManager.PANORAMIO_ITEM_EXTRA, item);
        i.putExtra(ImageManager.ZOOM_EXTRA, mZoom);
        i.putExtra(ImageManager.LATITUDE_E6_EXTRA, mLatitudeE6);
        i.putExtra(ImageManager.LONGITUDE_E6_EXTRA, mLongitudeE6);
        startActivity(i);
    }   
    
    
}