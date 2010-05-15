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

package com.google.android.photostream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ViewAnimator;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;

/**
 * Activity that displays a photo along with its title and the date at which it was taken.
 * This activity also lets the user set the photo as the wallpaper.
 */
public class ViewPhotoActivity extends Activity implements View.OnClickListener,
        ViewTreeObserver.OnGlobalLayoutListener {

    static final String ACTION = "com.google.android.photostream.FLICKR_PHOTO";

    private static final String RADAR_ACTION = "com.google.android.radar.SHOW_RADAR";
    private static final String RADAR_EXTRA_LATITUDE = "latitude";
    private static final String RADAR_EXTRA_LONGITUDE = "longitude";

    private static final String EXTRA_PHOTO = "com.google.android.photostream.photo";

    private static final String WALLPAPER_FILE_NAME = "wallpaper";

    private static final int REQUEST_CROP_IMAGE = 42;

    private Flickr.Photo mPhoto;

    private ViewAnimator mSwitcher;
    private ImageView mPhotoView;
    private ViewGroup mContainer;

    private UserTask<?, ?, ?> mTask;
    private TextView mPhotoTitle;
    private TextView mPhotoDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPhoto = getPhoto();

        setContentView(R.layout.screen_photo);
        setupViews();
    }

    /**
     * Starts the ViewPhotoActivity for the specified photo.
     *
     * @param context The application's environment.
     * @param photo The photo to display and optionally set as a wallpaper.
     */
    static void show(Context context, Flickr.Photo photo) {
        final Intent intent = new Intent(ACTION);
        intent.putExtra(EXTRA_PHOTO, photo);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTask != null && mTask.getStatus() != UserTask.Status.RUNNING) {
            mTask.cancel(true);
        }
    }

    private void setupViews() {
        mContainer = (ViewGroup) findViewById(R.id.container_photo);
        mSwitcher = (ViewAnimator) findViewById(R.id.switcher_menu);
        mPhotoView = (ImageView) findViewById(R.id.image_photo);

        mPhotoTitle = (TextView) findViewById(R.id.caption_title);
        mPhotoDate = (TextView) findViewById(R.id.caption_date);

        findViewById(R.id.menu_back).setOnClickListener(this);
        findViewById(R.id.menu_set).setOnClickListener(this);

        mPhotoTitle.setText(mPhoto.getTitle());
        mPhotoDate.setText(mPhoto.getDate());

        mContainer.setVisibility(View.INVISIBLE);

        // Sets up a view tree observer. The photo will be scaled using the size
        // of one of our views so we must wait for the first layout pass to be
        // done to make sure we have the correct size.
        mContainer.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * Loads the photo after the first layout. The photo is scaled using the
     * dimension of the ImageView that will ultimately contain the photo's
     * bitmap. We make sure that the ImageView is laid out at least once to
     * get its correct size.
     */
    public void onGlobalLayout() {
        mContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        loadPhoto(mPhotoView.getMeasuredWidth(), mPhotoView.getMeasuredHeight());
    }

    /**
     * Loads the photo either from the last known instance or from the network.
     * Loading it from the last known instance allows for fast display rotation
     * without having to download the photo from the network again.
     *
     * @param width The desired maximum width of the photo.
     * @param height The desired maximum height of the photo.
     */
    private void loadPhoto(int width, int height) {
        final Object data = getLastNonConfigurationInstance();
        if (data == null) {
            mTask = new LoadPhotoTask().execute(mPhoto, width, height);
        } else {
            mPhotoView.setImageBitmap((Bitmap) data);
            mSwitcher.showNext();
        }
    }

    /**
     * Loads the {@link com.google.android.photostream.Flickr.Photo} to display
     * from the intent used to start the activity.
     *
     * @return The photo to display, or null if the photo cannot be found.
     */
    public Flickr.Photo getPhoto() {
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();

        Flickr.Photo photo = null;
        if (extras != null) {
            photo = extras.getParcelable(EXTRA_PHOTO);
        }

        return photo;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_photo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_radar:
                onShowRadar();
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void onShowRadar() {
        new ShowRadarTask().execute(mPhoto);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menu_back:
                onBack();
                break;
            case R.id.menu_set:
                onSet();
                break;
        }
    }

    private void onSet() {
        mTask = new CropWallpaperTask().execute(mPhoto);
    }

    private void onBack() {
        finish();
    }

    /**
     * If we successfully loaded a photo, send it to our future self to allow
     * for fast display rotation. By doing so, we avoid reloading the photo
     * from the network when the activity is taken down and recreated upon
     * display rotation.
     *
     * @return The Bitmap displayed in the ImageView, or null if the photo
     *         wasn't loaded.
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        final Drawable d = mPhotoView.getDrawable();
        return d != null ? ((BitmapDrawable) d).getBitmap() : null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Spawns a new task to set the wallpaper in a background thread when/if
        // we receive a successful result from the image cropper.
        if (requestCode == REQUEST_CROP_IMAGE) {
            if (resultCode == RESULT_OK) {
                mTask = new SetWallpaperTask().execute();
            } else {
                cleanupWallpaper();
                showWallpaperError();
            }
        }
    }

    private void showWallpaperError() {
        Toast.makeText(ViewPhotoActivity.this, R.string.error_cannot_save_file,
                Toast.LENGTH_SHORT).show();
    }

    private void showWallpaperSuccess() {
        Toast.makeText(ViewPhotoActivity.this, R.string.success_wallpaper_set,
                Toast.LENGTH_SHORT).show();
    }

    private void cleanupWallpaper() {
        deleteFile(WALLPAPER_FILE_NAME);
        mSwitcher.showNext();
    }

    /**
     * Background task to load the photo from Flickr. The task loads the bitmap,
     * then scale it to the appropriate dimension. The task ends by readjusting
     * the activity's layout so that everything aligns correctly.
     */
    private class LoadPhotoTask extends UserTask<Object, Void, Bitmap> {
        public Bitmap doInBackground(Object... params) {
            Bitmap bitmap = ((Flickr.Photo) params[0]).loadPhotoBitmap(Flickr.PhotoSize.MEDIUM);
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.not_found);
            }

            final int width = (Integer) params[1];
            final int height = (Integer) params[2];

            final Bitmap framed = ImageUtilities.scaleAndFrame(bitmap, width, height);
            bitmap.recycle();

            return framed;
        }

        @Override
        public void onPostExecute(Bitmap result) {
            mPhotoView.setImageBitmap(result);

            // Find by how many pixels the title and date must be shifted on the
            // horizontal axis to be left aligned with the photo
            final int offsetX = (mPhotoView.getMeasuredWidth() - result.getWidth()) / 2;

            // Forces the ImageView to have the same size as its embedded bitmap
            // This will remove the empty space between the title/date pair and
            // the photo itself
            LinearLayout.LayoutParams params;
            params = (LinearLayout.LayoutParams) mPhotoView.getLayoutParams();
            params.height = result.getHeight();
            params.weight = 0.0f;
            mPhotoView.setLayoutParams(params);

            params = (LinearLayout.LayoutParams) mPhotoTitle.getLayoutParams();
            params.leftMargin = offsetX;
            mPhotoTitle.setLayoutParams(params);

            params = (LinearLayout.LayoutParams) mPhotoDate.getLayoutParams();
            params.leftMargin = offsetX;
            mPhotoDate.setLayoutParams(params);

            mSwitcher.showNext();
            mContainer.startAnimation(AnimationUtils.loadAnimation(ViewPhotoActivity.this,
                    R.anim.fade_in));
            mContainer.setVisibility(View.VISIBLE);

            mTask = null;            
        }
    }

    /**
     * Background task to crop a large version of the image. The cropped result will
     * be set as a wallpaper. The tasks sarts by showing the progress bar, then
     * downloads the large version of hthe photo into a temporary file and ends by
     * sending an intent to the Camera application to crop the image.
     */
    private class CropWallpaperTask extends UserTask<Flickr.Photo, Void, Boolean> {
        private File mFile;

        @Override
        public void onPreExecute() {
            mFile = getFileStreamPath(WALLPAPER_FILE_NAME);
            mSwitcher.showNext();
        }

        public Boolean doInBackground(Flickr.Photo... params) {
            boolean success = false;

            OutputStream out = null;
            try {
                out = openFileOutput(mFile.getName(), MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
                Flickr.get().downloadPhoto(params[0], Flickr.PhotoSize.LARGE, out);
                success = true;
            } catch (FileNotFoundException e) {
                android.util.Log.e(Flickr.LOG_TAG, "Could not download photo", e);
                success = false;
            } catch (IOException e) {
                android.util.Log.e(Flickr.LOG_TAG, "Could not download photo", e);
                success = false;
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        success = false;
                    }
                }
            }

            return success;
        }

        @Override
        public void onPostExecute(Boolean result) {
            if (!result) {
                cleanupWallpaper();
                showWallpaperError();
            } else {
                final int width = getWallpaperDesiredMinimumWidth();
                final int height = getWallpaperDesiredMinimumHeight();

                final Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setClassName("com.android.camera", "com.android.camera.CropImage");
                intent.setData(Uri.fromFile(mFile));
                intent.putExtra("outputX", width);
                intent.putExtra("outputY", height);
                intent.putExtra("aspectX", width);
                intent.putExtra("aspectY", height);
                intent.putExtra("scale", true);
                intent.putExtra("noFaceDetection", true);
                intent.putExtra("output", Uri.parse("file:/" + mFile.getAbsolutePath()));

                startActivityForResult(intent, REQUEST_CROP_IMAGE);
            }

            mTask = null;
        }
    }

    /**
     * Background task to set the cropped image as the wallpaper. The task simply
     * open the temporary file and sets it as the new wallpaper. The task ends by
     * deleting the temporary file and display a message to the user.
     */
    private class SetWallpaperTask extends UserTask<Void, Void, Boolean> {
        public Boolean doInBackground(Void... params) {
            boolean success = false;
            InputStream in = null;
            try {
                in = openFileInput(WALLPAPER_FILE_NAME);
                setWallpaper(in);
                success = true;
            } catch (IOException e) {
                success = false;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        success = false;
                    }
                }
            }
            return success;
        }

        @Override
        public void onPostExecute(Boolean result) {
            cleanupWallpaper();

            if (!result) {
                showWallpaperError();
            } else {
                showWallpaperSuccess();
            }

            mTask = null;
        }
    }

    private class ShowRadarTask extends UserTask<Flickr.Photo, Void, Flickr.Location> {
        public Flickr.Location doInBackground(Flickr.Photo... params) {
            return Flickr.get().getLocation(params[0]);
        }

        @Override
        public void onPostExecute(Flickr.Location location) {
            if (location != null) {
                final Intent intent = new Intent(RADAR_ACTION);
                intent.putExtra(RADAR_EXTRA_LATITUDE, location.getLatitude());
                intent.putExtra(RADAR_EXTRA_LONGITUDE, location.getLongitude());

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ViewPhotoActivity.this, R.string.error_cannot_find_radar,
                        Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ViewPhotoActivity.this, R.string.error_cannot_find_location,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
