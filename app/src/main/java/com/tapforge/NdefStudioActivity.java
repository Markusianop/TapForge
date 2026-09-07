package com.tapforge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class NdefStudioActivity extends Activity {
    private final List<NdefRecord> records=new ArrayList<>();
    private String type=NdefFactory.URL;
    private TextView typeText, recordsTitle, recordsText;
    private EditText value, extra, profileName;

    @Override protected void attachBaseContext(Context newBase){ super.attachBaseContext(LocaleHelper.wrap(newBase)); }
    @Override protected void onCreate(Bundle b){
        ThemeHelper.apply(this); super.onCreate(b); setContentView(R.layout.activity_ndef_studio);
        UiAdaptation.apply(this,findViewById(R.id.studioRoot)); UiAdaptation.applyAdaptiveWidth(this,findViewById(R.id.studioContent));
        typeText=findViewById(R.id.studioTypeText); recordsTitle=findViewById(R.id.studioRecordsTitle); recordsText=findViewById(R.id.studioRecords);
        value=findViewById(R.id.studioValue); extra=findViewById(R.id.studioExtra); profileName=findViewById(R.id.studioProfileName);
        findViewById(R.id.studioBack).setOnClickListener(v->Navigation.pop(this));
        findViewById(R.id.studioTypeRow).setOnClickListener(this::showTypeMenu);
        findViewById(R.id.studioAdd).setOnClickListener(v->addRecord()); findViewById(R.id.studioAddActive).setOnClickListener(v->addActive()); findViewById(R.id.studioAddLastScan).setOnClickListener(v->addLastScan()); findViewById(R.id.studioClear).setOnClickListener(v->{records.clear();render();});
        findViewById(R.id.studioSave).setOnClickListener(v->save()); findViewById(R.id.studioWrite).setOnClickListener(v->launchWrite(true)); findViewById(R.id.studioBatch).setOnClickListener(v->launchWrite(false));
        for(int id:new int[]{R.id.studioBack,R.id.studioTypeRow,R.id.studioAdd,R.id.studioAddActive,R.id.studioAddLastScan,R.id.studioClear,R.id.studioSave,R.id.studioWrite,R.id.studioBatch}) Motion.press(findViewById(id));
        setType(NdefFactory.URL); render(); Motion.enter(this,findViewById(R.id.studioHeader),findViewById(R.id.studioTypeRow),findViewById(R.id.studioRecordsCard));
    }
    private void showTypeMenu(View a){
        PopupMenu m=new PopupMenu(this,a); add(m,1,R.string.type_url); add(m,2,R.string.type_text); add(m,3,R.string.type_email); add(m,4,R.string.type_phone); add(m,5,R.string.type_sms); add(m,6,R.string.type_location); add(m,7,R.string.type_wifi); add(m,8,R.string.type_app); add(m,9,R.string.type_custom_mime); add(m,10,R.string.type_external); add(m,11,R.string.type_raw_ndef);
        m.setOnMenuItemClickListener(i->{String t; switch(i.getItemId()){case 1:t=NdefFactory.URL;break;case 2:t=NdefFactory.TEXT;break;case 3:t=NdefFactory.EMAIL;break;case 4:t=NdefFactory.PHONE;break;case 5:t=NdefFactory.SMS;break;case 6:t=NdefFactory.LOCATION;break;case 7:t=NdefFactory.WIFI;break;case 8:t=NdefFactory.APP;break;case 9:t=NdefFactory.MIME;break;case 10:t=NdefFactory.EXTERNAL;break;default:t=NdefFactory.RAW;} setType(t); return true;}); m.show();
    }
    private void add(PopupMenu m,int id,int s){m.getMenu().add(0,id,id,getString(s));}
    private void setType(String t){
        type=t; extra.setVisibility(View.GONE); value.setInputType(InputType.TYPE_CLASS_TEXT); extra.setInputType(InputType.TYPE_CLASS_TEXT);
        if(NdefFactory.URL.equals(t)){typeText.setText(R.string.type_url);value.setHint(R.string.hint_url);} else if(NdefFactory.TEXT.equals(t)){typeText.setText(R.string.type_text);value.setHint(R.string.hint_text);value.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);} else if(NdefFactory.EMAIL.equals(t)){typeText.setText(R.string.type_email);value.setHint(R.string.hint_email);} else if(NdefFactory.PHONE.equals(t)){typeText.setText(R.string.type_phone);value.setHint(R.string.hint_phone);} else if(NdefFactory.SMS.equals(t)){typeText.setText(R.string.type_sms);value.setHint(R.string.hint_phone);extra.setHint(R.string.sms_body);extra.setVisibility(View.VISIBLE);} else if(NdefFactory.LOCATION.equals(t)){typeText.setText(R.string.type_location);value.setHint(R.string.hint_location);} else if(NdefFactory.WIFI.equals(t)){typeText.setText(R.string.type_wifi);value.setHint(R.string.hint_wifi_ssid);extra.setHint(R.string.hint_wifi_password);extra.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);extra.setVisibility(View.VISIBLE);} else if(NdefFactory.APP.equals(t)){typeText.setText(R.string.type_app);value.setHint(R.string.hint_package);} else if(NdefFactory.MIME.equals(t)){typeText.setText(R.string.type_custom_mime);value.setHint(R.string.hint_payload);extra.setHint(R.string.hint_mime);extra.setVisibility(View.VISIBLE);} else if(NdefFactory.EXTERNAL.equals(t)){typeText.setText(R.string.type_external);value.setHint(R.string.hint_payload);extra.setHint(R.string.hint_external);extra.setVisibility(View.VISIBLE);} else {typeText.setText(R.string.type_raw_ndef);value.setHint(R.string.hint_raw_ndef);} Motion.flash(typeText);
    }
    private void addRecord(){
        try{
            NdefMessage m=NdefFactory.build(type,value.getText().toString(),extra.getText().toString(),"","","","","","","");
            for(NdefRecord r:m.getRecords()) records.add(r); value.setText(""); if(extra.getVisibility()==View.VISIBLE && !NdefFactory.MIME.equals(type)&&!NdefFactory.EXTERNAL.equals(type)) extra.setText(""); render(); Motion.success(findViewById(R.id.studioAdd));
        }catch(Exception e){Toast.makeText(this,R.string.invalid_ndef_content,Toast.LENGTH_LONG).show();Motion.error(findViewById(R.id.studioAdd));}
    }
    private void addActive(){
        try { NdefMessage m=TapForgeProfiles.activeNdef(this); if(m==null){Toast.makeText(this,R.string.no_ndef_profile,Toast.LENGTH_SHORT).show();return;} for(NdefRecord r:m.getRecords())records.add(r);render();Motion.success(findViewById(R.id.studioAddActive)); }
        catch(Exception e){Toast.makeText(this,R.string.invalid_ndef_content,Toast.LENGTH_SHORT).show();}
    }
    private void addLastScan(){
        try { String b64=getSharedPreferences("tapforge_clone",MODE_PRIVATE).getString("ndef",null); if(b64==null){Toast.makeText(this,R.string.no_last_scan,Toast.LENGTH_SHORT).show();return;} NdefMessage m=new NdefMessage(Base64.decode(b64,Base64.DEFAULT));for(NdefRecord r:m.getRecords())records.add(r);render();Motion.success(findViewById(R.id.studioAddLastScan)); }
        catch(Exception e){Toast.makeText(this,R.string.no_last_scan,Toast.LENGTH_SHORT).show();}
    }

    private NdefMessage message(){ if(records.isEmpty()) return null; return new NdefMessage(records.toArray(new NdefRecord[0])); }
    private void render(){
        NdefMessage m=message(); if(m==null){recordsTitle.setText(R.string.records_empty);recordsText.setText(R.string.records_empty_detail);return;}
        recordsTitle.setText(getString(R.string.records_count,records.size(),m.toByteArray().length)); recordsText.setText(TagInspector.describeMessage(m));
    }
    private void save(){
        try{NdefMessage m=message(); if(m==null){Toast.makeText(this,R.string.add_record_first,Toast.LENGTH_SHORT).show();return;} NdefProfileStore.save(this,profileName.getText().toString(),m);Toast.makeText(this,R.string.profile_saved,Toast.LENGTH_SHORT).show();Motion.success(findViewById(R.id.studioSave));}
        catch(Exception e){Toast.makeText(this,R.string.save_failed,Toast.LENGTH_SHORT).show();}
    }
    private void launchWrite(boolean single){
        NdefMessage m=message(); if(m==null){Toast.makeText(this,R.string.add_record_first,Toast.LENGTH_SHORT).show();return;} Intent i=new Intent(this,BatchWriteActivity.class); i.putExtra(BatchWriteActivity.EXTRA_NDEF_B64,Base64.encodeToString(m.toByteArray(),Base64.NO_WRAP)); i.putExtra(BatchWriteActivity.EXTRA_SINGLE,single); Navigation.push(this,i);
    }
    @Override public void onBackPressed(){Navigation.pop(this);}
}
