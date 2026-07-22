/* Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2022-2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * Copyright 2026 Mercedes Benz Group AG
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.bluetooth.a2dp;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.car.Car;
import android.car.media.CarAudioManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;

import com.android.bluetooth.btservice.AdapterUtil;

import java.util.HashMap;
import java.util.Objects;

public final class A2dpAudioZone {
    private static final String TAG = "A2dpAudioZone";
    private static final boolean DBG = true;

    private static final int MAX_A2DP_AUDIO_ZONE = 2;

    private Context mContext;
    private BluetoothDevice mDevice;
    private AudioManager mAudioManager;

    private static HashMap<Integer, CarAudioZone> sCarAudioZone = new HashMap<>();
    private static final Object sLock = new Object();
    private static Car sCar;
    private static CarAudioManager sCarAudioManager;

    A2dpAudioZone(Context context, BluetoothDevice device) {
        mContext = Objects.requireNonNull(context);
        mDevice = Objects.requireNonNull(device);
        mAudioManager = (AudioManager) Objects.requireNonNull(
                mContext.getSystemService(Context.AUDIO_SERVICE));
    }

    public static boolean isAudioZoneAvailable(Context context) {
        synchronized (sLock) {
            if (sCarAudioZone.size() == 0) {
                initAudioZone(context);
            }
            return sCarAudioZone.size() > 0;
        }
    }

    public static void clear() {
        sCarAudioManager = null;
        if (sCar != null) {
            sCar.disconnect();
            sCar = null;
        }
    }

    private static void initAudioZone(Context context) {
        int audioZoneIndex = 0;
        CarAudioManager carAudioManager = getCarAudioManager(context);
        int audioZoneOffset;
        if (carAudioManager == null) {
            return;
        }
        // The first 2 headset audio zones are for Adapter 0
        // The last 2 headset audio zones are for Adapter 1
        if (AdapterUtil.isAdapterDefault()) {
            audioZoneOffset = 0;
        } else {
            audioZoneOffset = 2;
        }
        debugLog("audioZoneOffset: " + audioZoneOffset);
        for (int audioZoneId: carAudioManager.getAudioZoneIds()) {
            AudioDeviceInfo deviceInfo = carAudioManager
                    .getOutputDeviceForUsage(audioZoneId, AudioAttributes.USAGE_MEDIA);
            if (deviceInfo == null) {
                continue;
            }
            String address = deviceInfo.getAddress();
            debugLog("audio zone id: " + audioZoneId +
                    ", type: " + deviceInfo.getType() +
                    ", id: " + deviceInfo.getId() +
                    ", address: " + address);
            if (isA2dpAudioZone(deviceInfo)) {
                // By pass the first 2 A2dp zone
                if (audioZoneOffset > 0) {
                    audioZoneOffset = audioZoneOffset-1;
                    continue;
                }
                debugLog("Add A2DP audio zone index " + audioZoneIndex + " audioZoneId " + audioZoneId);
                sCarAudioZone.put(audioZoneIndex, new CarAudioZone(audioZoneId, address));
                ++audioZoneIndex;
                if (audioZoneIndex >= MAX_A2DP_AUDIO_ZONE)
                    break;
            }
        }
        debugLog("A2DP audio zone number: " + audioZoneIndex);
    }

    private static boolean isA2dpAudioZone(AudioDeviceInfo deviceInfo) {
        return deviceInfo.getAddress().contains("BT_HEADPHONE");
    }

    private static Car getCar(Context context) {
        if (sCar == null) {
            sCar = Car.createCar(context);
        }
        return sCar;
    }

    private static CarAudioManager getCarAudioManager(Context context) {
        if (sCarAudioManager == null) {
            Car car = getCar(context);
            if (car == null) {
                return null;
            }
            sCarAudioManager = (CarAudioManager) Objects.requireNonNull(
                    car.getCarManager(Car.AUDIO_SERVICE));
        }
        return sCarAudioManager;
    }

    public void notifyA2dpStatus(boolean connected) {
        mAudioManager.setParameters(String.format(
                "bt_a2dp: addr=%s, connected=%d",
                mDevice.getAddress(),
                connected ? 1 : 0));
    }

    public boolean setMediaPlayer(String mediaPlayer, int zoneIndex, int groupIndex) {
        if (!bindMediaPlayer(mediaPlayer, zoneIndex, groupIndex)) {
            Log.e(TAG, "setMediaPlayer: fail to bind media player: " +
                    mediaPlayer + ", in audio zone index: " + zoneIndex);
            return false;
        }
        return mapAudioZone(zoneIndex, groupIndex);
    }

    public static boolean validMediaPlayer(Context context, String mediaPlayer) {
        if (mediaPlayer.isEmpty())
            return false;
        return getApplicationInfo(context, mediaPlayer) != null;
    }

    private static ApplicationInfo getApplicationInfo(Context context, String mediaPlayer) {
        try {
            return context.getPackageManager().getApplicationInfo(mediaPlayer, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ApplicationInfo getApplicationInfo(String mediaPlayer) {
        return getApplicationInfo(mContext, mediaPlayer);
    }

    private boolean bindMediaPlayer(String mediaPlayer, int zoneIndex, int groupIndex) {
        synchronized (sLock) {
            CarAudioZone audioZone = sCarAudioZone.get(zoneIndex);
            // Don't set audio zone for media player if share audio of other zone
            audioZone.storeDevice(mDevice);
            audioZone.storeGroupBus(sCarAudioZone.get(groupIndex).getBus());
            if (zoneIndex == groupIndex) {
                audioZone.storeMediaPlayer(mDevice, mediaPlayer);
                int zone = audioZone.getZone();

                ApplicationInfo appInfo = getApplicationInfo(mediaPlayer);
                if (appInfo == null) {
                    return false;
                }
                int uid = appInfo.uid;
                debugLog("bindMediaPlayer: media player: " + mediaPlayer +
                        ", uid: " + uid + ", audio zone: " + zone);
                CarAudioManager carAudioManager = getCarAudioManager(mContext);
                return carAudioManager != null ?
                        carAudioManager.setZoneIdForUid(zone, uid) :
                        false;
            }
            return true;
        }
    }

    private boolean mapAudioZone(int zoneIndex, int groupIndex) {
        String addr = mDevice.getAddress();

        synchronized (sLock) {
            CarAudioZone audioZone = sCarAudioZone.get(zoneIndex);
            int zone = audioZone.getZone();
            String bus = audioZone.getBus();
            String groupBus;
            if (zoneIndex == groupIndex) {
                groupBus = bus;
            } else {
                CarAudioZone groupZone = sCarAudioZone.get(groupIndex);
                groupBus = groupZone.getBus();

            }
            debugLog("mapAudioZone: bt addr: " + addr + ", audio bus: " + bus);
            mAudioManager.setParameters(String.format(
                    "bdaddress=%s;bus=%s;map=true;groupBus=%s",
                    addr, bus, groupBus));
            audioZone.setMediaPlayerMapped(mDevice);
        }
        return true;
    }

    public void clearMediaPlayer() {
        clearZoneId(mDevice);
        clearMediaPlayer(mDevice);
    }

    private void clearZoneId(BluetoothDevice device) {
        debugLog("clearZoneId: device: " + device);
        synchronized (sLock) {
            for (int zoneIndex : sCarAudioZone.keySet()) {
                CarAudioZone audioZone = sCarAudioZone.get(zoneIndex);
                if (audioZone.getDevice() == null) {
                    continue;
                }

                if (audioZone.getDevice().equals(device)) {
                       debugLog("Notify A2dp hal to unmap " + audioZone.getBus());
                       mAudioManager.setParameters(String.format(
                            "bdaddress=%s;bus=%s;map=false", device.getAddress(), audioZone.getBus()));
                }

                String mediaPlayer = audioZone.getMediaPlayer(device);
                if (mediaPlayer.isEmpty()) {
                    debugLog("No mapped mediaPlayer");
                    continue;
                }

                // Clear audio zone mapping of media player's uid if there is only 1 BluetoothDevice
                if (audioZone.getMediaPlayerListSize() == 1) {
                    ApplicationInfo appInfo = getApplicationInfo(mediaPlayer);
                    if (appInfo == null) {
                        debugLog("Cannot get appInfo");
                        continue;
                    }
                    CarAudioManager carAudioManager = getCarAudioManager(mContext);
                    if (carAudioManager != null) {
                        carAudioManager.clearZoneIdForUid(appInfo.uid);
                    }
                }
            }
        }
    }

    public static void clearMediaPlayer(BluetoothDevice device) {
        synchronized (sLock) {
            for (int zoneIndex : sCarAudioZone.keySet()) {
                CarAudioZone audioZone = sCarAudioZone.get(zoneIndex);
                audioZone.clearMediaPlayer(device);
            }
        }
    }

    public static boolean isMediaPlayerMapped(BluetoothDevice device, String mediaPlayer) {
        synchronized (sLock) {
            for (int zoneIndex : sCarAudioZone.keySet()) {
                CarAudioZone audioZone = sCarAudioZone.get(zoneIndex);
                if (audioZone.isMediaPlayerMapped(device, mediaPlayer)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static int getAudioZoneAvailable(String mediaPlayer) {
        synchronized (sLock) {
            for (int zoneIndex : sCarAudioZone.keySet()) {
                CarAudioZone audioZone = sCarAudioZone.get(zoneIndex);
                int size = audioZone.getMediaPlayerListSize();
                if (size > 0) {
                    if (audioZone.existMediaPlayer(mediaPlayer)) {
                        return zoneIndex;
                    }
                } else if (size == 0) {
                    return zoneIndex;
                }
            }
        }
        // NOT found audio zone available
        return MAX_A2DP_AUDIO_ZONE;
    }

    public static int getAudioZoneAvailable(BluetoothDevice device) {
        synchronized (sLock) {
            for (int zoneIndex : sCarAudioZone.keySet()) {
                CarAudioZone audioZone = sCarAudioZone.get(zoneIndex);
                int size = audioZone.getMediaPlayerListSize();
                if (size == 0) {
                    return zoneIndex;
                }
            }
        }
        // NOT found audio zone available
        return MAX_A2DP_AUDIO_ZONE;
    }

    private static int getAudioZoneIndex(BluetoothDevice device, String mediaPlayer) {
        synchronized (sLock) {
            for (int zoneIndex : sCarAudioZone.keySet()) {
                CarAudioZone audioZone = sCarAudioZone.get(zoneIndex);
                if (audioZone.existMediaPlayer(device, mediaPlayer)) {
                    return zoneIndex;
                }
            }
        }
        // NOT found audio zone matched with media player
        return MAX_A2DP_AUDIO_ZONE;
    }

    public static boolean validZoneIndex(int zoneIndex) {
        return (zoneIndex >= 0) && (zoneIndex < MAX_A2DP_AUDIO_ZONE);
    }

    private static class CarAudioZone {
        private final int mZone;
        private final String mBus;
        private String mGroupBus;
        private BluetoothDevice mDevice;
        private final HashMap<BluetoothDevice, MediaPlayerInfo> mMediaPlayerList = new HashMap<>();

        CarAudioZone(int zone, String bus) {
            mZone = zone;
            mBus = bus;
            mGroupBus = bus;
        }

        int getZone() {
            return mZone;
        }

        String getBus() {
            return mBus;
        }

        HashMap<BluetoothDevice, MediaPlayerInfo> getMediaPlayerList() {
            return mMediaPlayerList;
        }

        int getMediaPlayerListSize() {
            return mMediaPlayerList.size();
        }

        String getMediaPlayer(BluetoothDevice device) {
            for (BluetoothDevice btDevice : mMediaPlayerList.keySet()) {
                if (btDevice.equals(device)) {
                    MediaPlayerInfo mpInfo = mMediaPlayerList.get(device);
                    return mpInfo.getMediaPlayer();
                }
            }
            // NOT found media player for device
            return "";
        }

        boolean existMediaPlayer(String mediaPlayer) {
            for (MediaPlayerInfo mpInfo : mMediaPlayerList.values()) {
                // Disallow to map different media player into same audio zone
                return mediaPlayer.equals(mpInfo.getMediaPlayer());
            }
            return false;
        }

        boolean existMediaPlayer(BluetoothDevice device, String mediaPlayer) {
            if (mMediaPlayerList.containsKey(device)) {
                if (mediaPlayer.equals(mMediaPlayerList.get(device))) {
                    return true;
                }
            }
            return false;
        }

        void storeMediaPlayer(BluetoothDevice device, String mediaPlayer) {
            if (mMediaPlayerList.containsKey(device)) {
                // Same media player
                if (mediaPlayer.equals(mMediaPlayerList.get(device))) {
                    return;
                }
                mMediaPlayerList.remove(device);
            }
            MediaPlayerInfo mpInfo = new MediaPlayerInfo(mediaPlayer);
            mMediaPlayerList.put(device, mpInfo);
        }

        void setMediaPlayerMapped(BluetoothDevice device) {
            if (mMediaPlayerList.containsKey(device)) {
                MediaPlayerInfo mpInfo = mMediaPlayerList.get(device);
                mpInfo.setMapped();
            } else {
                Log.w(TAG, "No MediaPlayerInfo for " + device);
            }
        }

        boolean isMediaPlayerMapped(BluetoothDevice device, String mediaPlayer) {
            if (mMediaPlayerList.containsKey(device)) {
                MediaPlayerInfo mpInfo = mMediaPlayerList.get(device);
                // Same media player
                if (mediaPlayer.equals(mpInfo.getMediaPlayer())) {
                    return mpInfo.isMapped();
                }
            }
            return false;
        }

        void clearMediaPlayer(BluetoothDevice device) {
            if (mMediaPlayerList.containsKey(device)) {
                debugLog("clearMediaPlayer " + device);
                mMediaPlayerList.remove(device);
            }
        }

        void storeDevice(BluetoothDevice device) {
            mDevice = device;
        }

        BluetoothDevice getDevice() {
            return mDevice;
        }

        boolean isDeviceMapped(BluetoothDevice device) {
            return device.equals(mDevice);
        }

        void storeGroupBus(String bus) {
            mGroupBus = bus;
        }

        String groupBus() {
            return mGroupBus;
        }

    }

    private static class MediaPlayerInfo {
        private final String mMediaPlayer;
        private boolean mIsMapped;

        MediaPlayerInfo(String mediaPlayer) {
            this(mediaPlayer, false);
        }

        MediaPlayerInfo(String mediaPlayer, boolean isMapped) {
            mMediaPlayer = mediaPlayer;
            mIsMapped = isMapped;
        }

        String getMediaPlayer() {
            return mMediaPlayer;
        }

        boolean isMapped() {
            return mIsMapped;
        }

        void setMapped() {
            mIsMapped = true;
        }
    }

    private static void debugLog(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }
}
