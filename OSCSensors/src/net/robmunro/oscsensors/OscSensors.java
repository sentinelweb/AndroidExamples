package net.robmunro.oscsensors;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

public class OscSensors extends Activity implements SensorListener {
	private static final String PREF_PORT = "port";
	private static final String PREF_IP_PREFIX = "ip";
	private static final int SMOOTH_MAX = 30;
	private static final String PREFS_NAME = "net.robmunro.OscSensors";
	private static final byte[] DEFAULT_IP = new byte[]{(byte) 192,(byte)168,1,8};
	private static final int DEFAULT_PORT=10001;
	private static final boolean ACCEL_ENABLED = false;
	//private int smoothPointer = 0;
	byte[] ip = DEFAULT_IP;
	int port = DEFAULT_PORT;
	
	SharedPreferences settings;
	
	OSCPortIn receiver;
	SeekBar sb[]=new SeekBar[5];
	TextView sbt[]=new TextView[5];
	TextView valsAvailTxt;
	//TextView valsAvailTxt;
	private float   smoothedValues[] = new float[3*2];// smoothed values
	private float	mValuesBuffer[][] = new float[SMOOTH_MAX][3*2];
    private float   mOrientationValues[] = new float[3];
    private float   mLastX;
    private float   mScale[] = new float[2];
    private float   mSpeed = 1.0f;
    private int smoothDepth = 10;
	
    public int keyValTest=0;
    
