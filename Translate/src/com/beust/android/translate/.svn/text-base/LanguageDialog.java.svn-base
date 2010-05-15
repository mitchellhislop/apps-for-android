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

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

import com.beust.android.translate.Languages.Language;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * This dialog displays a list of languages and then tells the calling activity which language
 * was selected. 
 */
public class LanguageDialog extends AlertDialog implements OnClickListener {
    private TranslateActivity mActivity;
    private boolean mFrom;

    protected LanguageDialog(TranslateActivity activity) {
        super(activity);

        mActivity = activity;

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
        ScrollView scrollView = (ScrollView) inflater.inflate(R.layout.language_dialog, null);
        setView(scrollView);
        
        LinearLayout layout = (LinearLayout) scrollView.findViewById(R.id.languages);
        
        LinearLayout current = null;
        Language[] languages = Language.values();
        for (int i = 0; i < languages.length; i++) {
            if (current != null) {
                layout.addView(current, new LayoutParams(FILL_PARENT, FILL_PARENT));
            }
            current = new LinearLayout(activity);
            current.setOrientation(LinearLayout.HORIZONTAL);
            Button button = (Button) inflater.inflate(R.layout.language_entry, current, false);

            Language language = languages[i];
            language.configureButton(mActivity, button);
            button.setOnClickListener(this);
            current.addView(button, button.getLayoutParams());
        }
        if (current != null) {
            layout.addView(current, new LayoutParams(FILL_PARENT, FILL_PARENT));
        }
        setTitle(" ");  // set later, but necessary to put a non-empty string here
    }

    private void log(String s) {
        Log.d(TranslateActivity.TAG, s);
    }

    public void onClick(View v) {
        mActivity.setNewLanguage((Language) v.getTag(), mFrom, true /* translate */);
        dismiss();
    }

    public void setFrom(boolean from) {
        log("From set to " + from);
        mFrom = from;
        setTitle(from ? mActivity.getString(R.string.translate_from) : mActivity.getString(R.string.translate_to)); 
    }
    
}
