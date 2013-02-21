package com.explode3;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;

public class Exploder3 {

	//the image used to represent the explosion
	BitmapDrawable image;
	
    /**
     * Constructor
     * 
     * @param BitmapDrawable image The image used to represent the explosion
     */

	public Exploder3(BitmapDrawable image) {
		this.image = image;
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
     * @return int explosionDuration Duration of the explosion in iterations of the animation 
     */
    public int getExplosionDuration(double angularVelocity)
    {
    	return 10;
    }
	
    /**
     * Draw one frame of the explosion animation.
     * 
     * @param Canvas canvas The android canvas
     * @param int absoluteDisplacementX The x-position of the center of the image on the canvas
     * @param int absoluteDisplacementY The y-position of the center of the image on the canvas
     * @param float interpolation Progress of the animation. Between 0 and 10.
     * @param float explosionScale Parameter giving the relative size of the explosion
     */
	public void draw(Canvas canvas, int absoluteDisplacementX, int absoluteDisplacementY, int interpolation, float explosionScale) {
		int explosionSize = 3 + (int) (0.25f * explosionScale * interpolation * interpolation);
		
		int xLeftExp = absoluteDisplacementX - explosionSize;
    	int yTopExp = absoluteDisplacementY - explosionSize;
    	int xRightExp = absoluteDisplacementX + explosionSize;
    	int yBottomExp = absoluteDisplacementY + explosionSize;
		
    	image.setBounds(xLeftExp, yTopExp, xRightExp, yBottomExp);
    	image.draw(canvas);
	}

}
