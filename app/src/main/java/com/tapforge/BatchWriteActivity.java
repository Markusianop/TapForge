package com.tapforge;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchWriteActivity extends Activity implements NfcAdapter.ReaderCallback {
    public static final String EXTRA_NDEF_B64="ndef_b64";
    public static final String EXTRA_SINGLE="single";
    private NfcAdapter adapter; private NdefMessage message; private boolean running; private boolean single;
    private final AtomicBoolean busy=new AtomicBoolean(false); private final Set<String> seen=new HashSet<>();
    private int written,verified,failed,skipped; private volatile boolean verifyEnabled=true, duplicateGuard=true;
    private TextView title,subtitle,counter,meta; private CheckBox verify,duplicate; private Button toggle; private View halo,haloOuter,glyph;
    @Override protected void attachBaseContext(Context c){super.attachBaseContext(LocaleHelper.wrap(c));}
    @Override protected void onCreate(Bundle b){
        ThemeHelper.apply(this);super.onCreate(b);setContentView(R.layout.activity_batch_write);UiAdaptation.apply(this,findViewById(R.id.batchRoot));UiAdaptation.applyAdaptiveWidth(this,findViewById(R.id.batchContent));
        adapter=NfcAdapter.getDefaultAdapter(this); single=getIntent().getBooleanExtra(EXTRA_SINGLE,false);
        try{String raw=getIntent().getStringExtra(EXTRA_NDEF_B64); message=raw==null?TapForgeProfiles.activeNdef(this):new NdefMessage(Base64.decode(raw,Base64.DEFAULT));}catch(Exception ignored){}
        title=findViewById(R.id.batchTitle);subtitle=findViewById(R.id.batchSubtitle);counter=findViewById(R.id.batchCounter);meta=findViewById(R.id.batchMeta);verify=findViewById(R.id.batchVerify);duplicate=findViewById(R.id.batchDuplicateGuard);toggle=findViewById(R.id.batchToggle);halo=findViewById(R.id.batchHalo);haloOuter=findViewById(R.id.batchHaloOuter);glyph=findViewById(R.id.batchGlyph);
        verify.setOnCheckedChangeListener((bttn,checked)->verifyEnabled=checked); duplicate.setOnCheckedChangeListener((bttn,checked)->duplicateGuard=checked);
        if(single){findViewById(R.id.batchHeaderTitle).setVisibility(View.VISIBLE);((TextView)findViewById(R.id.batchHeaderTitle)).setText(R.string.write_once);duplicate.setVisibility(View.GONE); duplicateGuard=false;}
        findViewById(R.id.batchBack).setOnClickListener(v->Navigation.pop(this));toggle.setOnClickListener(v->{if(running)stop();else start();});Motion.press(findViewById(R.id.batchBack));Motion.press(toggle);tune();update();
        if(message==null){title.setText(R.string.no_ndef_profile);subtitle.setText(R.string.choose_profile_first);toggle.setEnabled(false);} else {start();}
    }
    private void start(){if(adapter==null){Toast.makeText(this,R.string.nfc_unavailable,Toast.LENGTH_LONG).show();return;}if(!adapter.isEnabled()){Toast.makeText(this,R.string.status_off,Toast.LENGTH_LONG).show();return;}running=true;title.setText(R.string.ready_for_tags);subtitle.setText(single?R.string.write_once_subtitle:R.string.ready_for_tags_subtitle);toggle.setText(R.string.stop_batch);startMotion();enable();update();}
    private void stop(){running=false;disable();stopMotion();toggle.setText(R.string.start_batch);title.setText(R.string.batch_paused);subtitle.setText(R.string.batch_paused_subtitle);}
    private void enable(){Bundle o=new Bundle();o.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY,180);int f=NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_NFC_F|NfcAdapter.FLAG_READER_NFC_V|NfcAdapter.FLAG_READER_NFC_BARCODE;try{adapter.enableReaderMode(this,this,f,o);}catch(Exception ignored){}}
    private void disable(){try{if(adapter!=null)adapter.disableReaderMode(this);}catch(Exception ignored){}}
    @Override public void onTagDiscovered(Tag tag){
        if(!running||message==null||!busy.compareAndSet(false,true))return;String uid=TagInspector.uid(tag);
        try{
            if(!single&&duplicateGuard&&seen.contains(uid)){skipped++;runOnUiThread(()->{subtitle.setText(R.string.duplicate_skipped);Motion.tap(glyph);update();});return;}
            NdefUndoStore.capture(this, tag);
            TagScanActivity.writeMessage(tag,message,true);written++;seen.add(uid);boolean ok=true;
            if(verifyEnabled){try{Thread.sleep(70L);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}Ndef n=Ndef.get(tag);NdefMessage read=null;try{if(n!=null){n.connect();read=n.getNdefMessage();}}finally{try{if(n!=null)n.close();}catch(Exception ignored){}}ok=read!=null&&Arrays.equals(message.toByteArray(),read.toByteArray());if(ok)verified++;else failed++;}
            boolean finalOk=ok;NfcHistory.add(this,"BATCH",getString(finalOk?R.string.tag_written:R.string.verify_failed),uid);
            runOnUiThread(()->{title.setText(finalOk?R.string.tag_written:R.string.verify_failed);subtitle.setText(finalOk?(verifyEnabled?R.string.verified_ok:R.string.tag_written_subtitle):R.string.verify_failed_subtitle);if(finalOk)Motion.success(glyph);else Motion.error(glyph);update();if(single){running=false;disable();stopMotion();toggle.setText(R.string.done);toggle.setOnClickListener(v->Navigation.pop(this));}});
        }catch(Exception e){failed++;runOnUiThread(()->{title.setText(R.string.write_failed);subtitle.setText(e.getMessage()==null?getString(R.string.write_failed):e.getMessage());Motion.error(glyph);update();});}
        finally{busy.set(false);}
    }
    private void update(){int bytes=message==null?0:message.toByteArray().length;counter.setText(getString(R.string.batch_counter,written,verified,failed,skipped));meta.setText(getString(R.string.batch_payload_meta,bytes,message==null?0:message.getRecords().length));}
    private void startMotion(){Motion.receiveGlyph(glyph,true);Motion.receiveHalo(halo,true,false);Motion.receiveHalo(haloOuter,true,true);}private void stopMotion(){Motion.receiveGlyph(glyph,false);Motion.receiveHalo(halo,false,false);Motion.receiveHalo(haloOuter,false,true);}
    private void tune(){Configuration c=getResources().getConfiguration();int w=Math.max(1,c.screenWidthDp),h=Math.max(1,c.screenHeightDp);boolean l=c.orientation==Configuration.ORIENTATION_LANDSCAPE;int s=Math.min((int)(w*(l?.38f:.75f)),(int)(h*(l?.42f:.33f)));s=Math.max(l?150:210,Math.min(l?220:300,s));sq(findViewById(R.id.batchStage),s);sq(haloOuter,Math.round(s*.76f));sq(halo,Math.round(s*.61f));sq(glyph,Math.round(s*.42f));}
    private void sq(View v,int d){ViewGroup.LayoutParams lp=v.getLayoutParams();lp.width=dp(d);lp.height=dp(d);v.setLayoutParams(lp);}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onResume(){super.onResume();if(running)enable();}@Override protected void onPause(){disable();super.onPause();}@Override public void onBackPressed(){Navigation.pop(this);}
}
