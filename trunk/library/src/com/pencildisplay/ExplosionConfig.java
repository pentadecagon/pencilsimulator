package com.pencildisplay;

/**
 * Holds config parameters for an explosion
 */

public class ExplosionConfig {
	 public boolean doExplosion = false;
	 public int explosionIteration = 0;
	 public float explosionScale = 1.0f;
	 public int explosionDuration = 15;
	 public int explosionXPosition;
	 public int explosionYPosition;
	 public int direction = 1;
	 
	 public ExplosionConfig(int direction)
	 {
		 this.direction = direction;
	 }
}
