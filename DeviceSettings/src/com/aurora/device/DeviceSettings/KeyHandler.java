/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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
 */

package com.aurora.device.DeviceSettings;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.IBinder;
import android.os.UEventObserver;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import androidx.preference.PreferenceManager;

import com.aurora.device.DeviceSettings.Constants;
import com.aurora.device.DeviceSettings.DeviceSettings;

public class KeyHandler extends Service {
    private AudioManager mAudioManager;
    private NotificationManager mNotificationManager;
    private Vibrator mVibrator;
    private UEventObserver mAlertSliderEventObserver;
    private BroadcastReceiver mBroadcastReceiver;
    private SharedPreferences mSharedPrefereces;

    private boolean wasMuted = false;

    // Vibration effects
    private static final VibrationEffect MODE_NORMAL_EFFECT = VibrationEffect.get(VibrationEffect.EFFECT_HEAVY_CLICK);
    private static final VibrationEffect MODE_VIBRATION_EFFECT = VibrationEffect.get(VibrationEffect.EFFECT_DOUBLE_CLICK);

    // Slider key positions
    private static final int POSITION_TOP = 1;
    private static final int POSITION_MIDDLE = 2;
    private static final int POSITION_BOTTOM = 3;

    // ZEN constants
    private static final int ZEN_OFFSET = 2;
    private static final int ZEN_PRIORITY_ONLY = 3;
    private static final int ZEN_TOTAL_SILENCE = 4;
    private static final int ZEN_ALARMS_ONLY = 5;

    // Preference keys
    private static final String ALERT_SLIDER_TOP_KEY = "config_top_position";
    private static final String ALERT_SLIDER_MIDDLE_KEY = "config_middle_position";
    private static final String ALERT_SLIDER_BOTTOM_KEY = "config_bottom_position";
    private static final String MUTE_MEDIA_WITH_SILENT = "config_mute_media";

    private final String TAG = "KeyHandler";

    @Override
    public void onCreate() {
        super.onCreate();

        mAudioManager = this.getSystemService(AudioManager.class);
        mVibrator = this.getSystemService(Vibrator.class);
        mNotificationManager = this.getSystemService(NotificationManager.class);
        mSharedPrefereces = PreferenceManager.getDefaultSharedPreferences(this);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int stream = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                boolean state = intent.getBooleanExtra(AudioManager.EXTRA_STREAM_VOLUME_MUTED, false);
                if (stream == AudioSystem.STREAM_MUSIC && state == false) {
                    wasMuted = false;
                }
            }
        };

        mAlertSliderEventObserver = new UEventObserver() {
            @Override
            public void onUEvent(UEvent event) {
                String switchEvent = event.get("SWITCH_STATE");
                if (switchEvent != null) {
                    handleMode(Integer.parseInt(switchEvent));
                    return;
                }
                String state = event.get("STATE");
                if (state != null) {
                    boolean none = state.contains("USB=0");
                    boolean vibration = state.contains("HOST=0");
                    boolean silent = state.contains("null)=0");

                    if (none && !vibration && !silent) {
                        handleMode(POSITION_BOTTOM);
                    } else if (!none && vibration && !silent) {
                        handleMode(POSITION_MIDDLE);
                    } else if (!none && !vibration && silent) {
                        handleMode(POSITION_TOP);
                    }

                    return;
                }
            }
        };

        this.registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(AudioManager.STREAM_MUTE_CHANGED_ACTION)
        );

        mAlertSliderEventObserver.startObserving("tri-state-key");
        mAlertSliderEventObserver.startObserving("tri_state_key");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void vibrateIfNeeded(int mode) {
        switch (mode) {
            case AudioManager.RINGER_MODE_VIBRATE:
                mVibrator.vibrate(MODE_VIBRATION_EFFECT);
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                mVibrator.vibrate(MODE_NORMAL_EFFECT);
                break;
            default:
                return;
        }
        return;
    }

    private void handleMode(int position) {
        boolean muteMedia = mSharedPrefereces.getBoolean(MUTE_MEDIA_WITH_SILENT, false);
        int mode;
        switch (position) {
            case POSITION_TOP:
                mode = Integer.parseInt(mSharedPrefereces.getString(ALERT_SLIDER_TOP_KEY, "0"));
                break;
            case POSITION_MIDDLE:
                mode = Integer.parseInt(mSharedPrefereces.getString(ALERT_SLIDER_MIDDLE_KEY, "1"));
                break;
            case POSITION_BOTTOM:
                mode = Integer.parseInt(mSharedPrefereces.getString(ALERT_SLIDER_BOTTOM_KEY, "2"));
                break;
            default:
                return;
        }

        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_OFF, null, TAG);
                mAudioManager.setRingerModeInternal(mode);
                if (muteMedia) {
                    mAudioManager.adjustVolume(AudioManager.ADJUST_MUTE, 0);
                    wasMuted = true;
                }
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
            case AudioManager.RINGER_MODE_NORMAL:
                mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_OFF, null, TAG);
                mAudioManager.setRingerModeInternal(mode);
                if (muteMedia && wasMuted) {
                    mAudioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, 0);
                }
                break;
            case ZEN_PRIORITY_ONLY:
            case ZEN_TOTAL_SILENCE:
            case ZEN_ALARMS_ONLY:
                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
                mNotificationManager.setZenMode(mode - ZEN_OFFSET, null, TAG);
                if (muteMedia && wasMuted) {
                    mAudioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, 0);
                }
                break;
            default:
                return;
        }
        vibrateIfNeeded(mode);
    }
}
