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

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * The Canvas version of a sprite.  This class keeps a pointer to a bitmap
 * and draws it at the Sprite's current location.
 */
public class CanvasSprite extends Renderable {
    private Bitmap mBitmap;
    
    public CanvasSprite(Bitmap bitmap) {
        mBitmap = bitmap;
    }
    
    public void draw(Canvas canvas) {
        // The Canvas system uses a screen-space coordinate system, that is,
        // 0,0 is the top-left point of the canvas.  But in order to align
        // with OpenGL's coordinate space (which places 0,0 and the lower-left),
        // for this test I flip the y coordinate.
        canvas.drawBitmap(mBitmap, x, canvas.getHeight() - (y + height), null);
    }
}
