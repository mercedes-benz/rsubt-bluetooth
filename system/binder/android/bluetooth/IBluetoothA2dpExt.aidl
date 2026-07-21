/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package android.bluetooth;

import android.bluetooth.IBluetoothA2dp;

/**
 * API for interacting with new A2DP
 * @hide
 */
interface IBluetoothA2dpExt
{
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    IBluetoothA2dp getBluetoothA2dp();
}
