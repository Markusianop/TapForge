package com.tapforge;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public final class LocaleHelper {
    private LocaleHelper() { }

    public static String currentPreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString(MainActivity.KEY_LANGUAGE, MainActivity.LANG_SYSTEM);
        return value == null ? MainActivity.LANG_SYSTEM : value;
    }

    public static Context wrap(Context base) {
        String selected = currentPreference(base);
        if (MainActivity.LANG_SYSTEM.equals(selected)) return base;

        Locale locale = new Locale(selected);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return base.createConfigurationContext(configuration);
    }

    public static String activeLanguageCode(Context context) {
        String selected = currentPreference(context);
        if (!MainActivity.LANG_SYSTEM.equals(selected)) return selected;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        return Locale.getDefault().getLanguage();
    }
}
