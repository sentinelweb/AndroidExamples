package net.robmunro.cameratest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author robm
 * code borrowed from:
 *  http://www.brighthub.com/mobile/google-android/articles/43414.aspx
 */
public class CameraTest extends Activity implements SurfaceHolder.Callback , SensorListener{
    private static final String OUTPUT_FILE = "/sdcard/test";
	private static final String CAMERA_LOG_TAG = "CameraTest";
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private Camera mCamera;
	private boolean mPreviewRunning;
	private TextView progressText;
	private SensorManager mSensorManager;
	private float   mScale[] = new float[2];
	private float   mOrientationValues[] = new float[3];
	int picCounter=0;
	private ProcessPhotoTask processTask;
	Timer uiTimer;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enable translucency
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        //enable fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // fix to landscape mode - preview doesnt work properly in portrait
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        setContentView(R.layout.main);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        int h=200;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
        progressText=  (TextView)findViewById(R.id.text_progress);
        progressText.setVisibility(View.GONE);
        
        shutterButton = (Button)findViewById(R.id.button_shutter);
        shutterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCamera!=null) {
					// raw handler image data is null.
					mCamera.takePicture(mShuttrCallback,  null,mPictureCallback);
				}
			}
		});
        shutterButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (mCamera!=null) {
					mCamera.autoFocus(mAutoFocusCallback);
				}
				return false;
			}
		});
    }
    
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mSensorManager.registerListener(this, 
                SensorManager.SENSOR_ORIENTATION,
                SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w,	int h) {
		
		if (mPreviewRunning) {
			mCamera.stopPreview();
		}
		/*
		w=w/4*4;
		h=h/4*4;
		if (w==320) {
			h=640;w=480;
		} else if (h==320){
			w=640;h=480;
		}
		*/
		//w=640;h=480;
		
		
		Log.d(CAMERA_LOG_TAG,"surfaceChanged: "+w+":"+h);
		if (w<h) {	int tmp = h;h=w;w=tmp;}		
		
		Camera.Parameters p = mCamera.getParameters();
		Rect determinedSize = new Rect(0,0,640,480);
		try {
			Method m = p.getClass().getMethod("getSupportedPreviewSizes", new Class[]{});
			
			if (m!=null) {
				List<Camera.Size> szs = (List<Size>) m.invoke(p,  new Object[]{});
				
				for (Camera.Size sz:szs) {
					if ( sz.width<w) {
						Log.d(CAMERA_LOG_TAG,"Determined Size:"+sz.width+"x"+sz.height);
						determinedSize = new Rect(0,0,sz.width,sz.height);
						break;
					}
					
				}
			}
		} catch (NullPointerException n) {
			Log.d(CAMERA_LOG_TAG,"Couldnt get sizes:",n);
		} catch (SecurityException e) {
			Log.d(CAMERA_LOG_TAG,"Couldnt get sizes:",e);
		} catch (NoSuchMethodException e) {
			Log.d(CAMERA_LOG_TAG,"Couldnt get sizes:",e);
		} catch (IllegalArgumentException e) {
			Log.d(CAMERA_LOG_TAG,"Couldnt get sizes:",e);
		} catch (IllegalAccessException e) {
			Log.d(CAMERA_LOG_TAG,"Couldnt get sizes:",e);
		} catch (InvocationTargetException e) {
			Log.d(CAMERA_LOG_TAG,"Couldnt get sizes:",e);
		}
		//List<Camera.Size> szs = p.getSupportedPreviewSizes();// only in android 2.0 up
		p.setPreviewSize(determinedSize.right, determinedSize.bottom);
		p.setPictureFormat(PixelFormat.JPEG);

		mCamera.setParameters(p);
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mCamera.startPreview();
		mPreviewRunning = true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		startCamera();
	}

	private void startCamera() {
		Log.d(CameraTest.CAMERA_LOG_TAG, "startCamera()");
		mCamera = Camera.open();
		//mCamera.startPreview();
		//mPreviewRunning = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		stopCamera();
	}

	private void stopCamera() {
		Log.d(CameraTest.CAMERA_LOG_TAG, "stopCamera()");
		mCamera.stopPreview();
		mPreviewRunning = false;
		mCamera.release();
		mCamera=null;
	}
	Camera.PictureCallback mPictureCallback = new  Camera.PictureCallback() {
		

		public void onPictureTaken(byte[] imageData, Camera c) {
			Log.d(CameraTest.CAMERA_LOG_TAG, "pic snap:"+imageData);
			if (imageData!=null) {
				processTask = new ProcessPhotoTask();
				processTask.execute(imageData);
				uiTimer = new Timer();
				uiTimer.scheduleAtFixedRate(new UiUpdater(), 0, 1000);
				
			} else {
				Log.d(CameraTest.CAMERA_LOG_TAG, "pic snap: couldnt output");
			}
		}
	};
	
	Camera.ShutterCallback mShuttrCallback = new Camera.ShutterCallback() {

		@Override
		public void onShutter() {
			Log.d(CAMERA_LOG_TAG, "shutter callback");
			
		}
		
	};
	Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
		
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			Log.d(CAMERA_LOG_TAG,"Camera Autofucus:"+success);
			
		}
	};
	private Button shutterButton;

	@Override
	public void onAccuracyChanged(int sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(int sensor, float[] values) {
		if (sensor == SensorManager.SENSOR_ORIENTATION) {
			for (int i = 0; i < 3; i++) {
				mOrientationValues[i] = values[i];
			}
		} 
	}
	
	private class ProcessPhotoTask extends AsyncTask<byte[] , Integer, Long> {
		String state = "none";
		private int decoded;
	    protected Long doInBackground(byte[]... imageData) {
	    	Bitmap decodeByteArray=null;
	    	decoded = 1;
	    	while (decoded>0 && decoded<5) {
				try {
					decodeByteArray = BitmapFactory.decodeByteArray(imageData[0], 0, imageData[0].length);
					decoded=0;
				} catch (OutOfMemoryError e1) {
					System.gc();
					decoded++;
				}
				if (decoded==5) {
					Log.d(CameraTest.CAMERA_LOG_TAG, "pic snap: unable to decode byte array");
					return 0l;
				}
	    	}
	    	state = "picture obtained";
			if (decodeByteArray!=null) {
				try {
					Log.d(CameraTest.CAMERA_LOG_TAG, "pic snap: start processing");
			       FileOutputStream out = new FileOutputStream(OUTPUT_FILE+picCounter+".png");
			       ++picCounter;
			       picCounter %= 5;
			       Log.d(CAMERA_LOG_TAG, "Sensors:"+mOrientationValues[0]+":"+mOrientationValues[1]+":"+mOrientationValues[2]+":");
			       int rotFact=-1;
			       if(Math.abs(mOrientationValues[2])>45 && Math.abs(mOrientationValues[2])<135) {
			    	   state = "picture rotating";
			    	   Log.d(CameraTest.CAMERA_LOG_TAG, "pic snap: Portrait");
			    	  
				       rotFact=90;
				       if (mOrientationValues[2]>0) {rotFact=-90;}
				       
			       } else if (Math.abs(mOrientationValues[2])>=135 ) {
			    	   rotFact=180;
			       }
			       if (rotFact!=-1) {
			    	   Matrix matrix = new Matrix();
				        // resize the bit map
				       matrix.postScale(1, 1);
				        // rotate the Bitmap
				       matrix.postRotate(rotFact);
				       decodeByteArray = Bitmap.createBitmap(decodeByteArray, 0, 0, decodeByteArray.getWidth(), decodeByteArray.getHeight(), matrix, true);
			       }
			       state = "picture saving";
			       decodeByteArray.compress(Bitmap.CompressFormat.PNG, 90, out);
			       out.flush();
			       out.close();
			       state = "picture finished";
			       Log.d(CameraTest.CAMERA_LOG_TAG, "pic snap: finish processing");
				} catch (Exception e) {
					Log.d(CameraTest.CAMERA_LOG_TAG, "pic snap: ex",e);
					return 0l;
				}
				return 1l;
			}
			return -1l;
	    }

	    protected void onProgressUpdate(Integer... progress) {
	    	 progressText.setVisibility(View.VISIBLE);
	    	 progressText.setText(state);
	    }
	    
	    protected void onPostExecute(Long result) {
	    	if (result==1) {
		    	 progressText.setVisibility(View.GONE);
		    	 mCamera.startPreview();
		    	 if (uiTimer!=null) {uiTimer.cancel();}
	    	} else {
	    		progressText.setText("Save failed");
	    	}
	    }
	}
	
	class UiUpdater extends TimerTask{
		
		@Override
		public void run() {
			runOnUiThread(new Runnable() {public void run() {if (processTask!=null) {processTask.onProgressUpdate(0);}}});
		}
    }
	
	

}