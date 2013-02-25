package com.pencilsimulator;

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
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.explode1.Exploder1;
import com.explode2.Exploder2;
import com.explode3.Exploder3;
import com.pencildisplay.ExplosionConfig;
import com.pencildisplay.PencilDisplayHelper;
import com.pencilmotionsimulator.MotionSimulator;

/** Show a pencil balanced on its tip, falling over. */

class PencilView extends SurfaceView implements SurfaceHolder.Callback {

	static int EXPLODE_STYLE = 1;
	
	//flag showing if at least one frame of the animation has been drawn
	public boolean drawingInitialized = false;
	
	public static HashMap<String, Long> highScores = new HashMap<String, Long>();
	
    class PencilThread extends Thread {
  
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

        /** The state of the game. One of READY, RUNNING, PAUSE or LOSE */
        private int mMode;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;
        
        /** Used to figure out elapsed time between frames */
        private long mLastTime = 0;
        
        private BitmapDrawable pencilDrawable = null;
        
        private Exploder1 exploder1 = null;
        
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
        
        //paint object for drawing
        private Paint paint;
        
        //get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;

        public PencilThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
        	
        	mSurfaceHolder = surfaceHolder;
        	paint = new Paint();
        	paint.setColor(Color.GRAY);
        	paint.setTextAlign(Align.RIGHT);
        	paint.setTextSize(10.0f * scale + 0.5f);
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
        		exploder1 = new Exploder1(resizedBitmap, 0.9f, 0.6f);
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

