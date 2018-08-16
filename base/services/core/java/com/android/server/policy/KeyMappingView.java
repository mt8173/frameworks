package com.android.server.policy;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.io.File;
import java.util.Arrays;

import com.android.internal.R;

public class KeyMappingView implements KeyMappingLayoutHelper.Action,KeyMappingInterface{
	private KeyMappingLayoutHelper helper;
	Context context;
	int width,height;
	String[] initValue =null;
	private boolean isShowing=false;
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams params;
	private View view;
	private BroadcastReceiver screenReceiver=new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {
			String action=intent.getAction();
			if(Intent.ACTION_SCREEN_OFF.equals(action)){
				dismiss();
				KeyMappingManager.getInstance().closeKeymapping();
			}else if(Intent.ACTION_USER_PRESENT.equals(action)){
				KeyMappingManager.getInstance().openKeymapping(KeyMappingManager.getTopPackageName(context));
			}
		};
	};
	
	public Context getRealContext(){
		return this.context;
	}
	public KeyMappingView(Context context) {
		this.context = context;
		mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		readConfig();
		IntentFilter filter= new IntentFilter(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		context.registerReceiver(screenReceiver, filter);
		
	}
	public void readConfig(){
		try {
			File file = new File("/system/xbin/softwin_config.properties");
			if (file.exists()) {
				java.io.FileInputStream input = new java.io.FileInputStream(file);
				java.util.Properties properties=new java.util.Properties();
				properties.load(input);
				String initValueString=properties.getProperty("ro.softwin.gamekey.default", null);
				String widthString=properties.getProperty("ro.softwin.layout.width", "1024");
				String heightString=properties.getProperty("ro.softwin.layout.height", "600");
				input.close();
				String[] position=initValueString.trim().split(" {1,}");
				if(position.length<31){
					initValue=null;
				}else{
					initValue=position;
				}
				width=Integer.parseInt(widthString);
				height=Integer.parseInt(heightString);
			}
		} catch (Exception e) {
			Log.i("xin_log","init gamekey.default exception,initValue="+initValue);
			width=1024;
			height=600;
			initValue=null;
		}
		Log.i("xin_log","loading initValue="+Arrays.toString(initValue));
	}
	public void buildParamsIfNecessary(){
		if(params==null){
	        params = new WindowManager.LayoutParams(
	        		WindowManager.LayoutParams.MATCH_PARENT,
	        		WindowManager.LayoutParams.MATCH_PARENT,
	                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
	                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
	                    | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
	                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
	                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
	                PixelFormat.TRANSLUCENT);

	        params.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

	        params.gravity = Gravity.FILL_HORIZONTAL|Gravity.FILL_VERTICAL;
	        params.setTitle("StatusBar");
	        params.packageName = "com.android.systemui";

		}
	}
	public void buildParamsIfNecessary2(){
		if(params==null){
			params = new WindowManager.LayoutParams();
			params.type=WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL;
			//params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
			params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
			//params.width = WindowManager.LayoutParams.MATCH_PARENT;
			//params.height = WindowManager.LayoutParams.MATCH_PARENT;
			params.width = width;
			params.height = height;
			params.format=PixelFormat.TRANSLUCENT;
		}
	}
	

	@Override
	public void dismiss() {
		dismissLayout();
		
	}
	
	public void cancelReceive() {
		if(context!=null){
			context.unregisterReceiver(screenReceiver);
		}
	}

	public void show(String value) {
	    if(!inKeyguardRestrictedInputMode()){
	    	//String value=KeyMappingManager.getCurrentValue();
	    	showLayout();
	    	if(value!=null&&!"0".equals(value)){
				try {
					String[] positon=value.trim().split(" {1,}");
					if(positon.length>=31){
						helper.parsePosition(positon);
					}
				} catch (Exception e) {
					Log.e("xin_log","parse keymap fail,"+e.getMessage());
					helper.parseDefault();
				}
	    	}else{
	    		helper.parseDefault();
	    	}
	    }
	}

	public void show() {
	    if(!inKeyguardRestrictedInputMode()){
	    	showLayout();
	    	helper.parseDefault();
	    }
	}
	private boolean inKeyguardRestrictedInputMode() {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return km.inKeyguardRestrictedInputMode();
    }
	
	@Override
	public void enable(String value) {
		dismiss();
		KeyMappingManager.saveCurrentMapping(context, value);
		KeyMappingManager.getInstance().openKeymapping(KeyMappingManager.getTopPackageName(context));
	}
	
	@Override
	public void disable() {
		dismiss();
		KeyMappingManager.cleanCurrentMapping(context);
		KeyMappingManager.getInstance().closeKeymapping();
	}
	@Override
	public boolean isShowing() {
		return isShowing;
	}
	public void showLayout(){
		isShowing=true;
		if(view==null){
			helper=new KeyMappingLayoutHelper(context,this, initValue, width, height);
			view=helper.getLayout();
			helper.reset();
		}
		buildParamsIfNecessary();
		if(view.getParent()==null){
			mWindowManager.addView(view, params);
		}

	}
	private void dismissLayout() {
		isShowing=false;
		if(view!=null&&view.getParent()!=null){
			mWindowManager.removeView(view);
		}
	}






	

}
