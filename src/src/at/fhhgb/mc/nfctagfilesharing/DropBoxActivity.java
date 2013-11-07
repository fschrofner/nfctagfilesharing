package at.fhhgb.mc.nfctagfilesharing;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DropBoxActivity extends Activity implements OnClickListener{
	
	String fileName;
	NfcAdapter adapter;
	boolean inWriteMode;			//identifies if the app is able to write
	String link;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_drop_box);
		
		Intent intent = getIntent();
		link = intent.getStringExtra("link");				//the dropbox link to write onto the tag
		
//		fileName = getIntent().getStringExtra("fileName");	
		adapter = NfcAdapter.getDefaultAdapter(this);
		
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
		adapter.enableForegroundDispatch(this, pi, filters, null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		enableWriteMode();
	}

	@Override
	protected void onPause() {
		super.onPause();
		adapter.disableForegroundDispatch(this);		//so other activities can read the nfc tag
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (inWriteMode) {
			inWriteMode = false;
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			writeTag(tag);
			((TextView)findViewById(R.id.DropboxText1)).setText(getResources().getString(R.string.TagWritten));
			((ImageView)findViewById(R.id.dropbox)).setImageDrawable(getResources().getDrawable(R.drawable.check_icon));
			((ImageView)findViewById(R.id.dropbox)).setOnClickListener(this);
			findViewById(R.id.DropboxText2).setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Writes the given tag object on a nfc tag.
	 * @param tag the tag you want to write
	 * @return true if successful
	 */
	private boolean writeTag(Tag tag) {
		try {
			StringBuffer textbuffer = new StringBuffer(link);
			textbuffer.replace(7, 19, "dl.dropboxusercontent");		//changes the dropbox link to a direct download link
			link = textbuffer.toString();
			NdefRecord cardRecord = NdefRecord.createUri(link);
			NdefMessage message = new NdefMessage(
					new NdefRecord[] { cardRecord });

			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();

				//read-only
				if (!ndef.isWritable()) {
					Toast.makeText(this, getResources().getString(R.string.ReadOnly), Toast.LENGTH_SHORT).show();
					return false;
				}

				// works out how much space will be needed for the data
				int size = message.toByteArray().length;
				if (ndef.getMaxSize() < size) {
					Toast.makeText(this, getResources().getString(R.string.NotEnoughSpace), Toast.LENGTH_SHORT).show();
					return false;
				}

				ndef.writeNdefMessage(message);
				//tag written successfully
				return true;
			} else {
				// attempt to format tag
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					try {
						format.connect();
						format.format(message);
						//tag written successfully
						return true;
					} catch (IOException e) {
						Toast.makeText(this, getResources().getString(R.string.NotFormatable), Toast.LENGTH_SHORT).show();
						return false;
					} catch (FormatException e) {
						// TODO Auto-generated catch block
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

	@Override
	public void onClick(View v) {
		this.finish();
	}

}
