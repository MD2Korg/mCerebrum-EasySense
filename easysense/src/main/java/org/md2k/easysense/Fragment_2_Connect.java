package org.md2k.easysense;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.md2k.easysense.bluetooth.MyBlueTooth;

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
public class Fragment_2_Connect extends Fragment_Base {
    private static final String TAG = Fragment_2_Connect.class.getSimpleName();
    int isBattery, isData;
    ActivityEasySense activity;
    Handler handler;


    /*
        Runnable runnableNextPage = new Runnable() {
            @Override
            public void run() {
                if (isData == 0 || isBattery == 0) return;
                stop();
                isData = 0;
                isBattery = 0;
                activity.nextSlide();

            }
        };
*/

    public static Fragment_2_Connect newInstance(String title, String message, int image) {
        Fragment_2_Connect f = new Fragment_2_Connect();
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
        handler = new Handler();
        activity = (ActivityEasySense) getActivity();
    }

    @Override
    public void onDestroy() {
//        handler.removeCallbacks(runnableNextPage);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_2_connect, container, false);

        TextView tv = (TextView) v.findViewById(R.id.text_view_title);
        tv.setText(getArguments().getString("title"));
        tv = (TextView) v.findViewById(R.id.text_view_message);
        tv.setText(getArguments().getString("message"));
        Button button1 = (Button) v.findViewById(R.id.button_1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(activity.myBlueTooth!=null){
                    activity.myBlueTooth.disconnect();
                    activity.myBlueTooth.scanOff();
                    activity.myBlueTooth.close();
                    activity.myBlueTooth = null;
                }
                activity.startSlide();
            }
        });

        return v;
    }


    public void start() {
        if(activity.myBlueTooth!=null){
            activity.myBlueTooth.disconnect();
            activity.myBlueTooth.scanOff();
            activity.myBlueTooth.close();
            activity.myBlueTooth = null;
        }
        activity.myBlueTooth = new MyBlueTooth(getActivity(), activity.onConnectionListener, activity.onReceiveListener);
    }

    public void stop() {
    }
}
