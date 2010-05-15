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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;


/**
 * Lolcat-specific subclass of ImageView, which manages the various
 * scaled-down Bitmaps and knows how to render and manipulate the
 * image captions.
 */
public class LolcatView extends ImageView {
    private static final String TAG = "LolcatView";

    // Standard lolcat size is 500x375.  (But to preserve the original
    // image's aspect ratio, we rescale so that the larger dimension ends
    // up being 500 pixels.)
    private static final float SCALED_IMAGE_MAX_DIMENSION = 500f;

    // Other standard lolcat image parameters
    private static final int FONT_SIZE = 44;

    private Bitmap mScaledBitmap;  // The photo picked by the user, scaled-down
    private Bitmap mWorkingBitmap;  // The Bitmap we render the caption text into

    // Current state of the captions.
    // TODO: This array currently has a hardcoded length of 2 (for "top"
    // and "bottom" captions), but eventually should support as many
    // captions as the user wants to add.
    private final Caption[] mCaptions = new Caption[] { new Caption(), new Caption() };

    // State used while dragging a caption around
    private boolean mDragging;
    private int mDragCaptionIndex;  // index of the caption (in mCaptions[]) that's being dragged
    private int mTouchDownX, mTouchDownY;
    private final Rect mInitialDragBox = new Rect();
    private final Rect mCurrentDragBox = new Rect();
    private final RectF mCurrentDragBoxF = new RectF();  // used in onDraw()
    private final RectF mTransformedDragBoxF = new RectF();  // used in onDraw()
    private final Rect mTmpRect = new Rect();

    public LolcatView(Context context) {
        super(context);
    }

    public LolcatView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LolcatView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public Bitmap getWorkingBitmap() {
        return mWorkingBitmap;
    }

    public String getTopCaption() {
        return mCaptions[0].caption;
    }

    public String getBottomCaption() {
        return mCaptions[1].caption;
    }

    /**
     * @return true if the user has set caption(s) for this LolcatView.
     */
    public boolean hasValidCaption() {
        return !TextUtils.isEmpty(mCaptions[0].caption)
                || !TextUtils.isEmpty(mCaptions[1].caption);
    }

    public void clear() {
        mScaledBitmap = null;
        mWorkingBitmap = null;
        setImageDrawable(null);

        // TODO: Anything else we need to do here to release resources
        // associated with this object, like maybe the Bitmap that got
        // created by the previous setImageURI() call?
    }

    public void loadFromUri(Uri uri) {
        // For now, directly load the specified Uri.
        setImageURI(uri);

        // TODO: Rather than calling setImageURI() with the URI of
        // the (full-size) photo, it would be better to turn the URI into
        // a scaled-down Bitmap right here, and load *that* into ourself.
        // I'd do that basically the same way that ImageView.setImageURI does it:
        //     [ . . . ]
        //     android.graphics.BitmapFactory.nativeDecodeStream(Native Method)
        //     android.graphics.BitmapFactory.decodeStream(BitmapFactory.java:304)
        //     android.graphics.drawable.Drawable.createFromStream(Drawable.java:635)
        //     android.widget.ImageView.resolveUri(ImageView.java:477)
        //     android.widget.ImageView.setImageURI(ImageView.java:281)
        //     [ . . . ]
        // But for now let's let ImageView do the work: we call setImageURI (above)
        // and immediately pull out a Bitmap (below).

        // Stash away a scaled-down bitmap.
        // TODO: is it safe to assume this will always be a BitmapDrawable?
        BitmapDrawable drawable = (BitmapDrawable) getDrawable();
        Log.i(TAG, "===> current drawable: " + drawable);

        Bitmap fullSizeBitmap = drawable.getBitmap();
        Log.i(TAG, "===> fullSizeBitmap: " + fullSizeBitmap
              + "  dimensions: " + fullSizeBitmap.getWidth()
              + " x " + fullSizeBitmap.getHeight());

        Bitmap.Config config = fullSizeBitmap.getConfig();
        Log.i(TAG, "  - config = " + config);

        // Standard lolcat size is 500x375.  But we don't want to distort
        // the image if it isn't 4x3, so let's just set the larger
        // dimension to 500 pixels and preserve the source aspect ratio.

        float origWidth = fullSizeBitmap.getWidth();
        float origHeight = fullSizeBitmap.getHeight();
        float aspect = origWidth / origHeight;
        Log.i(TAG, "  - aspect = " + aspect + "(" + origWidth + " x " + origHeight + ")");

        float scaleFactor = ((aspect > 1.0) ? origWidth : origHeight) / SCALED_IMAGE_MAX_DIMENSION;
        int scaledWidth = Math.round(origWidth / scaleFactor);
        int scaledHeight = Math.round(origHeight / scaleFactor);

        mScaledBitmap = Bitmap.createScaledBitmap(fullSizeBitmap,
                                                  scaledWidth,
                                                  scaledHeight,
                                                  true /* filter */);
        Log.i(TAG, "  ===> mScaledBitmap: " + mScaledBitmap
              + "  dimensions: " + mScaledBitmap.getWidth()
              + " x " + mScaledBitmap.getHeight());
        Log.i(TAG, "       isMutable = " + mScaledBitmap.isMutable());
    }

