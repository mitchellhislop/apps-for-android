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
package com.google.clickin2dabeat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Draws the beat targets and takes user input. 
 * This view is used as the foreground; the background is the video 
 * being played.
 */
public class GameView extends View {
  private static final long[] VIBE_PATTERN = {0, 1, 40, 41};

  private static final int INTERVAL = 10; // in ms
  private static final int PRE_THRESHOLD = 1000; // in ms
  private static final int POST_THRESHOLD = 500; // in ms
  private static final int TOLERANCE = 100; // in ms
  private static final int POINTS_FOR_PERFECT = 100;
  private static final double PENALTY_FACTOR = .25;
  private static final double COMBO_FACTOR = .1;

  private static final float TARGET_RADIUS = 50;
  
  public static final String LAST_RATING_OK = "(^_')";
  public static final String LAST_RATING_PERFECT = "(^_^)/";
  public static final String LAST_RATING_MISS = "(X_X)";

  private C2B parent;
  private Vibrator vibe;
  private SoundPool snd;
  private int hitOkSfx;
  private int hitPerfectSfx;
  private int missSfx;

  public int comboCount;
  public int longestCombo;
  
  public String lastRating;

  Paint innerPaint;
  Paint borderPaint;
  Paint haloPaint;

  private ArrayList<Target> drawnTargets;
  private int lastTarget;

  public ArrayList<Target> recordedTargets;

  private int score;

  public GameView(Context context) {
    super(context);

    parent = (C2B) context;
    lastTarget = 0;
    score = 0;
    comboCount = 0;
    longestCombo = 0;
    lastRating = "";

    drawnTargets = new ArrayList<Target>();
    recordedTargets = new ArrayList<Target>();

    vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

    snd = new SoundPool(10, AudioManager.STREAM_SYSTEM, 0);
    missSfx = snd.load(context, R.raw.miss, 0);
    hitOkSfx = snd.load(context, R.raw.ok, 0);
    hitPerfectSfx = snd.load(context, R.raw.perfect, 0);

    innerPaint = new Paint();
    innerPaint.setColor(Color.argb(127, 0, 0, 0));
    innerPaint.setStyle(Paint.Style.FILL);
    borderPaint = new Paint();
    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setAntiAlias(true);
    borderPaint.setStrokeWidth(2);
    haloPaint = new Paint();
    haloPaint.setStyle(Paint.Style.STROKE);
    haloPaint.setAntiAlias(true);
    haloPaint.setStrokeWidth(4);

    Thread monitorThread = (new Thread(new Monitor()));
    monitorThread.setPriority(Thread.MIN_PRIORITY);
    monitorThread.start();
  }


  private void updateTargets() {
    int i = lastTarget;
    int currentTime = parent.getCurrentTime();

    // Add any targets that are within the pre-threshold to the list of
    // drawnTargets
    boolean cont = true;
    while (cont && (i < parent.targets.size())) {
      if (parent.targets.get(i).time < currentTime + PRE_THRESHOLD) {
        drawnTargets.add(parent.targets.get(i));
        i++;
      } else {
        cont = false;
      }
    }
    lastTarget = i;

    // Move any expired targets out of drawn targets
    for (int j = 0; j < drawnTargets.size(); j++) {
      Target t = drawnTargets.get(j);
      if (t == null) {
        // Do nothing - this is a concurrency issue where
        // the target is already gone, so just ignore it
      } else if (t.time + POST_THRESHOLD < currentTime) {
        try {
          drawnTargets.remove(j);
        } catch (IndexOutOfBoundsException e){
          // Do nothing here, j is already gone
        }
        if (longestCombo < comboCount) {
          longestCombo = comboCount;
        }
        comboCount = 0;
        lastRating = LAST_RATING_MISS;
      }
    }
  }


  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      int currentTime = parent.getCurrentTime();
      float x = event.getX();
      float y = event.getY();
      boolean hadHit = false;

