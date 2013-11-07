package at.fhhgb.mc.nfctagfilesharing;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class MainActivity extends Activity implements OnClickListener{

	final int REQUESTCODE_GET_FILE = 1;
	String fileName;
	SharedPreferences pref;
	
	//constants used for the dropbox api
	final static private String APP_KEY = "1bi36u1lx0ncffz";
	final static private String APP_SECRET = "wukr92qyzn6qnmc";
	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	private DropboxAPI<AndroidAuthSession> mDBApi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ImageView i = (ImageView)findViewById(R.id.mainButton);
		i.setOnClickListener(this);
		i = (ImageView)findViewById(R.id.about);
		i.setOnClickListener(this);
		
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);				//used for the dropbox authentication
		
		AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		
		if (pref.getString("dropbox_key", null) == null || pref.getString("dropbox_secret", null) == null) {
			mDBApi.getSession().startAuthentication(this);
		} else {
			//if the app is already authenticated and the tokenpair is saved inside the shared preferences
			AccessTokenPair tp = new AccessTokenPair(pref.getString("dropbox_key", null), pref.getString("dropbox_secret", null));
			mDBApi.getSession().setAccessTokenPair(tp);
		}
		
		AppObject appObject = (AppObject) getApplication();
		appObject.setmDBApi(mDBApi);			//used to handover the DropboxAPI object to the writer activity
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (mDBApi.getSession().authenticationSuccessful()) {
	        try {
	            // Required to complete auth, sets the access token on the session
	            mDBApi.getSession().finishAuthentication();            
	            AccessTokenPair tokens = mDBApi.getSession().getAccessTokenPair();
	            Editor editor = pref.edit();
	            editor.putString("dropbox_key", tokens.key);
	            editor.putString("dropbox_secret", tokens.secret);
	            editor.commit();
	        } catch (IllegalStateException e) {
	            Log.i("DbAuthLog", "Error authenticating", e);
	        }
	    }
	}

	@Override
	public void onClick(View arg0) {
		switch(arg0.getId()){
		//starts the installed filemanager to pick a file
		case R.id.mainButton : {
			Intent intent = new Intent();           
			intent.setAction(android.content.Intent.ACTION_GET_CONTENT);
			intent.setType("file/*");
			startActivityForResult(intent, REQUESTCODE_GET_FILE);
		}
		break;
		case R.id.about : {
			Intent intent = new Intent(this, AboutActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(intent);
		}
		break;
		}
	}

	@Override
	//if the file(which should be written) was picked
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUESTCODE_GET_FILE && resultCode == RESULT_OK){
			fileName = data.getData().getPath();
			Intent intent = new Intent(this, NfcWriterActivity.class);
			intent.putExtra("fileName", fileName);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(intent);
		}
	}

}
