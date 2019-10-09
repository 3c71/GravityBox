/*
 * Copyright (C) 2018 Peter Gregus for GravityBox Project (C3C076@xda)
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
package com.ceco.q.gravitybox.managers;

import de.robv.android.xposed.XSharedPreferences;

import com.ceco.q.gravitybox.GravityBox;
import com.ceco.q.gravitybox.GravityBoxSettings;
import com.ceco.q.gravitybox.PhoneWrapper;
import com.ceco.q.gravitybox.ledcontrol.QuietHoursActivity;
import com.ceco.q.gravitybox.tuner.TunerMainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

public class SysUiManagers {
    private static final String TAG = "GB:SysUiManagers";

    public static SysUiBatteryInfoManager BatteryInfoManager;
    public static SysUiStatusBarIconManager IconManager;
    public static SysUiStatusbarQuietHoursManager QuietHoursManager;
    public static SysUiAppLauncher AppLauncher;
    public static SysUiKeyguardStateMonitor KeyguardMonitor;
    public static SysUiFingerprintLauncher FingerprintLauncher;
    public static SysUiNotificationDataMonitor NotifDataMonitor;
    public static SysUiGpsStatusMonitor GpsMonitor;
    public static SysUiSubscriptionManager SubscriptionMgr;
    public static SysUiTunerManager TunerMgr;
    public static SysUiPackageManager PackageMgr;
    public static SysUiConfigChangeMonitor ConfigChangeMonitor;

    public static void init(Context context, XSharedPreferences prefs, XSharedPreferences qhPrefs, XSharedPreferences tunerPrefs) {
        if (context == null)
            throw new IllegalArgumentException("Context cannot be null");
        if (prefs == null)
            throw new IllegalArgumentException("Prefs cannot be null");

        try {
            ConfigChangeMonitor = new SysUiConfigChangeMonitor(context);
        } catch (Throwable t) {
            GravityBox.log(TAG, "Error creating ConfigurationChangeMonitor: ", t);
        }

        createKeyguardMonitor(context, prefs);

        try {
            BatteryInfoManager = new SysUiBatteryInfoManager(context, prefs, qhPrefs);
        } catch (Throwable t) {
            GravityBox.log(TAG, "Error creating BatteryInfoManager: ", t);
        }

        try {
            IconManager = new SysUiStatusBarIconManager(context);
        } catch (Throwable t) {
            GravityBox.log(TAG, "Error creating IconManager: ", t);
        }

        try {
            QuietHoursManager = SysUiStatusbarQuietHoursManager.getInstance(context, qhPrefs);
        } catch (Throwable t) {
            GravityBox.log(TAG, "Error creating QuietHoursManager: ", t);
        }

        try {
            AppLauncher = new SysUiAppLauncher(context, prefs);
            if (ConfigChangeMonitor != null) {
                ConfigChangeMonitor.addConfigChangeListener(AppLauncher);
            }
        } catch (Throwable t) {
            GravityBox.log(TAG, "Error creating AppLauncher: ", t);
        }

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FINGERPRINT_LAUNCHER_ENABLE, false)) {
            try {
                FingerprintLauncher = new SysUiFingerprintLauncher(context, prefs);
            } catch (Throwable t) {
                GravityBox.log(TAG, "Error creating FingerprintLauncher: ", t);
            }
        }

        try {
            NotifDataMonitor = new SysUiNotificationDataMonitor(context);
        } catch (Throwable t) {
            GravityBox.log(TAG, "Error creating NotificationDataMonitor: ", t);
        }

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_QUICK_SETTINGS_ENABLE, false)) {
            try {
                GpsMonitor = new SysUiGpsStatusMonitor(context);
            } catch (Throwable t) {
                GravityBox.log(TAG, "Error creating GpsStatusMonitor: ", t);
            }
        }

        if (PhoneWrapper.hasMsimSupport()) {
            try {
                SubscriptionMgr = new SysUiSubscriptionManager(context);
            } catch (Throwable t) {
                GravityBox.log(TAG, "Error creating SubscriptionManager: ", t);
            }
        }

        if (tunerPrefs.getBoolean(TunerMainActivity.PREF_KEY_ENABLED, false) &&
                !tunerPrefs.getBoolean(TunerMainActivity.PREF_KEY_LOCKED, false)) {
            try {
                TunerMgr = new SysUiTunerManager(context);
            } catch (Throwable t) {
                GravityBox.log(TAG, "Error creating TunerManager: ", t);
            }
        }

        try {
            PackageMgr = new SysUiPackageManager(context);
        } catch (Throwable t) {
            GravityBox.log(TAG, "Error creating PackageManager: ", t);
        }

        IntentFilter intentFilter = new IntentFilter();

        // Configuration change monitor
        intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);

        // battery info manager
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_SOUND_CHANGED);
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_LOW_BATTERY_WARNING_POLICY_CHANGED);
        intentFilter.addAction(SysUiBatteryInfoManager.ACTION_POWER_SAVE_MODE_CHANGING);

        // quiet hours manager
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(QuietHoursActivity.ACTION_QUIET_HOURS_CHANGED);

        // AppLauncher
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_APP_LAUNCHER_CHANGED);
        intentFilter.addAction(SysUiAppLauncher.ACTION_SHOW_APP_LAUCNHER);

        // KeyguardStateMonitor
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_POWER_CHANGED);
        intentFilter.addAction(GravityBoxSettings.ACTION_LOCKSCREEN_SETTINGS_CHANGED);

        // FingerprintLauncher
        if (FingerprintLauncher != null) {
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);
            intentFilter.addAction(GravityBoxSettings.ACTION_FPL_SETTINGS_CHANGED);
        }

        // GpsStatusMonitor
        if (GpsMonitor != null) {
            intentFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
            intentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);
        }

        // SubscriptionManager
        if (SubscriptionMgr != null) {
            intentFilter.addAction(SysUiSubscriptionManager.ACTION_CHANGE_DEFAULT_SIM_SLOT);
            intentFilter.addAction(SysUiSubscriptionManager.ACTION_GET_DEFAULT_SIM_SLOT);
        }

        // TunerManager
        if (TunerMgr != null) {
            intentFilter.addAction(SysUiTunerManager.ACTION_GET_TUNEABLES);
        }

        context.registerReceiver(sBroadcastReceiver, intentFilter);
    }

    public static void createKeyguardMonitor(Context ctx, XSharedPreferences prefs) {
        if (KeyguardMonitor != null) return;
        try {
            KeyguardMonitor = new SysUiKeyguardStateMonitor(ctx, prefs);
        } catch (Throwable t) {
            GravityBox.log(TAG, "Error creating KeyguardMonitor: ", t);
        }
    }

    private static BroadcastReceiver sBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConfigChangeMonitor != null) {
                ConfigChangeMonitor.onBroadcastReceived(context, intent);
            }
            if (BatteryInfoManager != null) {
                BatteryInfoManager.onBroadcastReceived(context, intent);
            }
            if (QuietHoursManager != null) {
                QuietHoursManager.onBroadcastReceived(context, intent);
            }
            if (AppLauncher != null) {
                AppLauncher.onBroadcastReceived(context, intent);
            }
            if (KeyguardMonitor != null) {
                KeyguardMonitor.onBroadcastReceived(context, intent);
            }
            if (FingerprintLauncher != null) {
                FingerprintLauncher.onBroadcastReceived(context, intent);
            }
            if (GpsMonitor != null) {
                GpsMonitor.onBroadcastReceived(context, intent);
            }
            if (SubscriptionMgr != null) {
                SubscriptionMgr.onBroadcastReceived(context, intent);
            }
            if (TunerMgr != null) {
                TunerMgr.onBroadcastReceived(context, intent);
            }
        }
    };
}
