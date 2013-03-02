package com.pencilsimulator;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/** Show a pencil balanced on its tip, falling over. */

public class PencilActivity extends Activity implements SensorEventListener {

    /** A handle to the View in which the game is running. */
    private PencilView mPencilView;

    /**
     * Motion sensor variables
     */
    
    /**
     * Gravity/ acceleration array
     * 0 = x
     * 1 = y
     * 2 = z
     */
    float[] gravityData = new float[3];
    
    /**
     * Manager of the acceleration sensor
     */
    private SensorManager sensorManager;
    
    /**
     * Accelerometer, which measures acceleration
     */
	private Sensor accelerometer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pencil);
		
		Log.d("pencil", "called onCreate");

		//get a handle to the view that will display the animation
		mPencilView = (PencilView) findViewById(R.id.pencil);

		mPencilView.setTextViews((TextView) findViewById(R.id.text), (TextViewUD) findViewById(R.id.text_ud));
		
        //initialize motion sensor
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_pencil, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.menu_gravity)
		{
			Activity ac = PencilActivity.this;
			Intent i = new Intent(ac, SettingsActivity.class);
			//make sure there's only one of each type of activity running
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			ac.startActivity(i);
			
			//startActivity(new Intent(PencilActivity.this, SettingsActivity.class));
			
			return true;
		} else if (item.getItemId() == R.id.menu_high_score)
		{
			Activity ac = PencilActivity.this;
			Intent i = new Intent(ac, HighScoreActivity.class);
			//make sure there's only one of each type of activity running
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			ac.startActivity(i);
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		//stop motion sensor on pause to avoid running down battery
		sensorManager.unregisterListener(this);
		super.onPause();
	}
	
    /**
     * Handle sensor detection
     */
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				System.arraycopy(event.values, 0, gravityData, 0, 3);
				break;
	         
			default:
				Log.d("sensor", "unidentified sensor type!");
				return;
		}
		
		if (gravityData != null) {
			//convert x, y to polar coordinates
			//the magnitude of the force is always the total magnitude including the z-component
			double g = Math.sqrt((gravityData[0] * gravityData[0] + gravityData[1] * gravityData[1] + gravityData[2] * gravityData[2]));
			//we only used the force in the x/y plane
			double theta = Math.atan2(gravityData[0], gravityData[1]);

			if (mPencilView.thread != null)
			{
				mPencilView.thread.setAccelerationData(g, theta);
			}
			
		} else
		{
			Log.d("sensor", "could not find geo array");
		}
	}
	
	//blank function: just needed for implementation of SensorEventListener
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
