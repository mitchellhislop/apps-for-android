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

import android.graphics.Color;

/**
 * Contains information about the when the beat should be displayed, where it
 * should be displayed, and what color it should be.
 */
public class Target {
  public double time;
  public int x;
  public int y;
  public int color;

  public Target(double timeToHit, int xpos, int ypos, String hexColor) {
    time = timeToHit;
    x = xpos;
    y = ypos;
    if (hexColor.length() == 6) {
      int r = Integer.parseInt(hexColor.substring(0, 2), 16);
      int g = Integer.parseInt(hexColor.substring(2, 4), 16);
      int b = Integer.parseInt(hexColor.substring(4, 6), 16);
      color = Color.rgb(r, g, b);
    } else {
      int colorChoice = ((int) (Math.random() * 100)) % 4;
      if (colorChoice == 0) {
        color = Color.RED;
      } else if (colorChoice == 1) {
        color = Color.GREEN;
      } else if (colorChoice == 2) {
        color = Color.BLUE;
      } else {
        color = Color.YELLOW;
      }
    }
  }

}
