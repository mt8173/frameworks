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

import android.content.Intent;
import android.os.UserManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.screenshot.GlobalScreenshot;

import android.util.Log;
/** Quick settings tile: Location **/
public class ScreenshotTile extends QSTile<QSTile.BooleanState> {

    private final AnimationIcon mEnable =
            new AnimationIcon(R.drawable.ic_signal_screenshot_enable_animation,
                    R.drawable.ic_signal_screenshot_enable);
    private final AnimationIcon mDisable =
            new AnimationIcon(R.drawable.ic_signal_screenshot_disable_animation,
                    R.drawable.ic_signal_screenshot_disable);

	private static final String TAG = "ScreenShotTile";  
    private static final int SCREEN_SHOT_MESSAGE = 10000;  
    private static GlobalScreenshot mScreenshot;  
      
    Handler mHandler = new Handler() {  
        public void handleMessage(Message msg) {  
            switch (msg.what) {  
            case SCREEN_SHOT_MESSAGE:  
                final Messenger callback = msg.replyTo;  
                if (mScreenshot == null) {  
                    mScreenshot = new GlobalScreenshot(mContext);  
                }  
                mScreenshot.takeScreenshot(new Runnable() {  
                    @Override public void run() {  
                        Message reply = Message.obtain(null, 1);  
                        try {  
                            if(callback != null){  
                                callback.send(reply);  
                            }  
                        }catch(RemoteException e){  
                        }  
                    }  
                }, true, true);  
                break;                      
            default:  
                break;  
            }  
        }  
    };  
      
    public ScreenshotTile(Host host) {
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
    protected void handleClick() {  
		mHost.collapsePanels();
        Message msg = mHandler.obtainMessage(SCREEN_SHOT_MESSAGE);  
        mHandler.sendMessageDelayed(msg,500);  
    }  
	
    @Override  
    protected void handleLongClick() {  
		mHost.collapsePanels();
        Message msg = mHandler.obtainMessage(SCREEN_SHOT_MESSAGE);  
        mHandler.sendMessageDelayed(msg,500);  
    }  

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_DISPLAY_SETTINGS);
    }
	
    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_screenshot_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (false) {
            state.icon = mEnable;
            state.label = mContext.getString(R.string.quick_settings_screenshot_label);
            state.contentDescription = mContext.getString(
                    R.string.accessibility_quick_settings_location_on);
        } else {
            state.icon = mDisable;
            state.label = mContext.getString(R.string.quick_settings_screenshot_label);
            state.contentDescription = mContext.getString(
                    R.string.accessibility_quick_settings_location_off);
        }
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
            return mContext.getString(R.string.accessibility_quick_settings_location_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_location_changed_off);
        }
    }

}
