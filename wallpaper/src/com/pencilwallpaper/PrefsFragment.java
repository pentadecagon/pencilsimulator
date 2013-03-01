package com.pencilwallpaper;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

public class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	 @Override
	 public void onCreate(Bundle savedInstanceState) {

	  super.onCreate(savedInstanceState);
	  
	  	getPreferenceManager().setSharedPreferencesName(
              PencilWallpaper.SETTINGS_SHARED_PREFS_NAME);
	  	// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.pencil_settings);
	 }
	 
	 @Override
	 public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	 {
		 //IT NEVER GETS IN HERE!
		 if (key.equals("pref_gravity"))
		 {
			 // Set summary to be the user-description for the selected value
			 String gravityFactorString = sharedPreferences.getString(key, "");
			 float gravityFactor = Float.parseFloat(gravityFactorString);
			 if (gravityFactor > 0 && gravityFactor <= 1)
			 {
				PencilWallpaper.gravityFactor = gravityFactor;
			 }
			 Log.d("pencil", "PrefsFragment: gravityFactor changed to "+gravityFactor);
		 }
	}
	 
	 @Override
	 public void onResume() {
	     super.onResume();
	     getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

	 }

	 @Override
	 public void onPause() {
	     getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	     super.onPause();
	 }

}