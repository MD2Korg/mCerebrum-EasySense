package org.md2k.easysense;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.github.paolorotolo.appintro.AppIntro;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.ResultCallback;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.easysense.bluetooth.MyBlueTooth;
import org.md2k.easysense.bluetooth.OnReceiveListener;
import org.md2k.easysense.devices.Devices;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.AlertDialogs;
import org.md2k.utilities.permission.PermissionInfo;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class ActivityEasySense extends AppIntro {
    private static final String TAG = ActivityEasySense.class.getSimpleName();
    public static final String DATA = "data";
    private DataKitAPI dataKitAPI = null;
    public static final String INTENT_RECEIVED_DATA="intent_received_data";
    Devices devices;
    MyBlueTooth myBlueTooth;
    public String writeString="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.getPermissions(this, new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                if (!result) {
                    Toast.makeText(getApplicationContext(), "!PERMISSION DENIED !!! Could not continue...", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    load();
                }
            }
        });
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    void load() {
        devices = new Devices(ActivityEasySense.this);
        if (devices.size() == 0) {
            Toast.makeText(this, "ERROR: EasySense device is not configured...", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            dataKitAPI = DataKitAPI.getInstance(getApplicationContext());
            try {
                dataKitAPI.connect(new OnConnectionListener() {
                    @Override
                    public void onConnected() {
                        try {
                            devices.register();
                        } catch (DataKitException e) {
                            Toast.makeText(ActivityEasySense.this, "Datakit Connection Error", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
            } catch (DataKitException e) {
                Toast.makeText(this, "Datakit Connection Error", Toast.LENGTH_SHORT).show();
                finish();
            }
            setBarColor(ContextCompat.getColor(ActivityEasySense.this, R.color.teal_500));
            setSeparatorColor(ContextCompat.getColor(ActivityEasySense.this, R.color.deeporange_500));
            setSwipeLock(true);
            setDoneText("");
            setNextArrowColor(ContextCompat.getColor(ActivityEasySense.this, R.color.teal_500));
            addSlide(Fragment_1_Info.newInstance("Power On", "Please TURN ON the chest sensor device and tap NEXT", R.drawable.ic_power_teal_48dp));
            addSlide(Fragment_2_Connect.newInstance("Connecting Device...", "Please wait while the device is connecting", R.drawable.ic_easysense_teal_48dp));
            addSlide(Fragment_3_Start.newInstance("Device Connected", "Hold the device to your chest and tap START", R.drawable.ic_easysense_position_teal_48dp));
            addSlide(Fragment_4_Reading.newInstance("Reading...", "Please keep the device in place until the reading is complete.", R.drawable.ic_easysense_teal_48dp));
            addSlide(Fragment_5_Success.newInstance("!!! Thank you !!!", "Chest measurement reading is complete. Please tap DONE.", R.drawable.ic_ok_teal_50dp));
        }
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        AlertDialogs.AlertDialog(ActivityEasySense.this, "Skip Chest Reading", "Are you sure to skip chest reading?", R.drawable.ic_info_teal_48dp, "Yes", "No", null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if (myBlueTooth != null) {
                        myBlueTooth.disconnect();
                        myBlueTooth.scanOff();
                        myBlueTooth.close();
                        myBlueTooth = null;
                    }
                    finish();
                }
            }
        });
    }

    public void nextSlide() {
        pager.setCurrentItem(pager.getCurrentItem() + 1);
    }

    public void startSlide() {
        pager.setCurrentItem(0);
    }

    public void prevSlide() {
        pager.setCurrentItem(pager.getCurrentItem() - 1);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        if (oldFragment != null) {
            Fragment_Base fb = (Fragment_Base) oldFragment;
            fb.stop();
        }
        if (newFragment != null) {
            Fragment_Base fb = (Fragment_Base) newFragment;
            fb.start();
        }
        Log.d("abc", "old=" + oldFragment + " new=" + newFragment);
    }

    @Override
    public void onDestroy() {
        if (dataKitAPI != null) {
            dataKitAPI.disconnect();
        }
        if (myBlueTooth != null) {
            myBlueTooth.disconnect();
            myBlueTooth.scanOff();
            myBlueTooth.close();
            myBlueTooth = null;
        }
        super.onDestroy();
    }

    OnReceiveListener onReceiveListener = new OnReceiveListener() {
        @Override
        public void onReceived(Message msg) {
            Log.d(TAG, "msg = " + msg.what);
            switch (msg.what) {
                case MyBlueTooth.MSG_ADV_CATCH_DEV:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                    if (bluetoothDevice.getAddress().equals(devices.get(0).getDeviceId()))
                        myBlueTooth.connect((BluetoothDevice) msg.obj);
                    break;
                case MyBlueTooth.MSG_CONNECTED:
                    nextSlide();
                    break;

                case MyBlueTooth.MSG_DATA_RECV:
                    Log.d(TAG,"received..."+msg.obj.toString());
                    Intent intent = new Intent(INTENT_RECEIVED_DATA);
                    BlData blData= (BlData) msg.obj;
                    int value = 0;
                    for (int i = 0; i < blData.getData().length; i++)
                    {
                        value += ((long) blData.getData()[i] & 0xffL) << (8 * i);
                    }
                    intent.putExtra(DATA,value);
                    LocalBroadcastManager.getInstance(ActivityEasySense.this).sendBroadcast(intent);
                    break;
            }
        }
    };
    org.md2k.easysense.bluetooth.OnConnectionListener onConnectionListener = new org.md2k.easysense.bluetooth.OnConnectionListener() {
        @Override
        public void onConnected() {
            UUID uuid = Constants.DEVICE_SERVICE_UUID;
            myBlueTooth.scanOn(new UUID[]{uuid});
        }

        @Override
        public void onDisconnected() {

        }
    };

}
