package com.android.server.policy;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import java.io.File;
import java.util.Arrays;

import com.android.internal.R;

public class KeyMappingDialog extends Dialog implements KeyMappingLayoutHelper.Action,KeyMappingInterface{
	private KeyMappingLayoutHelper helper;
	Context context;
	int width,height;
	String[] initValue =null;
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
	public KeyMappingDialog(Context context) {
		super(context,R.style.Theme_Dialog_RecentApplications);
		this.context = context;
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
	

	/**
	 * We create the recent applications dialog just once, and it stays around
	 * (hidden) until activated by the user.
	 * 
	 * @see PhoneWindowManager#showRecentAppsDialog
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Context context = getContext();

		Window window = getWindow();
		window.requestFeature(Window.FEATURE_NO_TITLE);
		window.setType(WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY);
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//window.setTitle("Recents");
		helper=new KeyMappingLayoutHelper(context,this, initValue, width, height);
		setContentView(helper.getLayout());
		helper.reset();
		final WindowManager.LayoutParams params = window.getAttributes();
		params.width = width;
		params.height = height;
		params.token = null;
		params.type = WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY;
		window.setAttributes(params);
		window.setFlags(0, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		IntentFilter filter= new IntentFilter(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		context.registerReceiver(screenReceiver, filter);
	}








	@Override
	public void dismiss() {
		super.dismiss();
		
	}
	public void cancelReceive() {
		if(context!=null){
			context.unregisterReceiver(screenReceiver);
		}
	}
	

	public void show(String value) {
	    if(!inKeyguardRestrictedInputMode()){
	    	//String value=KeyMappingManager.getCurrentValue();
	    	super.show();
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

	@Override
	public void show() {
	    if(!inKeyguardRestrictedInputMode()){
	    	super.show();
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






	

}
