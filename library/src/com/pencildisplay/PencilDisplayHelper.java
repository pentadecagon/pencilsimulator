package com.pencildisplay;

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
	public boolean isTouchInAreaOfPencil(float mTouchX, float mTouchY, double tiltAngle)
    {
    	//Log.d("pencil", "called isTouchInAreaOfPencil with mTouchX="+mTouchX+", mTouchY="+mTouchY);
    	/*check if the touch is in the area of the pencil*/        	
    	//if x or y is 0 
    	if (mTouchX < 1 || mTouchY < 1) {
    		//Log.d("pencil", "isTouchInAreaOfPencil false 1");
    		return false;
    	}

    	//x - w/2 and tiltAngle need to have the same sign
    	if (
    		(mTouchX < (0.5f * mCanvasWidth - 0.5f * pencilDisplayWidth) && (tiltAngle > 0))
    		|| (mTouchX > (0.5f * mCanvasWidth + 0.5f * pencilDisplayWidth) && (tiltAngle < 0))

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
    		touch = ((mTouchX >= (0.5f * mCanvasWidth - 0.5f * pencilDisplayWidth)) && (mTouchX <= (0.5f * mCanvasWidth + 0.5f * pencilDisplayWidth)));
    		//Log.d("pencil", "isTouchInAreaOfPencil : at 1: "+touch);
    	} else
    	{
    		float x = Math.abs(mTouchX - 0.5f * mCanvasWidth);
    		float yCenter = mCanvasHeight - x/((float) Math.tan(Math.abs(tiltAngle)));
    		float yRange =  0.5f * pencilDisplayWidth/ ((float) Math.sin(Math.abs(tiltAngle)));
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
    public double calculateTiltAngleFromTouchPosition(float mTouchX, float mTouchY, double angularOffset)
    {
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
	
}
