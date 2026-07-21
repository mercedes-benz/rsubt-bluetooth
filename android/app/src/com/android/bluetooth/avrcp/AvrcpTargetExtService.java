/*
 * Copyright 2018 The Android Open Source Project
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
 * SPDX-License-Identifier: BSD-3-Clause-Clear.
 */

package com.android.bluetooth.avrcp;

import android.bluetooth.IBluetoothAvrcpTarget;
import android.bluetooth.IBluetoothAvrcpTargetExt;
import android.util.Log;

/**
 * Provides Bluetooth AVRCP Target profile as a service in the Bluetooth application.
 * @hide
 */
public class AvrcpTargetExtService extends AvrcpTargetService {
    private static final String TAG = "AvrcpTargetExtService";
    private static final boolean DEBUG = false;

    @Override
    protected IProfileServiceBinder initBinder() {
        if (DEBUG) Log.d(TAG, "initBinder");
        return new AvrcpTargetExtBinder(this);
    }

    public IBluetoothAvrcpTarget getBluetoothAvrcpTarget() {
        if (DEBUG) Log.d(TAG, "getBluetoothAvrcpTarget");
        return IBluetoothAvrcpTarget.Stub.asInterface(super.initBinder());
    }

    @Override
    public String getName() {
        return TAG;
    }

    private static class AvrcpTargetExtBinder extends IBluetoothAvrcpTargetExt.Stub
            implements IProfileServiceBinder {
        private AvrcpTargetExtService mService;

        AvrcpTargetExtBinder(AvrcpTargetExtService service) {
            mService = service;
        }

        @Override
        public void cleanup() {
            mService = null;
        }

        // New API to get Bluetooth interface in new Bluetooth adapter
        @Override
        public synchronized IBluetoothAvrcpTarget getBluetoothAvrcpTarget() {
            if (DEBUG) Log.d(TAG, "AvrcpTargetExtBinder getBluetoothAvrcpTarget");
            return mService.getBluetoothAvrcpTarget();
        }
    }
}
