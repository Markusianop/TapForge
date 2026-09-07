package com.tapforge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Base64;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.CheckBox;
import android.nfc.tech.Ndef;
import java.util.Arrays;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

public class TagWriteActivity extends Activity implements NfcAdapter.ReaderCallback {
    private NfcAdapter adapter;
    private String type = NdefFactory.URL;
    private NdefMessage pending;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private boolean armed;

    private TextView typeText, sizeHint, overlayTitle, overlaySubtitle;
    private EditText value, extra, cName, cPhone, cEmail, cOrg, cTitle, cAddress, cWeb;
    private View contactFields, overlay, halo, haloOuter, glyph;
    private CheckBox verifyWrite;
    private volatile boolean verifyEnabled = true;

    @Override protected void attachBaseContext(Context newBase) { super.attachBaseContext(LocaleHelper.wrap(newBase)); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_write);
        UiAdaptation.apply(this, findViewById(R.id.tagWriteRoot));
        UiAdaptation.applyAdaptiveWidth(this, findViewById(R.id.writeContent));
        adapter = NfcAdapter.getDefaultAdapter(this);
        bind();
        tuneWriteOverlay();
        setType(NdefFactory.URL, false);

        findViewById(R.id.writeBack).setOnClickListener(v -> Navigation.pop(this));
        findViewById(R.id.writeTypeRow).setOnClickListener(this::showTypeMenu);
        findViewById(R.id.useActiveProfile).setOnClickListener(v -> useActiveProfile());
        findViewById(R.id.startWriteButton).setOnClickListener(v -> armWrite());
        findViewById(R.id.saveWriterProfile).setOnClickListener(v -> saveCurrentToLibrary());
        findViewById(R.id.cancelWrite).setOnClickListener(v -> disarm());
        Motion.press(findViewById(R.id.writeBack)); Motion.press(findViewById(R.id.writeTypeRow));
        Motion.press(findViewById(R.id.useActiveProfile)); Motion.press(findViewById(R.id.saveWriterProfile)); Motion.press(findViewById(R.id.startWriteButton)); Motion.press(findViewById(R.id.cancelWrite));
        Motion.enter(this, findViewById(R.id.writeHeader), findViewById(R.id.writeTypeRow), findViewById(R.id.startWriteButton));

