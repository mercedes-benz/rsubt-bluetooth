/* Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2022-2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.bluetooth.btservice;

import android.annotation.NonNull;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapterCommon;
import android.bluetooth.BluetoothAdapterExt;
import android.bluetooth.BluetoothAdapterUtil;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.Settings;

import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.a2dp.A2dpExtService;
import com.android.bluetooth.avrcp.AvrcpTargetService;
import com.android.bluetooth.avrcp.AvrcpTargetExtService;

import com.android.bluetooth.gatt.GattService;
import com.android.bluetooth.gatt.GattExtService;
import com.android.bluetooth.opp.BluetoothOppService;
import com.android.bluetooth.R;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Bluetooth adapter utility
 */
public final class AdapterUtil {
    private static final String TAG = "AdapterUtil";

    private static final int ADAPTER_DEFAULT = BluetoothAdapterCommon.ADAPTER_DEFAULT;
    private static final int ADAPTER_1 = BluetoothAdapterCommon.ADAPTER_1;
    private static final int ADAPTER_NUMBER = BluetoothAdapterCommon.ADAPTER_NUMBER;
    // @link BluetoothClass.Device.Major.BITMASK
    private static final int DEFAULT_BLUETOOTH_CLASS = 0x1F00;

    private static Context sContext = null;
    private static boolean sDualBluetooth = false;
    private static int sAdapterIndex = ADAPTER_DEFAULT;
    private static BluetoothAdapter sAdapter = null;
    private static boolean sDualAdapterMode = false;
    private static boolean sFilterDevice = false;
    private static String sCounterpartAddress = null;
    private static HashMap<Integer, ArrayList<Integer>> sProfiles;

    private static boolean sIsA2dpSinkRole = true;

    public static void init(@NonNull Context context) {
        sContext = context;
        sDualBluetooth = SystemProperties.getBoolean("persist.bluetooth.dual_bt", false);
        sAdapterIndex = Application.getProcessName().equals(sContext.getPackageName()) ?
               ADAPTER_DEFAULT : ADAPTER_1;
        sAdapter = getAdapter(sAdapterIndex);
        sDualAdapterMode = SystemProperties.getBoolean("persist.bluetooth.dual_adapter_mode", false);
        sFilterDevice = getFilterDeviceConfig();
        if (isDualAdapterMode() && isAdapterDefault()) {
            // In dual adapter mode, default adapter needs to monitor
            // new adapter's state.
            AdapterExt.create(sContext);
        }

        sIsA2dpSinkRole = SystemProperties.getBoolean("persist.bluetooth.adapter0.isA2dpSink", true);

        // Init profile supported in Bluetooth adapter
        sProfiles = new HashMap<Integer, ArrayList<Integer>>(ADAPTER_NUMBER);
        ArrayList<Integer> profileArray = new ArrayList<Integer>(Arrays.asList(
                BluetoothProfile.GATT,
                BluetoothProfile.GATT_SERVER));
        if (sIsA2dpSinkRole) {
            profileArray.add(BluetoothProfile.A2DP_SINK);
            profileArray.add(BluetoothProfile.AVRCP_CONTROLLER);
            profileArray.add(BluetoothProfile.HEADSET_CLIENT);
            profileArray.add(BluetoothProfile.PBAP_CLIENT);
            profileArray.add(BluetoothProfile.HID_HOST);
            profileArray.add(BluetoothProfile.MAP_CLIENT);
            profileArray.add(BluetoothProfile.PAN);
        } else {
            profileArray.add(BluetoothProfile.A2DP);
            profileArray.add(BluetoothProfile.AVRCP);
            profileArray.add(BluetoothProfile.HID_HOST);
        }
        sProfiles.put(ADAPTER_DEFAULT, profileArray);
        sProfiles.put(ADAPTER_1, new ArrayList<Integer>(Arrays.asList(
                BluetoothProfile.A2DP,
                BluetoothProfile.AVRCP,
                BluetoothProfile.GATT,
                BluetoothProfile.GATT_SERVER)));
    }

    private static boolean getFilterDeviceConfig() {
        return sDualBluetooth &&
                sContext.getResources().getBoolean(R.bool.filter_device);
    }

    public static boolean isDualBluetoothEnabled() {
        return sDualBluetooth;
    }

    public static int getAdapterIndex() {
        return sAdapterIndex;
    }

    public static boolean isAdapterDefault() {
        return isAdapterDefault(getAdapterIndex());
    }

    private static boolean isAdapterDefault(int adapterIndex) {
        return BluetoothAdapterCommon.isAdapterDefault(adapterIndex);
    }

    public static boolean isAdapterDefault(BluetoothDevice device) {
        return isAdapterDefault(device.getAdapterIndex());
    }

    public static boolean isAdapter1() {
        return isAdapter1(getAdapterIndex());
    }

    private static boolean isAdapter1(int adapterIndex) {
        return BluetoothAdapterCommon.isAdapter1(adapterIndex);
    }

    public static BluetoothAdapter getAdapter() {
        return sAdapter;
    }

    public static Intent newIntent(String action, String newAction) {
        return new Intent(isAdapter1() ? newAction : action);
    }

    public static boolean isProfileSupported(int profileId) {
        return sProfiles.get(sAdapterIndex).contains(profileId);
    }

    public static boolean isProfileSupported(long supportedProfiles, int profileId) {
        return (supportedProfiles & (1 << profileId)) != 0;
    }

    public static Class getGattServiceClass() {
        return isAdapter1() ? GattExtService.class : GattService.class;
    }

    public static Class getA2dpServiceClass() {
        return isAdapter1() ? A2dpExtService.class : A2dpService.class;
    }

    public static Class getAvrcpTargetServiceClass() {
        return isAdapter1() ? AvrcpTargetExtService.class : AvrcpTargetService.class;
    }

    public static boolean isDualAdapterMode() {
        return sDualBluetooth && sDualAdapterMode;
    }

    public static boolean filterDevice(BluetoothDevice device) {
        return sFilterDevice ? isCounterpartDevice(device) : false;
    }

    public static boolean isCounterpartDevice(BluetoothDevice device) {
        if (sCounterpartAddress == null) {
            sCounterpartAddress = getCounterpartAddress();
        }
        return sCounterpartAddress != null ?
                device.getAddress().equals(sCounterpartAddress) :
                false;
    }

    private static String getCounterpartAddress() {
        return isAdapter1() ? getAddress(ADAPTER_DEFAULT) : getAddress(ADAPTER_1);
    }

    private static int getCounterpartIndex() {
        return isAdapterDefault() ? ADAPTER_1 : ADAPTER_DEFAULT;
    }

    private static String getAddress(int adapterIndex) {
        BluetoothAdapter adapter = getAdapter(adapterIndex);
        return adapter != null ? adapter.getAddress() : null;
    }

    private static BluetoothAdapter getAdapter(int adapterIndex) {
        return BluetoothAdapterUtil.getAdapter(adapterIndex);
    }

    public static BluetoothDevice getCounterpartDevice(BluetoothDevice device) {
        BluetoothAdapter counterpartAdapter = BluetoothAdapterUtil.getAdapter(getCounterpartIndex());
        return counterpartAdapter.getRemoteDevice(device.getAddress());
    }

    public static int getDefaultBluetoothClass() {
        return DEFAULT_BLUETOOTH_CLASS;
    }
}
