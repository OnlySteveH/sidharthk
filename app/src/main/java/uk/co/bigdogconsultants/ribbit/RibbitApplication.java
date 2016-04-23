package uk.co.bigdogconsultants.ribbit;

import uk.co.bigdogconsultants.ribbit.ui.MainActivity;
import uk.co.bigdogconsultants.ribbit.utilities.ParseConstants;
import android.app.Application;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.PushService;

public class RibbitApplication extends Application {
	
	@Override
	public void onCreate() { 
		super.onCreate();
//	    Parse.initialize(this, "dbrOyEHgX2GR3EPTIblrNNAd4ITXarc82shcXdlu", "6iyrGf1I8pXKRGlxMuL4NQQ4doZLrR5btJ2ZlvII");
		Parse.initialize(this, "Kkh35Frn0LsIt2OD6aGMpF17XvGtxEphMG590WWV", "gwQgVmDdLgU6EQozOCfLzx9eW8nS43zXddSqqsLr");
		PushService.setDefaultPushCallback(this, MainActivity.class);
		ParseInstallation.getCurrentInstallation().saveInBackground();
	}
	public static void updateParseInstallation(ParseUser user){
		ParseInstallation installation = ParseInstallation.getCurrentInstallation();
		installation.put(ParseConstants.KEY_USER_ID, user.getObjectId());
		installation.saveInBackground();
	}
}
