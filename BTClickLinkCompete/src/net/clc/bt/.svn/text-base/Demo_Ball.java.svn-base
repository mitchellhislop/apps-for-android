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

package net.clc.bt;

import java.util.Arrays;
import java.util.List;

/**
 * Model of a "ball" object used by the demo app.
 */

public class Demo_Ball {
    public enum BOUNCE_TYPE {
        TOPLEFT, TOP, TOPRIGHT, LEFT, RIGHT, BOTTOMLEFT, BOTTOM, BOTTOMRIGHT
    }
    
    private final float radius = 20;

    private final float pxPerM = 2; // Let 2 pixels represent 1 meter

    private final float reboundEnergyFactor = 0.6f; // Amount of energy returned

    // when rebounding

    private float xPos = 160;

    private float yPos = 240;

    private float xMax = 320;
    
    private float yMax = 410;

    private float xAcceleration = 0;

    private float yAcceleration = 0;

    private float xVelocity = 0;

    private float yVelocity = 0;

    private float reboundXPos = 0;

    private float reboundYPos = 0;

    private float reboundXVelocity = 0;

    private float reboundYVelocity = 0;

    private long lastUpdate;

    private boolean onScreen;

    public Demo_Ball(boolean visible) {
        onScreen = visible;
        lastUpdate = System.currentTimeMillis();
    }
    
    public Demo_Ball(boolean visible, int screenSizeX, int screenSizeY) {
        onScreen = visible;
        lastUpdate = System.currentTimeMillis();
        xMax = screenSizeX;
        yMax = screenSizeY;
    }

    public float getRadius() {
        return radius;
    }

    public float getXVelocity() {
        return xVelocity;
    }

    public float getX() {
        if (!onScreen) {
            return -1;
        }
        return xPos;
    }

    public float getY() {
        if (!onScreen) {
            return -1;
        }
        return yPos;
    }

    public void putOnScreen(float x, float y, float ax, float ay, float vx, float vy,
            int startingSide) {
        xPos = x;
        yPos = y;
        xVelocity = vx;
        yVelocity = vy;
        xAcceleration = ax;
        yAcceleration = ay;
        lastUpdate = System.currentTimeMillis();

        if (startingSide == Demo_Multiscreen.RIGHT) {
            xPos = xMax - radius - 2;
        } else if (startingSide == Demo_Multiscreen.LEFT) {
            xPos = radius + 2;
        } else if (startingSide == Demo_Multiscreen.UP) {
            yPos = radius + 2;
        } else if (startingSide == Demo_Multiscreen.DOWN) {
            yPos = yMax - radius - 2;
        } else if (startingSide == AirHockey.FLIPTOP) {
            yPos = radius + 2;
            xPos = xMax - x;
            if (xPos < 0){
                xPos = 0;
            }
            yVelocity = -vy;
            yAcceleration = -ay;
        }

        onScreen = true;
    }

    public void setAcceleration(float ax, float ay) {
        if (!onScreen) {
            return;
        }
        xAcceleration = ax;
        yAcceleration = ay;
    }
    
    public boolean isOnScreen(){
        return onScreen;
    }

    public int update() {
        if (!onScreen) {
            return 0;
        }
        long currentTime = System.currentTimeMillis();

        long elapsed = currentTime - lastUpdate;
        lastUpdate = currentTime;

        xVelocity += ((elapsed * xAcceleration) / 1000) * pxPerM;
        yVelocity += ((elapsed * yAcceleration) / 1000) * pxPerM;

        xPos += ((xVelocity * elapsed) / 1000) * pxPerM;
        yPos += ((yVelocity * elapsed) / 1000) * pxPerM;

        // Handle rebounding
        if (yPos - radius < 0) {
            reboundXPos = xPos;
            reboundYPos = radius;
            reboundXVelocity = xVelocity;
            reboundYVelocity = -yVelocity * reboundEnergyFactor;
            onScreen = false;
            return Demo_Multiscreen.UP;
        } else if (yPos + radius > yMax) {
            reboundXPos = xPos;
            reboundYPos = yMax - radius;
            reboundXVelocity = xVelocity;
            reboundYVelocity = -yVelocity * reboundEnergyFactor;
            onScreen = false;
            return Demo_Multiscreen.DOWN;
        }

        if (xPos - radius < 0) {
            reboundXPos = radius;
            reboundYPos = yPos;
            reboundXVelocity = -xVelocity * reboundEnergyFactor;
            reboundYVelocity = yVelocity;
            onScreen = false;
            return Demo_Multiscreen.LEFT;
        } else if (xPos + radius > xMax) {
            reboundXPos = xMax - radius;
            reboundYPos = yPos;
            reboundXVelocity = -xVelocity * reboundEnergyFactor;
            reboundYVelocity = yVelocity;
            onScreen = false;
            return Demo_Multiscreen.RIGHT;
        }
        return Demo_Multiscreen.CENTER;
    }

    public void doRebound() {
        xPos = reboundXPos;
        yPos = reboundYPos;
        xVelocity = reboundXVelocity;
        yVelocity = reboundYVelocity;
        onScreen = true;
    }

    public String getState() {
        String state = "";
        state = xPos + "|" + yPos + "|" + xAcceleration + "|" + yAcceleration + "|" + xVelocity
                + "|" + yVelocity;
        return state;
    }

    public void restoreState(String state) {
        List<String> stateInfo = Arrays.asList(state.split("\\|"));
        putOnScreen(Float.parseFloat(stateInfo.get(0)), Float.parseFloat(stateInfo.get(1)), Float
                .parseFloat(stateInfo.get(2)), Float.parseFloat(stateInfo.get(3)), Float
                .parseFloat(stateInfo.get(4)), Float.parseFloat(stateInfo.get(5)), Integer
                .parseInt(stateInfo.get(6)));
    }
    
    public void doBounce(BOUNCE_TYPE bounceType, float vX, float vY){
        switch (bounceType){
            case TOPLEFT:
                if (xVelocity > 0){
                    xVelocity = -xVelocity * reboundEnergyFactor;
                }
                if (yVelocity > 0){
                    yVelocity = -yVelocity * reboundEnergyFactor;
                }
                break;
            case TOP:
                if (yVelocity > 0){
                    yVelocity = -yVelocity * reboundEnergyFactor;
                }
                break;
            case TOPRIGHT:
                if (xVelocity < 0){
                    xVelocity = -xVelocity * reboundEnergyFactor;
                }
                if (yVelocity > 0){
                    yVelocity = -yVelocity * reboundEnergyFactor;
                }
                break;
            case LEFT:
                if (xVelocity > 0){
                    xVelocity = -xVelocity * reboundEnergyFactor;
                }
                break;
            case RIGHT:
                if (xVelocity < 0){
                    xVelocity = -xVelocity * reboundEnergyFactor;
                }
                break;
            case BOTTOMLEFT:
                if (xVelocity > 0){
                    xVelocity = -xVelocity * reboundEnergyFactor;
                }
                if (yVelocity < 0){
                    yVelocity = -yVelocity * reboundEnergyFactor;
                }
                break;
            case BOTTOM:
                if (yVelocity < 0){
                    yVelocity = -yVelocity * reboundEnergyFactor;
                }
                break;
            case BOTTOMRIGHT:
                if (xVelocity < 0){
                    xVelocity = -xVelocity * reboundEnergyFactor;
                }
                if (yVelocity < 0){
                    yVelocity = -yVelocity * reboundEnergyFactor;
                }
                break;
        }
        xVelocity = xVelocity + (vX * 500);
        yVelocity = yVelocity + (vY * 150);
    }
}
