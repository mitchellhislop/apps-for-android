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

import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Presents the user with a list of types of shortucts that can be created.
 * When Any Cut is launched through the home screen this is the activity that comes up.
 */
public class CreateShortcutActivity extends ListActivity implements DialogInterface.OnClickListener,
        Dialog.OnCancelListener {
    private static final boolean ENABLE_ACTION_ICON_OVERLAYS = false;

    private static final int REQUEST_PHONE = 1;
    private static final int REQUEST_TEXT = 2;
    private static final int REQUEST_ACTIVITY = 3;
    private static final int REQUEST_CUSTOM = 4;

    private static final int LIST_ITEM_DIRECT_CALL = 0;
    private static final int LIST_ITEM_DIRECT_TEXT = 1;
    private static final int LIST_ITEM_ACTIVITY = 2;
    private static final int LIST_ITEM_CUSTOM = 3;

    private static final int DIALOG_SHORTCUT_EDITOR = 1;

    private Intent mEditorIntent;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setListAdapter(ArrayAdapter.createFromResource(this, R.array.mainMenu,
                android.R.layout.simple_list_item_1));
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        switch (position) {
            case LIST_ITEM_DIRECT_CALL: {
                Intent intent = new Intent(Intent.ACTION_PICK, Phones.CONTENT_URI);
                intent.putExtra(Contacts.Intents.UI.TITLE_EXTRA_KEY,
                        getText(R.string.callShortcutActivityTitle));
                startActivityForResult(intent, REQUEST_PHONE);
                break;
            }

            case LIST_ITEM_DIRECT_TEXT: {
                Intent intent = new Intent(Intent.ACTION_PICK, Phones.CONTENT_URI);
                intent.putExtra(Contacts.Intents.UI.TITLE_EXTRA_KEY,
                        getText(R.string.textShortcutActivityTitle));
                startActivityForResult(intent, REQUEST_TEXT);
                break;
            }

            case LIST_ITEM_ACTIVITY: {
                Intent intent = new Intent();
                intent.setClass(this, ActivityPickerActivity.class);
                startActivityForResult(intent, REQUEST_ACTIVITY);
                break;
            }

            case LIST_ITEM_CUSTOM: {
                Intent intent = new Intent();
                intent.setClass(this, CustomShortcutCreatorActivity.class);
                startActivityForResult(intent, REQUEST_CUSTOM);
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_PHONE: {
                startShortcutEditor(generatePhoneShortcut(result, R.drawable.sym_action_call,
                        "tel", Intent.ACTION_CALL));
                break;
            }

            case REQUEST_TEXT: {
                startShortcutEditor(generatePhoneShortcut(result, R.drawable.sym_action_sms,
                        "smsto", Intent.ACTION_SENDTO));
                break;
            }

            case REQUEST_ACTIVITY:
            case REQUEST_CUSTOM: {
                startShortcutEditor(result);
                break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_SHORTCUT_EDITOR: {
                return new ShortcutEditorDialog(this, this, this);
            }
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    protected void onPrepareDialog(int dialogId, Dialog dialog) {
        switch (dialogId) {
            case DIALOG_SHORTCUT_EDITOR: {
                if (mEditorIntent != null) {
                    // If the editor intent hasn't been set already set it
                    ShortcutEditorDialog editor = (ShortcutEditorDialog) dialog;
                    editor.setIntent(mEditorIntent);
                    mEditorIntent = null;
                }
            }
        }
    }

    /**
     * Starts the shortcut editor
     *
     * @param shortcutIntent The shortcut intent to edit
     */
    private void startShortcutEditor(Intent shortcutIntent) {
        mEditorIntent = shortcutIntent;
        showDialog(DIALOG_SHORTCUT_EDITOR);
    }

    public void onCancel(DialogInterface dialog) {
        // Remove the dialog, it won't be used again
        removeDialog(DIALOG_SHORTCUT_EDITOR);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON1) {
            // OK button
            ShortcutEditorDialog editor = (ShortcutEditorDialog) dialog;
            Intent shortcut = editor.getIntent();
            setResult(RESULT_OK, shortcut);
            finish();
        }

        // Remove the dialog, it won't be used again
        removeDialog(DIALOG_SHORTCUT_EDITOR);
    }

    /**
     * Returns an Intent describing a direct text message shortcut.
     *
     * @param result The result from the phone number picker
     * @return an Intent describing a phone number shortcut
     */
    private Intent generatePhoneShortcut(Intent result, int actionResId, String scheme, String action) {
        Uri phoneUri = result.getData();
        long personId = 0;
        String name = null;
        String number = null;
        int type;
        Cursor cursor = getContentResolver().query(phoneUri,
                new String[] { Phones.PERSON_ID, Phones.DISPLAY_NAME, Phones.NUMBER, Phones.TYPE },
                null, null, null);
        try {
            cursor.moveToFirst();
            personId = cursor.getLong(0);
            name = cursor.getString(1);
            number = cursor.getString(2);
            type = cursor.getInt(3);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Intent intent = new Intent();
        Uri personUri = ContentUris.withAppendedId(People.CONTENT_URI, personId);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON,
                generatePhoneNumberIcon(personUri, type, actionResId));

        // Make the URI a direct tel: URI so that it will always continue to work
        phoneUri = Uri.fromParts(scheme, number, null);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(action, phoneUri));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        return intent;
    }

    /**
     * Generates a phone number shortcut icon. Adds an overlay describing the type of the phone
     * number, and if there is a photo also adds the call action icon.
     *
     * @param personUri The person the phone number belongs to
     * @param type The type of the phone number
     * @param actionResId The ID for the action resource
     * @return The bitmap for the icon
     */
    private Bitmap generatePhoneNumberIcon(Uri personUri, int type, int actionResId) {
        final Resources r = getResources();
        boolean drawPhoneOverlay = true;

        Bitmap photo = People.loadContactPhoto(this, personUri, 0, null);
        if (photo == null) {
            // If there isn't a photo use the generic phone action icon instead
            Bitmap phoneIcon = getPhoneActionIcon(r, actionResId);
            if (phoneIcon != null) {
                photo = phoneIcon;
                drawPhoneOverlay = false;
            } else {
                return null;
            }
        }

        // Setup the drawing classes
        int iconSize = (int) r.getDimension(android.R.dimen.app_icon_size);
        Bitmap icon = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);

        // Copy in the photo
        Paint photoPaint = new Paint();
        photoPaint.setDither(true);
        photoPaint.setFilterBitmap(true);
        Rect src = new Rect(0,0, photo.getWidth(),photo.getHeight());
        Rect dst = new Rect(0,0, iconSize,iconSize);
        canvas.drawBitmap(photo, src, dst, photoPaint);

        // Create an overlay for the phone number type
        String overlay = null;
        switch (type) {
            case Phones.TYPE_HOME:
                overlay = "H";
                break;

            case Phones.TYPE_MOBILE:
                overlay = "M";
                break;

            case Phones.TYPE_WORK:
                overlay = "W";
                break;

            case Phones.TYPE_PAGER:
                overlay = "P";
                break;

            case Phones.TYPE_OTHER:
                overlay = "O";
                break;
        }
        if (overlay != null) {
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            textPaint.setTextSize(20.0f);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setColor(r.getColor(R.color.textColorIconOverlay));
            textPaint.setShadowLayer(3f, 1, 1, r.getColor(R.color.textColorIconOverlayShadow));
            canvas.drawText(overlay, 2, 16, textPaint);
        }

        // Draw the phone action icon as an overlay
        if (ENABLE_ACTION_ICON_OVERLAYS && drawPhoneOverlay) {
            Bitmap phoneIcon = getPhoneActionIcon(r, actionResId);
            if (phoneIcon != null) {
                src.set(0,0, phoneIcon.getWidth(),phoneIcon.getHeight());
                int iconWidth = icon.getWidth();
                dst.set(iconWidth - 20, -1, iconWidth, 19);
                canvas.drawBitmap(phoneIcon, src, dst, photoPaint);
            }
        }

        return icon;
    }

    /**
     * Returns the icon for the phone call action.
     *
     * @param r The resources to load the icon from
     * @param resId The resource ID to load
     * @return the icon for the phone call action
     */
    private Bitmap getPhoneActionIcon(Resources r, int resId) {
        Drawable phoneIcon = r.getDrawable(resId);
        if (phoneIcon instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) phoneIcon;
            return bd.getBitmap();
        } else {
            return null;
        }
    }
}