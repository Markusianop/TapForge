package com.tapforge;

import android.app.Activity;
import android.content.Intent;

public final class Navigation {
    private Navigation() { }

    public static void push(Activity activity, Intent intent) {
        activity.startActivity(intent);
        if (!Motion.reduced(activity)) activity.overridePendingTransition(R.anim.tf_enter, R.anim.tf_exit);
    }

    public static void pushTools(Activity activity, Intent intent) {
        activity.startActivity(intent);
        if (!Motion.reduced(activity)) {
            activity.overridePendingTransition(R.anim.tf_tools_enter, R.anim.tf_tools_under_exit);
        } else {
            activity.overridePendingTransition(0, 0);
        }
    }

    public static void pop(Activity activity) {
        activity.finish();
        if (!Motion.reduced(activity)) activity.overridePendingTransition(R.anim.tf_pop_enter, R.anim.tf_pop_exit);
    }

    public static void replace(Activity activity, Intent intent) {
        activity.startActivity(intent);
        activity.finish();
        if (!Motion.reduced(activity)) activity.overridePendingTransition(R.anim.tf_enter, R.anim.tf_exit);
    }
}
