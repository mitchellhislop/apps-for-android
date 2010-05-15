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
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * A simple list activity that displays Bluetooth devices that are in
 * discoverable mode. This can be used as a gamelobby where players can see
 * available servers and pick the one they wish to connect to.
 */

public class ServerListActivity extends ListActivity {
    public static String EXTRA_SELECTED_ADDRESS = "btaddress";

    private BluetoothAdapter myBt;

    private ServerListActivity self;

    private ArrayAdapter<String> arrayAdapter;

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Parcelable btParcel = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothDevice btDevice = (BluetoothDevice) btParcel;
            arrayAdapter.add(btDevice.getName() + " - " + btDevice.getAddress());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        self = this;
        arrayAdapter = new ArrayAdapter<String>(self, R.layout.text);
        ListView lv = self.getListView();
        lv.setAdapter(arrayAdapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                myBt.cancelDiscovery(); // Cancel BT discovery explicitly so
                // that connections can go through
                String btDeviceInfo = ((TextView) arg1).getText().toString();
                String btHardwareAddress = btDeviceInfo.substring(btDeviceInfo.length() - 17);
                Intent i = new Intent();
                i.putExtra(EXTRA_SELECTED_ADDRESS, btHardwareAddress);
                self.setResult(Activity.RESULT_OK, i);
                finish();
            }
        });
        myBt = BluetoothAdapter.getDefaultAdapter();
        myBt.startDiscovery();
        self.setResult(Activity.RESULT_CANCELED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(myReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(myReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myBt != null) {
            myBt.cancelDiscovery(); // Ensure that we don't leave discovery
            // running by accident
        }
    }

}
