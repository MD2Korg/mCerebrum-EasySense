package org.md2k.easysense;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.easysense.devices.Devices;
import org.md2k.utilities.UI.AlertDialogs;

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
public class PrefsFragmentSettings extends PreferenceFragment {
    private static final String TAG = PrefsFragmentSettings.class.getSimpleName();
    private static final int ADD_DEVICE = 1;
    Devices devices;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        devices = new Devices(getActivity());
        addPreferencesFromResource(R.xml.pref_settings_general);
//                setPreferenceBluetoothPair();
        setPreferenceScreenDeviceAdd();
        setPreferenceScreenConfigured();
        setCloseButton();
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

    void setPreferenceScreenConfigured() {
        for (int i = 0; i < devices.size(); i++) {
            String name = devices.get(i).getName();
            String deviceId = devices.get(i).getDeviceId();
            addToConfiguredList(deviceId, name);
        }
    }

    private void setPreferenceScreenDeviceAdd() {
        Preference preference = findPreference("key_device_add");
        preference.setOnPreferenceClickListener(preference1 -> {
            final Intent intent = new Intent(getActivity(), ActivitySettingsPlatform.class);
            startActivityForResult(intent, ADD_DEVICE);
            return false;
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "onActivityResult(): result ok");
                String deviceId = data.getStringExtra(METADATA.DEVICE_ID);
                String name = data.getStringExtra(METADATA.NAME);
                if (devices.find(deviceId) != null)
                    Toast.makeText(getActivity(), "Error: Device is already configured...", Toast.LENGTH_SHORT).show();
                else if (devices.find(null) != null)
                    Toast.makeText(getActivity(), "Error: A device is already configured with same placement...", Toast.LENGTH_SHORT).show();
                else {
                    devices.add(PlatformType.EASYSENSE, deviceId, name);
                    addToConfiguredList(deviceId, name);
                    try {
                        devices.writeDataSourceToFile();
                    } catch (Exception ignored) {

                    }
                }
            }
        }
    }

    private void addToConfiguredList(String deviceId, String name) {
        PreferenceCategory category = (PreferenceCategory) findPreference("key_device_configured");
        Preference preference = new Preference(getActivity());
        preference.setKey(deviceId);
        if (name == null || name.length() == 0)
            preference.setTitle(deviceId);
        else
            preference.setTitle(name + " (" + deviceId + ")");
        preference.setIcon(R.drawable.ic_easysense_teal_48dp);
        preference.setOnPreferenceClickListener(preferenceListenerConfigured());
        category.addPreference(preference);
    }

    private Preference.OnPreferenceClickListener preferenceListenerConfigured() {
        return preference -> {
            final String deviceId = preference.getKey();
            AlertDialogs.AlertDialog(getActivity(), "Delete Device", "Delete Device (" + preference.getTitle() + ")?", R.drawable.ic_delete_red_48dp, "Delete", "Cancel", null, (dialog, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    PreferenceCategory category = (PreferenceCategory) findPreference("key_device_configured");
                    for (int i = 0; i < category.getPreferenceCount(); i++) {
                        Preference preference1 = category.getPreference(i);
                        if (preference1.getKey().equals(deviceId)) {
                            category.removePreference(preference1);
                            devices.delete(deviceId);
                            try {
                                devices.writeDataSourceToFile();
                            } catch (Exception ignored) {

                            }
                            return;
                        }
                    }
                } else {
                    dialog.dismiss();
                }
            });
            return true;
        };
    }

    private void setCloseButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText(R.string.button_close);
        button.setOnClickListener(v -> getActivity().finish());
    }
}
