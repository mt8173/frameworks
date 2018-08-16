package com.android.server.policy;
import android.content.Context;

public class KeyMappingPolicy{
	public static final boolean USE_DIALAG=false;
	public static KeyMappingInterface getWindow(Context context){
		if(USE_DIALAG){
			return new KeyMappingDialog(context);
		}else{
			return  new KeyMappingView(context);
		}
		
	}

	
}
