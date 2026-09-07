package com.tapforge;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ViewGroup;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompareTagsActivity extends Activity implements NfcAdapter.ReaderCallback {
    private NfcAdapter adapter; private Snapshot first; private final AtomicBoolean busy=new AtomicBoolean(false); private View halo,outer,glyph,resultCard; private TextView title,subtitle,resultTitle,result;
    static class Snapshot{String uid,tech;int capacity;boolean writable;NdefMessage msg;}
    @Override protected void attachBaseContext(Context c){super.attachBaseContext(LocaleHelper.wrap(c));}
    @Override protected void onCreate(Bundle b){ThemeHelper.apply(this);super.onCreate(b);setContentView(R.layout.activity_compare_tags);UiAdaptation.apply(this,findViewById(R.id.compareRoot));UiAdaptation.applyAdaptiveWidth(this,findViewById(R.id.compareContent));adapter=NfcAdapter.getDefaultAdapter(this);halo=findViewById(R.id.compareHalo);outer=findViewById(R.id.compareHaloOuter);glyph=findViewById(R.id.compareGlyph);resultCard=findViewById(R.id.compareResultCard);title=findViewById(R.id.compareTitle);subtitle=findViewById(R.id.compareSubtitle);resultTitle=findViewById(R.id.compareResultTitle);result=findViewById(R.id.compareResult);findViewById(R.id.compareBack).setOnClickListener(v->Navigation.pop(this));findViewById(R.id.compareAgain).setOnClickListener(v->reset());Motion.press(findViewById(R.id.compareBack));Motion.press(findViewById(R.id.compareAgain));tune();startMotion();}
    @Override protected void onResume(){super.onResume();enable();}@Override protected void onPause(){disable();super.onPause();}
    private void enable(){if(adapter==null||!adapter.isEnabled())return;Bundle o=new Bundle();o.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY,180);int f=NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_NFC_F|NfcAdapter.FLAG_READER_NFC_V|NfcAdapter.FLAG_READER_NFC_BARCODE;try{adapter.enableReaderMode(this,this,f,o);}catch(Exception ignored){}}
    private void disable(){try{if(adapter!=null)adapter.disableReaderMode(this);}catch(Exception ignored){}}
    @Override public void onTagDiscovered(Tag tag){if(!busy.compareAndSet(false,true))return;try{Snapshot s=snap(tag);if(first==null){first=s;runOnUiThread(()->{title.setText(R.string.scan_second_tag);subtitle.setText(R.string.scan_second_tag_subtitle);Motion.success(glyph);});}else{Snapshot a=first,b=s;String text=diff(a,b);NfcHistory.add(this,"COMPARE",getString(R.string.compare_complete),a.uid+" ↔ "+b.uid);runOnUiThread(()->{disable();stopMotion();title.setText(R.string.compare_complete);subtitle.setText(R.string.compare_complete_subtitle);resultTitle.setText(a.uid+"  ↔  "+b.uid);result.setText(text);resultCard.setVisibility(View.VISIBLE);Motion.enter(this,resultCard);Motion.success(glyph);});}}catch(Exception e){runOnUiThread(()->{title.setText(R.string.read_failed);subtitle.setText(e.getMessage()==null?getString(R.string.read_failed):e.getMessage());Motion.error(glyph);});}finally{busy.set(false);}}
    private Snapshot snap(Tag tag)throws Exception{Snapshot s=new Snapshot();s.uid=TagInspector.uid(tag);s.tech=TagInspector.techs(tag);Ndef n=Ndef.get(tag);if(n!=null){s.capacity=n.getMaxSize();s.writable=n.isWritable();try{n.connect();s.msg=n.getNdefMessage();}finally{try{n.close();}catch(Exception ignored){}}}return s;}
    private String diff(Snapshot a,Snapshot b){StringBuilder x=new StringBuilder();x.append("UID  ").append(eq(a.uid,b.uid)?"same":"different").append('\n');x.append("Tech  ").append(eq(a.tech,b.tech)?"same":"different").append("\n  A: ").append(a.tech).append("\n  B: ").append(b.tech).append('\n');x.append("Capacity  ").append(a.capacity).append(" B ↔ ").append(b.capacity).append(" B\n");x.append("Writable  ").append(a.writable).append(" ↔ ").append(b.writable).append('\n');boolean same=NfcAnalysis.sameMessage(a.msg,b.msg);x.append("NDEF  ").append(same?"identical":"different").append('\n');x.append("A fingerprint  ").append(NfcAnalysis.shortFingerprint(a.msg)).append('\n');x.append("B fingerprint  ").append(NfcAnalysis.shortFingerprint(b.msg));if(a.msg!=null&&b.msg!=null)x.append("\nA bytes  ").append(a.msg.toByteArray().length).append("\nB bytes  ").append(b.msg.toByteArray().length);return x.toString();}
    private boolean eq(String a,String b){return a==null?b==null:a.equals(b);}private void reset(){first=null;resultCard.setVisibility(View.GONE);title.setText(R.string.scan_first_tag);subtitle.setText(R.string.scan_first_tag_subtitle);startMotion();enable();}
    private void startMotion(){Motion.receiveGlyph(glyph,true);Motion.receiveHalo(halo,true,false);Motion.receiveHalo(outer,true,true);}private void stopMotion(){Motion.receiveGlyph(glyph,false);Motion.receiveHalo(halo,false,false);Motion.receiveHalo(outer,false,true);}private void tune(){Configuration c=getResources().getConfiguration();int w=Math.max(1,c.screenWidthDp),h=Math.max(1,c.screenHeightDp);boolean l=c.orientation==Configuration.ORIENTATION_LANDSCAPE;int size=Math.min((int)(w*(l?.38f:.72f)),(int)(h*(l?.46f:.33f)));size=Math.max(l?150:205,Math.min(l?220:290,size));square(findViewById(R.id.compareStage),size);square(outer,Math.round(size*.75f));square(halo,Math.round(size*.61f));square(glyph,Math.round(size*.40f));}
    private void square(View v,int d){ViewGroup.LayoutParams lp=v.getLayoutParams();lp.width=dp(d);lp.height=dp(d);v.setLayoutParams(lp);} private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onBackPressed(){Navigation.pop(this);}
}
