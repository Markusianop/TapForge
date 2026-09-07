package com.tapforge;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApduLabActivity extends Activity implements NfcAdapter.ReaderCallback {
    private NfcAdapter adapter; private EditText input; private TextView status,result; private View resultCard; private boolean armed; private final AtomicBoolean busy=new AtomicBoolean(false); private List<byte[]> commands=new ArrayList<>();
    @Override protected void attachBaseContext(Context c){super.attachBaseContext(LocaleHelper.wrap(c));}
    @Override protected void onCreate(Bundle b){ThemeHelper.apply(this);super.onCreate(b);setContentView(R.layout.activity_apdu_lab);UiAdaptation.apply(this,findViewById(R.id.apduRoot));UiAdaptation.applyAdaptiveWidth(this,findViewById(R.id.apduContent));adapter=NfcAdapter.getDefaultAdapter(this);input=findViewById(R.id.apduInput);status=findViewById(R.id.apduStatus);result=findViewById(R.id.apduResult);resultCard=findViewById(R.id.apduResultCard);findViewById(R.id.apduBack).setOnClickListener(v->Navigation.pop(this));findViewById(R.id.apduArm).setOnClickListener(v->arm());Motion.press(findViewById(R.id.apduBack));Motion.press(findViewById(R.id.apduArm));}
    private void arm(){try{commands=parse(input.getText().toString());if(commands.isEmpty())throw new IllegalArgumentException();}catch(Exception e){Toast.makeText(this,R.string.invalid_apdu,Toast.LENGTH_LONG).show();Motion.error(findViewById(R.id.apduArm));return;}if(adapter==null||!adapter.isEnabled()){Toast.makeText(this,R.string.status_off,Toast.LENGTH_LONG).show();return;}armed=true;status.setText(R.string.apdu_waiting);result.setText("");resultCard.setVisibility(View.VISIBLE);Motion.enter(this,resultCard);enable();}
    private List<byte[]> parse(String s){ArrayList<byte[]> out=new ArrayList<>();for(String line:s.split("\\r?\\n")){String x=line.trim();if(x.isEmpty()||x.startsWith("#"))continue;out.add(NdefFactory.hexToBytes(x));}return out;}
    private void enable(){Bundle o=new Bundle();o.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY,160);int f=NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;try{adapter.enableReaderMode(this,this,f,o);}catch(Exception ignored){}}
    private void disable(){try{if(adapter!=null)adapter.disableReaderMode(this);}catch(Exception ignored){}}
    @Override public void onTagDiscovered(Tag tag){if(!armed||!busy.compareAndSet(false,true))return;IsoDep iso=IsoDep.get(tag);if(iso==null){runOnUiThread(()->{status.setText(R.string.apdu_not_isodep);Motion.error(findViewById(R.id.apduArm));});busy.set(false);return;}StringBuilder log=new StringBuilder();try{iso.connect();iso.setTimeout(3500);for(byte[] cmd:commands){byte[] r=iso.transceive(cmd);log.append("→ ").append(NdefFactory.bytesToHex(cmd)).append("\n← ").append(NdefFactory.bytesToHex(r)).append("\n\n");}armed=false;disable();NfcHistory.add(this,"APDU",getString(R.string.apdu_complete),TagInspector.uid(tag)+" · "+commands.size()+" cmd");runOnUiThread(()->{status.setText(R.string.apdu_complete);result.setText(log.toString().trim());Motion.success(findViewById(R.id.apduArm));});}catch(Exception e){runOnUiThread(()->{status.setText(R.string.apdu_failed);result.setText((e.getMessage()==null?e.getClass().getSimpleName():e.getMessage())+"\n\n"+log);Motion.error(findViewById(R.id.apduArm));});}finally{try{iso.close();}catch(Exception ignored){}busy.set(false);}}
    @Override protected void onPause(){disable();super.onPause();}@Override protected void onResume(){super.onResume();if(armed)enable();}@Override public void onBackPressed(){Navigation.pop(this);}
}
