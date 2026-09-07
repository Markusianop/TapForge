package com.tapforge;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/** TapForge motion + haptics. Motion is deliberately restrained and can be reduced in Settings. */
public final class Motion {
    private static final Interpolator EASE_OUT = new PathInterpolator(0.16f, 1f, 0.30f, 1f);
    private static final Interpolator STANDARD = new PathInterpolator(0.22f, 0.61f, 0.36f, 1f);
    private Motion() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
    }

    public static boolean reduced(Context c) {
        return prefs(c).getBoolean(MainActivity.KEY_REDUCE_MOTION, false);
    }

    public static boolean haptics(Context c) {
        return prefs(c).getBoolean(MainActivity.KEY_HAPTICS, true);
    }

    public static void enter(Activity a, View... views) {
        if (reduced(a)) {
            for (View v : views) if (v != null) { v.setAlpha(1f); v.setTranslationY(0f); v.setScaleX(1f); v.setScaleY(1f); }
            return;
        }
        float y = dp(a, 14);
        int visibleIndex = 0;
        for (View v : views) {
            if (v == null || v.getVisibility() != View.VISIBLE) continue;
            v.animate().cancel();
            v.setAlpha(0f); v.setTranslationY(y); v.setScaleX(.992f); v.setScaleY(.992f);
            v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(visibleIndex++ * 34L).setDuration(440L).setInterpolator(EASE_OUT).start();
        }
    }

    public static void reveal(Activity a, View v) {
        if (v == null) return;
        v.animate().cancel();
        v.setVisibility(View.VISIBLE);
        if (reduced(a)) { v.setAlpha(1f); v.setTranslationY(0f); v.setScaleX(1f); v.setScaleY(1f); return; }
        v.setAlpha(0f); v.setTranslationY(dp(a, 9)); v.setScaleX(.987f); v.setScaleY(.987f);
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(360L).setInterpolator(EASE_OUT).start();
    }

    public static void hide(View v) {
        if (v == null || v.getVisibility() != View.VISIBLE) return;
        v.animate().cancel();
        if (reduced(v.getContext())) { v.setVisibility(View.GONE); return; }
        v.animate().alpha(0f).translationY(-4f).scaleX(.992f).scaleY(.992f)
                .setDuration(145L).setInterpolator(STANDARD)
                .withEndAction(() -> {
                    v.setVisibility(View.GONE); v.setAlpha(1f); v.setTranslationY(0f); v.setScaleX(1f); v.setScaleY(1f);
                }).start();
    }

    public static void swap(Activity a, View oldV, View newV) {
        if (oldV == newV) return;
        if (reduced(a)) { if (oldV != null) oldV.setVisibility(View.GONE); if (newV != null) newV.setVisibility(View.VISIBLE); return; }
        hide(oldV);
        if (newV != null) newV.postDelayed(() -> reveal(a, newV), 80L);
    }

    public static void press(View v) {
        if (v == null) return;
        v.setOnTouchListener((x, e) -> {
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                tap(x);
                if (!reduced(x.getContext())) x.animate().scaleX(.976f).scaleY(.976f).setDuration(85L).setInterpolator(STANDARD).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                x.animate().scaleX(1f).scaleY(1f).setDuration(reduced(x.getContext()) ? 0L : 250L).setInterpolator(EASE_OUT).start();
            }
            return false;
        });
    }

    public static void pulse(View v, boolean enabled) {
        if (v == null) return;
        Object tag = v.getTag();
        if (tag instanceof ObjectAnimator) ((ObjectAnimator) tag).cancel();
        v.setTag(null); v.setAlpha(1f); v.setScaleX(1f); v.setScaleY(1f);
        if (!enabled || reduced(v.getContext())) return;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, View.ALPHA, 1f, .48f, 1f);
        a.setDuration(1900L); a.setRepeatCount(ValueAnimator.INFINITE); a.setInterpolator(STANDARD);
        v.setTag(a); a.start();
    }

    public static void breathe(View v, boolean enabled) {
        if (v == null) return;
        Object tag = v.getTag(R.id.motion_animator_tag);
        if (tag instanceof ObjectAnimator) ((ObjectAnimator) tag).cancel();
        v.setTag(R.id.motion_animator_tag, null); v.setScaleX(1f); v.setScaleY(1f); v.setAlpha(1f);
        if (!enabled || reduced(v.getContext())) return;
        PropertyValuesHolder sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.05f, 1f);
        PropertyValuesHolder sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.05f, 1f);
        PropertyValuesHolder al = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, .70f, 1f);
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v, sx, sy, al);
        a.setDuration(2300L); a.setRepeatCount(ValueAnimator.INFINITE); a.setInterpolator(STANDARD);
        v.setTag(R.id.motion_animator_tag, a); a.start();
    }

    /** Receive-screen brand motion: the glyph barely breathes while the halos do the visible expansion. */
    public static void receiveGlyph(View v, boolean enabled) {
        if (v == null) return;
        Object tag = v.getTag(R.id.motion_animator_tag);
        if (tag instanceof ObjectAnimator) ((ObjectAnimator) tag).cancel();
        v.setTag(R.id.motion_animator_tag, null);
        v.setScaleX(1f); v.setScaleY(1f); v.setAlpha(1f);
        if (!enabled || reduced(v.getContext())) return;
        PropertyValuesHolder sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.018f, 1f);
        PropertyValuesHolder sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.018f, 1f);
        PropertyValuesHolder al = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, .94f, 1f);
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v, sx, sy, al);
        a.setDuration(2650L);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(STANDARD);
        v.setTag(R.id.motion_animator_tag, a);
        a.start();
    }

    /**
     * Expanding receive halo. The outer ring is intentionally larger/slower.
     * Its parent stage has overscan room and clipChildren=false, so expansion never reveals a square crop.
     */
    public static void receiveHalo(View v, boolean enabled, boolean outer) {
        if (v == null) return;
        Object tag = v.getTag(R.id.motion_animator_tag);
        if (tag instanceof ObjectAnimator) ((ObjectAnimator) tag).cancel();
        v.setTag(R.id.motion_animator_tag, null);
        float baseAlpha = outer ? .32f : .72f;
        v.setScaleX(1f); v.setScaleY(1f); v.setAlpha(baseAlpha);
        if (!enabled || reduced(v.getContext())) return;

        float peak = outer ? 1.16f : 1.10f;
        float lowAlpha = outer ? .07f : .28f;
        PropertyValuesHolder sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, peak, 1f);
        PropertyValuesHolder sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, peak, 1f);
        PropertyValuesHolder al = PropertyValuesHolder.ofFloat(View.ALPHA, baseAlpha, lowAlpha, baseAlpha);
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v, sx, sy, al);
        a.setDuration(outer ? 3200L : 2700L);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setStartDelay(outer ? 180L : 0L);
        a.setInterpolator(STANDARD);
        v.setTag(R.id.motion_animator_tag, a);
        a.start();
    }

    public static void flash(View v) {
        if (v == null) return;
        v.animate().cancel();
        if (reduced(v.getContext())) return;
        v.setAlpha(.78f); v.setScaleX(.989f); v.setScaleY(.989f);
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(350L).setInterpolator(EASE_OUT).start();
    }

    public static void tap(View v) {
        if (v == null || !haptics(v.getContext())) return;
        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    public static void success(View v) {
        if (v == null || !haptics(v.getContext())) return;
        int constant = Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.KEYBOARD_TAP;
        v.performHapticFeedback(constant);
    }

    public static void error(View v) {
        if (v == null || !haptics(v.getContext())) return;
        int constant = Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS;
        v.performHapticFeedback(constant);
    }

    private static float dp(Activity a, int n) { return n * a.getResources().getDisplayMetrics().density; }
}
