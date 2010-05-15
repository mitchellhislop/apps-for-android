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

import android.os.Bundle;
import android.preference.*;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Holds preferences for the game
 */
public class Preferences extends PreferenceActivity {

    public static final String KEY_VIBRATE = "key_vibrate";
    public static final String KEY_DIFFICULTY = "key_difficulty";

    public enum Difficulty {
        Easy(5),
        Medium(3),
        Hard(1);

        private final int mLivesToStart;

        Difficulty(int livesToStart) {
            mLivesToStart = livesToStart;
        }

        public int getLivesToStart() {
            return mLivesToStart;
        }
    }

    public static Difficulty getCurrentDifficulty(Context context) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        final String diffic = preferences
                .getString(KEY_DIFFICULTY, DEFAULT_DIFFICULTY.toString());
        return Difficulty.valueOf(diffic);
    }

    public static final Difficulty DEFAULT_DIFFICULTY = Difficulty.Medium;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setPreferenceScreen(createPreferenceHierarchy());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        // vibrate on/off
        CheckBoxPreference vibratePref = new CheckBoxPreference(this);
        vibratePref.setDefaultValue(true);
        vibratePref.setKey(KEY_VIBRATE);
        vibratePref.setTitle(R.string.settings_vibrate);
        vibratePref.setSummary(R.string.settings_vibrate_summary);
        root.addPreference(vibratePref);

        // difficulty level
        ListPreference difficultyPref = new ListPreference(this);
        difficultyPref.setEntries(new String[] {
                getString(R.string.settings_five_lives),
                getString(R.string.settings_three_lives),
                getString(R.string.settings_one_life)});
        difficultyPref.setEntryValues(new String[] {
                Difficulty.Easy.toString(),
                Difficulty.Medium.toString(),
                Difficulty.Hard.toString()});
        difficultyPref.setKey(KEY_DIFFICULTY);
        difficultyPref.setTitle(R.string.settings_difficulty);
        difficultyPref.setSummary(R.string.settings_difficulty_summary);
        final SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPrefs.contains(KEY_DIFFICULTY)) {
            difficultyPref.setValue(DEFAULT_DIFFICULTY.toString());
        }
        root.addPreference(difficultyPref);
        return root;
    }
}
