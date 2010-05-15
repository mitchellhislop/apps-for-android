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

package com.beust.android.translate;

import com.beust.android.translate.Languages.Language;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

/**
 * Main activity for the Translate application.
 *
 * @author Cedric Beust
 * @author Daniel Rall
 */
public class TranslateActivity extends Activity implements OnClickListener {
    static final String TAG = "Translate";
    private EditText mToEditText;
    private EditText mFromEditText;
    private Button mFromButton;
    private Button mToButton;
    private Button mTranslateButton;
    private Button mSwapButton;
    private Handler mHandler = new Handler();
    private ProgressBar mProgressBar;
    private TextView mStatusView;
    
    // true if changing a language should automatically trigger a translation
    private boolean mDoTranslate = true;

    // Dialog id's
    private static final int LANGUAGE_DIALOG_ID = 1;
    private static final int ABOUT_DIALOG_ID = 2;

    // Saved preferences
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    
    // Default language pair if no saved preferences are found
    private static final String DEFAULT_FROM = Language.ENGLISH.getShortName();
    private static final String DEFAULT_TO = Language.GERMAN.getShortName();

    private Button mLatestButton;

    private OnClickListener mClickListener = new OnClickListener() {
        public void onClick(View v) {
            mLatestButton = (Button) v;
            showDialog(LANGUAGE_DIALOG_ID);
        }
    };

    // Translation service handle.
    private ITranslate mTranslateService;