        TextWatcher watcher = new TextWatcher() { public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){ updateSize(); } public void afterTextChanged(Editable e){} };
        for (EditText e : new EditText[]{value, extra, cName, cPhone, cEmail, cOrg, cTitle, cAddress, cWeb}) e.addTextChangedListener(watcher);
    }

    private void bind() {
        typeText = findViewById(R.id.writeTypeText); sizeHint = findViewById(R.id.writeSizeHint);
        value = findViewById(R.id.writeValue); extra = findViewById(R.id.writeExtra);
        cName = findViewById(R.id.writeContactName); cPhone = findViewById(R.id.writeContactPhone); cEmail = findViewById(R.id.writeContactEmail);
        cOrg = findViewById(R.id.writeContactOrg); cTitle = findViewById(R.id.writeContactTitle); cAddress = findViewById(R.id.writeContactAddress); cWeb = findViewById(R.id.writeContactWeb);
        contactFields = findViewById(R.id.writeContactFields); overlay = findViewById(R.id.writeOverlay);
        overlayTitle = findViewById(R.id.writeOverlayTitle); overlaySubtitle = findViewById(R.id.writeOverlaySubtitle);
        halo = findViewById(R.id.writeHalo); haloOuter = findViewById(R.id.writeHaloOuter); glyph = findViewById(R.id.writeGlyph); verifyWrite = findViewById(R.id.writeVerify); verifyWrite.setOnCheckedChangeListener((b,checked) -> verifyEnabled = checked);
    }

    private void showTypeMenu(View anchor) {
        PopupMenu m = new PopupMenu(this, anchor);
        add(m,1,R.string.type_url); add(m,2,R.string.type_text); add(m,3,R.string.type_email); add(m,4,R.string.type_phone);
        add(m,5,R.string.type_sms); add(m,6,R.string.type_location); add(m,7,R.string.type_contact); add(m,8,R.string.type_wifi); add(m,9,R.string.type_app);
        add(m,10,R.string.type_custom_mime); add(m,11,R.string.type_external); add(m,12,R.string.type_raw_ndef);
        m.setOnMenuItemClickListener(i -> { String t;
            switch(i.getItemId()) { case 1:t=NdefFactory.URL;break; case 2:t=NdefFactory.TEXT;break; case 3:t=NdefFactory.EMAIL;break; case 4:t=NdefFactory.PHONE;break;
                case 5:t=NdefFactory.SMS;break; case 6:t=NdefFactory.LOCATION;break; case 7:t=NdefFactory.CONTACT;break; case 8:t=NdefFactory.WIFI;break; case 9:t=NdefFactory.APP;break;
                case 10:t=NdefFactory.MIME;break; case 11:t=NdefFactory.EXTERNAL;break; default:t=NdefFactory.RAW; }
            setType(t,true); return true; });
        m.show();
    }
    private void add(PopupMenu m, int id, int string) { m.getMenu().add(0,id,id,getString(string)); }

    private void setType(String t, boolean animate) {
        type = t;
        boolean contact = NdefFactory.CONTACT.equals(t);
        contactFields.setVisibility(contact ? View.VISIBLE : View.GONE);
        value.setVisibility(contact ? View.GONE : View.VISIBLE);
        extra.setVisibility(View.GONE);
        extra.setInputType(InputType.TYPE_CLASS_TEXT);
        value.setInputType(InputType.TYPE_CLASS_TEXT);
        if (NdefFactory.URL.equals(t)) { typeText.setText(R.string.type_url); value.setHint(R.string.hint_url); value.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI); }
        else if (NdefFactory.TEXT.equals(t)) { typeText.setText(R.string.type_text); value.setHint(R.string.hint_text); value.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE); }
        else if (NdefFactory.EMAIL.equals(t)) { typeText.setText(R.string.type_email); value.setHint(R.string.hint_email); value.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); }
        else if (NdefFactory.PHONE.equals(t)) { typeText.setText(R.string.type_phone); value.setHint(R.string.hint_phone); value.setInputType(InputType.TYPE_CLASS_PHONE); }
        else if (NdefFactory.SMS.equals(t)) { typeText.setText(R.string.type_sms); value.setHint(R.string.hint_phone); value.setInputType(InputType.TYPE_CLASS_PHONE); extra.setHint(R.string.sms_body); extra.setVisibility(View.VISIBLE); }
        else if (NdefFactory.LOCATION.equals(t)) { typeText.setText(R.string.type_location); value.setHint(R.string.hint_location); }
        else if (NdefFactory.CONTACT.equals(t)) { typeText.setText(R.string.type_contact); }
        else if (NdefFactory.WIFI.equals(t)) { typeText.setText(R.string.type_wifi); value.setHint(R.string.hint_wifi_ssid); extra.setHint(R.string.hint_wifi_password); extra.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); extra.setVisibility(View.VISIBLE); }
        else if (NdefFactory.APP.equals(t)) { typeText.setText(R.string.type_app); value.setHint(R.string.hint_package); }
        else if (NdefFactory.MIME.equals(t)) { typeText.setText(R.string.type_custom_mime); value.setHint(R.string.hint_payload); extra.setHint(R.string.hint_mime); extra.setVisibility(View.VISIBLE); }
        else if (NdefFactory.EXTERNAL.equals(t)) { typeText.setText(R.string.type_external); value.setHint(R.string.hint_payload); extra.setHint(R.string.hint_external); extra.setVisibility(View.VISIBLE); }
        else { typeText.setText(R.string.type_raw_ndef); value.setHint(R.string.hint_raw_ndef); }
        if (animate) { Motion.flash(typeText); Motion.flash(contact ? contactFields : value); }
        updateSize();
    }

    private NdefMessage build() throws Exception {
        return NdefFactory.build(type, value.getText().toString(), extra.getText().toString(),
                cName.getText().toString(), cPhone.getText().toString(), cEmail.getText().toString(), cOrg.getText().toString(),
                cTitle.getText().toString(), cAddress.getText().toString(), cWeb.getText().toString());
    }

    private void updateSize() {
        try { NdefMessage m = build(); sizeHint.setText(getString(R.string.ndef_size, m.toByteArray().length)); }
        catch (Exception e) { sizeHint.setText(""); }
    }

    private void useActiveProfile() {
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        if (!p.getBoolean(MainActivity.KEY_PROFILE_ENABLED, false) || MainActivity.MODE_CUSTOM.equals(p.getString(MainActivity.KEY_MODE, MainActivity.MODE_NDEF))) {
            Toast.makeText(this, R.string.no_ndef_profile, Toast.LENGTH_SHORT).show(); return;
        }
        String t = p.getString(MainActivity.KEY_NDEF_TYPE, MainActivity.TYPE_URL);
        if (MainActivity.TYPE_RAW_NDEF.equals(t)) {
            try {
                setType(NdefFactory.RAW, true);
                byte[] raw = Base64.decode(p.getString(MainActivity.KEY_RAW_NDEF, ""), Base64.DEFAULT);
                value.setText(NdefFactory.bytesToHex(raw));
                Motion.success(findViewById(R.id.useActiveProfile));
            } catch (Exception e) { Toast.makeText(this, R.string.invalid_ndef_content, Toast.LENGTH_SHORT).show(); }
            return;
        }
        if (MainActivity.TYPE_VCARD.equals(t)) {
            setType(NdefFactory.CONTACT, true);
            cName.setText(p.getString(MainActivity.KEY_VCARD_NAME,"")); cPhone.setText(p.getString(MainActivity.KEY_VCARD_PHONE,""));
            cEmail.setText(p.getString(MainActivity.KEY_VCARD_EMAIL,"")); cOrg.setText(p.getString(MainActivity.KEY_VCARD_ORG,""));
            cTitle.setText(p.getString(MainActivity.KEY_VCARD_TITLE,"")); cAddress.setText(p.getString(MainActivity.KEY_VCARD_ADDRESS,"")); cWeb.setText(p.getString(MainActivity.KEY_VCARD_WEB,""));
        } else {
            String mapped = MainActivity.TYPE_TEXT.equals(t)?NdefFactory.TEXT:MainActivity.TYPE_EMAIL.equals(t)?NdefFactory.EMAIL:MainActivity.TYPE_PHONE.equals(t)?NdefFactory.PHONE:MainActivity.TYPE_MIME.equals(t)?NdefFactory.MIME:NdefFactory.URL;
            setType(mapped,true); value.setText(p.getString(MainActivity.KEY_NDEF_VALUE,""));
            if (NdefFactory.MIME.equals(mapped)) extra.setText(p.getString(MainActivity.KEY_MIME_TYPE,"text/plain"));
        }
        Motion.success(findViewById(R.id.useActiveProfile));
    }

    private void saveCurrentToLibrary() {
        final NdefMessage msg;
        try { msg = build(); }
        catch (Exception e) { Toast.makeText(this, R.string.invalid_ndef_content, Toast.LENGTH_LONG).show(); return; }
        EditText name = new EditText(this); name.setHint(R.string.profile_name); name.setPadding(dp(20), dp(8), dp(20), dp(8));
        new AlertDialog.Builder(this).setTitle(R.string.save_to_library).setView(name)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (d,w) -> {
                    try { NdefProfileStore.save(this, name.getText().toString(), msg); Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show(); Motion.success(findViewById(R.id.saveWriterProfile)); }
                    catch (Exception ex) { Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show(); }
                }).show();
    }

    private void armWrite() {
        if (adapter == null) { Toast.makeText(this,R.string.nfc_unavailable,Toast.LENGTH_LONG).show(); return; }
        if (!adapter.isEnabled()) { Toast.makeText(this,R.string.status_off,Toast.LENGTH_LONG).show(); return; }
        try { pending = build(); }
        catch (Exception e) { Toast.makeText(this,R.string.invalid_ndef_content,Toast.LENGTH_LONG).show(); Motion.error(findViewById(R.id.startWriteButton)); return; }
        armed = true; overlay.setVisibility(View.VISIBLE); overlayTitle.setText(R.string.approach_tag); overlaySubtitle.setText(getString(R.string.write_bytes_ready,pending.toByteArray().length));
        Motion.enter(this, overlay); startMotion(); enableReader();
    }

    private void enableReader() {
        if (!armed || adapter == null) return;
        Bundle opts = new Bundle(); opts.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY,180);
        int flags=NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_NFC_F|NfcAdapter.FLAG_READER_NFC_V|NfcAdapter.FLAG_READER_NFC_BARCODE;
        try { adapter.enableReaderMode(this,this,flags,opts); } catch(Exception ignored){}
    }
    private void disableReader(){ try{ if(adapter!=null)adapter.disableReaderMode(this);}catch(Exception ignored){} }

    @Override public void onTagDiscovered(Tag tag) {
        if (!armed || pending==null || !busy.compareAndSet(false,true)) return;
        try {
            NdefUndoStore.capture(this, tag);
            TagScanActivity.writeMessage(tag,pending,true);
            boolean verifiedOk = true;
            if (verifyEnabled) {
                try { Thread.sleep(70L); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                Ndef n = Ndef.get(tag); NdefMessage readBack = null;
                try { if (n != null) { n.connect(); readBack = n.getNdefMessage(); } }
                finally { try { if (n != null) n.close(); } catch (Exception ignored) { } }
                verifiedOk = readBack != null && Arrays.equals(pending.toByteArray(), readBack.toByteArray());
                if (!verifiedOk) throw new IllegalStateException(getString(R.string.verify_failed_subtitle));
            }
            NfcHistory.add(this,"WRITE",getString(R.string.tag_written),TagInspector.uid(tag)+" · "+pending.toByteArray().length+" B"+(verifyEnabled?" · verified":""));
            runOnUiThread(() -> { disableReader(); stopMotion(); overlayTitle.setText(R.string.tag_written); overlaySubtitle.setText(verifyEnabled?R.string.verified_ok:R.string.tag_written_subtitle); Motion.success(glyph); armed=false; });
        } catch(Exception e) {
            String msg=e.getMessage()==null?getString(R.string.write_failed):e.getMessage();
            runOnUiThread(() -> { overlayTitle.setText(R.string.write_failed); overlaySubtitle.setText(msg); Motion.error(glyph); });
        } finally { busy.set(false); }
    }

    private void disarm(){ armed=false; pending=null; disableReader(); stopMotion(); overlay.setVisibility(View.GONE); }
    private void startMotion(){ Motion.receiveGlyph(glyph,true); Motion.receiveHalo(halo,true,false); Motion.receiveHalo(haloOuter,true,true); }
    private void stopMotion(){ Motion.receiveGlyph(glyph,false); Motion.receiveHalo(halo,false,false); Motion.receiveHalo(haloOuter,false,true); }
    @Override protected void onResume(){ super.onResume(); if(armed)enableReader(); }
    @Override protected void onPause(){ disableReader(); super.onPause(); }
    private void tuneWriteOverlay() {
        Configuration c = getResources().getConfiguration();
        int width = Math.max(1, c.screenWidthDp), height = Math.max(1, c.screenHeightDp);
        boolean landscape = c.orientation == Configuration.ORIENTATION_LANDSCAPE;
        int scene = Math.min((int)(width * (landscape ? .40f : .84f)), (int)(height * (landscape ? .43f : .39f)));
        scene = Math.max(landscape ? 154 : 238, Math.min(landscape ? 220 : 330, scene));
        setSquare(findViewById(R.id.writeScanStage), scene);
        setSquare(haloOuter, Math.round(scene * .76f));
        setSquare(halo, Math.round(scene * .61f));
        setSquare(glyph, Math.round(scene * .42f));
    }
    private void setSquare(View v, int sizeDp) { ViewGroup.LayoutParams lp=v.getLayoutParams(); lp.width=dp(sizeDp); lp.height=dp(sizeDp); v.setLayoutParams(lp); }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    @Override public void onBackPressed(){ if(overlay.getVisibility()==View.VISIBLE)disarm(); else Navigation.pop(this); }
}