    /**
     * Sets the captions for this LolcatView.
     */
    public void setCaptions(String topCaption, String bottomCaption) {
        Log.i(TAG, "setCaptions: '" + topCaption + "', '" + bottomCaption + "'");
        if (topCaption == null) topCaption = "";
        if (bottomCaption == null) bottomCaption = "";

        mCaptions[0].caption = topCaption;
        mCaptions[1].caption = bottomCaption;

        // If the user clears a caption, reset its position (so that it'll
        // come back in the default position if the user re-adds it.)
        if (TextUtils.isEmpty(mCaptions[0].caption)) {
            Log.i(TAG, "- invalidating position of caption 0...");
            mCaptions[0].positionValid = false;
        }
        if (TextUtils.isEmpty(mCaptions[1].caption)) {
            Log.i(TAG, "- invalidating position of caption 1...");
            mCaptions[1].positionValid = false;
        }

        // And *any* time the captions change, blow away the cached
        // caption bounding boxes to make sure we'll recompute them in
        // renderCaptions().
        mCaptions[0].captionBoundingBox = null;
        mCaptions[1].captionBoundingBox = null;

        renderCaptions(mCaptions);
    }

    /**
     * Clears the captions for this LolcatView.
     */
    public void clearCaptions() {
        setCaptions("", "");
    }

