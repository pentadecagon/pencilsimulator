package com.pencildisplay;

import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.pencilanimations.ExplosionConfig;

/** Contains methods to help with the display of the pencil simulator. */

public class PencilDisplayHelper {

	//height and width of the canvas
	private float mCanvasWidth, mCanvasHeight;
	
	//height and width of the pencil being displayed
	private float pencilDisplayWidth, pencilDisplayLength;
	
    /**
     * Constructor
     */
	public PencilDisplayHelper(float mCanvasWidth, float mCanvasHeight, float pencilDisplayWidth,
			float pencilDisplayLength)
	{
		this.mCanvasWidth = mCanvasWidth;
		this.mCanvasHeight = mCanvasHeight;
		this.pencilDisplayWidth = pencilDisplayWidth;
		this.pencilDisplayLength = pencilDisplayLength;
	}
	
    /**
     * Calculate max angle that the pencil is allowed to tilt.
     * 
     * return double angle Maximum angle that the pencil is allowed to tilt from the horizontal before it hits a wall
     */
	public double calculateMaxTiltAngle()
    {
    	//approximate contact point with the side, as measured from tip end of pencil
    	double contactPointDistance = Math.sqrt((0.95 * pencilDisplayLength) * (0.95 * pencilDisplayLength) + (0.5 * pencilDisplayWidth) * (0.5 * pencilDisplayWidth));
    	
    	//angle from the pencil's point to the contact point when the pencil is touching the side
    	float angle = (float) Math.asin((0.5 * (double) mCanvasWidth/ contactPointDistance))
    			//angle from the center of the pencil to the point where the pencil touches the side
    			- (float) Math.asin((0.5 * (double) pencilDisplayWidth/ contactPointDistance));
    	
    	return angle;
    }
	
    /**
     * Check if the user is touching the pencil.
     * 
     * @param float mTouchX The x-position of the touch on the screen
     * @param float mTouchY The y-position of the touch on the screen
     * @param double tiltAngle The pencil tilt angle to the horizontal
     * 
     * @return boolean True if the user is touching the pencil, otherwise false
     */
	public boolean isTouchInAreaOfPencil(float mTouchX, float mTouchY, double tiltAngle, boolean isInverted)
    {
		//if screen is inverted, translate mTouchX and mTouchY to their standard equivalents
		if (isInverted)
		{
			mTouchX = mCanvasWidth - mTouchX;
			mTouchY = mCanvasHeight - mTouchY;
		}
		
		//make the effective touch width a bit bigger because the pencil is a bit narrow
		float effectiveTouchWidth = 1.6f * pencilDisplayWidth;
    	//Log.d("pencil", "called isTouchInAreaOfPencil with mTouchX="+mTouchX+", mTouchY="+mTouchY);
    	/*check if the touch is in the area of the pencil*/        	
    	//if x or y is 0 
    	if (mTouchX < 1 || mTouchY < 1) {
    		//Log.d("pencil", "isTouchInAreaOfPencil false 1");
    		return false;
    	}

    	//x - w/2 and tiltAngle need to have the same sign
    	if (
    		(mTouchX < (0.5f * mCanvasWidth - 0.5f * effectiveTouchWidth) && (tiltAngle > 0))
    		|| (mTouchX > (0.5f * mCanvasWidth + 0.5f * effectiveTouchWidth) && (tiltAngle < 0))

    	) {
    		//Log.d("pencil", "isTouchInAreaOfPencil false 2");
    		return false;
    	}
    	
    	//if y is outside the range inhabited by the pencil
    	if (mTouchY < (mCanvasHeight - pencilDisplayLength * (float) Math.cos(tiltAngle))) {
    		//Log.d("pencil", "isTouchInAreaOfPencil false 3");
    		return false;
    	}

    	//if x and y are inside the pencil
    	boolean touch;
    	if (tiltAngle == 0)
    	{
    		touch = ((mTouchX >= (0.5f * mCanvasWidth - 0.5f * effectiveTouchWidth)) && (mTouchX <= (0.5f * mCanvasWidth + 0.5f * effectiveTouchWidth)));
    		//Log.d("pencil", "isTouchInAreaOfPencil : at 1: "+touch);
    	} else
    	{
    		float x = Math.abs(mTouchX - 0.5f * mCanvasWidth);
    		float yCenter = mCanvasHeight - x/((float) Math.tan(Math.abs(tiltAngle)));
    		float yRange =  0.5f * effectiveTouchWidth/ ((float) Math.sin(Math.abs(tiltAngle)));
    		touch = ((mTouchY >= (yCenter - yRange)) && (mTouchY <= (yCenter + yRange)));
    		//Log.d("pencil", "isTouchInAreaOfPencil : at 2: "+touch);
    	}
    	
    	return touch;      	
    }
    
