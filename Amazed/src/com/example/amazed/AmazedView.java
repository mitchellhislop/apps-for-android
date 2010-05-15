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

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * Custom view used to draw the maze and marble. Responds to accelerometer
 * updates to roll the marble around the screen.
 */
public class AmazedView extends View {
    // Game objects
    private Marble mMarble;
    private Maze mMaze;
    private Activity mActivity;

    // canvas we paint to.
    private Canvas mCanvas;

    private Paint mPaint;
    private Typeface mFont = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private int mTextPadding = 10;
    private int mHudTextY = 440;

    // game states
    private final static int NULL_STATE = -1;
    private final static int GAME_INIT = 0;
    private final static int GAME_RUNNING = 1;
    private final static int GAME_OVER = 2;
    private final static int GAME_COMPLETE = 3;
    private final static int GAME_LANDSCAPE = 4;
    // current state of the game
    private static int mCurState = NULL_STATE;

    // game strings
    private final static int TXT_LIVES = 0;
    private final static int TXT_LEVEL = 1;
    private final static int TXT_TIME = 2;
    private final static int TXT_TAP_SCREEN = 3;
    private final static int TXT_GAME_COMPLETE = 4;
    private final static int TXT_GAME_OVER = 5;
    private final static int TXT_TOTAL_TIME = 6;
    private final static int TXT_GAME_OVER_MSG_A = 7;
    private final static int TXT_GAME_OVER_MSG_B = 8;
    private final static int TXT_RESTART = 9;
    private final static int TXT_LANDSCAPE_MODE = 10;
    private static String mStrings[];

    // this prevents the user from dying instantly when they start a level if
    // the device is tilted.
    private boolean mWarning = false;

    // screen dimensions
    private int mCanvasWidth = 0;
    private int mCanvasHeight = 0;
    private int mCanvasHalfWidth = 0;
    private int mCanvasHalfHeight = 0;

    // are we running in portrait mode.
    private boolean mPortrait;

    // current level
    private int mlevel = 1;

    // timing used for scoring.
    private long mTotalTime = 0;
    private long mStartTime = 0;
    private long mEndTime = 0;

    // sensor manager used to control the accelerometer sensor.
    private SensorManager mSensorManager;
    // accelerometer sensor values.
    private float mAccelX = 0;
    private float mAccelY = 0;
    private float mAccelZ = 0; // this is never used but just in-case future
    // versions make use of it.

    // accelerometer buffer, currently set to 0 so even the slightest movement
    // will roll the marble.
    private float mSensorBuffer = 0;

    // http://code.google.com/android/reference/android/hardware/SensorManager.html#SENSOR_ACCELEROMETER
    // for an explanation on the values reported by SENSOR_ACCELEROMETER.
    private final SensorListener mSensorAccelerometer = new SensorListener() {

        // method called whenever new sensor values are reported.
        public void onSensorChanged(int sensor, float[] values) {
            // grab the values required to respond to user movement.
            mAccelX = values[0];
            mAccelY = values[1];
            mAccelZ = values[2];
        }

        // reports when the accuracy of sensor has change
        // SENSOR_STATUS_ACCURACY_HIGH = 3
        // SENSOR_STATUS_ACCURACY_LOW = 1
        // SENSOR_STATUS_ACCURACY_MEDIUM = 2
        // SENSOR_STATUS_UNRELIABLE = 0 //calibration required.
        public void onAccuracyChanged(int sensor, int accuracy) {
            // currently not used
        }
    };

    /**
     * Custom view constructor.
     * 
     * @param context
     *            Application context
     * @param activity
     *            Activity controlling the view
     */
    public AmazedView(Context context, Activity activity) {
        super(context);

        mActivity = activity;

        // init paint and make is look "nice" with anti-aliasing.
        mPaint = new Paint();
        mPaint.setTextSize(14);
        mPaint.setTypeface(mFont);
        mPaint.setAntiAlias(true);

        // setup accelerometer sensor manager.
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        // register our accelerometer so we can receive values.
        // SENSOR_DELAY_GAME is the recommended rate for games
        mSensorManager.registerListener(mSensorAccelerometer, SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_GAME);

        // setup our maze and marble.
        mMaze = new Maze(mActivity);
        mMarble = new Marble(this);

        // load array from /res/values/strings.xml
        mStrings = getResources().getStringArray(R.array.gameStrings);

        // set the starting state of the game.
        switchGameState(GAME_INIT);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // get new screen dimensions.
        mCanvasWidth = w;
        mCanvasHeight = h;

        mCanvasHalfWidth = w / 2;
        mCanvasHalfHeight = h / 2;

        // are we in portrait or landscape mode now?
        // you could use bPortrait = !bPortrait however in the future who know's
        // how many different ways a device screen may be rotated.
        if (mCanvasHeight > mCanvasWidth)
            mPortrait = true;
        else {
            mPortrait = false;
            switchGameState(GAME_LANDSCAPE);
        }
    }

