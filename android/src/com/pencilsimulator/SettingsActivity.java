package com.pencilsimulator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SettingsActivity extends Activity {

	private SeekBar bar; // declare seekbar object variable
	
	// declare text label objects
	private TextView gravityTextProgress;

	private float gravityFactorLocal;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		Log.d("pencil", "called SettingsActivity.onCreate");
		
		gravityFactorLocal = PencilView.gravityFactor;
		
		//initialise slider
        bar = (SeekBar)findViewById(R.id.seekBar1); // make seekbar object
        bar.setOnSeekBarChangeListener(gravitySeekBarListener); // set seekbar listener.

        // since we are using this class as the listener the class is "this"        
        // make text label for progress value
        gravityTextProgress = (TextView)findViewById(R.id.textViewProgress);
        gravityTextProgress.setText(gravityFactorLocal + "");
        
        bar.setProgress((int) (gravityFactorLocal * 200f) - 1);
        
        Button go = (Button) findViewById(R.id.go);
        go.setOnClickListener(goOnClickListener);
	}
	
	private GravitySeekBarListener gravitySeekBarListener = new GravitySeekBarListener();
	    
	private class GravitySeekBarListener implements OnSeekBarChangeListener {
		//set onchanged activity for sliders
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// change progress text label with current seekbar value
			gravityFactorLocal = (progress + 1)/200f;
			gravityTextProgress.setText(gravityFactorLocal + "");
		}
	
		//set method for when user stops dragging slider
		public void onStopTrackingTouch(SeekBar seekBar) {
			seekBar.setSecondaryProgress(seekBar.getProgress()); // set the shade of the previous value. 	
		}
	
		//set method for when user starts dragging slider
		public void onStartTrackingTouch(SeekBar seekBar) {
		}	
	}
	
	private OnClickListener goOnClickListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			if (gravityFactorLocal > 0 && gravityFactorLocal <= 1)
			{
				PencilView.gravityFactor = gravityFactorLocal;
				
				//update shared preferences
				SharedPreferences settings = getApplicationContext().getSharedPreferences(PencilView.SETTINGS_SHARED_PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
	            editor.putString("pref_gravity", gravityFactorLocal+"");
	            editor.commit();
			} else
			{
				Log.d("pencil", "found unexpected value of gravityFactorLocal: "+gravityFactorLocal);
			}
            
			Activity ac = SettingsActivity.this;
			Intent i = new Intent(ac, PencilActivity.class);
			//make sure there's only one of each type of activity running
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			ac.startActivity(i);
			
			//startActivity(new Intent(SettingsActivity.this, PencilActivity.class));
		}
	};
}
