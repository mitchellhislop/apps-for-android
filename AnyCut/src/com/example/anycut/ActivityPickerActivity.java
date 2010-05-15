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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.photostream.UserTask;

/**
 * Presents a list of activities to choose from. This list only contains activities
 * that have ACTION_MAIN, since other types may require data as input.
 */
public class ActivityPickerActivity extends ListActivity {
    PackageManager mPackageManager;

    /**
     * This class is used to wrap ResolveInfo so that it can be filtered using
     * ArrayAdapter's built int filtering logic, which depends on toString().
     */
    private final class ResolveInfoWrapper {
        private ResolveInfo mInfo;

        public ResolveInfoWrapper(ResolveInfo info) {
            mInfo = info;
        }

        @Override
        public String toString() {
            return mInfo.loadLabel(mPackageManager).toString();
        }

        public ResolveInfo getInfo() {
            return mInfo;
        }
    }

    private class ActivityAdapter extends ArrayAdapter<ResolveInfoWrapper> {
        LayoutInflater mInflater;

        public ActivityAdapter(Activity activity, ArrayList<ResolveInfoWrapper> activities) {
            super(activity, 0, activities);
            mInflater = activity.getLayoutInflater();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ResolveInfoWrapper info = getItem(position);

            View view = convertView;
            if (view == null) {
                // Inflate the view and cache the pointer to the text view
                view = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                view.setTag(view.findViewById(android.R.id.text1));
            }

            final TextView textView = (TextView) view.getTag();
            textView.setText(info.getInfo().loadLabel(mPackageManager));

            return view;
        }
    }

    private final class LoadingTask extends UserTask<Object, Object, ActivityAdapter> {
        @Override
        public void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        public ActivityAdapter doInBackground(Object... params) {
            // Load the activities
            Intent queryIntent = new Intent(Intent.ACTION_MAIN);
            List<ResolveInfo> list = mPackageManager.queryIntentActivities(queryIntent, 0);

            // Sort the list
            Collections.sort(list, new ResolveInfo.DisplayNameComparator(mPackageManager));

            // Make the wrappers
            ArrayList<ResolveInfoWrapper> activities = new ArrayList<ResolveInfoWrapper>(list.size());
            for(ResolveInfo item : list) {
                activities.add(new ResolveInfoWrapper(item));
            }
            return new ActivityAdapter(ActivityPickerActivity.this, activities);
        }

        @Override
        public void onPostExecute(ActivityAdapter result) {
            setProgressBarIndeterminateVisibility(false);
            setListAdapter(result);
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.list);

        getListView().setTextFilterEnabled(true);

        mPackageManager = getPackageManager();

        // Start loading the data
        new LoadingTask().execute((Object[]) null);
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        ResolveInfoWrapper wrapper = (ResolveInfoWrapper) getListAdapter().getItem(position);
        ResolveInfo info = wrapper.getInfo();

        // Build the intent for the chosen activity
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(info.activityInfo.applicationInfo.packageName,
                info.activityInfo.name));
        Intent result = new Intent();
        result.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);

        // Set the name of the activity
        result.putExtra(Intent.EXTRA_SHORTCUT_NAME, info.loadLabel(mPackageManager));

        // Build the icon info for the activity
        Drawable drawable = info.loadIcon(mPackageManager);
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) drawable;
            result.putExtra(Intent.EXTRA_SHORTCUT_ICON, bd.getBitmap());
        }
//        ShortcutIconResource iconResource = new ShortcutIconResource();
//        iconResource.packageName = info.activityInfo.packageName;
//        iconResource.resourceName = getResources().getResourceEntryName(info.getIconResource());
//        result.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Set the result
        setResult(RESULT_OK, result);
        finish();
    }
}
