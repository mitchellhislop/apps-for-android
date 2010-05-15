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

/**
 * Keeps the state for the line that extends in two directions until it hits its boundaries.  This is triggered
 * by the user gesture in a horizontal or vertical direction.
 */
public class AnimatingLine extends Shape2d {

    private Direction mDirection;

    // for vertical lines, the y offset
    // for horizontal, the x offset
    float mPerpAxisOffset;

    float mStart;
    float mEnd;

    float mMin;
    float mMax;

    private long mLastUpdate = 0;
    private float mPixelsPerSecond = 101.0f;

    /**
     * @param direction The direction of the line
     * @param now What 'now' is
     * @param axisStart Where on the perpindicular axis the line is extending from
     * @param start The point the line is extending from on the parallel axis
     * @param min The lower bound for the line (i.e the left most point)
     * @param max The upper bound for the line (i.e the right most point)
     */
    public AnimatingLine(
            Direction direction,
            long now,
            float axisStart,
            float start,
            float min, float max) {
        mDirection = direction;
        mLastUpdate = now;
        mPerpAxisOffset = axisStart;
        mStart = mEnd = start;
        mMin = min;
        mMax = max;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public float getPerpAxisOffset() {
        return mPerpAxisOffset;
    }

    public float getStart() {
        return mStart;
    }

    public float getEnd() {
        return mEnd;
    }

    public float getMin() {
        return mMin;
    }

    public float getMax() {
        return mMax;
    }

    public float getLeft() {
        return mDirection == Direction.Horizontal ? mStart : mPerpAxisOffset;
    }

    public float getRight() {
        return mDirection == Direction.Horizontal ? mEnd : mPerpAxisOffset;
    }

    public float getTop() {
        return mDirection == Direction.Vertical ? mStart : mPerpAxisOffset;
    }

    public float getBottom() {
        return mDirection == Direction.Vertical ? mEnd : mPerpAxisOffset;
    }

    public float getPercentageDone() {
        return (mEnd - mStart) / (mMax - mMin);
    }

    /**
     * Extend the line according to the animation.
     * @return whether the line has reached its end.
     */
    public boolean update(long time) {
        if (time == mLastUpdate) return false;
        float delta = (time - mLastUpdate) * mPixelsPerSecond;
        delta = delta / 1000;
        mLastUpdate = time;
        mStart -= delta;
        mEnd += delta;

        if (mStart < mMin) mStart = mMin;
        if (mEnd > mMax) mEnd = mMax;

        return mStart == mMin && mEnd == mMax;
    }

    public void setNow(long now) {
        mLastUpdate = now;
    }
}
