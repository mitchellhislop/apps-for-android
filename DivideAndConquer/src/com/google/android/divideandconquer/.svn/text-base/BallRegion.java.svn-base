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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.ref.WeakReference;

/**
 * A ball region is a rectangular region that contains bouncing
 * balls, and possibly one animating line.  In its {@link #update(long)} method,
 * it will update all of its balls, the moving line.  It detects collisions
 * between the balls and the moving line, and when the line is complete, handles
 * splitting off a new region.
 */
public class BallRegion extends Shape2d {

    private float mLeft;
    private float mRight;
    private float mTop;
    private float mBottom;

    private List<Ball> mBalls;

    private AnimatingLine mAnimatingLine;

    private boolean mShrinkingToFit = false;
    private long mLastUpdate = 0;
    private static final float PIXELS_PER_SECOND = 25.0f;

    private static final float SHRINK_TO_FIT_AREA_THRESH = 10000.0f;
    private static final float SHRINK_TO_FIT_AREA_THRESH_ONE_BALL = 20000.0f;
    private static final float SHRINK_TO_FIT_AREA = 1000f;
    private static final float MIN_EDGE = 30f;
    private boolean mDoneShrinking = false;

    private WeakReference<BallEngine.BallEventCallBack> mCallBack;

    /*
     * @param left The minimum x component
     * @param right The maximum x component
     * @param top The minimum y component
     * @param bottom The maximum y component
     * @param balls the balls of the region
     */
    public BallRegion(long now, float left, float right, float top, float bottom,
                      ArrayList<Ball> balls) {
        mLastUpdate = now;
        mLeft = left;
        mRight = right;
        mTop = top;
        mBottom = bottom;

        mBalls = balls;
        final int numBalls = mBalls.size();
        for (int i = 0; i < numBalls; i++) {
            final Ball ball = mBalls.get(i);
            ball.setRegion(this);
        }
        checkShrinkToFit();
    }

    public void setCallBack(BallEngine.BallEventCallBack callBack) {
        this.mCallBack = new WeakReference<BallEngine.BallEventCallBack>(callBack);
    }

    private void checkShrinkToFit() {
        final float area = getArea();
        if (area < SHRINK_TO_FIT_AREA_THRESH) {
            mShrinkingToFit = true;
        } else if (area < SHRINK_TO_FIT_AREA_THRESH_ONE_BALL && mBalls.size() == 1) {
            mShrinkingToFit = true;
        }
    }

    public float getLeft() {
        return mLeft;
    }

    public float getRight() {
        return mRight;
    }

    public float getTop() {
        return mTop;
    }

    public float getBottom() {
        return mBottom;
    }

    public List<Ball> getBalls() {
        return mBalls;
    }


    public AnimatingLine getAnimatingLine() {
        return mAnimatingLine;
    }

    public boolean consumeDoneShrinking() {
        if (mDoneShrinking) {
            mDoneShrinking = false;
            return true;
        }
        return false;
    }

    public void setNow(long now) {
        mLastUpdate = now;

        // update the balls
        final int numBalls = mBalls.size();
        for (int i = 0; i < numBalls; i++) {
            final Ball ball = mBalls.get(i);
            ball.setNow(now);
        }

        if (mAnimatingLine != null) {
            mAnimatingLine.setNow(now);
        }
    }

    /**
     * Update the balls an (if it exists) the animating line in this region.
     * @param now in millis
     * @return A new region if a split has occured because the animating line
     *     finished.
     */
    public BallRegion update(long now) {

        // update the animating line
        final boolean newRegion =
                (mAnimatingLine != null && mAnimatingLine.update(now));

        final int numBalls = mBalls.size();

        // move balls, check for collision with animating line
        for (int i = 0; i < numBalls; i++) {
            final Ball ball = mBalls.get(i);
            ball.update(now);
            if (mAnimatingLine != null && ball.isIntersecting(mAnimatingLine)) {
                mAnimatingLine = null;
                if (mCallBack != null) mCallBack.get().onBallHitsLine(now, ball, mAnimatingLine);
            }
        }

        // update ball to ball collisions
        for (int i = 0; i < numBalls; i++) {
            final Ball ball = mBalls.get(i);
            for (int j = i + 1; j < numBalls; j++) {
                Ball other = mBalls.get(j);
                if (ball.isCircleOverlapping(other)) {
                    Ball.adjustForCollision(ball, other);
                    break;
                }
            }
        }        

        handleShrinkToFit(now);

        // no collsion, new region means we need to split out the apropriate
        // balls into a new region
        if (newRegion && mAnimatingLine != null) {
            BallRegion otherRegion = splitRegion(
                    now,
                    mAnimatingLine.getDirection(),
                    mAnimatingLine.getPerpAxisOffset());
            mAnimatingLine = null;
            return otherRegion;
        } else {
            return null;
        }
    }

