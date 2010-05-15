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
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;


/**
 * Displays a custom map which shows our current location and the location
 * where the photo was taken.
 */
public class ViewMap extends MapActivity {
    private MapView mMapView;

    private MyLocationOverlay mMyLocationOverlay;

    ArrayList<PanoramioItem> mItems = null;

    private PanoramioItem mItem;

    private Drawable mMarker;

    private int mMarkerXOffset;

    private int mMarkerYOffset;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout frame = new FrameLayout(this);
        mMapView = new MapView(this, "MapViewCompassDemo_DummyAPIKey");
        frame.addView(mMapView, 
                new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        setContentView(frame);

        mMyLocationOverlay = new MyLocationOverlay(this, mMapView);

        mMarker = getResources().getDrawable(R.drawable.map_pin);
        
        // Make sure to give mMarker bounds so it will draw in the overlay
        final int intrinsicWidth = mMarker.getIntrinsicWidth();
        final int intrinsicHeight = mMarker.getIntrinsicHeight();
        mMarker.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
        
        mMarkerXOffset = -(intrinsicWidth / 2);
        mMarkerYOffset = -intrinsicHeight;
        
        // Read the item we are displaying from the intent, along with the 
        // parameters used to set up the map
        Intent i = getIntent();
        mItem = i.getParcelableExtra(ImageManager.PANORAMIO_ITEM_EXTRA);
        int mapZoom = i.getIntExtra(ImageManager.ZOOM_EXTRA, Integer.MIN_VALUE);
        int mapLatitudeE6 = i.getIntExtra(ImageManager.LATITUDE_E6_EXTRA, Integer.MIN_VALUE);
        int mapLongitudeE6 = i.getIntExtra(ImageManager.LONGITUDE_E6_EXTRA, Integer.MIN_VALUE);
        
        final List<Overlay> overlays = mMapView.getOverlays();
        overlays.add(mMyLocationOverlay);
        overlays.add(new PanoramioOverlay());
        
        final MapController controller = mMapView.getController();
        if (mapZoom != Integer.MIN_VALUE && mapLatitudeE6 != Integer.MIN_VALUE
                && mapLongitudeE6 != Integer.MIN_VALUE) {
            controller.setZoom(mapZoom);
            controller.setCenter(new GeoPoint(mapLatitudeE6, mapLongitudeE6));
        } else {
            controller.setZoom(15);
            mMyLocationOverlay.runOnFirstFix(new Runnable() {
                public void run() {
                    controller.animateTo(mMyLocationOverlay.getMyLocation());
                }
            });
        }

        mMapView.setClickable(true);
        mMapView.setEnabled(true);
        mMapView.setSatellite(true);
        addZoomControls(frame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMyLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onStop() {
        mMyLocationOverlay.disableMyLocation();
        super.onStop();
    }

    /**
     * Get the zoom controls and add them to the bottom of the map
     */
    private void addZoomControls(FrameLayout frame) {
        View zoomControls = mMapView.getZoomControls();

        FrameLayout.LayoutParams p = 
            new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.BOTTOM + Gravity.CENTER_HORIZONTAL);
        frame.addView(zoomControls, p);
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    /**
     * Custom overlay to display the Panoramio pushpin
     */
    public class PanoramioOverlay extends Overlay {
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (!shadow) {
                Point point = new Point();
                Projection p = mapView.getProjection();
                p.toPixels(mItem.getLocation(), point);
                super.draw(canvas, mapView, shadow);
                drawAt(canvas, mMarker, point.x + mMarkerXOffset, point.y + mMarkerYOffset, shadow);
            }
        }
    }
    
}
