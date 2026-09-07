package com.tapforge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.LinearLayout;

public class NfcToolsActivity extends Activity {
    private boolean closing = false;
    private OnBackInvokedCallback backCallback;
    @Override
    protected void attachBaseContext(Context newBase) { super.attachBaseContext(LocaleHelper.wrap(newBase)); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_tools);
        UiAdaptation.apply(this, findViewById(R.id.toolsRoot));
        UiAdaptation.applyAdaptiveWidth(this, findViewById(R.id.toolsContent));

        findViewById(R.id.toolsBack).setOnClickListener(v -> finishWithMotion());
        bind(R.id.readTagCard, v -> openScan(TagScanActivity.MODE_READ));
        bind(R.id.writeTagCard, v -> Navigation.push(this, new Intent(this, TagWriteActivity.class)));
        bind(R.id.cloneTagCard, v -> openScan(TagScanActivity.MODE_CLONE));
        bind(R.id.studioCard, v -> Navigation.push(this, new Intent(this, NdefStudioActivity.class)));
        bind(R.id.batchCard, v -> Navigation.push(this, new Intent(this, BatchWriteActivity.class)));
        bind(R.id.libraryCard, v -> Navigation.push(this, new Intent(this, ProfileLibraryActivity.class)));
        bind(R.id.compareCard, v -> Navigation.push(this, new Intent(this, CompareTagsActivity.class)));
        bind(R.id.compatCard, v -> Navigation.push(this, new Intent(this, CompatibilityActivity.class)));
        bind(R.id.apduCard, v -> Navigation.push(this, new Intent(this, ApduLabActivity.class)));
        bind(R.id.undoWriteCard, v -> openScan(TagScanActivity.MODE_RESTORE));
        bind(R.id.eraseTagCard, v -> openScan(TagScanActivity.MODE_ERASE));
        bind(R.id.formatTagCard, v -> openScan(TagScanActivity.MODE_FORMAT));
        bind(R.id.lockTagCard, v -> confirmLock());
        bind(R.id.historyCard, v -> Navigation.push(this, new Intent(this, NfcHistoryActivity.class)));

        Motion.press(findViewById(R.id.toolsBack));
        Motion.enter(this, findViewById(R.id.toolsHeader), findViewById(R.id.toolsHero),
                findViewById(R.id.readTagCard), findViewById(R.id.writeTagCard), findViewById(R.id.cloneTagCard),
                findViewById(R.id.studioCard), findViewById(R.id.batchCard), findViewById(R.id.libraryCard),
                findViewById(R.id.compareCard), findViewById(R.id.compatCard), findViewById(R.id.apduCard), findViewById(R.id.undoWriteCard), findViewById(R.id.eraseTagCard), findViewById(R.id.formatTagCard), findViewById(R.id.lockTagCard),
                findViewById(R.id.historyCard));

        // Android 13+ can route the system back gesture outside onBackPressed().
        // Register it explicitly so the toolbar arrow, button and gesture all close Tools identically.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backCallback = this::finishWithMotion;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
    }

    private void bind(int id, View.OnClickListener listener) {
        View v = findViewById(id);
        v.setOnClickListener(listener);
        Motion.press(v);
    }

    private void openScan(String mode) {
        Intent i = new Intent(this, TagScanActivity.class);
        i.putExtra(TagScanActivity.EXTRA_MODE, mode);
        Navigation.push(this, i);
    }

    private void confirmLock() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.lock_tag)
                .setMessage(R.string.lock_irreversible)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.continue_action, (d, w) -> openScan(TagScanActivity.MODE_LOCK))
                .show();
    }

    private void finishWithMotion() {
        if (closing) return;
        closing = true;
        finish();
        if (!Motion.reduced(this)) {
            overridePendingTransition(R.anim.tf_tools_pop_enter, R.anim.tf_tools_pop_exit);
        } else {
            overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        super.onDestroy();
    }

    @Override public void onBackPressed() { finishWithMotion(); }
}
