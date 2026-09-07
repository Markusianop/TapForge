package com.tapforge;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Local reusable NDEF templates. No network/account required. */
public final class NdefProfileStore {
    private static final String PREF = "tapforge_profile_library";
    private static final String KEY = "profiles";
    private static final int LIMIT = 80;

    public static final class Item {
        public String id;
        public String name;
        public long updatedAt;
        public byte[] ndef;
        public int records;
        public int bytes;
    }

    private NdefProfileStore() { }

    public static synchronized Item save(Context c, String name, NdefMessage message) throws Exception {
        if (message == null) throw new IllegalArgumentException("message");
        byte[] raw = message.toByteArray();
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        JSONArray old = new JSONArray(p.getString(KEY, "[]"));
        JSONArray next = new JSONArray();
        JSONObject o = new JSONObject();
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        o.put("id", id);
        o.put("name", (name == null || name.trim().isEmpty()) ? "NDEF profile" : name.trim());
        o.put("updated", now);
        o.put("ndef", Base64.encodeToString(raw, Base64.NO_WRAP));
        next.put(o);
        for (int i = 0; i < old.length() && next.length() < LIMIT; i++) next.put(old.getJSONObject(i));
        p.edit().putString(KEY, next.toString()).apply();
        Item it = new Item(); it.id=id; it.name=o.getString("name"); it.updatedAt=now; it.ndef=raw; it.records=message.getRecords().length; it.bytes=raw.length; return it;
    }

    public static synchronized void delete(Context c, String id) {
        try {
            SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(p.getString(KEY, "[]"));
            JSONArray next = new JSONArray();
            for (int i=0;i<old.length();i++) {
                JSONObject o=old.getJSONObject(i);
                if (!o.optString("id").equals(id)) next.put(o);
            }
            p.edit().putString(KEY,next.toString()).apply();
        } catch(Exception ignored) { }
    }

    public static List<Item> list(Context c) {
        ArrayList<Item> out=new ArrayList<>();
        try {
            JSONArray a=new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));
            for(int i=0;i<a.length();i++) {
                JSONObject o=a.getJSONObject(i);
                byte[] raw=Base64.decode(o.optString("ndef"),Base64.DEFAULT);
                NdefMessage m=new NdefMessage(raw);
                Item it=new Item(); it.id=o.optString("id"); it.name=o.optString("name","NDEF profile"); it.updatedAt=o.optLong("updated"); it.ndef=raw; it.records=m.getRecords().length; it.bytes=raw.length; out.add(it);
            }
        } catch(Exception ignored) { }
        return out;
    }

    public static NdefMessage message(Item it) throws Exception { return new NdefMessage(it.ndef); }

    public static String exportJson(Context c) {
        try {
            JSONArray a=new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));
            JSONObject root=new JSONObject(); root.put("format","tapforge-profile-library"); root.put("version",1); root.put("profiles",a); return root.toString(2);
        } catch(Exception e) { return "{\"format\":\"tapforge-profile-library\",\"version\":1,\"profiles\":[]}"; }
    }

    public static int importJson(Context c, String json) throws Exception {
        JSONObject root=new JSONObject(json);
        if (!"tapforge-profile-library".equals(root.optString("format"))) throw new IllegalArgumentException("format");
        JSONArray incoming=root.optJSONArray("profiles"); if(incoming==null) return 0;
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        JSONArray current=new JSONArray(p.getString(KEY,"[]"));
        JSONArray next=new JSONArray(); int added=0;
        for(int i=0;i<incoming.length() && next.length()<LIMIT;i++) {
            JSONObject o=incoming.getJSONObject(i);
            byte[] raw=Base64.decode(o.optString("ndef"),Base64.DEFAULT);
            new NdefMessage(raw); // validate
            JSONObject copy=new JSONObject(); copy.put("id",UUID.randomUUID().toString()); copy.put("name",o.optString("name","Imported profile")); copy.put("updated",System.currentTimeMillis()); copy.put("ndef",Base64.encodeToString(raw,Base64.NO_WRAP)); next.put(copy); added++;
        }
        for(int i=0;i<current.length() && next.length()<LIMIT;i++) next.put(current.getJSONObject(i));
        p.edit().putString(KEY,next.toString()).apply();
        return added;
    }
}
