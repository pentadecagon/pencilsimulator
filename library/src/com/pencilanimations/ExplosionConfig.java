package com.pencilanimations;

/**
 * Holds config parameters for an explosion
 */

public class ExplosionConfig {
	 //whether explosion is in progress at this time
	 public boolean doExplosion = false;
	 //relative size of explosion
	 public float explosionScale = 1.0f;
	 //duration in miliseconds
	 public long explosionDuration = 100;
	 //x-position from which explosion should start
	 public int explosionXPosition;
	 //y-position from which explosion should start
	 public int explosionYPosition;
	 //explosion direction: 1 = right-hand wall, -1 = left-hand wall
	 public int direction = 1;
	 //start time of explosion
	 public long explosionStartTime;
	 
	 public ExplosionConfig(int direction)
	 {
		 this.direction = direction;
	 }
}
