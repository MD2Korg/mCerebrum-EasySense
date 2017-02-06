package org.md2k.easysense;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleScanResult;
import com.polidea.rxandroidble.exceptions.BleScanException;

import org.md2k.datakitapi.source.METADATA;
import org.md2k.easysense.bluetooth.MyBlueTooth;

import java.util.ArrayList;

import rx.Observer;
import rx.Subscription;

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

public class PrefsFragmentSettingsPlatform extends PreferenceFragment {
    public static final String TAG = PrefsFragmentSettingsPlatform.class.getSimpleName();
    String deviceId = "";
    boolean isScanning;
    private ArrayAdapter<String> adapterDevices;
    private ArrayList<String> devices = new ArrayList<>();
    private MyBlueTooth myBlueTooth;
    public Subscription subscriptionScan;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myBlueTooth = new MyBlueTooth();
        addPreferencesFromResource(R.xml.pref_settings_platform);
        setupListViewDevices();
        setupPreferenceDeviceId();
        setAddButton();
        setScanButton();
        setCancelButton();
        subscribeScan();
    }
    private void subscribeScan(){
        isScanning = true;
        Button button = (Button) getActivity().findViewById(R.id.button_2);
        button.setText(R.string.button_scanning);
        subscriptionScan=myBlueTooth.observableScan(MyBlueTooth.DEVICE_SERVICE_UUID)
                .subscribe(new Observer<RxBleScanResult>() {
                    @Override
                    public void onCompleted() {
                        Log.e(TAG,"scan .. .onCompleted()");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"scan error...e="+e.getMessage());
                        if (e instanceof BleScanException) {
                            handleBleScanException((BleScanException) e);
                        }
                    }

                    @Override
                    public void onNext(RxBleScanResult rxBleScanResult) {
                        Log.e(TAG,"scan .. .onNext()");
                        String macAddress = rxBleScanResult.getBleDevice().getMacAddress();
                        String name = rxBleScanResult.getBleDevice().getName();
                        if(name==null || name.length()==0)
                            name=macAddress;
                        else name+=" ("+macAddress+")";
                        for (int i = 0; i < devices.size(); i++)
                            if (devices.get(i).equals(name))
                                return;
                        devices.add(name);
                        adapterDevices.notifyDataSetChanged();

                    }
                });
    }

    void setupListViewDevices() {
        adapterDevices = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_single_choice,
                android.R.id.text1, devices);
        ListView listViewDevices = (ListView) getActivity().findViewById(R.id.listView_devices);
        listViewDevices.setAdapter(adapterDevices);
        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            String item = ((TextView) view).getText().toString().trim();
            Preference preference = findPreference("deviceId");
            deviceId = item;
            preference.setSummary(item);
        });
    }

    private void setupPreferenceDeviceId() {
        Preference preference = findPreference("deviceId");
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            Log.d(TAG, preference1.getKey() + " " + newValue.toString());
            deviceId = newValue.toString().trim();
            preference1.setSummary(newValue.toString().trim());
            return false;
        });

    }


    private String getName(String str) {
        if (str.endsWith(")")) {
            String[] arr = str.split(" ");
            return arr[0];
        } else
            return null;
    }

    private String getDeviceId(String str) {
        if (str.endsWith(")")) {
            String[] arr = deviceId.split(" ");
            return arr[1].substring(1, arr[1].length() - 1);
        } else
            return str;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        assert v != null;
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setPadding(0, 0, 0, 0);
        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        unSubscribeScan();
        super.onDestroy();
    }
    void unSubscribeScan(){
        Log.d(TAG,"unSubscribeScan()...");
        isScanning = false;
        Button button = (Button) getActivity().findViewById(R.id.button_2);
        button.setText(R.string.button_scan);

        if(subscriptionScan!=null && !subscriptionScan.isUnsubscribed())
            subscriptionScan.unsubscribe();
    }
    private void setAddButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_3);
        button.setText(R.string.button_text_save);

        button.setOnClickListener(v -> {
            if (deviceId == null || deviceId.equals(""))
                Toast.makeText(getActivity(), "!!! Device ID is missing !!!", Toast.LENGTH_LONG).show();
            else {
                Intent returnIntent = new Intent();
                returnIntent.putExtra(METADATA.DEVICE_ID, getDeviceId(deviceId));
                returnIntent.putExtra(METADATA.NAME, getName((deviceId)));
                getActivity().setResult(Activity.RESULT_OK, returnIntent);
                getActivity().finish();
            }
        });
    }

    private void setScanButton() {
        try {
            final Button button = (Button) getActivity().findViewById(R.id.button_2);
            button.setOnClickListener(v -> {
                if(isScanning){
                    unSubscribeScan();
                }else{
                    subscribeScan();

                }
            });
        } catch (Exception ignored) {

        }
    }

    private void setCancelButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText(R.string.button_close);

        button.setOnClickListener(v -> {
            Intent returnIntent = new Intent();
            getActivity().setResult(Activity.RESULT_CANCELED, returnIntent);
            unSubscribeScan();
            getActivity().finish();
        });
    }
    private void handleBleScanException(BleScanException bleScanException) {

        switch (bleScanException.getReason()) {
            case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                Toast.makeText(getActivity(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                getActivity().finish();
                break;
            case BleScanException.BLUETOOTH_DISABLED:
//                Toast.makeText(getActivity(), "Enable bluetooth and try again", Toast.LENGTH_SHORT).show();
                myBlueTooth.enable();
                unSubscribeScan();
                subscribeScan();
                break;
            case BleScanException.LOCATION_PERMISSION_MISSING:
                Toast.makeText(getActivity(),
                        "On Android 6.0 location permission is required. Implement Runtime Permissions", Toast.LENGTH_SHORT).show();
                getActivity().finish();
                break;
            case BleScanException.LOCATION_SERVICES_DISABLED:
                Toast.makeText(getActivity(), "Location services needs to be enabled on Android 6.0", Toast.LENGTH_SHORT).show();
                getActivity().finish();
                break;
            case BleScanException.BLUETOOTH_CANNOT_START:
            default:
                Toast.makeText(getActivity(), "Unable to start scanning", Toast.LENGTH_SHORT).show();
                getActivity().finish();
                break;
        }
    }

}
