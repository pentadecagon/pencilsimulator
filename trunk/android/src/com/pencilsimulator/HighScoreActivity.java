package com.pencilsimulator;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HighScoreActivity extends Activity {
	
	private Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_high_score);
		
		TextView title = (TextView) findViewById(R.id.highScoreTitle);
		title.setText("High score @ gravity " + SettingsActivity.gravityFactor);
		
		LinearLayout progress = (LinearLayout) findViewById(R.id.highScoreProgress);
    	progress.setVisibility(View.VISIBLE);
    	
    	TextView number = (TextView) findViewById(R.id.highScoreNumber);
    	number.setVisibility(View.GONE);
		
    	//need to add a delay because sometimes the high score takes a while to be updated by a thread in PencilView
		handler.postDelayed(new ViewUpdater(), 3000);
		
	}
	
	private long highScoreForCurrentGravity()
	{
		/*
		//check if the high score for the amount of time the pencil has been in the air has been beaten
    	SharedPreferences settings = getSharedPreferences("PencilHighScores", PencilActivity.MODE_PRIVATE);
    	//get the current high score for this gravity
    	String key = "highscore_grav_"+SettingsActivity.gravityFactor;
    	long highScore = settings.getLong(key, 0L);
    	return highScore;*/
		String key = "highscore_grav_"+SettingsActivity.gravityFactor;
    	Long highScore = PencilView.highScores.get(key);
    	if (highScore != null)
    	{
    		return highScore;
    	}
    	return 0L;
	}
	
	private class ViewUpdater implements Runnable{

        @Override
        public void run() {
        	long highScore = highScoreForCurrentGravity();

        	LinearLayout progress = (LinearLayout) findViewById(R.id.highScoreProgress);
        	progress.setVisibility(View.GONE);
        	
    		TextView number = (TextView) findViewById(R.id.highScoreNumber);
    		number.setVisibility(View.VISIBLE);

    		if (highScore > 0)
    		{
    			number.setText(highScore + " sec");
    		} else
    		{
    			number.setText("No high score yet");
    		}
        }
    }
	
	
	
	
	
}
