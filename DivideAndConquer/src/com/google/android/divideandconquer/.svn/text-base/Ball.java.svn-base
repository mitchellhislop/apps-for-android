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
 * A ball has a current location, a trajectory angle, a speed in pixels per
 * second, and a last update time.  It is capable of updating itself based on
 * its trajectory and speed.
 *
 * It also knows its boundaries, and will 'bounce' off them when it reaches them.
 */
public class Ball extends Shape2d {

    private long mLastUpdate;
    private float mX;
    private float mY;
    private double mAngle;

    private final float mPixelsPerSecond;
    private final float mRadiusPixels;

    private Shape2d mRegion;


    private Ball(long now, float pixelsPerSecond, float x, float y,
            double angle, float radiusPixels) {
        mLastUpdate = now;
        mPixelsPerSecond = pixelsPerSecond;
        mX = x;
        mY = y;
        mAngle = angle;
        mRadiusPixels = radiusPixels;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public float getLeft() {
        return mX - mRadiusPixels;
    }

    public float getRight() {
        return mX + mRadiusPixels;
    }

    public float getTop() {
        return mY - mRadiusPixels;
    }

    public float getBottom() {
        return mY + mRadiusPixels;
    }

    public float getRadiusPixels() {
        return mRadiusPixels;
    }

    public double getAngle() {
        return mAngle;
    }

    /**
     * Get the region the ball is contained in.
     */
    public Shape2d getRegion() {
        return mRegion;
    }

    /**
     * Set the region that the ball is contained in.
     * @param region The region.
     */
    public void setRegion(Shape2d region) {
        if (mX < region.getLeft()) {
            mX = region.getLeft();
            bounceOffLeft();
        } else if (mX > region.getRight()) {
            mX = region.getRight();
            bounceOffRight();
        }
        if (mY < region.getTop()) {
            mY = region.getTop();
            bounceOffTop();
        } else if (mY > region.getBottom()) {
            mY = region.getBottom();
            bounceOffBottom();
        }
        mRegion = region;
    }

    public void setNow(long now) {
        mLastUpdate = now;
    }

    public boolean isCircleOverlapping(Ball otherBall) {
        final float dy = otherBall.mY - mY;
        final float dx = otherBall.mX - mX;

        final float distance = dy * dy + dx * dx;

        return (distance < ((2 * mRadiusPixels) * (2 *mRadiusPixels)))
                // avoid jittery collisions
                && !movingAwayFromEachother(this, otherBall);
    }

    private boolean movingAwayFromEachother(Ball ballA, Ball ballB) {
        double collA = Math.atan2(ballB.mY - ballA.mY, ballB.mX - ballA.mX);
        double collB = Math.atan2(ballA.mY - ballB.mY, ballA.mX - ballB.mX);

        double ax = Math.cos(ballA.mAngle - collA);
        double bx = Math.cos(ballB.mAngle - collB);

        return ax + bx < 0;        
    }

    public void update(long now) {
        if (now <= mLastUpdate) return;

        // bounce when at walls
        if (mX <= mRegion.getLeft() + mRadiusPixels) {
            // we're at left wall
            mX = mRegion.getLeft() + mRadiusPixels;
            bounceOffLeft();
        } else if (mY <= mRegion.getTop() + mRadiusPixels) {
            // at top wall
            mY = mRegion.getTop() + mRadiusPixels;
            bounceOffTop();
        } else if (mX >= mRegion.getRight() - mRadiusPixels) {
            // at right wall
            mX = mRegion.getRight() - mRadiusPixels;
            bounceOffRight();
        } else if (mY >= mRegion.getBottom() - mRadiusPixels) {
            // at bottom wall
            mY = mRegion.getBottom() - mRadiusPixels;
            bounceOffBottom();
        }

        float delta = (now - mLastUpdate) * mPixelsPerSecond;
        delta = delta / 1000f;

        mX += (delta * Math.cos(mAngle));
        mY += (delta * Math.sin(mAngle));

        mLastUpdate = now;
    }

    private void bounceOffBottom() {
        if (mAngle < 0.5*Math.PI) {
            // going right
            mAngle = -mAngle;
        } else {
            // going left
            mAngle += (Math.PI - mAngle) * 2;
        }
    }

    private void bounceOffRight() {
        if (mAngle > 1.5*Math.PI) {
            // going up
            mAngle -= (mAngle - 1.5*Math.PI) * 2;
        } else {
            // going down
            mAngle += (.5*Math.PI - mAngle) * 2;
        }
    }

    private void bounceOffTop() {
        if (mAngle < 1.5 * Math.PI) {
            // going left
            mAngle -= (mAngle - Math.PI) * 2;
        } else {
            // going right
            mAngle += (2*Math.PI - mAngle) * 2;
            mAngle -= 2*Math.PI;
        }
    }

    private void bounceOffLeft() {
        if (mAngle < Math.PI) {
            // going down
            mAngle -= ((mAngle - (Math.PI / 2)) * 2);
        } else {
            // going up
            mAngle += (((1.5 * Math.PI) - mAngle) * 2);
        }
    }


    /**
     * Given that ball a and b have collided, adjust their angles to reflect their state
     * after the collision.
     *
     * This method works based on the conservation of energy and momentum in an elastic
     * collision.  Because the balls have equal mass and speed, it ends up being that they
     * simply swap velocities along the axis of the collision, keeping the velocities tangent
     * to the collision constant.
     *
     * @param ballA The first ball in a collision
     * @param ballB The second ball in a collision
     */
    public static void adjustForCollision(Ball ballA, Ball ballB) {

        final double collA = Math.atan2(ballB.mY - ballA.mY, ballB.mX - ballA.mX);
        final double collB = Math.atan2(ballA.mY - ballB.mY, ballA.mX - ballB.mX);

        final double ax = Math.cos(ballA.mAngle - collA);
        final double ay = Math.sin(ballA.mAngle - collA);

        final double bx = Math.cos(ballB.mAngle - collB);
        final double by = Math.cos(ballB.mAngle - collB);

        final double diffA = Math.atan2(ay, -bx);
        final double diffB = Math.atan2(by, -ax);

        ballA.mAngle = collA + diffA;
        ballB.mAngle = collB + diffB;
    }


    @Override
    public String toString() {
        return String.format(
            "Ball(x=%f, y=%f, angle=%f)",
                mX, mY, Math.toDegrees(mAngle));
    }

    /**
     * A more readable way to create balls than using a 5 param
     * constructor of all numbers.
     */
    public static class Builder {
        private long mNow = -1;
        private float mX = -1;
        private float mY = -1;
        private double mAngle = -1;
        private float mRadiusPixels = -1;

        private float mPixelsPerSecond = 45f;

        public Ball create() {
            if (mNow < 0) {
                throw new IllegalStateException("must set 'now'");
            }
            if (mX < 0) {
                throw new IllegalStateException("X must be set");
            }
            if (mY < 0) {
                throw new IllegalStateException("Y must be stet");
            }
            if (mAngle < 0) {
                throw new IllegalStateException("angle must be set");
            }
            if (mAngle > 2 * Math.PI) {
                throw new IllegalStateException("angle must be less that 2Pi");
            }
            if (mRadiusPixels <= 0) {
                throw new IllegalStateException("radius must be set");
            }
            return new Ball(mNow, mPixelsPerSecond, mX, mY, mAngle, mRadiusPixels);
        }

        public Builder setNow(long now) {
            mNow = now;
            return this;
        }

        public Builder setPixelsPerSecond(float pixelsPerSecond) {
            mPixelsPerSecond = pixelsPerSecond;
            return this;
        }

        public Builder setX(float x) {
            mX = x;
            return this;
        }

        public Builder setY(float y) {
            mY = y;
            return this;
        }

        public Builder setAngle(double angle) {
            mAngle = angle;
            return this;
        }

        public Builder setRadiusPixels(float pixels) {
            mRadiusPixels = pixels;
            return this;
        }
    }
}
