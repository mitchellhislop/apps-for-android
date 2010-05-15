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

package com.google.android.photostream;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;
import android.content.ContentValues;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.BaseColumns;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Helper class to interact with the database that stores the Flickr contacts.
 */
class UserDatabase extends SQLiteOpenHelper implements BaseColumns {
    private static final String DATABASE_NAME = "flickr";
    private static final int DATABASE_VERSION = 1;

    static final String TABLE_USERS = "users";
    static final String COLUMN_USERNAME = "username";
    static final String COLUMN_REALNAME = "realname";
    static final String COLUMN_NSID = "nsid";
    static final String COLUMN_BUDDY_ICON = "buddy_icon";
    static final String COLUMN_LAST_UPDATE = "last_update";

    static final String SORT_DEFAULT = COLUMN_USERNAME + " ASC";

    private Context mContext;

    UserDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users ("
                + "_id INTEGER PRIMARY KEY, "
                + "username TEXT, "
                + "realname TEXT, "
                + "nsid TEXT, "
                + "buddy_icon BLOB,"
                + "last_update INTEGER);");

        addUser(db, "Bob Lee", "Bob Lee", "45701389@N00", R.drawable.boblee_buddyicon);
        addUser(db, "ericktseng", "Erick Tseng", "76701017@N00", R.drawable.ericktseng_buddyicon);
        addUser(db, "romainguy", "Romain Guy", "24046097@N00", R.drawable.romainguy_buddyicon);
    }

    private void addUser(SQLiteDatabase db, String userName, String realName,
            String nsid, int icon) {

        final ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, userName);
        values.put(COLUMN_REALNAME, realName);
        values.put(COLUMN_NSID, nsid);
        values.put(COLUMN_LAST_UPDATE, System.currentTimeMillis());

        final Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), icon);
        writeBitmap(values, COLUMN_BUDDY_ICON, bitmap);

        db.insert(TABLE_USERS, COLUMN_LAST_UPDATE, values);
    }

    static void writeBitmap(ContentValues values, String name, Bitmap bitmap) {
        if (bitmap != null) {
            // Try go guesstimate how much space the icon will take when serialized
            // to avoid unnecessary allocations/copies during the write.
            int size = bitmap.getWidth() * bitmap.getHeight() * 2;
            ByteArrayOutputStream out = new ByteArrayOutputStream(size);
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                values.put(name, out.toByteArray());
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(Flickr.LOG_TAG, "Upgrading database from version " + oldVersion + " to " +
                newVersion + ", which will destroy all old data");

        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}