    private void handleShrinkToFit(long now) {
        // update shrinking to fit
        if (mShrinkingToFit && mAnimatingLine == null) {
            if (now == mLastUpdate) return;
            float delta = (now - mLastUpdate) * PIXELS_PER_SECOND;
            delta = delta / 1000;

            if (getHeight()  > MIN_EDGE) {
                mTop += delta;
                mBottom -= delta;
            }
            if (getWidth() > MIN_EDGE) {
                mLeft += delta;
                mRight -= delta;                
            }

            final int numBalls = mBalls.size();
            for (int i = 0; i < numBalls; i++) {
                final Ball ball = mBalls.get(i);
                ball.setRegion(this);
            }
            if (getArea() <= SHRINK_TO_FIT_AREA) {
                mShrinkingToFit = false;
                mDoneShrinking = true;
            }
        }
        mLastUpdate = now;
    }

    /**
     * Return whether this region can start a line at a certain point.
     */
    public boolean canStartLineAt(float x, float y) {
        return !mShrinkingToFit && mAnimatingLine == null && isPointWithin(x, y);
    }


    /**
     * Start a horizontal line at a point.
     * @param now What 'now' is.
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void startHorizontalLine(long now, float x, float y) {
        if (!canStartLineAt(x, y)) {
            throw new IllegalArgumentException(
                    "can't start line with point (" + x + "," + y + ")");
        }
        mAnimatingLine =
                new AnimatingLine(Direction.Horizontal, now, y, x, mLeft, mRight);
    }

    /**
     * Start a vertical line at a point.
     * @param now What 'now' is.
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void startVerticalLine(long now, float x, float y) {
        if (!canStartLineAt(x, y)) {
            throw new IllegalArgumentException(
                    "can't start line with point (" + x + "," + y + ")");
        }
        mAnimatingLine =
                new AnimatingLine(Direction.Vertical, now, x, y, mTop, mBottom);
    }

    /**
     * Splits this region at a certain offset, shrinking this one down and returning
     * the other region that makes up the rest.
     * @param direction The direction of the line.
     * @param perpAxisOffset The offset of the perpendicular axis of the line.
     * @return A new region containing a portion of the balls.
     */
    private BallRegion splitRegion(long now, Direction direction, float perpAxisOffset) {

        ArrayList<Ball> splitBalls = new ArrayList<Ball>();

        if (direction == Direction.Horizontal) {
            Iterator<Ball> it = mBalls.iterator();
            while (it.hasNext()) {
                Ball ball = it.next();
                if (ball.getY() > perpAxisOffset) {
                    it.remove();
                    splitBalls.add(ball);
                }
            }
            float oldBottom = mBottom;
            mBottom = perpAxisOffset;
            checkShrinkToFit();
            final BallRegion region = new BallRegion(now, mLeft, mRight, perpAxisOffset,
                    oldBottom, splitBalls);
            region.setCallBack(mCallBack.get());
            return region;
        } else  {
            assert(direction == Direction.Vertical);
            Iterator<Ball> it = mBalls.iterator();
            while (it.hasNext()) {
                Ball ball = it.next();
                if (ball.getX() > perpAxisOffset) {
                    it.remove();
                    splitBalls.add(ball);
                }
            }
            float oldRight = mRight;
            mRight = perpAxisOffset;
            checkShrinkToFit();
            final BallRegion region = new BallRegion(now, perpAxisOffset, oldRight, mTop,
                    mBottom, splitBalls);
            region.setCallBack(mCallBack.get());
            return region;
        }
    }

}
