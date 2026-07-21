/*
 * Copyright (C) 2012 The Android Open Source Project
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
 *
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.bluetooth.a2dp;

import android.bluetooth.IBluetoothA2dp;
import android.bluetooth.IBluetoothA2dpExt;
import android.util.Log;

/**
 * Provides Bluetooth A2DP profile, as a service in the new Bluetooth application.
 * @hide
 */
public class A2dpExtService extends A2dpService {
    private static final String TAG = "A2dpExtService";
    private static final boolean DBG = true;

    @Override
    protected IProfileServiceBinder initBinder() {
        Log.i(TAG, "initBinder");
        return new BluetoothA2dpExtBinder(this);
    }


    public IBluetoothA2dp getBluetoothA2dp() {
        Log.i(TAG, "getBluetoothA2dp");
        return IBluetoothA2dp.Stub.asInterface(super.initBinder());
    }

    /**
     * Handlers for incoming service calls
     */
    private static class BluetoothA2dpExtBinder extends IBluetoothA2dpExt.Stub
            implements IProfileServiceBinder {
        private A2dpExtService mService;

        BluetoothA2dpExtBinder(A2dpExtService svc) {
            mService = svc;
        }

        @Override
        public void cleanup() {
            mService = null;
        }

        // New API to get Bluetooth interface in new Bluetooth adapter
        @Override
        public synchronized IBluetoothA2dp getBluetoothA2dp() {
            Log.i(TAG, "BluetoothA2dpExtBinder getBluetoothA2dp");
            return mService.getBluetoothA2dp();
        }
    }
}
