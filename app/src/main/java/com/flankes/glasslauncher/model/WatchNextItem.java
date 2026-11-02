package com.flankes.glasslauncher.model;

public class WatchNextItem {
    public final long id;
    public final String packageName;
    public final String title;
    public final String subtitle;
    public final String posterUri;
    public final String intentUri;
    public final double progress; // between 0.0 and 1.0

    public WatchNextItem(long id, String packageName, String title, String subtitle, String posterUri, String intentUri, double progress) {
        this.id = id;
        this.packageName = packageName;
        this.title = title;
        this.subtitle = subtitle;
        this.posterUri = posterUri;
        this.intentUri = intentUri;
        this.progress = progress;
    }
}
