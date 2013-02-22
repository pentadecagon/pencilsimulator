
package com.pencilwallpaper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.explode1.Exploder1;
import com.explode2.Exploder2;
import com.explode3.Exploder3;
import com.pencildisplay.PencilDisplayHelper;
import com.pencildisplay.ExplosionConfig;
import com.pencilmotionsimulator.MotionSimulator;

/** Show a pencil balanced on its tip, falling over. */

public class PencilWallpaper extends WallpaperService {
	
	static int EXPLODE_STYLE = 1;
	
	//flag showing if at least one frame of the animation has been drawn
	public boolean drawingInitialized = false;

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

    	//time interval between drawing of animation frames in miliseconds
    	final private static int FRAME_INTERVAL = 10;
    	
    	//gravitational parameter
    	private double g = 9.81;
    	
    	//artificial parameter we use to reduce the size of gravity for testing because pencil falls over too fast
    	//with real gravity
    	final private double GRAVITY_REDUCTION_FACTOR = 0.02;
    	
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
    	private double maxTiltAngle;

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

        private Exploder1 exploder1 = null;
        
        private Exploder2 exploder2 = null;
        
        private Exploder3 exploder3 = null;
        
        private ExplosionConfig explosionConfig = new ExplosionConfig();
        
        //last user touch positions
        private float mTouchX = -1, mTouchY = -1;
        
