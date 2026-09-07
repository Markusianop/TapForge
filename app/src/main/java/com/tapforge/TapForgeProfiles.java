package com.tapforge;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.util.Base64;

public final class TapForgeProfiles {
    private TapForgeProfiles() { }
    public static NdefMessage activeNdef(Context c) throws Exception {
        SharedPreferences p=c.getSharedPreferences(MainActivity.PREFS,Context.MODE_PRIVATE);
        if(!p.getBoolean(MainActivity.KEY_PROFILE_ENABLED,false)) return null;
        if(MainActivity.MODE_CUSTOM.equals(p.getString(MainActivity.KEY_MODE,MainActivity.MODE_NDEF))) return null;
        String t=p.getString(MainActivity.KEY_NDEF_TYPE,MainActivity.TYPE_URL);
        if (MainActivity.TYPE_RAW_NDEF.equals(t)) {
            String raw=p.getString(MainActivity.KEY_RAW_NDEF,null); return raw==null?null:new NdefMessage(Base64.decode(raw,Base64.DEFAULT));
        }
        String mapped=MainActivity.TYPE_TEXT.equals(t)?NdefFactory.TEXT:MainActivity.TYPE_EMAIL.equals(t)?NdefFactory.EMAIL:MainActivity.TYPE_PHONE.equals(t)?NdefFactory.PHONE:MainActivity.TYPE_MIME.equals(t)?NdefFactory.MIME:MainActivity.TYPE_VCARD.equals(t)?NdefFactory.CONTACT:NdefFactory.URL;
        return NdefFactory.build(mapped,p.getString(MainActivity.KEY_NDEF_VALUE,""),p.getString(MainActivity.KEY_MIME_TYPE,"text/plain"),
                p.getString(MainActivity.KEY_VCARD_NAME,""),p.getString(MainActivity.KEY_VCARD_PHONE,""),p.getString(MainActivity.KEY_VCARD_EMAIL,""),p.getString(MainActivity.KEY_VCARD_ORG,""),p.getString(MainActivity.KEY_VCARD_TITLE,""),p.getString(MainActivity.KEY_VCARD_ADDRESS,""),p.getString(MainActivity.KEY_VCARD_WEB,""));
    }
}
