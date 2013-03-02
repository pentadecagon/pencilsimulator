package com.pencilsimulator;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.pencildisplay.PencilDisplayHelper;
import com.pencilsimulator.PencilView.PencilThread;

/**
 * Times how long the pencil has been in balance.
 */

public class BalanceTimer {
    
    //the last time that the balance timer started
    public long balanceStartTime = -1;
    //the last score, formatted for display, achieved through balancing the pencil
    public String balanceLastScore = "0.0";
    //the last time that the balance timer stopped
    public long balanceStopTime = -1;
    //the minimum score for the user to achieve for the timer to be paused
    final public static long BALANCE_PAUSE_GAME_MINIMUM_SCORE = 2000;
    //the amount of time to wait before pausing the timer
    final public static long BALANCE_PAUSE_GAME_DELAY_PERIOD = 250;
    
    //timer is running
    final public static int BALANCE_TIMER_STATE_RUNNING = 0;
    //timer is stopped
    final public static int BALANCE_TIMER_STATE_STOPPED = 1;
    //timer is delayed
    final public static int BALANCE_TIMER_STATE_DELAYED = 2;
    
    //current state of timer
    public int state = BALANCE_TIMER_STATE_STOPPED;
    
    //the activity using the timer
    private PencilActivity context;
    
    //the thread using the timer
    private PencilThread ownerThread;

    /**
     * Constructor
     * 
     * @param Context context The activity using the timer
     * @param PencilThread ownerThread The thread using the timer
     */
    public BalanceTimer(Context context, PencilThread ownerThread)
    {
    	this.context = (PencilActivity) context;
    	this.ownerThread = ownerThread;
    }
    
    /**
     * Set the state of the timer, either running, delayed or stopped
     * 
     * @param int state The state of the timer
     */
    public void setTimerState(int state)
    {
    	this.state = state;
    }
    
    /**
     * Start the timer
     * 
     * @param long now The current time in milliseconds
     */
    public void start(long now)
    {
    	Log.d("pencil", "stopBalanceTimer.start called");
    	
    	balanceStartTime = now;
    	balanceStopTime = -1;
    	setTimerState(BALANCE_TIMER_STATE_RUNNING);
    	//set game state
    	this.ownerThread.setGameState(PencilThread.STATE_RUNNING);
    }
    
    /**
     * Stop the timer
     * 
     * @param boolean updateSharedPreferences Whether or not to update the shared preference high scores
     * @param boolean pauseGame Whether or not the game should be paused after an interval given by BALANCE_PAUSE_GAME_DELAY_PERIOD
     * @param long now The current time in milliseconds
     */
    public void stop(boolean pauseGame, long now)
    {
    	if (state != BALANCE_TIMER_STATE_RUNNING)
    	{
    		return;
    	}
    	
    	if (pauseGame)
    	{
    		Log.d("pencil", "stopBalanceTimer.stop called with pauseGame=true");
    	}
    	//Log.d("pencil", "stopBalanceTimer called with balanceStartTime="+balanceStartTime);
    	int timerState = BALANCE_TIMER_STATE_STOPPED;
    	
    	if (balanceStartTime > 0)
    	{
        	//get session duration in seconds
        	long sessionDuration = now - balanceStartTime;
        	//update the high score in local memory
        	updateHighScore(sessionDuration);
        	
        	balanceLastScore = PencilDisplayHelper.formatInterval(sessionDuration);
        	
        	Log.d("pencil", "stopBalanceTimer.stop: sessionDuration="+sessionDuration);
        	
    		if (pauseGame && sessionDuration > BALANCE_PAUSE_GAME_MINIMUM_SCORE)
    		{
    			Log.d("pencil", "stopBalanceTimer.stop: session duration is above min so going to delay game");
    			//balanceLastScore = PencilDisplayHelper.formatInterval(sessionDuration);
    			timerState = BALANCE_TIMER_STATE_DELAYED;
    		} else
    		{
    			Log.d("pencil", "stopBalanceTimer.stop: session duration is below min so not going to delay game");
    		}
    	} else
    	{
    		Log.d("pencil", "stopBalanceTimer.stop: balanceStartTime is less than 0");
    		balanceLastScore= "0.0";
    	}
    	
    	balanceStopTime = now;
    	balanceStartTime = -1;
    	setTimerState(timerState);
    	
    }
    
    /**
     * Check if the game should be paused
     * 
     * @param long now The current time in milliseconds
     */
    public boolean checkAndPauseGameIfNecessary(long now)
    {
    	if (state == BALANCE_TIMER_STATE_DELAYED
    			&& (
    					balanceStopTime < 0 //should never happen but have this check just in case
    					|| ((now - balanceStopTime) > BALANCE_PAUSE_GAME_DELAY_PERIOD) 
    				)
    	)
    	{
        	//set game state
    		this.ownerThread.setGameState(PencilThread.STATE_PAUSED);
    		return true;
    	}
    	return false;
    }

    /**
     * Update the high score for this gravity if it is suitably impressive
     * 
     * @param long sessionDuration How long the balance session lasted
     */
    public void updateHighScore(long sessionDuration)
	{
		//Log.d("pencil", "called updateHighScore");
    	String key = "highscore_grav_"+PencilView.gravityFactor;
    	Long highScore = PencilView.highScores.get(key);
    	if (highScore == null || (sessionDuration > highScore))
    	{
    		//Log.d("pencil", "checkAndUpdateHighScore: going to update high score to "+sessionDuration);
    		PencilView.highScores.put(key, sessionDuration);
    	}
	}
   
    /**
     * Update the high score shared preferences which are stored on the device permanently
     */
    public void updateHighScoreSharedPreferences()
    {
    	Log.d("pencil", "called updateHighScoreSharedPreferences");
    	SharedPreferences settings = context.getSharedPreferences(PencilView.HIGH_SCORE_SHARED_PREFS_NAME, PencilActivity.MODE_PRIVATE);
    	for (String key : PencilView.highScores.keySet())
    	{
        	if (PencilView.highScores.get(key) != null)
        	{
        		long oldHighScore = settings.getLong(key, 0L);
        		long newHighScore = PencilView.highScores.get(key);
        		if (newHighScore > oldHighScore)
        		{
        			Log.d("pencil", "updateHighScoreSharedPreferences: going to update value for "+key);
        			Log.d("pencil", "updateHighScoreSharedPreferences: old value: "+oldHighScore+", newHighScore="+newHighScore);
        			SharedPreferences.Editor editor = settings.edit();
                    editor.putLong(key, newHighScore);
                    editor.commit();
        		} else
        		{
        			Log.d("pencil", "updateHighScoreSharedPreferences: not going to update value for "+key+" because old score is higher than new score");
        			Log.d("pencil", "updateHighScoreSharedPreferences: old value: "+oldHighScore+", newHighScore="+newHighScore);
        		}
        	}
    	}

    }
   
}