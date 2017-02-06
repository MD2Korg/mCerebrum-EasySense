package org.md2k.easysense.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleScanResult;

import org.md2k.easysense.ApplicationWithBluetooth;
import org.md2k.utilities.Report.Log;

import java.math.BigInteger;
import java.util.UUID;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;


/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
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

public class MyBlueTooth {
    private static final String TAG = MyBlueTooth.class.getSimpleName();
    private Context context;
    private RxBleClient rxBleClient;
    private RxBleConnection rxBleConnection;
    private RxBleDevice rxBleDevice;
    public static final UUID DEVICE_SERVICE_UUID = UUID.fromString("ea210000-0000-1000-8000-00805f9b5d6f");
    private static final UUID CHARACTERISTIC_WRITE = UUID.fromString("ea210001-0000-1000-8000-00805f9b5d6f");

    public static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public void enable() {
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter == null) return;
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }

    public MyBlueTooth() {
        context = ApplicationWithBluetooth.getAppContext();
        rxBleClient = ApplicationWithBluetooth.getRxBleClient();
    }

    public boolean hasSupport() {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
            return false;
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        return mBluetoothAdapter != null;
    }

    public RxBleDevice getDevice(String macAddress) {
        return rxBleClient.getBleDevice(macAddress);
    }

    public boolean isConnected() {
        return rxBleDevice != null && rxBleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    public Observable<RxBleScanResult> observableScan(UUID uuid) {
        return ApplicationWithBluetooth.getRxBleClient().scanBleDevices(uuid);
    }

    public Observable<RxBleConnection> observableConnect(String macAddress) {
        rxBleDevice = rxBleClient.getBleDevice(macAddress);
        return rxBleDevice
                .establishConnection(context, false)
                .doOnNext(rxBleConnectionTemp -> rxBleConnection = rxBleConnectionTemp);
    }

    public Observable<Integer> observableWrite(String writeString) {
        return rxBleConnection.writeCharacteristic(CHARACTERISTIC_WRITE, getBytes(writeString))
                .flatMap(new Func1<byte[], Observable<Observable<byte[]>>>() {
                    @Override
                    public Observable<Observable<byte[]>> call(byte[] bytes) {
                        return rxBleConnection.setupNotification(CHARACTERISTIC_WRITE);
                    }
                }).flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .map(values -> {
                    int value = 0;
                    for (int i = 0; i < values.length; i++) {
                        value += ((long) values[i] & 0xffL) << (8 * i);
                    }
                    Log.d(TAG, "values=" + value);
                    return value;
                });
    }

    public Observable<Integer> observableRead() {
        return rxBleConnection.setupNotification(CHARACTERISTIC_WRITE)
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .map(values -> {
                    int value = 0;
                    for (int i = 0; i < values.length; i++) {
                        value += ((long) values[i] & 0xffL) << (8 * i);
                    }
                    Log.d(TAG, "values=" + value);
                    return value;
                });
    }

    private byte[] getBytes(String writeString) {
        BigInteger bigInteger = new BigInteger(writeString, 16);
        return bigInteger.toByteArray();
    }
}
