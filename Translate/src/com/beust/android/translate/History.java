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

package com.beust.android.translate;

import com.beust.android.translate.Languages.Language;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This class handles the history of past translations.
 */
public class History {
    private static final String HISTORY = "history";

    /**
     * Sort the translations by timestamp.
     */
    private static final Comparator<HistoryRecord> MOST_RECENT_COMPARATOR
             = new Comparator<HistoryRecord>() {

        public int compare(HistoryRecord object1, HistoryRecord object2) {
            return (int) (object2.when - object1.when);
        }
    };

    /**
     * Sort the translations by destination language and then by input.
     */
    private static final Comparator<HistoryRecord> LANGUAGE_COMPARATOR
            = new Comparator<HistoryRecord>() {

        public int compare(HistoryRecord object1, HistoryRecord object2) {
            int result = object1.to.getLongName().compareTo(object2.to.getLongName());
            if (result == 0) {
                result = object1.input.compareTo(object2.input);
            }
            return result;
        }
    };

    private List<HistoryRecord> mHistoryRecords = Lists.newArrayList();
    
    public History(SharedPreferences prefs) {
        mHistoryRecords = restoreHistory(prefs);
    }

    public static List<HistoryRecord> restoreHistory(SharedPreferences prefs) {
        List<HistoryRecord> result = Lists.newArrayList();
        boolean done = false;
        int i = 0;
        Map<String, ?> allKeys = prefs.getAll();
        for (String key : allKeys.keySet()) {
            if (key.startsWith(HISTORY)) {
                String value = (String) allKeys.get(key);
                result.add(HistoryRecord.decode(value));
            }
        }
//        while (! done) {
//            String history = prefs.getString(HISTORY + "-" + i++, null);
//            if (history != null) {
//                result.add(HistoryRecord.decode(history));
//            } else {
//                done = true;
//            }
//        }

        return result;
    }

//    public void saveHistory(Editor edit) {
//        log("Saving history");
//        for (int i = 0; i < mHistoryRecords.size(); i++) {
//            HistoryRecord hr = mHistoryRecords.get(i);
//            edit.putString(HISTORY + "-" + i, hr.encode());
//        }
//    }
    
    public static void addHistoryRecord(Context context,
        Language from, Language to, String input, String output) {
        History historyRecord = new History(TranslateActivity.getPrefs(context));
        HistoryRecord hr = new HistoryRecord(from, to, input, output, System.currentTimeMillis());
        
        // Find an empty key to add this history record
        SharedPreferences prefs = TranslateActivity.getPrefs(context);
        int i = 0;
        while (true) {
            String key = HISTORY + "-" + i; 
            if (!prefs.contains(key)) {
                Editor edit = prefs.edit();
                edit.putString(key, hr.encode());
                log("Committing " + key + " " + hr.encode());
                edit.commit();
                return;
            } else {
                i++;
            }
        }
    }
    
//    public static void addHistoryRecord(Context context, List<HistoryRecord> result, HistoryRecord hr) {
//        if (! result.contains(hr)) {
//            result.add(hr);
//        }
//        Editor edit = getPrefs(context).edit();
//    }

    private static void log(String s) {
        Log.d(TranslateActivity.TAG, "[History] " + s);
    }
    
    
    public List<HistoryRecord> getHistoryRecordsMostRecentFirst() {
        Collections.sort(mHistoryRecords, MOST_RECENT_COMPARATOR);
        return mHistoryRecords;
    }
    
    public List<HistoryRecord> getHistoryRecordsByLanguages() {
        Collections.sort(mHistoryRecords, LANGUAGE_COMPARATOR);
        return mHistoryRecords;
    }

    public List<HistoryRecord> getHistoryRecords(Comparator<HistoryRecord> comparator) {
        if (comparator != null) {
            Collections.sort(mHistoryRecords, comparator);
        }
        return mHistoryRecords;
    }

    public void clear(Context context) {
        int size = mHistoryRecords.size();
        mHistoryRecords = Lists.newArrayList();
        Editor edit = TranslateActivity.getPrefs(context).edit();
        for (int i = 0; i < size; i++) {
            String key = HISTORY + "-" + i;
            log("Removing key " + key);
            edit.remove(key);
        }
        edit.commit();
    }


}
