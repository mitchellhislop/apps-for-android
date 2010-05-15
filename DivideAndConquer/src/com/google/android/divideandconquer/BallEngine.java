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

import android.util.Log;
import android.content.Context;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Keeps track of the current state of balls bouncing around within a a set of
 * regions.
 *
 * Note: 'now' is the elapsed time in milliseconds since some consistent point in time.
 * As long as the reference point stays consistent, the engine will be happy, though
 * typically this is {@link android.os.SystemClock#elapsedRealtime()} 
 */
public class BallEngine {

    static public interface BallEventCallBack {

        void onBallHitsBall(Ball ballA, Ball ballB);

        void onBallHitsLine(long when, Ball ball, AnimatingLine animatingLine);
    }

    private final float mMinX;
    private final float mMaxX;
    private final float mMinY;
    private final float mMaxY;

    private float mBallSpeed;
    private float mBallRadius;

    private BallEventCallBack mCallBack;

    /**
     * Holds onto new regions during a split
     */
    private List<BallRegion> mNewRegions = new ArrayList<BallRegion>(8);

    private List<BallRegion> mRegions = new ArrayList<BallRegion>(8);

    public BallEngine(float minX, float maxX,
            float minY,
            float maxY,
            float ballSpeed,
            float ballRadius) {
        mMinX = minX;
        mMaxX = maxX;
        mMinY = minY;
        mMaxY = maxY;
        mBallSpeed = ballSpeed;
        mBallRadius = ballRadius;
    }

    public void setCallBack(BallEventCallBack mCallBack) {
        this.mCallBack = mCallBack;
    }

    /**
     * Update the notion of 'now' in milliseconds.  This can be usefull
     * when unpausing for instance.
     * @param now Milliseconds since some consistent point in time.
     */
    public void setNow(long now) {
        for (int i = 0; i < mRegions.size(); i++) {
            final BallRegion region = mRegions.get(i);
            region.setNow(now);
        }
    }

    /**
     * Rest the engine back to a single region with a certain number of balls
     * that will be placed randomly and sent in random directions.
     * @param now milliseconds since some consistent point in time.
     * @param numBalls
     */
    public void reset(long now, int numBalls) {
        mRegions.clear();

        ArrayList<Ball> balls = new ArrayList<Ball>(numBalls);
        for (int i = 0; i < numBalls; i++) {
            Ball ball = new Ball.Builder()
                    .setNow(now)
                    .setPixelsPerSecond(mBallSpeed)
                    .setAngle(Math.random() * 2 * Math.PI)
                    .setX((float) Math.random() * (mMaxX - mMinX) + mMinX)
                    .setY((float) Math.random() * (mMaxY - mMinY) + mMinY)
                    .setRadiusPixels(mBallRadius)
                    .create();
            balls.add(ball);
        }
        BallRegion region = new BallRegion(now, mMinX, mMaxX, mMinY, mMaxY, balls);
        region.setCallBack(mCallBack);

        mRegions.add(region);
    }

    public List<BallRegion> getRegions() {
        return mRegions;
    }

    public float getPercentageFilled() {
        float total = 0f;
        for (int i = 0; i < mRegions.size(); i++) {
            BallRegion region = mRegions.get(i);
            total += region.getArea();
            Log.d("Balls", "total now " + total);
        }
        return 1f - (total / getArea());
    }

    /**
     * @return the area in the region in pixel*pixel
     */
    public float getArea() {
        return (mMaxX - mMinX) * (mMaxY - mMinY);
    }

    /**
     * Can any of the regions within start a line at this point?
     * @param x The x coordinate.
     * @param y The y coordinate
     * @return Whether a region can start a line.
     */
    public boolean canStartLineAt(float x, float y) {
        for (BallRegion region : mRegions) {
            if (region.canStartLineAt(x, y)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Start a horizontal line at a certain point.
     * @throws IllegalArgumentException if there is no region that can start a
     *     line at the point.
     */
    public void startHorizontalLine(long now, float x, float y) {
        for (BallRegion region : mRegions) {
            if (region.canStartLineAt(x, y)) {
                region.startHorizontalLine(now, x, y);
                return;
            }
        }
        throw new IllegalArgumentException("no region can start a new line at "
                + x + ", " + y + ".");
    }

    /**
     * Start a vertical line at a certain point.
     * @throws IllegalArgumentException if there is no region that can start a
     *     line at the point.
     */
    public void startVerticalLine(long now, float x, float y) {
        for (BallRegion region : mRegions) {
            if (region.canStartLineAt(x, y)) {
                region.startVerticalLine(now, x, y);
                return;
            }
        }
        throw new IllegalArgumentException("no region can start a new line at "
                + x + ", " + y + ".");
    }

    /**
     * @param now The latest notion of 'now'
     * @return whether any new regions were added by the update.
     */
    public boolean update(long now) {
        boolean regionChange = false;
        Iterator<BallRegion> it = mRegions.iterator();
        while (it.hasNext()) {
            final BallRegion region = it.next();
            final BallRegion newRegion = region.update(now);

            if (newRegion != null) {
                regionChange = true;
                if (!newRegion.getBalls().isEmpty()) {
                    mNewRegions.add(newRegion);
                }

                // current region may not have any balls left
                if (region.getBalls().isEmpty()) {
                    it.remove();
                }
            } else if (region.consumeDoneShrinking()) {
                regionChange = true;
            }
        }
        mRegions.addAll(mNewRegions);
        mNewRegions.clear();

        return regionChange;
    }
}
