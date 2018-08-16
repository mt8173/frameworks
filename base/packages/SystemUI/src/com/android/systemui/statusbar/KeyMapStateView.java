/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews.RemoteView;
import com.android.systemui.R;
@RemoteView
public class KeyMapStateView extends ImageView {
	boolean mAttached;
	StateReceiver mReceiver;

	public KeyMapStateView(Context context) {
		super(context);
	}

	public KeyMapStateView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mAttached = true;
		if (mReceiver == null) {
			mReceiver = new StateReceiver();
			IntentFilter filter=new IntentFilter();
			filter.addAction("softwin.intent.action.enablekeymap");
			Intent intent=getContext().registerReceiver(mReceiver, filter);
			if(intent!=null){
				mReceiver.onReceive(getContext(), intent);
			}
		}
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mReceiver != null) {
			getContext().unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		mAttached = false;
	}

	class StateReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			Log.i("xin_temp", "keymapping:" + intent);
			ImageView keymappingView = KeyMapStateView.this;
			boolean isEnabledHardware = intent.getBooleanExtra(
					"isEnabledHardware", true);
			if (!isEnabledHardware) {
				keymappingView
						.setImageResource(R.drawable.ic_sysbar_game_disable);
				return;
			}
			boolean show = intent.getBooleanExtra("isEnabled", false);
			if (show) {
				keymappingView.setImageResource(R.drawable.ic_sysbar_game_used);
			} else {
				keymappingView.setImageResource(R.drawable.ic_sysbar_game);
			}
		}

	}
}