    // ServiceConnection implementation for translation.
    private ServiceConnection mTranslateConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTranslateService = ITranslate.Stub.asInterface(service);
            /* TODO(dlr): Register a callback to assure we don't lose our svc.
            try {
                mTranslateervice.registerCallback(mTranslateCallback);
            } catch (RemoteException e) {
                log("Failed to establish Translate service connection: " + e);
                return;
            }
            */
            if (mTranslateService != null) {
                mTranslateButton.setEnabled(true);
            } else {
                mTranslateButton.setEnabled(false);
                mStatusView.setText(getString(R.string.error));
                log("Unable to acquire TranslateService");
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mTranslateButton.setEnabled(false);
            mTranslateService = null;
        }
    };

    // Dictionary
    private static byte[] mWordBuffer;
    private static int mWordCount;
    private static ArrayList<Integer> mWordIndices;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.translate_activity);
        mFromEditText = (EditText) findViewById(R.id.input);
        mToEditText = (EditText) findViewById(R.id.translation);
        mFromButton = (Button) findViewById(R.id.from);
        mToButton = (Button) findViewById(R.id.to);
        mTranslateButton = (Button) findViewById(R.id.button_translate);
        mSwapButton = (Button) findViewById(R.id.button_swap);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mStatusView = (TextView) findViewById(R.id.status);
        
        //
        // Install the language adapters on both the From and To spinners.
        //
        mFromButton.setOnClickListener(mClickListener);
        mToButton.setOnClickListener(mClickListener);

        mTranslateButton.setOnClickListener(this);
        mSwapButton.setOnClickListener(this);
        mFromEditText.selectAll();

        connectToTranslateService();
    }
    
    private void connectToTranslateService() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        bindService(intent, mTranslateConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = getPrefs(this);
        mDoTranslate = false;

        //
        // See if we have any saved preference for From
        //
        Language from = Language.findLanguageByShortName(prefs.getString(FROM, DEFAULT_FROM));
        updateButton(mFromButton, from, false /* don't translate */);

        //
        // See if we have any saved preference for To
        //
        //
        Language to = Language.findLanguageByShortName(prefs.getString(TO, DEFAULT_TO));
        updateButton(mToButton, to, true /* translate */);
        
        //
        // Restore input and output, if any
        //
        mFromEditText.setText(prefs.getString(INPUT, ""));
        setOutputText(prefs.getString(OUTPUT, ""));
        mDoTranslate = true;
    }
    
    private void setOutputText(String string) {
        log("Setting output to " + string);
        mToEditText.setText(new Entities().unescape(string));
    }

    private void updateButton(Button button, Language language, boolean translate) {
        language.configureButton(this, button);
        if (translate) maybeTranslate();
    }

    /**
     * Launch the translation if the input text field is not empty.
     */
    private void maybeTranslate() {
        if (mDoTranslate && !TextUtils.isEmpty(mFromEditText.getText().toString())) {
            doTranslate();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();

        //
        // Save the content of our views to the shared preferences
        //
        Editor edit = getPrefs(this).edit();
        String f = ((Language) mFromButton.getTag()).getShortName();
        String t = ((Language) mToButton.getTag()).getShortName();
        String input = mFromEditText.getText().toString();
        String output = mToEditText.getText().toString();
        savePreferences(edit, f, t, input, output);
    }
    
    static void savePreferences(Editor edit, String from, String to, String input, String output) {
        log("Saving preferences " + from + " " + to + " " + input + " " + output);
        edit.putString(FROM, from);
        edit.putString(TO, to);
        edit.putString(INPUT, input);
        edit.putString(OUTPUT, output);
        edit.commit();
    }
    
    static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mTranslateConn);
    }

    public void onClick(View v) {
        if (v == mTranslateButton) {
            maybeTranslate();
        } else if (v == mSwapButton) {
            Object newFrom = mToButton.getTag();
            Object newTo = mFromButton.getTag();
            mFromEditText.setText(mToEditText.getText());
            mToEditText.setText("");
            setNewLanguage((Language) newFrom, true /* from */, false /* don't translate */);
            setNewLanguage((Language) newTo, false /* to */, true /* translate */);
            mFromEditText.requestFocus();
            mStatusView.setText(R.string.languages_swapped);
        }
    }
    
    private void doTranslate() {
        mStatusView.setText(R.string.retrieving_translation);
        mHandler.post(new Runnable() {
            public void run() {
                mProgressBar.setVisibility(View.VISIBLE);
                String result = "";
                try {
                    Language from = (Language) mFromButton.getTag();
                    Language to = (Language) mToButton.getTag();
                    String fromShortName = from.getShortName();
                    String toShortName = to.getShortName();
                    String input = mFromEditText.getText().toString();
                    log("Translating from " + fromShortName + " to " + toShortName);
                    result = mTranslateService.translate(input, fromShortName, toShortName);
                    if (result == null) {
                        throw new Exception(getString(R.string.translation_failed));
                    }
                    History.addHistoryRecord(TranslateActivity.this, from, to, input, result);
                    mStatusView.setText(R.string.found_translation);
                    setOutputText(result);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mFromEditText.selectAll();
                } catch (Exception e) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mStatusView.setText("Error:" + e.getMessage());
                }
            }
        });
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d) {
        if (id == LANGUAGE_DIALOG_ID) {
            boolean from = mLatestButton == mFromButton;
            ((LanguageDialog) d).setFrom(from);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == LANGUAGE_DIALOG_ID) {
            return new LanguageDialog(this);
        } else if (id == ABOUT_DIALOG_ID) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.about_title);
            builder.setMessage(getString(R.string.about_message));
            builder.setIcon(R.drawable.babelfish);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setNeutralButton(R.string.send_email,
                    new android.content.DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse("mailto:cedric@beust.com"));
                            startActivity(intent);
                        }
                    });
            builder.setCancelable(true);
            return builder.create();
        }
        return null;
    }
    
    /**
     * Pick a random word and set it as the input word.
     */
    public void selectRandomWord() {
        BufferedReader fr = null;
        try {
            GZIPInputStream is =
                    new GZIPInputStream(getResources().openRawResource(R.raw.dictionary));
            if (mWordBuffer == null) {
                mWordBuffer = new byte[601000];
                int n = is.read(mWordBuffer, 0, mWordBuffer.length);
                int current = n;
                while (n != -1) {
                    n = is.read(mWordBuffer, current, mWordBuffer.length - current);
                    current += n;
                }
                is.close();
                mWordCount = 0;
                mWordIndices = Lists.newArrayList();
                for (int i = 0; i < mWordBuffer.length; i++) {
                    if (mWordBuffer[i] == '\n') {
                        mWordCount++;
                        mWordIndices.add(i);
                    }
                }
                log("Found " + mWordCount + " words");
            }

            int randomWordIndex = (int) (System.currentTimeMillis() % (mWordCount - 1));
            log("Random word index:" + randomWordIndex + " wordCount:" + mWordCount);
            int start = mWordIndices.get(randomWordIndex);
            int end = mWordIndices.get(randomWordIndex + 1);
            byte[] b = new byte[end - start - 2];
            System.arraycopy(mWordBuffer, start + 1, b, 0, (end - start - 2)); 
            String randomWord = new String(b);
            mFromEditText.setText(randomWord);
            updateButton(mFromButton,
                    Language.findLanguageByShortName(Language.ENGLISH.getShortName()),
                    true /* translate */);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        
    }

    public void setNewLanguage(Language language, boolean from, boolean translate) {
        updateButton(from ? mFromButton : mToButton, language, translate);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.translate_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.about:
            showDialog(ABOUT_DIALOG_ID);
            break;

        case R.id.show_history:
            showHistory();
            break;

        case R.id.random_word:
            selectRandomWord();
            break;

        // We shouldn't need this menu item but because of a bug in 1.0, neither SMS nor Email
        // filter on the ACTION_SEND intent.  Since they won't be shown in the activity chooser,
        // I need to make an explicit menu for SMS
        case R.id.send_with_sms: {
            Intent i = new Intent(Intent.ACTION_PICK, Contacts.Phones.CONTENT_URI);
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(Contacts.Phones.CONTENT_TYPE);
            startActivityForResult(intent, 42 /* not used */);
            break;
        }

        case R.id.send_with_email:
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, mToEditText.getText());
            startActivity(Intent.createChooser(intent, null));
            break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        if (resultIntent != null) {
            Uri contactURI = resultIntent.getData();
            
            Cursor cursor = getContentResolver().query(contactURI,
                    new String[] { Contacts.PhonesColumns.NUMBER }, 
                    null, null, null);
            if (cursor.moveToFirst()) {
                String phone = cursor.getString(0);
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto://" + phone));
                intent.putExtra("sms_body", mToEditText.getText().toString());
                startActivity(intent);
            }
        }
    }
    
    private void showHistory() {
        startActivity(new Intent(this, HistoryActivity.class));
    }
    
    private static void log(String s) {
        Log.d(TAG, "[TranslateActivity] " + s);
    }

}
