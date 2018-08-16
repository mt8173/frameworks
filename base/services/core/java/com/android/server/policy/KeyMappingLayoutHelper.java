package com.android.server.policy;
import com.android.internal.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.AbsoluteLayout.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.RadioButton;

public class KeyMappingLayoutHelper implements
		OnClickListener, OnTouchListener,OnCheckedChangeListener {
	private final boolean DEBUG=true;
	public static final int MODE1=1;
	public static final int MODE2=2;
	public static final int MODE_NULL=3;
	
	public static final int LAYOUT_ALL=1;
	public static final int LAYOUT_WITHOUT_RIGHT_ROCKER=2;
	public int layoutType;
	private int width;
	private int height;
	Context context;
	private ViewGroup layout;
	TextView switchTextView;

	private int[] location_l=new int[2];
	private int[] location_r=new int[2];
	private int[] location_l2=new int[2];
	private int[] location_r2=new int[2];
	private ImageButton button_down, button_left, button_right, button_up;
	private ImageButton button_x, button_y, button_a, button_b;
	private ImageButton button_r, button_r2, button_r3, button_l, button_l2, button_l3,button_xr, button_xr2, button_xl, button_xl2;
	private ImageButton button_start, button_select;
	private Button btn_Able, btn_Disable, reset;
	// private Button bt_modeswitch;
	private int absoluteLayoutMove_xSpan, absoluteLayoutMove_ySpan;
	View menu;
	private int mode;
	private RadioButton button_mode1, button_mode2;
	private Button button_left_bigger, button_left_smaller;
	private Button button_right_bigger, button_right_smaller;
	private ImageButton ib_analog_l, ib_analog_r, ib_analog_view;
	private CheckBox leftEnable,rightEnable,viewEnable;
	private Bitmap leftBitmap,rightBitmap;
	private double leftScale=1.0,rightScale=1.0;
	//-xin-add
	private LinearLayout leftLayout,rightLayout,viewLayout;
	private String[] initValue;
	private Action action;
	public Context getRealContext(){
		return this.context;
	}
	public KeyMappingLayoutHelper(Context context,Action action,String[] initValue,int width,int height) {
		this.context = context;
		this.action=action;
		this.initValue=initValue;
		this.width=width;
		this.height=height;
		this.layout=new FrameLayout(context);
		setLayoutType(LAYOUT_ALL);

	}
	public void reset() {
		layout.removeAllViews();
		View.inflate(context, R.layout.gpad_keymapping_layout, layout);
		findViewByIdInit();
		resetLayoutStyle();
		buttonListener();
		parseDefault();
		
	}
	public void setLayoutType(int type){
		switch (type) {
		case LAYOUT_WITHOUT_RIGHT_ROCKER:
			this.layoutType=type;
			setMode(MODE_NULL);
			break;
		case LAYOUT_ALL:
		default:
			this.layoutType=LAYOUT_ALL;
			setMode(MODE1);
			break;
		}
		
	}
	public View getLayout() {
		return layout;
	}

	private void resetLayoutStyle(){
		switch (layoutType) {
		case LAYOUT_WITHOUT_RIGHT_ROCKER:
			loadBitmap(true,false);
			button_mode1.setVisibility(View.GONE);
			button_mode2.setVisibility(View.GONE);
			rightLayout.setVisibility(View.GONE);
			viewLayout.setVisibility(View.GONE);
			ib_analog_r.setVisibility(View.GONE);
			ib_analog_view.setVisibility(View.GONE);
			button_xl.setVisibility(View.GONE);
			button_xl2.setVisibility(View.GONE);
			button_xr.setVisibility(View.GONE);
			button_xr2.setVisibility(View.GONE);
			button_l.setVisibility(View.GONE);
			button_r.setVisibility(View.GONE);
			button_l2.setVisibility(View.GONE);
			button_r2.setVisibility(View.GONE);
			break;
		case LAYOUT_ALL:
		default:
			loadBitmap(true,true);
			if(isMode(MODE1)){
				button_mode1.setChecked(true);
				button_xl.setVisibility(View.GONE);
	               		button_xl2.setVisibility(View.GONE);
	                        button_xr.setVisibility(View.GONE);
	                        button_xr2.setVisibility(View.GONE);
			}
			if(isMode(MODE2)){
				button_mode2.setChecked(true);
			}
			break;
		}
		
	}
	private boolean isMode(int cmode){
		int realmode;
		if(layoutType==LAYOUT_ALL){
			realmode=mode;
		}else{
			realmode=MODE_NULL;
		}
		return cmode==realmode;
	}
	private void setMode(int cmode){
		if(layoutType==LAYOUT_ALL){
			if(cmode==MODE1||cmode==MODE2){
				mode=cmode;
			}else{
				mode=MODE1;
			}
		}else{
			mode=MODE_NULL;
		}
	}
	private void loadBitmap(boolean left,boolean right){
		if(left){
			if(leftBitmap==null){
				leftBitmap = BitmapFactory.decodeResource(context.getResources(),
						R.drawable.gpad_analog_l);
			}
			ib_analog_l.setImageBitmap(leftBitmap);
		}
		if(right){
			if(rightBitmap==null){
				rightBitmap = BitmapFactory.decodeResource(context.getResources(),
						R.drawable.gpad_analog_r);
			}
			ib_analog_r.setImageBitmap(rightBitmap);
		}
		
		
	}


	public void findViewByIdInit() {
		button_x = (ImageButton) layout.findViewById(R.id.gpad_mc_x);
		button_y = (ImageButton) layout.findViewById(R.id.gpad_mc_y);
		button_a = (ImageButton) layout.findViewById(R.id.gpad_mc_a);
		button_b = (ImageButton) layout.findViewById(R.id.gpad_mc_b);

		button_l = (ImageButton) layout.findViewById(R.id.gpad_l);
		button_r = (ImageButton) layout.findViewById(R.id.gpad_r);
		button_l2 = (ImageButton) layout.findViewById(R.id.gpad_l2);
		button_r2 = (ImageButton) layout.findViewById(R.id.gpad_r2);

                button_xl = (ImageButton) layout.findViewById(R.id.gpad_xl);
                button_xr = (ImageButton) layout.findViewById(R.id.gpad_xr);
                button_xl2 = (ImageButton) layout.findViewById(R.id.gpad_xl2);
                button_xr2 = (ImageButton) layout.findViewById(R.id.gpad_xr2);

		button_l3 = (ImageButton) layout.findViewById(R.id.gpad_l3);
		button_r3 = (ImageButton) layout.findViewById(R.id.gpad_r3);
		button_start = (ImageButton) layout.findViewById(R.id.gpad_start);
		button_select = (ImageButton) layout.findViewById(R.id.gpad_select);

		ib_analog_l = (ImageButton) layout.findViewById(R.id.gpad_analog_l);
		ib_analog_r = (ImageButton) layout.findViewById(R.id.gpad_analog_r);
		ib_analog_view = (ImageButton) layout.findViewById(R.id.gpad_analog_view);

		btn_Able = (Button) layout.findViewById(R.id.gpad_menu_able);
		btn_Disable = (Button) layout.findViewById(R.id.gpad_menu_disable);
		reset = (Button) layout.findViewById(R.id.gpad_reset);

		button_up = (ImageButton) layout.findViewById(R.id.gpad_fx_up);
		button_down = (ImageButton) layout.findViewById(R.id.gpad_fx_down);
		button_left = (ImageButton) layout.findViewById(R.id.gpad_fx_left);
		button_right = (ImageButton) layout.findViewById(R.id.gpad_fx_right);

		button_mode1 = (RadioButton) layout.findViewById(R.id.gpad_menu_mode1);
		button_mode2 = (RadioButton) layout.findViewById(R.id.gpad_menu_mode2);
		button_left_bigger = (Button) layout.findViewById(R.id.gpad_left_bigger);
		button_left_smaller = (Button) layout.findViewById(R.id.gpad_left_smaller);
		button_right_bigger = (Button) layout.findViewById(R.id.gpad_right_bigger);
		button_right_smaller = (Button) layout.findViewById(R.id.gpad_right_smaller);

		menu = (LinearLayout) layout.findViewById(R.id.gpad_menu_layout);
		leftEnable=(CheckBox) layout.findViewById(R.id.rocker_left_enable);
		rightEnable=(CheckBox) layout.findViewById(R.id.rocker_right_enable);
		viewEnable=(CheckBox) layout.findViewById(R.id.rocker_view_enable);
		
		rightLayout=(LinearLayout) layout.findViewById(R.id.gpad_rocker_right);
		viewLayout=(LinearLayout) layout.findViewById(R.id.gpad_rocker_view);
		leftLayout=(LinearLayout) layout.findViewById(R.id.gpad_rocker_left);
	}



	public void buttonListener() {
		button_x.setOnTouchListener(this);
		button_y.setOnTouchListener(this);
		button_a.setOnTouchListener(this);
		button_b.setOnTouchListener(this);
		
		button_l.setOnTouchListener(this);
		button_r.setOnTouchListener(this);
		button_l2.setOnTouchListener(this);
		button_r2.setOnTouchListener(this);

                button_xl.setOnTouchListener(this);
                button_xr.setOnTouchListener(this);
                button_xl2.setOnTouchListener(this);
                button_xr2.setOnTouchListener(this);

		button_l3.setOnTouchListener(this);
		button_r3.setOnTouchListener(this);
		button_start.setOnTouchListener(this);
		button_select.setOnTouchListener(this);

		ib_analog_l.setOnTouchListener(this);
		ib_analog_r.setOnTouchListener(this);
		ib_analog_view.setOnTouchListener(this);
		
		menu.setOnTouchListener(this);
		
		button_down.setOnTouchListener(this);
		button_left.setOnTouchListener(this);
		button_up.setOnTouchListener(this);
		button_right.setOnTouchListener(this);
		
		btn_Able.setOnClickListener(this);
		btn_Disable.setOnClickListener(this);
		reset.setOnClickListener(this);
		button_left_bigger.setOnClickListener(this);
		button_right_bigger.setOnClickListener(this);
		button_left_smaller.setOnClickListener(this);
		button_right_smaller.setOnClickListener(this);
		
		
		switch (layoutType) {
		case LAYOUT_WITHOUT_RIGHT_ROCKER:
			leftEnable.setOnCheckedChangeListener(this);
			break;
		case LAYOUT_ALL:
			leftEnable.setOnCheckedChangeListener(this);
			
			rightEnable.setOnCheckedChangeListener(this);
			viewEnable.setOnCheckedChangeListener(this);
			button_mode1.setOnCheckedChangeListener(this);
			button_mode2.setOnCheckedChangeListener(this);
		default:
			break;
		}

	}



	private void moveView(View view, float x, float y) {
		log("moveView,xy="+x+","+y);
		if(y>0&&x>0){
			view.setVisibility(View.VISIBLE);
		}else{
			view.setVisibility(View.GONE);
			return;
		}
		int viewWidth=view.getWidth();
		int viewHeight=view.getHeight();
		if(viewWidth==0||viewHeight==0){
			if(view instanceof ImageButton){
				ImageButton ib=(ImageButton) view;
				viewWidth=ib.getDrawable().getIntrinsicWidth();
				viewHeight=ib.getDrawable().getIntrinsicHeight();
			}
		}
		int left=(int) x - viewWidth / 2;
		int top=(int) y - viewHeight / 2;
		log("moveViewByTopLeft,l,t="+left+","+top);
		@SuppressWarnings("deprecation")
		LayoutParams params=new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, left, top);
		view.setLayoutParams(params);
		
	}
	private void movePosition(ImageButton view, float x, float y) {
		int viewWidth=view.getDrawable().getIntrinsicWidth();
		int viewHeight=view.getDrawable().getIntrinsicHeight();
		int left=(int) x - viewWidth / 2;
		int top=(int) y - viewHeight / 2;
		LayoutParams params=new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, left, top);
		log("p left="+left+",top="+top);
		view.setLayoutParams(params);
	}
	
	private void log(String msg){
		if(DEBUG){
			Log.i("xin_km_layout",msg);
		}
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.gpad_mc_x:
		case R.id.gpad_mc_y:
		case R.id.gpad_mc_a:
		case R.id.gpad_mc_b:
		case R.id.gpad_l:
		case R.id.gpad_r:
		case R.id.gpad_l2:
		case R.id.gpad_r2:
                case R.id.gpad_xl:
                case R.id.gpad_xr:
                case R.id.gpad_xl2:
                case R.id.gpad_xr2:
		case R.id.gpad_l3:
		case R.id.gpad_r3:
		case R.id.gpad_start:
		case R.id.gpad_select:
		case R.id.gpad_analog_l:
		case R.id.gpad_analog_r:
		//case R.id.gpad_analog_view:
		case R.id.gpad_fx_up:
		case R.id.gpad_fx_down:
		case R.id.gpad_fx_left:
		case R.id.gpad_fx_right:
			if (event.getAction() == MotionEvent.ACTION_MOVE){
				moveView(v, v.getLeft()+event.getX(), v.getTop()+event.getY());
			}
			break;
		case R.id.gpad_menu_layout:
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				absoluteLayoutMove_xSpan = (int) event.getX();
				absoluteLayoutMove_ySpan = (int) event.getY();
			}
			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				@SuppressWarnings("deprecation")
				LayoutParams layParams1=(LayoutParams) menu.getLayoutParams();
				layParams1.x=v.getLeft()+(int) event.getX()-absoluteLayoutMove_xSpan;
				layParams1.y=v.getTop()+(int) event.getY()-absoluteLayoutMove_ySpan;
				menu.setLayoutParams(layParams1);
			}
			break;
		}
		return true;
	}
	
	@Override
	public void onCheckedChanged(CompoundButton v, boolean isChecked) {
		log("20,21,22="+initValue[20]+","+initValue[21]+","+initValue[22]);
		int[] location=new int[2];
		switch (v.getId()) {
		case R.id.rocker_left_enable:
			if(isChecked){
				int p[]=new int [2];
				ib_analog_l.getLocationInWindow(p);
				if(p[0]>0&&p[1]>0){
					ib_analog_l.setVisibility(View.VISIBLE);
				}else{
					moveView(ib_analog_l, Integer.parseInt(initValue[1]), Integer.parseInt(initValue[2]));
				}
			}else{
				ib_analog_l.setVisibility(View.GONE);
			}
			break;
			
		case R.id.rocker_right_enable:
			if(isChecked){
				int p[]=new int [2];
				ib_analog_r.getLocationInWindow(p);
				log(java.util.Arrays.toString(p));
				if(p[0]>0&&p[1]>0){
					ib_analog_r.setVisibility(View.VISIBLE);
					button_xl.setVisibility(View.VISIBLE);
					button_xl2.setVisibility(View.VISIBLE);
					button_xr.setVisibility(View.VISIBLE);
					button_xr2.setVisibility(View.VISIBLE);
				}else{
					if(Integer.parseInt(initValue[0])==1){
						moveView(ib_analog_r, Integer.parseInt(initValue[21]), Integer.parseInt(initValue[22]));
					}else{
						moveView(ib_analog_r, Integer.parseInt(initValue[20]), Integer.parseInt(initValue[21]));
					}
				}
			}else{
				ib_analog_r.setVisibility(View.GONE);
				button_xl.setVisibility(View.GONE);
				button_xl2.setVisibility(View.GONE);
				button_xr.setVisibility(View.GONE);
				button_xr2.setVisibility(View.GONE);
			}
			break;
		case R.id.rocker_view_enable:
			if(isChecked){
				moveView(ib_analog_view, width*3/4, height/2);
			}else{
				ib_analog_view.setVisibility(View.GONE);
				button_l.setVisibility(View.GONE);
				button_l2.setVisibility(View.GONE);
				button_r.setVisibility(View.GONE);
				button_r2.setVisibility(View.GONE);
			}
			break;
		case R.id.gpad_menu_mode1:
			if(isChecked){
				log("mode1");
				leftLayout.setVisibility(View.VISIBLE);
				rightLayout.setVisibility(View.GONE);
				viewLayout.setVisibility(View.VISIBLE);
				getViewCenter(button_xl,location_l);
				getViewCenter(button_xr,location_r);
				getViewCenter(button_xl2,location_l2);
				getViewCenter(button_xr2,location_r2);
				button_xl.setVisibility(View.GONE);
				button_xl2.setVisibility(View.GONE);
				button_xr.setVisibility(View.GONE);
				button_xr2.setVisibility(View.GONE);
				
				ib_analog_r.setVisibility(View.GONE);
				if(viewEnable.isChecked()){
					ib_analog_view.setVisibility(View.VISIBLE);
					button_l.setVisibility(View.VISIBLE);
					button_l2.setVisibility(View.VISIBLE);
					button_r.setVisibility(View.VISIBLE);
					button_r2.setVisibility(View.VISIBLE);
					moveView(button_l, location_l[0], location_l[1]);
					moveView(button_r, location_r[0], location_r[1]);
					moveView(button_l2, location_l2[0], location_l2[1]);
					moveView(button_r2, location_r2[0], location_r2[1]);
				}else{
					ib_analog_view.setVisibility(View.GONE);
                        	        button_l.setVisibility(View.GONE);
                	                button_l2.setVisibility(View.GONE);
        	                        button_r.setVisibility(View.GONE);
	                                button_r2.setVisibility(View.GONE);
				}
				setMode(MODE1);
			}
			break;
		case R.id.gpad_menu_mode2:
			if(isChecked){
				log("mode2");
				leftLayout.setVisibility(View.VISIBLE);
				rightLayout.setVisibility(View.VISIBLE);
				viewLayout.setVisibility(View.GONE);
				ib_analog_view.setVisibility(View.GONE);
				getViewCenter(button_l,location_l);
				getViewCenter(button_r,location_r);
				getViewCenter(button_l2,location_l2);
				getViewCenter(button_r2,location_r2);
                                button_l.setVisibility(View.GONE);
                                button_l2.setVisibility(View.GONE);
                                button_r.setVisibility(View.GONE);
                                button_r2.setVisibility(View.GONE);
				if(rightEnable.isChecked()){
					ib_analog_r.setVisibility(View.VISIBLE);
					button_xl.setVisibility(View.VISIBLE);
					button_xl2.setVisibility(View.VISIBLE);
					button_xr.setVisibility(View.VISIBLE);
					button_xr2.setVisibility(View.VISIBLE);
					moveView(button_xl, location_l[0], location_l[1]);
					moveView(button_xr, location_r[0], location_r[1]);
					moveView(button_xl2, location_l2[0], location_l2[1]);
					moveView(button_xr2, location_r2[0], location_r2[1]);
				}else{
					ib_analog_r.setVisibility(View.GONE);
					button_xl.setVisibility(View.GONE);
					button_xl2.setVisibility(View.GONE);
					button_xr.setVisibility(View.GONE);
					button_xr2.setVisibility(View.GONE);
				}
				setMode(MODE2);
			}
		break;

		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.gpad_menu_able:
			String analogValue=createDemonString();
			if(action!=null){
				action.enable(analogValue);
			}
			break;
		case R.id.gpad_menu_disable:
			if(action!=null){
				action.disable();
			}
			break;
		case R.id.gpad_left_bigger:
			leftScale=leftScale+0.1;
			if(leftScale<0.5)leftScale=0.5;
			if(leftScale>1.5)leftScale=1.5;
			scale(ib_analog_l,leftBitmap,leftScale);
			break;
		case R.id.gpad_left_smaller:
			leftScale=leftScale-0.1;
			if(leftScale<0.5)leftScale=0.5;
			if(leftScale>1.5)leftScale=1.5;
			scale(ib_analog_l,leftBitmap,leftScale);
			break;
		case R.id.gpad_right_bigger:
			rightScale=rightScale+0.1;
			if(rightScale<0.5)rightScale=0.5;
			if(rightScale>1.5)rightScale=1.5;
			scale(ib_analog_r,rightBitmap,rightScale);
			break;
		case R.id.gpad_right_smaller:
			rightScale=rightScale-0.1;
			if(rightScale<0.5)rightScale=0.5;
			if(rightScale>1.5)rightScale=1.5;
			scale(ib_analog_r,rightBitmap,rightScale);
			break;
		case R.id.gpad_reset:
			reset();
			//parseDefault();
			break;

		}

	}
	

	private String createDemonString(){
		final String split=" ";
		StringBuilder sb=new StringBuilder();
		if(isMode(MODE1)){
			sb.append("1"+split);
		}else if(isMode(MODE2)){
			sb.append("2"+split);
		}else if(isMode(MODE_NULL)){
			sb.append("2"+split);
		}
		int[] location=new int[2];
		getViewCenter(ib_analog_l,location);
		sb.append(location[0]+split+location[1]+split);
		sb.append(ib_analog_l.getWidth()/2+split);
		getViewCenter(button_a,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_b,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_x,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_y,location);
		sb.append(location[0]+split+location[1]+split);

		if(isMode(MODE1)){
        	        getViewCenter(button_l,location);
	                sb.append(location[0]+split+location[1]+split);
        	        getViewCenter(button_r,location);
	                sb.append(location[0]+split+location[1]+split);
        	        getViewCenter(button_l2,location);
	                sb.append(location[0]+split+location[1]+split);
        	        getViewCenter(button_r2,location);
	                sb.append(location[0]+split+location[1]+split);

			sb.append(getCheckedVisual2()+split);
			getViewCenter(ib_analog_view,location);
			sb.append(location[0]+split+location[1]+split);
		}else if(isMode(MODE2)){
                        getViewCenter(button_xl,location);
                        sb.append(location[0]+split+location[1]+split);
                        getViewCenter(button_xr,location);
                        sb.append(location[0]+split+location[1]+split);
                        getViewCenter(button_xl2,location);
                        sb.append(location[0]+split+location[1]+split);
                        getViewCenter(button_xr2,location);
                        sb.append(location[0]+split+location[1]+split);

			getViewCenter(ib_analog_r,location);
			sb.append(location[0]+split+location[1]+split);
			sb.append(ib_analog_r.getWidth()/2+split);
		}else if(isMode(MODE_NULL)){
                        getViewCenter(button_l,location);
                        sb.append(location[0]+split+location[1]+split);
                        getViewCenter(button_r,location);
                        sb.append(location[0]+split+location[1]+split);
                        getViewCenter(button_l2,location);
                        sb.append(location[0]+split+location[1]+split);
                        getViewCenter(button_r2,location);
                        sb.append(location[0]+split+location[1]+split);

			sb.append("0"+split+"0"+split);
			sb.append("10"+split);
		}
		getViewCenter(button_left,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_right,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_up,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_down,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_l3,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_r3,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_start,location);
		sb.append(location[0]+split+location[1]+split);
		getViewCenter(button_select,location);
		sb.append(location[0]+split+location[1]);
		return sb.toString();
	}
	
	private void getViewCenter(View v,int[] p){
		if(v.getVisibility()==View.VISIBLE){
			v.getLocationInWindow(p);
			p[0]+=v.getWidth()/2;
			p[1]+=v.getHeight()/2;
		}else{
			p[0]=p[1]=-100;
		}
	}
	



	private String getCheckedVisual2(){
		android.widget.RatingBar bar=(android.widget.RatingBar)layout.findViewById(R.id.gpad_right_control);
		return Integer.toString((int)bar.getRating());
	}
	private void setCheckedVisual2(int value){
		android.widget.RatingBar bar=(android.widget.RatingBar)layout.findViewById(R.id.gpad_right_control);
		bar.setRating(value);
	}

	private void scale(ImageButton view,Bitmap bitmap, double scale) {
		int primaryWidth=bitmap.getWidth();
		int primaryHeight=bitmap.getHeight();
		Matrix matrix = new Matrix();
		matrix.postScale((float) scale, (float) scale);
		Bitmap newBmp = Bitmap.createBitmap(bitmap, 0, 0, primaryWidth,
				primaryHeight, matrix, true);
		view.setImageBitmap(newBmp);
	}

	void parsePosition(String[] pos){
		//frist,all view is set to size,then position;
		int x,y;
		leftScale=Double.parseDouble(pos[3])/(leftBitmap.getWidth()*1.0/2);
		scale(ib_analog_l,leftBitmap,leftScale);
		int i=0;
		setMode(Integer.parseInt(pos[i++]));
		if(isMode(MODE1)){
			button_mode1.setChecked(true);
		}
		if(isMode(MODE2)){
			rightScale=Double.parseDouble(pos[22])/(rightBitmap.getWidth()*1.0/2);
			scale(ib_analog_r,rightBitmap,rightScale);
			button_mode2.setChecked(true);
			
		}
		x=Integer.parseInt(pos[i++]);
		y=Integer.parseInt(pos[i++]);
		moveView(ib_analog_l, x, y);
		leftEnable.setChecked(x>0&&y>0?true:false);
		i++;
		moveView(button_a, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_b, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_x, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_y, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		if(isMode(MODE1))
			moveView(button_l, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		else
			moveView(button_xl, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		if(isMode(MODE1))
			moveView(button_r, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		else
			moveView(button_xr, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		if(isMode(MODE1))
			moveView(button_l2, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		else
			moveView(button_xl2, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		if(isMode(MODE1))
			moveView(button_r2, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		else
			moveView(button_xr2, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		
		if(isMode(MODE1)){
			setCheckedVisual2(Integer.parseInt(pos[i++]));
			x=Integer.parseInt(pos[i++]);
			y=Integer.parseInt(pos[i++]);
			//moveView(ib_analog_view, x, y);
			moveView(ib_analog_view, width*3/4, height/2);
			viewEnable.setChecked(x>0&&y>0?true:false);
			movePosition(ib_analog_r,x,y);
		}else if(isMode(MODE2)){
			x=Integer.parseInt(pos[i++]);
			y=Integer.parseInt(pos[i++]);
			moveView(ib_analog_r, x, y);
			rightEnable.setChecked(x>0&&y>0?true:false);
			i++;
			movePosition(ib_analog_view, width*3/4, height/2);
		}else if(isMode(MODE_NULL)){
			i+=3;
		}
		moveView(button_left, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_right, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_up, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_down, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_l3, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_r3, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_start, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
		moveView(button_select, Integer.parseInt(pos[i++]), Integer.parseInt(pos[i++]));
	}
	

	public void parseDefault(){
		if(initValue!=null){
			try {
				parsePosition(initValue);
			} catch (Exception e) {
				log("default:parse keymap fail,"+e.getMessage());
				initValue=null;
			}
		}
	}
	interface Action{
		void enable(String value);
		void disable();
	}
}

