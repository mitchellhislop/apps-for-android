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
package com.google.android.divideandconquer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Debug;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.ArrayList;

/**
 * Handles the visual display and touch input for the game.
 */
public class DivideAndConquerView extends View implements BallEngine.BallEventCallBack {

    static final int BORDER_WIDTH = 10;

    // this needs to match size of ball drawable
    static final float BALL_RADIUS = 5f;

    static final float BALL_SPEED = 80f;

    // if true, will profile the drawing code during each animating line and export
    // the result to a file named 'BallsDrawing.trace' on the sd card
    // this file can be pulled off and profiled with traceview
    // $ adb pull /sdcard/BallsDrawing.trace .
    // traceview BallsDrawing.trace
    private static final boolean PROFILE_DRAWING = false;

    private boolean mDrawingProfilingStarted = false;

    private final Paint mPaint;
    private BallEngine mEngine;

    private Mode mMode = Mode.Paused;

    private BallEngineCallBack mCallback;

    // interface for starting a line
    private DirectionPoint mDirectionPoint = null;
    private Bitmap mBallBitmap;
    private float mBallBitmapRadius;
    private final Bitmap mExplosion1;
    private final Bitmap mExplosion2;
    private final Bitmap mExplosion3;

    /**
     * Callback notifying of events related to the ball engine.
     */
    static interface BallEngineCallBack {

        /**
         * The engine has its dimensions and is ready to go.
         * @param ballEngine The ball engine.
         */
        void onEngineReady(BallEngine ballEngine);

        /**
         * A ball has hit a moving line.
         * @param ballEngine The engine.
         * @param x The x coordinate of the ball.
         * @param y The y coordinate of the ball.
         */
        void onBallHitsMovingLine(BallEngine ballEngine, float x, float y);

        /**
         * A line made it to the edges of its region, splitting off a new region.
         * @param ballEngine The engine.
         */
        void onAreaChange(BallEngine ballEngine);
    }

    /**
     * @return The ball engine associated with the game.
     */
    public BallEngine getEngine() {
        return mEngine;
    }

    /**
     * Keeps track of the mode of this view.
     */
    enum Mode {

        /**
         * The balls are bouncing around.
         */
        Bouncing,

        /**
         * The animation has stopped and the balls won't move around.  The user
         * may not unpause it; this is used to temporarily stop games between
         * levels, or when the game is over and the activity places a dialog up.
         */
        Paused,

        /**
         * Same as {@link #Paused}, but paints the word 'touch to unpause' on
         * the screen, so the user knows he/she can unpause the game.
         */
        PausedByUser
    }

    public DivideAndConquerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(2);
        mPaint.setColor(Color.BLACK);

        // so we can see the back key
        setFocusableInTouchMode(true);

        drawBackgroundGradient();

