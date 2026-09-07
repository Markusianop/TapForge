package com.tapforge;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

public final class ThemeHelper {
    private ThemeHelper() { }

    public static String normalizedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString(MainActivity.KEY_THEME, MainActivity.THEME_SYSTEM);
        if ("oled".equals(value)) {
            value = MainActivity.THEME_DARK;
            prefs.edit().putString(MainActivity.KEY_THEME, value).apply();
        }
        return value == null ? MainActivity.THEME_SYSTEM : value;
    }


    public static boolean isDark(Context context) {
        String theme = normalizedTheme(context);
        if (MainActivity.THEME_DARK.equals(theme)) return true;
        if (MainActivity.THEME_LIGHT.equals(theme)) return false;
        int night = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void apply(Activity activity) {
        boolean dark = isDark(activity);
        activity.setTheme(dark ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
    }
}
