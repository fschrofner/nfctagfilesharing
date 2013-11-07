package at.fhhgb.mc.nfctagfilesharing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxLink;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

public class DropBoxThread extends Thread {
	
	String filePath;
	DropboxAPI<AndroidAuthSession> mDBApi;
	Context context;
	String fileName;
	Handler handler;
	
	DropBoxThread(String filePath, String fileName, DropboxAPI<AndroidAuthSession> mDBApi, Context context, Handler handler){
		this.filePath = filePath;
		this.mDBApi = mDBApi;
		this.context = context;
		this.fileName = fileName;
		this.handler = handler;		//handler of the nfcwriter activity
	}
	
	@Override
	public void run() {
		super.run();
		File file = new File(filePath);			//opens the file selected in the main activity
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			handler.post(new Runnable(){

				@Override
				public void run() {
					((NfcWriterActivity)context).findViewById(R.id.progressBar1).setVisibility(View.VISIBLE);
					((ImageView)((NfcWriterActivity)context).findViewById(R.id.writeIcon)).setImageDrawable(context.getResources().getDrawable(R.drawable.upload_icon));
					((TextView)((NfcWriterActivity)context).findViewById(R.id.DropboxText)).setText("your file is uploading..");				
				}
				
			});
			mDBApi.putFile("/" + fileName, inputStream,
		        file.length(), null, null);
			DropboxLink dblink = mDBApi.share("/" + fileName);			//generates the link for the uploaded file
//			Log.i("Dropbox Thread", dblink.url);
			URL url = new URL(dblink.url);								//the url will be called here to unshorten the link
			HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
			ucon.setInstanceFollowRedirects(false);
			URL secondUrl = new URL(ucon.getHeaderField("Location"));   //unshortens the dropbox url (db.tt/..)
			Intent intent = new Intent(context, DropBoxActivity.class);
			intent.putExtra("link", secondUrl.toString());
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			context.startActivity(intent);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (DropboxException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
