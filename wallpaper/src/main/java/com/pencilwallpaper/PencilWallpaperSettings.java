package com.pencilwallpaper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

public class PencilWallpaperSettings extends Activity {

	@Override
	 protected void onCreate(Bundle savedInstanceState) {
	  // TODO Auto-generated method stub
	  super.onCreate(savedInstanceState);

	  getFragmentManager().beginTransaction().replace(android.R.id.content,
	                new PrefsFragment()).commit();
	 }

}
