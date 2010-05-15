/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.spritemethodtest;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;

/**
 * Activity for testing Canvas drawing speed.  This activity sets up sprites and
 * passes them off to a CanvasSurfaceView for rendering and movement.  It is
 * very similar to OpenGLTestActivity.  Note that Bitmap objects come out of a
 * pool and must be explicitly recycled on shutdown.  See onDestroy().
 */
public class CanvasTestActivity extends Activity {
    private CanvasSurfaceView mCanvasSurfaceView;
    // Describes the image format our bitmaps should be converted to.
    private static BitmapFactory.Options sBitmapOptions 
        = new BitmapFactory.Options();
    private Bitmap[] mBitmaps;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCanvasSurfaceView = new CanvasSurfaceView(this);
        SimpleCanvasRenderer spriteRenderer = new SimpleCanvasRenderer();
       
        // Sets our preferred image format to 16-bit, 565 format.
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;

        // Clear out any old profile results.
        ProfileRecorder.sSingleton.resetAll();
        
        final Intent callingIntent = getIntent();
        
        // Allocate our sprites and add them to an array.
        final int robotCount = callingIntent.getIntExtra("spriteCount", 10);
        final boolean animate = callingIntent.getBooleanExtra("animate", true);
        
        // Allocate space for the robot sprites + one background sprite.
        CanvasSprite[] spriteArray = new CanvasSprite[robotCount + 1];    
        
        mBitmaps = new Bitmap[4];
        mBitmaps[0] = loadBitmap(this, R.drawable.background);
        mBitmaps[1] = loadBitmap(this, R.drawable.skate1);
        mBitmaps[2] = loadBitmap(this, R.drawable.skate2);
        mBitmaps[3] = loadBitmap(this, R.drawable.skate3);
        
        // We need to know the width and height of the display pretty soon,
        // so grab the information now.
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        
        // Make the background.
        // Note that the background image is larger than the screen, 
        // so some clipping will occur when it is drawn.
        CanvasSprite background = new CanvasSprite(mBitmaps[0]);
        background.width = mBitmaps[0].getWidth();
        background.height = mBitmaps[0].getHeight();
        spriteArray[0] = background;
        
        // This list of things to move. It points to the same content as
        // spriteArray except for the background.
        Renderable[] renderableArray = new Renderable[robotCount]; 
        final int robotBucketSize = robotCount / 3;
        for (int x = 0; x < robotCount; x++) {
            CanvasSprite robot;
            // Our robots come in three flavors.  Split them up accordingly.
            if (x < robotBucketSize) {
                robot = new CanvasSprite(mBitmaps[1]);
            } else if (x < robotBucketSize * 2) {
                robot = new CanvasSprite(mBitmaps[2]);
            } else {
                robot = new CanvasSprite(mBitmaps[3]);
            }
            
            robot.width = 64;
            robot.height = 64;
            
            // Pick a random location for this sprite.
            robot.x = (float)(Math.random() * dm.widthPixels);
            robot.y = (float)(Math.random() * dm.heightPixels);
            
            // Add this robot to the spriteArray so it gets drawn and to the
            // renderableArray so that it gets moved.
            spriteArray[x + 1] = robot;
            renderableArray[x] = robot;
        }
        
       
        // Now's a good time to run the GC.  Since we won't do any explicit
        // allocation during the test, the GC should stay dormant and not
        // influence our results.
        Runtime r = Runtime.getRuntime();
        r.gc();
        
        spriteRenderer.setSprites(spriteArray);
        mCanvasSurfaceView.setRenderer(spriteRenderer);

        if (animate) {
            Mover simulationRuntime = new Mover();
            simulationRuntime.setRenderables(renderableArray);

            simulationRuntime.setViewSize(dm.widthPixels, dm.heightPixels);
        
            mCanvasSurfaceView.setEvent(simulationRuntime);
        }
        setContentView(mCanvasSurfaceView);
    }
    
    
    /** Recycles all of the bitmaps loaded in onCreate(). */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCanvasSurfaceView.clearEvent();
        mCanvasSurfaceView.stopDrawing();
        
        for (int x = 0; x < mBitmaps.length; x++) {
            mBitmaps[x].recycle();
            mBitmaps[x] = null;
        }
    }


    /**
     * Loads a bitmap from a resource and converts it to a bitmap.  This is
     * a much-simplified version of the loadBitmap() that appears in
     * SimpleGLRenderer.
     * @param context  The application context.
     * @param resourceId  The id of the resource to load.
     * @return  A bitmap containing the image contents of the resource, or null
     *     if there was an error.
     */
    protected Bitmap loadBitmap(Context context, int resourceId) {
        Bitmap bitmap = null;
        if (context != null) {
          
            InputStream is = context.getResources().openRawResource(resourceId);
            try {
                bitmap = BitmapFactory.decodeStream(is, null, sBitmapOptions);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }

        return bitmap;
    }
}
