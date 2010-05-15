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
 * To specify a dividing line, a user hits the screen and drags in a
 * certain direction.  Once the line has been drawn long enough and mostly
 * in a particular direction (vertical, or horizontal), we can decide we
 * know what they mean.  Otherwise, it is unknown.
 *
 * This is also nice because if the user decides they don't want to send
 * a dividing line, they can just drag their finger back to where they first
 * touched and let go, cancelling.
 */
public class DirectionPoint {
    enum AmbiguousDirection {
        Vertical,
        Horizonal,
        Unknown
    }

    private float mX;
    private float mY;

    private float endLineX;
    private float endLineY;

    public DirectionPoint(float x, float y) {
        mX = x;
        mY = y;
        endLineX = x;
        endLineY = y;
    }

    public void updateEndPoint(float x, float y) {
        endLineX = x;
        endLineY = y;
    }


    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    /**
     * We know the direction when the line is at leat 20 pixels long,
     * and the angle is no more than PI / 6 away from a definitive direction.
     */
    public AmbiguousDirection getDirection() {
        float dx = endLineX - mX;
        double distance = Math.hypot(dx, endLineY - mY);
        if (distance < 10) {
            return AmbiguousDirection.Unknown;
        }
        double angle = Math.acos(dx / distance);
        double thresh = Math.PI / 6;
        if ((angle < thresh || (angle > (Math.PI - thresh)))) {
            return AmbiguousDirection.Horizonal;
        }
        if ((angle > 2 * thresh) && angle < 4*thresh) {
            return AmbiguousDirection.Vertical;
        }
        return AmbiguousDirection.Unknown;
    }
}
