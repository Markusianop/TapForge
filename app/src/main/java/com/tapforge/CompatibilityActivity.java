package com.tapforge;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ViewGroup;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

public class CompatibilityActivity extends Activity implements NfcAdapter.ReaderCallback {
    private NfcAdapter adapter; private NdefMessage intended; private final AtomicBoolean busy=new AtomicBoolean(false); private View resultCard,halo,outer,glyph; private TextView title,subtitle,result;
    @Override protected void attachBaseContext(Context c){super.attachBaseContext(LocaleHelper.wrap(c));}
    @Override protected void onCreate(Bundle b){ThemeHelper.apply(this);super.onCreate(b);setContentView(R.layout.activity_compatibility);UiAdaptation.apply(this,findViewById(R.id.compatRoot));UiAdaptation.applyAdaptiveWidth(this,findViewById(R.id.compatContent));adapter=NfcAdapter.getDefaultAdapter(this);try{intended=TapForgeProfiles.activeNdef(this);}catch(Exception ignored){}resultCard=findViewById(R.id.compatResultCard);halo=findViewById(R.id.compatHalo);outer=findViewById(R.id.compatHaloOuter);glyph=findViewById(R.id.compatGlyph);title=findViewById(R.id.compatTitle);subtitle=findViewById(R.id.compatSubtitle);result=findViewById(R.id.compatResult);findViewById(R.id.compatBack).setOnClickListener(v->Navigation.pop(this));findViewById(R.id.compatAgain).setOnClickListener(v->reset());Motion.press(findViewById(R.id.compatBack));Motion.press(findViewById(R.id.compatAgain));tune();startMotion();}
    @Override protected void onResume(){super.onResume();enable();}@Override protected void onPause(){disable();super.onPause();}
    private void enable(){if(adapter==null||!adapter.isEnabled())return;Bundle o=new Bundle();o.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY,180);int f=NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_NFC_F|NfcAdapter.FLAG_READER_NFC_V|NfcAdapter.FLAG_READER_NFC_BARCODE;try{adapter.enableReaderMode(this,this,f,o);}catch(Exception ignored){}}
    private void disable(){try{if(adapter!=null)adapter.disableReaderMode(this);}catch(Exception ignored){}}
    @Override public void onTagDiscovered(Tag tag){if(!busy.compareAndSet(false,true))return;try{String s=NfcAnalysis.compatibility(tag,intended);NfcHistory.add(this,"CHECK",getString(R.string.compatibility_check),TagInspector.uid(tag));runOnUiThread(()->{disable();stopMotion();title.setText(R.string.compatibility_result);subtitle.setText(intended==null?R.string.compatibility_no_profile:R.string.compatibility_profile_checked);result.setText(s);resultCard.setVisibility(View.VISIBLE);Motion.enter(this,resultCard);Motion.success(glyph);});}finally{busy.set(false);}}
    private void reset(){resultCard.setVisibility(View.GONE);title.setText(R.string.check_tag_ready);subtitle.setText(R.string.check_tag_subtitle);startMotion();enable();}
    private void startMotion(){Motion.receiveGlyph(glyph,true);Motion.receiveHalo(halo,true,false);Motion.receiveHalo(outer,true,true);}private void stopMotion(){Motion.receiveGlyph(glyph,false);Motion.receiveHalo(halo,false,false);Motion.receiveHalo(outer,false,true);}private void tune(){Configuration c=getResources().getConfiguration();int w=Math.max(1,c.screenWidthDp),h=Math.max(1,c.screenHeightDp);boolean l=c.orientation==Configuration.ORIENTATION_LANDSCAPE;int size=Math.min((int)(w*(l?.38f:.72f)),(int)(h*(l?.46f:.33f)));size=Math.max(l?150:205,Math.min(l?220:290,size));square(findViewById(R.id.compatStage),size);square(outer,Math.round(size*.75f));square(halo,Math.round(size*.61f));square(glyph,Math.round(size*.40f));}
    private void square(View v,int d){ViewGroup.LayoutParams lp=v.getLayoutParams();lp.width=dp(d);lp.height=dp(d);v.setLayoutParams(lp);} private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onBackPressed(){Navigation.pop(this);}
}
