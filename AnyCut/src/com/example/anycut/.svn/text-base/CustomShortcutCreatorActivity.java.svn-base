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

package com.example.anycut;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

/**
 * A simple activity to allow the user to manually type in an Intent.
 */
public class CustomShortcutCreatorActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.custom_shortcut_creator);

        findViewById(R.id.ok).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok: {
                Intent intent = createShortcutIntent();
                setResult(RESULT_OK, intent);
                finish();
                break;
            }

            case R.id.cancel: {
                setResult(RESULT_CANCELED);
                finish();
                break;
            }
        }
    }

    private Intent createShortcutIntent() {
        Intent intent = new Intent();

        EditText view;
        view = (EditText) findViewById(R.id.action);
        intent.setAction(view.getText().toString());

        view = (EditText) findViewById(R.id.data);
        String data = view.getText().toString();
        view = (EditText) findViewById(R.id.type);
        String type = view.getText().toString();

        boolean dataEmpty = TextUtils.isEmpty(data);
        boolean typeEmpty = TextUtils.isEmpty(type);
        if (!dataEmpty && typeEmpty) {
            intent.setData(Uri.parse(data));
        } else if (!typeEmpty && dataEmpty) {
            intent.setType(type);
        } else if (!typeEmpty && !dataEmpty) {
            intent.setDataAndType(Uri.parse(data), type);
        }

        return new Intent().putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
    }
}
