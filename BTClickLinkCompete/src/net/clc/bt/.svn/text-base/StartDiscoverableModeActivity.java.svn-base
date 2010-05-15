/*
 * Copyright (C) 2009 The Android Open Source Project
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

package net.clc.bt;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.Toast;

/**
 * A simple activity that displays a prompt telling users to enable discoverable
 * mode for Bluetooth.
 */

public class StartDiscoverableModeActivity extends Activity {
    public static final int REQUEST_DISCOVERABLE_CODE = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent();
        i.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(i, REQUEST_DISCOVERABLE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DISCOVERABLE_CODE) {
            // Bluetooth Discoverable Mode does not return the standard
            // Activity result codes.
            // Instead, the result code is the duration (seconds) of
            // discoverability or a negative number if the user answered "NO".
            if (resultCode < 0) {
                showWarning();
            } else {
                Toast.makeText(this, "Discoverable mode enabled.", 1).show();
                finish();
            }
        }
    }

    private void showWarning() {
        Builder warningDialog = new Builder(this);
        final Activity self = this;

        warningDialog.setTitle(R.string.DISCOVERABLE_MODE_NOT_ENABLED);
        warningDialog.setMessage(R.string.WARNING_MESSAGE);

        warningDialog.setPositiveButton("Yes", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent();
                i.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(i, REQUEST_DISCOVERABLE_CODE);
            }
        });

        warningDialog.setNegativeButton("No", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(self, "Discoverable mode NOT enabled.", 1).show();
                finish();
            }
        });
        warningDialog.setCancelable(false);
        warningDialog.show();
    }

}
