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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.explode1.Exploder1;
import com.explode2.Exploder2;
import com.explode3.Exploder3;
import com.pencilanimations.ExplosionConfig;
import com.pencilanimations.FallAnimator;
import com.pencilanimations.FallConfig;
import com.pencildisplay.PencilDisplayHelper;
import com.pencilmotionsimulator.MotionSimulator;

/** Show a pencil balanced on its tip, falling over. */

class PencilView extends SurfaceView implements SurfaceHolder.Callback {

	static int EXPLODE_STYLE = 1;
    
    private TextView mStatusText;
    
    private TextViewUD mStatusTextUD;
    
	//will be set from shared preferences in setGravityFromSharedPreferences
	public static float gravityFactor = 0.015f;
	
	final static String SETTINGS_SHARED_PREFS_NAME = "PencilSettings";
	final static String HIGH_SCORE_SHARED_PREFS_NAME = "PencilHighScores";

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

    	private Handler mHandler;
        /*
         * State-tracking constants
         */
        public static final int STATE_PAUSED = 1;
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

        //bounds of pencil in standard orientation
        private int xLeftStandard, yTopStandard, xRightStandard, yBottomStandard;
        
        //rotation pivot in standard orientation
        private float pivotXStandard, pivotYStandard;
        
        //bounds of pencil in inverted orientation
        private int xLeftInverted, yTopInverted, xRightInverted, yBottomInverted;
        
        //rotation pivot in inverted orientation
        private float pivotXInverted, pivotYInverted;
        
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
        
        public BalanceTimer balanceTimer;

        public PencilThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
        	
        	mHandler = handler;
        	
        	mSurfaceHolder = surfaceHolder;
        	
        	paintTimer = new Paint();
        	paintTimer.setColor(Color.GRAY);
        	paintTimer.setTextAlign(Align.CENTER);
        	paintTimer.setTextSize(60.0f * scale + 0.5f);
        	
        	paintText = new Paint();
        	paintText.setColor(Color.GRAY);
        	paintText.setTextAlign(Align.RIGHT);
        	paintText.setTextSize(10.0f * scale + 0.5f);
        	
        	balanceTimer = new BalanceTimer(context, this);
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

    	/**
         * Run the game
         */
        @Override
        public void run() {

        	//whether or not physics has been updated on this iteration
        	boolean physicsUpdated;
        	//count for how many iterations the physics has not been updated
        	int noUpdateCount = 0;
        	
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

        public void showTapToStartMessage()
        {
        	 Message msg = mHandler.obtainMessage();
             Bundle b = new Bundle();
             b.putBoolean("inverted", isInverted);
             b.putString("text", "tap to restart timer");
             b.putInt("viz", View.VISIBLE);
             msg.setData(b);
             mHandler.sendMessage(msg);
        }
        
        public void hideTapToStartMessage()
        {
        	Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putBoolean("inverted", isInverted);
            b.putString("text", "");
            b.putInt("viz", View.GONE);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
        
        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state etc.
         *
         * @see #setGameState(int, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setGameState(int mode) {
            synchronized (mSurfaceHolder) {
                setGameState(mode, null);
            }
        }
    
        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state etc.
         *
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        public void setGameState(int mode, CharSequence message) {

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
            }
        }

        /**
         * Draw the scene
         */
        private void doDraw(Canvas canvas) {

        	//Log.d("pencil", "called doDraw with isInverted="+isInverted);
        	if (canvas == null)
        	{
        		Log.v("pencil", "doDraw: no canvas. returning.");
        		return;
        	}
        	
        	drawBackground(canvas);
        
        	if (fallConfig.doAnimation && (mMode == STATE_RUNNING))
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
        		if (balanceTimer.state == BalanceTimer.BALANCE_TIMER_STATE_PAUSED)
        		{
        			//renew the "tap to restart" message so it's the right way up
        			showTapToStartMessage();
        		}
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
        	if (balanceTimer.state == BalanceTimer.BALANCE_TIMER_STATE_RUNNING)
        	{
        		time = PencilDisplayHelper.formatInterval(now - balanceTimer.balanceStartTime);
        	} else
        	{
        		time = balanceTimer.balanceLastScore;
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
        	long duration;
        	if (mMode == STATE_PAUSED)
        	{
        		//if paused, freeze at the last explosion time
        		duration = (balanceTimer.balanceStopTime + BalanceTimer.BALANCE_PAUSE_GAME_DELAY_PERIOD) - config.explosionStartTime;
        	} else
        	{
        		duration = now - config.explosionStartTime;
        	}
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
	            		//Log.d("pencil", "user has touched pencil");
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
		        		//balance timer should not be running at this point, but if it is, stop it
		        		if(balanceTimer.state == BalanceTimer.BALANCE_TIMER_STATE_RUNNING)
		        		{
		        			balanceTimer.stop(false, now);
		        		}
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
		            double[] motionArray = motionSimulator.calc(tiltAngle, angularVelocity, PencilView.gravityFactor*g, theta, elapsed);
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
	            	//Log.d("pencil", "going to stop timer &  pause game");
	            	balanceTimer.stop(true, now);
	            	balanceTimerShouldBeActive = false;
	            }
            }
            
