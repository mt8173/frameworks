/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

import com.mediatek.miravision.setting.MiraVisionJni;
import com.mediatek.miravision.setting.MiraVisionJni.Range;
import com.mediatek.pq.PictureQuality;
import android.util.Log;

/** Quick settings tile: Location **/
public class MiraVisionTile extends QSTile<QSTile.BooleanState> {

    private final AnimationIcon mEnable =
            new AnimationIcon(R.drawable.ic_signal_miravision_enable_animation,
                    R.drawable.ic_signal_miravision_enable);
    private final AnimationIcon mDisable =
            new AnimationIcon(R.drawable.ic_signal_miravision_disable_animation,
                    R.drawable.ic_signal_miravision_disable); 

    public MiraVisionTile(Host host) {
        super(host);

    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }
	
	@Override
    public void setListening(boolean listening) {

    }
	
    @Override
    public Intent getLongClickIntent() {
        return new Intent("com.android.settings.MIRA_VISION");
    }

    @Override
    protected void handleClick() {
		mUiHandler.post(new Runnable() {
			@Override
            public void run() {
				mHost.startActivityDismissingKeyguard(new Intent("com.android.settings.MIRA_VISION"));
			}
		});
		
        /*int miravisionMode = Settings.System.getInt(mContext.getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        final boolean wasEnabled = miravisionMode != SCREEN_BRIGHTNESS_MODE_MANUAL;
		Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    wasEnabled ? SCREEN_BRIGHTNESS_MODE_MANUAL : SCREEN_BRIGHTNESS_MODE_AUTOMATIC);*/
        MetricsLogger.action(mContext, getMetricsCategory(), false);
		refreshState();
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_miravision_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
		int miravisionMode = Settings.System.getInt(mContext.getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        final boolean wasEnabled = false;//miravisionMode != SCREEN_BRIGHTNESS_MODE_MANUAL;
        if (wasEnabled) {
            state.icon = mEnable;
        } else {
            state.icon = mDisable;
        }
		state.label = mContext.getString(R.string.quick_settings_miravision_label);
        state.contentDescription = mContext.getString(
                    R.string.quick_settings_miravision_label);
        state.minimalAccessibilityClassName = state.expandedAccessibilityClassName
                = Switch.class.getName();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_LOCATION;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.quick_settings_miravision_label);
        } else {
            return mContext.getString(R.string.quick_settings_miravision_label);
        }
    }
}