      if (parent.mode == C2B.ONEPASS_MODE) { // Record this point as a target
        hadHit = true;
        snd.play(hitPerfectSfx, 1, 1, 0, 0, 1);
        Target targ = new Target(currentTime, (int) x, (int) y, "");
        recordedTargets.add(targ);
      } else if (parent.mode == C2B.TWOPASS_MODE) {
        hadHit = true;
        parent.beatTimes.add(currentTime);
      } else { // Play the game normally
        for (int i = 0; i < drawnTargets.size(); i++) {
          if (hitTarget(x, y, drawnTargets.get(i))) {
            Target t = drawnTargets.get(i);
            int points;
            double timeDiff = Math.abs(currentTime - t.time);
            if (timeDiff < TOLERANCE) {
              points = POINTS_FOR_PERFECT;
              snd.play(hitPerfectSfx, 1, 1, 0, 0, 1);
              lastRating = LAST_RATING_PERFECT;
            } else {
              points = (int) (POINTS_FOR_PERFECT - (timeDiff * PENALTY_FACTOR));
              points = points + (int) (points * (comboCount * COMBO_FACTOR));
              snd.play(hitOkSfx, 1, 1, 0, 0, 1);
              lastRating = LAST_RATING_OK;
            }
            if (points > 0) {
              score = score + points;
              hadHit = true;
            }
            drawnTargets.remove(i);
            break;
          }
        }
      }
      if (hadHit) {
        comboCount++;
      } else {
        if (longestCombo < comboCount) {
          longestCombo = comboCount;
        }
        comboCount = 0;
        snd.play(missSfx, 1, 1, 0, 0, 1);
        lastRating = LAST_RATING_MISS;
      }
      vibe.vibrate(VIBE_PATTERN, -1);
    }
    return true;
  }

  private boolean hitTarget(float x, float y, Target t) {
    if (t == null) {
      return false;
    }
    // Use the pythagorean theorem to solve this.
    float xSquared = (t.x - x) * (t.x - x);
    float ySquared = (t.y - y) * (t.y - y);
    if ((xSquared + ySquared) < (TARGET_RADIUS * TARGET_RADIUS)) {
      return true;
    }
    return false;
  }


  @Override
  public void onDraw(Canvas canvas) {
    if (parent.mode != C2B.GAME_MODE) {
      return;
    }

    int currentTime = parent.getCurrentTime();

    // Draw the circles
    for (int i = 0; i < drawnTargets.size(); i++) {
      Target t = drawnTargets.get(i);
      if (t == null) {
        break;
      }
      // Insides should be semi-transparent
      canvas.drawCircle(t.x, t.y, TARGET_RADIUS, innerPaint);
      // Set colors for the target
      borderPaint.setColor(t.color);
      haloPaint.setColor(t.color);
      // Perfect timing == hitting the halo inside the borders
      canvas.drawCircle(t.x, t.y, TARGET_RADIUS - 5, borderPaint);
      canvas.drawCircle(t.x, t.y, TARGET_RADIUS, borderPaint);
      // Draw timing halos - may need to change the formula here
      float percentageOff = ((float) (t.time - currentTime)) / PRE_THRESHOLD;
      int haloSize = (int) (TARGET_RADIUS + (percentageOff * TARGET_RADIUS));
      canvas.drawCircle(t.x, t.y, haloSize, haloPaint);
    }



    // Score and Combo info
    String scoreText = "Score: " + Integer.toString(score);
    int x = getWidth() - 100; // Fudge factor for making it on the top right
    // corner
    int y = 30;
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(Color.RED);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTextSize(24);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
    y -= paint.ascent() / 2;
    canvas.drawText(scoreText, x, y, paint);

    x = getWidth() / 2;
    canvas.drawText(lastRating, x, y, paint);

    String comboText = "Combo: " + Integer.toString(comboCount);
    x = 60;
    canvas.drawText(comboText, x, y, paint);
  }



  private class Monitor implements Runnable {
    public void run() {
      while (true) {
        try {
          Thread.sleep(INTERVAL);
        } catch (InterruptedException e) {
          // This should not be interrupted. If it is, just dump the stack
          // trace.
          e.printStackTrace();
        }
        updateTargets();
        postInvalidate();
      }
    }
  }


}
