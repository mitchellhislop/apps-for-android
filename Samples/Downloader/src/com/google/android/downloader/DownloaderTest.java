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

package com.google.android.downloader;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class DownloaderTest extends Activity {

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (! DownloaderActivity.ensureDownloaded(this,
                getString(R.string.app_name), FILE_CONFIG_URL,
                CONFIG_VERSION, DATA_PATH, USER_AGENT)) {
            return;
        }
        setContentView(R.layout.main);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = true;
        int id = item.getItemId();

        if (id == R.id.menu_main_download_again) {
            downloadAgain();
        } else {
            handled = false;
        }

        if (!handled) {
            handled = super.onOptionsItemSelected(item);
        }
        return handled;
    }

    private void downloadAgain() {
        DownloaderActivity.deleteData(DATA_PATH);
        startActivity(getIntent());
        finish();
    }
    /**
     * Fill this in with your own web server.
     */
    private final static String FILE_CONFIG_URL =
        "http://example.com/download.config";
    private final static String CONFIG_VERSION = "1.0";
    private final static String DATA_PATH = "/sdcard/data/downloadTest";
    private final static String USER_AGENT = "MyApp Downloader";
}
