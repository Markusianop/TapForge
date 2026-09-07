package com.tapforge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private String themeAtCreate;
    private String languageAtCreate;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        ThemeHelper.apply(this);
        themeAtCreate = ThemeHelper.normalizedTheme(this);
        languageAtCreate = LocaleHelper.currentPreference(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        UiAdaptation.apply(this, findViewById(R.id.settingsRoot));

        LinearLayout content = findViewById(R.id.settingsContent);
        UiAdaptation.applyAdaptiveWidth(this, content);

        ImageButton back = findViewById(R.id.backButton);
        RadioGroup modeGroup = findViewById(R.id.modeRadioGroup);
        RadioGroup themeGroup = findViewById(R.id.themeRadioGroup);
        RadioGroup languageGroup = findViewById(R.id.languageRadioGroup);
        RadioButton modeNdef = findViewById(R.id.modeNdefRadio);
        RadioButton modeCustom = findViewById(R.id.modeCustomRadio);
        RadioButton themeSystem = findViewById(R.id.themeSystemRadio);
        RadioButton themeLight = findViewById(R.id.themeLightRadio);
        RadioButton themeDark = findViewById(R.id.themeDarkRadio);
        RadioButton languageSystem = findViewById(R.id.languageSystemRadio);
        RadioButton languageSpanish = findViewById(R.id.languageSpanishRadio);
        RadioButton languageEnglish = findViewById(R.id.languageEnglishRadio);
        RadioButton languagePortuguese = findViewById(R.id.languagePortugueseRadio);
        RadioButton languageFrench = findViewById(R.id.languageFrenchRadio);
        RadioButton languageGerman = findViewById(R.id.languageGermanRadio);
        RadioButton languageItalian = findViewById(R.id.languageItalianRadio);
        RadioButton languageJapanese = findViewById(R.id.languageJapaneseRadio);
        RadioButton languageChinese = findViewById(R.id.languageChineseRadio);
        RadioButton languageKorean = findViewById(R.id.languageKoreanRadio);
        RadioButton languageRussian = findViewById(R.id.languageRussianRadio);
        Switch keepScreen = findViewById(R.id.keepScreenSwitch);
        Switch haptics = findViewById(R.id.hapticsSwitch);
        Switch reduceMotion = findViewById(R.id.reduceMotionSwitch);
        Button showOnboarding = findViewById(R.id.showOnboardingButton);
        TextView device = findViewById(R.id.settingsDeviceText);
        TextView version = findViewById(R.id.settingsVersionText);

        back.setOnClickListener(v -> Navigation.pop(this));
        Motion.press(back);

        String mode = prefs.getString(MainActivity.KEY_MODE, MainActivity.MODE_NDEF);
        if (MainActivity.MODE_CUSTOM.equals(mode)) modeCustom.setChecked(true);
        else modeNdef.setChecked(true);

        String theme = ThemeHelper.normalizedTheme(this);
        if (MainActivity.THEME_DARK.equals(theme)) themeDark.setChecked(true);
        else if (MainActivity.THEME_LIGHT.equals(theme)) themeLight.setChecked(true);
        else themeSystem.setChecked(true);

        String language = LocaleHelper.currentPreference(this);
        if (MainActivity.LANG_ES.equals(language)) languageSpanish.setChecked(true);
        else if (MainActivity.LANG_EN.equals(language)) languageEnglish.setChecked(true);
        else if (MainActivity.LANG_PT.equals(language)) languagePortuguese.setChecked(true);
        else if (MainActivity.LANG_FR.equals(language)) languageFrench.setChecked(true);
        else if (MainActivity.LANG_DE.equals(language)) languageGerman.setChecked(true);
        else if (MainActivity.LANG_IT.equals(language)) languageItalian.setChecked(true);
        else if (MainActivity.LANG_JA.equals(language)) languageJapanese.setChecked(true);
        else if (MainActivity.LANG_ZH.equals(language)) languageChinese.setChecked(true);
        else if (MainActivity.LANG_KO.equals(language)) languageKorean.setChecked(true);
        else if (MainActivity.LANG_RU.equals(language)) languageRussian.setChecked(true);
        else languageSystem.setChecked(true);

        keepScreen.setChecked(prefs.getBoolean(MainActivity.KEY_KEEP_SCREEN_ON, true));
        haptics.setChecked(prefs.getBoolean(MainActivity.KEY_HAPTICS, true));
        reduceMotion.setChecked(prefs.getBoolean(MainActivity.KEY_REDUCE_MOTION, false));
        updateDeviceInfo(device);
        updateAppVersion(version);

        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String next = checkedId == R.id.modeCustomRadio ? MainActivity.MODE_CUSTOM : MainActivity.MODE_NDEF;
            prefs.edit().putString(MainActivity.KEY_MODE, next).putBoolean(MainActivity.KEY_PROFILE_ENABLED, false).apply();
        });

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String next;
            if (checkedId == R.id.themeDarkRadio) next = MainActivity.THEME_DARK;
            else if (checkedId == R.id.themeLightRadio) next = MainActivity.THEME_LIGHT;
            else next = MainActivity.THEME_SYSTEM;
            if (!next.equals(ThemeHelper.normalizedTheme(this))) {
                prefs.edit().putString(MainActivity.KEY_THEME, next).apply();
                recreate();
            }
        });

        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String next;
            if (checkedId == R.id.languageSpanishRadio) next = MainActivity.LANG_ES;
            else if (checkedId == R.id.languageEnglishRadio) next = MainActivity.LANG_EN;
            else if (checkedId == R.id.languagePortugueseRadio) next = MainActivity.LANG_PT;
            else if (checkedId == R.id.languageFrenchRadio) next = MainActivity.LANG_FR;
            else if (checkedId == R.id.languageGermanRadio) next = MainActivity.LANG_DE;
            else if (checkedId == R.id.languageItalianRadio) next = MainActivity.LANG_IT;
            else if (checkedId == R.id.languageJapaneseRadio) next = MainActivity.LANG_JA;
            else if (checkedId == R.id.languageChineseRadio) next = MainActivity.LANG_ZH;
            else if (checkedId == R.id.languageKoreanRadio) next = MainActivity.LANG_KO;
            else if (checkedId == R.id.languageRussianRadio) next = MainActivity.LANG_RU;
            else next = MainActivity.LANG_SYSTEM;
            if (!next.equals(LocaleHelper.currentPreference(this))) {
                prefs.edit().putString(MainActivity.KEY_LANGUAGE, next).apply();
                recreate();
            }
        });

        keepScreen.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(MainActivity.KEY_KEEP_SCREEN_ON, isChecked).apply());
        haptics.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(MainActivity.KEY_HAPTICS, isChecked).apply());
        reduceMotion.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(MainActivity.KEY_REDUCE_MOTION, isChecked).apply());
        showOnboarding.setOnClickListener(v -> {
            prefs.edit().putBoolean(MainActivity.KEY_ONBOARDING_DONE, false).apply();
            Navigation.replace(this, new Intent(this, OnboardingActivity.class));
        });
        Motion.press(showOnboarding);

        Motion.enter(this,
                findViewById(R.id.settingsHeader),
                findViewById(R.id.modeCard),
                findViewById(R.id.appearanceCard),
                findViewById(R.id.languageCard),
                findViewById(R.id.screenCard),
                findViewById(R.id.interactionCard),
                findViewById(R.id.deviceSettingsCard));
    }

    @Override public void onBackPressed() { Navigation.pop(this); }

    @Override
    protected void onResume() {
        super.onResume();
        String themeNow = ThemeHelper.normalizedTheme(this);
        String languageNow = LocaleHelper.currentPreference(this);
        if (!themeNow.equals(themeAtCreate) || !languageNow.equals(languageAtCreate)) {
            recreate();
        }
    }

    private void updateAppVersion(TextView view) {
        String versionName = "";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (versionName == null || versionName.trim().isEmpty()) {
            view.setText(getString(R.string.app_name));
        } else {
            view.setText(getString(R.string.app_name) + " " + versionName);
        }
    }

    private void updateDeviceInfo(TextView view) {
        PackageManager pm = getPackageManager();
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        boolean hce = pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        String manufacturer = nice(Build.MANUFACTURER);
        String model = Build.MODEL == null ? getString(R.string.unknown) : Build.MODEL;
        String state = adapter == null ? getString(R.string.nfc_unavailable) :
                (adapter.isEnabled() ? getString(R.string.nfc_on) : getString(R.string.nfc_off));
        view.setText(getString(R.string.device_info_format,
                manufacturer, model, Build.VERSION.RELEASE, Build.VERSION.SDK_INT,
                state, hce ? getString(R.string.yes) : getString(R.string.no)));
    }

    private String nice(String value) {
        if (value == null || value.isEmpty()) return getString(R.string.unknown);
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
