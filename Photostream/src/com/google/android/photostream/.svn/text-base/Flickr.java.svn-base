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

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.params.HttpParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.net.URL;

import android.util.Xml;
import android.view.InflateException;
import android.net.Uri;
import android.os.Parcelable;
import android.os.Parcel;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Utility class to interact with the Flickr REST-based web services.
 *
 * This class uses a default Flickr API key that you should replace with your own if
 * you reuse this code to redistribute it with your application(s).
 *
 * This class is used as a singleton and cannot be instanciated. Instead, you must use
 * {@link #get()} to retrieve the unique instance of this class.
 */
class Flickr {
    static final String LOG_TAG = "Photostream";

    // IMPORTANT: Replace this Flickr API key with your own
    private static final String API_KEY = "730e3a4f253b30adf30177df803d38c4";    

    private static final String API_REST_HOST = "api.flickr.com";
    private static final String API_REST_URL = "/services/rest/";
    private static final String API_FEED_URL = "/services/feeds/photos_public.gne";

    private static final String API_PEOPLE_FIND_BY_USERNAME = "flickr.people.findByUsername";
    private static final String API_PEOPLE_GET_INFO = "flickr.people.getInfo";
    private static final String API_PEOPLE_GET_PUBLIC_PHOTOS = "flickr.people.getPublicPhotos";
    private static final String API_PEOPLE_GET_LOCATION = "flickr.photos.geo.getLocation";

    private static final String PARAM_API_KEY = "api_key";
    private static final String PARAM_METHOD= "method";
    private static final String PARAM_USERNAME = "username";
    private static final String PARAM_USERID = "user_id";
    private static final String PARAM_PER_PAGE = "per_page";
    private static final String PARAM_PAGE = "page";
    private static final String PARAM_EXTRAS = "extras";
    private static final String PARAM_PHOTO_ID = "photo_id";
    private static final String PARAM_FEED_ID = "id";
    private static final String PARAM_FEED_FORMAT = "format";

    private static final String VALUE_DEFAULT_EXTRAS = "date_taken";
    private static final String VALUE_DEFAULT_FORMAT = "atom";

    private static final String RESPONSE_TAG_RSP = "rsp";
    private static final String RESPONSE_ATTR_STAT = "stat";
    private static final String RESPONSE_STATUS_OK = "ok";

    private static final String RESPONSE_TAG_USER = "user";
    private static final String RESPONSE_ATTR_NSID = "nsid";

    private static final String RESPONSE_TAG_PHOTOS = "photos";
    private static final String RESPONSE_ATTR_PAGE = "page";
    private static final String RESPONSE_ATTR_PAGES = "pages";

    private static final String RESPONSE_TAG_PHOTO = "photo";
    private static final String RESPONSE_ATTR_ID = "id";
    private static final String RESPONSE_ATTR_SECRET = "secret";
    private static final String RESPONSE_ATTR_SERVER = "server";
    private static final String RESPONSE_ATTR_FARM = "farm";
    private static final String RESPONSE_ATTR_TITLE = "title";
    private static final String RESPONSE_ATTR_DATE_TAKEN = "datetaken";

    private static final String RESPONSE_TAG_PERSON = "person";
    private static final String RESPONSE_ATTR_ISPRO = "ispro";
    private static final String RESPONSE_ATTR_ICONSERVER = "iconserver";
    private static final String RESPONSE_ATTR_ICONFARM = "iconfarm";
    private static final String RESPONSE_TAG_USERNAME = "username";
    private static final String RESPONSE_TAG_REALNAME = "realname";
    private static final String RESPONSE_TAG_LOCATION = "location";
    private static final String RESPONSE_ATTR_LATITUDE = "latitude";
    private static final String RESPONSE_ATTR_LONGITUDE = "longitude";
    private static final String RESPONSE_TAG_PHOTOSURL = "photosurl";
    private static final String RESPONSE_TAG_PROFILEURL = "profileurl";
    private static final String RESPONSE_TAG_MOBILEURL = "mobileurl";

    private static final String RESPONSE_TAG_FEED = "feed";
    private static final String RESPONSE_TAG_UPDATED = "updated";

    private static final String PHOTO_IMAGE_URL = "http://farm%s.static.flickr.com/%s/%s_%s%s.jpg";
    private static final String BUDDY_ICON_URL =
            "http://farm%s.static.flickr.com/%s/buddyicons/%s.jpg";
    private static final String DEFAULT_BUDDY_ICON_URL =
            "http://www.flickr.com/images/buddyicon.jpg";

    private static final int IO_BUFFER_SIZE = 4 * 1024;

    private static final boolean FLAG_DECODE_PHOTO_STREAM_WITH_SKIA = false;

    private static final Flickr sInstance = new Flickr();

    private HttpClient mClient;

    /**
     * Defines the size of the image to download from Flickr.
     *
     * @see com.google.android.photostream.Flickr.Photo
     */
    enum PhotoSize {
        /**
         * Small square image (75x75 px).
         */
        SMALL_SQUARE("_s", 75),
        /**
         * Thumbnail image (the longest side measures 100 px).
         */
        THUMBNAIL("_t", 100),
        /**
         * Small image (the longest side measures 240 px).
         */
        SMALL("_m", 240),
        /**
         * Medium image (the longest side measures 500 px).
         */
        MEDIUM("", 500),
        /**
         * Large image (the longest side measures 1024 px).
         */
        LARGE("_b", 1024);

        private final String mSize;
        private final int mLongSide;

        private PhotoSize(String size, int longSide) {
            mSize = size;
            mLongSide = longSide;
        }

        /**
         * Returns the size in pixels of the longest side of the image.
         *
         * @return THe dimension in pixels of the longest side.
         */
        int longSide() {
            return mLongSide;
        }

        /**
         * Returns the name of the size, as defined by Flickr. For instance,
         * the LARGE size is defined by the String "_b".
         *
         * @return
         */
        String size() {
            return mSize;
        }

        @Override
        public String toString() {
            return name() + ", longSide=" + mLongSide;
        }
    }

    /**
     * Represents the geographical location of a photo.
     */
    static class Location {
        private float mLatitude;
        private float mLongitude;

        private Location(float latitude, float longitude) {
            mLatitude = latitude;
            mLongitude = longitude;
        }

        float getLatitude() {
            return mLatitude;
        }

        float getLongitude() {
            return mLongitude;
        }
    }

    /**
     * A Flickr user, in the strictest sense, is only defined by its NSID. The NSID
     * is usually obtained by {@link Flickr#findByUserName(String)
     * looking up a user by its user name}.
     *
     * To obtain more information about a given user, refer to the UserInfo class.
     *
     * @see Flickr#findByUserName(String)
     * @see Flickr#getUserInfo(com.google.android.photostream.Flickr.User) 
     * @see com.google.android.photostream.Flickr.UserInfo
     */
    static class User implements Parcelable {
        private final String mId;

        private User(String id) {
            mId = id;
        }

        private User(Parcel in) {
            mId = in.readString();
        }

        /**
         * Returns the Flickr NSDID of the user. The NSID is used to identify the
         * user with any operation performed on Flickr.
         *
         * @return The user's NSID.
         */
        String getId() {
            return mId;
        }

        /**
         * Creates a new instance of this class from the specified Flickr NSID.
         *
         * @param id The NSID of the Flickr user.
         *
         * @return An instance of User whose id might not be valid.
         */
        static User fromId(String id) {
            return new User(id);
        }

        @Override
        public String toString() {
            return "User[" + mId + "]";
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mId);
        }

        public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
            public User createFromParcel(Parcel in) {
                return new User(in);
            }

            public User[] newArray(int size) {
                return new User[size];
            }
        };
    }

    /**
     * A set of information for a given Flickr user. The information exposed include:
     * - The user's NSDID
     * - The user's name
     * - The user's real name
     * - The user's location
     * - The URL to the user's photos
     * - The URL to the user's profile
     * - The URL to the user's mobile web site
     * - Whether the user has a pro account
     */
    static class UserInfo implements Parcelable {
        private String mId;
        private String mUserName;
        private String mRealName;
        private String mLocation;
        private String mPhotosUrl;
        private String mProfileUrl;
        private String mMobileUrl;
        private boolean mIsPro;
        private String mIconServer;
        private String mIconFarm;

        private UserInfo(String nsid) {
            mId = nsid;
        }

        private UserInfo(Parcel in) {
            mId = in.readString();
            mUserName = in.readString();
            mRealName = in.readString();
            mLocation = in.readString();
            mPhotosUrl = in.readString();
            mProfileUrl = in.readString();
            mMobileUrl = in.readString();
            mIsPro = in.readInt() == 1;
            mIconServer = in.readString();
            mIconFarm = in.readString();
        }

        /**
         * Returns the Flickr NSID that identifies this user.
         *
         * @return The Flickr NSID.
         */
        String getId() {
            return mId;
        }

        /**
         * Returns the user's name. This is the name that the user authenticates with,
         * and the name that Flickr uses in the URLs
         * (for instance, http://flickr.com/photos/romainguy, where romainguy is the user
         * name.)
         *
         * @return The user's Flickr name.
         */
        String getUserName() {
            return mUserName;
        }

        /**
         * Returns the user's real name. The real name is chosen by the user when
         * creating his account and might not reflect his civil name.
         *
         * @return The real name of the user.
         */
        String getRealName() {
            return mRealName;
        }

        /**
         * Returns the user's location, if publicly exposed.
         *
         * @return The location of the user.
         */
        String getLocation() {
            return mLocation;
        }

        /**
         * Returns the URL to the photos of the user. For instance,
         * http://flickr.com/photos/romainguy.
         *
         * @return The URL to the photos of the user.
         */
        String getPhotosUrl() {
            return mPhotosUrl;
        }

        /**
         * Returns the URL to the profile of the user. For instance,
         * http://flickr.com/people/romainguy/.
         *
         * @return The URL to the photos of the user.
         */
        String getProfileUrl() {
            return mProfileUrl;
        }

        /**
         * Returns the mobile URL of the user.
         *
         * @return The mobile URL of the user.
         */
        String getMobileUrl() {
            return mMobileUrl;
        }

        /**
         * Indicates whether the user owns a pro account.
         *
         * @return true, if the user has a pro account, false otherwise.
         */
        boolean isPro() {
            return mIsPro;
        }

        /**
         * Returns the URL to the user's buddy icon. The buddy icon is a 48x48
         * image chosen by the user. If no icon can be found, a default image
         * URL is returned.
         *
         * @return The URL to the user's buddy icon.
         */
        String getBuddyIconUrl() {
            if (mIconFarm == null || mIconServer == null || mId == null) {
                return DEFAULT_BUDDY_ICON_URL;
            }
            return String.format(BUDDY_ICON_URL, mIconFarm, mIconServer, mId);
        }

        /**
         * Loads the user's buddy icon as a Bitmap. The user's buddy icon is loaded
         * from the URL returned by {@link #getBuddyIconUrl()}. The buddy icon is
         * not cached locally.
         *
         * @return A 48x48 bitmap if the icon was loaded successfully or null otherwise.
         */
        Bitmap loadBuddyIcon() {
            Bitmap bitmap = null;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new BufferedInputStream(new URL(getBuddyIconUrl()).openStream(),
                        IO_BUFFER_SIZE);

                if (FLAG_DECODE_PHOTO_STREAM_WITH_SKIA) {
                    bitmap = BitmapFactory.decodeStream(in);
                } else {
                    final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                    out = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
                    copy(in, out);
                    out.flush();

                    final byte[] data = dataStream.toByteArray();
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                }
            } catch (IOException e) {
                android.util.Log.e(Flickr.LOG_TAG, "Could not load buddy icon: " + this, e);
            } finally {
                closeStream(in);
                closeStream(out);
            }

            return bitmap;
        }

        @Override
        public String toString() {
            return mRealName + " (" + mUserName + ", " + mId + ")";
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mId);
            dest.writeString(mUserName);
            dest.writeString(mRealName);
            dest.writeString(mLocation);
            dest.writeString(mPhotosUrl);
            dest.writeString(mProfileUrl);
            dest.writeString(mMobileUrl);
            dest.writeInt(mIsPro ? 1 : 0);
            dest.writeString(mIconServer);
            dest.writeString(mIconFarm);
        }

        public static final Parcelable.Creator<UserInfo> CREATOR =
                new Parcelable.Creator<UserInfo>() {
            public UserInfo createFromParcel(Parcel in) {
                return new UserInfo(in);
            }

            public UserInfo[] newArray(int size) {
                return new UserInfo[size];
            }
        };
    }

    /**
     * A photo is represented by a title, the date at which it was taken and a URL.
     * The URL depends on the desired {@link com.google.android.photostream.Flickr.PhotoSize}.
     */
    static class Photo implements Parcelable {
        private String mId;
        private String mSecret;
        private String mServer;
        private String mFarm;
        private String mTitle;
        private String mDate;

        private Photo() {
        }

        private Photo(Parcel in) {
            mId = in.readString();
            mSecret = in.readString();
            mServer = in.readString();
            mFarm = in.readString();
            mTitle = in.readString();
            mDate = in.readString();
        }

        /**
         * Returns the title of the photo, if specified.
         *
         * @return The title of the photo. The returned value can be empty or null.
         */
        String getTitle() {
            return mTitle;
        }

        /**
         * Returns the date at which the photo was taken, formatted in the current locale
         * with the following pattern: MMMM d, yyyy.
         *
         * @return The title of the photo. The returned value can be empty or null.
         */
        String getDate() {
            return mDate;
        }

        /**
         * Returns the URL to the photo for the specified size.
         *
         * @param photoSize The required size of the photo.
         *
         * @return A URL to the photo for the specified size.
         *
         * @see com.google.android.photostream.Flickr.PhotoSize
         */
        String getUrl(PhotoSize photoSize) {
            return String.format(PHOTO_IMAGE_URL, mFarm, mServer, mId, mSecret, photoSize.size());
        }

        /**
         * Loads a Bitmap representing the photo for the specified size. The Bitmap is loaded
         * from the URL returned by
         * {@link #getUrl(com.google.android.photostream.Flickr.PhotoSize)}. 
         *
         * @param size The size of the photo to load.
         *
         * @return A Bitmap whose longest size is the same as the longest side of the
         *         specified {@link com.google.android.photostream.Flickr.PhotoSize}, or null
         *         if the photo could not be loaded.
         */
        Bitmap loadPhotoBitmap(PhotoSize size) {
            Bitmap bitmap = null;
            InputStream in = null;
            BufferedOutputStream out = null;

            try {
                in = new BufferedInputStream(new URL(getUrl(size)).openStream(),
                        IO_BUFFER_SIZE);

                if (FLAG_DECODE_PHOTO_STREAM_WITH_SKIA) {
                    bitmap = BitmapFactory.decodeStream(in);
                } else {
                    final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                    out = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
                    copy(in, out);
                    out.flush();

                    final byte[] data = dataStream.toByteArray();
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                }
            } catch (IOException e) {
                android.util.Log.e(Flickr.LOG_TAG, "Could not load photo: " + this, e);
            } finally {
                closeStream(in);
                closeStream(out);
            }

            return bitmap;
        }

        @Override
        public String toString() {
            return mTitle + ", " + mDate + " @" + mId;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mId);
            dest.writeString(mSecret);
            dest.writeString(mServer);
            dest.writeString(mFarm);
            dest.writeString(mTitle);
            dest.writeString(mDate);
        }

        public static final Parcelable.Creator<Photo> CREATOR = new Parcelable.Creator<Photo>() {
            public Photo createFromParcel(Parcel in) {
                return new Photo(in);
            }

            public Photo[] newArray(int size) {
                return new Photo[size];
            }
        };
    }

    /**
     * A list of {@link com.google.android.photostream.Flickr.Photo photos}. A list
     * represents a series of photo on a page from the user's photostream, a list is
     * therefore associated with a page index and a page count. The page index and the
     * page count both depend on the number of photos per page.
     */
    static class PhotoList {
        private ArrayList<Photo> mPhotos;
        private int mPage;
        private int mPageCount;

        private void add(Photo photo) {
            mPhotos.add(photo);
        }

        /**
         * Returns the photo at the specified index in the current set. An
         * {@link ArrayIndexOutOfBoundsException} can be thrown if the index is
         * less than 0 or greater then or equals to {@link #getCount()}.
         *
         * @param index The index of the photo to retrieve from the list.
         *
         * @return A valid {@link com.google.android.photostream.Flickr.Photo}.
         */
        public Photo get(int index) {
            return mPhotos.get(index);
        }

        /**
         * Returns the number of photos in the list.
         *
         * @return A positive integer, or 0 if the list is empty.
         */
        public int getCount() {
            return mPhotos.size();
        }

        /**
         * Returns the page index of the photos from this list.
         *
         * @return The index of the Flickr page that contains the photos of this list.
         */
        public int getPage() {
            return mPage;
        }

        /**
         * Returns the total number of photo pages.
         *
         * @return A positive integer, or 0 if the photostream is empty.
         */
        public int getPageCount() {
            return mPageCount;
        }
    }

    /**
     * Returns the unique instance of this class.
     *
     * @return The unique instance of this class.
     */
    static Flickr get() {
        return sInstance;
    }    

    private Flickr() {
        final HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");

        final SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

        final ThreadSafeClientConnManager manager =
                new ThreadSafeClientConnManager(params, registry);

        mClient = new DefaultHttpClient(manager, params);
    }

    /**
     * Finds a user by its user name. This method will return an instance of
     * {@link com.google.android.photostream.Flickr.User} containing the user's
     * NSID, or null if the user could not be found.
     *
     * The returned User contains only the user's NSID. To retrieve more information
     * about the user, please refer to
     * {@link #getUserInfo(com.google.android.photostream.Flickr.User)}
     *
     * @param userName The name of the user to find.
     *
     * @return A User instance with a valid NSID, or null if the user cannot be found.
     *
     * @see #getUserInfo(com.google.android.photostream.Flickr.User)
     * @see com.google.android.photostream.Flickr.User
     * @see com.google.android.photostream.Flickr.UserInfo
     */
    User findByUserName(String userName) {
        final Uri.Builder uri = buildGetMethod(API_PEOPLE_FIND_BY_USERNAME);
        uri.appendQueryParameter(PARAM_USERNAME, userName);

        final HttpGet get = new HttpGet(uri.build().toString());
        final String[] userId = new String[1];

        try {
            executeRequest(get, new ResponseHandler() {
                public void handleResponse(InputStream in) throws IOException {
                    parseResponse(in, new ResponseParser() {
                        public void parseResponse(XmlPullParser parser)
                                throws XmlPullParserException, IOException {
                            parseUser(parser, userId);
                        }
                    });
                }
            });

            if (userId[0] != null) {
                return new User(userId[0]);
            }
        } catch (IOException e) {
            android.util.Log.e(LOG_TAG, "Could not find the user with name: " + userName);
        }

        return null;
    }

    /**
     * Retrieves a public set of information about the specified user. The user can
     * either be {@link com.google.android.photostream.Flickr.User#fromId(String) created manually}
     * or {@link #findByUserName(String) obtained from a user name}.
     *
     * @param user The user, whose NSID is valid, to retrive public information for.
     *
     * @return An instance of {@link com.google.android.photostream.Flickr.UserInfo} or null
     *         if the user could not be found.
     *
     * @see com.google.android.photostream.Flickr.UserInfo
     * @see com.google.android.photostream.Flickr.User
     * @see #findByUserName(String) 
     */
    UserInfo getUserInfo(User user) {
        final String nsid = user.getId();
        final Uri.Builder uri = buildGetMethod(API_PEOPLE_GET_INFO);
        uri.appendQueryParameter(PARAM_USERID, nsid);

        final HttpGet get = new HttpGet(uri.build().toString());

        try {
            final UserInfo info = new UserInfo(nsid);

            executeRequest(get, new ResponseHandler() {
                public void handleResponse(InputStream in) throws IOException {
                    parseResponse(in, new ResponseParser() {
                        public void parseResponse(XmlPullParser parser)
                                throws XmlPullParserException, IOException {
                            parseUserInfo(parser, info);
                        }
                    });
                }
            });

            return info;
        } catch (IOException e) {
            android.util.Log.e(LOG_TAG, "Could not find the user with id: " + nsid);
        }

        return null;
    }

    /**
     * Retrives a list of photos for the specified user. The list contains at most the
     * number of photos specified by <code>perPage</code>. The photos are retrieved
     * starting a the specified page index. For instance, if a user has 10 photos in
     * his photostream, calling getPublicPhotos(user, 5, 2) will return the last 5 photos
     * of the photo stream.
     *
     * The page index starts at 1, not 0.
     *
     * @param user The user to retrieve photos from.
     * @param perPage The maximum number of photos to retrieve.
     * @param page The index (starting at 1) of the page in the photostream.
     *
     * @return A list of at most perPage photos.
     *
     * @see com.google.android.photostream.Flickr.Photo
     * @see com.google.android.photostream.Flickr.PhotoList
     * @see #downloadPhoto(com.google.android.photostream.Flickr.Photo,
     *          com.google.android.photostream.Flickr.PhotoSize, java.io.OutputStream) 
     */
    PhotoList getPublicPhotos(User user, int perPage, int page) {
        final Uri.Builder uri = buildGetMethod(API_PEOPLE_GET_PUBLIC_PHOTOS);
        uri.appendQueryParameter(PARAM_USERID, user.getId());
        uri.appendQueryParameter(PARAM_PER_PAGE, String.valueOf(perPage));
        uri.appendQueryParameter(PARAM_PAGE, String.valueOf(page));
        uri.appendQueryParameter(PARAM_EXTRAS, VALUE_DEFAULT_EXTRAS);

        final HttpGet get = new HttpGet(uri.build().toString());
        final PhotoList photos = new PhotoList();

        try {
            executeRequest(get, new ResponseHandler() {
                public void handleResponse(InputStream in) throws IOException {
                    parseResponse(in, new ResponseParser() {
                        public void parseResponse(XmlPullParser parser)
                                throws XmlPullParserException, IOException {
                            parsePhotos(parser, photos);
                        }
                    });
                }
            });
        } catch (IOException e) {
            android.util.Log.e(LOG_TAG, "Could not find photos for user: " + user);
        }

        return photos;
    }

    /**
     * Retrieves the geographical location of the specified photo. If the photo
     * has no geodata associated with it, this method returns null.
     *
     * @param photo The photo to get the location of.
     *
     * @return The geo location of the photo, or null if the photo has no geodata
     *         or the photo cannot be found.
     *
     * @see com.google.android.photostream.Flickr.Location
     */
    Location getLocation(Flickr.Photo photo) {
        final Uri.Builder uri = buildGetMethod(API_PEOPLE_GET_LOCATION);
        uri.appendQueryParameter(PARAM_PHOTO_ID, photo.mId);

        final HttpGet get = new HttpGet(uri.build().toString());
        final Location location = new Location(0.0f, 0.0f);

        try {
            executeRequest(get, new ResponseHandler() {
                public void handleResponse(InputStream in) throws IOException {
                    parseResponse(in, new ResponseParser() {
                        public void parseResponse(XmlPullParser parser)
                                throws XmlPullParserException, IOException {
                            parsePhotoLocation(parser, location);
                        }
                    });
                }
            });
            return location;
        } catch (IOException e) {
            android.util.Log.e(LOG_TAG, "Could not find location for photo: " + photo);
        }

        return null;
    }

    /**
     * Checks the specified user's feed to see if any updated occured after the
     * specified date.
     *
     * @param user The user whose feed must be checked.
     * @param reference The date after which to check for updates.
     *
     * @return True if any update occured after the reference date, false otherwise.
     */
    boolean hasUpdates(User user, final Calendar reference) {
        final Uri.Builder uri = new Uri.Builder();
        uri.path(API_FEED_URL);
        uri.appendQueryParameter(PARAM_FEED_ID, user.getId());
        uri.appendQueryParameter(PARAM_FEED_FORMAT, VALUE_DEFAULT_FORMAT);

        final HttpGet get = new HttpGet(uri.build().toString());
        final boolean[] updated = new boolean[1];

        try {
            executeRequest(get, new ResponseHandler() {
                public void handleResponse(InputStream in) throws IOException {
                    parseFeedResponse(in, new ResponseParser() {
                        public void parseResponse(XmlPullParser parser)
                                throws XmlPullParserException, IOException {
                            updated[0] = parseUpdated(parser, reference);
                        }
                    });
                }
            });
        } catch (IOException e) {
            android.util.Log.e(LOG_TAG, "Could not find feed for user: " + user);
        }

        return updated[0];
    }

    /**
     * Downloads the specified photo at the specified size in the specified destination.
     *
     * @param photo The photo to download.
     * @param size The size of the photo to download.
     * @param destination The output stream in which to write the downloaded photo.
     *
     * @throws IOException If any network exception occurs during the download.
     */
    void downloadPhoto(Photo photo, PhotoSize size, OutputStream destination) throws IOException {
        final BufferedOutputStream out = new BufferedOutputStream(destination, IO_BUFFER_SIZE);
        final String url = photo.getUrl(size);
        final HttpGet get = new HttpGet(url);

        HttpEntity entity = null;
        try {
            final HttpResponse response = mClient.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                entity = response.getEntity();
                entity.writeTo(out);
                out.flush();
            }
        } finally {
            if (entity != null) {
                entity.consumeContent();
            }
        }
    }

    private boolean parseUpdated(XmlPullParser parser, Calendar reference) throws IOException,
            XmlPullParserException {

        int type;
        String name;
        final int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            name = parser.getName();
            if (RESPONSE_TAG_UPDATED.equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        final String text = parser.getText().replace('T', ' ').replace('Z', ' ');
                        final Calendar calendar = new GregorianCalendar();
                        calendar.setTimeInMillis(format.parse(text).getTime());

                        return calendar.after(reference);
                    } catch (ParseException e) {
                        // Ignore
                    }
                }
            }
        }

        return false;
    }    

    private void parsePhotos(XmlPullParser parser, PhotoList photos)
            throws XmlPullParserException, IOException {
        int type;
        String name;
        SimpleDateFormat parseFormat = null;
        SimpleDateFormat outputFormat = null;

        final int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            name = parser.getName();
            if (RESPONSE_TAG_PHOTOS.equals(name)) {
                photos.mPage = Integer.parseInt(parser.getAttributeValue(null, RESPONSE_ATTR_PAGE));
                photos.mPageCount = Integer.parseInt(parser.getAttributeValue(null,
                        RESPONSE_ATTR_PAGES));
                photos.mPhotos = new ArrayList<Photo>();
            } else if (RESPONSE_TAG_PHOTO.equals(name)) {
                final Photo photo = new Photo();
                photo.mId = parser.getAttributeValue(null, RESPONSE_ATTR_ID);
                photo.mSecret = parser.getAttributeValue(null, RESPONSE_ATTR_SECRET);
                photo.mServer = parser.getAttributeValue(null, RESPONSE_ATTR_SERVER);
                photo.mFarm = parser.getAttributeValue(null, RESPONSE_ATTR_FARM);
                photo.mTitle = parser.getAttributeValue(null, RESPONSE_ATTR_TITLE);
                photo.mDate = parser.getAttributeValue(null, RESPONSE_ATTR_DATE_TAKEN);

                if (parseFormat == null) {
                    parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    outputFormat = new SimpleDateFormat("MMMM d, yyyy");
                }

                try {
                    photo.mDate = outputFormat.format(parseFormat.parse(photo.mDate));
                } catch (ParseException e) {
                    android.util.Log.w(LOG_TAG, "Could not parse photo date", e);
                }

                photos.add(photo);
            }
        }
    }

    private void parsePhotoLocation(XmlPullParser parser, Location location)
            throws XmlPullParserException, IOException {
        int type;
        String name;
        final int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            name = parser.getName();
            if (RESPONSE_TAG_LOCATION.equals(name)) {
                try {
                    location.mLatitude = Float.parseFloat(parser.getAttributeValue(null,
                            RESPONSE_ATTR_LATITUDE));
                    location.mLongitude = Float.parseFloat(parser.getAttributeValue(null,
                            RESPONSE_ATTR_LONGITUDE));
                } catch (NumberFormatException e) {
                    throw new XmlPullParserException("Could not parse lat/lon", parser, e);
                }
            }
        }
    }

    private void parseUser(XmlPullParser parser, String[] userId)
            throws XmlPullParserException, IOException {
        int type;
        String name;
        final int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            name = parser.getName();
            if (RESPONSE_TAG_USER.equals(name)) {
                userId[0] = parser.getAttributeValue(null, RESPONSE_ATTR_NSID);
            }
        }
    }

    private void parseUserInfo(XmlPullParser parser, UserInfo info)
            throws XmlPullParserException, IOException {
        int type;
        String name;
        final int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            name = parser.getName();
            if (RESPONSE_TAG_PERSON.equals(name)) {
                info.mIsPro = "1".equals(parser.getAttributeValue(null, RESPONSE_ATTR_ISPRO));
                info.mIconServer = parser.getAttributeValue(null, RESPONSE_ATTR_ICONSERVER);
                info.mIconFarm = parser.getAttributeValue(null, RESPONSE_ATTR_ICONFARM);
            } else if (RESPONSE_TAG_USERNAME.equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    info.mUserName = parser.getText();
                }
            } else if (RESPONSE_TAG_REALNAME.equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    info.mRealName = parser.getText();
                }
            } else if (RESPONSE_TAG_LOCATION.equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    info.mLocation = parser.getText();
                }
            } else if (RESPONSE_TAG_PHOTOSURL.equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    info.mPhotosUrl = parser.getText();
                }
            } else if (RESPONSE_TAG_PROFILEURL.equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    info.mProfileUrl = parser.getText();
                }
            } else if (RESPONSE_TAG_MOBILEURL.equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    info.mMobileUrl = parser.getText();
                }
            }
        }
    }

    /**
     * Parses a valid Flickr XML response from the specified input stream. When the Flickr
     * response contains the OK tag, the response is sent to the specified response parser.
     *
     * @param in The input stream containing the response sent by Flickr.
     * @param responseParser The parser to use when the response is valid.
     * 
     * @throws IOException
     */
    private void parseResponse(InputStream in, ResponseParser responseParser) throws IOException {
        final XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(new InputStreamReader(in));

            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG &&
                    type != XmlPullParser.END_DOCUMENT) {
                // Empty
            }

            if (type != XmlPullParser.START_TAG) {
                throw new InflateException(parser.getPositionDescription()
                        + ": No start tag found!");
            }

            String name = parser.getName();
            if (RESPONSE_TAG_RSP.equals(name)) {
                final String value = parser.getAttributeValue(null, RESPONSE_ATTR_STAT);
                if (!RESPONSE_STATUS_OK.equals(value)) {
                    throw new IOException("Wrong status: " + value);
                }
            }

            responseParser.parseResponse(parser);

        } catch (XmlPullParserException e) {
            final IOException ioe = new IOException("Could not parser the response");
            ioe.initCause(e);
            throw ioe;
        }
    }

    /**
     * Parses a valid Flickr Atom feed response from the specified input stream.
     *
     * @param in The input stream containing the response sent by Flickr.
     * @param responseParser The parser to use when the response is valid.
     *
     * @throws IOException
     */
    private void parseFeedResponse(InputStream in, ResponseParser responseParser)
            throws IOException {

        final XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(new InputStreamReader(in));

            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG &&
                    type != XmlPullParser.END_DOCUMENT) {
                // Empty
            }

            if (type != XmlPullParser.START_TAG) {
                throw new InflateException(parser.getPositionDescription()
                        + ": No start tag found!");
            }

            String name = parser.getName();
            if (RESPONSE_TAG_FEED.equals(name)) {
                responseParser.parseResponse(parser);
            } else {
                throw new IOException("Wrong start tag: " + name);                
            }

        } catch (XmlPullParserException e) {
            final IOException ioe = new IOException("Could not parser the response");
            ioe.initCause(e);
            throw ioe;
        }
    }

    /**
     * Executes an HTTP request on Flickr's web service. If the response is ok, the content
     * is sent to the specified response handler.
     *
     * @param get The GET request to executed.
     * @param handler The handler which will parse the response.
     * 
     * @throws IOException
     */
    private void executeRequest(HttpGet get, ResponseHandler handler) throws IOException {
        HttpEntity entity = null;
        HttpHost host = new HttpHost(API_REST_HOST, 80, "http");
        try {
            final HttpResponse response = mClient.execute(host, get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                entity = response.getEntity();
                final InputStream in = entity.getContent();
                handler.handleResponse(in);
            }
        } finally {
            if (entity != null) {
                entity.consumeContent();
            }
        }
    }

    /**
     * Builds an HTTP GET request for the specified Flickr API method. The returned request
     * contains the web service path, the query parameter for the API KEY and the query
     * parameter for the specified method.
     *
     * @param method The Flickr API method to invoke.
     *
     * @return A Uri.Builder containing the GET path, the API key and the method already
     *         encoded.
     */
    private static Uri.Builder buildGetMethod(String method) {
        final Uri.Builder builder = new Uri.Builder();
        builder.path(API_REST_URL).appendQueryParameter(PARAM_API_KEY, API_KEY);
        builder.appendQueryParameter(PARAM_METHOD, method);
        return builder;
    }

    /**
     * Copy the content of the input stream into the output stream, using a temporary
     * byte array buffer whose size is defined by {@link #IO_BUFFER_SIZE}.
     *
     * @param in The input stream to copy from.
     * @param out The output stream to copy to.
     *
     * @throws IOException If any error occurs during the copy.
     */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                android.util.Log.e(Flickr.LOG_TAG, "Could not close stream", e);
            }
        }
    }

    /**
     * Response handler used with
     * {@link Flickr#executeRequest(org.apache.http.client.methods.HttpGet,
     * com.google.android.photostream.Flickr.ResponseHandler)}. The handler is invoked when
     * a response is sent by the server. The response is made available as an input stream. 
     */
    private static interface ResponseHandler {
        /**
         * Processes the responses sent by the HTTP server following a GET request.
         *
         * @param in The stream containing the server's response.
         *
         * @throws IOException
         */
        public void handleResponse(InputStream in) throws IOException;
    }

    /**
     * Response parser used with {@link Flickr#parseResponse(java.io.InputStream,
     * com.google.android.photostream.Flickr.ResponseParser)}. When Flickr returns a valid
     * response, this parser is invoked to process the XML response.
     */
    private static interface ResponseParser {
        /**
         * Processes the XML response sent by the Flickr web service after a successful
         * request.
         *
         * @param parser The parser containing the XML responses.
         *
         * @throws XmlPullParserException
         * @throws IOException
         */
        public void parseResponse(XmlPullParser parser) throws XmlPullParserException, IOException;
    }
}
