package com.tapforge;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContentValues;
import android.content.res.Configuration;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;

public class ReceiveActivity extends Activity implements NfcAdapter.ReaderCallback {
    private NfcAdapter adapter;
    private LinearLayout resultCard, scanCopy;
    private ViewGroup scanCenter;
    private FrameLayout scanStage;
    private View scanHalo, scanHaloOuter;
    private ImageView scanGlyph;
    private TextView scanTitle, scanSubtitle, resultType, resultValue;
    private Button primaryAction, copyAction, shareAction, scanSettingsButton;
    private NdefRecord lastRecord;
    private String lastText = "";
    private String lastUri = "";
    private String lastVCard = "";

    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleHelper.wrap(base)); }

    @Override protected void onCreate(Bundle b) {
        ThemeHelper.apply(this);
        super.onCreate(b);
        setContentView(R.layout.activity_receive);
        UiAdaptation.apply(this, findViewById(R.id.receiveRoot));

        adapter = NfcAdapter.getDefaultAdapter(this);
        ImageButton back = findViewById(R.id.receiveBack);
        scanCenter = findViewById(R.id.scanCenter);
        scanStage = findViewById(R.id.scanStage);
        scanCopy = findViewById(R.id.scanCopy);
        scanHalo = findViewById(R.id.scanHalo);
        scanHaloOuter = findViewById(R.id.scanHaloOuter);
        scanGlyph = findViewById(R.id.scanGlyph);
        scanTitle = findViewById(R.id.scanTitle);
        scanSubtitle = findViewById(R.id.scanSubtitle);
        resultCard = findViewById(R.id.resultCard);
        resultType = findViewById(R.id.resultType);
        resultValue = findViewById(R.id.resultValue);
        primaryAction = findViewById(R.id.primaryAction);
        copyAction = findViewById(R.id.copyAction);
        shareAction = findViewById(R.id.shareAction);
        scanSettingsButton = findViewById(R.id.scanSettingsButton);
        back.setOnClickListener(v -> Navigation.pop(this));
        primaryAction.setOnClickListener(v -> doPrimaryAction());
        copyAction.setOnClickListener(v -> copyResult());
        shareAction.setOnClickListener(v -> shareResult());
        scanSettingsButton.setOnClickListener(v -> openNfcSettings());
        Motion.press(back); Motion.press(primaryAction); Motion.press(copyAction); Motion.press(shareAction); Motion.press(scanSettingsButton);
        tuneReceiveLayout();
        Motion.enter(this, findViewById(R.id.receiveHeader), scanStage, scanCopy);
        updateScanState();
    }

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null && adapter.isEnabled()) {
            Bundle extras = new Bundle();
            extras.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
            adapter.enableReaderMode(this, this,
                    NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B,
                    extras);
        }
        updateScanState();
    }

    @Override protected void onPause() {
        if (adapter != null) adapter.disableReaderMode(this);
        setReceiveMotion(false);
        super.onPause();
    }

    private void updateScanState() {
        if (adapter == null) {
            scanTitle.setText(R.string.receive_unavailable);
            scanSubtitle.setText(R.string.receive_unavailable_subtitle);
            scanSettingsButton.setVisibility(View.GONE);
        } else if (!adapter.isEnabled()) {
            scanTitle.setText(R.string.receive_nfc_off);
            scanSubtitle.setText(R.string.receive_nfc_off_subtitle);
            scanSettingsButton.setVisibility(View.VISIBLE);
        } else {
            scanTitle.setText(R.string.receive_ready);
            scanSubtitle.setText(R.string.receive_ready_subtitle);
            scanSettingsButton.setVisibility(View.GONE);
        }
        boolean scanning = adapter != null && adapter.isEnabled();
        setReceiveMotion(scanning);
    }

    @Override public void onBackPressed() { Navigation.pop(this); }

    @Override public void onTagDiscovered(Tag tag) {
        // First accept normal Android NDEF tags/readers. If the stack does not expose NDEF
        // (common with HCE peers), fall back to a direct NFC Forum Type 4 APDU read.
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage message = ndef.getNdefMessage();
                if (message != null && message.getRecords().length > 0) {
                    NdefRecord record = chooseRecord(message);
                    if (record != null) {
                        runOnUiThread(() -> renderRecord(record));
                        return;
                    }
                }
            } catch (Exception ignored) {
                // Continue with ISO-DEP fallback.
            } finally { try { ndef.close(); } catch (Exception ignored) {} }
        }

        IsoDep iso = IsoDep.get(tag);
        if (iso == null) { runOnUiThread(() -> showError(getString(R.string.receive_not_ndef))); return; }
        try {
            iso.connect();
            iso.setTimeout(3000);
            if (!ok(iso.transceive(selectAid("D2760000850101"))) && !ok(iso.transceive(selectAid("D2760000850100"))))
                throw new Exception("NDEF application unavailable");
            if (!ok(iso.transceive(hex("00A4000C02E104")))) throw new Exception("NDEF file unavailable");
            byte[] lenResp = iso.transceive(hex("00B0000002"));
            if (!ok(lenResp) || lenResp.length < 4) throw new Exception("Bad NLEN");
            int length = ((lenResp[0] & 0xff) << 8) | (lenResp[1] & 0xff);
            if (length <= 0 || length > 32767) throw new Exception("Invalid NDEF length");
            byte[] data = new byte[length];
            int read = 0;
            int chunkLimit = Math.max(1, Math.min(240, iso.getMaxTransceiveLength() - 2));
            while (read < length) {
                int count = Math.min(chunkLimit, length - read);
                int offset = read + 2;
                byte[] cmd = new byte[]{0x00, (byte)0xB0, (byte)(offset >> 8), (byte)offset, (byte)count};
                byte[] resp = iso.transceive(cmd);
                if (!ok(resp) || resp.length < 2) throw new Exception("Read failed");
                int got = resp.length - 2;
                if (got <= 0) throw new Exception("Empty read");
                int copy = Math.min(got, length - read);
                System.arraycopy(resp, 0, data, read, copy);
                read += copy;
            }
            NdefMessage message = new NdefMessage(data);
            NdefRecord record = chooseRecord(message);
            if (record == null) throw new Exception("Empty NDEF");
            runOnUiThread(() -> renderRecord(record));
        } catch (Exception e) {
            runOnUiThread(() -> showError(getString(R.string.receive_failed)));
        } finally { try { iso.close(); } catch (Exception ignored) {} }
    }

    private NdefRecord chooseRecord(NdefMessage message) {
        if (message == null) return null;
        NdefRecord fallback = null;
        for (NdefRecord record : message.getRecords()) {
            if (record == null) continue;
            if (fallback == null) fallback = record;
            String mime = record.toMimeType();
            if (mime != null) return record;
            if (record.toUri() != null) return record;
            if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) return record;
        }
        return fallback;
    }

    private void renderRecord(NdefRecord r) {
        setReceiveMotion(false);
        lastRecord = r; lastText = ""; lastUri = ""; lastVCard = "";
        String mime = r.toMimeType();
        android.net.Uri uri = r.toUri();
        if (mime != null && (mime.equalsIgnoreCase("text/vcard") || mime.equalsIgnoreCase("text/x-vcard"))) {
            lastVCard = new String(r.getPayload(), StandardCharsets.UTF_8);
            resultType.setText(R.string.type_contact);
            lastText = VCardParser.summary(lastVCard);
            resultValue.setText(lastText);
            primaryAction.setText(R.string.save_contact);
            primaryAction.setVisibility(View.VISIBLE);
        } else if (r.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(r.getType(), NdefRecord.RTD_TEXT)) {
            byte[] p = r.getPayload();
            int langLen = p.length == 0 ? 0 : (p[0] & 0x3F);
            boolean utf16 = p.length > 0 && (p[0] & 0x80) != 0;
            Charset cs = utf16 ? StandardCharsets.UTF_16 : StandardCharsets.UTF_8;
            int start = Math.min(p.length, 1 + langLen);
            lastText = new String(p, start, p.length - start, cs);
            resultType.setText(R.string.type_text);
            resultValue.setText(lastText);
            primaryAction.setVisibility(View.GONE);
        } else if (uri != null) {
            lastUri = uri.toString();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
            if ("mailto".equals(scheme)) {
                lastText = uri.getSchemeSpecificPart();
                resultType.setText(R.string.type_email);
                resultValue.setText(lastText);
                primaryAction.setText(R.string.open);
            } else if ("tel".equals(scheme)) {
                lastText = uri.getSchemeSpecificPart();
                resultType.setText(R.string.type_phone);
                resultValue.setText(lastText);
                primaryAction.setText(R.string.open);
            } else {
                lastText = lastUri;
                resultType.setText(R.string.type_url);
                resultValue.setText(lastUri);
                primaryAction.setText(R.string.open_link);
            }
            primaryAction.setVisibility(View.VISIBLE);
        } else if (mime != null) {
            lastText = new String(r.getPayload(), StandardCharsets.UTF_8);
            resultType.setText(mime);
            resultValue.setText(lastText);
            primaryAction.setVisibility(View.GONE);
        } else {
            lastText = NdefHostApduService.toHex(r.getPayload());
            resultType.setText(R.string.received_data);
            resultValue.setText(lastText);
            primaryAction.setVisibility(View.GONE);
        }
        copyAction.setVisibility(View.VISIBLE);
        shareAction.setVisibility(View.VISIBLE);
        if (resultCard.getVisibility() != View.VISIBLE) {
            Motion.hide(scanCopy);
            Motion.reveal(this, resultCard);
        } else {
            Motion.flash(resultCard);
        }
        scanTitle.setText(R.string.receive_success);
        scanSubtitle.setText(R.string.receive_success_subtitle);
        Motion.success(scanStage);
    }

    private void doPrimaryAction() {
        if (!lastVCard.isEmpty()) {
            VCardParser.Contact c = VCardParser.parse(lastVCard);
            Intent i = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
            i.putExtra(ContactsContract.Intents.Insert.NAME, c.name);
            i.putExtra(ContactsContract.Intents.Insert.PHONE, c.phone);
            i.putExtra(ContactsContract.Intents.Insert.EMAIL, c.email);
            i.putExtra(ContactsContract.Intents.Insert.COMPANY, c.org);
            i.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, c.title);
            ArrayList<ContentValues> rows = new ArrayList<>();
            if (!c.address.isEmpty()) {
                ContentValues v = new ContentValues();
                v.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
                v.put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, c.address);
                rows.add(v);
            }
            if (!c.web.isEmpty()) {
                ContentValues v = new ContentValues();
                v.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
                v.put(ContactsContract.CommonDataKinds.Website.URL, c.web);
                rows.add(v);
            }
            if (!rows.isEmpty()) i.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, rows);
            try { startActivity(i); } catch (Exception e) { Toast.makeText(this, R.string.contacts_app_missing, Toast.LENGTH_SHORT).show(); }
        } else if (!lastUri.isEmpty()) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(lastUri))); }
            catch (Exception e) { Toast.makeText(this, R.string.no_handler, Toast.LENGTH_SHORT).show(); }
        }
    }

    private void copyResult() {
        ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("TapForge", lastVCard.isEmpty() ? lastText : lastVCard));
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        Motion.success(copyAction);
    }

    private void openNfcSettings() {
        try { startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS)); }
        catch (Exception e) { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
    }

    private void shareResult() {
        String value = lastVCard.isEmpty() ? lastText : lastVCard;
        if (value == null || value.isEmpty()) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(lastVCard.isEmpty() ? "text/plain" : "text/vcard");
        send.putExtra(Intent.EXTRA_TEXT, value);
        try { startActivity(Intent.createChooser(send, getString(R.string.share))); }
        catch (Exception e) { Toast.makeText(this, R.string.no_handler, Toast.LENGTH_SHORT).show(); }
    }

    private void showError(String s) {
        if (scanCopy.getVisibility() != View.VISIBLE) Motion.reveal(this, scanCopy);
        if (resultCard.getVisibility() == View.VISIBLE) Motion.hide(resultCard);
        scanTitle.setText(R.string.receive_try_again);
        scanSubtitle.setText(s);
        Motion.error(scanStage);
        Motion.flash(scanGlyph);
        setReceiveMotion(adapter != null && adapter.isEnabled());
    }

    private void setReceiveMotion(boolean enabled) {
        Motion.receiveGlyph(scanGlyph, enabled);
        Motion.receiveHalo(scanHalo, enabled, false);
        Motion.receiveHalo(scanHaloOuter, enabled, true);
    }

    private void tuneReceiveLayout() {
        Configuration c = getResources().getConfiguration();
        int width = Math.max(1, c.screenWidthDp);
        int height = Math.max(1, c.screenHeightDp);
        boolean landscape = c.orientation == Configuration.ORIENTATION_LANDSCAPE;

        // The scanner and its copy are separate layout regions. scanCopy is positioned
        // BELOW scanStage by RelativeLayout, so translated text can never drift into
        // the animated brand mark on unusual densities/aspect ratios.
        int scene = Math.min((int)(width * (landscape ? 0.40f : 0.84f)),
                (int)(height * (landscape ? 0.43f : 0.39f)));
        scene = Math.max(landscape ? 154 : 238, Math.min(landscape ? 220 : 330, scene));

        setSquare(scanStage, scene);
        setSquare(scanHaloOuter, Math.round(scene * 0.76f));
        setSquare(scanHalo, Math.round(scene * 0.61f));
        setSquare(scanGlyph, Math.round(scene * 0.42f));

        // Keep a predictable optical gap below the animated rings. This is a real
        // layout margin rather than translationY, so font scaling and localization
        // cannot make the text overlap the scanner.
        ViewGroup.LayoutParams rawCopy = scanCopy.getLayoutParams();
        if (rawCopy instanceof android.widget.RelativeLayout.LayoutParams) {
            android.widget.RelativeLayout.LayoutParams copy =
                    (android.widget.RelativeLayout.LayoutParams) rawCopy;
            copy.topMargin = dp(landscape ? 16 : (height > 780 ? 30 : 24));
            scanCopy.setLayoutParams(copy);
        }
        scanCopy.setTranslationY(0f);

        FrameLayout.LayoutParams panel = (FrameLayout.LayoutParams) resultCard.getLayoutParams();
        if (width >= 600) {
            panel.width = dp(Math.min(540, width - 64));
            panel.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            panel.leftMargin = 0;
            panel.rightMargin = 0;
        } else {
            panel.width = ViewGroup.LayoutParams.MATCH_PARENT;
            panel.gravity = Gravity.BOTTOM;
            panel.leftMargin = dp(20);
            panel.rightMargin = dp(20);
        }
        resultCard.setLayoutParams(panel);
    }

    private void setSquare(View v, int sizeDp) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = dp(sizeDp);
        lp.height = dp(sizeDp);
        v.setLayoutParams(lp);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    private static boolean ok(byte[] r) { return r != null && r.length >= 2 && (r[r.length-2]&0xff)==0x90 && (r[r.length-1]&0xff)==0x00; }
    private static byte[] selectAid(String aid) { byte[] a=hex(aid); byte[] out=new byte[6+a.length]; out[0]=0;out[1]=(byte)0xA4;out[2]=4;out[3]=0;out[4]=(byte)a.length;System.arraycopy(a,0,out,5,a.length);out[out.length-1]=0;return out; }
    private static byte[] hex(String s) { int n=s.length()/2; byte[] o=new byte[n]; for(int i=0;i<n;i++)o[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16); return o; }
}
