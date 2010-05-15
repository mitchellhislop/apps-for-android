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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.Menu;
import android.view.KeyEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.CursorAdapter;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;

/**
 * Activity used to login the user. The activity asks for the user name and then add
 * the user to the users list upong successful login. If the login is unsuccessful,
 * an error message is displayed. Clicking any stored user launches PhotostreamActivity.
 *
 * This activity is also used to create Home shortcuts. When the intent
 * {@link Intent#ACTION_CREATE_SHORTCUT} is used to start this activity, sucessful login
 * returns a shortcut Intent to Home instead of proceeding to PhotostreamActivity.
 *
 * The shortcut Intent contains the real name of the user, his buddy icon, the action
 * {@link android.content.Intent#ACTION_VIEW} and the URI flickr://photos/nsid.
 */
public class LoginActivity extends Activity implements View.OnKeyListener,
        AdapterView.OnItemClickListener {

    private static final int MENU_ID_SHOW = 1;
    private static final int MENU_ID_DELETE = 2;

    private boolean mCreateShortcut;

    private TextView mUsername;
    private ProgressBar mProgress;

    private SQLiteDatabase mDatabase;
    private UsersAdapter mAdapter;

    private UserTask<String, Void, Flickr.User> mTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        schedule();

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon successful login
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            mCreateShortcut = true;
        }

        mDatabase = new UserDatabase(this).getWritableDatabase();

        setContentView(R.layout.screen_login);
        setupViews();
    }

    private void schedule() {
        //SharedPreferences preferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
        //if (!preferences.getBoolean(Preferences.KEY_ALARM_SCHEDULED, false)) {
        CheckUpdateService.schedule(this);
        //    preferences.edit().putBoolean(Preferences.KEY_ALARM_SCHEDULED, true).commit();
        //}
    }

    private void setupViews() {
        mUsername = (TextView) findViewById(R.id.input_username);
        mUsername.setOnKeyListener(this);
        mUsername.requestFocus();

        mAdapter = new UsersAdapter(this, initializeCursor());

        final ListView userList = (ListView) findViewById(R.id.list_users);
        userList.setAdapter(mAdapter);
        userList.setOnItemClickListener(this);

        registerForContextMenu(userList);

        mProgress = (ProgressBar) findViewById(R.id.progress);
    }

    private Cursor initializeCursor() {
        Cursor cursor = mDatabase.query(UserDatabase.TABLE_USERS,
                new String[] { UserDatabase._ID, UserDatabase.COLUMN_REALNAME,
                UserDatabase.COLUMN_NSID, UserDatabase.COLUMN_BUDDY_ICON },
                null, null, null, null, UserDatabase.SORT_DEFAULT);

        startManagingCursor(cursor);

        return cursor;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                SettingsActivity.show(this);
                return true;
            case R.id.menu_item_info:
                Eula.showDisclaimer(this);
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (v.getId()) {
                case R.id.input_username:
                    if (keyCode == KeyEvent.KEYCODE_ENTER ||
                            keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        onAddUser(mUsername.getText().toString());
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Flickr.User user = Flickr.User.fromId(((UserDescription) view.getTag()).nsid);

        if (!mCreateShortcut) {
            onShowPhotostream(user);
        } else {
            onCreateShortcut(user);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(((TextView) info.targetView).getText());

        menu.add(0, MENU_ID_SHOW, 0, R.string.context_menu_show_photostream);
        menu.add(0, MENU_ID_DELETE, 0, R.string.context_menu_delete_user);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)
                item.getMenuInfo();
        final UserDescription description = (UserDescription) info.targetView.getTag();

        switch (item.getItemId()) {
            case MENU_ID_SHOW:
                final Flickr.User user = Flickr.User.fromId(description.nsid);
                onShowPhotostream(user);
                return true;
            case MENU_ID_DELETE:
                onRemoveUser(description.id);
                return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mProgress.getVisibility() == View.VISIBLE) {
            mProgress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mTask != null && mTask.getStatus() == UserTask.Status.RUNNING) {
            mTask.cancel(true);
        }

        mAdapter.cleanup();
        mDatabase.close();
    }

    private void onAddUser(String username) {
        // When the user enters his user name, we need to find his NSID before
        // adding it to the list.
        mTask = new FindUserTask().execute(username);
    }

    private void onRemoveUser(String id) {
        int rows = mDatabase.delete(UserDatabase.TABLE_USERS, UserDatabase._ID + "=?",
                new String[] { id });
        if (rows > 0) {
            mAdapter.refresh();
        }
    }

    private void onError() {
        hideProgress();
        mUsername.setError(getString(R.string.screen_login_error));
    }

    private void hideProgress() {
        if (mProgress.getVisibility() != View.GONE) {
            final Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            mProgress.setVisibility(View.GONE);
            mProgress.startAnimation(fadeOut);
        }
    }

    private void showProgress() {
        if (mProgress.getVisibility() != View.VISIBLE) {
            final Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            mProgress.setVisibility(View.VISIBLE);
            mProgress.startAnimation(fadeIn);
        }
    }

    private void onShowPhotostream(Flickr.User user) {
        PhotostreamActivity.show(this, user);
    }

    /**
     * Creates the shortcut Intent to send back to Home. The intent is a view action
     * to a flickr://photos/nsid URI, with a title (real name or user name) and a
     * custom icon (the user's buddy icon.)
     *
     * @param user The user to create a shortcut for.
     */
    private void onCreateShortcut(Flickr.User user) {
        final Cursor cursor = mDatabase.query(UserDatabase.TABLE_USERS,
                new String[] { UserDatabase.COLUMN_REALNAME, UserDatabase.COLUMN_USERNAME,
                UserDatabase.COLUMN_BUDDY_ICON }, UserDatabase.COLUMN_NSID + "=?",
                new String[] { user.getId() }, null, null, UserDatabase.SORT_DEFAULT);
        cursor.moveToFirst();

        final Intent shortcutIntent = new Intent(PhotostreamActivity.ACTION);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shortcutIntent.putExtra(PhotostreamActivity.EXTRA_NSID, user.getId());

        // Sets the custom shortcut's title to the real name of the user. If no
        // real name was found, use the user name instead.
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        String name = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_REALNAME));
        if (name == null || name.length() == 0) {
            name = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_USERNAME));
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);

        // Sets the custom shortcut icon to the user's buddy icon. If no buddy
        // icon was found, use a default local buddy icon instead.
        byte[] data = cursor.getBlob(cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_BUDDY_ICON));
        Bitmap icon = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (icon != null) {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        } else {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.default_buddyicon));
        }

        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Background task used to load the user's NSID. The task begins by showing the
     * progress bar, then loads the user NSID from the network and finally open
     * PhotostreamActivity.
     */
    private class FindUserTask extends UserTask<String, Void, Flickr.User> {
        @Override
        public void onPreExecute() {
            showProgress();
        }

        public Flickr.User doInBackground(String... params) {
            final String name = params[0].trim();
            if (name.length() == 0) return null;

            final Flickr.User user = Flickr.get().findByUserName(name);
            if (isCancelled() || user == null) return null;

            Flickr.UserInfo info = Flickr.get().getUserInfo(user);
            if (isCancelled() || info == null) return null;

            String realname = info.getRealName();
            if (realname == null) realname = name;

            final ContentValues values = new ContentValues();
            values.put(UserDatabase.COLUMN_USERNAME, name);
            values.put(UserDatabase.COLUMN_REALNAME, realname);
            values.put(UserDatabase.COLUMN_NSID, user.getId());
            values.put(UserDatabase.COLUMN_LAST_UPDATE, System.currentTimeMillis());
            UserDatabase.writeBitmap(values, UserDatabase.COLUMN_BUDDY_ICON,
                    info.loadBuddyIcon());

            long result = -1;
            if (!isCancelled()) {
                result = mDatabase.insert(UserDatabase.TABLE_USERS,
                        UserDatabase.COLUMN_REALNAME, values);
            }

            return result != -1 ? user : null;
        }

        @Override
        public void onPostExecute(Flickr.User user) {
            if (user == null) {
                onError();
            } else {
                mAdapter.refresh();
                hideProgress();
            }
        }
    }

    private class UsersAdapter extends CursorAdapter {
        private final LayoutInflater mInflater;
        private final int mRealname;
        private final int mId;
        private final int mNsid;
        private final int mBuddyIcon;
        private final Drawable mDefaultIcon;
        private final HashMap<String, Drawable> mIcons = new HashMap<String, Drawable>();

        public UsersAdapter(Context context, Cursor cursor) {
            super(context, cursor, true);

            mInflater = LayoutInflater.from(context);
            mDefaultIcon = context.getResources().getDrawable(R.drawable.default_buddyicon);

            mRealname = cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_REALNAME);
            mId = cursor.getColumnIndexOrThrow(UserDatabase._ID);
            mNsid = cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_NSID);
            mBuddyIcon = cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_BUDDY_ICON);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mInflater.inflate(R.layout.list_item_user, parent, false);
            final UserDescription description = new UserDescription();
            view.setTag(description);
            return view;
        }

        public void bindView(View view, Context context, Cursor cursor) {
            final UserDescription description = (UserDescription) view.getTag();
            description.id = cursor.getString(mId);
            description.nsid = cursor.getString(mNsid);

            final TextView textView = (TextView) view;
            textView.setText(cursor.getString(mRealname));

            Drawable icon = mIcons.get(description.nsid);
            if (icon == null) {
                final byte[] data = cursor.getBlob(mBuddyIcon);

                Bitmap bitmap = null;
                if (data != null) bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                if (bitmap != null) {
                    icon = new FastBitmapDrawable(bitmap);
                } else {
                    icon = mDefaultIcon;
                }

                mIcons.put(description.nsid, icon);
            }

            textView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }

        void cleanup() {
            for (Drawable icon : mIcons.values()) {
                icon.setCallback(null);
            }
        }

        void refresh() {
            getCursor().requery();
        }
    }

    private static class UserDescription {
        String id;
        String nsid;
    }
}
