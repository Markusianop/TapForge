package com.tapforge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public class NfcHistoryActivity extends Activity {
    private LinearLayout list;
    private TextView empty;

    @Override protected void attachBaseContext(Context newBase) { super.attachBaseContext(LocaleHelper.wrap(newBase)); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        UiAdaptation.apply(this, findViewById(R.id.historyRoot));
        UiAdaptation.applyAdaptiveWidth(this, findViewById(R.id.historyContent));
        list = findViewById(R.id.historyList);
        empty = findViewById(R.id.historyEmpty);
        findViewById(R.id.historyBack).setOnClickListener(v -> Navigation.pop(this));
        findViewById(R.id.clearHistory).setOnClickListener(v -> confirmClear());
        Motion.press(findViewById(R.id.historyBack));
        Motion.press(findViewById(R.id.clearHistory));
        render();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear, (d, w) -> { NfcHistory.clear(this); render(); })
                .show();
    }

    private void render() {
        list.removeAllViews();
        JSONArray a = NfcHistory.get(this);
        empty.setVisibility(a.length() == 0 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < a.length(); i++) {
            try {
                JSONObject o = a.getJSONObject(i);
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(18), dp(16), dp(18), dp(16));
                card.setBackgroundResource(R.drawable.bg_card);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                cp.topMargin = i == 0 ? 0 : dp(10);
                card.setLayoutParams(cp);

                TextView title = tv(o.optString("title"), 16, true);
                TextView meta = tv(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(o.optLong("time"))), 12, false);
                TextView detail = tv(o.optString("detail"), 13, false);
                int secondary = ThemeHelper.isDark(this) ? 0xffa5a3aa : 0xff6f6d76;
                meta.setTextColor(secondary);
                detail.setTextColor(secondary);
                LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2); mp.topMargin = dp(5); meta.setLayoutParams(mp);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2); dlp.topMargin = dp(8); detail.setLayoutParams(dlp);
                card.addView(title); card.addView(meta); if (!o.optString("detail").isEmpty()) card.addView(detail);
                list.addView(card);
                Motion.press(card);
            } catch (Exception ignored) { }
        }
    }

    private TextView tv(String s, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(ThemeHelper.isDark(this) ? 0xfff7f7f8 : 0xff151518);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    @Override public void onBackPressed() { Navigation.pop(this); }
}
