
package com.pencilwallpaper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.pencilmotionsimulator.MotionSimulator;

/** Show a pencil balanced on its tip, falling over. */

public class PencilWallpaper extends WallpaperService {

    private final Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new PencilEngine();
    }

    class PencilEngine extends Engine {

    	//gravitational parameter
    	private double g = 9.81;
    	
    	//artificial parameter we use to reduce the size of gravity for testing because pencil falls over too fast
    	//with real gravity
    	final private double GRAVITY_REDUCTION_FACTOR = 0.01;
    	
    	//angle of gravitational force from negative y axis
    	private double theta = 0.0;

    	//distance from pivot to center of mass of pencil as used in physical calculations
    	private double pencilPhysicalLength = 0.1;
    	
    	//motion simulator used to animate pencil
    	private MotionSimulator motionSimulator = new MotionSimulator(pencilPhysicalLength);
    	
    	//display length of pencil
    	private float pencilDisplayLength;

    	//display width of pencil
    	private float pencilDisplayWidth;

    	//maximum angle that the pencil is allowed to tilt before it hits the side of the box
    	private float maxTiltAngle;

    	//initial angular displacement
    	final private double INITIAL_TILT_ANGLE = 0.0;
    	
    	//angular displacement
    	private double tiltAngle = INITIAL_TILT_ANGLE;
    	
    	//initial angular velocity
    	final private double INITIAL_ANGULAR_VELOCITY = 0.0;
    	
    	//angular velocity
    	private double angularVelocity = INITIAL_ANGULAR_VELOCITY;

        /**
         * Current height of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int mCanvasHeight = 1;

        /**
         * Current width of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int mCanvasWidth = 1;

        /** Used to figure out elapsed time between frames */
        private long mLastTime = 0;
    	
        //the drawable that holds the pencil image
        private BitmapDrawable pencilDrawable = null;

        //the runnable that will be used to animate the wallpaper
        private final Runnable mDrawPencil = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        
        //whether or not the launch screen is currently visible
        private boolean mVisible;
        
        /**
         * Motion sensor variables
         */
        /**
         * Manager of the acceleration sensor
         */
        private SensorManager sensorManager;

        PencilEngine() {
        	initializeBitmap();
        }
        
    	/**
         * Initialize the pencil bitmap
         */
        private void initializeBitmap()
        {
        	Context context = getApplicationContext();
        	Resources res = context.getResources();
        	pencilDrawable = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.pencil));
        	pencilDrawable.setAntiAlias(true);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            //get sensor manager
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawPencil);
            
            //unregister sensor listener to save battery
            sensorManager.unregisterListener(mSensorListener);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                //register sensor listener
                Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorManager.registerListener(mSensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawPencil);
                
                //unregister sensor listener to save battery
                sensorManager.unregisterListener(mSensorListener);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            mCanvasWidth = width;
            mCanvasHeight = height;
            
            //initialize drawing dimension parameters
            pencilDisplayLength = 0.7f * mCanvasHeight;

            float drawableWidth = pencilDrawable.getIntrinsicWidth();
            float drawableHeight = pencilDrawable.getIntrinsicHeight();
            pencilDisplayWidth = (drawableWidth/ drawableHeight) * pencilDisplayLength + 1.0f;

            maxTiltAngle = calculateMaxTiltAngle();
            drawFrame();
        }
        
        /**
         * Calculate max angle that the pencil is allowed to tilt
         */
        private float calculateMaxTiltAngle()
        {
        	//approximate contact point with the side, as measured from tip end of pencil
        	double contactPointDistance = Math.sqrt((0.95 * pencilDisplayLength) * (0.95 * pencilDisplayLength) + (0.5 * pencilDisplayWidth) * (0.5 * pencilDisplayWidth));
        	
        	//angle from the pencil's point to the contact point when the pencil is touching the side
        	float angle = (float) Math.asin((0.5 * (double) mCanvasWidth/ contactPointDistance))
        			//angle from the center of the pencil to the point where the pencil touches the side
        			- (float) Math.asin((0.5 * (double) pencilDisplayWidth/ contactPointDistance));
        	
        	return angle;
        	
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawPencil);
        }

        /**
         * Draw the scene
         */
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                	updatePhysics();
                    // draw something
                    doDraw(c);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawPencil);
            if (mVisible) {
                mHandler.postDelayed(mDrawPencil, 1000 / 25);
            }
        }


        /**
         * Calculate the angular displacement and speed of the pencil
         */
        private void updatePhysics() {

        	//initialize mLastTime
        	if (mLastTime == 0)
        	{
        		mLastTime = System.currentTimeMillis() + 100;
        		return;
        	}

        	long now = System.currentTimeMillis();
        	
        	//if pencil is at rest such that it's impossible to move it, don't do anything else
        	if (isAtRest())
        	{
        		mLastTime = now;
        		angularVelocity = 0.0;
        		return;
        	}

        	// Do nothing if mLastTime is in the future.
            // This allows the game-start to delay the start of the physics
            // by 100ms or whatever.
            if (mLastTime > now) return;
            
            double elapsed = (now - mLastTime) / 1000.0;

            //do calculations
            double[] motionArray = motionSimulator.calc(tiltAngle, angularVelocity, GRAVITY_REDUCTION_FACTOR*g, theta, elapsed);
            tiltAngle = motionArray[0];
            angularVelocity = motionArray[1];

            //if the pencil has reached the maximum tilt angle, don't let it go any further
            if (Math.abs(tiltAngle) > maxTiltAngle)
            {
            	//make sure the pencil is shown as lying on the side
            	tiltAngle = (tiltAngle > 0) ? maxTiltAngle : - maxTiltAngle;
            	
            	if (Math.abs(angularVelocity) < 0.01)
            	{
            		//stop pencil if it's moving too slowly
            		angularVelocity = 0.0;
            	} else if ((tiltAngle > 0 && angularVelocity > 0) || (tiltAngle < 0 && angularVelocity < 0))
            	{
            		//make pencil bounce off side
            		angularVelocity = -0.5 * angularVelocity;
            	}
            }
            
            mLastTime = now;
        }
        
        /**
         * Check if pencil is at rest, with no chance of moving.
         * 
         * @return boolean True if pencil cannot be moved, otherwise false
         */
        private boolean isAtRest()
        {
        	if (
        		//if the tilt angle is at the max
        		(Math.abs((Math.abs(tiltAngle) - maxTiltAngle)/maxTiltAngle) < 0.001)
        		//if the velocity is zero
        		&& (Math.abs(angularVelocity) < 0.01)
        		//if the acceleration force is such that it can't change the tilt angle
        		&& (
        				(tiltAngle > 0 && theta < maxTiltAngle)
        				|| (tiltAngle < 0 && theta > (-maxTiltAngle))
        		)      			
        	)
        	{
        		return true;
        	}
        	return false;
        }

        /**
         * Update the acceleration parameters used by the thread.
         * 
         * @param double g Magnitude of gravitational acceleration
         * @param double theta Angle of direction of gravitational force to the negative y axis
         */
        public void setAccelerationData(double g, double theta)
        {
        	this.g = g;
        	this.theta = theta;
        }
        
        /**
         * Draw the scene
         */
        void doDraw(Canvas canvas) {
        	
        	if (canvas == null)
        	{
        		Log.v("pencil", "doDraw: no canvas. returning.");
        		return;
        	}
        	
        	drawBackground(canvas);
        	
            drawPencilDrawable(canvas);
        }
        
        /**
         * Draw the background
         */
        private void drawBackground(Canvas canvas)
        {
        	//draw backdrop
        	canvas.drawColor(android.graphics.Color.BLACK);
        	
        }
        
        /**
         * Draw the pencil
         */
        private void drawPencilDrawable(Canvas canvas)
        {
        	canvas.save();
        	
        	canvas.rotate((float) tiltAngle * 180.0f/((float) Math.PI), (float) mCanvasWidth/2.0f, (float) mCanvasHeight);
        	
        	int xLeft = (int) (mCanvasWidth/2.0 - pencilDisplayWidth/2.0);
        	int yTop = (int) (mCanvasHeight - pencilDisplayLength);
        	int xRight = (int) (xLeft + pencilDisplayWidth);
        	int yBottom = (int) (yTop + pencilDisplayLength);

        	pencilDrawable.setBounds(xLeft, yTop, xRight, yBottom);

        	pencilDrawable.draw(canvas);

        	canvas.restore();
        }
      
        /**
         * Motion sensor to detect acceleration.
         */
        
        private final SensorEventListener mSensorListener = new SensorEventListener() {
            /**
             * Gravity/ acceleration array
             * 0 = x
             * 1 = y
             * 2 = z
             */
            float[] gravityData = new float[3];
	
            /**
             * Handle sensor detection
             */
        	public void onSensorChanged(SensorEvent event) {

        		switch (event.sensor.getType()) {
	    			case Sensor.TYPE_ACCELEROMETER:
	    				System.arraycopy(event.values, 0, gravityData, 0, 3);
	    				break;
	    	         
	    			default:
	    				Log.d("sensor", "unidentified sensor type!");
	    				return;
	    		}
	    		
	    		if (gravityData != null) {
	    			//convert x, y to polar coordinates
	    			double g = Math.sqrt((gravityData[0] * gravityData[0] + gravityData[1] * gravityData[1]));
	    			double theta = Math.atan2(gravityData[0], gravityData[1]);
	
	    			setAccelerationData(g, theta);
	    			
	    		} else
	    		{
	    			Log.d("sensor", "could not find geo array");
	    		}
	        }
        	
        	//blank function: just needed for implementation of SensorEventListener
        	public void onAccuracyChanged(Sensor sensor, int accuracy) {		
        	}
        };

    }

}
