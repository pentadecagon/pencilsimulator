package com.pencilsimulator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorManager;
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

/** Show a pencil balanced on its tip, falling over. */

class PencilView extends SurfaceView implements SurfaceHolder.Callback {

    class PencilThread extends Thread {
  
    	//gravitational parameter
    	private double g = 9.81;
    	
    	//artificial parameter we use to reduce the size of gravity for testing because pencil falls over too fast
    	//with real gravity
    	final private double GRAVITY_REDUCTION_FACTOR = 0.01;
    	
    	//angle of gravitational force from negative y axis
    	private double theta = 0.0;

    	//length of pencil as used in physical calculations
    	private double pencilPhysicalLength = 0.1;
    	
    	//motion simulator used to animate pencil
    	private MotionSimulator motionSimulator = new MotionSimulator(pencilPhysicalLength);
    	
    	//display length of pencil
    	private float pencilDisplayLength;
    	
    	//display length of pencil end
    	private float pencilEndDisplayLength;

    	//display width of pencil
    	private float pencilDisplayWidth;
    	
    	//distance of floor from top of screen
    	private float floorLevel;
    	
    	//location of pencil point
    	private float xPencilPoint, yPencilPoint;
    	
    	//maximum angle that the pencil is allowed to tilt before it hits the side of the box
    	private float maxTiltAngle;

    	//initial angular displacement
    	final private double INITIAL_TILT_ANGLE = -Math.PI/50.0;
    	
    	//angular displacement
    	private double tiltAngle = INITIAL_TILT_ANGLE;
    	
    	//initial angular velocity
    	final private double INITIAL_ANGULAR_VELOCITY = 0.0;
    	
    	//angular velocity
    	private double angularVelocity = INITIAL_ANGULAR_VELOCITY;
    	
    	//color of sky
    	int skyColor = android.graphics.Color.parseColor("#E0EAF4");
    	
    	//color of floor
    	int floorColor = android.graphics.Color.parseColor("#A6D785");
    	
    	//pencil colors
    	int[] pencilColors = {
    			android.graphics.Color.parseColor("#FF0000"), //body
    			android.graphics.Color.parseColor("#EFDD6F"), //tip
    	};
    	
        /*
         * State-tracking constants
         */
        public static final int STATE_LOSE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;

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
        
        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** Paint to draw the lines on screen. */
        private Paint mLinePaint;

        /** The state of the game. One of READY, RUNNING, PAUSE or LOSE */
        private int mMode;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;
        
        /** Used to figure out elapsed time between frames */
        private long mLastTime;

        public PencilThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
        	mSurfaceHolder = surfaceHolder;
            mHandler = handler;

            // Initialize paints
            mLinePaint = new Paint();
            mLinePaint.setAntiAlias(true);
            
            showStartMessage();
        }

    	/**
         * Show a message telling the user to tap to start
         */
        private void showStartMessage()
        {
        	Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString("text", getContext().getResources().getString(R.string.pencil_start_message));
            b.putInt("viz", View.VISIBLE);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
        
        /**
         * Starts the game
         */
        public void doStart() {

            synchronized (mSurfaceHolder) {

            	//hide the information message
            	Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", "");
                b.putInt("viz", View.GONE);
                msg.setData(b);
                mHandler.sendMessage(msg);
            	
            	//initialize position of pencil
            	tiltAngle = INITIAL_TILT_ANGLE;
            	angularVelocity = INITIAL_ANGULAR_VELOCITY;
            	
            	//start clock
            	mLastTime = System.currentTimeMillis() + 100;
            	
            	//set the game running
                setState(STATE_RUNNING);
            }
        }

    	/**
         * Run the game
         */
        @Override
        public void run() {

            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) updatePhysics();
                        doDraw(c);
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
                
                //initialize drawing dimension parameters
                pencilDisplayLength = 0.8f * mCanvasHeight;
                pencilEndDisplayLength = 0.14f * pencilDisplayLength + 0.1f;
                pencilDisplayWidth = 0.05f * pencilDisplayLength + 1.0f;
                floorLevel = 0.67f * mCanvasHeight;

                //position of the point, on which the pencil is balanced
                xPencilPoint = 0.5f * mCanvasWidth;
                yPencilPoint = mCanvasHeight;

                maxTiltAngle = (float) Math.asin((0.5 * mCanvasWidth/ ((double) pencilDisplayLength)));
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

            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
            canvas.drawColor(Color.BLACK);

            drawBackground(canvas);
            
            drawPencil(canvas);
        }
        
        /**
         * Draw the background
         */
        private void drawBackground(Canvas canvas)
        {
        	//draw backdrop
        	canvas.drawColor(skyColor);
        	
        	//draw floor
        	mLinePaint.setColor(floorColor);
        	mLinePaint.setStyle(Paint.Style.FILL);
        	 
        	Path path = new Path();
        	path.moveTo(0.0f, floorLevel);
        	path.lineTo(mCanvasWidth, floorLevel);
        	path.lineTo(mCanvasWidth, mCanvasHeight);
        	path.lineTo(0.0f, mCanvasHeight);
        	path.lineTo(0.0f, floorLevel);

        	canvas.drawPath(path, mLinePaint);
        	
        }
        
