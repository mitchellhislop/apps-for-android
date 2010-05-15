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
 * A 2d shape has left, right, top and bottom dimensions.
 *
 */
public abstract class Shape2d {

    public abstract float getLeft();
    public abstract float getRight();
    public abstract float getTop();
    public abstract float getBottom();

    /**
     * @param other Another 2d shape
     * @return Whether this shape is intersecting with the other.
     */
    public boolean isIntersecting(Shape2d other) {
        return getLeft() <= other.getRight() && getRight() >= other.getLeft()
                && getTop() <= other.getBottom() && getBottom() >= other.getTop();
    }

    /**
     * @param x An x coordinate
     * @param y A y coordinate
     * @return Whether the point is within this shape
     */
    public boolean isPointWithin(float x, float y) {
        return (x > getLeft() && x < getRight()
                && y > getTop() && y < getBottom());

    }

    public float getArea() {
        return getHeight() * getWidth();
    }

    public float getHeight() {
        return getBottom() - getTop();
    }

    public float getWidth () {
        return getRight() - getLeft();
    }
}
