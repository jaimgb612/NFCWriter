package com.example.androidnfcwriterdemo;

import java.io.IOException;
import java.nio.charset.Charset;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WriterDemoActivity extends Activity{

	private Button write_button;
	private boolean writeProtect = false;
	private NfcAdapter mNfcAdapter;
    private IntentFilter[] mWriteTagFilters;
	private PendingIntent mNfcPendingIntent;
	private boolean ready_to_write = false;
	private Tag detectedTag;
	private Context mContext;
	private Activity mActivity;
	private EditText nfcdata;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.writer);
		write_button = (Button)findViewById(R.id.writeButton);
		write_button.setOnClickListener(writelistener);
		nfcdata = (EditText) findViewById(R.id.cardData);
		
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);
        IntentFilter discovery=new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[] { discovery };
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		if(mNfcAdapter != null) 
		{
			mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
		} 
		else 
		{
			Toast.makeText(getApplicationContext(), "Sorry, No NFC Adapter found.", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(mNfcAdapter != null) mNfcAdapter.disableForegroundDispatch(this);
	}
	
	
	public void writeDataToCard()
	{
		if(ready_to_write)
		{
			ready_to_write = false;
			WriteResponse wr = writeTag(getTagAsNdef(), detectedTag);
           	//WriteResponse wr = writeTag(createText("nilkash"), detectedTag);
           	String message = (wr.getStatus() == 1? "Success: " : "Failed: ") + wr.getMessage();
           	Toast.makeText(mContext,message,Toast.LENGTH_SHORT).show();
	    }
       else
	    {
	    	Toast.makeText(mContext,"Device not in contact",Toast.LENGTH_SHORT).show();
	    }
	}
	private OnClickListener writelistener = new OnClickListener() 
    {
		@SuppressWarnings("deprecation")
		@Override
		public void onClick(View v) 
		{
			if(ready_to_write)
			{
				ready_to_write = false;
				WriteResponse wr = writeTag(getTagAsNdef(), detectedTag);
	           	//WriteResponse wr = writeTag(createText("nilkash"), detectedTag);
	           	String message = (wr.getStatus() == 1? "Success: " : "Failed: ") + wr.getMessage();
	           	Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
		    }
	       else
		    {
		    	Toast.makeText(getApplicationContext(),"Device not in contact",Toast.LENGTH_SHORT).show();
		    }	
		}
	};
	
	private WriteResponse writeTag(NdefMessage message, Tag tag) 
    {
        int size = message.toByteArray().length;
        String mess = "";

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    return new WriteResponse(0,"Tag is read-only");

                }
                if (ndef.getMaxSize() < size) {
                    mess = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
                            + " bytes.";
                    return new WriteResponse(0,mess);
                }

                ndef.writeNdefMessage(message);
                if(writeProtect)  ndef.makeReadOnly();
                mess = "Wrote message to pre-formatted tag.";
                return new WriteResponse(1,mess);
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        mess = "Formatted tag and wrote message";
                        return new WriteResponse(1,mess);
                    } catch (IOException e) {
                        mess = "Failed to format tag.";
                        return new WriteResponse(0,mess);
                    }
                } else {
                    mess = "Tag doesn't support NDEF.";
                    return new WriteResponse(0,mess);
                }
            }
        } catch (Exception e) {
            mess = "Failed to write tag";
            return new WriteResponse(0,mess);
        }
    }
	
	 private class WriteResponse 
	 {
    	int status;
    	String message;
    	WriteResponse(int Status, String Message) {
    		this.status = Status;
    		this.message = Message;
    	}
    	public int getStatus() {
    		return status;
    	}
    	public String getMessage() {
    		return message;
    	}
    }
	private NdefMessage getTagAsNdef() 
	{
       
	   boolean addAAR = true;
       String data = nfcdata.getText().toString();
   	   byte[] uriField = data.getBytes(Charset.forName("US-ASCII"));
       byte[] payload = new byte[uriField.length + 1];              //add 1 for the URI Prefix
       payload[0] = 0x01;                                      	//prefixes http://www. to the URI
           
           System.arraycopy(uriField, 0, payload, 1, uriField.length);  //appends URI to payload
           NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
           
           if(addAAR) 
           {
              return new NdefMessage(new NdefRecord[] {
            		  rtdUriRecord, NdefRecord.createApplicationRecord("com.example.androidnfcreaderdemo")
           }); 
           }
           else 
           {
        	   return new NdefMessage(new NdefRecord[] {rtdUriRecord});
           }
     }
	
	@Override
	protected void onNewIntent(Intent intent) 
	{
		super.onNewIntent(intent);
		Log.i("*******************************", "inside new intent writer1");
		if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) 
		{
        	// validate that this tag can be written....
            detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if(supportedTechs(detectedTag.getTechList())) {
	            // check if tag is writable (to the extent that we can
	            if(writableTag(detectedTag)) {
	            	ready_to_write = true;
	            } else {
	            	Toast.makeText(getApplicationContext(),"This tag is not writable",Toast.LENGTH_SHORT).show();
	            }	            
            } else {
            	Toast.makeText(getApplicationContext(),"This tag type is not supported",Toast.LENGTH_SHORT).show();
            }
        }
    
	}
	
	
	private static boolean supportedTechs(String[] techs)
	{
	    boolean ultralight=false;
	    boolean nfcA=false;
	    boolean ndef=false;
	    for(String tech:techs) {
	    	if(tech.equals("android.nfc.tech.MifareUltralight")) {
	    		ultralight=true;
	    	}else if(tech.equals("android.nfc.tech.NfcA")) { 
	    		nfcA=true;
	    	} else if(tech.equals("android.nfc.tech.Ndef") || tech.equals("android.nfc.tech.NdefFormatable")) {
	    		ndef=true;
	    	}
	    }
        if(ultralight && nfcA && ndef) {
        	return true;
        } else {
        	return false;
        }
	}
	
	private boolean writableTag(Tag tag) {

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(mContext,"Tag is read-only.",Toast.LENGTH_SHORT).show();
                    //Sounds.PlayFailed(context, silent);
                    ndef.close(); 
                    return false;
                }
                ndef.close();
                return true;
            } 
        } catch (Exception e) {
            Toast.makeText(mContext,"Failed to read tag",Toast.LENGTH_SHORT).show();
            //Sounds.PlayFailed(context, silent);
        }

        return false;
    }

}
