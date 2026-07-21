/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package android.bluetooth;

import android.bluetooth.IBluetoothAvrcpTarget;

/**
 * API for Bluetooth AVRCP Target Extended Interface
 *
 * @hide
 */
interface IBluetoothAvrcpTargetExt {
    /**
     * @hide
     */
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    IBluetoothAvrcpTarget getBluetoothAvrcpTarget();
}
