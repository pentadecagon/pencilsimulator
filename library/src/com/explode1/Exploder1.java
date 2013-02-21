package com.explode1;

import java.util.ArrayList;
import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Region;
import android.os.SystemClock;

public class Exploder1 {

    private ArrayList<BitmapFragment> fragments;
    
    //the image used to represent the explosion
    Bitmap image;

    //The x-point within the image at which the explosion will start. Between 0 and 1.
    public static float DEFAULT_BREAKPOINT_X = 0.5f;
    
    //The y-point within the image at which the explosion will start. Between 0 and 1.
    public static float DEFAULT_BREAKPOINT_Y = 0.5f;

    /**
     * Standard constructor
     * 
     * @param Bitmap image The image used to represent the explosion
     */
    public Exploder1(Bitmap image) {
    	this(image, DEFAULT_BREAKPOINT_X, DEFAULT_BREAKPOINT_Y);
    }
    
    /**
     * Constructor that allows custom breakpoints for the explosion
     * 
     * @param Bitmap image The image used to represent the explosion
     * @param float breakpointX The x-point within the image at which the explosion will start. Between 0 and 1.
     * @param float breakpointY The y-point within the image at which the explosion will start. Between 0 and 1.
     */
    public Exploder1(Bitmap image, float breakpointX, float breakpointY) {
    	this.image = image;

    	init();
    	prepare(breakpointX, breakpointY);
    }
    
    /**
     * Get relative size of the explosion based on the angular speed (if the pencil hits harder, the explosion is bigger).
     * 
     * @param double angularVelocity The angular speed
     * @return float explosionScale Relative size of the explosion 
     */
    public float getExplosionScale(double angularVelocity)
    {
    	return (1.8f * (float) (angularVelocity * angularVelocity));
    }
    
    /**
     * Calculate the duration of the explosion based on the angular speed (if the pencil hits harder, the explosion is bigger).
     * 
     * @param double angularVelocity The angular speed
     * @return int explosionDuration Duration of the explosion in iterations of the animation 
     */
    public int getExplosionDuration(double angularVelocity)
    {
    	return ((int) (17 * (float) Math.abs(angularVelocity)) + 1);
    }

    /**
     * Initialize the explosion animation
     */
    public void init() {
        Random rnd=new Random(SystemClock.uptimeMillis());

        final int w=image.getWidth();
        final int h=image.getHeight();
        final int sliceW=w/BitmapFragment.SLICES_WIDTH;
        final int sliceH=h/BitmapFragment.SLICES_HEIGHT;

        fragments=new ArrayList<BitmapFragment>(BitmapFragment.SLICES_WIDTH*BitmapFragment.SLICES_HEIGHT);
        for(int i=0;i<BitmapFragment.SLICES_WIDTH;i++){
            int x=i*sliceW;
            for(int j=0;j<BitmapFragment.SLICES_HEIGHT;j++){
                int y=j*sliceH;
                Bitmap part=Bitmap.createBitmap(image,x,y,sliceW,sliceH);
                Path tri=new Path();
                tri.moveTo(0,0);
                for(float jj=0;jj<=1;jj+=.2){
                    tri.lineTo(rnd.nextInt(sliceW), sliceH * jj);
                }
                tri.lineTo(0,sliceH);
                tri.close();

                fragments.add(new BitmapFragment(part, x, y, w, h, Region.Op.DIFFERENCE, tri));
                fragments.add(new BitmapFragment(part, x, y, w, h, Region.Op.REPLACE, tri));
            }
        }        
    }

    /**
     * Prepare the explosion animation
     * 
     * @param float breakpointX The x-point within the image at which the explosion will start. Between 0 and 1.
     * @param float breakpointY The y-point within the image at which the explosion will start. Between 0 and 1.
     */
    public void prepare(float breakpointX, float breakpointY)
    {
    	for (int i = 0; i < fragments.size(); i++)
    	{
        	fragments.get(i).prepare(Math.round(breakpointX * image.getWidth()), Math.round(breakpointY * image.getHeight()));
    	}
    }

    /**
     * Draw one frame of the explosion animation.
     * 
     * @param Canvas canvas The android canvas
     * @param int absoluteDisplacementX The x-position of the center of the image on the canvas
     * @param int absoluteDisplacementY The y-position of the center of the image on the canvas
     * @param float interpolation Progress of the animation. Between 0 and 1.
     */
    public void draw(Canvas canvas, int absoluteDisplacementX, int absoluteDisplacementY, float interpolation) {

    	for (int i = 0; i < fragments.size(); i++)
    	{
    		BitmapFragment part=fragments.get(i);
    		
    		int diffX=part.destX-part.sourceX;
            int diffY=part.destY-part.sourceY;
            
            int drawX=part.sourceX+(Math.round(diffX*interpolation));
            int drawY=part.sourceY+(Math.round(diffY*interpolation));

            canvas.save();
            canvas.translate(absoluteDisplacementX-image.getWidth()/2+drawX, absoluteDisplacementY-image.getHeight()/2+drawY);
            canvas.clipPath(part.triangle, part.op);
            canvas.drawBitmap(part.bmp, 0, 0, null);
            canvas.restore();
    	} 
    }	
}
