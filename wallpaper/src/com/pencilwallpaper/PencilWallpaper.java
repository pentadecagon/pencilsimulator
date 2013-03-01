
package com.pencilwallpaper;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
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
import com.pencilanimations.ExplosionConfig;
import com.pencilanimations.FallAnimator;
import com.pencilanimations.FallConfig;
import com.pencildisplay.PencilDisplayHelper;
import com.pencilmotionsimulator.MotionSimulator;

/** Show a pencil balanced on its tip, falling over. */

public class PencilWallpaper extends WallpaperService {
	
	static int EXPLODE_STYLE = 1;
	
	//will be set from shared preferences in setGravityFromSharedPreferences
	public static float gravityFactor = 0.015f;
	
	final static String SETTINGS_SHARED_PREFS_NAME = "PencilWallpaperSettings";
	final static String HIGH_SCORE_SHARED_PREFS_NAME = "PencilWallpaperHighScores";
	
	public static HashMap<String, Long> highScores = new HashMap<String, Long>();

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
    	//final private double GRAVITY_REDUCTION_FACTOR = 0.05;
    	
    	//angle of gravitational force from negative y axis
    	private double theta = 0.0;

    	//distance from pivot to center of mass of pencil as used in physical calculations
    	private double pencilPhysicalLength = 0.05;
    	
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

        /*
         * State-tracking constants
         */
        public static final int STATE_READY = 1;
        public static final int STATE_RUNNING = 2;

        /**
         * Current height of the surface/canvas.
         */
        private int mCanvasHeight = 1;

        /**
         * Current width of the surface/canvas.
         */
        private int mCanvasWidth = 1;

        //bounds of pencil in standard orientation
        private int xLeftStandard, yTopStandard, xRightStandard, yBottomStandard;
        
        //rotation pivot in standard orientation
        private float pivotXStandard, pivotYStandard;
        
        //bounds of pencil in inverted orientation
        private int xLeftInverted, yTopInverted, xRightInverted, yBottomInverted;
        
        //rotation pivot in inverted orientation
        private float pivotXInverted, pivotYInverted;

        /** Used to figure out elapsed time between frames */
        private long mLastTime = 0;
        
        private BitmapDrawable pencilDrawable = null;
        
        private Exploder1 exploder1 = null;
        private Exploder1 exploder1Inverted = null;
        
        private Exploder2 exploder2 = null;
        
        private Exploder3 exploder3 = null;

        //config for the explosion when the pencil hits the right-hand wall
        private ExplosionConfig explosionConfigRhs = new ExplosionConfig(1);
        
        //config for the explosion when the pencil hits the left-hand wall
        private ExplosionConfig explosionConfigLhs = new ExplosionConfig(-1);

        //last user touch positions
        private float mTouchX = -1, mTouchY = -1;
        
        //helper for doing calculations related to display
        PencilDisplayHelper displayHelper = null;
        
        //whether or not the pencil is under touch control
        private boolean underTouchControl = false;
        
        //angular offset between the position the user is touching the pencil and the pencil center
        private double touchControlOffset = 0.0f;
        
        //the time when the pencil last hit the wall, so we can record how long the pencil has been upright
        public long balanceStartTime = -1;
        
        //the last score, formatted for display, achieved through balancing the pencil
        public String balanceLastScore = null;
        //the last time that the balance timer stopped
        public long balanceStopTime = -1;
        //the minimum score for the user to achieve for the score to be displayed for a couple of seconds after
        //the pencil hits the wall
        final public long BALANCE_LAST_SCORE_DISPLAY_MINIMUM_SCORE = 2000;
        //the amount of time for which to display the score after the pencil hits the wall
        final public long BALANCE_LAST_SCORE_DISPLAY_PERIOD = 1000;
        
        //paint object for drawing timer
        private Paint paintTimer;
        
        //paint object for drawing info text
        private Paint paintText;
        
        //get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;
        
        //true if view is inverted and pencil is balanced on the top of the screen, otherwise false
        private boolean isInverted = false;

        //controls the falling pencil animation when the phone is flipped upside down
        private FallConfig fallConfig = new FallConfig();
        
        //runs the falling pencil animation when the phone is flipped upside down
        private FallAnimator fallAnimator = null;

        private int framesPerSecond = 0;
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
        	paintTimer = new Paint();
        	paintTimer.setColor(Color.GRAY);
        	paintTimer.setTextAlign(Align.CENTER);
        	paintTimer.setTextSize(60.0f * scale + 0.5f);
        	
