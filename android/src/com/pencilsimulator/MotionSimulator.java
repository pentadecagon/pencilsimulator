package com.pencilsimulator;

/** calculate current angular displacement and velocity of falling pencil */

public class MotionSimulator {

	double l=100.0; //length

	public MotionSimulator(double l)
	{
		this.l = l;
	}

	/**
     * Current new angular displacement and velocity from previous values.
     *
     * @param double x Current angular dislacement in m
     * @param double v Current angular velocity in m
     * @param double g Magnitude of gravitational acceleration
     * @param double theta Angle of direction of gravitational force to the negative y axis
     * @param double dt Time in seconds elapsed since the last calculation
     * 
     * @return double xf New angular displacement
     * @return double vf New angular velocity
     */
    public double[] calc(double x, double v, double g, double theta, double dt)
    {
        double x1 = x;
        double v1 = v;
        double a1 = acceleration(x1, g, theta);

        double x2 = x + 0.5*v1*dt;
        double v2 = v + 0.5*a1*dt;
        double a2 = acceleration(x2, g, theta);

        double x3 = x + 0.5*v2*dt;
        double v3 = v + 0.5*a2*dt;
        double a3 = acceleration(x3, g, theta);

        double x4 = x + v3*dt;
        double v4 = v + a3*dt;
        double a4 = acceleration(x4, g, theta);

        double xf = x + (dt/6.0)*(v1 + 2*v2 + 2*v3 + v4);
        double vf = v + (dt/6.0)*(a1 + 2*a2 + 2*a3 + a4);

        double[] returnArray = {xf, vf};
        return returnArray;
    
    }
    
	/**
     * Angular velocity equation.
     *
     * @param double x Current angular dislacement in m
     * @param double theta Angle of direction of gravitational force to the negative y axis
     * @param double dt Time in seconds elapsed since the last calculation
     * 
     * @return double v Angular velocity in m
     */
    public double acceleration(double x, double g, double theta)
    {
    	return (g/l)*Math.sin(x - theta);
    }
}