            //update balance timer
        	if (balanceTimer.state == BalanceTimer.BALANCE_TIMER_STATE_PAUSED)
        	{
        	  //game is waiting to be paused: do nothing
        	} else if (balanceTimerShouldBeActive && (balanceTimer.state != BalanceTimer.BALANCE_TIMER_STATE_RUNNING))
            {
            	//Log.d("pencil", "going to start balance timer (balanceStartTime="+balanceStartTime+", tiltAngle="+tiltAngle+", maxTiltAngle="+maxTiltAngle);
            	balanceTimer.start(now);
            } else if (!balanceTimerShouldBeActive && (balanceTimer.state == BalanceTimer.BALANCE_TIMER_STATE_RUNNING))
            {
            	//Log.d("pencil", "going to stop balance timer (balanceStartTime="+balanceStartTime);
            	balanceTimer.stop(false, now);
            } else
            {
            	//do nothing
            }
            
            //calculate frames per second for display
            framesPerSecond = (int) (1000L/(now - mLastTime));

            mLastTime = now;
            return true;
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
        	//Log.d("pencil", "pencil falling animation triggered");
    		fallConfig.doAnimation = true;
    		fallConfig.startTime = System.currentTimeMillis();
    		fallAnimator.init(isInverted, tiltAngle, -tiltAngle);
    		tiltAngle = -tiltAngle; //for end of animation
    		angularVelocity = -angularVelocity; //for end of animation
        }
    }
    
    public void setHighScoresFromSharedPreferences()
    {
    	Log.d("pencil", "called setHighScoresFromSharedPreferences");
    	SharedPreferences settings = getContext().getSharedPreferences(HIGH_SCORE_SHARED_PREFS_NAME, PencilActivity.MODE_PRIVATE);
    	PencilView.highScores = (HashMap<String, Long>) settings.getAll();

    	for (String key : PencilView.highScores.keySet())
    	{
    		Log.d("pencil", "setHighScoresFromSharedPreferences: set local value "+key+", "+PencilView.highScores.get(key));
    	}
    }
    
    
    public void setGravityFromSharedPreferences()
    {
        SharedPreferences settings = getContext().getSharedPreferences(SETTINGS_SHARED_PREFS_NAME, PencilActivity.MODE_PRIVATE);
        String gravityFactorString = settings.getString("pref_gravity", "0.015");
        gravityFactor = Float.parseFloat(gravityFactorString);
        Log.d("pencil", "setGravityFromSharedPreferences: set local value pref_gravity, "+gravityFactor);
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

    	//set high scores from shared preferences if not yet done
    	setHighScoresFromSharedPreferences();
    	//set gravity from shared preferences
    	setGravityFromSharedPreferences();
    	
    	if (thread != null)
    	{
    		thread.interrupt();
    		thread = null;
    	}
    	
    	// create thread
        thread = new PencilThread(holder, getContext(), new Handler() {
        	
        	 @Override
             public void handleMessage(Message m) {
        		 if (m.getData().getBoolean("inverted"))
        		 {
        			 mStatusText.setVisibility(View.GONE);
        			 mStatusTextUD.setVisibility(m.getData().getInt("viz"));
	                 mStatusTextUD.setText(m.getData().getString("text"));
        		 } else
        		 {
        			 mStatusTextUD.setVisibility(View.GONE);
        			 mStatusText.setVisibility(m.getData().getInt("viz"));
	                 mStatusText.setText(m.getData().getString("text"));
        		 }
             }
        });
        
        thread.setGameState(PencilThread.STATE_RUNNING);
        
    	thread.setRunning(true);
    	thread.start();
    	
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {

    	//Log.d("pencil", "called surfaceDestroyed");
    	if (thread != null)
    	{

        	thread.balanceTimer.stop(false, System.currentTimeMillis());
        	thread.balanceTimer.updateHighScoreSharedPreferences();

    		thread.setRunning(false);
    		thread.interrupt();
    		thread = null;
    	}
    }
    public void setTextViews(TextView textView, TextViewUD textViewUD) {
        mStatusText = textView;
        mStatusTextUD = textViewUD;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	
    	if (event.getAction() == MotionEvent.ACTION_MOVE) {
            thread.mTouchX = event.getX();
            thread.mTouchY = event.getY();
        } else if (event.getAction() == MotionEvent.ACTION_UP)
        {
        	thread.mTouchX = -1;
        	thread.mTouchY = -1;
        	long now = System.currentTimeMillis();
        	thread.mLastTime = now;
    		thread.balanceTimer.start(now);
        } else
        {
        	thread.mTouchX = -1;
        	thread.mTouchY = -1;
        }

    	return true;
    }
}

