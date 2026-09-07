package com.tapforge;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.content.Context;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

public class TagScanActivity extends Activity implements NfcAdapter.ReaderCallback {
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_READ = "read";
    public static final String MODE_CLONE = "clone";
    public static final String MODE_ERASE = "erase";
    public static final String MODE_FORMAT = "format";
    public static final String MODE_LOCK = "lock";
    public static final String MODE_RESTORE = "restore";

    private static final String CLONE_PREF = "tapforge_clone";
    private static final String CLONE_DATA = "ndef";

    private NfcAdapter adapter;
    private String mode;
    private boolean cloneWriting;
    private NdefMessage cloneMessage;
    private NdefMessage lastReadMessage;
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private TextView headerTitle, scanTitle, scanSubtitle, resultTitle, resultMeta, resultContent;
    private View resultScroll, halo, haloOuter, glyph;
    private Button primary, secondary, tertiary;

    @Override protected void attachBaseContext(Context newBase) { super.attachBaseContext(LocaleHelper.wrap(newBase)); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_scan);
        UiAdaptation.apply(this, findViewById(R.id.tagScanRoot));
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = MODE_READ;
        adapter = NfcAdapter.getDefaultAdapter(this);
        bindViews();
        configureCopy();
        tuneLayout();
        findViewById(R.id.tagScanBack).setOnClickListener(v -> Navigation.pop(this));
        primary.setOnClickListener(v -> resetScan());
        secondary.setOnClickListener(v -> {
            if (cloneMessage != null) {
                cloneWriting = true;
                resultScroll.setVisibility(View.GONE);
                scanTitle.setText(R.string.clone_destination);
                scanSubtitle.setText(R.string.clone_destination_subtitle);
                startMotion();
                enableReader();
            }
        });
        tertiary.setOnClickListener(v -> saveLastRead());
        Motion.press(findViewById(R.id.tagScanBack)); Motion.press(primary); Motion.press(secondary); Motion.press(tertiary);
        if (adapter == null) showFatal(R.string.nfc_unavailable, R.string.status_unavailable_detail);
    }

    private void bindViews() {
        headerTitle = findViewById(R.id.tagScanHeaderTitle);
        scanTitle = findViewById(R.id.tagScanTitle);
        scanSubtitle = findViewById(R.id.tagScanSubtitle);
        resultScroll = findViewById(R.id.tagResultScroll);
        resultTitle = findViewById(R.id.tagResultTitle);
        resultMeta = findViewById(R.id.tagResultMeta);
        resultContent = findViewById(R.id.tagResultContent);
        primary = findViewById(R.id.tagPrimaryAction);
        secondary = findViewById(R.id.tagSecondaryAction); tertiary = findViewById(R.id.tagTertiaryAction);
        halo = findViewById(R.id.tagHalo); haloOuter = findViewById(R.id.tagHaloOuter); glyph = findViewById(R.id.tagGlyph);
    }

    private void configureCopy() {
        if (MODE_READ.equals(mode)) {
            headerTitle.setText(R.string.read_tag); scanTitle.setText(R.string.ready_to_scan_tag); scanSubtitle.setText(R.string.ready_to_scan_tag_subtitle);
        } else if (MODE_CLONE.equals(mode)) {
            headerTitle.setText(R.string.clone_ndef); scanTitle.setText(R.string.clone_source); scanSubtitle.setText(R.string.clone_source_subtitle);
        } else if (MODE_ERASE.equals(mode)) {
            headerTitle.setText(R.string.erase_tag); scanTitle.setText(R.string.approach_tag); scanSubtitle.setText(R.string.erase_scan_subtitle);
        } else if (MODE_FORMAT.equals(mode)) {
            headerTitle.setText(R.string.format_tag); scanTitle.setText(R.string.approach_tag); scanSubtitle.setText(R.string.format_scan_subtitle);
        } else if (MODE_RESTORE.equals(mode)) {
            headerTitle.setText(R.string.undo_last_write); scanTitle.setText(R.string.undo_scan_title); scanSubtitle.setText(R.string.undo_scan_subtitle);
        } else {
            headerTitle.setText(R.string.lock_tag); scanTitle.setText(R.string.approach_tag); scanSubtitle.setText(R.string.lock_scan_subtitle);
        }
    }

    @Override protected void onResume() { super.onResume(); if (resultScroll.getVisibility() != View.VISIBLE || MODE_CLONE.equals(mode)) enableReader(); startMotion(); }
    @Override protected void onPause() { disableReader(); stopMotion(); super.onPause(); }

    private void enableReader() {
        if (adapter == null || !adapter.isEnabled()) {
            showFatal(R.string.status_off, R.string.status_off_detail); return;
        }
        Bundle opts = new Bundle(); opts.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 180);
        int flags = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B |
                NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V | NfcAdapter.FLAG_READER_NFC_BARCODE;
        try { adapter.enableReaderMode(this, this, flags, opts); } catch (Exception ignored) { }
    }
    private void disableReader() { try { if (adapter != null) adapter.disableReaderMode(this); } catch (Exception ignored) { } }

    @Override public void onTagDiscovered(Tag tag) {
        if (!busy.compareAndSet(false, true)) return;
        try {
            if (MODE_READ.equals(mode)) readTag(tag, false);
            else if (MODE_CLONE.equals(mode)) {
                if (!cloneWriting) readTag(tag, true); else writeClone(tag);
            } else if (MODE_ERASE.equals(mode)) erase(tag, false);
            else if (MODE_FORMAT.equals(mode)) erase(tag, true);
            else if (MODE_RESTORE.equals(mode)) restore(tag);
            else lock(tag);
        } finally { busy.set(false); }
    }

    private void readTag(Tag tag, boolean forClone) {
        Ndef ndef = Ndef.get(tag); NdefMessage msg = null;
        try {
            if (ndef != null) { ndef.connect(); msg = ndef.getNdefMessage(); }
        } catch (Exception e) { postError(getString(R.string.read_failed), e.getMessage()); return; }
        finally { close(ndef); }
        final NdefMessage fmsg = msg; final Ndef fndef = ndef;
        if (!forClone) lastReadMessage = msg;
        if (forClone) {
            if (msg == null) { postError(getString(R.string.no_ndef_message), getString(R.string.clone_requires_ndef)); return; }
            cloneMessage = msg;
            getSharedPreferences(CLONE_PREF, MODE_PRIVATE).edit().putString(CLONE_DATA, Base64.encodeToString(msg.toByteArray(), Base64.NO_WRAP)).apply();
            NfcHistory.add(this, "CLONE", getString(R.string.clone_source_saved), TagInspector.uid(tag));
            runOnUiThread(() -> {
                cloneWriting = true;
                scanTitle.setText(R.string.clone_destination);
                scanSubtitle.setText(R.string.clone_destination_subtitle);
                Motion.success(glyph);
            });
            return;
        }
        String meta = TagInspector.tagSummary(tag, fndef, fmsg);
        String content = TagInspector.describeMessage(fmsg);
        if (msg != null) getSharedPreferences(CLONE_PREF, MODE_PRIVATE).edit().putString(CLONE_DATA, Base64.encodeToString(msg.toByteArray(), Base64.NO_WRAP)).apply();
        NfcHistory.add(this, "READ", getString(R.string.tag_read), TagInspector.uid(tag));
        runOnUiThread(() -> showResult(getString(R.string.tag_read), meta, content, getString(R.string.scan_again),
                fmsg == null ? null : getString(R.string.clone_this_ndef), false));
    }

    private void writeClone(Tag tag) {
        if (cloneMessage == null) {
            try {
                String b64 = getSharedPreferences(CLONE_PREF, MODE_PRIVATE).getString(CLONE_DATA, null);
                if (b64 != null) cloneMessage = new NdefMessage(Base64.decode(b64, Base64.DEFAULT));
            } catch (Exception ignored) { }
        }
        if (cloneMessage == null) { postError(getString(R.string.clone_failed), getString(R.string.clone_requires_ndef)); return; }
        try {
            NdefUndoStore.capture(this, tag);
            writeMessage(tag, cloneMessage, true);
            NfcHistory.add(this, "CLONE", getString(R.string.clone_complete), TagInspector.uid(tag));
            runOnUiThread(() -> showResult(getString(R.string.clone_complete), getString(R.string.clone_complete_subtitle),
                    TagInspector.uid(tag), getString(R.string.clone_another), null, true));
        } catch (Exception e) { postError(getString(R.string.clone_failed), message(e)); }
    }

    private void erase(Tag tag, boolean formatMode) {
        try {
            NdefUndoStore.capture(this, tag);
            NdefMessage empty = NdefFactory.emptyMessage();
            if (formatMode) writeMessage(tag, empty, true);
            else {
                Ndef n = Ndef.get(tag);
                if (n == null) throw new IllegalStateException(getString(R.string.tag_not_ndef));
                try { n.connect(); if (!n.isWritable()) throw new IllegalStateException(getString(R.string.tag_read_only)); n.writeNdefMessage(empty); }
                finally { close(n); }
            }
            NfcHistory.add(this, formatMode ? "FORMAT" : "ERASE", formatMode ? getString(R.string.tag_formatted) : getString(R.string.tag_erased), TagInspector.uid(tag));
            runOnUiThread(() -> showResult(formatMode ? getString(R.string.tag_formatted) : getString(R.string.tag_erased),
                    TagInspector.uid(tag), getString(R.string.operation_complete), getString(R.string.scan_another), null, true));
        } catch (Exception e) { postError(formatMode ? getString(R.string.format_failed) : getString(R.string.erase_failed), message(e)); }
    }

    private void restore(Tag tag) {
        NdefUndoStore.Backup backup = NdefUndoStore.get(this);
        if (backup == null) { postError(getString(R.string.undo_unavailable), getString(R.string.undo_unavailable_subtitle)); return; }
        String uid = TagInspector.uid(tag);
        if (!backup.uid.equalsIgnoreCase(uid)) { postError(getString(R.string.wrong_tag), getString(R.string.wrong_tag_subtitle, backup.uid)); return; }
        try {
            writeMessage(tag, backup.message, true);
            NdefUndoStore.clear(this);
            NfcHistory.add(this, "UNDO", getString(R.string.undo_complete), uid);
            runOnUiThread(() -> showResult(getString(R.string.undo_complete), uid, getString(R.string.undo_complete_subtitle), getString(R.string.done), null, true));
        } catch (Exception e) { postError(getString(R.string.undo_failed), message(e)); }
    }

    private void lock(Tag tag) {
        Ndef n = Ndef.get(tag);
        if (n == null) { postError(getString(R.string.lock_failed), getString(R.string.tag_not_ndef)); return; }
        try {
            n.connect();
            if (!n.canMakeReadOnly()) throw new IllegalStateException(getString(R.string.lock_not_supported));
            boolean ok = n.makeReadOnly();
            if (!ok) throw new IllegalStateException(getString(R.string.lock_failed));
            NfcHistory.add(this, "LOCK", getString(R.string.tag_locked), TagInspector.uid(tag));
            runOnUiThread(() -> showResult(getString(R.string.tag_locked), TagInspector.uid(tag), getString(R.string.lock_done), getString(R.string.done), null, true));
        } catch (Exception e) { postError(getString(R.string.lock_failed), message(e)); }
        finally { close(n); }
    }

    public static void writeMessage(Tag tag, NdefMessage msg, boolean allowFormat) throws Exception {
        Ndef n = Ndef.get(tag);
        if (n != null) {
            try {
                n.connect();
                if (!n.isWritable()) throw new IllegalStateException("Tag is read-only");
                if (msg.toByteArray().length > n.getMaxSize()) throw new IllegalStateException("NDEF message is larger than tag capacity");
                n.writeNdefMessage(msg); return;
            } finally { close(n); }
        }
        if (allowFormat) {
            NdefFormatable f = NdefFormatable.get(tag);
            if (f != null) {
                try { f.connect(); f.format(msg); return; }
                finally { try { f.close(); } catch (Exception ignored) { } }
            }
        }
        throw new IllegalStateException("Tag is not NDEF writable");
    }

    private static void close(Ndef n) { try { if (n != null) n.close(); } catch (Exception ignored) { } }

    private void postError(String title, String detail) {
        runOnUiThread(() -> showResult(title, detail == null ? "" : detail, "", getString(R.string.try_again), null, false));
    }

    private String message(Exception e) { String s = e.getMessage(); return s == null || s.trim().isEmpty() ? e.getClass().getSimpleName() : s; }

    private void showResult(String title, String meta, String content, String primaryText, String secondaryText, boolean success) {
        disableReader(); stopMotion();
        resultTitle.setText(title); resultMeta.setText(meta); resultContent.setText(content);
        primary.setText(primaryText); primary.setVisibility(View.VISIBLE);
        if (secondaryText != null) { secondary.setText(secondaryText); secondary.setVisibility(View.VISIBLE); } else secondary.setVisibility(View.GONE);
        tertiary.setVisibility(MODE_READ.equals(mode) && lastReadMessage != null ? View.VISIBLE : View.GONE);
        resultScroll.setVisibility(View.VISIBLE);
        if (success) Motion.success(glyph); else Motion.tap(glyph);
        Motion.enter(this, findViewById(R.id.tagResultCard));
    }

    private void saveLastRead() {
        if (lastReadMessage == null) return;
        EditText name = new EditText(this); name.setHint(R.string.profile_name); name.setPadding(dp(20),dp(8),dp(20),dp(8));
        new AlertDialog.Builder(this).setTitle(R.string.save_to_library).setView(name).setNegativeButton(R.string.cancel,null)
                .setPositiveButton(R.string.save,(d,w)->{try{NdefProfileStore.save(this,name.getText().toString(),lastReadMessage);Toast.makeText(this,R.string.profile_saved,Toast.LENGTH_SHORT).show();Motion.success(tertiary);}catch(Exception e){Toast.makeText(this,R.string.save_failed,Toast.LENGTH_SHORT).show();}}).show();
    }

    private void resetScan() {
        resultScroll.setVisibility(View.GONE);
        if (MODE_CLONE.equals(mode)) { cloneWriting = false; cloneMessage = null; configureCopy(); }
        else configureCopy();
        busy.set(false); startMotion(); enableReader();
    }

    private void showFatal(int title, int subtitle) {
        scanTitle.setText(title); scanSubtitle.setText(subtitle); stopMotion();
    }
    private void startMotion() { Motion.receiveGlyph(glyph, true); Motion.receiveHalo(halo, true, false); Motion.receiveHalo(haloOuter, true, true); }
    private void stopMotion() { Motion.receiveGlyph(glyph, false); Motion.receiveHalo(halo, false, false); Motion.receiveHalo(haloOuter, false, true); }

    private void tuneLayout() {
        Configuration c = getResources().getConfiguration();
        int width = Math.max(1, c.screenWidthDp);
        int height = Math.max(1, c.screenHeightDp);
        boolean landscape = c.orientation == Configuration.ORIENTATION_LANDSCAPE;
        int scene = Math.min((int)(width * (landscape ? .40f : .84f)), (int)(height * (landscape ? .43f : .39f)));
        scene = Math.max(landscape ? 154 : 238, Math.min(landscape ? 220 : 330, scene));
        setSquare(findViewById(R.id.tagScanStage), scene);
        setSquare(haloOuter, Math.round(scene * .76f));
        setSquare(halo, Math.round(scene * .61f));
        setSquare(glyph, Math.round(scene * .42f));
        View copy = findViewById(R.id.tagScanCopy);
        ViewGroup.LayoutParams raw = copy.getLayoutParams();
        if (raw instanceof android.widget.RelativeLayout.LayoutParams) {
            android.widget.RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) raw;
            lp.topMargin = dp(landscape ? 16 : (height > 780 ? 30 : 24));
            copy.setLayoutParams(lp);
        }
    }

    private void setSquare(View v, int sizeDp) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = dp(sizeDp); lp.height = dp(sizeDp); v.setLayoutParams(lp);
    }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    @Override public void onBackPressed() { Navigation.pop(this); }
}