    /**
     * Called every cycle, used to process current game state.
     */
    public void gameTick() {
        // very basic state machine, makes a good foundation for a more complex
        // game.
        switch (mCurState) {
        case GAME_INIT:
            // prepare a new game for the user.
            initNewGame();
            switchGameState(GAME_RUNNING);

        case GAME_RUNNING:
            // update our marble.
            if (!mWarning)
                updateMarble();
            break;
        }

        // redraw the screen once our tick function is complete.
        invalidate();
    }

    /**
     * Reset game variables in preparation for a new game.
     */
    public void initNewGame() {
        mMarble.setLives(5);
        mTotalTime = 0;
        mlevel = 0;
        initLevel();
    }

    /**
     * Initialize the next level.
     */
    public void initLevel() {
        if (mlevel < mMaze.MAX_LEVELS) {
            // setup the next level.
            mWarning = true;
            mlevel++;
            mMaze.load(mActivity, mlevel);
            mMarble.init();
        } else {
            // user has finished the game, update state machine.
            switchGameState(GAME_COMPLETE);
        }
    }

    /**
     * Called from gameTick(), update marble x,y based on latest values obtained
     * from the Accelerometer sensor. AccelX and accelY are values received from
     * the accelerometer, higher values represent the device tilted at a more
     * acute angle.
     */
    public void updateMarble() {
        // we CAN give ourselves a buffer to stop the marble from rolling even
        // though we think the device is "flat".
        if (mAccelX > mSensorBuffer || mAccelX < -mSensorBuffer)
            mMarble.updateX(mAccelX);
        if (mAccelY > mSensorBuffer || mAccelY < -mSensorBuffer)
            mMarble.updateY(mAccelY);

        // check which cell the marble is currently occupying.
        if (mMaze.getCellType(mMarble.getX(), mMarble.getY()) == mMaze.VOID_TILE) {
            // user entered the "void".
            if (mMarble.getLives() > 0) {
                // user still has some lives remaining, restart the level.
                mMarble.death();
                mMarble.init();
                mWarning = true;
            } else {
                // user has no more lives left, end of game.
                mEndTime = System.currentTimeMillis();
                mTotalTime += mEndTime - mStartTime;
                switchGameState(GAME_OVER);
            }

        } else if (mMaze.getCellType(mMarble.getX(), mMarble.getY()) == mMaze.EXIT_TILE) {
            // user has reached the exit tiles, prepare the next level.
            mEndTime = System.currentTimeMillis();
            mTotalTime += mEndTime - mStartTime;
            initLevel();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // we only want to handle down events .
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mCurState == GAME_OVER || mCurState == GAME_COMPLETE) {
                // re-start the game.
                mCurState = GAME_INIT;
            } else if (mCurState == GAME_RUNNING) {
                // in-game, remove the pop-up text so user can play.
                mWarning = false;
                mStartTime = System.currentTimeMillis();
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // quit application if user presses the back key.
        if (keyCode == KeyEvent.KEYCODE_BACK)
            cleanUp();

        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        // update our canvas reference.
        mCanvas = canvas;

        // clear the screen.
        mPaint.setColor(Color.WHITE);
        mCanvas.drawRect(0, 0, mCanvasWidth, mCanvasHeight, mPaint);

        // simple state machine, draw screen depending on the current state.
        switch (mCurState) {
        case GAME_RUNNING:
            // draw our maze first since everything else appears "on top" of it.
            mMaze.draw(mCanvas, mPaint);

            // draw our marble and hud.
            mMarble.draw(mCanvas, mPaint);

            // draw hud
            drawHUD();
            break;

        case GAME_OVER:
            drawGameOver();
            break;

        case GAME_COMPLETE:
            drawGameComplete();
            break;

        case GAME_LANDSCAPE:
            drawLandscapeMode();
            break;
        }

        gameTick();
    }

    /**
     * Called from onDraw(), draws the in-game HUD
     */
    public void drawHUD() {
        mPaint.setColor(Color.BLACK);
        mPaint.setTextAlign(Paint.Align.LEFT);
        mCanvas.drawText(mStrings[TXT_TIME] + ": " + (mTotalTime / 1000), mTextPadding, mHudTextY,
                mPaint);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mCanvas.drawText(mStrings[TXT_LEVEL] + ": " + mlevel, mCanvasHalfWidth, mHudTextY, mPaint);
        mPaint.setTextAlign(Paint.Align.RIGHT);
        mCanvas.drawText(mStrings[TXT_LIVES] + ": " + mMarble.getLives(), mCanvasWidth - mTextPadding,
                mHudTextY, mPaint);

        // do we need to display the warning message to save the user from
        // possibly dying instantly.
        if (mWarning) {
            mPaint.setColor(Color.BLUE);
            mCanvas
                    .drawRect(0, mCanvasHalfHeight - 15, mCanvasWidth, mCanvasHalfHeight + 5,
                            mPaint);
            mPaint.setColor(Color.WHITE);
            mPaint.setTextAlign(Paint.Align.CENTER);
            mCanvas.drawText(mStrings[TXT_TAP_SCREEN], mCanvasHalfWidth, mCanvasHalfHeight, mPaint);
        }
    }

    /**
     * Called from onDraw(), draws the game over screen.
     */
    public void drawGameOver() {
        mPaint.setColor(Color.BLACK);
        mPaint.setTextAlign(Paint.Align.CENTER);

        mCanvas.drawText(mStrings[TXT_GAME_OVER], mCanvasHalfWidth, mCanvasHalfHeight, mPaint);
        mCanvas.drawText(mStrings[TXT_TOTAL_TIME] + ": " + (mTotalTime / 1000) + "s",
                mCanvasHalfWidth, mCanvasHalfHeight + mPaint.getFontSpacing(), mPaint);
        mCanvas.drawText(mStrings[TXT_GAME_OVER_MSG_A] + " " + (mlevel - 1) + " "
                + mStrings[TXT_GAME_OVER_MSG_B], mCanvasHalfWidth, mCanvasHalfHeight
                + (mPaint.getFontSpacing() * 2), mPaint);
        mCanvas.drawText(mStrings[TXT_RESTART], mCanvasHalfWidth, mCanvasHeight
                - (mPaint.getFontSpacing() * 3), mPaint);
    }

    /**
     * Called from onDraw(), draws the game complete screen.
     */
    public void drawGameComplete() {
        mPaint.setColor(Color.BLACK);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mCanvas.drawText(mStrings[GAME_COMPLETE], mCanvasHalfWidth, mCanvasHalfHeight, mPaint);
        mCanvas.drawText(mStrings[TXT_TOTAL_TIME] + ": " + (mTotalTime / 1000) + "s",
                mCanvasHalfWidth, mCanvasHalfHeight + mPaint.getFontSpacing(), mPaint);
        mCanvas.drawText(mStrings[TXT_RESTART], mCanvasHalfWidth, mCanvasHeight
                - (mPaint.getFontSpacing() * 3), mPaint);
    }

    /**
     * Called from onDraw(), displays a message asking the user to return the
     * device back to portrait mode.
     */
    public void drawLandscapeMode() {
        mPaint.setColor(Color.WHITE);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mCanvas.drawRect(0, 0, mCanvasWidth, mCanvasHeight, mPaint);
        mPaint.setColor(Color.BLACK);
        mCanvas.drawText(mStrings[TXT_LANDSCAPE_MODE], mCanvasHalfWidth, mCanvasHalfHeight, mPaint);
    }

    /**
     * Updates the current game state with a new state. At the moment this is
     * very basic however if the game was to get more complicated the code
     * required for changing game states could grow quickly.
     * 
     * @param newState
     *            New game state
     */
    public void switchGameState(int newState) {
        mCurState = newState;
    }

    /**
     * Register the accelerometer sensor so we can use it in-game.
     */
    public void registerListener() {
        mSensorManager.registerListener(mSensorAccelerometer, SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Unregister the accelerometer sensor otherwise it will continue to operate
     * and report values.
     */
    public void unregisterListener() {
        mSensorManager.unregisterListener(mSensorAccelerometer);
    }

    /**
     * Clean up the custom view and exit the application.
     */
    public void cleanUp() {
        mMarble = null;
        mMaze = null;
        mStrings = null;
        unregisterListener();
        mActivity.finish();
    }
}