    private SensorManager mSensorManager;
    OSCPortOut sender;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
          requestWindowFeature(Window.FEATURE_NO_TITLE);  
                 getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
                                         WindowManager.LayoutParams.FLAG_FULLSCREEN);  
        setContentView(R.layout.main);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        int h=200;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
        
        settings = getSharedPreferences(PREFS_NAME, 0);
        restorePrefs();
        
		
		startReceiver();
		
		sb[0] = (SeekBar)findViewById(R.id.main_slider1);
		sbt[0] = (TextView)findViewById(R.id.main_slider1_txt);
		sb[0].setOnSeekBarChangeListener(new OnSeekBarChangeListener () {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
				int value = seekBar.getProgress();
				sbt[0].setText("sldr1:"+value);
				sendMsg("/sldr1",new float[]{value});
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {	}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		
		sb[1] = (SeekBar)findViewById(R.id.main_slider2);
		sb[1].setMax(SMOOTH_MAX-1);
		sbt[1] = (TextView)findViewById(R.id.main_slider2_txt);
		valsAvailTxt= (TextView)findViewById(R.id.main_slider2_txt1);
		//valsAvailTxt= (TextView)findViewById(R.id.main_slider2_txt2);
		sb[1].setOnSeekBarChangeListener(new OnSeekBarChangeListener () {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
				int value = seekBar.getProgress();
				sbt[1].setText("smoothing:"+value);
				smoothDepth=value+1;
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {	}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		Button b = (Button) findViewById(R.id.main_connect);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (sender!=null ) { 
					sender.close();
					sender=null;
				}
				TextView t = (TextView)findViewById(R.id.main_ip);
				String ipStr = t.getText().toString();
				String[] ipStrSplit = ipStr.split("\\.");
				if (ipStrSplit.length!=4) {
					Toast.makeText(OscSensors.this, "need 4 block ip address", 400).show();
					return;
				}
				byte[] newIP = new byte[4];
				try {
					for (int i=0;i<ipStrSplit.length;i++) {
						newIP[i]=toUnsignedByte(Integer.parseInt(ipStrSplit[i]));
					}
				} catch (NumberFormatException e) {
					Toast.makeText(OscSensors.this, "parse Error IP", 400).show();
					return;
				}
				ip = newIP;
				TextView tp = (TextView)findViewById(R.id.main_port);
				try {
					String portStr = tp.getText().toString();
					port = Integer.parseInt(portStr);
				} catch (NumberFormatException e) {
					Toast.makeText(OscSensors.this, "parse Error port", 400).show();
					return;
				}
				Editor edit = settings.edit();
				for (int i=0;i<4;i++) {
					edit.putInt(PREF_IP_PREFIX+i, ip[i]); 
				}
				edit.putInt(PREF_PORT, port);
				edit.commit();
				if (startSender()){
					Toast.makeText(OscSensors.this, "connected", 400).show();
				} else {
					Toast.makeText(OscSensors.this, "connection problem", 400).show(); 
				}
			}
			
		});
		Button b1 = (Button) findViewById(R.id.main_close);
		b1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (sender!=null ) {
					sender.close();
					sender=null;
				}
			}
			
		});
    }
	private void restorePrefs() {
		TextView t = (TextView)findViewById(R.id.main_ip);
        TextView tp = (TextView)findViewById(R.id.main_port);
		byte[] newIP = new byte[4];
		String ipstr = "";
		for (int i=0;i<4;i++) {
			int ipFrag = settings.getInt(PREF_IP_PREFIX+i, DEFAULT_IP[i]);
			if (ipFrag<0) {	ipFrag=256+ipFrag;}// unsigned byte shit
			newIP[i] = toUnsignedByte(ipFrag);
			
			ipstr+=ipFrag+(i<3?".":"");
		}
		ip=newIP;
		t.setText(ipstr);
		
		port = settings.getInt(PREF_PORT, DEFAULT_PORT); 
		tp.setText(""+port);
	}
	private void startReceiver() {
		try {
			receiver = new OSCPortIn(10011);
			OSCListener listener = new OSCListener() {
				public void acceptMessage(java.util.Date time, OSCMessage message) {
					System.out.println("Message received!");
				}
			};
			receiver.addListener("/hello", listener);
			receiver.startListening();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
    private void sendMsg(String msgName,int val) {
    	 sendMsg(msgName,new float[] {val});
    }
    private void sendMsg(String msgName,float[] vals) {
		Float[] sendValues = new Float[vals.length];
		for (int i=0;i<vals.length;i++){sendValues[i]=vals[i];} 
		//OSCMessage msg = new OSCMessage(msgName, sendValues);
		if (msgName.equals("/orientations")) {
			StringBuffer s = new StringBuffer();
			s.append("set_bpm fm  ");
			s.append(vals[1]/(float)180);
			s.append("\n");
			Log.d("osc",s.toString());
			 try {
				//if (sender!=null) {sender.send(msg);}
				 if (sender!=null) {sender.send(s.toString().getBytes());}
			 } catch (Exception e) {
				 Log.e("testOSC","Couldn't send");
			 }
		}
		
	}
	
	int valsAvailable[] = new int[2];
	public void onSensorChanged(int sensor, float[] values) {
        synchronized (this) {
			if (sensor == SensorManager.SENSOR_ORIENTATION) {
				for (int i = 0; i < 3; i++) {
					mOrientationValues[i] = values[i];
				}
				sendMsg("/orientations", mOrientationValues);
			} else if (ACCEL_ENABLED) {
				//float nextValues[] = new float[3*2];
				float deltaX = mSpeed;
				float newX = mLastX + deltaX;
				int j = (sensor == SensorManager.SENSOR_MAGNETIC_FIELD) ? 1 : 0;
				if (valsAvailable[j]<smoothDepth) {
					valsAvailable[j]++;
				} else {
					valsAvailable[j]=smoothDepth;
				}
				valsAvailTxt.setText("avail:a"+valsAvailable[0]+" m:"+valsAvailable[1]);
				for (int i=valsAvailable[j];i>0;i--) {
					for (int m=0;m<3;m++) {
						mValuesBuffer[i][m+3*j]=mValuesBuffer[i-1][m+3*j];
					
					}
				}
				// so first 3 are Accelerometer , last 3 mag field
				
				//Log.e("OSCSensor", ""+sensor);
				for (int i = 0; i < 3; i++) {
					int k = i + j * 3;
					mValuesBuffer[0][k] = values[i] * mScale[j];
				}
				
				for (int m=0;m<3;m++) {
					smoothedValues[m+3*j]=0;
					for (int i=0;i<valsAvailable[j];i++) {
						smoothedValues[m+3*j]+=mValuesBuffer[i][m+3*j];
					}
					smoothedValues[m+3*j]/=valsAvailable[j];
				}
				
				sendMsg("/accel", new float[]{smoothedValues[0],smoothedValues[1],smoothedValues[2]});
				sendMsg("/mag", new float[]{smoothedValues[3],smoothedValues[4],smoothedValues[5]});
				
				if (sensor == SensorManager.SENSOR_MAGNETIC_FIELD){
					mLastX += mSpeed;
				}
				//sendMsg("/speed", new float[]{mSpeed});
			}
		}
    }
	
	 @Override
	    protected void onResume() {
	        super.onResume();
	        mSensorManager.registerListener(this, 
	                SensorManager.SENSOR_ACCELEROMETER | 
	                SensorManager.SENSOR_MAGNETIC_FIELD | 
	                SensorManager.SENSOR_ORIENTATION,
	                SensorManager.SENSOR_DELAY_FASTEST);
	        startSender();
	    }
	private boolean startSender() {
		try {
			sender = new OSCPortOut(InetAddress.getByAddress(ip),port);
			return true;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	    
	    @Override
	    protected void onStop() {
	    	super.onStop();
	        mSensorManager.unregisterListener(this);
	        if (sender!=null) {	sender.close(); }
	        
	    }
		@Override
		public void onAccuracyChanged(int sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			switch (keyCode) {
				case KeyEvent.KEYCODE_VOLUME_DOWN: return changeKey(false); 
				case KeyEvent.KEYCODE_VOLUME_UP:return changeKey(true);
				default:return super.onKeyDown(keyCode, event);
			}
		}
		
		private boolean changeKey(boolean up) {
			boolean consumed;
			consumed = true;
			keyValTest+=up?1:-1;
			Log.d("OSCSensors", "key event:"+keyValTest);
			sendMsg("/key",keyValTest);
			return consumed;
		}
		/*
		@Override
		public boolean onKeyLongPress(int keyCode, KeyEvent event) {
			// TODO Auto-generated method stub
			return super.onKeyLongPress(keyCode, event);
		}*/
		@Override
		public boolean onKeyMultiple(int keyCode, int repeatCount,
				KeyEvent event) {
			// TODO Auto-generated method stub
			return super.onKeyMultiple(keyCode, repeatCount, event);
		}
		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			// TODO Auto-generated method stub
			return super.onKeyUp(keyCode, event);
		}
		public static byte toUnsignedByte(int intVal) {
			byte byteVal;
			if (intVal > 127) {
				int temp = intVal - 256;
				byteVal = (byte)temp;
			}
			else {
				byteVal = (byte)intVal;
			}	
			return byteVal;
		}

}