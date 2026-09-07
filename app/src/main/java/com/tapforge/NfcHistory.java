package com.tapforge;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public final class NfcHistory {
    private static final String PREF = "tapforge_nfc_history";
    private static final String KEY = "entries";
    private static final int LIMIT = 40;

    private NfcHistory() { }

    public static synchronized void add(Context context, String action, String title, String detail) {
        try {
            SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(p.getString(KEY, "[]"));
            JSONArray next = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("time", System.currentTimeMillis());
            item.put("action", action == null ? "" : action);
            item.put("title", title == null ? "" : title);
            item.put("detail", detail == null ? "" : detail);
            next.put(item);
            for (int i = 0; i < old.length() && next.length() < LIMIT; i++) next.put(old.getJSONObject(i));
            p.edit().putString(KEY, next.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static JSONArray get(Context context) {
        try {
            return new JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }
}