    /**
     * Calculate the tilt angle from the touch position on the screen.
     * 
     * @param float mTouchX The x-position of the touch on the screen
     * @param float mTouchY The y-position of the touch on the screen
     * @param double angularOffset Desired radial angular offset between the touch position on the screen and the tilt angle of the pencil
     * 
     * @return double tiltAngle The tilt angle to the horizontal corresponding to the touched position on the screen
     */
    public double calculateTiltAngleFromTouchPosition(float mTouchX, float mTouchY, double angularOffset, boolean isInverted)
    {
		//if screen is inverted, translate mTouchX and mTouchY to their standard equivalents
		if (isInverted)
		{
			mTouchX = mCanvasWidth - mTouchX;
			mTouchY = mCanvasHeight - mTouchY;
		}
    	
    	if (mTouchX == 0) { return 0; }
    	
    	double x = (double) (mTouchX - 0.5f * mCanvasWidth);
    	double y = (double) (mCanvasHeight - mTouchY);
    	double tiltAngle = Math.atan(x/y) - angularOffset;
    	
    	return tiltAngle;
    }
    
    /**
     * Check if pencil is at rest, with no chance of moving.
     * 
     * @param double tiltAngle The pencil tilt angle to the horizontal
     * @param double maxTiltAngle The maximum allowed tilt angle before the pencil hits the wall
     * @param double angularVelocity The pencil's angular velocity
     * @param double theta The angle to the negative y-axis of the gravity + other acceleration force on the pencil
     * 
     * @return boolean True if pencil cannot be moved, otherwise false
     */
    public boolean isAtRest(double tiltAngle, double maxTiltAngle, double angularVelocity, double theta)
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
    
    public static String formatInterval(final long l)
    {
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        final long tenths = (long) (ms/100);
        StringBuilder strBuilder = new StringBuilder();
        if (hr > 0)
        {
        	//return String.format("%02d:%02d:%02d.%1d", hr, min, sec, tenths);
        	if (hr < 10) {
        		strBuilder.append("0");
        	}
        	strBuilder.append(hr);
        	strBuilder.append(":");
        	if (min < 10) {
        		strBuilder.append("0");
        	}
        	strBuilder.append(min);
        	strBuilder.append(":");
        	if (sec < 10) {
        		strBuilder.append("0");
        	}
        	strBuilder.append(sec);
        	strBuilder.append(".");
        	strBuilder.append(tenths);
        	return strBuilder.toString();
        } else if (min > 0)
        {
        	//return String.format("%02d:%02d.%1d", min, sec, tenths);
        	if (min < 10) {
        		strBuilder.append("0");
        	}
        	strBuilder.append(min);
        	strBuilder.append(":");
        	if (sec < 10) {
        		strBuilder.append("0");
        	}
        	strBuilder.append(sec);
        	strBuilder.append(".");
        	strBuilder.append(tenths);
        	return strBuilder.toString();
        } else
        {
        	//return String.format("%d.%1d", sec, tenths);
        	strBuilder.append(sec);
        	strBuilder.append(".");
        	strBuilder.append(tenths);
        	return strBuilder.toString();
        }
    }
    
    /**
     * Calculate position of explosion relative to pencil.
     * 
     * @param ExplosionConfig config The explosion config
     * @param double angularVelocity The pencil's angular velocity
     * @param double maxTiltAngle The maximum allowed tilt angle before the pencil hits the wall
     * @param boolean isInverted Whether or not screen is inverted
     * 
     * @return float X position of the explosion relative to the pencil
     * @return float Y position of the explosion relative to the pencil
     */
    public int[] getExplosionPosition(ExplosionConfig config, double angularVelocity, double maxTiltAngle,
    		boolean isInverted)
    {
    	angularVelocity = Math.abs(angularVelocity);

    	int[] position = new int[2];
    	
    	if (isInverted)
    	{
    		position[0] = ((config.direction < 0) ? ((int) ( 0.985 * mCanvasWidth)) : ((int) (0.015 * mCanvasWidth)));
    		position[1] = (int) (1.0 * pencilDisplayLength * (float) Math.cos(maxTiltAngle));
    	} else
    	{   	
	    	float paddingX, paddingY;
	    	float limit1 = 0.3f;
	    	float limit2 = 0.9f;
	    	if (angularVelocity > limit2)
	    	{
	    		paddingX = 0.01f * mCanvasWidth;
	    		paddingY = 1.02f * pencilDisplayLength * (float) Math.cos(maxTiltAngle);
	    	} else if (angularVelocity < limit1)
	    	{
	    		paddingX = 0.0f;
	    		paddingY = 0.97f * pencilDisplayLength * (float) Math.cos(maxTiltAngle);
	    	} else
	    	{
	    		float progress = ((float) angularVelocity - limit1)/(limit2 - limit1);
	    		paddingX = progress * 0.01f * mCanvasWidth;
	    		paddingY = (0.97f + progress * 0.05f) * pencilDisplayLength * (float) Math.cos(maxTiltAngle);
	    	}
	    	position[0] = ((config.direction > 0) ? ((int) (mCanvasWidth - paddingX)) : ((int) (paddingX)));
	    	position[1] = (int) (mCanvasHeight - paddingY);
    	}
    	
    	return position;       	
    }

	
}