        //helper for doing calculations related to display
        PencilDisplayHelper displayHelper = null;
        
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
        }
        
        /**
         * Initialize the pencil image
         */
        private void initializePencilBitmap(Context context)
        {
        	Resources res = context.getResources();
        	pencilDrawable = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.pencil));
        	pencilDrawable.setAntiAlias(true);    	
        }
        
        /**
         * Initialize the explosion image
         */
        private void initializeExplosionBitmap(Context context)
        {
        	if (EXPLODE_STYLE == 1)
        	{
        		Bitmap explodableBmp = BitmapFactory.decodeResource(getResources(), R.drawable.explodable);
        		Matrix matrix = new Matrix();
        		float scale = (0.03f * mCanvasWidth)/ explodableBmp.getWidth();
        		matrix.postScale(scale, scale);
        		Bitmap resizedBitmap = Bitmap.createBitmap(explodableBmp, 0, 0, explodableBmp.getWidth(), explodableBmp.getHeight(), matrix, true);
        		exploder1 = new Exploder1(resizedBitmap, 0.5f, 0.6f);
        	} else if (EXPLODE_STYLE == 2)
        	{
        		Resources res = context.getResources();
        		BitmapDrawable[] explodableBmps = new BitmapDrawable[12];
        		explodableBmps[0] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom1));
        		explodableBmps[1] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom2));
        		explodableBmps[2] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom3));
        		explodableBmps[3] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom4));
        		explodableBmps[4] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom5));
        		explodableBmps[5] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom6));
        		explodableBmps[6] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom7));
        		explodableBmps[7] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom8));
        		explodableBmps[8] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom9));
        		explodableBmps[9] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom10));
        		explodableBmps[10] = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom11));
        		exploder2 = new Exploder2(explodableBmps);
        	}  else if (EXPLODE_STYLE == 3)
        	{
        		Resources res = context.getResources();
        		BitmapDrawable explodableBmp = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.boom));
        		exploder3 = new Exploder3(explodableBmp);
        	}
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
                
                drawingInitialized = false;
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
            
            initializePencilBitmap(getApplicationContext());
            
            initializeExplosionBitmap(getApplicationContext());
            
            //initialize drawing dimension parameters
            pencilDisplayLength = 0.7f * mCanvasHeight;

            float drawableWidth = pencilDrawable.getIntrinsicWidth();
            float drawableHeight = pencilDrawable.getIntrinsicHeight();
            pencilDisplayWidth = (drawableWidth/ drawableHeight) * pencilDisplayLength + 1.0f;

            //create display helper
            displayHelper = new PencilDisplayHelper(mCanvasWidth, mCanvasHeight, pencilDisplayWidth, pencilDisplayLength);
            
            maxTiltAngle = displayHelper.calculateMaxTiltAngle();
            
            drawingInitialized = false;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            drawingInitialized = false;
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
                	boolean physicsUpdated = updatePhysics();
                	if (drawingInitialized && !physicsUpdated)
                    {
                    	//if view has been drawn at least once, and the physics was NOT updated in the last cycle, don't redraw it
                    } else
                    {
                    	// draw something
                    	doDraw(c);
                    }
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawPencil);
            if (mVisible) {
                mHandler.postDelayed(mDrawPencil, FRAME_INTERVAL);
            }
        }

        /**
         * Calculate the angular displacement and speed of the pencil
         * 
         * @return boolean True if the physical parameters of the system were updated and the view needs to be
         *  re-drawn, otherwise false
         */
        private boolean updatePhysics() {

        	//initialize mLastTime
        	if (mLastTime == 0)
        	{
        		mLastTime = System.currentTimeMillis() + 100;
        		return true;
        	}

        	long now = System.currentTimeMillis();
        	
            //check if user is currently touching the pencil
            boolean underTouchControl = false;
            if (mTouchX > -1)
            {
            	underTouchControl = displayHelper.isTouchInAreaOfPencil(mTouchX, mTouchY, tiltAngle);
            }
        	
        	//if pencil is at rest such that it's impossible to move it, don't do anything else
            if (!underTouchControl)
            {
	        	if (displayHelper.isAtRest(tiltAngle, maxTiltAngle, angularVelocity, theta))
	        	{
	        		mLastTime = now;
	        		angularVelocity = 0.0;
	        		return false;
	        	}
            }
            
        	// Do nothing if mLastTime is in the future.
            // This allows the game-start to delay the start of the physics
            // by 100ms or whatever.
            if (mLastTime >= now) return true;
            
            double elapsed = (now - mLastTime) / 1000.0;

            //do calculations
            if (underTouchControl)
            {
            	//if user is touching pencil, calculate the motion directly from the touch position
            	double oldTiltAngle = tiltAngle;
            	tiltAngle = displayHelper.calculateTiltAngleFromTouchPosition(mTouchX, mTouchY);
            	angularVelocity	= (tiltAngle - oldTiltAngle)/elapsed;
            } else
            {
            	//calculate pencil's motion under acceleration as measured from the sensors
	            double[] motionArray = motionSimulator.calc(tiltAngle, angularVelocity, GRAVITY_REDUCTION_FACTOR*g, theta, elapsed);
	            tiltAngle = motionArray[0];
	            angularVelocity = motionArray[1];
            }
            
            //if the pencil has reached the maximum tilt angle, don't let it go any further
            if (Math.abs(tiltAngle) > maxTiltAngle)
            {
            	//make sure the pencil is shown as lying on the side
            	tiltAngle = (tiltAngle > 0) ? maxTiltAngle : - maxTiltAngle;
            	
            	if (!underTouchControl && (Math.abs(angularVelocity) < 0.01))
            	{
            		//stop pencil if it's moving too slowly
            		angularVelocity = 0.0;
            	}

            	if ((tiltAngle > 0 && angularVelocity > 0) || (tiltAngle < 0 && angularVelocity < 0))
            	{
            		//if pencil hits the wall at above a certain speed, generate an explosion 
            		if (!explosionConfig.doExplosion && Math.abs(angularVelocity) > 0.3f)
            		{
            			initializeExplosion(explosionConfig);
            		}
            		
            		if (!underTouchControl)
            		{
	            		//make pencil bounce off side
	            		angularVelocity = -0.5 * angularVelocity;
            		}
            	}
            }
            
            mLastTime = now;
            return true;
        }
        
        /**
         * Initialize the explosion when the pencil hits the side
         */
        private void initializeExplosion(ExplosionConfig config)
        {
        	config.doExplosion = true;
        	config.explosionIteration = 0;

        	if (tiltAngle > 0)
    		{
        		config.explosionXPosition = (int) ( 0.5 * mCanvasWidth + 0.5 * pencilDisplayWidth);
    		} else
    		{
    			config.explosionXPosition = (int) (0.5 * mCanvasWidth -  0.5 * pencilDisplayWidth);
    		}
        	config.explosionYPosition = (int) (mCanvasHeight - 0.98f * pencilDisplayLength);
        	if (EXPLODE_STYLE == 1)
        	{
        		config.explosionScale = exploder1.getExplosionScale(angularVelocity);
        		config.explosionDuration = exploder1.getExplosionDuration(angularVelocity);
        		//exploder1.prepare(0.5f, 0.6f);
        	} else if (EXPLODE_STYLE == 2)
        	{
        		config.explosionScale = exploder2.getExplosionScale(angularVelocity);
        		config.explosionDuration = exploder2.getExplosionDuration(angularVelocity);
        	} else if (EXPLODE_STYLE == 3)
        	{
        		config.explosionScale = exploder3.getExplosionScale(angularVelocity);
        		config.explosionDuration = exploder3.getExplosionDuration(angularVelocity);
        	}
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
            
            drawingInitialized = true;
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

        	if (explosionConfig.doExplosion)
        	{
        		drawExplosion(canvas, explosionConfig);
        	}
        	
        	canvas.restore();
        }
      
        /**
         * Draw the explosion when the pencil hits the side
         */
        private void drawExplosion(Canvas canvas, ExplosionConfig config)
        {
        	if (EXPLODE_STYLE == 1)
        	{
        		exploder1.draw(canvas, config.explosionXPosition, config.explosionYPosition, config.explosionIteration * 0.1f * config.explosionScale);
        	} else if (EXPLODE_STYLE == 2)
        	{
        		exploder2.draw(canvas, config.explosionXPosition, config.explosionYPosition, config.explosionIteration, config.explosionScale);
        	} else if (EXPLODE_STYLE == 3)
        	{
        		exploder3.draw(canvas, config.explosionXPosition, config.explosionYPosition, config.explosionIteration, config.explosionScale);
        	}
        	
        	config.explosionIteration++;
        	if (config.explosionIteration > config.explosionDuration)
        	{
        		config.doExplosion = false;
        		config.explosionIteration = 0;
        	}
        }
        
        @Override
        public void onTouchEvent(MotionEvent event) {
        	
        	if (event.getAction() == MotionEvent.ACTION_MOVE) {
                mTouchX = event.getX();
                mTouchY = event.getY();
            } else {
            	mTouchX = -1;
            	mTouchY = -1;
            }
        	super.onTouchEvent(event);
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
