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

import com.google.android.maps.GeoPoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Activity which displays a single image.
 */
public class ViewImage extends Activity {
    private static final String TAG = "Panoramio";

    private static final int MENU_RADAR = Menu.FIRST + 1;

    private static final int MENU_MAP = Menu.FIRST + 2;

    private static final int MENU_AUTHOR = Menu.FIRST + 3;

    private static final int MENU_VIEW = Menu.FIRST + 4;

    private static final int DIALOG_NO_RADAR = 1;

    PanoramioItem mItem;

    private Handler mHandler;

    private ImageView mImage;

    private TextView mTitle;

    private TextView mOwner;
    
    private View mContent;

    private int mMapZoom;

    private int mMapLatitudeE6;

    private int mMapLongitudeE6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_image);

        // Remember the user's original search area and zoom level
        Intent i = getIntent();
        mItem = i.getParcelableExtra(ImageManager.PANORAMIO_ITEM_EXTRA);
        mMapZoom = i.getIntExtra(ImageManager.ZOOM_EXTRA, Integer.MIN_VALUE);
        mMapLatitudeE6 = i.getIntExtra(ImageManager.LATITUDE_E6_EXTRA, Integer.MIN_VALUE);
        mMapLongitudeE6 = i.getIntExtra(ImageManager.LONGITUDE_E6_EXTRA, Integer.MIN_VALUE);
        
        mHandler = new Handler();

        mContent = findViewById(R.id.content);
        mImage = (ImageView) findViewById(R.id.image);
        mTitle = (TextView) findViewById(R.id.title);
        mOwner = (TextView) findViewById(R.id.owner);
        
        mContent.setVisibility(View.GONE);
        getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                Window.PROGRESS_VISIBILITY_ON);
        new LoadThread().start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_RADAR, 0, R.string.menu_radar)
                .setIcon(R.drawable.ic_menu_radar)
                .setAlphabeticShortcut('R');
        menu.add(0, MENU_MAP, 0, R.string.menu_map)
                .setIcon(R.drawable.ic_menu_map)
                .setAlphabeticShortcut('M');
        menu.add(0, MENU_AUTHOR, 0, R.string.menu_author)
                .setIcon(R.drawable.ic_menu_author)
                .setAlphabeticShortcut('A');
        menu.add(0, MENU_VIEW, 0, R.string.menu_view)
                .setIcon(android.R.drawable.ic_menu_view)
                .setAlphabeticShortcut('V');
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RADAR: {
            // Launch the radar activity (if it is installed)
            Intent i = new Intent("com.google.android.radar.SHOW_RADAR");
            GeoPoint location = mItem.getLocation();
            i.putExtra("latitude", (float)(location.getLatitudeE6() / 1000000f));
            i.putExtra("longitude", (float)(location.getLongitudeE6() / 1000000f));
            try {
                startActivity(i);
            } catch (ActivityNotFoundException ex) {
                showDialog(DIALOG_NO_RADAR);
            }
            return true;
        }
        case MENU_MAP: {
            // Display our custom map 
            Intent i = new Intent(this, ViewMap.class);
            i.putExtra(ImageManager.PANORAMIO_ITEM_EXTRA, mItem);
            i.putExtra(ImageManager.ZOOM_EXTRA, mMapZoom);
            i.putExtra(ImageManager.LATITUDE_E6_EXTRA, mMapLatitudeE6);
            i.putExtra(ImageManager.LONGITUDE_E6_EXTRA, mMapLongitudeE6);
            
            startActivity(i);

            return true;
        }
        case MENU_AUTHOR: {
            // Display the author info page in the browser
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(mItem.getOwnerUrl()));
            startActivity(i);
            return true;
        }
        case MENU_VIEW: {
            // Display the photo info page in the browser
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(mItem.getPhotoUrl()));
            startActivity(i);
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_NO_RADAR:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            return builder.setTitle(R.string.no_radar_title)
                .setMessage(R.string.no_radar)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, null).create();
        }
        return null;
    }


    /**
     * Utility to load a larger version of the image in a separate thread.
     *
     */
    private class LoadThread extends Thread {

        public LoadThread() {
        }

        @Override
        public void run() {
            try {
                String uri = mItem.getThumbUrl();
                uri = uri.replace("thumbnail", "medium");
                final Bitmap b = BitmapUtils.loadBitmap(uri);
                mHandler.post(new Runnable() {
                    public void run() {

                        mImage.setImageBitmap(b);
                        mTitle.setText(mItem.getTitle());
                        mOwner.setText(mItem.getOwner());
                        mContent.setVisibility(View.VISIBLE);
                        getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                                Window.PROGRESS_VISIBILITY_OFF);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

}