package at.fhhgb.mc.nfctagfilesharing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

public class NfcReaderActivity extends Activity implements OnClickListener{

	String fileName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfc_reader);
		
		Intent intent = getIntent();
		String application = "application/at.fhhgb.mc.nfctagfilesharing";		//will search for this string inside the intent	
		
		if(intent.getType() != null && intent.getType().equals(application)){
			Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			NdefMessage msg = (NdefMessage)rawMsgs[0];
			NdefRecord cardFile = msg.getRecords()[0];							//gets the file
			NdefRecord cardName = msg.getRecords()[1];							//gets the file name
			fileName = new String(cardName.getPayload());
			byte[] file = cardFile.getPayload();
			try {
				File outputDir = new File(Environment.getExternalStorageDirectory() + getResources().getString(R.string.path) + "/");
				outputDir.mkdirs();
				File outputFile = new File( outputDir, new String(cardName.getPayload()));
				FileOutputStream fileOutput = new FileOutputStream(outputFile);
				fileOutput.write(file);			//writes the data from the tag onto the phone's storage
				fileOutput.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			((ImageView)findViewById(R.id.saveIcon)).setOnClickListener(this);
			((TextView)findViewById(R.id.saved2)).setText(getResources().getString(R.string.path) + "/" + fileName);
		}
	}

	@Override
	public void onClick(View v) {
	    MimeTypeMap map = MimeTypeMap.getSingleton();
	    //gets the applications available to open this filetype
	    String ext = MimeTypeMap.getFileExtensionFromUrl(fileName);
	    String type = map.getMimeTypeFromExtension(ext);	    
	    Uri data = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + getResources().getString(R.string.path) + "/" + fileName));
	    
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(data, type);
		//shows the dialog to choose the application with which you want to open the file
		startActivity(Intent.createChooser(intent, "Open with:"));		
	}
}
