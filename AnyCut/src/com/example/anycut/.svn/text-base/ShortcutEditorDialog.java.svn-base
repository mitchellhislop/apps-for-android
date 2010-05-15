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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent.ShortcutIconResource;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * A dialog that can edit a shortcut intent. For now the icon is displayed, and only
 * the name may be edited.
 */
public class ShortcutEditorDialog extends AlertDialog implements OnClickListener, TextWatcher {
    static final String STATE_INTENT = "intent";

    private boolean mCreated = false;

    private Intent mIntent;
    private ImageView mIconView;
    private EditText mNameView;
    private OnClickListener mOnClick;
    private OnCancelListener mOnCancel;

    public ShortcutEditorDialog(Context context, OnClickListener onClick,
            OnCancelListener onCancel) {
        super(context);

        mOnClick = onClick;
        mOnCancel = onCancel;

        // Setup the dialog
        View view = getLayoutInflater().inflate(R.layout.shortcut_editor, null, false);
        setTitle(R.string.shortcutEditorTitle);
        setButton(context.getText(android.R.string.ok), this);
        setButton2(context.getText(android.R.string.cancel), mOnClick);
        setOnCancelListener(mOnCancel);
        setCancelable(true);
        setView(view);

        mIconView = (ImageView) view.findViewById(R.id.shortcutIcon);
        mNameView = (EditText) view.findViewById(R.id.shortcutName);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON1) {
            String name = mNameView.getText().toString();
            if (TextUtils.isEmpty(name)) {
                // Don't allow an empty name
                mNameView.setError(getContext().getText(R.string.errorEmptyName));
                return;
            }
            mIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        }
        mOnClick.onClick(dialog, which);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            mIntent = state.getParcelable(STATE_INTENT);
        }

        mCreated = true;

        // If an intent is set make sure to load it now that it's safe
        if (mIntent != null) {
            loadIntent(mIntent);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putParcelable(STATE_INTENT, getIntent());
        return state;
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Do nothing
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Do nothing
    }

    public void afterTextChanged(Editable text) {
        if (text.length() == 0) {
            mNameView.setError(getContext().getText(R.string.errorEmptyName));
        } else {
            mNameView.setError(null);
        }
    }

    /**
     * Saves the current state of the editor into the intent and returns it.
     *
     * @return the intent for the shortcut being edited
     */
    public Intent getIntent() {
        mIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mNameView.getText().toString());
        return mIntent;
    }

    /**
     * Reads the state of the shortcut from the intent and sets up the editor
     *
     * @param intent A shortcut intent to edit
     */
    public void setIntent(Intent intent) {
        mIntent = intent;
        if (mCreated) {
            loadIntent(intent);
        }
    }

    /**
     * Loads the editor state from a shortcut intent.
     *
     * @param intent The shortcut intent to load the editor from
     */
    private void loadIntent(Intent intent) {
        // Show the icon
        Bitmap iconBitmap = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (iconBitmap != null) {
            mIconView.setImageBitmap(iconBitmap);
        } else {
            ShortcutIconResource iconRes = intent.getParcelableExtra(
                    Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (iconRes != null) {
                int res = getContext().getResources().getIdentifier(iconRes.resourceName, null,
                        iconRes.packageName);
                mIconView.setImageResource(res);
            } else {
                mIconView.setVisibility(View.INVISIBLE);
            }
        }

        // Fill in the name field for editing
        mNameView.addTextChangedListener(this);
        mNameView.setText(intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));

        // Ensure the intent has the proper flags
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
