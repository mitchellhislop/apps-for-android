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

import android.graphics.Canvas;

import com.android.spritemethodtest.CanvasSurfaceView.Renderer;

/**
 * An extremely simple renderer based on the CanvasSurfaceView drawing
 * framework.  Simply draws a list of sprites to a canvas every frame.
 */
public class SimpleCanvasRenderer implements Renderer {

    private CanvasSprite[] mSprites;
    
    public void setSprites(CanvasSprite[] sprites) {
        mSprites = sprites;
    }
    
    public void drawFrame(Canvas canvas) {
        if (mSprites != null) {

            for (int x = 0; x < mSprites.length; x++) {
                mSprites[x].draw(canvas);
            }
        }
        
    }

    public void sizeChanged(int width, int height) {
        
    }

}