    	/**
         * Run the game
         */
        @Override
        public void run() {

        	boolean physicsUpdated;
        	
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                    	physicsUpdated = false;
                        if (mMode == STATE_RUNNING)
                        {
                        	physicsUpdated = updatePhysics();
                        }

                        if (drawingInitialized && !physicsUpdated)
                        {
                        	//if view has been drawn at least once, and the physics was NOT updated in the last cycle, don't redraw it
                        } else
                        {
                        	doDraw(c);
                        }
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        /**
         * Used to signal the thread whether it should be running or not.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state etc.
         *
         * @see #setState(int, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                setState(mode, null);
            }
        }
    
        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state etc.
         *
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        public void setState(int mode, CharSequence message) {

            synchronized (mSurfaceHolder) {
                mMode = mode;
            }
        }

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
                
                initializePencilBitmap(getContext());
                
                initializeExplosionBitmap(getContext());
                
                //initialize drawing dimension parameters
                pencilDisplayLength = 0.7f * mCanvasHeight;

                float drawableWidth = pencilDrawable.getIntrinsicWidth();
                float drawableHeight = pencilDrawable.getIntrinsicHeight();
                pencilDisplayWidth = (drawableWidth/ drawableHeight) * pencilDisplayLength + 1.0f;

                //create display helper
                displayHelper = new PencilDisplayHelper(mCanvasWidth, mCanvasHeight, pencilDisplayWidth, pencilDisplayLength);
                
                maxTiltAngle = displayHelper.calculateMaxTiltAngle();
            }
        }

        /**
         * Draw the scene
         */
        private void doDraw(Canvas canvas) {

        	if (canvas == null)
        	{
        		Log.v("pencil", "doDraw: no canvas. returning.");
        		return;
        	}

        	drawBackground(canvas);
        	
            drawPencilDrawable(canvas);
            
            updateBalanceTimerDisplay(canvas);
            
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
        
        private void updateBalanceTimerDisplay(Canvas canvas)
        {
        	if (balanceStartTime > 0)
        	{
        		String time = PencilDisplayHelper.formatInterval(System.currentTimeMillis() - balanceStartTime);
        		canvas.drawText(time, mCanvasWidth, 0.98f * mCanvasHeight, paint);
        	} else
        	{
        		canvas.drawText("0", mCanvasWidth, 0.98f * mCanvasHeight, paint);
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
        		exploder1.draw(canvas, config.explosionXPosition, config.explosionYPosition, interpolation * config.explosionScale);
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

        	boolean balanceTimerShouldBeActive = true;
        	
            //check if user is currently touching the pencil
        	boolean previousUnderTouchControl = underTouchControl; //previous status of underTouchControl
            underTouchControl = false;
            if (mTouchX > -1)
            {
            	underTouchControl = displayHelper.isTouchInAreaOfPencil(mTouchX, mTouchY, tiltAngle);
            	//if this is the first time the user has touched the pencil, calculate the angular offset
            	//so that the user gets a smooth experience when they move the pencil manually
            	if (underTouchControl && !previousUnderTouchControl)
            	{
            		//angular offset between the position the user is touching the pencil and the pencil center
            		touchControlOffset = displayHelper.calculateTiltAngleFromTouchPosition(mTouchX, mTouchY, 0.0) - tiltAngle;
            		//set the balanceStartTime to -1 to show that we are no longer timing how long the pencil is in balance
            		balanceTimerShouldBeActive = false;
            	} else if (!underTouchControl && previousUnderTouchControl)
            	{
            		//if the user has just released the pencil, start the balance timer again
            		balanceTimerShouldBeActive = true;
            	}
            }
        	
        	//if pencil is at rest such that it's impossible to move it, don't do anything else
            if (!underTouchControl)
            {
	        	if (displayHelper.isAtRest(tiltAngle, maxTiltAngle, angularVelocity, theta))
	        	{
	        		mLastTime = now;
	        		angularVelocity = 0.0;
	        		balanceStartTime = -1;
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
            	tiltAngle = displayHelper.calculateTiltAngleFromTouchPosition(mTouchX, mTouchY, touchControlOffset);
            	angularVelocity	= (tiltAngle - oldTiltAngle)/elapsed;
            } else
            {
            	//calculate pencil's motion under acceleration as measured from the sensors
	            double[] motionArray = motionSimulator.calc(tiltAngle, angularVelocity, SettingsActivity.gravityFactor*g, theta, elapsed);
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
        		//Log.d("pencil", "stopBalanceTimer: going to check high score");
        		//get session duration in seconds
        		long sessionDuration = (long) ((System.currentTimeMillis() - balanceStartTime)/1000L);
        		//update the high score in local memory
        		updateHighScore(sessionDuration);
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
        	String key = "highscore_grav_"+SettingsActivity.gravityFactor;
        	Long highScore = PencilView.highScores.get(key);
        	if (highScore == null || (sessionDuration > highScore))
        	{
        		//Log.d("pencil", "checkAndUpdateHighScore: going to update high score to "+sessionDuration);
        		PencilView.highScores.put(key, sessionDuration);
        	}
    	}
        
        public void updateHighScoreSharedPreferences()
        {
        	Log.d("pencil", "called updateHighScoreSharedPreferences");
        	PencilActivity ac = (PencilActivity) PencilView.this.getContext();
        	SharedPreferences settings = ac.getSharedPreferences("PencilHighScores", PencilActivity.MODE_PRIVATE);
        	for (String key : PencilView.highScores.keySet())
        	{
            	if (PencilView.highScores.get(key) != null)
            	{
            		long oldHighScore = settings.getLong(key, 0L);
            		long newHighScore = PencilView.highScores.get(key);
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
        	
        	config.explosionXPosition = ((config.direction > 0) ? ((int) ( 0.99 * mCanvasWidth)) : ((int) (0.01 * mCanvasWidth)));
        	config.explosionYPosition = (int) (mCanvasHeight - 1.02 * pencilDisplayLength * (float) Math.cos(maxTiltAngle));
        	if (EXPLODE_STYLE == 1)
        	{
        		config.explosionScale = exploder1.getExplosionScale(angularVelocity);
        		config.explosionDuration = exploder1.getExplosionDuration(angularVelocity);
        		float breakpointX = ((config.direction == 1) ? 0.9f : 0.1f);
        		exploder1.prepare(breakpointX, 0.6f);
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
    }
    
    public void setHighScoresFromSharedPreferences()
    {
    	Log.d("pencil", "called setHighScoresFromSharedPreferences");
    	PencilActivity ac = (PencilActivity) PencilView.this.getContext();
    	SharedPreferences settings = ac.getSharedPreferences("PencilHighScores", PencilActivity.MODE_PRIVATE);
    	PencilView.highScores = (HashMap<String, Long>) settings.getAll();

    	for (String key : PencilView.highScores.keySet())
    	{
    		Log.d("pencil", "setHighScoresFromSharedPreferences: set local value "+key+", "+PencilView.highScores.get(key));
    	}
    }
    
    /** The thread that actually draws the animation */
    public PencilThread thread = null;

    public PencilView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        setFocusable(true); // make sure we get key events
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {

    	drawingInitialized = false;
    	
    	//set high scores from shared preferences if not yet done
    	setHighScoresFromSharedPreferences();
    	
    	if (thread != null)
    	{
    		thread.interrupt();
    		thread = null;
    	}
    	
    	// create thread
        thread = new PencilThread(holder, getContext(), new Handler() {
        });
        thread.balanceStartTime = -1;
        thread.setState(PencilThread.STATE_RUNNING);

    	thread.setRunning(true);
    	thread.start();
    	
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {

    	Log.d("pencil", "called surfaceDestroyed");
    	if (thread != null)
    	{
        	if (thread.balanceStartTime > 0)
        	{
        		long sessionDuration = (long) ((System.currentTimeMillis() - thread.balanceStartTime)/1000L);
        		Log.d("pencil", "going to update high score to "+sessionDuration);
        		thread.stopBalanceTimer(true);
        		thread.balanceStartTime = -1;
        	}
    		thread.setRunning(false);
    		thread.interrupt();
    		thread = null;
    	}
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	
    	if (event.getAction() == MotionEvent.ACTION_MOVE) {
            thread.mTouchX = event.getX();
            thread.mTouchY = event.getY();
        } else {
        	thread.mTouchX = -1;
        	thread.mTouchY = -1;
        }

    	return true;
    }
}