        	paintText = new Paint();
        	paintText.setColor(Color.GRAY);
        	paintText.setTextAlign(Align.RIGHT);
        	paintText.setTextSize(10.0f * scale + 0.5f);
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
        		//explodable picture for the standard pencil orientation
        		Bitmap explodableBmp = BitmapFactory.decodeResource(getResources(), R.drawable.explodable);
        		Matrix matrix = new Matrix();
        		float scale = (0.03f * mCanvasWidth)/ explodableBmp.getWidth();
        		matrix.postScale(scale, scale);
        		Bitmap resizedBitmap = Bitmap.createBitmap(explodableBmp, 0, 0, explodableBmp.getWidth(), explodableBmp.getHeight(), matrix, true);
        		exploder1 = new Exploder1(resizedBitmap, 0.9f, 0.6f);
        		
        		//explodable picture for the inverted pencil orientation
        		Bitmap explodableBmpInverted = BitmapFactory.decodeResource(getResources(), R.drawable.explodable_inverted);
        		Matrix matrixInverted = new Matrix();
        		float scaleInverted = (0.02f * mCanvasWidth)/ explodableBmpInverted.getWidth();
        		matrixInverted.postScale(scaleInverted, scaleInverted);
        		Bitmap resizedBitmapInverted = Bitmap.createBitmap(explodableBmpInverted, 0, 0, explodableBmpInverted.getWidth(), explodableBmpInverted.getHeight(), matrixInverted, true);
        		exploder1Inverted = new Exploder1(resizedBitmapInverted, 0.9f, 0.4f);	
        	} else if (EXPLODE_STYLE == 2)
        	{
        		Resources res = context.getResources();
        		BitmapDrawable[] explodableBmps = new BitmapDrawable[11];
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
            
            //bounds of pencil in standard orientation
        	xLeftStandard = (int) (mCanvasWidth/2.0 - pencilDisplayWidth/2.0);
        	yTopStandard = (int) (mCanvasHeight - pencilDisplayLength);
        	xRightStandard = (int) (xLeftStandard + pencilDisplayWidth);
        	yBottomStandard = (int) mCanvasHeight;
            
        	//bounds of pencil in inverted orientation
    		xLeftInverted = (int) (mCanvasWidth/2.0 - pencilDisplayWidth/2.0);
    		yTopInverted = 0;
    		xRightInverted = (int) (xLeftInverted + pencilDisplayWidth);
    		yBottomInverted = (int) pencilDisplayLength;
    		
    		//rotation pivot in standard orientation
    		pivotXStandard = mCanvasWidth/2.0f;
    		pivotYStandard = mCanvasHeight;

    		//rotation pivot in inverted orientation
    		pivotXInverted = mCanvasWidth/2.0f;
    		pivotYInverted = 0f;
    		
    		fallAnimator = new FallAnimator(xLeftStandard, yTopStandard, xRightStandard, yBottomStandard,
    			xLeftInverted, yTopInverted, xRightInverted, yBottomInverted,
    			pivotXStandard, pivotYStandard, pivotXInverted, pivotYInverted, pencilDrawable);
    		
            drawFrame();
        }
        
        public void setHighScoresFromSharedPreferences()
        {
        	Log.d("pencil", "called setHighScoresFromSharedPreferences");
        	SharedPreferences settings = getApplicationContext().getSharedPreferences(HIGH_SCORE_SHARED_PREFS_NAME, MODE_PRIVATE);
        	highScores = (HashMap<String, Long>) settings.getAll();

        	for (String key : highScores.keySet())
        	{
        		Log.d("pencil", "setHighScoresFromSharedPreferences: set local value "+key+", "+highScores.get(key));
        	}
        }
        
        public void setGravityFromSharedPreferences()
        {
            SharedPreferences settings = getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFS_NAME, MODE_PRIVATE);
            String gravityFactorString = settings.getString("pref_gravity", "0.015");
            gravityFactor = Float.parseFloat(gravityFactorString);
            Log.d("pencil", "setGravityFromSharedPreferences: set local value pref_gravity, "+gravityFactor);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);

        	//set high scores from shared preferences if not yet done
        	setHighScoresFromSharedPreferences();
        	setGravityFromSharedPreferences();
        	balanceStartTime = -1;
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
        	
        	if (balanceStartTime > 0)
        	{
        		long sessionDuration = (long) ((System.currentTimeMillis() - balanceStartTime)/1000L);
        		Log.d("pencil", "going to try to update high score to "+sessionDuration);
        		stopBalanceTimer(true);
        		balanceStartTime = -1;
        	}
        	
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawPencil);
        }

        /**
         * Draw the scene
         */
        //count for how many iterations the physics has not been updated
    	int noUpdateCount = 0;
        void drawFrame() {

            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                	boolean physicsUpdated = updatePhysics();
                	if (physicsUpdated)
                	{
                		noUpdateCount = 0;
                	} else
                	{
                		noUpdateCount++;
                	}
                	
                	if (!physicsUpdated && noUpdateCount > 5)
                    {
                		//if view has been drawn at least 5 times (because sometimes frames overlap),
                    	//and the physics was NOT updated in the last cycle, don't redraw it
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
  
        	// Do nothing if mLastTime is in the future.
            // This allows the game-start to delay the start of the physics
            // by 100ms or whatever.
            if (mLastTime >= now) return true;

        	boolean balanceTimerShouldBeActive = true;

        	if (fallConfig.doAnimation)
        	{
        		balanceTimerShouldBeActive = false;
        	}
        	else
        	{
	            //check if user is currently touching the pencil
	        	boolean previousUnderTouchControl = underTouchControl; //previous status of underTouchControl
	            underTouchControl = false;
	            if (mTouchX > -1)
	            {
	            	underTouchControl = displayHelper.isTouchInAreaOfPencil(mTouchX, mTouchY, tiltAngle, isInverted);
	            	//if this is the first time the user has touched the pencil, calculate the angular offset
	            	//so that the user gets a smooth experience when they move the pencil manually
	            	if (underTouchControl && !previousUnderTouchControl)
	            	{
	            		//angular offset between the position the user is touching the pencil and the pencil center
	            		touchControlOffset = displayHelper.calculateTiltAngleFromTouchPosition(mTouchX, mTouchY, 0.0, isInverted) - tiltAngle;
	            		//set the balanceStartTime to -1 to show that we are no longer timing how long the pencil is in balance
	            		Log.d("pencil", "user has touched pencil");
	            	}

	            	balanceTimerShouldBeActive = !underTouchControl;
	            }
	        	
	        	//if pencil is at rest such that it's impossible to move it, don't do anything else
	            if (!underTouchControl)
	            {
		        	if (displayHelper.isAtRest(tiltAngle, maxTiltAngle, angularVelocity, theta))
		        	{
		        		//Log.d("pencil", "pencil is at rest");
		        		mLastTime = now;
		        		tiltAngle = (tiltAngle > 0) ? maxTiltAngle : -maxTiltAngle;
		        		angularVelocity = 0.0;
		        		balanceStartTime = -1;
		        		return false;
		        	}
	            }

            	double elapsed = (now - mLastTime) / 1000.0;

	            //do calculations
	            if (underTouchControl)
	            {
	            	//if user is touching pencil, calculate the motion directly from the touch position
	            	double oldTiltAngle = tiltAngle;
	            	tiltAngle = displayHelper.calculateTiltAngleFromTouchPosition(mTouchX, mTouchY, touchControlOffset, isInverted);
	            	angularVelocity	= (tiltAngle - oldTiltAngle)/elapsed;
	            } else
	            {
	            	//calculate pencil's motion under acceleration as measured from the sensors
		            double[] motionArray = motionSimulator.calc(tiltAngle, angularVelocity, PencilWallpaper.gravityFactor*g, theta, elapsed);
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
	            		if (Math.abs(angularVelocity) > 0.3f)
	            		{
		            		if (tiltAngle > 0 && !explosionConfigRhs.doExplosion)
		            		{
		            			//initialize explosion on right-hand wall
		            			initializeExplosion(explosionConfigRhs);
		            		} else if (tiltAngle < 0 && !explosionConfigLhs.doExplosion)
		            		{
		            			//initialize explosion on left-hand wall
		            			initializeExplosion(explosionConfigLhs);
		            		}
	            		}
	            		
	            		if (!underTouchControl)
	            		{
		            		//make pencil bounce off side
		            		angularVelocity = -0.5 * angularVelocity;
	            		}
	            	}
	            }
	            
	            //if pencil is *visibly* in contact with the wall, stop the timer
	            //(use slightly more generous critieria than for motion equations to prevent rapid stop/ starting of the timer)
	            if (Math.abs(tiltAngle) >= 0.995*maxTiltAngle)
	            {
	            	//reset the timer that records how long since the pencil hit the wall
	            	balanceTimerShouldBeActive = false;
	            }
            }
            
            //update balance timer
            if (balanceTimerShouldBeActive && (balanceStartTime < 0))
            {
            	//Log.d("pencil", "going to start balance timer (balanceStartTime="+balanceStartTime+", tiltAngle="+tiltAngle+", maxTiltAngle="+maxTiltAngle);
            	startBalanceTimer();
            } else if (!balanceTimerShouldBeActive && (balanceStartTime > 0))
            {
            	//Log.d("pencil", "going to stop balance timer (balanceStartTime="+balanceStartTime);
            	stopBalanceTimer(false);
            } else
            {
            	//do nothing
            }
            
            //calculate frames per second for display
            framesPerSecond = (int) (1000L/(now - mLastTime));

            mLastTime = now;
            return true;
        }
        
        
        public void startBalanceTimer()
        {
        	//Log.d("pencil", "startBalanceTimer called");
        	balanceStartTime = System.currentTimeMillis();
        }
        
        public void stopBalanceTimer(boolean updateSharedPreferences)
        {
        	//Log.d("pencil", "stopBalanceTimer called with balanceStartTime="+balanceStartTime);
        	if (balanceStartTime > 0)
        	{
        		long now = System.currentTimeMillis();
        		//Log.d("pencil", "stopBalanceTimer: going to check high score");
        		//get session duration in seconds
        		long sessionDuration = now - balanceStartTime;
        		//update the high score in local memory
        		updateHighScore(sessionDuration);
        		
        		//if the session was sufficiently impressive, continue to display it for a couple of seconds
        		if (sessionDuration > BALANCE_LAST_SCORE_DISPLAY_MINIMUM_SCORE)
        		{
        			balanceLastScore = PencilDisplayHelper.formatInterval(sessionDuration);
        			balanceStopTime = now;
        		} else
        		{
        			balanceLastScore = null;
        			balanceStopTime = -1;
        		}
        		
        		if (updateSharedPreferences)
        		{
        			//if the update shared preferences flag is set, update the shared preferences in the device storage
        			//so that the high scores will be remembered next time we start
        			updateHighScoreSharedPreferences();
        		}
        	}
        	balanceStartTime = -1;
        }
        
        public void updateHighScore(long sessionDuration)
    	{
    		//Log.d("pencil", "called updateHighScore");
        	String key = "highscore_grav_"+PencilWallpaper.gravityFactor;
        	Long highScore = highScores.get(key);
        	if (highScore == null || (sessionDuration > highScore))
        	{
        		//Log.d("pencil", "checkAndUpdateHighScore: going to update high score to "+sessionDuration);
        		highScores.put(key, sessionDuration);
        	}
    	}
        
        public void updateHighScoreSharedPreferences()
        {
        	Log.d("pencil", "called updateHighScoreSharedPreferences");
        	SharedPreferences settings = getApplicationContext().getSharedPreferences(HIGH_SCORE_SHARED_PREFS_NAME, MODE_PRIVATE);
        	for (String key : highScores.keySet())
        	{
            	if (highScores.get(key) != null)
            	{
            		long oldHighScore = settings.getLong(key, 0L);
            		long newHighScore = highScores.get(key);
            		if (newHighScore > oldHighScore)
            		{
            			Log.d("pencil", "updateHighScoreSharedPreferences: going to update value for "+key);
            			Log.d("pencil", "updateHighScoreSharedPreferences: old value: "+oldHighScore+", newHighScore="+newHighScore);
            			SharedPreferences.Editor editor = settings.edit();
                        editor.putLong(key, newHighScore);
                        editor.commit();
            		} else
            		{
            			Log.d("pencil", "updateHighScoreSharedPreferences: not going to update value for "+key+" because old score is higher than new score");
            			Log.d("pencil", "updateHighScoreSharedPreferences: old value: "+oldHighScore+", newHighScore="+newHighScore);
            		}
            	}
        	}
        	
        	
        }

        /**
         * Initialize the explosion when the pencil hits the side
         */
        private void initializeExplosion(ExplosionConfig config)
        {
        	config.doExplosion = true;

        	config.explosionStartTime = System.currentTimeMillis();

        	//config.explosionXPosition = ((direction > 0) ? ((int) ( 0.5 * mCanvasWidth + 0.5 * pencilDisplayWidth)) : ((int) (0.5 * mCanvasWidth -  0.5 * pencilDisplayWidth)));
        	//config.explosionYPosition = (int) (mCanvasHeight - 0.98f * pencilDisplayLength);
        	
        	if (isInverted)
        	{
        		config.explosionXPosition = ((config.direction < 0) ? ((int) ( 0.985 * mCanvasWidth)) : ((int) (0.015 * mCanvasWidth)));
        		config.explosionYPosition = (int) (1.0 * pencilDisplayLength * (float) Math.cos(maxTiltAngle));
        	} else
        	{
        		config.explosionXPosition = ((config.direction > 0) ? ((int) ( 0.99 * mCanvasWidth)) : ((int) (0.01 * mCanvasWidth)));
        		config.explosionYPosition = (int) (mCanvasHeight - 1.02 * pencilDisplayLength * (float) Math.cos(maxTiltAngle));
        	}
        	if (EXPLODE_STYLE == 1)
        	{
        		config.explosionScale = exploder1.getExplosionScale(angularVelocity);
        		config.explosionDuration = exploder1.getExplosionDuration(angularVelocity);
        		float breakpointX, breakpointY;
        		if (isInverted)
            	{
        			breakpointX = ((config.direction < 0) ? 0.9f : 0.1f);
        			breakpointY = 0.4f;
        			exploder1Inverted.prepare(breakpointX, breakpointY);
            	} else
            	{
        			breakpointX = ((config.direction > 0) ? 0.9f : 0.1f);
        			breakpointY = 0.6f;
        			exploder1.prepare(breakpointX, breakpointY);
            	}
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
        	//magnitude of acceleration
        	this.g = g;
        	if (Math.abs(theta) > Math.PI/2)
        	{
        		//check if the previous force was coming from below: if so, make the pencil fall to the top of the screen
        		if (!fallConfig.doAnimation)
        		{
        			if (!isInverted)
        			{
        				triggerFallingPencilAnimation();
        			}
        			//pencil is standing on the top of the screen, drawing is flipped upside down
            		isInverted = true;
        		}		
        		//the "effective" theta, with which we do the drawing is always between -PI/2 and +PI/2 (we just flip the drawing upside down)
        		this.theta = ((theta > Math.PI/2) ? (theta - Math.PI) : (theta + Math.PI)) ;
        	} else
        	{
        		//check if the previous force was coming from above: if so make the pencil fall to the bottom of the screen
        		if (!fallConfig.doAnimation)
        		{
        			if (isInverted)
        			{
        				triggerFallingPencilAnimation();
        			}
        			//pencil is standing on the bottom of the screen, drawing is the normal way up
            		isInverted = false;
        		}     		
        		//the "effective" theta, with which we do the drawing is always between -PI/2 and +PI/2 (we just flip the drawing upside down)
        		this.theta = theta;
        	}
        }
        
        /**
         * If phone has just been flipped upside down, trigger the falling pencil animation
         */
        private void triggerFallingPencilAnimation()
        {
        	Log.d("pencil", "pencil falling animation triggered");
    		fallConfig.doAnimation = true;
    		fallConfig.startTime = System.currentTimeMillis();
    		fallAnimator.init(isInverted, tiltAngle, -tiltAngle);
    		tiltAngle = -tiltAngle; //for end of animation
    		angularVelocity = -angularVelocity; //for end of animation
        }

        /**
         * Draw the scene
         */
        void doDraw(Canvas canvas) {

        	//Log.d("pencil", "called doDraw with isInverted="+isInverted);
        	if (canvas == null)
        	{
        		Log.v("pencil", "doDraw: no canvas. returning.");
        		return;
        	}
        	
        	drawBackground(canvas);
        
        	if (fallConfig.doAnimation)
        	{
        		//Log.d("pencil", "doDraw: fallConfig.doAnimation is true so going to draw falling pencil");
        		drawFallAnimation(canvas);
        	} else
        	{
        		//Log.d("pencil", "doDraw: fallConfig.doAnimation is NOT true so NOT going to draw falling pencil");
        		drawPencil(canvas);
        	}
            
            updateBalanceTimerDisplay(canvas);
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
        private void drawPencil(Canvas canvas)
        {
        	//Log.d("pencil", "drawing normal pencil with isInverted="+isInverted);
        	
        	canvas.save();

        	if (isInverted)
        	{
        		//Log.d("pencil", "drawPencil: draw with with tiltAngle="+tiltAngle+", pivotX="+pivotXInverted+", pivotY="+pivotYInverted);
        		canvas.rotate((float) tiltAngle * 180.0f/((float) Math.PI), pivotXInverted, pivotYInverted);
	        	
        		pencilDrawable.setBounds(xLeftInverted, yTopInverted, xRightInverted, yBottomInverted);
        	} else
        	{
        		//Log.d("pencil", "drawPencil: draw with with tiltAngle="+tiltAngle+", pivotX="+pivotXStandard+", pivotY="+pivotYStandard);
	        	canvas.rotate((float) tiltAngle * 180.0f/((float) Math.PI), pivotXStandard, pivotYStandard);
	        	
	        	pencilDrawable.setBounds(xLeftStandard, yTopStandard, xRightStandard, yBottomStandard);
        	}

        	pencilDrawable.draw(canvas);
	
        	canvas.restore();
        	
        	//explosion on right-hand wall
        	if (explosionConfigRhs.doExplosion)
            {
            	drawExplosion(canvas, explosionConfigRhs);
            }
        	//explosion on left-hand wall
        	if (explosionConfigLhs.doExplosion)
            {
            	drawExplosion(canvas, explosionConfigLhs);
            }
        }
        
        /**
         * Draw the pencil falling animation
         */
        private void drawFallAnimation(Canvas canvas)
        {
        	long now = System.currentTimeMillis();
        	long progress = now - fallConfig.startTime;
        	float interpolation = (float) progress/(float) fallConfig.duration;
        	if (interpolation > 1.0f)
        	{
        		interpolation = 1.0f;
        		//end animation
        		fallConfig.doAnimation = false;
        	}
        	//Log.d("pencil", "drawing fall animation with interpolation="+interpolation);
        	fallAnimator.draw(canvas, interpolation);
        }
        
        private void updateBalanceTimerDisplay(Canvas canvas)
        {
        	//Log.d("pencil", "called updateBalanceTimerDisplay with isInverted="+isInverted+", fallConfig.doAnimation="+fallConfig.doAnimation+", balanceStartTime="+balanceStartTime);
        	if (isInverted && !fallConfig.doAnimation)
        	{
	        	canvas.save();
	        	canvas.rotate(-180, mCanvasWidth/2.0f, mCanvasHeight/2.0f);
        	}
        	
        	long now = System.currentTimeMillis();
        	
        	String time;
        	if (balanceStopTime > 0 && ((now - balanceStopTime) < BALANCE_LAST_SCORE_DISPLAY_PERIOD))
        	{
        		time = balanceLastScore;
        	} else if (balanceStartTime > 0)
        	{
        		time = PencilDisplayHelper.formatInterval(now - balanceStartTime);
        	} else
        	{
        		time = "0";
        	}
        	canvas.drawText(time, 0.5f * mCanvasWidth, 0.18f * mCanvasHeight, paintTimer);
        	
            //show frames per second
            canvas.drawText("FPS: "+framesPerSecond, mCanvasWidth, 0.98f * mCanvasHeight, paintText);
        	
        	if (isInverted && !fallConfig.doAnimation)
        	{
        		canvas.restore();
        	}
        }
      
        /**
         * Draw the explosion when the pencil hits the side
         */
        private void drawExplosion(Canvas canvas, ExplosionConfig config)
        {
        	long now = System.currentTimeMillis();
        	long duration = now - config.explosionStartTime;
        	if (EXPLODE_STYLE == 1)
        	{
        		float interpolation = duration/100f;
        		if (isInverted)
        		{
        			exploder1Inverted.draw(canvas, config.explosionXPosition, config.explosionYPosition, interpolation * config.explosionScale);
        		} else
        		{
        			exploder1.draw(canvas, config.explosionXPosition, config.explosionYPosition, interpolation * config.explosionScale);
        		}
        	} else if (EXPLODE_STYLE == 2)
        	{
        		int iteration = Math.round(duration/10f);
        		exploder2.draw(canvas, config.explosionXPosition, config.explosionYPosition, iteration, config.explosionScale);
        	} else if (EXPLODE_STYLE == 3)
        	{
        		float interpolation = duration/100f;
        		exploder3.draw(canvas, config.explosionXPosition, config.explosionYPosition, interpolation, config.explosionScale);
        	}

        	if (duration > config.explosionDuration)
        	{
        		config.doExplosion = false;
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
        			//the magnitude of the force is always the total magnitude including the z-component
        			double g = Math.sqrt((gravityData[0] * gravityData[0] + gravityData[1] * gravityData[1] + gravityData[2] * gravityData[2]));
        			//we only used the force in the x/y plane
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
