package com.tapforge;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public final class UiAdaptation {
    private UiAdaptation() { }

    public static void apply(Activity activity, View root) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        boolean dark = ThemeHelper.isDark(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int appearance = dark ? 0 : (WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                controller.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int flags = window.getDecorView().getSystemUiVisibility();
            if (!dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            else flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            window.getDecorView().setSystemUiVisibility(flags);
        }

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = 0, bottom = 0, left = 0, right = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets safe = insets.getInsets(
                        WindowInsets.Type.statusBars() |
                        WindowInsets.Type.navigationBars() |
                        WindowInsets.Type.displayCutout());
                top = safe.top;
                bottom = safe.bottom;
                left = safe.left;
                right = safe.right;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft();
                right = insets.getSystemWindowInsetRight();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    DisplayCutout cutout = insets.getDisplayCutout();
                    if (cutout != null) {
                        top = Math.max(top, cutout.getSafeInsetTop());
                        bottom = Math.max(bottom, cutout.getSafeInsetBottom());
                        left = Math.max(left, cutout.getSafeInsetLeft());
                        right = Math.max(right, cutout.getSafeInsetRight());
                    }
                }
            }
            v.setPadding(left, top + topBreathing(activity), right, bottom + dp(activity, 16));
            return insets;
        });
        root.requestApplyInsets();
    }

    public static void applyAdaptiveWidth(Activity activity, LinearLayout container) {
        int widthDp = activity.getResources().getConfiguration().screenWidthDp;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) container.getLayoutParams();
        if (widthDp >= 600) {
            int gutter = widthDp >= 840 ? 64 : 48;
            int cap = widthDp >= 840 ? 760 : 640;
            int target = Math.min(cap, Math.max(1, widthDp - gutter));
            lp.width = dp(activity, target);
            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        } else {
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
            lp.gravity = android.view.Gravity.TOP;
        }
        container.setLayoutParams(lp);
        applyContentRhythm(activity, container);
    }

    private static void applyContentRhythm(Activity activity, LinearLayout container) {
        Configuration c = activity.getResources().getConfiguration();
        int width = c.screenWidthDp;
        int height = c.screenHeightDp;
        int side = width < 350 ? 16 : (width >= 600 ? 24 : 20);
        int top = height < 600 ? 4 : 8;
        container.setPadding(dp(activity, side), dp(activity, top), dp(activity, side), dp(activity, 34));
    }

    private static int topBreathing(Activity activity) {
        Configuration c = activity.getResources().getConfiguration();
        if (c.orientation == Configuration.ORIENTATION_LANDSCAPE) return dp(activity, 8);
        float h = c.screenHeightDp;
        float w = Math.max(1, c.screenWidthDp);
        if (w >= 600f) return dp(activity, 18);
        // Continuous spacing avoids abrupt jumps between phones with similar aspect ratios.
        float extra = 14f + Math.max(0f, h - 600f) * 0.045f;
        float ratio = h / w;
        if (ratio > 2.05f) extra += Math.min(6f, (ratio - 2.05f) * 18f);
        extra = Math.max(14f, Math.min(34f, extra));
        return dp(activity, Math.round(extra));
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
