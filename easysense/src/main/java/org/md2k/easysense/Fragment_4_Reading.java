package org.md2k.easysense;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.md2k.datakitapi.datatype.DataTypeJSONObject;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.easysense.data.Status;

/**
 * Copyright (c) 2016, The University of Memphis, MD2K Center
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
public class Fragment_4_Reading extends Fragment_Base {
    private static final String TAG = Fragment_4_Reading.class.getSimpleName();
    ActivityEasySense activity;
    DonutProgress donutProgress;
    Handler handler;
    boolean isAlreadyWrite;
    String writeString;
    long timeStamp;
    String timeString;


    public static Fragment_4_Reading newInstance(String title, String message, int image) {
        Fragment_4_Reading f = new Fragment_4_Reading();
        Bundle b = new Bundle();
        b.putString("title", title);
        b.putString("message", message);
        b.putInt("image", image);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (ActivityEasySense) getActivity();
        handler = new Handler();
    }
    void writeToDataKit(int statusCode, boolean isSuccess){
        Status status;
        try {
            status = new Status();
            status.setDuration(Constants.READ_TIME_IN_SECOND);
            status.setStatus_code(statusCode);
            status.setIs_success(isSuccess);
            status.setWrite_to_device(writeString);
            status.setTime_string(timeString);
            status.setTimestamp(timeStamp);
            Gson gson = new Gson();
            JsonObject sample = new JsonParser().parse(gson.toJson(status)).getAsJsonObject();
            DataTypeJSONObject dataTypeJSONObject = new DataTypeJSONObject(DateTime.getDateTime(), sample);
            activity.devices.get(0).getSensor(DataSourceType.STATUS).insert(dataTypeJSONObject);
        } catch (DataKitException e) {
            e.printStackTrace();
        }

    }

    BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int statusCode = intent.getIntExtra(ActivityEasySense.DATA, 0);
            stop();
            if (!isAlreadyWrite) {
                Toast.makeText(getActivity(), "!!! Still Running !!!. Please try again after a minute...", Toast.LENGTH_SHORT).show();
                handler.postDelayed(runnableWriteCharacteristics, 2000);
                return;
            }
            switch (statusCode) {
                case Status.SUCCESS:
                case Status.VERIFIED:
                    writeToDataKit(statusCode, true);
                    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
                    activity.nextSlide();
                    break;
                case Status.FAILURE:
                case Status.INPUT_TO_LARGE:
                case Status.INPUT_TO_SMALL:
                    writeToDataKit(statusCode, false);
                    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
                    Toast.makeText(getActivity(), "!!! ERROR !!!. Could not read properly. Please try again..", Toast.LENGTH_LONG).show();
                    if (activity.myBlueTooth != null) {
                        activity.myBlueTooth.disconnect();
                        activity.myBlueTooth.scanOff();
                        activity.myBlueTooth.close();
                        activity.myBlueTooth = null;
                    }
                    activity.startSlide();
                    break;
                default:
                    double progress = (double) (statusCode * 100) / ((double) (Constants.READ_TIME_IN_SECOND) * 1.5);
                    donutProgress.setProgress((int) progress);
                    break;
            }
        }
    };

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_4_reading, container, false);

        TextView tv = (TextView) v.findViewById(R.id.text_view_title);
        tv.setText(getArguments().getString("title"));
        tv = (TextView) v.findViewById(R.id.text_view_message);
        tv.setText(getArguments().getString("message"));
        Button button1 = (Button) v.findViewById(R.id.button_1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.myBlueTooth != null) {
                    activity.myBlueTooth.disconnect();
                    activity.myBlueTooth.scanOff();
                    activity.myBlueTooth.close();
                    activity.myBlueTooth = null;
                }
                stop();
                activity.startSlide();
            }
        });
        donutProgress = (DonutProgress) v.findViewById(R.id.donut_progress);
        assert donutProgress != null;
        donutProgress.setMax(100);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(ActivityEasySense.INTENT_RECEIVED_DATA));

        return v;
    }

    private Runnable runnableWriteCharacteristics = new Runnable() {
        @Override
        public void run() {
            timeStamp = DateTime.getDateTime();
            timeString = DateTime.convertTimeStampToDateTime(timeStamp);
            writeString = Long.toHexString(Constants.READ_TIME_IN_SECOND) + Long.toHexString(timeStamp / 1000);
            activity.myBlueTooth.writeCharacteristic(writeString);
            isAlreadyWrite = true;
        }
    };

    public void start() {
        isAlreadyWrite = false;
        handler.postDelayed(runnableWriteCharacteristics, 2000);
    }

    public void stop() {
        handler.removeCallbacks(runnableWriteCharacteristics);
    }
}
