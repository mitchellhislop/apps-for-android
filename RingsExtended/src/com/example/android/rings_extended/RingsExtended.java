package com.example.android.rings_extended;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The RingsExtended application, implementing an advanced ringtone picker.
 * This is a ListActivity display an adapter of dynamic state built by the
 * activity: at the top are simple options the user can be picked, next an
 * item to run the built-in ringtone picker, and next are any other activities
 * that can supply music Uris.
 */
public class RingsExtended extends ListActivity
        implements View.OnClickListener, MediaPlayer.OnCompletionListener {
    static final boolean DBG = false;
    static final String TAG = "RingsExtended";
    
    /**
     * Request code when we are launching an activity to handle our same
     * original Intent, meaning we can propagate its result back to our caller
     * as-is.
     */
    static final int REQUEST_ORIGINAL = 2;
    
    /**
     * Request code when launching an activity that returns an audio Uri,
     * meaning we need to translate its result into one that our caller
     * expects.
     */
    static final int REQUEST_SOUND = 1;
    
    Adapter mAdapter;
    
    private View mOkayButton;
    private View mCancelButton;
    
    /** Where the silent option item is in the list, or -1 if there is none. */
    private int mSilentItemIdx = -1;
    
    /** The Uri to play when the 'Default' item is clicked. */
    private Uri mUriForDefaultItem;
    
    /** Where the default option item is in the list, or -1 if there is none. */
    private int mDefaultItemIdx = -1;
    
    /** The Uri to place a checkmark next to. */
    private Uri mExistingUri;
    
    /** Where the existing option item is in the list. */
    private int mExistingItemIdx;
    
    /** Currently selected options in the radio buttons, if any. */
    private long mSelectedItem = -1;
    
    /** Loaded ringtone for the existing URI. */
    private Ringtone mExistingRingtone;
    
    /** Id of option that is currently playing. */
    private long mPlayingId = -1;
    
    /** Used for playing previews of ring tones. */
    private MediaPlayer mMediaPlayer;
    
    /**
     * Information about one static item in the list.  This is used for items
     * that are added and handled manually, which don't have an Intent
     * associated with them.
     */
    final static class ItemInfo {
        final CharSequence name;
        final CharSequence subtitle;
        final Drawable icon;
        
        ItemInfo(CharSequence _name, CharSequence _subtitle, Drawable _icon) {
            name = _name;
            subtitle = _subtitle;
            icon = _icon;
        }
    }
    
    /**
     * Our special adapter implementation, merging the various kinds of items
     * that we will display into one list.  There are two sections to the
     * list of items:
     * (1) First are any fixed items as described by ItemInfo objects.
     * (2) Next are any activities that do the same thing as our own.
     * (3) Finally are any activities that can execute a different Intent.
     */
    private final class Adapter extends BaseAdapter {
        private final List<ItemInfo> mInitialItems;
        private final Intent mIntent;
        private final Intent mOrigIntent;
        private final LayoutInflater mInflater;

        private List<ResolveInfo> mList;
        private int mRealListStart = 0;
        
        class ViewHolder {
            ImageView icon;
            RadioButton radio;
            TextView textSingle;
            TextView textDouble1;
            TextView textDouble2;
            ImageView more;
        }
        
        /**
         * Create a new adapter with the items to be displayed.
         * 
         * @param context The Context we are running in.
         * @param initialItems A fixed set of items that appear at the
         * top of the list.
         * @param origIntent The original Intent that was used to launch this
         * activity, used to find all activities that can do the same thing.
         * @param excludeOrigIntent Our component name, to exclude from the
         * origIntent list since that is what the user is already running!
         * @param intent An Intent used to query for additional items to
         * appear in the rest of the list.
         */
        public Adapter(Context context, List<ItemInfo> initialItems,
                Intent origIntent, ComponentName excludeOrigIntent, Intent intent) {
            mInitialItems = initialItems;
            mIntent = new Intent(intent);
            mIntent.setComponent(null);
            mIntent.setFlags(0);
            if (origIntent != null) {
                mOrigIntent = new Intent(origIntent);
                mOrigIntent.setComponent(null);
                mOrigIntent.setFlags(0);
            } else {
                mOrigIntent = null;
            }
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            mList = getActivities(context, mIntent, null);
            
            if (origIntent != null) {
                List<ResolveInfo> orig = getActivities(context, mOrigIntent,
                        excludeOrigIntent);
                if (orig != null && orig.size() > 0) {
                    mRealListStart = orig.size();
                    orig.addAll(mList);
                    mList = orig;
                }
            }
        }

        /**
         * If the position is within the range of initial items, return the
         * corresponding index into that array.  Otherwise return -1.
         */
        public int initialItemForPosition(int position) {
            if (position >= getIntentStartIndex()) {
                return -1;
            }
            return position;
        }
        
        /**
         * Returns true if the given position is for one of the
         * "original intent" items.
         */
        public boolean isOrigIntentPosition(int position) {
            position -= getIntentStartIndex();
            return position >= 0 && position < mRealListStart;
        }
        
        /**
         * Returns the ResolveInfo corresponding to the given position, or null
         * if that position is not an Intent item (that is if it is one
         * of the static list items).
         */
        public ResolveInfo resolveInfoForPosition(int position) {
            position -= getIntentStartIndex();
            if (mList == null || position < 0) {
                return null;
            }

            return mList.get(position);
        }

        /**
         * Returns the Intent corresponding to the given position, or null
         * if that position is not an Intent item (that is if it is one
         * of the static list items).
         */
        public Intent intentForPosition(int position) {
            position -= getIntentStartIndex();
            if (mList == null || position < 0) {
                return null;
            }

            Intent intent = new Intent(
                    position >= mRealListStart ? mIntent : mOrigIntent);
            ActivityInfo ai = mList.get(position).activityInfo;
            intent.setComponent(new ComponentName(
                    ai.applicationInfo.packageName, ai.name));
            return intent;
        }

        public int getCount() {
            return getIntentStartIndex() + (mList != null ? mList.size() : 0);
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.list_item, parent, false);
                ViewHolder vh = new ViewHolder();
                vh.icon = (ImageView)view.findViewById(R.id.icon);
                vh.radio = (RadioButton)view.findViewById(R.id.radio);
                vh.textSingle = (TextView)view.findViewById(R.id.textSingle);
                vh.textDouble1 = (TextView)view.findViewById(R.id.textDouble1);
                vh.textDouble2 = (TextView)view.findViewById(R.id.textDouble2);
                vh.more = (ImageView)view.findViewById(R.id.more);
                view.setTag(vh);
            } else {
                view = convertView;
            }
            int intentStart = getIntentStartIndex();
            if (position < intentStart) {
                bindView(view, position, mInitialItems.get(position));
            } else {
                bindView(view, mList.get(position-intentStart));
            }
            return view;
        }

        private final int getIntentStartIndex() {
            return mInitialItems != null ? mInitialItems.size() : 0;
        }
        
        private final void bindView(View view, ResolveInfo info) {
            PackageManager pm = getPackageManager();

            ViewHolder vh = (ViewHolder)view.getTag();
            
            CharSequence label = info.loadLabel(pm);
            if (label == null) label = info.activityInfo.name;
            bindTextViews(vh, label, null);
            vh.icon.setImageDrawable(info.loadIcon(pm));
            vh.icon.setVisibility(View.VISIBLE);
            vh.radio.setVisibility(View.GONE);
            vh.more.setImageResource(R.drawable.icon_more);
            vh.more.setVisibility(View.VISIBLE);
        }
        
        private final void bindTextViews(ViewHolder vh, CharSequence txt1,
                CharSequence txt2) {
            if (txt2 == null) {
                vh.textSingle.setText(txt1);
                vh.textSingle.setVisibility(View.VISIBLE);
                vh.textDouble1.setVisibility(View.INVISIBLE);
                vh.textDouble2.setVisibility(View.INVISIBLE);
            } else {
                vh.textDouble1.setText(txt1);
                vh.textDouble1.setVisibility(View.VISIBLE);
                vh.textDouble2.setText(txt2);
                vh.textDouble2.setVisibility(View.VISIBLE);
                vh.textSingle.setVisibility(View.INVISIBLE);
            }
        }
        
        private final void bindView(View view, int position, ItemInfo inf) {
            ViewHolder vh = (ViewHolder)view.getTag();
            bindTextViews(vh, inf.name, inf.subtitle);
            
            // Set the standard icon and radio button.  When the radio button
            // is displayed, we mark it if this is the currently selected row,
            // meaning we need to invalidate the view list whenever the
            // selection changes.
            if (inf.icon != null) {
                vh.icon.setImageDrawable(inf.icon);
                vh.icon.setVisibility(View.VISIBLE);
                vh.radio.setVisibility(View.GONE);
            } else {
                vh.icon.setVisibility(View.GONE);
                vh.radio.setVisibility(View.VISIBLE);
                vh.radio.setChecked(position == mSelectedItem);
            }
            
            // Show the "now playing" icon if this item is playing.  Doing this
            // means that we need to invalidate the displayed views when the
            // playing state changes.
            if (mPlayingId == position) {
                vh.more.setImageResource(R.drawable.now_playing);
                vh.more.setVisibility(View.VISIBLE);
            } else {
                vh.more.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Retrieve a list of all of the activities that can handle the given Intent,
     * optionally excluding the explicit component 'exclude'.  The returned list
     * is sorted by the label for reach resolved activity.
     */
    static final List<ResolveInfo> getActivities(Context context, Intent intent,
            ComponentName exclude) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (list != null) {
            int N = list.size();
            if (exclude != null) {
                for (int i=0; i<N; i++) {
                    ResolveInfo ri = list.get(i);
                    if (ri.activityInfo.packageName.equals(exclude.getPackageName())
                            || ri.activityInfo.name.equals(exclude.getClassName())) {
                        list.remove(i);
                        N--;
                    }
                }
            }
            if (N > 1) {
                // Only display the first matches that are either of equal
                // priority or have asked to be default options.
                ResolveInfo r0 = list.get(0);
                for (int i=1; i<N; i++) {
                    ResolveInfo ri = list.get(i);
                    if (Config.LOGV) Log.v(
                        "ResolveListActivity",
                        r0.activityInfo.name + "=" +
                        r0.priority + "/" + r0.isDefault + " vs " +
                        ri.activityInfo.name + "=" +
                        ri.priority + "/" + ri.isDefault);
                    if (r0.priority != ri.priority ||
                        r0.isDefault != ri.isDefault) {
                        while (i < N) {
                            list.remove(i);
                            N--;
                        }
                    }
                }
                Collections.sort(list, new ResolveInfo.DisplayNameComparator(pm));
            }
        }
        
        return list;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.rings_extended);
        
        mOkayButton = findViewById(R.id.okayButton);
        mOkayButton.setOnClickListener(this);
        mCancelButton = findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(this);
        
        Intent intent = getIntent();

        /*
         * Get whether to show the 'Default' item, and the URI to play when the
         * default is clicked
         */
        mUriForDefaultItem = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI);
        if (mUriForDefaultItem == null) {
            mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
        }
        
        // Get the URI whose list item should have a checkmark
        mExistingUri = intent
                .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
        
        // We are now going to build the set of static items.
        ArrayList<ItemInfo> initialItems = new ArrayList<ItemInfo>();
        
        // If the caller has asked to allow the user to select "silent", then
        // show an option for that.
        if (intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)) {
            mSilentItemIdx = initialItems.size();
            initialItems.add(new ItemInfo(getText(R.string.silentLabel),
                    null, null));
        }
        
        // If the caller has asked to allow the user to select "default", then
        // show an option for that.
        if (intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)) {
            mDefaultItemIdx = initialItems.size();
            Ringtone defRing = RingtoneManager.getRingtone(this, mUriForDefaultItem);
            initialItems.add(new ItemInfo(getText(R.string.defaultRingtoneLabel),
                    defRing.getTitle(this), null));
        }
        
        // If the caller has supplied a currently selected Uri, then show an
        // open for keeping that.
        if (mExistingUri != null) {
            mExistingRingtone = RingtoneManager.getRingtone(this, mExistingUri);
            mExistingItemIdx = initialItems.size();
            initialItems.add(new ItemInfo(getText(R.string.existingRingtoneLabel),
                    mExistingRingtone.getTitle(this), null));
        }
        
        if (DBG) {
            Log.v(TAG, "default=" + mUriForDefaultItem);
            Log.v(TAG, "existing=" + mExistingUri);
        }
        
        // Figure out which of the static items should start out with its
        // radio button checked.
        if (mExistingUri == null) {
            if (mSilentItemIdx >= 0) {
                mSelectedItem = mSilentItemIdx;
            }
        } else if (mDefaultItemIdx >= 0 && mExistingUri.equals(mUriForDefaultItem)) {
            mSelectedItem = mDefaultItemIdx;
        } else {
            mSelectedItem = mExistingItemIdx;
        }
        
        if (mSelectedItem >= 0) {
            mOkayButton.setEnabled(true);
        }
        
        mAdapter = new Adapter(this, initialItems, getIntent(), getComponentName(),
                new Intent(Intent.ACTION_GET_CONTENT).setType("audio/mp3")
                .addCategory(Intent.CATEGORY_OPENABLE));
        this.setListAdapter(mAdapter);
    }

    @Override public void onPause() {
        super.onPause();
        stopMediaPlayer();
    }
    
    public void onCompletion(MediaPlayer mp) {
        if (mMediaPlayer == mp) {
            mp.stop();
            mp.release();
            mMediaPlayer = null;
            mPlayingId = -1;
            getListView().invalidateViews();
        }
    }
    
    private void stopMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mPlayingId = -1;
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        int initialItem = mAdapter.initialItemForPosition((int)id);
        if (initialItem >= 0) {
            // If the selected item is from our static list, then take
            // care of handling it.
            mSelectedItem = initialItem;
            Uri uri = getSelectedUri();
            
            // If a new item has been selected, then play it for the user.
            if (uri != null && (id != mPlayingId || mMediaPlayer == null)) {
                stopMediaPlayer();
                mMediaPlayer = new MediaPlayer();
                try {
                    if (DBG) Log.v(TAG, "Playing: " + uri);
                    mMediaPlayer.setDataSource(this, uri);
                    mMediaPlayer.setOnCompletionListener(this);
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                    mPlayingId = id;
                    getListView().invalidateViews();
                } catch (IOException e) {
                    Log.w("MusicPicker", "Unable to play track", e);
                }
                
            // Otherwise stop any currently playing item.
            } else if (mMediaPlayer != null) {
                stopMediaPlayer();
                getListView().invalidateViews();
            }
            getListView().invalidateViews();
            mOkayButton.setEnabled(true);
            
        } else if (mAdapter.isOrigIntentPosition((int)id)) {
            // If the item is one of the original intent activities, then
            // launch it with the result code to simply propagate its result
            // back to our caller.
            Intent intent = mAdapter.intentForPosition((int)id);
            startActivityForResult(intent, REQUEST_ORIGINAL);
            
        } else {
            // If the item is one of the music retrieval activities, then launch
            // it with the result code to transform its result into our caller's
            // expected result.
            Intent intent = mAdapter.intentForPosition((int)id);
            intent.putExtras(getIntent());
            startActivityForResult(intent, REQUEST_SOUND);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SOUND && resultCode == RESULT_OK) {
            Intent resultIntent = new Intent();
            Uri uri = data != null ? data.getData() : null;
            resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else if (requestCode == REQUEST_ORIGINAL && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
    }
    
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.okayButton:
                Intent resultIntent = new Intent();
                Uri uri = getSelectedUri();
                resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
                setResult(RESULT_OK, resultIntent);
                finish();
                break;

            case R.id.cancelButton:
                finish();
                break;
        }
    }
    
    private Uri getSelectedUri() {
        if (mSelectedItem == mSilentItemIdx) {
            // The null uri is silent.
            return null;
        } else if (mSelectedItem == mDefaultItemIdx) {
            return mUriForDefaultItem;
        } else if (mSelectedItem == mExistingItemIdx) {
            return mExistingUri;
        }
        return null;
    }
    
}
