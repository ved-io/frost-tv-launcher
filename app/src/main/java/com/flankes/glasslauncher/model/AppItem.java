package com.flankes.glasslauncher.model;

import android.graphics.drawable.Drawable;

public class AppItem {
    public final String packageName;
    public final String activityName;
    public final String label;
    public final Drawable icon;

    public AppItem(String packageName, String activityName, String label, Drawable icon) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.label = label;
        this.icon = icon;
    }
}
