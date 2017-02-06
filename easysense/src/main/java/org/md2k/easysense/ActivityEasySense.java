package org.md2k.easysense;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.polidea.rxandroidble.RxBleConnection;

import org.md2k.datakitapi.datatype.DataTypeJSONObject;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.easysense.bluetooth.MyBlueTooth;
import org.md2k.easysense.data.Status;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.dialog.Dialog;

import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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

public class ActivityEasySense extends AbstractActivityEasySense {
    public static final int STATE_SKIP = -1;
    public static final int STATE_INFO = 0;
    public static final int STATE_CONNECT = 1;
    public static final int STATE_CONNECT_RETRY = 2;
    public static final int STATE_CONNECTED=3;
    public static final int STATE_READ = 4;
    private static final int STATE_SUCCESS = 5;
    public static final int PAGE_INFO = 0;
    public static final int PAGE_CONNECT = 1;
    public static final int PAGE_CONNECTED = 2;
    public static final int PAGE_READING = 3;
    public static final int PAGE_SUCCESS = 4;
    private static final String TAG = ActivityEasySense.class.getSimpleName();
    int state;
    MyBlueTooth myBlueTooth;
    private Subscription subscriptionConnect;
    private Subscription subscriptionWrite;
    Status status=new Status();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myBlueTooth = new MyBlueTooth();
        UISettings();
    }

    @Override
    public void onDestroy() {
        unSubscribeAll();
        super.onDestroy();
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        new Dialog().Question(this, "Skip Chest Reading", "Are you sure to skip chest reading?", new String[]{"Yes", "No"}, (which, result) -> {
            if (which == Dialog.DialogResponse.POSITIVE) {
                setState(STATE_SKIP);
            }
        }).show();
    }

    public void setState(int state) {
        this.state = state;
        switch (state) {
            case STATE_SKIP:
                unSubscribeAll();
                finish();
                break;
            case STATE_INFO:
                unSubscribeAll();
                pager.setCurrentItem(PAGE_INFO);
                break;
            case STATE_CONNECT:
                unSubscribeAll();
                subscribeConnect(devices.get(0).getDeviceId());
                pager.setCurrentItem(PAGE_CONNECT);
                break;
            case STATE_CONNECT_RETRY:
                unSubscribeAll();
                subscribeConnect(devices.get(0).getDeviceId());
                pager.setCurrentItem(PAGE_CONNECT);
                break;
            case STATE_CONNECTED:
                pager.setCurrentItem(PAGE_CONNECTED);
                break;
            case STATE_READ:
                subscribeWrite();
                //subscribeRead();
                pager.setCurrentItem(PAGE_READING);
                break;
            case STATE_SUCCESS:
                unSubscribeAll();
                pager.setCurrentItem(PAGE_SUCCESS);
        }
    }

    public void subscribeConnect(String macAddress) {
        subscriptionConnect = myBlueTooth
                .observableConnect(macAddress)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RxBleConnection>() {
            @Override
            public void onCompleted() {
                Log.d(TAG,"onCompleted()...");
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG,"onError()...e="+e.getMessage());
                new Dialog().Error(ActivityEasySense.this, "Connection Error", "Could not connect with the device...Please try again", new String[]{"Ok"}, (which, result) -> {
                }).show();
            }

            @Override
            public void onNext(RxBleConnection rxBleConnectionn) {
                Log.d(TAG,"onNext()...");
                if (myBlueTooth.isConnected())
                    setState(STATE_CONNECTED);
                else {
                    new Dialog().Error(ActivityEasySense.this, "Connection Error", "Connection Error... Please try again", new String[]{"Ok"}, (which, result) -> {
                    }).show();
                }
            }
        });
    }
    public void subscribeWrite(){
        long timeStamp = DateTime.getDateTime();
        String writeString = Long.toHexString(Constants.READ_TIME_IN_SECOND) + Long.toHexString(timeStamp / 1000);
        status.setTimestamp(timeStamp);
        status.setWrite_to_device(writeString);
        status.setDuration(Constants.READ_TIME_IN_SECOND);
        subscriptionWrite = myBlueTooth.observableWrite(writeString)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                               @Override
                               public void onCompleted() {
                                   Log.d(TAG,"onCompleted()...");
                               }

                               @Override
                               public void onError(Throwable e) {
                                   Log.d(TAG,"onError()...");
                               }

                               @Override
                               public void onNext(Integer statusCode) {
                                   Log.d(TAG,"onNext()...");
                                   switch (statusCode) {
                                       case Status.SUCCESS:
                                       case Status.VERIFIED:
                                           status.setStatus_code(statusCode);
                                           status.setIs_success(true);
                                           writeToDataKit(status);
                                           setState(STATE_SUCCESS);
                                           break;
                                       case Status.FAILURE:
                                       case Status.INPUT_TO_LARGE:
                                       case Status.INPUT_TO_SMALL:
                                           status.setStatus_code(statusCode);
                                           status.setIs_success(false);
                                           //writeToDataKit(status);
                                           Toast.makeText(ActivityEasySense.this, "!!! ERROR !!!. Could not read properly. Please try again..", Toast.LENGTH_LONG).show();
                                           setState(PAGE_INFO);
                                           break;
                                       default:
                                           double progress = (double) (statusCode * 100) / ((double) (Constants.READ_TIME_IN_SECOND) * 1.5);
                                           DonutProgress donutProgress= (DonutProgress) findViewById(R.id.donut_progress);
                                           donutProgress.setProgress((int) progress);
                                           break;

                                   }
                               }
                           }
                );

/*
                .subscribe(new Observer<byte[]>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(byte[] bytes) {

                    }
                });
*/

    }


    void unSubscribeConnect() {
        if (subscriptionConnect != null && !subscriptionConnect.isUnsubscribed())
            subscriptionConnect.unsubscribe();
    }

    void unSubscribeWrite() {
        if (subscriptionWrite != null && !subscriptionWrite.isUnsubscribed())
            subscriptionWrite.unsubscribe();
    }

    void unSubscribeAll() {
        unSubscribeConnect();
        unSubscribeWrite();
    }

    void writeToDataKit(Status status) {
        try {
            Gson gson = new Gson();
            JsonObject sample = new JsonParser().parse(gson.toJson(status)).getAsJsonObject();
            DataTypeJSONObject dataTypeJSONObject = new DataTypeJSONObject(DateTime.getDateTime(), sample);
            devices.get(0).getSensor(DataSourceType.STATUS).insert(dataTypeJSONObject);
        } catch (DataKitException e) {
            e.printStackTrace();
        }
    }
    boolean UISettings() {
        setBarColor(ContextCompat.getColor(this, R.color.teal_500));
        setSeparatorColor(ContextCompat.getColor(this, R.color.deeporange_500));
        setSwipeLock(true);
        setDoneText("");
        setNextArrowColor(ContextCompat.getColor(this, R.color.teal_500));
        addSlide(Fragment_1_Info.newInstance(this));
        addSlide(Fragment_2_Connect.newInstance(this));
        addSlide(Fragment_3_Connected.newInstance(this));
        addSlide(Fragment_4_Reading.newInstance(this));
        addSlide(Fragment_5_Success.newInstance(this));
        return true;
    }

}
