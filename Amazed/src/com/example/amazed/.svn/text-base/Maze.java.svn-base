/*
 * Copyright (C) 2008 Jason Tomlinson.
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

package com.example.amazed;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

/**
 * Maze drawn on screen, each new level is loaded once the previous level has
 * been completed.
 */
public class Maze {
	
    // maze tile size and dimension
    private final static int TILE_SIZE = 16;
    private final static int MAZE_COLS = 20;
    private final static int MAZE_ROWS = 26;

    // tile types
    public final static int PATH_TILE = 0;
    public final static int VOID_TILE = 1;
    public final static int EXIT_TILE = 2;

    // tile colors
    private final static int VOID_COLOR = Color.BLACK;

    // maze level data
    private static int[] mMazeData;

    // number of level
    public final static int MAX_LEVELS = 10;

    // current tile attributes
    private Rect mRect = new Rect();
    private int mRow;
    private int mCol;
    private int mX;
    private int mY;

    // tile bitmaps
    private Bitmap mImgPath;
    private Bitmap mImgExit;

    /**
     * Maze constructor.
     * 
     * @param context
     *            Application context used to load images.
     */
    Maze(Activity activity) {

        // load bitmaps.
        mImgPath = BitmapFactory.decodeResource(activity.getApplicationContext().getResources(),
                R.drawable.path);
        mImgExit = BitmapFactory.decodeResource(activity.getApplicationContext().getResources(),
                R.drawable.exit);
    }

    /**
     * Load specified maze level.
     * 
     * @param activity
     *           Activity controlled the maze, we use this load the level data
     * @param newLevel
     *            Maze level to be loaded.
     */
    void load(Activity activity, int newLevel) {
        // maze data is stored in the assets folder as level1.txt, level2.txt
        // etc....
        String mLevel = "level" + newLevel + ".txt";

        InputStream is = null;

        try {
            // construct our maze data array.
            mMazeData = new int[MAZE_ROWS * MAZE_COLS];
            // attempt to load maze data.
            is = activity.getAssets().open(mLevel);

            // we need to loop through the input stream and load each tile for
            // the current maze.
            for (int i = 0; i < mMazeData.length; i++) {
                // data is stored in unicode so we need to convert it.
                mMazeData[i] = Character.getNumericValue(is.read());

                // skip the "," and white space in our human readable file.
                is.read();
                is.read();
            }
        } catch (Exception e) {
            Log.i("Maze", "load exception: " + e);
        } finally {
            closeStream(is);
        }

    }

    /**
     * Draw the maze.
     * 
     * @param canvas
     *            Canvas object to draw too.
     * @param paint
     *            Paint object used to draw with.
     */
    public void draw(Canvas canvas, Paint paint) {
        // loop through our maze and draw each tile individually.
        for (int i = 0; i < mMazeData.length; i++) {
            // calculate the row and column of the current tile.
            mRow = i / MAZE_COLS;
            mCol = i % MAZE_COLS;

            // convert the row and column into actual x,y co-ordinates so we can
            // draw it on screen.
            mX = mCol * TILE_SIZE;
            mY = mRow * TILE_SIZE;

            // draw the actual tile based on type.
            if (mMazeData[i] == PATH_TILE)
                canvas.drawBitmap(mImgPath, mX, mY, paint);
            else if (mMazeData[i] == EXIT_TILE)
                canvas.drawBitmap(mImgExit, mX, mY, paint);
            else if (mMazeData[i] == VOID_TILE) {
                // since our "void" tile is purely black lets draw a rectangle
                // instead of using an image.

                // tile attributes we are going to paint.
                mRect.left = mX;
                mRect.top = mY;
                mRect.right = mX + TILE_SIZE;
                mRect.bottom = mY + TILE_SIZE;

                paint.setColor(VOID_COLOR);
                canvas.drawRect(mRect, paint);
            }
        }

    }

    /**
     * Determine which cell the marble currently occupies.
     * 
     * @param x
     *            Current x co-ordinate.
     * @param y
     *            Current y co-ordinate.
     * @return The actual cell occupied by the marble.
     */
    public int getCellType(int x, int y) {
        // convert the x,y co-ordinate into row and col values.
        int mCellCol = x / TILE_SIZE;
        int mCellRow = y / TILE_SIZE;

        // location is the row,col coordinate converted so we know where in the
        // maze array to look.
        int mLocation = 0;

        // if we are beyond the 1st row need to multiple by the number of
        // columns.
        if (mCellRow > 0)
            mLocation = mCellRow * MAZE_COLS;

        // add the column location.
        mLocation += mCellCol;

        return mMazeData[mLocation];
    }

    /**
     * Closes the specified stream.
     * 
     * @param stream
     *            The stream to close.
     */
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}