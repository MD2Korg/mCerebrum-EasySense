package org.md2k.easysense.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;


import org.md2k.easysense.Constants;

import java.util.List;
import java.util.UUID;

/**
 * Copyright (c) 2016, The University of Memphis, MD2K Center
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
public class  RunnableServiceStart implements Runnable {
    private static final String TAG = RunnableServiceStart.class.getSimpleName();
    BluetoothGattService bluetoothGattService;
    BluetoothGatt gatt;
    boolean ret;

    RunnableServiceStart(BluetoothGatt gatt, BluetoothGattService bluetoothGattService) {
        Log.d(TAG,"RunnableServiceStart()..."+bluetoothGattService.getUuid());
        this.bluetoothGattService = bluetoothGattService;
        this.gatt = gatt;
    }

    @Override
    public void run() {
        if (bluetoothGattService.getUuid().equals(Constants.IMU_SERVICE_UUID)) {
            List<BluetoothGattCharacteristic> chars = bluetoothGattService.getCharacteristics();
            for (int j = 0; j < chars.size(); j++) {
                BluetoothGattCharacteristic characteristic = chars.get(j);
                if (characteristic == null) {
                    continue;
                }
                if (characteristic.getDescriptors().size() == 0) continue;
                Log.d(TAG, "characteristic UUID=" + characteristic.getUuid());

                if (characteristic.getUuid().equals(Constants.IMU_SERV_CHAR_UUID)) {
                    ret = gatt.setCharacteristicNotification(characteristic, true);
                    if (!ret) {
                        continue;
                    }
                    Log.i(TAG, "[LOG]Blood_Pressure_Measurement:characteristic.getDescriptor");
                    UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor); //descriptor write operation successfully started?

                }
            }
        }
        // When Battery Service is discovered

        if (bluetoothGattService.getUuid().equals(Constants.BATTERY_SERVICE_UUID)) {
            Log.i(TAG, "[LOG]Battery Service is discovered");

            BluetoothGattCharacteristic characteristic = bluetoothGattService.getCharacteristic(Constants.BATTERY_SERV_CHAR_UUID);
            if (characteristic == null) {
                return;
            }
            if (characteristic.getDescriptors().size() == 0) return;

            ret = gatt.setCharacteristicNotification(characteristic, true);
            if (!ret) {
                return;
            }
            UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor); //descriptor write operation successfully started?
            Log.i(TAG, "[LOG]Battery Service:characteristic.getDescriptor");
        }

    }
}
