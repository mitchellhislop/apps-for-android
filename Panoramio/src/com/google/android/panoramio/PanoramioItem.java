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

import com.google.android.maps.GeoPoint;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds one item returned from the Panoramio server. This includes
 * the bitmap along with other meta info.
 *
 */
public class PanoramioItem implements Parcelable {
    
    private long mId;
    private Bitmap mBitmap;
    private GeoPoint mLocation;
    private String mTitle;
    private String mOwner;
    private String mThumbUrl;
    private String mOwnerUrl;
    private String mPhotoUrl;
    
    public PanoramioItem(Parcel in) {
        mId = in.readLong();
        mBitmap = Bitmap.CREATOR.createFromParcel(in);
        mLocation = new GeoPoint(in.readInt(), in.readInt());
        mTitle = in.readString();
        mOwner = in.readString();
        mThumbUrl = in.readString();
        mOwnerUrl = in.readString();
        mPhotoUrl = in.readString();
    }
    
    public PanoramioItem(long id, String thumbUrl, Bitmap b, int latitudeE6, int longitudeE6,
            String title, String owner, String ownerUrl, String photoUrl) {
        mBitmap = b;
        mLocation = new GeoPoint(latitudeE6, longitudeE6);
        mTitle = title;
        mOwner = owner;
        mThumbUrl = thumbUrl;
        mOwnerUrl = ownerUrl;
        mPhotoUrl = photoUrl;
    }
    
    public long getId() {
        return mId;
    }
    
    public Bitmap getBitmap() {
        return mBitmap;
    }

    public GeoPoint getLocation() {
        return mLocation;
    }
    
    public String getTitle() {
        return mTitle;
    }
    
    public String getOwner() {
        return mOwner;
    }
    
    public String getThumbUrl() {
        return mThumbUrl;
    }

    public String getOwnerUrl() {
        return mOwnerUrl;
    }

    public String getPhotoUrl() {
        return mPhotoUrl;
    }

    public static final Parcelable.Creator<PanoramioItem> CREATOR =
        new Parcelable.Creator<PanoramioItem>() {
        public PanoramioItem createFromParcel(Parcel in) {
            return new PanoramioItem(in);
        }

        public PanoramioItem[] newArray(int size) {
            return new PanoramioItem[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mId);
        mBitmap.writeToParcel(parcel, 0);
        parcel.writeInt(mLocation.getLatitudeE6());
        parcel.writeInt(mLocation.getLongitudeE6());
        parcel.writeString(mTitle);
        parcel.writeString(mOwner);
        parcel.writeString(mThumbUrl);
        parcel.writeString(mOwnerUrl);
        parcel.writeString(mPhotoUrl);
   }
}