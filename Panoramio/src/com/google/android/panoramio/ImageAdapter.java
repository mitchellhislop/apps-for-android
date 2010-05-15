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

package com.google.android.panoramio;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Adapter used to bind data for the main list of photos
 */
public class ImageAdapter extends BaseAdapter {

    /**
     * Maintains the state of our data
     */
    private ImageManager mImageManager;

    private Context mContext;
    
    private MyDataSetObserver mObserver;

    /**
     * Used by the {@link ImageManager} to report changes in the list back to
     * this adapter.
     */
    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            notifyDataSetInvalidated();
        }
    }
    
    public ImageAdapter(Context c) {
        mImageManager = ImageManager.getInstance(c);
        mContext = c;
        mObserver = new MyDataSetObserver();
        
        mImageManager.addObserver(mObserver);
    }

    /**
     * Returns the number of images to display
     * 
     * @see android.widget.Adapter#getCount()
     */
    public int getCount() {
        return mImageManager.size();
    }

    /**
     * Returns the image at a specified position
     * 
     * @see android.widget.Adapter#getItem(int)
     */
    public Object getItem(int position) {
        return mImageManager.get(position);
    }

    /**
     * Returns the id of an image at a specified position
     * 
     * @see android.widget.Adapter#getItemId(int)
     */
    public long getItemId(int position) {
        PanoramioItem s = mImageManager.get(position);
        return s.getId();
    }

    /**
     * Returns a view to display the image at a specified position
     * 
     * @param position The position to display
     * @param convertView An existing view that we can reuse. May be null.
     * @param parent The parent view that will eventually hold the view we return.
     * @return A view to display the image at a specified position
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            // Make up a new view
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.image_item, null);
        } else {
            // Use convertView if it is available
            view = convertView;
        }
        PanoramioItem s = mImageManager.get(position);

        ImageView i = (ImageView) view.findViewById(R.id.image);
        i.setImageBitmap(s.getBitmap());
        i.setBackgroundResource(R.drawable.picture_frame);
        
        TextView t = (TextView) view.findViewById(R.id.title);
        t.setText(s.getTitle());
        
        t = (TextView) view.findViewById(R.id.owner);
        t.setText(s.getOwner());
        return view;
    }

}