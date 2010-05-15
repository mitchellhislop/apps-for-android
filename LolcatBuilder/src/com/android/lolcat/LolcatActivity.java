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

package com.android.lolcat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Lolcat builder activity.
 *
 * Instructions:
 * (1) Take photo of cat using Camera
 * (2) Run LolcatActivity
 * (3) Pick photo
 * (4) Add caption(s)
 * (5) Save and share
 *
 * See README.txt for a list of currently-missing features and known bugs.
 */
public class LolcatActivity extends Activity
        implements View.OnClickListener {
    private static final String TAG = "LolcatActivity";

    // Location on the SD card for saving lolcat images
    private static final String LOLCAT_SAVE_DIRECTORY = "lolcats/";

    // Mime type / format / extension we use (must be self-consistent!)
    private static final String SAVED_IMAGE_EXTENSION = ".png";
    private static final Bitmap.CompressFormat SAVED_IMAGE_COMPRESS_FORMAT =
            Bitmap.CompressFormat.PNG;
    private static final String SAVED_IMAGE_MIME_TYPE = "image/png";

    // UI Elements
    private Button mPickButton;
    private Button mCaptionButton;
    private Button mSaveButton;
    private Button mClearCaptionButton;
    private Button mClearPhotoButton;
    private LolcatView mLolcatView;

    private AlertDialog mCaptionDialog;
    private ProgressDialog mSaveProgressDialog;
    private AlertDialog mSaveSuccessDialog;

    private Handler mHandler;

    private Uri mPhotoUri;

    private String mSavedImageFilename;
    private Uri mSavedImageUri;

    private MediaScannerConnection mMediaScannerConnection;

    // Request codes used with startActivityForResult()
    private static final int PHOTO_PICKED = 1;

    // Dialog IDs
    private static final int DIALOG_CAPTION = 1;
    private static final int DIALOG_SAVE_PROGRESS = 2;
    private static final int DIALOG_SAVE_SUCCESS = 3;

    // Keys used with onSaveInstanceState()
    private static final String PHOTO_URI_KEY = "photo_uri";
    private static final String SAVED_IMAGE_FILENAME_KEY = "saved_image_filename";
    private static final String SAVED_IMAGE_URI_KEY = "saved_image_uri";
    private static final String TOP_CAPTION_KEY = "top_caption";
    private static final String BOTTOM_CAPTION_KEY = "bottom_caption";
    private static final String CAPTION_POSITIONS_KEY = "caption_positions";


    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate()...  icicle = " + icicle);

        super.onCreate(icicle);

        setContentView(R.layout.lolcat_activity);

        // Look up various UI elements

        mPickButton = (Button) findViewById(R.id.pick_button);
        mPickButton.setOnClickListener(this);

        mCaptionButton = (Button) findViewById(R.id.caption_button);
        mCaptionButton.setOnClickListener(this);

        mSaveButton = (Button) findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(this);

        mClearCaptionButton = (Button) findViewById(R.id.clear_caption_button);
        // This button doesn't exist in portrait mode.
        if (mClearCaptionButton != null) mClearCaptionButton.setOnClickListener(this);

        mClearPhotoButton = (Button) findViewById(R.id.clear_photo_button);
        mClearPhotoButton.setOnClickListener(this);

        mLolcatView = (LolcatView) findViewById(R.id.main_image);

        // Need one of these to call back to the UI thread
        // (and run AlertDialog.show(), for that matter)
        mHandler = new Handler();

        mMediaScannerConnection = new MediaScannerConnection(this, mMediaScanConnClient);

        if (icicle != null) {
            Log.i(TAG, "- reloading state from icicle!");
            restoreStateFromIcicle(icicle);
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()...");
        super.onResume();

        updateButtons();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()...");
        super.onSaveInstanceState(outState);

        // State from the Activity:
        outState.putParcelable(PHOTO_URI_KEY, mPhotoUri);
        outState.putString(SAVED_IMAGE_FILENAME_KEY, mSavedImageFilename);
        outState.putParcelable(SAVED_IMAGE_URI_KEY, mSavedImageUri);

        // State from the LolcatView:
        // (TODO: Consider making Caption objects, or even the LolcatView
        // itself, Parcelable?  Probably overkill, though...)
        outState.putString(TOP_CAPTION_KEY, mLolcatView.getTopCaption());
        outState.putString(BOTTOM_CAPTION_KEY, mLolcatView.getBottomCaption());
        outState.putIntArray(CAPTION_POSITIONS_KEY, mLolcatView.getCaptionPositions());
    }

    /**
     * Restores the activity state from the specified icicle.
     * @see onCreate()
     * @see onSaveInstanceState()
     */
    private void restoreStateFromIcicle(Bundle icicle) {
        Log.i(TAG, "restoreStateFromIcicle()...");

        // State of the Activity:

        Uri photoUri = icicle.getParcelable(PHOTO_URI_KEY);
        Log.i(TAG, "  - photoUri: " + photoUri);
        if (photoUri != null) {
            loadPhoto(photoUri);
        }

        mSavedImageFilename = icicle.getString(SAVED_IMAGE_FILENAME_KEY);
        mSavedImageUri = icicle.getParcelable(SAVED_IMAGE_URI_KEY);

        // State of the LolcatView:

        String topCaption = icicle.getString(TOP_CAPTION_KEY);
        String bottomCaption = icicle.getString(BOTTOM_CAPTION_KEY);
        int[] captionPositions = icicle.getIntArray(CAPTION_POSITIONS_KEY);
        Log.i(TAG, "  - captions: '" + topCaption + "', '" + bottomCaption + "'");
        if (!TextUtils.isEmpty(topCaption) || !TextUtils.isEmpty(bottomCaption)) {
            mLolcatView.setCaptions(topCaption, bottomCaption);
            mLolcatView.setCaptionPositions(captionPositions);
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()...");
        super.onDestroy();
        clearPhoto();  // Free up some resources, and force a GC
    }

    // View.OnClickListener implementation
    public void onClick(View view) {
        int id = view.getId();
        Log.i(TAG, "onClick(View " + view + ", id " + id + ")...");

        switch (id) {
            case R.id.pick_button:
                Log.i(TAG, "onClick: pick_button...");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                intent.setType("image/*");

                // Note: we could have the "crop" UI come up here by
                // default by doing this:
                //   intent.putExtra("crop", "true");
                // (But watch out: if you do that, the Intent that comes
                // back to onActivityResult() will have the URI (of the
                // cropped image) in the "action" field, not the "data"
                // field!)

                startActivityForResult(intent, PHOTO_PICKED);
                break;

            case R.id.caption_button:
                Log.i(TAG, "onClick: caption_button...");
                showCaptionDialog();
                break;

            case R.id.save_button:
                Log.i(TAG, "onClick: save_button...");
                saveImage();
                break;

            case R.id.clear_caption_button:
                Log.i(TAG, "onClick: clear_caption_button...");
                clearCaptions();
                updateButtons();
                break;

            case R.id.clear_photo_button:
                Log.i(TAG, "onClick: clear_photo_button...");
                clearPhoto();  // Also does clearCaptions()
                updateButtons();
                break;

            default:
                Log.w(TAG, "Click from unexpected source: " + view + ", id " + id);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(request " + requestCode
              + ", result " + resultCode + ", data " + data + ")...");

        if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
            Toast t = Toast.makeText(this, R.string.lolcat_nothing_picked, Toast.LENGTH_SHORT);
            t.show();
            return;
        }

        if (requestCode == PHOTO_PICKED) {
            // "data" is an Intent containing (presumably) a URI like
            // "content://media/external/images/media/3".

            if (data == null) {
                Log.w(TAG, "Null data, but RESULT_OK, from image picker!");
                Toast t = Toast.makeText(this, R.string.lolcat_nothing_picked,
                                         Toast.LENGTH_SHORT);
                t.show();
                return;
            }

            if (data.getData() == null) {
                Log.w(TAG, "'data' intent from image picker contained no data!");
                Toast t = Toast.makeText(this, R.string.lolcat_nothing_picked,
                                         Toast.LENGTH_SHORT);
                t.show();
                return;
            }

            loadPhoto(data.getData());
            updateButtons();
        }
    }

    /**
     * Updates the enabled/disabled state of the onscreen buttons.
     */
    private void updateButtons() {
        Log.i(TAG, "updateButtons()...");

        // mPickButton is always enabled.

        // Do we have a valid photo and/or caption(s) yet?
        Drawable d = mLolcatView.getDrawable();
        // Log.i(TAG, "===> current mLolcatView drawable: " + d);
        boolean validPhoto = (d != null);
        boolean validCaption = mLolcatView.hasValidCaption();

        mCaptionButton.setText(validCaption
                               ? R.string.lolcat_change_captions : R.string.lolcat_add_captions);
        mCaptionButton.setEnabled(validPhoto);

        mSaveButton.setEnabled(validPhoto && validCaption);

        if (mClearCaptionButton != null) {
            mClearCaptionButton.setEnabled(validPhoto && validCaption);
        }

        mClearPhotoButton.setEnabled(validPhoto);
    }

    /**
     * Clears out any already-entered captions for this lolcat.
     */
    private void clearCaptions() {
        mLolcatView.clearCaptions();

        // Clear the text fields in the caption dialog too.
        if (mCaptionDialog != null) {
            EditText topText = (EditText) mCaptionDialog.findViewById(R.id.top_edittext);
            topText.setText("");
            EditText bottomText = (EditText) mCaptionDialog.findViewById(R.id.bottom_edittext);
            bottomText.setText("");
            topText.requestFocus();
        }

        // This also invalidates any image we've previously written to the
        // SD card...
        mSavedImageFilename = null;
        mSavedImageUri = null;
    }

    /**
     * Completely resets the UI to its initial state, with no photo
     * loaded, and no captions.
     */
    private void clearPhoto() {
        mLolcatView.clear();

        mPhotoUri = null;
        mSavedImageFilename = null;
        mSavedImageUri = null;

        clearCaptions();

        // Force a gc (to be sure to reclaim the memory used by our
        // potentially huge bitmap):
        System.gc();
    }

    /**
     * Loads the image with the specified Uri into the UI.
     */
    private void loadPhoto(Uri uri) {
        Log.i(TAG, "loadPhoto: uri = " + uri);

        clearPhoto();  // Be sure to release the previous bitmap
                       // before creating another one
        mPhotoUri = uri;

        // A new photo always starts out uncaptioned.
        clearCaptions();

        // Load the selected photo into our ImageView.
        mLolcatView.loadFromUri(mPhotoUri);
    }

    private void showCaptionDialog() {
        // If the dialog already exists, always reset focus to the top
        // item each time it comes up.
        if (mCaptionDialog != null) {
            EditText topText = (EditText) mCaptionDialog.findViewById(R.id.top_edittext);
            topText.requestFocus();
        }

        showDialog(DIALOG_CAPTION);
    }

    private void showSaveSuccessDialog() {
        // If the dialog already exists, update the body text based on the
        // current values of mSavedImageFilename and mSavedImageUri
        // (otherwise the dialog will still have the body text from when
        // it was first created!)

        if (mSaveSuccessDialog != null) {
            updateSaveSuccessDialogBody();
        }
        showDialog(DIALOG_SAVE_SUCCESS);
    }

    private void updateSaveSuccessDialogBody() {
        if (mSaveSuccessDialog == null) {
            throw new IllegalStateException(
                    "updateSaveSuccessDialogBody: mSaveSuccessDialog hasn't been created yet");
        }
        String dialogBody = String.format(
                getResources().getString(R.string.lolcat_save_succeeded_dialog_body_format),
                mSavedImageFilename, mSavedImageUri);
        mSaveSuccessDialog.setMessage(dialogBody);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Log.i(TAG, "onCreateDialog(id " + id + ")...");

        // This is only run once (per dialog), the very first time
        // a given dialog needs to be shown.

        switch (id) {
            case DIALOG_CAPTION:
                LayoutInflater factory = LayoutInflater.from(this);
                final View textEntryView = factory.inflate(R.layout.lolcat_caption_dialog, null);
                mCaptionDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.lolcat_caption_dialog_title)
                        .setIcon(0)
                        .setView(textEntryView)
                        .setPositiveButton(
                                R.string.lolcat_caption_dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Log.i(TAG, "Caption dialog: OK...");
                                        updateCaptionsFromDialog();
                                    }
                                })
                        .setNegativeButton(
                                R.string.lolcat_caption_dialog_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Log.i(TAG, "Caption dialog: CANCEL...");
                                        // Nothing to do here (for now at least)
                                    }
                                })
                        .create();
                return mCaptionDialog;

            case DIALOG_SAVE_PROGRESS:
                mSaveProgressDialog = new ProgressDialog(this);
                mSaveProgressDialog.setMessage(getResources().getString(R.string.lolcat_saving));
                mSaveProgressDialog.setIndeterminate(true);
                mSaveProgressDialog.setCancelable(false);
                return mSaveProgressDialog;

            case DIALOG_SAVE_SUCCESS:
                mSaveSuccessDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.lolcat_save_succeeded_dialog_title)
                        .setIcon(0)
                        .setPositiveButton(
                                R.string.lolcat_save_succeeded_dialog_view,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Log.i(TAG, "Save dialog: View...");
                                        viewSavedImage();
                                    }
                                })
                        .setNeutralButton(
                                R.string.lolcat_save_succeeded_dialog_share,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Log.i(TAG, "Save dialog: Share...");
                                        shareSavedImage();
                                    }
                                })
                        .setNegativeButton(
                                R.string.lolcat_save_succeeded_dialog_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Log.i(TAG, "Save dialog: CANCEL...");
                                        // Nothing to do here...
                                    }
                                })
                        .create();
                updateSaveSuccessDialogBody();
                return mSaveSuccessDialog;

            default:
                Log.w(TAG, "Request for unexpected dialog id: " + id);
                break;
        }
        return null;
    }

    private void updateCaptionsFromDialog() {
        Log.i(TAG, "updateCaptionsFromDialog()...");

        if (mCaptionDialog == null) {
            Log.w(TAG, "updateCaptionsFromDialog: null mCaptionDialog!");
            return;
        }

        // Get the two caption strings:

        EditText topText = (EditText) mCaptionDialog.findViewById(R.id.top_edittext);
        Log.i(TAG, "- Top editText: " + topText);
        String topString = topText.getText().toString();
        Log.i(TAG, "  - String: '" + topString + "'");

        EditText bottomText = (EditText) mCaptionDialog.findViewById(R.id.bottom_edittext);
        Log.i(TAG, "- Bottom editText: " + bottomText);
        String bottomString = bottomText.getText().toString();
        Log.i(TAG, "  - String: '" + bottomString + "'");

        mLolcatView.setCaptions(topString, bottomString);
        updateButtons();
    }


    /**
     * Kicks off the process of saving the LolcatView's working Bitmap to
     * the SD card, in preparation for viewing it later and/or sharing it.
     */
    private void saveImage() {
        Log.i(TAG, "saveImage()...");

        // First of all, bring up a progress dialog.
        showDialog(DIALOG_SAVE_PROGRESS);

        // We now need to save the bitmap to the SD card, and then ask the
        // MediaScanner to scan it.  Do the actual work of all this in a
        // helper thread, since it's fairly slow (and will occasionally
        // ANR if we do it here in the UI thread.)

        Thread t = new Thread() {
                public void run() {
                    Log.i(TAG, "Running worker thread...");
                    saveImageInternal();
                }
            };
        t.start();
        // Next steps:
        // - saveImageInternal()
        // - onMediaScannerConnected()
        // - onScanCompleted
    }

    /**
     * Saves the LolcatView's working Bitmap to the SD card, in
     * preparation for viewing it later and/or sharing it.
     *
     * The bitmap will be saved as a new file in the directory
     * LOLCAT_SAVE_DIRECTORY, with an automatically-generated filename
     * based on the current time.  It also connects to the
     * MediaScanner service, since we'll need to scan that new file (in
     * order to get a Uri we can then VIEW or share.)
     *
     * This method is run in a worker thread; @see saveImage().
     */
    private void saveImageInternal() {
        Log.i(TAG, "saveImageInternal()...");

        // TODO: Currently we save the bitmap to a file on the sdcard,
        // then ask the MediaScanner to scan it (which gives us a Uri we
        // can then do an ACTION_VIEW on.)  But rather than doing these
        // separate steps, maybe there's some easy way (given an
        // OutputStream) to directly talk to the MediaProvider
        // (i.e. com.android.provider.MediaStore) and say "here's an
        // image, please save it somwhere and return the URI to me"...

        // Save the bitmap to a file on the sdcard.
        // (Based on similar code in MusicUtils.java.)
        // TODO: Make this filename more human-readable?  Maybe "Lolcat-YYYY-MM-DD-HHMMSS.png"?
        String filename = Environment.getExternalStorageDirectory()
                + "/" + LOLCAT_SAVE_DIRECTORY
                + String.valueOf(System.currentTimeMillis() + SAVED_IMAGE_EXTENSION);
        Log.i(TAG, "- filename: '" + filename + "'");

        if (ensureFileExists(filename)) {
            try {
                OutputStream outstream = new FileOutputStream(filename);
                Bitmap bitmap = mLolcatView.getWorkingBitmap();
                boolean success = bitmap.compress(SAVED_IMAGE_COMPRESS_FORMAT,
                                                  100, outstream);
                Log.i(TAG, "- success code from Bitmap.compress: " + success);
                outstream.close();

                if (success) {
                    Log.i(TAG, "- Saved!  filename = " + filename);
                    mSavedImageFilename = filename;

                    // Ok, now we need to get the MediaScanner to scan the
                    // file we just wrote.  Step 1 is to get our
                    // MediaScannerConnection object to connect to the
                    // MediaScanner service.
                    mMediaScannerConnection.connect();
                    // See onMediaScannerConnected() for the next step
                } else {
                    Log.w(TAG, "Bitmap.compress failed: bitmap " + bitmap
                          + ", filename '" + filename + "'");
                    onSaveFailed(R.string.lolcat_save_failed);
                }
            } catch (FileNotFoundException e) {
                Log.w(TAG, "error creating file", e);
                onSaveFailed(R.string.lolcat_save_failed);
            } catch (IOException e) {
                Log.w(TAG, "error creating file", e);
                onSaveFailed(R.string.lolcat_save_failed);
            }
        } else {
            Log.w(TAG, "ensureFileExists failed for filename '" + filename + "'");
            onSaveFailed(R.string.lolcat_save_failed);
        }
    }


    //
    // MediaScanner-related code
    //

    /**
     * android.media.MediaScannerConnection.MediaScannerConnectionClient implementation.
     */
    private MediaScannerConnection.MediaScannerConnectionClient mMediaScanConnClient =
        new MediaScannerConnection.MediaScannerConnectionClient() {
            /**
             * Called when a connection to the MediaScanner service has been established.
             */
            public void onMediaScannerConnected() {
                Log.i(TAG, "MediaScannerConnectionClient.onMediaScannerConnected...");
                // The next step happens in the UI thread:
                mHandler.post(new Runnable() {
                        public void run() {
                            LolcatActivity.this.onMediaScannerConnected();
                        }
                    });
            }

            /**
             * Called when the media scanner has finished scanning a file.
             * @param path the path to the file that has been scanned.
             * @param uri the Uri for the file if the scanning operation succeeded
             *        and the file was added to the media database, or null if scanning failed.
             */
            public void onScanCompleted(final String path, final Uri uri) {
                Log.i(TAG, "MediaScannerConnectionClient.onScanCompleted: path "
                      + path + ", uri " + uri);
                // Just run the "real" onScanCompleted() method in the UI thread:
                mHandler.post(new Runnable() {
                        public void run() {
                            LolcatActivity.this.onScanCompleted(path, uri);
                        }
                    });
            }
        };

    /**
     * This method is called when our MediaScannerConnection successfully
     * connects to the MediaScanner service.  At that point we fire off a
     * request to scan the lolcat image we just saved.
     *
     * This needs to run in the UI thread, so it's called from
     * mMediaScanConnClient's onMediaScannerConnected() method via our Handler.
     */
    private void onMediaScannerConnected() {
        Log.i(TAG, "onMediaScannerConnected()...");

        // Update the message in the progress dialog...
        mSaveProgressDialog.setMessage(getResources().getString(R.string.lolcat_scanning));

        // Fire off a request to the MediaScanner service to scan this
        // file; we'll get notified when the scan completes.
        Log.i(TAG, "- Requesting scan for file: " + mSavedImageFilename);
        mMediaScannerConnection.scanFile(mSavedImageFilename,
                                         null /* mimeType */);

        // Next step: mMediaScanConnClient will get an onScanCompleted() callback,
        // which calls our own onScanCompleted() method via our Handler.
    }

    /**
     * Updates the UI after the media scanner finishes the scanFile()
     * request we issued from onMediaScannerConnected().
     *
     * This needs to run in the UI thread, so it's called from
     * mMediaScanConnClient's onScanCompleted() method via our Handler.
     */
    private void onScanCompleted(String path, final Uri uri) {
        Log.i(TAG, "onScanCompleted: path " + path + ", uri " + uri);
        mMediaScannerConnection.disconnect();

        if (uri == null) {
            Log.w(TAG, "onScanCompleted: scan failed.");
            mSavedImageUri = null;
            onSaveFailed(R.string.lolcat_scan_failed);
            return;
        }

        // Success!

        dismissDialog(DIALOG_SAVE_PROGRESS);

        // We can now access the saved lolcat image using the specified Uri.
        mSavedImageUri = uri;

        // Bring up a success dialog, giving the user the option to go to
        // the pictures app (so you can share the image).
        showSaveSuccessDialog();
    }


    //
    // Other misc utility methods
    //

    /**
     * Ensure that the specified file exists on the SD card, creating it
     * if necessary.
     *
     * Copied from MediaProvider / MusicUtils.
     *
     * @return true if the file already exists, or we
     *         successfully created it.
     */
    private static boolean ensureFileExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            return true;
        } else {
            // we will not attempt to create the first directory in the path
            // (for example, do not create /sdcard if the SD card is not mounted)
            int secondSlash = path.indexOf('/', 1);
            if (secondSlash < 1) return false;
            String directoryPath = path.substring(0, secondSlash);
            File directory = new File(directoryPath);
            if (!directory.exists())
                return false;
            file.getParentFile().mkdirs();
            try {
                return file.createNewFile();
            } catch (IOException ioe) {
                Log.w(TAG, "File creation failed", ioe);
            }
            return false;
        }
    }

    /**
     * Updates the UI after a failure anywhere in the bitmap saving / scanning
     * sequence.
     */
    private void onSaveFailed(int errorMessageResId) {
        dismissDialog(DIALOG_SAVE_PROGRESS);
        Toast.makeText(this, errorMessageResId, Toast.LENGTH_SHORT).show();
    }

    /**
     * Goes to the Pictures app for the specified URI.
     */
    private void viewSavedImage(Uri uri) {
        Log.i(TAG, "viewSavedImage(" + uri + ")...");

        if (uri == null) {
            Log.w(TAG, "viewSavedImage: null uri!");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        Log.i(TAG, "- running startActivity...  Intent = " + intent);
        startActivity(intent);
    }

    private void viewSavedImage() {
        viewSavedImage(mSavedImageUri);
    }

    /**
     * Shares the image with the specified URI.
     */
    private void shareSavedImage(Uri uri) {
        Log.i(TAG, "shareSavedImage(" + uri + ")...");

        if (uri == null) {
            Log.w(TAG, "shareSavedImage: null uri!");
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType(SAVED_IMAGE_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        try {
            startActivity(
                    Intent.createChooser(
                            intent,
                            getResources().getString(R.string.lolcat_sendImage_label)));
        } catch (android.content.ActivityNotFoundException ex) {
            Log.w(TAG, "shareSavedImage: startActivity failed", ex);
            Toast.makeText(this, R.string.lolcat_share_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareSavedImage() {
        shareSavedImage(mSavedImageUri);
    }
}