        /**
         * Draw the pencil
         */
        private void drawPencil(Canvas canvas)
        {
        	float so = (float) Math.sin(tiltAngle);
            float co = (float) Math.cos(tiltAngle);
 
            //draw the rectangular body of the pencil
            float[] pencilBodyXCoordinates = {
            		xPencilPoint + pencilDisplayLength * so - 0.5f * pencilDisplayWidth * co, //top left
            		xPencilPoint + pencilDisplayLength * so + 0.5f * pencilDisplayWidth * co, //top right
            		xPencilPoint + pencilEndDisplayLength * so + 0.5f * pencilDisplayWidth * co, //bottom right
            		xPencilPoint + pencilEndDisplayLength * so - 0.5f * pencilDisplayWidth * co //bottom left          		
            };          
            float[] pencilBodyYCoordinates = {
            		yPencilPoint - pencilDisplayLength * co - 0.5f * pencilDisplayWidth * so, //top left
            		yPencilPoint - pencilDisplayLength * co + 0.5f * pencilDisplayWidth * so, //top right
            		yPencilPoint - pencilEndDisplayLength * co + 0.5f * pencilDisplayWidth * so, //bottom right
            		yPencilPoint - pencilEndDisplayLength * co - 0.5f * pencilDisplayWidth * so //bottom left   		
            };   	
        	drawShape(canvas, pencilColors[0], pencilBodyXCoordinates, pencilBodyYCoordinates);
        	
        	//draw the triangular end of the pencil
        	float[] pencilEndXCoordinates = {
        			xPencilPoint,
        			pencilBodyXCoordinates[2],
        			pencilBodyXCoordinates[3],      	
        	};      	
        	float[] pencilEndYCoordinates = {
        			yPencilPoint,
        			pencilBodyYCoordinates[2],
        			pencilBodyYCoordinates[3],      	
        	};
        	drawShape(canvas, pencilColors[1], pencilEndXCoordinates, pencilEndYCoordinates);
        }
        
        /**
         * Draw a generic shape of one color.
         * 
         * @param Canvas The canvas object
         * @param int color The color of the shape
         * @param float[] xCoordinates list of x-coordinates of the shape
         * @param float[] yCoordinates list of y-coordinates of the shape
         */
        private void drawShape(Canvas canvas, int color, float[] xCoordinates, float[] yCoordinates)
        {
            mLinePaint.setColor(color);
            mLinePaint.setStyle(Paint.Style.FILL);

        	Path path = new Path();
        	path.moveTo(xCoordinates[0], yCoordinates[0]);
        	for (int i = 1; i < xCoordinates.length; i++)
        	{
        		path.lineTo(xCoordinates[i], yCoordinates[i]);
        	}
        	
        	canvas.drawPath(path, mLinePaint);
        }
        
        /**
         * Calculate the angular displacement and speed of the pencil
         */
        private void updatePhysics() {
	
        	long now = System.currentTimeMillis();
        	// Do nothing if mLastTime is in the future.
            // This allows the game-start to delay the start of the physics
            // by 100ms or whatever.
            if (mLastTime > now) return;
            
            double elapsed = (now - mLastTime) / 1000.0;

            //do calculations
            //Log.d("pencil", "calculating new position and velocity with g = "+GRAVITY_REDUCTION_FACTOR*g+", theta="+theta);
            double[] motionArray = motionSimulator.calc(tiltAngle, angularVelocity, GRAVITY_REDUCTION_FACTOR*g, theta, elapsed);
            tiltAngle = motionArray[0];
            angularVelocity = motionArray[1];
            
            //if the pencil has fallen over horizontally, indicate that the game is over and set it to "lose" mode
            if (hasReachedMaxTiltAngle(tiltAngle))
            {
            	//make sure the pencil is shown as lying flat on the ground
            	tiltAngle = (tiltAngle > 0) ? maxTiltAngle : - maxTiltAngle;
            	angularVelocity = 0.0;
            }
            
            mLastTime = now;
        }

        /**
         * Check if the pencil has reached its maximum tilt angle and hit the side of the box.
         * 
         * @param double tiltAngle The pencil's angular displacement to the vertical, in radians
         * 
         * @return boolean true if the pencil has hit the side of the box, otherwise false
         */
        private boolean hasReachedMaxTiltAngle(double tiltAngle)
        {
        	return (Math.abs(tiltAngle) > maxTiltAngle);
        }

        public void setAccelerationData(double g, double theta)
        {
        	this.g = g;
        	this.theta = theta;
        }
    }
    
    /** Pointer to the text view to display "Paused.." etc. */
    private TextView mStatusText;

    /** The thread that actually draws the animation */
    public PencilThread thread = null;

    public PencilView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        setFocusable(true); // make sure we get key events
    }

    /**
     * Handle an ontouch event
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	
    	Log.d("pencil", "called onTouchEvent, thread.mMode="+thread.mMode);
    	//if the game is either in "ready" (not started yet) or "lose" mode, start the game from the beginning
    	if (thread.mMode == PencilThread.STATE_READY)
    	{
    		thread.doStart();
    	}
    	
    	return true;
    }

    /**
     * Installs a pointer to the text view used for messages.
     */
    public void setTextView(TextView textView) {
        mStatusText = textView;
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

    	if (thread != null)
    	{
    		thread.interrupt();
    		thread = null;
    	}
    	
    	// create thread
        thread = new PencilThread(holder, getContext(), new Handler() {
            @Override
            public void handleMessage(Message m) {
            	mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));   					

            }
        });
        thread.setState(PencilThread.STATE_READY);

    	thread.setRunning(true);
    	thread.start();

    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {

    	if (thread != null)
    	{
    		thread.setRunning(false);
    		thread.interrupt();
    		thread = null;
    	}
    }
}

