package org.md2k.easysense;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.github.paolorotolo.appintro.AppIntro;

import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.easysense.datakit.DataKitManager;
import org.md2k.easysense.devices.Devices;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.Report.LogStorage;
import org.md2k.utilities.dialog.Dialog;
import org.md2k.utilities.permission.ObservablePermission;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

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

public abstract class AbstractActivityEasySense extends AppIntro {
    private static final String TAG = AbstractActivityEasySense.class.getSimpleName();
    Devices devices;
    Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subscription = permission()
                .map(aBoolean -> {
                    Log.d(TAG, "before ReadSettings...");
                    boolean result = readSettings();
                    Log.d(TAG, "after ReadSettings...result=" + result);
                    return result;

                })
                .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Boolean aBoolean) {
                        Log.d(TAG, "before datakit connect");
                        return DataKitManager.observableConnect();
                    }
                })
                .map(aBoolean -> {
                    try {
                        Log.d(TAG,"before Registration..");
                        devices.register();
                    } catch (DataKitException e) {
                        throw new RuntimeException("DataKit Registration fail");
                    }
                    return true;
                })
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted()...");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError()...e=" + e.getMessage());
                        finish();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.d(TAG, "onNext()...");
                    }
                });
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
    }

    @Override
    public void onDestroy() {
        if (subscription != null && !subscription.isUnsubscribed())
            subscription.unsubscribe();
        super.onDestroy();
    }

    Observable<Boolean> permission() {
        Observable<Boolean> observable = ObservablePermission.get(this);
        observable.doOnNext(aBoolean -> {
            if (!aBoolean) {
                Toast.makeText(this, R.string.text_permission, Toast.LENGTH_LONG).show();
                throw new RuntimeException("Permission Denied");
            }
        });
        return observable;
    }

    boolean readSettings() {
        LogStorage.startLogFileStorageProcess(getApplicationContext().getPackageName());
        devices = new Devices(this);
        if (devices.size() == 0) {
            Toast.makeText(this, "ERROR: EasySense device is not configured...", Toast.LENGTH_SHORT).show();
            throw new RuntimeException("Configuration problem");
        }
        return true;
    }


}