        mBallBitmap = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.ball);

        mBallBitmapRadius = ((float) mBallBitmap.getWidth()) / 2f;

        mExplosion1 = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.explosion1);
        mExplosion2 = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.explosion2);
        mExplosion3 = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.explosion3);
    }

    final GradientDrawable mBackgroundGradient =
            new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.RED, Color.YELLOW});

    void drawBackgroundGradient() {
        setBackgroundDrawable(mBackgroundGradient);
    }

    /**
     * Set the callback that will be notified of events related to the ball
     * engine.
     * @param callback The callback.
     */
    public void setCallback(BallEngineCallBack callback) {
        mCallback = callback;
    }

    @Override
    protected void onSizeChanged(int i, int i1, int i2, int i3) {
        super.onSizeChanged(i, i1, i2,
                i3);

        // this should only happen once when the activity is first launched.
        // we could be smarter about saving / restoring across activity
        // lifecycles, but for now, this is good enough to handle in game play,
        // and most cases of navigating away with the home key and coming back.
        mEngine = new BallEngine(
                BORDER_WIDTH, getWidth() - BORDER_WIDTH,
                BORDER_WIDTH, getHeight() - BORDER_WIDTH,
                BALL_SPEED,
                BALL_RADIUS);
        mEngine.setCallBack(this);
        mCallback.onEngineReady(mEngine);
    }


    /**
     * @return the current mode of operation.
     */
    public Mode getMode() {
        return mMode;
    }

    /**
     * Set the mode of operation.
     * @param mode The mode.
     */
    public void setMode(Mode mode) {
        mMode = mode;

        if (mMode == Mode.Bouncing && mEngine != null) {
            // when starting up again, the engine needs to know what 'now' is.
            final long now = SystemClock.elapsedRealtime();
            mEngine.setNow(now);

            mExplosions.clear();
            invalidate();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // the first time the user hits back while the balls are moving,
        // we'll pause the game.  but if they hit back again, we'll do the usual
        // (exit the activity)
        if (keyCode == KeyEvent.KEYCODE_BACK && mMode == Mode.Bouncing) {
            setMode(Mode.PausedByUser);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        if (mMode == Mode.PausedByUser) {
            // touching unpauses when the game was paused by the user.
            setMode(Mode.Bouncing);
            return true;
        } else if (mMode == Mode.Paused) {
            return false;
        }

        final float x = motionEvent.getX();
        final float y = motionEvent.getY();
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mEngine.canStartLineAt(x, y)) {
                    mDirectionPoint =
                            new DirectionPoint(x, y);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mDirectionPoint != null) {
                    mDirectionPoint.updateEndPoint(x, y);
                } else if (mEngine.canStartLineAt(x, y)) {
                    mDirectionPoint =
                        new DirectionPoint(x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (mDirectionPoint != null) {
                    switch (mDirectionPoint.getDirection()) {
                        case Unknown:
                            // do nothing
                            break;
                        case Horizonal:
                            mEngine.startHorizontalLine(SystemClock.elapsedRealtime(),
                                    mDirectionPoint.getX(), mDirectionPoint.getY());
                            if (PROFILE_DRAWING) {
                                if (!mDrawingProfilingStarted) {
                                    Debug.startMethodTracing("BallsDrawing");
                                    mDrawingProfilingStarted = true;
                                }
                            }
                            break;
                        case Vertical:
                            mEngine.startVerticalLine(SystemClock.elapsedRealtime(),
                                    mDirectionPoint.getX(), mDirectionPoint.getY());
                            if (PROFILE_DRAWING) {
                                if (!mDrawingProfilingStarted) {
                                    Debug.startMethodTracing("BallsDrawing");
                                    mDrawingProfilingStarted = true;
                                }
                            }
                            break;
                    }
                }
                mDirectionPoint = null;
                return true;
            case MotionEvent.ACTION_CANCEL:
                mDirectionPoint = null;
                return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    public void onBallHitsBall(Ball ballA, Ball ballB) {

    }

    /** {@inheritDoc} */
    public void onBallHitsLine(long when, Ball ball, AnimatingLine animatingLine) {
        mCallback.onBallHitsMovingLine(mEngine, ball.getX(), ball.getY());

        mExplosions.add(
                new Explosion(
                        when,
                        ball.getX(), ball.getY(),
                        mExplosion1, mExplosion2, mExplosion3));

    }

    static class Explosion {
        private long mLastUpdate;
        private long mProgress = 0;
        private final float mX;
        private final float mY;

        private final Bitmap mExplosion1;
        private final Bitmap mExplosion2;
        private final Bitmap mExplosion3;
        private final float mRadius;

        Explosion(long mLastUpdate, float mX, float mY,
                  Bitmap explosion1, Bitmap explosion2, Bitmap explosion3) {
            this.mLastUpdate = mLastUpdate;
            this.mX = mX;
            this.mY = mY;
            this.mExplosion1 = explosion1;
            this.mExplosion2 = explosion2;
            this.mExplosion3 = explosion3;
            mRadius = ((float) mExplosion1.getWidth()) / 2f;

        }

        public void update(long now) {
            mProgress += (now - mLastUpdate);
            mLastUpdate = now;
        }

        public void setNow(long now) {
            mLastUpdate = now;
        }

        public void draw(Canvas canvas, Paint paint) {
            if (mProgress < 80L) {
                canvas.drawBitmap(mExplosion1, mX - mRadius, mY - mRadius, paint);
            } else if (mProgress < 160L) {
                canvas.drawBitmap(mExplosion2, mX - mRadius, mY - mRadius, paint);
            } else if (mProgress < 400L) {
                canvas.drawBitmap(mExplosion3, mX - mRadius, mY - mRadius, paint);
            }
        }

        public boolean done() {
            return mProgress > 700L;
        }
    }

    private ArrayList<Explosion> mExplosions = new ArrayList<Explosion>();


    @Override
    protected void onDraw(Canvas canvas) {
        boolean newRegion = false;

        if (mMode == Mode.Bouncing) {

            // handle the ball engine
            final long now = SystemClock.elapsedRealtime();
            newRegion = mEngine.update(now);

            if (newRegion) {
                mCallback.onAreaChange(mEngine);

                // reset back to full alpha bg color
                drawBackgroundGradient();
            }

            if (PROFILE_DRAWING) {
                if (newRegion && mDrawingProfilingStarted) {
                    mDrawingProfilingStarted = false;
                    Debug.stopMethodTracing();
                }
            }

            // the X-plosions
            for (int i = 0; i < mExplosions.size(); i++) {
                final Explosion explosion = mExplosions.get(i);
                explosion.update(now);
            }


        }

        for (int i = 0; i < mEngine.getRegions().size(); i++) {
            BallRegion region = mEngine.getRegions().get(i);
            drawRegion(canvas, region);
        }

        for (int i = 0; i < mExplosions.size(); i++) {
            final Explosion explosion = mExplosions.get(i);
            explosion.draw(canvas, mPaint);
            // TODO prune explosions that are done
        }


        if (mMode == Mode.PausedByUser) {
            drawPausedText(canvas);
        } else if (mMode == Mode.Bouncing) {
            // keep em' bouncing!
            invalidate();
        }
    }

    /**
     * Pain the text instructing the user how to unpause the game.
     */
    private void drawPausedText(Canvas canvas) {
        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(
                    TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP,
                            20,
                            getResources().getDisplayMetrics()));
        final String unpauseInstructions = getContext().getString(R.string.unpause_instructions);
        canvas.drawText(unpauseInstructions, getWidth() / 5, getHeight() / 2, mPaint);
        mPaint.setAntiAlias(false);
    }

    private RectF mRectF = new RectF();

    /**
     * Draw a ball region.
     */
    private void drawRegion(Canvas canvas, BallRegion region) {

        // draw fill rect to offset against background
        mPaint.setColor(Color.LTGRAY);

        mRectF.set(region.getLeft(), region.getTop(),
                region.getRight(), region.getBottom());
        canvas.drawRect(mRectF, mPaint);


        //draw an outline
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawRect(mRectF, mPaint);
        mPaint.setStyle(Paint.Style.FILL);  // restore style

        // draw each ball
        for (Ball ball : region.getBalls()) {
//            canvas.drawCircle(ball.getX(), ball.getY(), BALL_RADIUS, mPaint);
            canvas.drawBitmap(
                    mBallBitmap,
                    ball.getX() - mBallBitmapRadius,
                    ball.getY() - mBallBitmapRadius,
                    mPaint);
        }

        // draw the animating line
        final AnimatingLine al = region.getAnimatingLine();
        if (al != null) {
            drawAnimatingLine(canvas, al);
        }
    }

    private static int scaleToBlack(int component, float percentage) {
//        return (int) ((1f - percentage*0.4f) * component);

        return (int) (percentage * 0.6f * (0xFF - component) + component);
    }

    /**
     * Draw an animating line.
     */
    private void drawAnimatingLine(Canvas canvas, AnimatingLine al) {

        final float perc = al.getPercentageDone();
        final int color = Color.RED;
        mPaint.setColor(Color.argb(
                0xFF,
                scaleToBlack(Color.red(color), perc),
                scaleToBlack(Color.green(color), perc),
                scaleToBlack(Color.blue(color), perc)
        ));
        switch (al.getDirection()) {
            case Horizontal:
                canvas.drawLine(
                        al.getStart(), al.getPerpAxisOffset(),
                        al.getEnd(), al.getPerpAxisOffset(),
                        mPaint);
                break;
            case Vertical:
                canvas.drawLine(
                        al.getPerpAxisOffset(), al.getStart(),
                        al.getPerpAxisOffset(), al.getEnd(),
                        mPaint);
                break;
        }
    }
}
