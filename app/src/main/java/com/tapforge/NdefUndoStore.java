package com.tapforge;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Base64;

/** One-step rollback for the most recently overwritten readable NDEF message. */
public final class NdefUndoStore {
    private static final String PREF="tapforge_ndef_undo", K_UID="uid", K_DATA="data", K_TIME="time";
    public static final class Backup { public String uid; public NdefMessage message; public long time; }
    private NdefUndoStore(){ }
    public static void capture(Context c, Tag tag){
        Ndef n=Ndef.get(tag); if(n==null)return; try{n.connect();NdefMessage m=n.getNdefMessage();if(m==null)return;String uid=TagInspector.uid(tag);c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(K_UID,uid).putString(K_DATA,Base64.encodeToString(m.toByteArray(),Base64.NO_WRAP)).putLong(K_TIME,System.currentTimeMillis()).apply();}catch(Exception ignored){}finally{try{n.close();}catch(Exception ignored){}}
    }
    public static Backup get(Context c){try{SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);String uid=p.getString(K_UID,null),data=p.getString(K_DATA,null);if(uid==null||data==null)return null;Backup b=new Backup();b.uid=uid;b.time=p.getLong(K_TIME,0);b.message=new NdefMessage(Base64.decode(data,Base64.DEFAULT));return b;}catch(Exception e){return null;}}
    public static void clear(Context c){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().clear().apply();}
}
