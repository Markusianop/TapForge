package com.tapforge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public class OnboardingActivity extends Activity {
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleHelper.wrap(base)); }

    @Override protected void onCreate(Bundle state) {
        ThemeHelper.apply(this);
        super.onCreate(state);
        setContentView(R.layout.activity_onboarding);
        UiAdaptation.apply(this, findViewById(R.id.onboardingRoot));
        LinearLayout content = findViewById(R.id.onboardingContent);
        UiAdaptation.applyAdaptiveWidth(this, content);

        Button start = findViewById(R.id.onboardingStart);
        start.setOnClickListener(v -> finishOnboarding());
        Motion.press(start);
        Motion.enter(this,
                findViewById(R.id.onboardingBrand),
                findViewById(R.id.onboardingTitle),
                findViewById(R.id.onboardingSubtitle),
                findViewById(R.id.featureSend),
                findViewById(R.id.featureReceive),
                findViewById(R.id.featurePrivacy),
                start);
    }

    @Override public void onBackPressed() {
        Navigation.pop(this);
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(MainActivity.KEY_ONBOARDING_DONE, true).apply();
        Motion.success(findViewById(R.id.onboardingStart));
        Navigation.replace(this, new Intent(this, MainActivity.class));
    }
}
