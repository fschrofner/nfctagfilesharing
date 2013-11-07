package at.fhhgb.mc.nfctagfilesharing;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

import android.app.Application;

public class AppObject extends Application {
	
	private DropboxAPI<AndroidAuthSession> mDBApi;

	public DropboxAPI<AndroidAuthSession> getmDBApi() {
		return mDBApi;
	}

	public void setmDBApi(DropboxAPI<AndroidAuthSession> mDBApi) {
		this.mDBApi = mDBApi;
	}

}
