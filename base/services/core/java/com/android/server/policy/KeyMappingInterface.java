package com.android.server.policy;
import android.content.Context;

public interface KeyMappingInterface {

	boolean isShowing();

	void dismiss();

	void show(String value);

	void cancelReceive();

	Context getRealContext();

}
