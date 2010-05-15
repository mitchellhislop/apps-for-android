// Copyright 2008 Google Inc. All Rights Reserved.
package com.beust.android.translate;

import com.beust.android.translate.ITranslate;
import com.beust.android.translate.Translate;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Performs language translation.
 * 
 * @author Daniel Rall
 */
public class TranslateService extends Service {
    public static final String TAG = "TranslateService";

    private static final String[] TRANSLATE_ACTIONS = {
        Intent.ACTION_GET_CONTENT,
        Intent.ACTION_PICK,
        Intent.ACTION_VIEW
    };

    private final ITranslate.Stub mBinder = new ITranslate.Stub() {
        /**
         * Translates text from a given language to another given language
         * using Google Translate.
         * 
         * @param text The text to translate.
         * @param from The language code to translate from.
         * @param to The language code to translate to.
         * @return The translated text, or <code>null</code> on error.
         */
        public String translate(String text, String from, String to) {
            try {
                return Translate.translate(text, from, to);
            } catch (Exception e) {
                Log.e(TAG, "Failed to perform translation: " + e.getMessage());
                return null;
            }
        }

        /**
         * @return The service version number.
         */
        public int getVersion() {
            return 1;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        for (int i = 0; i < TRANSLATE_ACTIONS.length; i++) {
            if (TRANSLATE_ACTIONS[i].equals(intent.getAction())) {
                return mBinder;
            }
        }
        return null;
    }

}
