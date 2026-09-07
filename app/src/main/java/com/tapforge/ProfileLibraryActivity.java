package com.tapforge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ComponentName;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class ProfileLibraryActivity extends Activity {
    private static final int REQ_IMPORT=7101,REQ_EXPORT=7102;
    private LinearLayout list; private TextView empty;
    @Override protected void attachBaseContext(Context c){super.attachBaseContext(LocaleHelper.wrap(c));}
    @Override protected void onCreate(Bundle b){ThemeHelper.apply(this);super.onCreate(b);setContentView(R.layout.activity_profile_library);UiAdaptation.apply(this,findViewById(R.id.libraryRoot));UiAdaptation.applyAdaptiveWidth(this,findViewById(R.id.libraryContent));list=findViewById(R.id.libraryList);empty=findViewById(R.id.libraryEmpty);findViewById(R.id.libraryBack).setOnClickListener(v->Navigation.pop(this));findViewById(R.id.librarySaveActive).setOnClickListener(v->saveActive());findViewById(R.id.libraryImport).setOnClickListener(v->importFile());findViewById(R.id.libraryExport).setOnClickListener(v->exportFile());for(int id:new int[]{R.id.libraryBack,R.id.librarySaveActive,R.id.libraryImport,R.id.libraryExport})Motion.press(findViewById(id));render();}
    private void saveActive(){try{NdefMessage m=TapForgeProfiles.activeNdef(this);if(m==null){Toast.makeText(this,R.string.no_ndef_profile,Toast.LENGTH_SHORT).show();return;}EditText e=new EditText(this);e.setHint(R.string.profile_name);e.setPadding(dp(20),dp(8),dp(20),dp(8));new AlertDialog.Builder(this).setTitle(R.string.save_active_profile).setView(e).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,(d,w)->{try{NdefProfileStore.save(this,e.getText().toString(),m);render();Toast.makeText(this,R.string.profile_saved,Toast.LENGTH_SHORT).show();}catch(Exception ex){Toast.makeText(this,R.string.save_failed,Toast.LENGTH_SHORT).show();}}).show();}catch(Exception ex){Toast.makeText(this,R.string.save_failed,Toast.LENGTH_SHORT).show();}}
    private void render(){list.removeAllViews();List<NdefProfileStore.Item> items=NdefProfileStore.list(this);empty.setVisibility(items.isEmpty()?View.VISIBLE:View.GONE);int i=0;for(NdefProfileStore.Item it:items){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(16),dp(18),dp(16));card.setBackgroundResource(R.drawable.bg_card);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=i++==0?0:dp(10);card.setLayoutParams(cp);TextView t=tv(it.name,17,true);TextView m=tv(getString(R.string.library_item_meta,it.records,it.bytes,NfcAnalysis.shortFingerprint(messageQuiet(it))),12,false);m.setTextColor(secondary());LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=dp(6);m.setLayoutParams(mp);card.addView(t);card.addView(m);card.setOnClickListener(v->options(it));Motion.press(card);list.addView(card);}}
    private NdefMessage messageQuiet(NdefProfileStore.Item it){try{return NdefProfileStore.message(it);}catch(Exception e){return null;}}
    private void options(NdefProfileStore.Item it){String[] a={getString(R.string.write_once),getString(R.string.batch_write),getString(R.string.activate_hce),getString(R.string.view_ndef),getString(R.string.delete)};new AlertDialog.Builder(this).setTitle(it.name).setItems(a,(d,which)->{if(which==0||which==1){Intent x=new Intent(this,BatchWriteActivity.class);x.putExtra(BatchWriteActivity.EXTRA_NDEF_B64,Base64.encodeToString(it.ndef,Base64.NO_WRAP));x.putExtra(BatchWriteActivity.EXTRA_SINGLE,which==0);Navigation.push(this,x);}else if(which==2){getSharedPreferences(MainActivity.PREFS,MODE_PRIVATE).edit().putString(MainActivity.KEY_MODE,MainActivity.MODE_NDEF).putString(MainActivity.KEY_NDEF_TYPE,MainActivity.TYPE_RAW_NDEF).putString(MainActivity.KEY_RAW_NDEF,Base64.encodeToString(it.ndef,Base64.NO_WRAP)).putBoolean(MainActivity.KEY_PROFILE_ENABLED,true).apply();preferNdef();Toast.makeText(this,R.string.hce_profile_activated,Toast.LENGTH_SHORT).show();Motion.success(findViewById(R.id.librarySaveActive));}else if(which==3){try{NdefMessage m=NdefProfileStore.message(it);new AlertDialog.Builder(this).setTitle(it.name).setMessage(TagInspector.describeMessage(m)+"\n\nSHA-256\n"+NfcAnalysis.sha256(it.ndef)).setPositiveButton(R.string.done,null).show();}catch(Exception ignored){}}else{NdefProfileStore.delete(this,it.id);render();}}).show();}
    private void preferNdef(){
        try { NfcAdapter a=NfcAdapter.getDefaultAdapter(this); if(a!=null) CardEmulation.getInstance(a).setPreferredService(this,new ComponentName(this,NdefHostApduService.class)); } catch(Exception ignored) { }
    }
    private void importFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_IMPORT);}
    private void exportFile(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"tapforge-library.json");startActivityForResult(i,REQ_EXPORT);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null||data.getData()==null)return;Uri u=data.getData();try{if(req==REQ_EXPORT){try(OutputStream o=getContentResolver().openOutputStream(u)){o.write(NdefProfileStore.exportJson(this).getBytes(StandardCharsets.UTF_8));}Toast.makeText(this,R.string.export_complete,Toast.LENGTH_SHORT).show();}else if(req==REQ_IMPORT){ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(u)){byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}int count=NdefProfileStore.importJson(this,out.toString("UTF-8"));render();Toast.makeText(this,getString(R.string.import_complete,count),Toast.LENGTH_SHORT).show();}}catch(Exception e){Toast.makeText(this,R.string.import_export_failed,Toast.LENGTH_LONG).show();}}
    private TextView tv(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(ThemeHelper.isDark(this)?0xfff7f7f8:0xff151518);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}private int secondary(){return ThemeHelper.isDark(this)?0xffa5a3aa:0xff6f6d76;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}@Override public void onBackPressed(){Navigation.pop(this);}
}
