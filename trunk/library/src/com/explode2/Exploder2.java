package com.explode2;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;

public class Exploder2 {
	
	//The series of images that will be shown in turn to animate the explosion
	BitmapDrawable[] images;
	
    /**
     * Constructor
     * 
     * @param BitmapDrawable[] images The series of images that will be shown in turn to animate the explosion
     */
	public Exploder2(BitmapDrawable[] images) {
		this.images = images;
    }
	
    /**
     * Get relative size of the explosion based on the angular speed (if the pencil hits harder, the explosion is bigger).
     * 
     * @param double angularVelocity The angular speed
     * @return float explosionScale Relative size of the explosion 
     */
    public float getExplosionScale(double angularVelocity)
    {
    	return (2.5f * (float) Math.abs(angularVelocity));
    }
    
    /**
     * Calculate the duration of the explosion based on the angular speed (if the pencil hits harder, the explosion is bigger).
     * 
     * @param double angularVelocity The angular speed
     * @return long explosionDuration Duration of the explosion in miliseconds
     */
    public long getExplosionDuration(double angularVelocity)
    {
    	return 100L;
    }

    /**
     * Draw one frame of the explosion animation.
     * 
     * @param Canvas canvas The android canvas
     * @param int absoluteDisplacementX The x-position of the center of the image on the canvas
     * @param int absoluteDisplacementY The y-position of the center of the image on the canvas
     * @param float interpolation Progress of the animation. Between 0 and 1.
     * @param float explosionScale Parameter giving the relative size of the explosion
     */ 
	public void draw(Canvas canvas, int absoluteDisplacementX, int absoluteDisplacementY, int interpolation, float explosionScale) {
		
		int explosionSize = 12 + (int) (4.0f * explosionScale * explosionScale);
		
		int xLeftExp = absoluteDisplacementX - explosionSize;
    	int yTopExp = absoluteDisplacementY - explosionSize;
    	int xRightExp = absoluteDisplacementX + explosionSize;
    	int yBottomExp = absoluteDisplacementY + explosionSize;
		
    	if (interpolation < images.length)
    	{
    		images[interpolation].setBounds(xLeftExp, yTopExp, xRightExp, yBottomExp);
    		images[interpolation].draw(canvas);
    	}
	}
}
