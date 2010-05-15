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

import android.app.Service;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * CheckUpdateService checks every 24 hours if updates have been made to the photostreams
 * of the current contacts. This service simply polls an RSS feed and compares the
 * modification timestamp with the one stored in the database.
 */
public class CheckUpdateService extends Service {
    private static boolean DEBUG = false;

    // Check interval: every 24 hours
    private static long UPDATES_CHECK_INTERVAL = 24 * 60 * 60 * 1000;

    private CheckForUpdatesTask mTask;

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        (mTask = new CheckForUpdatesTask()).execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null && mTask.getStatus() == UserTask.Status.RUNNING) {
            mTask.cancel(true);
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    static void schedule(Context context) {
        final Intent intent = new Intent(context, CheckUpdateService.class);
        final PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);

        Calendar c = new GregorianCalendar();
        c.add(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        final AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pending);
        if (DEBUG) {
            alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                    30 * 1000, pending);
        } else {
            alarm.setRepeating(AlarmManager.RTC, c.getTimeInMillis(),
                    UPDATES_CHECK_INTERVAL, pending);
        }
    }

    private class CheckForUpdatesTask extends UserTask<Void, Object, Void> {
        private SharedPreferences mPreferences;
        private NotificationManager mManager;

        @Override
        public void onPreExecute() {
            mPreferences = getSharedPreferences(Preferences.NAME, MODE_PRIVATE);
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        public Void doInBackground(Void... params) {
            final UserDatabase helper = new UserDatabase(CheckUpdateService.this);
            final SQLiteDatabase database = helper.getWritableDatabase();

            Cursor cursor = null;
            try {
                cursor = database.query(UserDatabase.TABLE_USERS,
                        new String[] { UserDatabase._ID, UserDatabase.COLUMN_NSID,
                        UserDatabase.COLUMN_REALNAME, UserDatabase.COLUMN_LAST_UPDATE },
                        null, null, null, null, null);

                int idIndex = cursor.getColumnIndexOrThrow(UserDatabase._ID);
                int realNameIndex = cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_REALNAME);
                int nsidIndex = cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_NSID);
                int lastUpdateIndex = cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_LAST_UPDATE);

                final Flickr flickr = Flickr.get();

                final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                final Calendar reference = Calendar.getInstance();

                while (!isCancelled() && cursor.moveToNext()) {
                    final String nsid = cursor.getString(nsidIndex);
                    calendar.setTimeInMillis(cursor.getLong(lastUpdateIndex));

                    reference.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));

                    if (flickr.hasUpdates(Flickr.User.fromId(nsid), reference)) {
                        publishProgress(nsid, cursor.getString(realNameIndex),
                                cursor.getInt(idIndex));
                    }
                }

                final ContentValues values = new ContentValues();
                values.put(UserDatabase.COLUMN_LAST_UPDATE, System.currentTimeMillis());

                database.update(UserDatabase.TABLE_USERS, values, null, null);
            } finally {
                if (cursor != null) cursor.close();
                database.close();
            }

            return null;
        }

        @Override
        public void onProgressUpdate(Object... values) {
            if (mPreferences.getBoolean(Preferences.KEY_ENABLE_NOTIFICATIONS, true)) {
                final Integer id = (Integer) values[2];
                final Intent intent = new Intent(PhotostreamActivity.ACTION);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(PhotostreamActivity.EXTRA_NOTIFICATION, id);
                intent.putExtra(PhotostreamActivity.EXTRA_NSID, values[0].toString());

                Notification notification = new Notification(R.drawable.stat_notify,
                        getString(R.string.notification_new_photos, values[1]),
                        System.currentTimeMillis());
                notification.setLatestEventInfo(CheckUpdateService.this,
                        getString(R.string.notification_title),
                        getString(R.string.notification_contact_has_new_photos, values[1]),
                        PendingIntent.getActivity(CheckUpdateService.this, 0, intent,
                                PendingIntent.FLAG_CANCEL_CURRENT));

                if (mPreferences.getBoolean(Preferences.KEY_VIBRATE, false)) {
                    notification.defaults |= Notification.DEFAULT_VIBRATE;
                }

                String ringtoneUri = mPreferences.getString(Preferences.KEY_RINGTONE, null);
                notification.sound = TextUtils.isEmpty(ringtoneUri) ? null : Uri.parse(ringtoneUri);

                mManager.notify(id, notification);
            }
        }

        @Override
        public void onPostExecute(Void aVoid) {
            stopSelf();
        }
    }
}