    /**
     * Renders this LolcatView's current image captions into our
     * underlying ImageView.
     *
     * We start with a scaled-down version of the photo originally chosed
     * by the user (mScaledBitmap), make a mutable copy (mWorkingBitmap),
     * render the specified strings into the bitmap, and show the
     * resulting image onscreen.
     */
    public void renderCaptions(Caption[] captions) {
        // TODO: handle an arbitrary array of strings, rather than
        // assuming "top" and "bottom" captions.

        String topString = captions[0].caption;
        boolean topStringValid = !TextUtils.isEmpty(topString);

        String bottomString = captions[1].caption;
        boolean bottomStringValid = !TextUtils.isEmpty(bottomString);

        Log.i(TAG, "renderCaptions: '" + topString + "', '" + bottomString + "'");

        if (mScaledBitmap == null) return;

        // Make a fresh (mutable) copy of the scaled-down photo Bitmap,
        // and render the desired text into it.

        Bitmap.Config config = mScaledBitmap.getConfig();
        Log.i(TAG, "  - mScaledBitmap config = " + config);

        mWorkingBitmap = mScaledBitmap.copy(config, true /* isMutable */);
        Log.i(TAG, "  ===> mWorkingBitmap: " + mWorkingBitmap
              + "  dimensions: " + mWorkingBitmap.getWidth()
              + " x " + mWorkingBitmap.getHeight());
        Log.i(TAG, "       isMutable = " + mWorkingBitmap.isMutable());

        Canvas canvas = new Canvas(mWorkingBitmap);
        Log.i(TAG, "- Canvas: " + canvas
              + "  dimensions: " + canvas.getWidth() + " x " + canvas.getHeight());

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(FONT_SIZE);
        textPaint.setColor(0xFFFFFFFF);
        Log.i(TAG, "- Paint: " + textPaint);

        Typeface face = textPaint.getTypeface();
        Log.i(TAG, "- default typeface: " + face);

        // The most standard font for lolcat captions is Impact.  (Arial
        // Black is also common.)  Unfortunately we don't have either of
        // these on the device by default; the closest we can do is
        // DroidSans-Bold:
        face = Typeface.DEFAULT_BOLD;
        Log.i(TAG, "- new face: " + face);
        textPaint.setTypeface(face);

        // Look up the positions of the captions, or if this is our very
        // first time rendering them, initialize the positions to default
        // values.

        final int edgeBorder = 20;
        final int fontHeight = textPaint.getFontMetricsInt(null);
        Log.i(TAG, "- fontHeight: " + fontHeight);

        Log.i(TAG, "- Caption positioning:");
        int topX = 0;
        int topY = 0;
        if (topStringValid) {
            if (mCaptions[0].positionValid) {
                topX = mCaptions[0].xpos;
                topY = mCaptions[0].ypos;
                Log.i(TAG, "  - TOP: already had a valid position: " + topX + ", " + topY);
            } else {
                // Start off with the "top" caption at the upper-left:
                topX = edgeBorder;
                topY = edgeBorder + (fontHeight * 3 / 4);
                mCaptions[0].setPosition(topX, topY);
                Log.i(TAG, "  - TOP: initializing to default position: " + topX + ", " + topY);
            }
        }

        int bottomX = 0;
        int bottomY = 0;
        if (bottomStringValid) {
            if (mCaptions[1].positionValid) {
                bottomX = mCaptions[1].xpos;
                bottomY = mCaptions[1].ypos;
                Log.i(TAG, "  - Bottom: already had a valid position: "
                      + bottomX + ", " + bottomY);
            } else {
                // Start off with the "bottom" caption at the lower-right:
                final int bottomTextWidth = (int) textPaint.measureText(bottomString);
                Log.i(TAG, "- bottomTextWidth (" + bottomString + "): " + bottomTextWidth);
                bottomX = canvas.getWidth() - edgeBorder - bottomTextWidth;
                bottomY = canvas.getHeight() - edgeBorder;
                mCaptions[1].setPosition(bottomX, bottomY);
                Log.i(TAG, "  - BOTTOM: initializing to default position: "
                      + bottomX + ", " + bottomY);
            }
        }

        // Finally, render the text.

        // Standard lolcat captions are drawn in white with a heavy black
        // outline (i.e. white fill, black stroke).  Our Canvas APIs can't
        // do this exactly, though.
        // We *could* get something decent-looking using a regular
        // drop-shadow, like this:
        //   textPaint.setShadowLayer(3.0f, 3, 3, 0xff000000);
        // but instead let's simulate the "outline" style by drawing the
        // text 4 separate times, with the shadow in a different direction
        // each time.
        // (TODO: This is a hack, and still doesn't look as good
        // as a real "white fill, black stroke" style.)

        final float shadowRadius = 2.0f;
        final int shadowOffset = 2;
        final int shadowColor = 0xff000000;

        // TODO: Right now we use offsets of 2,2 / -2,2 / 2,-2 / -2,-2 .
        // But 2,0 / 0,2 / -2,0 / 0,-2 might look better.

        textPaint.setShadowLayer(shadowRadius, shadowOffset, shadowOffset, shadowColor);
        if (topStringValid) canvas.drawText(topString, topX, topY, textPaint);
        if (bottomStringValid) canvas.drawText(bottomString, bottomX, bottomY, textPaint);
        //
        textPaint.setShadowLayer(shadowRadius, -shadowOffset, shadowOffset, shadowColor);
        if (topStringValid) canvas.drawText(topString, topX, topY, textPaint);
        if (bottomStringValid) canvas.drawText(bottomString, bottomX, bottomY, textPaint);
        //
        textPaint.setShadowLayer(shadowRadius, shadowOffset, -shadowOffset, shadowColor);
        if (topStringValid) canvas.drawText(topString, topX, topY, textPaint);
        if (bottomStringValid) canvas.drawText(bottomString, bottomX, bottomY, textPaint);
        //
        textPaint.setShadowLayer(shadowRadius, -shadowOffset, -shadowOffset, shadowColor);
        if (topStringValid) canvas.drawText(topString, topX, topY, textPaint);
        if (bottomStringValid) canvas.drawText(bottomString, bottomX, bottomY, textPaint);

        // Stash away bounding boxes for the captions if this
        // is our first time rendering them.
        // Watch out: the x/y position we use for drawing the text is
        // actually the *lower* left corner of the bounding box...

        int textWidth, textHeight;

        if (topStringValid && mCaptions[0].captionBoundingBox == null) {
            Log.i(TAG, "- Computing initial bounding box for top caption...");
            textPaint.getTextBounds(topString, 0, topString.length(), mTmpRect);
            textWidth = mTmpRect.width();
            textHeight = mTmpRect.height();
            Log.i(TAG, "-  text dimensions: " + textWidth + " x " + textHeight);
            mCaptions[0].captionBoundingBox = new Rect(topX, topY - textHeight,
                                                       topX + textWidth, topY);
            Log.i(TAG, "-   RESULTING RECT: " + mCaptions[0].captionBoundingBox);
        }
        if (bottomStringValid && mCaptions[1].captionBoundingBox == null) {
            Log.i(TAG, "- Computing initial bounding box for bottom caption...");
            textPaint.getTextBounds(bottomString, 0, bottomString.length(), mTmpRect);
            textWidth = mTmpRect.width();
            textHeight = mTmpRect.height();
            Log.i(TAG, "-  text dimensions: " + textWidth + " x " + textHeight);
            mCaptions[1].captionBoundingBox = new Rect(bottomX, bottomY - textHeight,
                                                       bottomX + textWidth, bottomY);
            Log.i(TAG, "-   RESULTING RECT: " + mCaptions[1].captionBoundingBox);
        }

        // Finally, display the new Bitmap to the user:
        setImageBitmap(mWorkingBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.i(TAG, "onDraw: " + canvas);
        super.onDraw(canvas);

        if (mDragging) {
            Log.i(TAG, "- dragging!  Drawing box at " + mCurrentDragBox);

            // mCurrentDragBox is in the coordinate system of our bitmap;
            // need to convert it into the coordinate system of the
            // overall LolcatView.
            //
            // To transform between coordinate systems we need to apply the
            // transformation described by the ImageView's matrix *and* also
            // account for our left and top padding.

            Matrix m = getImageMatrix();

            mCurrentDragBoxF.set(mCurrentDragBox);
            m.mapRect(mTransformedDragBoxF, mCurrentDragBoxF);
            mTransformedDragBoxF.offset(getPaddingLeft(), getPaddingTop());

            Paint p = new Paint();
            p.setColor(0xFFFFFFFF);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f);
            Log.i(TAG, "- Paint: " + p);

            canvas.drawRect(mTransformedDragBoxF, p);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.i(TAG, "onTouchEvent: " + ev);

        // Watch out: ev.getX() and ev.getY() are in the
        // coordinate system of the entire LolcatView, although
        // all the positions and rects we use here (like
        // mCaptions[].captionBoundingBox) are relative to the bitmap
        // that's drawn inside the LolcatView.
        //
        // To transform between coordinate systems we need to apply the
        // transformation described by the ImageView's matrix *and* also
        // account for our left and top padding.

        Matrix m = getImageMatrix();

        Matrix invertedMatrix = new Matrix();
        m.invert(invertedMatrix);

        float[] pointArray = new float[] { ev.getX() - getPaddingLeft(),
                                           ev.getY() - getPaddingTop() };
        Log.i(TAG, "  - BEFORE: pointArray = " + pointArray[0] + ", " + pointArray[1]);

        // Transform the X/Y position of the DOWN event back into bitmap coords
        invertedMatrix.mapPoints(pointArray);
        Log.i(TAG, "  - AFTER:  pointArray = " + pointArray[0] + ", " + pointArray[1]);

        int eventX = (int) pointArray[0];
        int eventY = (int) pointArray[1];

        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mDragging) {
                    Log.w(TAG, "Got an ACTION_DOWN, but we were already dragging!");
                    mDragging = false;  // and continue as if we weren't already dragging...
                }
                if (!hasValidCaption()) {
                    Log.w(TAG, "No caption(s) yet; ignoring this ACTION_DOWN event.");
                    return true;
                }

                // See if this DOWN event hit one of the caption bounding
                // boxes.  If so, start dragging!
                for (int i = 0; i < mCaptions.length; i++) {
                    Rect boundingBox = mCaptions[i].captionBoundingBox;
                    Log.i(TAG, "  - boundingBox #" + i + ": " + boundingBox + "...");

                    if (boundingBox != null) {
                        // Expand the bounding box by a fudge factor to make it
                        // easier to hit (since touch accuracy is pretty poor on a
                        // real device, and the captions are fairly small...)
                        mTmpRect.set(boundingBox);

                        final int touchPositionSlop = 40;  // pixels
                        mTmpRect.inset(-touchPositionSlop, -touchPositionSlop);

                        Log.i(TAG, "  - Checking expanded bounding box #" + i
                              + ": " + mTmpRect + "...");
                        if (mTmpRect.contains(eventX, eventY)) {
                            Log.i(TAG, "    - Hit! " + mCaptions[i]);
                            mDragging = true;
                            mDragCaptionIndex = i;
                            break;
                        }
                    }
                }
                if (!mDragging) {
                    Log.i(TAG, "- ACTION_DOWN event didn't hit any captions; ignoring.");
                    return true;
                }

                mTouchDownX = eventX;
                mTouchDownY = eventY;

                mInitialDragBox.set(mCaptions[mDragCaptionIndex].captionBoundingBox);
                mCurrentDragBox.set(mCaptions[mDragCaptionIndex].captionBoundingBox);

                invalidate();

                return true;

            case MotionEvent.ACTION_MOVE:
                if (!mDragging) {
                    return true;
                }

                int displacementX = eventX - mTouchDownX;
                int displacementY = eventY - mTouchDownY;

                mCurrentDragBox.set(mInitialDragBox);
                mCurrentDragBox.offset(displacementX, displacementY);

                invalidate();

                return true;

            case MotionEvent.ACTION_UP:
                if (!mDragging) {
                    return true;
                }

                mDragging = false;

                // Reposition the selected caption!
                Log.i(TAG, "- Done dragging!  Repositioning caption #" + mDragCaptionIndex + ": "
                      + mCaptions[mDragCaptionIndex]);

                int offsetX = eventX - mTouchDownX;
                int offsetY = eventY - mTouchDownY;
                Log.i(TAG, "  - OFFSET: " + offsetX + ", " + offsetY);

                // Reposition the the caption we just dragged, and blow
                // away the cached bounding box to make sure it'll get
                // recomputed in renderCaptions().
                mCaptions[mDragCaptionIndex].xpos += offsetX;
                mCaptions[mDragCaptionIndex].ypos += offsetY;
                mCaptions[mDragCaptionIndex].captionBoundingBox = null;

                Log.i(TAG, "  - Updated caption: " + mCaptions[mDragCaptionIndex]);

                // Finally, refresh the screen.
                renderCaptions(mCaptions);
                return true;

            // This case isn't expected to happen.
            case MotionEvent.ACTION_CANCEL:
                if (!mDragging) {
                    return true;
                }

                mDragging = false;
                // Refresh the screen.
                renderCaptions(mCaptions);
                return true;

            default:
                return super.onTouchEvent(ev);
        }
    }

    /**
     * Returns an array containing the xpos/ypos of each Caption in our
     * array of captions.  (This method and setCaptionPositions() are used
     * by LolcatActivity to save and restore the activity state across
     * orientation changes.)
     */
    public int[] getCaptionPositions() {
        // TODO: mCaptions currently has a hardcoded length of 2 (for
        // "top" and "bottom" captions).
        int[] captionPositions = new int[4];

        if (mCaptions[0].positionValid) {
            captionPositions[0] = mCaptions[0].xpos;
            captionPositions[1] = mCaptions[0].ypos;
        } else {
            captionPositions[0] = -1;
            captionPositions[1] = -1;
        }

        if (mCaptions[1].positionValid) {
            captionPositions[2] = mCaptions[1].xpos;
            captionPositions[3] = mCaptions[1].ypos;
        } else {
            captionPositions[2] = -1;
            captionPositions[3] = -1;
        }

        Log.i(TAG, "getCaptionPositions: returning " + captionPositions);
        return captionPositions;
    }

    /**
     * Sets the xpos and ypos values of each Caption in our array based on
     * the specified values.  (This method and getCaptionPositions() are
     * used by LolcatActivity to save and restore the activity state
     * across orientation changes.)
     */
    public void setCaptionPositions(int[] captionPositions) {
        // TODO: mCaptions currently has a hardcoded length of 2 (for
        // "top" and "bottom" captions).

        Log.i(TAG, "setCaptionPositions(" + captionPositions + ")...");

        if (captionPositions[0] < 0) {
            mCaptions[0].positionValid = false;
            Log.i(TAG, "- TOP caption: no valid position");
        } else {
            mCaptions[0].setPosition(captionPositions[0], captionPositions[1]);
            Log.i(TAG, "- TOP caption: got valid position: "
                  + mCaptions[0].xpos + ", " + mCaptions[0].ypos);
        }

        if (captionPositions[2] < 0) {
            mCaptions[1].positionValid = false;
            Log.i(TAG, "- BOTTOM caption: no valid position");
        } else {
            mCaptions[1].setPosition(captionPositions[2], captionPositions[3]);
            Log.i(TAG, "- BOTTOM caption: got valid position: "
                  + mCaptions[1].xpos + ", " + mCaptions[1].ypos);
        }

        // Finally, refresh the screen.
        renderCaptions(mCaptions);
    }

    /**
     * Structure used to hold the entire state of a single caption.
     */
    class Caption {
        public String caption;
        public Rect captionBoundingBox;  // updated by renderCaptions()
        public int xpos, ypos;
        public boolean positionValid;

        public void setPosition(int x, int y) {
            positionValid = true;
            xpos = x;
            ypos = y;
            // Also blow away the cached bounding box, to make sure it'll
            // get recomputed in renderCaptions().
            captionBoundingBox = null;
        }

        @Override
        public String toString() {
            return "Caption['" + caption + "'; bbox " + captionBoundingBox
                    + "; pos " + xpos + ", " + ypos + "; posValid = " + positionValid + "]";
        }
    }

}
