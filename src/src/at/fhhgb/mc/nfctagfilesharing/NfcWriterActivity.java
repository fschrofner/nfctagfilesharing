package at.fhhgb.mc.nfctagfilesharing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class NfcWriterActivity extends Activity implements DialogInterface.OnClickListener, OnClickListener {
	String filePath;
	String fileName;
	NfcAdapter adapter;
	boolean inWriteMode;
	Handler handler = new Handler();
	FileInputStream fileInput;
	
	private DropboxAPI<AndroidAuthSession> mDBApi;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfc_writer);
		
		filePath = getIntent().getStringExtra("fileName");		//gets the selected file
		adapter = NfcAdapter.getDefaultAdapter(this);
		findViewById(R.id.progressBar1).setVisibility(View.INVISIBLE);
		((TextView)findViewById(R.id.DropboxText)).setText(getResources().getString(R.string.TagWrite));
		((ImageView)findViewById(R.id.writeIcon)).setImageDrawable(getResources().getDrawable(R.drawable.tag_icon));
		
		AppObject appObject = (AppObject) getApplication();
		mDBApi = appObject.getmDBApi();

	}

	/**
	 * Enables the write mode and initialises a pending intent calling this activity,
	 * if a new tag is discovered.
	 */
	private void enableWriteMode() {
		inWriteMode = true;
		Intent intent = new Intent(this, getClass());
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
		IntentFilter tagDetected = new IntentFilter(
				NfcAdapter.ACTION_TAG_DISCOVERED);
		IntentFilter[] filters = new IntentFilter[] { tagDetected };
		adapter.enableForegroundDispatch(this, pi, filters, null);		//this activity will be the first one called, when a new tag is discovered
	}

	@Override
	protected void onResume() {
		super.onResume();
		enableWriteMode();
	}

	@Override
	protected void onPause() {
		super.onPause();
		adapter.disableForegroundDispatch(this);		//so other apps can read tags
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (inWriteMode) {
			inWriteMode = false;
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			writeTag(tag);
		}
	}

	/**
	 * Writes the given tag object on a nfc tag.
	 * @param tag the tag you want to write
	 * @return true if successful
	 */
	private boolean writeTag(Tag tag) {
		try {
			StringBuffer buffer = new StringBuffer(filePath);
			int index = buffer.lastIndexOf("/", buffer.length()-1);
			buffer.delete(0, index+1);
			fileName = buffer.toString();
			
			fileInput = new FileInputStream(filePath);
			byte[] payload = new byte[(int) new File(filePath).length()];
			fileInput.read(payload);
			fileInput.close();
			String application = "application/at.fhhgb.mc.nfctagfilesharing";		//so this application will be opened when a new app is discovered
			byte[] mimeBytes = application
					.getBytes(Charset.forName("US-ASCII"));
			NdefRecord cardFile = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,			//this saves the data
					mimeBytes, new byte[0], payload);
			NdefRecord cardName = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,mimeBytes, new byte[0], fileName.getBytes());		//writes the file name on the tag so it can be saved with the correct name
			NdefMessage message = new NdefMessage(
					new NdefRecord[] { cardFile, cardName });
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();

				if (!ndef.isWritable()) {
					Toast.makeText(this, getResources().getString(R.string.ReadOnly), Toast.LENGTH_SHORT).show();
					return false;
				}

				// work out how much space we need for the data
				int size = message.toByteArray().length;
				if (ndef.getMaxSize() < size) {
					AlertDialog.Builder dialog = new AlertDialog.Builder(this);
					dialog.setTitle("File too big!");
					dialog.setMessage("Upload the file to dropbox and write the link instead?");
					dialog.setPositiveButton("Upload",this);	
					dialog.setNegativeButton("Cancel", this);
					dialog.show();
					return false;
				}

				ndef.writeNdefMessage(message);
				((ImageView)findViewById(R.id.writeIcon)).setImageDrawable(getResources().getDrawable(R.drawable.check_icon));
				((ImageView)findViewById(R.id.writeIcon)).setOnClickListener(this);
				((TextView)findViewById(R.id.DropboxText)).setText(getResources().getString(R.string.TagWritten));
				return true;
			} else {
				// attempt to format tag
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					try {
						format.connect();
						format.format(message);
						return true;
					} catch (IOException e) {
						Toast.makeText(this, getResources().getString(R.string.NotFormatable), Toast.LENGTH_SHORT).show();
						return false;
					} catch (FormatException e) {
						e.printStackTrace();
					}
				} else {
					Toast.makeText(this, getResources().getString(R.string.NotSupported), Toast.LENGTH_SHORT).show();
					return false;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Starts a thread to upload the file to dropbox.
	 * @param fileInput the inputstream of the file
	 * @param filePath the name of the file
	 */
	private void saveToDropbox(FileInputStream fileInput, String filePath) {
		Thread thread = new DropBoxThread(filePath,fileName, mDBApi, this, handler);
		thread.start();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(which == DialogInterface.BUTTON_POSITIVE) saveToDropbox(fileInput, filePath);
		dialog.dismiss();
	}

	@Override
	public void onClick(View v) {
		this.finish();
	}

}
