package com.pencilanimations;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

/**
 * Class that renders the falling pencil animation
 */
public class FallAnimator {

	//the drawable object
	private BitmapDrawable drawable;

	//bounds of pencil in standard orientation
	private int xLeftStandard, yTopStandard, xRightStandard, yBottomStandard,
		xLeftInverted, yTopInverted, xRightInverted, yBottomInverted;

	//bounds of pencil in inverted orientation
    private float pivotXStandard, pivotYStandard, pivotXInverted, pivotYInverted;

    //rotation pivot in standard and inverted orientation
    double initialTiltAngle, finalTiltAngle;
    
    //whether or not the view is flipped upside down
    boolean isInverted;
    
	/*constructor*/
	public FallAnimator(int xLeftStandard, int yTopStandard, int xRightStandard, int yBottomStandard,
			int xLeftInverted, int yTopInverted, int xRightInverted, int yBottomInverted,
			float pivotXStandard, float pivotYStandard,
			float pivotXInverted, float pivotYInverted,
			BitmapDrawable drawable)
	{
		this.xLeftStandard = xLeftStandard;
		this.yTopStandard = yTopStandard;
		this.xRightStandard = xRightStandard;
		this.yBottomStandard = yBottomStandard;
		
		this.xLeftInverted = xLeftInverted;
		this.yTopInverted = yTopInverted;
		this.xRightInverted = xRightInverted;
		this.yBottomInverted = yBottomInverted;
		
		this.pivotXStandard = pivotXStandard;
		this.pivotYStandard = pivotYStandard;
		this.pivotXInverted = pivotXInverted;
		this.pivotYInverted = pivotYInverted;
		this.drawable = drawable;	
	}
	
    /**
     * Initialize the fall animation.
     * 
     * @param boolean isInverted Whether or not the view is flipped upside down
     * @param double initialTiltAngle Initial pencil tilt angle to the horizontal
     * @param double finalTiltAngle Final pencil tilt angle to the horizontal
     */
	public void init(boolean isInverted, double initialTiltAngle, double finalTiltAngle)
	{
		Log.d("pencil", "init FallAnimator with initialTiltAngle="+initialTiltAngle+", finalTiltAngle="+finalTiltAngle);
		this.isInverted = isInverted;
		this.initialTiltAngle = initialTiltAngle;
		this.finalTiltAngle = finalTiltAngle;
	}
	
    /**
     * Draw one frame of the fall animation.
     * 
     * @param Canvas canvas The android canvas
     * @param float interpolation Progress of the animation. Between 0 and 1.
     */
	public void draw(Canvas canvas, float interpolation)
	{
		canvas.save();
		
		int xLeft, yTop, xRight, yBottom;
		float pivotX, pivotY;
		
		if (isInverted)
		{
			xLeft = xLeftInverted + (int) (((float) (xLeftStandard - xLeftInverted)) * interpolation);
			yTop = yTopInverted + (int) (((float) (yTopStandard - yTopInverted)) * interpolation);
			xRight = xRightInverted + (int) (((float) (xRightStandard - xRightInverted)) * interpolation);
			yBottom = yBottomInverted + (int) (((float) (yBottomStandard- yBottomInverted)) * interpolation);
			pivotX = pivotXInverted + (pivotXStandard - pivotXInverted) * interpolation;
			pivotY = pivotYInverted + (pivotYStandard - pivotYInverted) * interpolation;		
		} else
		{
			xLeft = xLeftStandard + (int) (((float) (xLeftInverted - xLeftStandard)) * interpolation);
			yTop = yTopStandard + (int) (((float) (yTopInverted - yTopStandard)) * interpolation);
			xRight = xRightStandard + (int) (((float) (xRightInverted - xRightStandard)) * interpolation);
			yBottom = yBottomStandard + (int) (((float) (yBottomInverted - yBottomStandard)) * interpolation);
			pivotX = pivotXStandard + (pivotXInverted - pivotXStandard) * interpolation;
			pivotY = pivotYStandard + (pivotYInverted - pivotYStandard) * interpolation;
		}

		double tiltAngle = initialTiltAngle + (finalTiltAngle - initialTiltAngle) * (double) interpolation;

		Log.d("pencil", "FallAnimator: draw with with tiltAngle="+tiltAngle+", pivotX="+pivotX+", pivotY="+pivotY);
		
		canvas.rotate((float) tiltAngle * 180.0f/((float) Math.PI), pivotX, pivotY);
		
    	drawable.setBounds(xLeft, yTop, xRight, yBottom);

    	drawable.draw(canvas);

    	canvas.restore();

	}

}
