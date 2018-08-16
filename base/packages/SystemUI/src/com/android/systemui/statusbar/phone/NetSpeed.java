package com.android.systemui.statusbar.phone;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NetSpeed {
	private long lastTotalRxBytes = 0;
	private long lastTimeStamp = 0;
	private Timer mTimer = null;
	private Context mContext;
	private static NetSpeed mNetSpeed;
	private Handler mHandler;

	private static final int NETSPEEDSINGNAL=1;
	private NetSpeed(Context mContext, Handler mHandler) {
		this.mContext = mContext;
		this.mHandler = mHandler;
	}

	public static NetSpeed getInstance(Context mContext, Handler mHandler) {
		if (mNetSpeed == null) {
			mNetSpeed = new NetSpeed(mContext, mHandler);
		}
		return mNetSpeed;
	}

	private long getNetworkRxBytes() {
		int currentUid = getUid();
		long net =TrafficStats.getTotalRxBytes();
		return  TrafficStats.getUidRxBytes(currentUid)==TrafficStats.UNSUPPORTED ? 0 :(net/1024);//转为KB
	}

	public String getNetSpeed() {

		long nowTotalRxBytes = getNetworkRxBytes();
	    long nowTimeStamp = System.currentTimeMillis();

	    double speed =((double )((nowTotalRxBytes - lastTotalRxBytes) * 1000)) / (double)(nowTimeStamp - lastTimeStamp);//毫秒转换
	   
	    lastTimeStamp = nowTimeStamp;
	    lastTotalRxBytes = nowTotalRxBytes;
	    String kb;
	    if (speed>999) {
			kb=String.valueOf(String.format("%.2f", speed/1024)) + " M/s";
		}else {
			kb=String.valueOf(String.format("%.2f", speed)) + " KB/s";
		}
		return kb;
	}

	public void startCalculateNetSpeed() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		if (mTimer == null) {
			mTimer = new Timer();
			mTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					Message msg = new Message();
					msg.what = 1;
					msg.obj = getNetSpeed();
					mHandler.sendMessage(msg);
				}
			}, 1000, 1000);
		}
	}

	public void stopCalculateNetSpeed() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
	}

	private int getUid() {
		try {
			PackageManager pm = mContext.getPackageManager();
			ApplicationInfo ai = pm.getApplicationInfo(
					mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
			return ai.uid;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return -1;
	}
}
