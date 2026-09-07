package com.tapforge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends Activity {
    public static final String PREFS = "tapforge_prefs";

    public static final String KEY_MODE = "mode";
    public static final String MODE_NDEF = "ndef";
    public static final String MODE_CUSTOM = "custom";

    public static final String KEY_NDEF_TYPE = "ndef_type";
    public static final String TYPE_URL = "url";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_PHONE = "phone";
    public static final String TYPE_VCARD = "vcard";
    public static final String TYPE_MIME = "mime";
    public static final String TYPE_RAW_NDEF = "raw_ndef_profile";
    public static final String KEY_RAW_NDEF = "raw_ndef_profile_bytes";
    public static final String KEY_MIME_TYPE = "mime_type";
    public static final String KEY_NDEF_VALUE = "ndef_value";
    public static final String KEY_VCARD_NAME = "vcard_name";
    public static final String KEY_VCARD_PHONE = "vcard_phone";
    public static final String KEY_VCARD_EMAIL = "vcard_email";
    public static final String KEY_VCARD_ORG = "vcard_org";
    public static final String KEY_VCARD_TITLE = "vcard_title";
    public static final String KEY_VCARD_ADDRESS = "vcard_address";
    public static final String KEY_VCARD_WEB = "vcard_web";

    public static final String KEY_CUSTOM_AID = "custom_aid";
    public static final String KEY_CUSTOM_RESPONSE = "custom_response";
    public static final String KEY_CUSTOM_STATUS = "custom_status";

    public static final String KEY_THEME = "theme";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String KEY_HAPTICS = "haptics";
    public static final String KEY_REDUCE_MOTION = "reduce_motion";
    public static final String KEY_ONBOARDING_DONE = "onboarding_done";
    public static final String KEY_PROFILE_ENABLED = "profile_enabled";

    public static final String KEY_LANGUAGE = "language";
    public static final String LANG_SYSTEM = "system";
    public static final String LANG_ES = "es";
    public static final String LANG_EN = "en";
    public static final String LANG_PT = "pt";
    public static final String LANG_FR = "fr";
    public static final String LANG_DE = "de";
    public static final String LANG_IT = "it";
    public static final String LANG_JA = "ja";
    public static final String LANG_ZH = "zh";
    public static final String LANG_KO = "ko";
    public static final String LANG_RU = "ru";

    public static final String DEFAULT_URL = "https://example.com";
    public static final String DEFAULT_CUSTOM_AID = "F0010203040506";
    public static final String DEFAULT_CUSTOM_RESPONSE = "546170466F726765";
    public static final String DEFAULT_CUSTOM_STATUS = "9000";

    private static final int REQ_CONTACTS_PERMISSION = 1101;
    private static final int REQ_PICK_CONTACT = 1102;

    private SharedPreferences prefs;
    private String themeAtCreate;
    private String languageAtCreate;
    private String currentNdefType;
    private String renderedMode;

    private LinearLayout contentContainer;
    private LinearLayout ndefPanel;
    private LinearLayout customPanel;
    private LinearLayout valueFields;
    private LinearLayout vcardFields;
    private LinearLayout statusCard;
    private LinearLayout profileCard;
    private EditText ndefInput;
    private EditText mimeTypeInput;
    private EditText vcardNameInput;
    private EditText vcardPhoneInput;
    private EditText vcardEmailInput;
    private EditText vcardOrgInput;
    private EditText vcardTitleInput;
    private EditText vcardAddressInput;
    private EditText vcardWebInput;
    private EditText customAidInput;
    private EditText customResponseInput;
    private EditText customStatusInput;
    private Button pickContactButton;
    private Button saveButton;
    private TextView ndefTypeText;
    private TextView statusTitle;
    private TextView statusSubtitle;
    private TextView modeChip;
    private TextView deviceText;
    private TextView activeProfileText;
    private View statusDot;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_PROFILE_ENABLED)) {
            boolean hadProfile = prefs.contains(KEY_NDEF_VALUE) || prefs.contains(KEY_CUSTOM_AID);
            prefs.edit().putBoolean(KEY_PROFILE_ENABLED, hadProfile).apply();
        }
        ThemeHelper.apply(this);
        themeAtCreate = ThemeHelper.normalizedTheme(this);
        languageAtCreate = LocaleHelper.currentPreference(this);
        super.onCreate(savedInstanceState);
        if (!prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
            Navigation.replace(this, new Intent(this, OnboardingActivity.class));
            return;
        }
        setContentView(R.layout.activity_main);
        UiAdaptation.apply(this, findViewById(R.id.rootFrame));

        bindViews();
        UiAdaptation.applyAdaptiveWidth(this, contentContainer);
        loadSavedValues();
        updatePanels(false);
        updateNdefFields(false);
        updateDeviceInfo();
        updateNfcStatus();
        updateActiveProfile();
        applyKeepScreenOn();

        ImageButton menuButton = findViewById(R.id.menuButton);
        LinearLayout ndefTypeRow = findViewById(R.id.ndefTypeRow);
        menuButton.setOnClickListener(this::showMainMenu);
        TextView receiveQuickButton = findViewById(R.id.receiveQuickButton);
        receiveQuickButton.setOnClickListener(v -> Navigation.push(this, new Intent(this, ReceiveActivity.class)));
        TextView nfcToolsQuickButton = findViewById(R.id.nfcToolsQuickButton);
        nfcToolsQuickButton.setOnClickListener(v -> Navigation.pushTools(this, new Intent(this, NfcToolsActivity.class)));
        ndefTypeRow.setOnClickListener(this::showNdefTypeMenu);
        saveButton.setOnClickListener(v -> saveAndActivate());
        pickContactButton.setOnClickListener(v -> startContactPicker());

        Motion.press(menuButton);
        Motion.press(receiveQuickButton);
        Motion.press(nfcToolsQuickButton);
        Motion.press(ndefTypeRow);
        Motion.press(saveButton);
        Motion.press(pickContactButton);

        View activePanel = MODE_NDEF.equals(renderedMode) ? ndefPanel : customPanel;
        Motion.enter(this,
                findViewById(R.id.headerRow),
                statusCard,
                nfcToolsQuickButton,
                activePanel,
                saveButton,
                profileCard,
                findViewById(R.id.deviceCard));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentTheme = ThemeHelper.normalizedTheme(this);
        String currentLanguage = LocaleHelper.currentPreference(this);
        if (!currentTheme.equals(themeAtCreate) || !currentLanguage.equals(languageAtCreate)) {
            recreate();
            return;
        }
        applyKeepScreenOn();
        updatePanels(true);
        updateNfcStatus();
        updateActiveProfile();
        activatePreferredService();
    }

    @Override
    protected void onPause() {
        Motion.pulse(statusDot, false);
        clearPreferredService();
        super.onPause();
    }

    private void bindViews() {
        contentContainer = findViewById(R.id.contentContainer);
        ndefPanel = findViewById(R.id.ndefPanel);
        customPanel = findViewById(R.id.customPanel);
        valueFields = findViewById(R.id.valueFields);
        vcardFields = findViewById(R.id.vcardFields);
        statusCard = findViewById(R.id.statusCard);
        profileCard = findViewById(R.id.profileCard);
        ndefInput = findViewById(R.id.ndefInput);
        mimeTypeInput = findViewById(R.id.mimeTypeInput);
        vcardNameInput = findViewById(R.id.vcardNameInput);
        vcardPhoneInput = findViewById(R.id.vcardPhoneInput);
        vcardEmailInput = findViewById(R.id.vcardEmailInput);
        vcardOrgInput = findViewById(R.id.vcardOrgInput);
        vcardTitleInput = findViewById(R.id.vcardTitleInput);
        vcardAddressInput = findViewById(R.id.vcardAddressInput);
        vcardWebInput = findViewById(R.id.vcardWebInput);
        customAidInput = findViewById(R.id.customAidInput);
        customResponseInput = findViewById(R.id.customResponseInput);
        customStatusInput = findViewById(R.id.customStatusInput);
        pickContactButton = findViewById(R.id.pickContactButton);
        saveButton = findViewById(R.id.saveButton);
        ndefTypeText = findViewById(R.id.ndefTypeText);
        statusTitle = findViewById(R.id.statusTitle);
        statusSubtitle = findViewById(R.id.statusSubtitle);
        modeChip = findViewById(R.id.modeChip);
        deviceText = findViewById(R.id.deviceText);
        activeProfileText = findViewById(R.id.activeProfileText);
        statusDot = findViewById(R.id.statusDot);
    }

    private void loadSavedValues() {
        currentNdefType = prefs.getString(KEY_NDEF_TYPE, TYPE_URL);
        if (currentNdefType == null) currentNdefType = TYPE_URL;
        ndefInput.setText(prefs.getString(KEY_NDEF_VALUE, ""));
        mimeTypeInput.setText(prefs.getString(KEY_MIME_TYPE, "text/plain"));
        vcardNameInput.setText(prefs.getString(KEY_VCARD_NAME, ""));
        vcardPhoneInput.setText(prefs.getString(KEY_VCARD_PHONE, ""));
        vcardEmailInput.setText(prefs.getString(KEY_VCARD_EMAIL, ""));
        vcardOrgInput.setText(prefs.getString(KEY_VCARD_ORG, ""));
        vcardTitleInput.setText(prefs.getString(KEY_VCARD_TITLE, ""));
        vcardAddressInput.setText(prefs.getString(KEY_VCARD_ADDRESS, ""));
        vcardWebInput.setText(prefs.getString(KEY_VCARD_WEB, ""));
        customAidInput.setText(prefs.getString(KEY_CUSTOM_AID, DEFAULT_CUSTOM_AID));
        customResponseInput.setText(prefs.getString(KEY_CUSTOM_RESPONSE, DEFAULT_CUSTOM_RESPONSE));
        customStatusInput.setText(prefs.getString(KEY_CUSTOM_STATUS, DEFAULT_CUSTOM_STATUS));
    }

    private void updatePanels(boolean animate) {
        String mode = prefs.getString(KEY_MODE, MODE_NDEF);
        if (mode == null) mode = MODE_NDEF;
        boolean ndef = MODE_NDEF.equals(mode);
        modeChip.setText(ndef ? "NDEF" : "APDU");

        if (renderedMode == null || !animate) {
            ndefPanel.setVisibility(ndef ? View.VISIBLE : View.GONE);
            customPanel.setVisibility(ndef ? View.GONE : View.VISIBLE);
            renderedMode = mode;
            return;
        }
        if (!mode.equals(renderedMode)) {
            Motion.swap(this, MODE_NDEF.equals(renderedMode) ? ndefPanel : customPanel,
                    ndef ? ndefPanel : customPanel);
            renderedMode = mode;
        }
    }

    private void showNdefTypeMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, getString(R.string.type_url));
        menu.getMenu().add(0, 2, 1, getString(R.string.type_text));
        menu.getMenu().add(0, 3, 2, getString(R.string.type_email));
        menu.getMenu().add(0, 4, 3, getString(R.string.type_phone));
        menu.getMenu().add(0, 5, 4, getString(R.string.type_contact));
        menu.getMenu().add(0, 6, 5, getString(R.string.type_custom_mime));
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: currentNdefType = TYPE_URL; break;
                case 2: currentNdefType = TYPE_TEXT; break;
                case 3: currentNdefType = TYPE_EMAIL; break;
                case 4: currentNdefType = TYPE_PHONE; break;
                case 5: currentNdefType = TYPE_VCARD; break;
                case 6: currentNdefType = TYPE_MIME; break;
                default: return false;
            }
            updateNdefFields(true);
            Motion.flash(ndefTypeText);
            return true;
        });
        menu.show();
    }

    private void updateNdefFields(boolean animate) {
        if (TYPE_RAW_NDEF.equals(currentNdefType)) {
            ndefTypeText.setText(R.string.type_multi_record);
            valueFields.setVisibility(View.GONE);
            vcardFields.setVisibility(View.GONE);
            return;
        }
        boolean contact = TYPE_VCARD.equals(currentNdefType);
        boolean wasContact = vcardFields.getVisibility() == View.VISIBLE;

        if (contact != wasContact || (!contact && valueFields.getVisibility() != View.VISIBLE)) {
            if (animate) Motion.swap(this, contact ? valueFields : vcardFields, contact ? vcardFields : valueFields);
            else {
                valueFields.setVisibility(contact ? View.GONE : View.VISIBLE);
                vcardFields.setVisibility(contact ? View.VISIBLE : View.GONE);
            }
        }

        mimeTypeInput.setVisibility(TYPE_MIME.equals(currentNdefType) ? View.VISIBLE : View.GONE);
        if (TYPE_URL.equals(currentNdefType)) {
            ndefTypeText.setText(R.string.type_url);
            ndefInput.setHint(R.string.hint_url);
            ndefInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        } else if (TYPE_TEXT.equals(currentNdefType)) {
            ndefTypeText.setText(R.string.type_text);
            ndefInput.setHint(R.string.hint_text);
            ndefInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        } else if (TYPE_EMAIL.equals(currentNdefType)) {
            ndefTypeText.setText(R.string.type_email);
            ndefInput.setHint(R.string.hint_email);
            ndefInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        } else if (TYPE_PHONE.equals(currentNdefType)) {
            ndefTypeText.setText(R.string.type_phone);
            ndefInput.setHint(R.string.hint_phone);
            ndefInput.setInputType(InputType.TYPE_CLASS_PHONE);
        } else if (TYPE_VCARD.equals(currentNdefType)) {
            ndefTypeText.setText(R.string.type_contact);
        } else {
            ndefTypeText.setText(R.string.type_custom_mime);
            ndefInput.setHint(R.string.hint_payload);
            ndefInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
    }

    private void showMainMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 4, 0, getString(R.string.receive));
        popup.getMenu().add(0, 6, 1, getString(R.string.nfc_tools));
        if (prefs.getBoolean(KEY_PROFILE_ENABLED, false))
            popup.getMenu().add(0, 5, 1, getString(R.string.deactivate_profile));
        popup.getMenu().add(0, 1, 2, getString(R.string.settings));
        popup.getMenu().add(0, 2, 3, getString(R.string.system_nfc_settings));
        popup.getMenu().add(0, 3, 4, getString(R.string.about_tapforge));
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 4) {
                Navigation.push(this, new Intent(this, ReceiveActivity.class));
                return true;
            }
            if (item.getItemId() == 6) {
                Navigation.pushTools(this, new Intent(this, NfcToolsActivity.class));
                return true;
            }
            if (item.getItemId() == 5) {
                disableProfile();
                return true;
            }
            if (item.getItemId() == 1) {
                Navigation.push(this, new Intent(this, SettingsActivity.class));
                return true;
            }
            if (item.getItemId() == 2) {
                openNfcSettings();
                return true;
            }
            if (item.getItemId() == 3) {
                showAbout();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void saveAndActivate() {
        if (!deviceSupportsHce()) {
            Toast.makeText(this, R.string.error_hce_unsupported, Toast.LENGTH_LONG).show();
            return;
        }

        String mode = prefs.getString(KEY_MODE, MODE_NDEF);
        SharedPreferences.Editor editor = prefs.edit();

        if (MODE_NDEF.equals(mode)) {
            editor.putString(KEY_NDEF_TYPE, currentNdefType);
            if (TYPE_RAW_NDEF.equals(currentNdefType)) {
                String raw = prefs.getString(KEY_RAW_NDEF, null);
                if (raw == null || raw.isEmpty()) { Toast.makeText(this, R.string.invalid_ndef_content, Toast.LENGTH_LONG).show(); return; }
            } else if (TYPE_VCARD.equals(currentNdefType)) {
                String name = text(vcardNameInput);
                String phone = text(vcardPhoneInput);
                String email = text(vcardEmailInput);
                String org = text(vcardOrgInput);
                String title = text(vcardTitleInput);
                String address = text(vcardAddressInput);
                String web = text(vcardWebInput);

                if (name.isEmpty() && phone.isEmpty() && email.isEmpty() && org.isEmpty() &&
                        title.isEmpty() && address.isEmpty() && web.isEmpty()) {
                    vcardNameInput.setError(getString(R.string.error_contact_required));
                    return;
                }
                if (!email.isEmpty() && !email.contains("@")) {
                    vcardEmailInput.setError(getString(R.string.error_email));
                    return;
                }
                if (!web.isEmpty()) {
                    if (!web.contains("://")) web = "https://" + web;
                    if (!isHttpUrl(web)) {
                        vcardWebInput.setError(getString(R.string.error_url));
                        return;
                    }
                    vcardWebInput.setText(web);
                }

                editor.putString(KEY_VCARD_NAME, name)
                        .putString(KEY_VCARD_PHONE, phone)
                        .putString(KEY_VCARD_EMAIL, email)
                        .putString(KEY_VCARD_ORG, org)
                        .putString(KEY_VCARD_TITLE, title)
                        .putString(KEY_VCARD_ADDRESS, address)
                        .putString(KEY_VCARD_WEB, web);
            } else {
                String value = text(ndefInput);
                if (value.isEmpty()) {
                    ndefInput.setError(getString(R.string.error_content_required));
                    return;
                }
                if (TYPE_URL.equals(currentNdefType)) {
                    if (!value.contains("://")) value = "https://" + value;
                    if (!isHttpUrl(value)) {
                        ndefInput.setError(getString(R.string.error_url));
                        return;
                    }
                    ndefInput.setText(value);
                } else if (TYPE_EMAIL.equals(currentNdefType)) {
                    if (value.toLowerCase(Locale.ROOT).startsWith("mailto:")) value = value.substring(7);
                    if (!value.contains("@")) {
                        ndefInput.setError(getString(R.string.error_email));
                        return;
                    }
                    ndefInput.setText(value);
                } else if (TYPE_PHONE.equals(currentNdefType)) {
                    if (value.toLowerCase(Locale.ROOT).startsWith("tel:")) value = value.substring(4);
                    ndefInput.setText(value);
                } else if (TYPE_MIME.equals(currentNdefType)) {
                    String mimeType = text(mimeTypeInput).toLowerCase(Locale.ROOT);
                    if (!mimeType.matches("[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+")) {
                        mimeTypeInput.setError(getString(R.string.error_mime));
                        return;
                    }
                    editor.putString(KEY_MIME_TYPE, mimeType);
                }
                editor.putString(KEY_NDEF_VALUE, value);
            }
            editor.apply();
            unregisterCustomAid();
        } else {
            String aid = normalizeHex(text(customAidInput));
            String response = normalizeHex(text(customResponseInput));
            String status = normalizeHex(text(customStatusInput));

            if (!isValidAid(aid)) {
                customAidInput.setError(getString(R.string.error_aid));
                return;
            }
            if (!response.isEmpty() && !isEvenHex(response)) {
                customResponseInput.setError(getString(R.string.error_hex_response));
                return;
            }
            if (status.isEmpty()) status = DEFAULT_CUSTOM_STATUS;
            if (status.length() != 4 || !isEvenHex(status)) {
                customStatusInput.setError(getString(R.string.error_status_word));
                return;
            }

            customAidInput.setText(aid);
            customResponseInput.setText(response);
            customStatusInput.setText(status);
            editor.putString(KEY_CUSTOM_AID, aid)
                    .putString(KEY_CUSTOM_RESPONSE, response)
                    .putString(KEY_CUSTOM_STATUS, status)
                    .apply();

            if (!registerCustomAid(aid)) {
                Toast.makeText(this, R.string.error_register_aid, Toast.LENGTH_LONG).show();
            }
        }

        prefs.edit().putBoolean(KEY_PROFILE_ENABLED, true).apply();
        activatePreferredService();
        updateNfcStatus();
        updateActiveProfile();
        Motion.success(saveButton);
        Motion.flash(statusCard);
        Motion.flash(profileCard);
        Toast.makeText(this, R.string.profile_applied, Toast.LENGTH_SHORT).show();
    }

    private void startContactPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACTS_PERMISSION);
            return;
        }
        launchContactPicker();
    }

    private void launchContactPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, REQ_PICK_CONTACT);
        } catch (Exception e) {
            Toast.makeText(this, R.string.contacts_app_missing, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchContactPicker();
            } else {
                Toast.makeText(this, R.string.contacts_permission_optional, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_CONTACT || resultCode != RESULT_OK || data == null || data.getData() == null) return;

        String contactId = null;
        String displayName = "";
        try (Cursor c = getContentResolver().query(data.getData(),
                new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                contactId = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                displayName = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            }
        } catch (Exception ignored) { }

        if (contactId == null) {
            Toast.makeText(this, R.string.contact_read_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        String phone = firstData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", contactId);
        String email = firstData(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?", contactId);
        String org = firstData(ContactsContract.Data.CONTENT_URI,
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
        String title = firstData(ContactsContract.Data.CONTENT_URI,
                ContactsContract.CommonDataKinds.Organization.TITLE,
                ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
        String address = firstData(ContactsContract.Data.CONTENT_URI,
                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                contactId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
        String web = firstData(ContactsContract.Data.CONTENT_URI,
                ContactsContract.CommonDataKinds.Website.URL,
                ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                contactId, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);

        vcardNameInput.setText(displayName == null ? "" : displayName);
        vcardPhoneInput.setText(phone);
        vcardEmailInput.setText(email);
        vcardOrgInput.setText(org);
        vcardTitleInput.setText(title);
        vcardAddressInput.setText(address);
        vcardWebInput.setText(web);
        Motion.success(pickContactButton);
        Motion.flash(vcardFields);
        Toast.makeText(this, R.string.contact_loaded, Toast.LENGTH_SHORT).show();
    }

    private String firstData(android.net.Uri uri, String column, String selection, String... args) {
        String sort = ContactsContract.Data.IS_SUPER_PRIMARY + " DESC, " + ContactsContract.Data.IS_PRIMARY + " DESC";
        try (Cursor c = getContentResolver().query(uri, new String[]{column}, selection, args, sort)) {
            if (c != null && c.moveToFirst()) {
                String value = c.getString(c.getColumnIndexOrThrow(column));
                return value == null ? "" : value;
            }
        } catch (Exception ignored) { }
        return "";
    }

    private void activatePreferredService() {
        if (!prefs.getBoolean(KEY_PROFILE_ENABLED, false)) return;
        try {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
            if (adapter == null || !adapter.isEnabled() || !deviceSupportsHce()) return;
            CardEmulation ce = CardEmulation.getInstance(adapter);
            ComponentName component;
            if (MODE_CUSTOM.equals(prefs.getString(KEY_MODE, MODE_NDEF))) {
                String aid = prefs.getString(KEY_CUSTOM_AID, DEFAULT_CUSTOM_AID);
                registerCustomAid(aid);
                component = new ComponentName(this, CustomHostApduService.class);
            } else {
                component = new ComponentName(this, NdefHostApduService.class);
            }
            ce.setPreferredService(this, component);
        } catch (Exception ignored) { }
    }

    private void clearPreferredService() {
        try {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
            if (adapter == null) return;
            CardEmulation.getInstance(adapter).unsetPreferredService(this);
        } catch (Exception ignored) { }
    }

    private boolean registerCustomAid(String aid) {
        try {
            if (!isValidAid(aid)) return false;
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
            if (adapter == null) return false;
            CardEmulation ce = CardEmulation.getInstance(adapter);
            ComponentName component = new ComponentName(this, CustomHostApduService.class);
            return ce.registerAidsForService(component, CardEmulation.CATEGORY_OTHER,
                    Collections.singletonList(aid));
        } catch (Exception e) {
            return false;
        }
    }

    private void unregisterCustomAid() {
        try {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
            if (adapter == null) return;
            CardEmulation.getInstance(adapter).removeAidsForService(
                    new ComponentName(this, CustomHostApduService.class), CardEmulation.CATEGORY_OTHER);
        } catch (Exception ignored) { }
    }

    private void updateDeviceInfo() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        boolean hce = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        String manufacturer = nice(Build.MANUFACTURER);
        String model = Build.MODEL == null ? getString(R.string.unknown) : Build.MODEL;
        String state = adapter == null ? getString(R.string.nfc_unavailable) :
                (adapter.isEnabled() ? getString(R.string.nfc_on) : getString(R.string.nfc_off));
        deviceText.setText(getString(R.string.device_info_format,
                manufacturer, model, Build.VERSION.RELEASE, Build.VERSION.SDK_INT,
                state, hce ? getString(R.string.yes) : getString(R.string.no)));
    }

    private void updateNfcStatus() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        boolean hasHce = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        String mode = prefs.getString(KEY_MODE, MODE_NDEF);
        modeChip.setText(MODE_CUSTOM.equals(mode) ? "APDU" : "NDEF");
        boolean ready = false;
        boolean enabled = prefs.getBoolean(KEY_PROFILE_ENABLED, false);

        if (adapter == null) {
            statusTitle.setText(R.string.status_unavailable);
            statusSubtitle.setText(R.string.status_unavailable_detail);
            setStatusDot(getColor(R.color.danger));
        } else if (!adapter.isEnabled()) {
            statusTitle.setText(R.string.status_off);
            statusSubtitle.setText(R.string.status_off_detail);
            setStatusDot(getColor(R.color.warning));
        } else if (!hasHce) {
            statusTitle.setText(R.string.status_hce_limited);
            statusSubtitle.setText(R.string.status_hce_limited_detail);
            setStatusDot(getColor(R.color.warning));
        } else if (!enabled) {
            statusTitle.setText(R.string.status_inactive);
            statusSubtitle.setText(R.string.status_inactive_detail);
            setStatusDot(getColor(R.color.neutral));
        } else {
            statusTitle.setText(R.string.status_ready);
            statusSubtitle.setText(MODE_CUSTOM.equals(mode) ? R.string.status_ready_apdu : R.string.status_ready_ndef);
            setStatusDot(getColor(R.color.ready));
            ready = true;
        }
        Motion.pulse(statusDot, ready);
    }

    private void setStatusDot(int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        statusDot.setBackground(dot);
    }

    private void updateActiveProfile() {
        if (!prefs.getBoolean(KEY_PROFILE_ENABLED, false)) {
            activeProfileText.setText(R.string.profile_inactive);
            return;
        }
        String mode = prefs.getString(KEY_MODE, MODE_NDEF);
        if (MODE_CUSTOM.equals(mode)) {
            activeProfileText.setText("HCE / APDU\nAID  " + prefs.getString(KEY_CUSTOM_AID, DEFAULT_CUSTOM_AID) +
                    "\nSELECT  " + prefs.getString(KEY_CUSTOM_RESPONSE, DEFAULT_CUSTOM_RESPONSE) +
                    " + " + prefs.getString(KEY_CUSTOM_STATUS, DEFAULT_CUSTOM_STATUS));
            return;
        }

        String type = prefs.getString(KEY_NDEF_TYPE, TYPE_URL);
        String summary;
        if (TYPE_RAW_NDEF.equals(type)) {
            try {
                byte[] raw = android.util.Base64.decode(prefs.getString(KEY_RAW_NDEF, ""), android.util.Base64.DEFAULT);
                NdefMessage m = new NdefMessage(raw);
                summary = getString(R.string.type_multi_record) + " · " + getString(R.string.records_count, m.getRecords().length, raw.length);
            } catch (Exception e) { summary = getString(R.string.type_multi_record); }
        } else if (TYPE_VCARD.equals(type)) {
            String name = prefs.getString(KEY_VCARD_NAME, "");
            if (name == null || name.trim().isEmpty()) name = getString(R.string.no_name);
            summary = getString(R.string.type_contact) + " · " + name;
        } else {
            String value = prefs.getString(KEY_NDEF_VALUE, DEFAULT_URL);
            if (value == null) value = "";
            if (value.length() > 120) value = value.substring(0, 117) + "…";
            summary = displayType(type) + " · " + value;
        }
        activeProfileText.setText(summary);
    }

    private void disableProfile() {
        prefs.edit().putBoolean(KEY_PROFILE_ENABLED, false).apply();
        clearPreferredService();
        unregisterCustomAid();
        updateNfcStatus();
        updateActiveProfile();
        Motion.success(findViewById(R.id.menuButton));
        Motion.flash(statusCard);
        Motion.flash(profileCard);
        Toast.makeText(this, R.string.profile_deactivated, Toast.LENGTH_SHORT).show();
    }

    private void applyKeepScreenOn() {
        if (prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void openNfcSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        }
    }

    private boolean deviceSupportsHce() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
                && NfcAdapter.getDefaultAdapter(this) != null;
    }

    private String displayType(String type) {
        if (TYPE_TEXT.equals(type)) return getString(R.string.type_text);
        if (TYPE_EMAIL.equals(type)) return getString(R.string.type_email);
        if (TYPE_PHONE.equals(type)) return getString(R.string.type_phone);
        if (TYPE_MIME.equals(type)) return getString(R.string.type_custom_mime);
        if (TYPE_VCARD.equals(type)) return getString(R.string.type_contact);
        if (TYPE_RAW_NDEF.equals(type)) return getString(R.string.type_multi_record);
        return getString(R.string.type_url);
    }

    private String nice(String value) {
        if (value == null || value.isEmpty()) return getString(R.string.unknown);
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String text(EditText view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String normalizeHex(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isEvenHex(String value) {
        return value != null && value.matches("[0-9A-Fa-f]*") && (value.length() % 2 == 0);
    }

    public static boolean isValidAid(String aid) {
        return aid != null && aid.matches("[0-9A-F]{10,32}") && (aid.length() % 2 == 0);
    }
}
