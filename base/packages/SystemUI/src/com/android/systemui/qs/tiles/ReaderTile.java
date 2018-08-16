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
public class ReaderTile extends QSTile<QSTile.BooleanState> {

    private final AnimationIcon mEnable =
            new AnimationIcon(R.drawable.ic_signal_reader_enable_animation,
                    R.drawable.ic_signal_reader_enable);
    private final AnimationIcon mDisable =
            new AnimationIcon(R.drawable.ic_signal_reader_disable_animation,
                    R.drawable.ic_signal_reader_disable); 

    public ReaderTile(Host host) {
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
        return new Intent(Settings.ACTION_DISPLAY_SETTINGS);
    }

    @Override
    protected void handleClick() {
		int readerMode = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0);
        final boolean wasEnabled = readerMode == 0;
		Log.i("xiao","readerMode:"+readerMode);
		
		if (!wasEnabled) {
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0);
        } else {
			mUiHandler.post(new Runnable() {
				@Override
                public void run() {
					Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 1);
					Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, 0);
				}
			});
		}

        MetricsLogger.action(mContext, getMetricsCategory(), !wasEnabled);
		refreshState();
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_reader_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
		int readerMode = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0);
        final boolean wasEnabled = readerMode != 0;
        if (wasEnabled) {
            state.icon = mEnable;
        } else {
            state.icon = mDisable;
        }
		state.label = mContext.getString(R.string.quick_settings_reader_label);
        state.contentDescription = mContext.getString(
                    R.string.quick_settings_reader_label);
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
            return mContext.getString(R.string.quick_settings_reader_label);
        } else {
            return mContext.getString(R.string.quick_settings_reader_label);
        }
    }
}
