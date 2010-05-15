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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Random;

/**
 * This class contains various utilities to manipulate Bitmaps. The methods of this class,
 * although static, are not thread safe and cannot be invoked by several threads at the
 * same time. Synchronization is required by the caller.
 */
final class ImageUtilities {
    private static final float PHOTO_BORDER_WIDTH = 3.0f;
    private static final int PHOTO_BORDER_COLOR = 0xffffffff;

    private static final float ROTATION_ANGLE_MIN = 2.5f;
    private static final float ROTATION_ANGLE_EXTRA = 5.5f;

    private static final Random sRandom = new Random();
    private static final Paint sPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final Paint sStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        sStrokePaint.setStrokeWidth(PHOTO_BORDER_WIDTH);
        sStrokePaint.setStyle(Paint.Style.STROKE);
        sStrokePaint.setColor(PHOTO_BORDER_COLOR);
    }

    /**
     * Rotate specified Bitmap by a random angle. The angle is either negative or positive,
     * and ranges, in degrees, from 2.5 to 8. After rotation a frame is overlaid on top
     * of the rotated image.
     *
     * This method is not thread safe.
     *
     * @param bitmap The Bitmap to rotate and apply a frame onto.
     *
     * @return A new Bitmap whose dimension are different from the original bitmap.
     */
    static Bitmap rotateAndFrame(Bitmap bitmap) {
        final boolean positive = sRandom.nextFloat() >= 0.5f;
        final float angle = (ROTATION_ANGLE_MIN + sRandom.nextFloat() * ROTATION_ANGLE_EXTRA) *
                (positive ? 1.0f : -1.0f);
        final double radAngle = Math.toRadians(angle);

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        final double cosAngle = Math.abs(Math.cos(radAngle));
        final double sinAngle = Math.abs(Math.sin(radAngle));

        final int strokedWidth = (int) (bitmapWidth + 2 * PHOTO_BORDER_WIDTH);
        final int strokedHeight = (int) (bitmapHeight + 2 * PHOTO_BORDER_WIDTH);

        final int width = (int) (strokedHeight * sinAngle + strokedWidth * cosAngle);
        final int height = (int) (strokedWidth * sinAngle + strokedHeight * cosAngle);

        final float x = (width - bitmapWidth) / 2.0f;
        final float y = (height - bitmapHeight) / 2.0f;

        final Bitmap decored = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(decored);

        canvas.rotate(angle, width / 2.0f, height / 2.0f);
        canvas.drawBitmap(bitmap, x, y, sPaint);
        canvas.drawRect(x, y, x + bitmapWidth, y + bitmapHeight, sStrokePaint);

        return decored;
    }

    /**
     * Scales the specified Bitmap to fit within the specified dimensions. After scaling,
     * a frame is overlaid on top of the scaled image.
     *
     * This method is not thread safe.
     *
     * @param bitmap The Bitmap to scale to fit the specified dimensions and to apply
     *               a frame onto.
     * @param width The maximum width of the new Bitmap.
     * @param height The maximum height of the new Bitmap.
     *
     * @return A scaled version of the original bitmap, whose dimension are less than or
     *         equal to the specified width and height.
     */
    static Bitmap scaleAndFrame(Bitmap bitmap, int width, int height) {
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        final float scale = Math.min((float) width / (float) bitmapWidth, 
                (float) height / (float) bitmapHeight);

        final int scaledWidth = (int) (bitmapWidth * scale);
        final int scaledHeight = (int) (bitmapHeight * scale);

        final Bitmap decored = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        final Canvas canvas = new Canvas(decored);

        final int offset = (int) (PHOTO_BORDER_WIDTH / 2);
        sStrokePaint.setAntiAlias(false);
        canvas.drawRect(offset, offset, scaledWidth - offset - 1,
                scaledHeight - offset - 1, sStrokePaint);
        sStrokePaint.setAntiAlias(true);

        return decored;

    }
}
