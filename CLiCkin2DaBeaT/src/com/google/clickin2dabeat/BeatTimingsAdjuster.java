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

import java.util.ArrayList;

/**
 * Adjusts the times for the beat targets using a linear least-squares fit.
 */
public class BeatTimingsAdjuster {
  private double[] adjustedBeatTimes;

  public void setRawBeatTimes(ArrayList<Integer> rawBeatTimes) {
    double[] beatTimes = new double[rawBeatTimes.size()];
    for (int i = 0; i < beatTimes.length; i++) {
      beatTimes[i] = rawBeatTimes.get(i);
    }

    adjustedBeatTimes = new double[beatTimes.length];
    double[] beatNumbers = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    double n = beatNumbers.length;
    // Some things can be computed once and never computed again
    double[] beatNumbersXNumbers = multiplyArrays(beatNumbers, beatNumbers);
    double sumOfBeatNumbersXNumbers = sum(beatNumbersXNumbers);
    double sumOfBeatNumbers = sum(beatNumbers);
    // The divisor is:
    // (n * sum(beatNumbersXNumbers) - (sum(beatNumbers) * sum(beatNumbers)))
    // Since these are all constants, they can be computed first for better
    // efficiency.
    double divisor = (n * sum(beatNumbersXNumbers) - (sum(beatNumbers) * sum(beatNumbers)));


    // Not enough data to adjust for the first 5 beats
    for (int i = 0; (i < beatTimes.length) && (i < 5); i++) {
      adjustedBeatTimes[i] = beatTimes[i];
    }

    // Adjust time for beat i by using timings for beats i-5 through i+5
    double[] beatWindow = new double[beatNumbers.length];
    for (int i = 0; i < beatTimes.length - 10; i++) {
      System.arraycopy(beatTimes, i, beatWindow, 0, beatNumbers.length);
      double[] beatNumbersXTimes = multiplyArrays(beatNumbers, beatWindow);
      double a =
          (sum(beatTimes) * sumOfBeatNumbersXNumbers - sumOfBeatNumbers * sum(beatNumbersXTimes))
              / divisor;

      double b = (n * sum(beatNumbersXTimes) - sumOfBeatNumbers * sum(beatTimes)) / divisor;

      adjustedBeatTimes[i + 5] = a + b * beatNumbers[5];
    }

    if (beatTimes.length - 10 < 0) {
      return;
    }

    // Not enough data to adjust for the last 5 beats
    for (int i = beatTimes.length - 10; i < beatTimes.length; i++) {
      adjustedBeatTimes[i] = beatTimes[i];
    }
  }

  public ArrayList<Target> adjustBeatTargets(ArrayList<Target> rawTargets) {
    ArrayList<Target> adjustedTargets = new ArrayList<Target>();
    int j = 0;
    double threshold = 200;
    for (int i = 0; i < rawTargets.size(); i++) {
      Target t = rawTargets.get(i);
      while ((j < adjustedBeatTimes.length) && (adjustedBeatTimes[j] < t.time)) {
        j++;
      }
      double prevTime = 0;
      if (j > 0) {
        prevTime = adjustedBeatTimes[j - 1];
      }
      double postTime = -1;
      if (j < adjustedBeatTimes.length) {
        postTime = adjustedBeatTimes[j];
      }
      if ((Math.abs(t.time - prevTime) < Math.abs(t.time - postTime))
          && Math.abs(t.time - prevTime) < threshold) {
        t.time = prevTime;
      } else if ((Math.abs(t.time - prevTime) > Math.abs(t.time - postTime))
          && Math.abs(t.time - postTime) < threshold) {
        t.time = postTime;
      }
      adjustedTargets.add(t);
    }
    return adjustedTargets;
  }


  private double sum(double[] numbers) {
    double sum = 0;
    for (int i = 0; i < numbers.length; i++) {
      sum = sum + numbers[i];
    }
    return sum;
  }


  private double[] multiplyArrays(double[] numberSetA, double[] numberSetB) {
    double[] sqArray = new double[numberSetA.length];
    for (int i = 0; i < numberSetA.length; i++) {
      sqArray[i] = numberSetA[i] * numberSetB[i];
    }
    return sqArray;
  }

}
