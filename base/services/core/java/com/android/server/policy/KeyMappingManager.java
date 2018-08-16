package com.android.server.policy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.HashSet;
import android.view.KeyEvent;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class KeyMappingManager {
	public static final String TAG="xin_log";
	public static final int key=KeyEvent.KEYCODE_F12;
	public static final String ACTION="softwin.intent.action.keymapping";
	public static final String ACTION_UPDATE="softwin.intent.action.keymapping_update";
	public static final String ACTION_UI = "softwin.intent.action.showkeymap";
        public static final String ACTION_NOTIFY_ENABLED = "softwin.intent.action.enablekeymap";
        public static final String FIELD_ENABLED="isEnabled";
	public static final String FIELD_ENABLED_HARDWARE="isEnabledHardware";
	public static final String FIELD_PACKAGE="packageName";
	public static final String FIELD_CLOSE="isClosing";
	public static final String FIELD_SHOW="show";
	public static final String FIELD_AUTO="auto";
	public static final String FIELD_FILE="file";
	public static KeyMappingManager mSelf;
	private final String filePath = "/data/data/com.android.settings/keymapping.properties";
	private final String initPath = "/system/xbin/keymapping.properties";
	private final String exeFilePath = "/system/xbin/keymapping.sh";
	private Properties properties;
	private boolean isEnable;
	private Context buildContext;
	private KeyMappingInterface keyMappingView;
	private static  HashSet<String> whiteList=new HashSet<String>();
	static{
		whiteList.add("com.android.launcher");
		whiteList.add("com.android.launcher3");
		whiteList.add("com.android.settings");
		whiteList.add("com.softwin.gbox.home");
		whiteList.add("com.joyemu.fbaapp.gpd");
		whiteList.add("paulscode.android.mupen64plusae.softwin");
		whiteList.add("game.emulator.gba");
		whiteList.add("com.softwin.emulator.collection");
		whiteList.add("com.reicast.emulator.gpd");
		whiteList.add("com.softwin.gbox.settings");
		whiteList.add("com.android.systemui");
	}
	private boolean isHardwareNormal=false;
	private boolean isHardwareNormal(){
		if(!isHardwareNormal){
			File file=new File("/sys/devices/soc/soc:joystick@/selftest");
			//File file=new File("/dev/selftest");
			byte[] buffer=new byte[128];
			FileInputStream reader=null;
			try {
				reader = new FileInputStream(file);
				reader.read(buffer, 0, 128);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(reader!=null)
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			String string=new String(buffer);
			isHardwareNormal=string!=null&&"1".equals(string.trim());
			Log.i("xin_log","check hardware,buffer="+java.util.Arrays.toString(buffer)+",s="+string);
		}
		return isHardwareNormal;
	}
	private final BroadcastReceiver mKeyMappingReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String packageName = intent.getStringExtra(FIELD_PACKAGE);
			boolean isClosing = intent.getBooleanExtra(FIELD_CLOSE, false);
			if (isClosing || packageName == null) {
				closeKeymapping();
			} else {
				openKeymapping(packageName);
			}
		}		
	};
	
	private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String filePath = intent.getStringExtra(FIELD_FILE);
			if(filePath!=null&&!"".equals(filePath.trim())){
				File file=new File(filePath);
				if(file.exists()&&file.canRead()){
					try {
						FileInputStream input = new FileInputStream(file);
						properties.load(input);
						input.close();
						writeAll();
					} catch (IOException e) {
						Log.d(TAG,"update-file," + e.getMessage());
					}
				}else{
					Log.d(TAG, "update-file state is no-exists or no-read,path="+filePath);
				}
				return;
			}else{
				Log.i("TAG","update-file,no file");
			}
			
		}		
	};
	
	
	private final  BroadcastReceiver mUIKeyMappingReceiver=new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean show = intent.getBooleanExtra(FIELD_SHOW, true);
			boolean auto = intent.getBooleanExtra(FIELD_AUTO, true);
			KeyMappingManager kmm=getInstance();
			String pkg=getTopPackageName(context);
			Log.i(TAG,"uireceiver,showLayout,pkg="+pkg);
			String value=kmm.getValue(pkg);
			KeyMappingInterface dialog=kmm.getKeyMappingView(context);
			if(auto){
				if(dialog.isShowing()){
					dialog.dismiss();
				}else{
					dialog.show(value);
				}
			}else{
				if(show){
					dialog.show(value);
				}else{
					dialog.dismiss();
				}
			}
			
			
			
		}
	};
	public void closeKeymapping() {
		execKeymapping("0");
	}
	
	public void openKeymapping(String packageName) {
		String value = properties.getProperty(packageName, "0");
		if(whiteList.contains(packageName)){
			closeKeymapping();
		}else{
			execKeymapping(value);
		}
		
	}
	
	private void execKeymapping(String value) {
		Log.i(TAG, "keymapping execute,value=" + value);
		if(!isHardwareNormal()){
		     Log.e(TAG,"============================================");
		     Log.e(TAG,"keymapping disable,because of hardware error");
		     Log.e(TAG,"============================================");
		     notifyKeyMappingEnabled(buildContext,false,false);
		     return;
		}
                writeKeymapping(value);
		notifyKeyMappingEnabled(buildContext,!"0".equals(value),true);
	}
        private void writeKeymapping(String value){
                try {
                        FileWriter fileWriter = new FileWriter("/sys/devices/soc/soc:joystick@/key");
                        fileWriter.write(""+value);
                        fileWriter.flush();
                        fileWriter.close();
                } catch (IOException e) {
                                // TODO Auto-generated catch block
                        e.printStackTrace();
                }
        }
        private void writeKeymappingOld(String value){
                Runtime runtime = Runtime.getRuntime();
                try {
                        String command = exeFilePath+" " + value;
                        runtime.exec(command);
                } catch (Exception e) {
                        Log.d(TAG, "keymapping exception," + e.getMessage());
                }
        }

	public KeyMappingInterface getKeyMappingView(Context context){
		if(buildContext!=null)context=buildContext;
		if(keyMappingView==null){
			keyMappingView=KeyMappingPolicy.getWindow(context);
		}else{
			if(keyMappingView.getRealContext()==context){
				
			}else{
				Log.d("xin_log","oldContext="+keyMappingView.getRealContext()+",newContext="+context);
				keyMappingView.cancelReceive();
				keyMappingView=KeyMappingPolicy.getWindow(context);
			}
		}
		return keyMappingView;
	}
	
	private KeyMappingManager(){
		File exe=new File(exeFilePath);
		if(exe.exists()&&exe.canExecute()){
			Log.d(TAG, "keymapping enable");
			isEnable=true;
		}else{
			Log.d(TAG, "keymapping not enable");
			isEnable=false;
		}
		properties=new Properties();
		readAll();
	}
	private void writeAll(){
		try {
			File file=new File(filePath);
			if(!file.exists()){file.createNewFile();}
			FileOutputStream out = new FileOutputStream(file);
			properties.store(out, "writen by xin");
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			Log.d(TAG,"file not found!" + e.getMessage());
		} catch (IOException e) {
			Log.d(TAG,"file save fail!" + e.getMessage());
		}
	}


	public void copyFile(String oldPath, String newPath) {
		try {
			File oldfile = new File(oldPath);
			if (oldfile.exists()&& oldfile.canRead()) {
				FileInputStream inStream = new FileInputStream(oldPath);
				FileOutputStream fs = new FileOutputStream(newPath);
				byte[] buffer = new byte[1024*4];
				int length;
				while ((length = inStream.read(buffer)) != -1) {
					fs.write(buffer, 0, length);
				}
				fs.flush();
				fs.close();
				inStream.close();
			}
		} catch (Exception e) {
			Log.i(TAG, "copy file error!,e.msg="+e.getMessage());

		}

	}

	private void readAll() {
		try {
			File file = new File(filePath);
			if(!file.exists()){
				copyFile(initPath, filePath);
			}
			if (file.exists()) {
				FileInputStream input = new FileInputStream(file);
				properties.load(input);
				input.close();
			}
		} catch (FileNotFoundException e) {
			Log.d(TAG,"file not found!" + e.getMessage());
		} catch (IOException e) {
			Log.d(TAG,"file read fail!" + e.getMessage());
		}
	}
	public static KeyMappingManager getInstance(){
		if(mSelf==null){
			mSelf=new KeyMappingManager();
		}
		return mSelf;
	}
	
	public boolean isEnable(){
		return isEnable;
	}
	public String getValue(String packageName){
		if(packageName==null)return "0";
		String value= properties.getProperty(packageName, "0");
		Log.i(TAG,"km:getValue,"+packageName+"="+value);
		return value;
	}
	
	private void writeKeyValue(String key,String value){
		Log.i(TAG,"storekv,key="+key+",value="+value);
		if(key!=null&&value!=null){
			if(value.trim().equals("0")){
				properties.remove(key);
			}else{
				properties.put(key, value);
			}
			writeAll();
		}
	}
	
	
	public static void saveCurrentMapping(Context context,String value){
		String pkg=getTopPackageName(context);
		if(pkg!=null){
			KeyMappingManager kmm=getInstance();
			kmm.writeKeyValue(pkg, value);
		}
	}
	
	public static void cleanCurrentMapping(Context context){
		saveCurrentMapping(context,"0");

	}
	
	public static void showLayout(Context context){
		KeyMappingManager kmm=getInstance();
		String pkg=getTopPackageName(context);
		Log.i(TAG,"showLayout,pkg="+pkg);
		String value=kmm.getValue(pkg);
		kmm.getKeyMappingView(context).show(value);
	}
	public static String getTopPackageName(Context context){
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);  
		List<RunningTaskInfo> runningTasks = manager .getRunningTasks(1);  
		RunningTaskInfo cinfo = runningTasks.get(0);  
		ComponentName component = cinfo.topActivity;
		return component.getPackageName();
	}
	
	public static void registerListener(Context context){
		KeyMappingManager kmm=getInstance();
		kmm.buildContext=context;
		if(kmm.isEnable()){
			IntentFilter filter=new IntentFilter();
			filter.addAction(ACTION);
			context.registerReceiver(kmm.mKeyMappingReceiver, filter);
			
			
			filter=new IntentFilter();
			filter.addAction(ACTION_UI);
			context.registerReceiver(kmm.mUIKeyMappingReceiver, filter);
			
			//add update interface
			IntentFilter updateFilter=new IntentFilter();
			updateFilter.addAction(ACTION_UPDATE);
			context.registerReceiver(kmm.mUpdateReceiver, updateFilter);
		}
	}
	
	public static void launchKeyMappingUI(Context context,boolean isShow){
		Intent intent=new Intent(ACTION_UI);
		intent.putExtra(FIELD_AUTO, false);
		intent.putExtra(FIELD_SHOW, isShow);
		context.sendBroadcast(intent);
	}
	public static void launchKeyMappingUIByDirect(Context context,boolean isAuto){
		Intent intent=new Intent(ACTION_UI);
		intent.putExtra(FIELD_AUTO, isAuto);
		getInstance().mUIKeyMappingReceiver.onReceive(context, intent);
	}
	public static void notifyKeyMappingEnabled(Context context,boolean isEnabled,boolean isHardwareNormal){
                Intent intent=new Intent(ACTION_NOTIFY_ENABLED);
                intent.putExtra(FIELD_ENABLED, isEnabled);
                intent.putExtra(FIELD_ENABLED_HARDWARE, isHardwareNormal);
                intent.setPackage("com.android.systemui");
                context.sendBroadcastAsUser(intent, android.os.UserHandle.ALL);
        }
}
