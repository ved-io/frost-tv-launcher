package com.flankes.glasslauncher.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import com.flankes.glasslauncher.model.AppItem;
import java.util.*;

public class AppListRepository {
    private static final String PREF_NAME = "glass_launcher_prefs";
    private static final String PREF_DOCK_ORDER = "dock_packages_order";

    public static final List<String> DEFAULT_DOCK_APPS = Arrays.asList(
        "com.github.damontecres.wholphin",
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.disney.disneyplus",
        "com.amazon.amazonvideo.livingroom",
        "com.wbd.stream",
        "com.seraphic.openinet.pre",
        "de.rtli.tvnow",
        "de.prosiebensat1digital.seventv"
    );

    private final Context context;
    private final SharedPreferences prefs;

    public AppListRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<String> getSavedDockPackages() {
        String saved = prefs.getString(PREF_DOCK_ORDER, null);
        if (saved != null && !saved.trim().isEmpty()) {
            String[] parts = saved.split(",");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty()) list.add(clean);
            }
            if (!list.isEmpty()) return list;
        }
        return new ArrayList<>(DEFAULT_DOCK_APPS);
    }

    public void saveDockPackages(List<String> packages) {
        if (packages == null || packages.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packages.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(packages.get(i));
        }
        prefs.edit().putString(PREF_DOCK_ORDER, sb.toString()).apply();
    }

    public List<AppItem> getDockApps() {
        PackageManager pm = context.getPackageManager();
        List<AppItem> dock = new ArrayList<>();
        List<String> dockPackages = getSavedDockPackages();

        for (String pkg : dockPackages) {
            String target = normalizePackage(pm, pkg);
            AppItem item = resolveAppItem(pm, target);
            if (item != null) {
                dock.add(item);
            }
        }

        return dock;
    }

    public List<AppItem> getAllApps() {
        PackageManager pm = context.getPackageManager();
        Map<String, AppItem> map = new LinkedHashMap<>();

        // 1. Query Leanback launcher apps
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN, null);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> leanbackApps = pm.queryIntentActivities(leanbackIntent, 0);
        for (ResolveInfo ri : leanbackApps) {
            if (ri.activityInfo != null && !ri.activityInfo.packageName.equals(context.getPackageName())) {
                String pkg = ri.activityInfo.packageName;
                String act = ri.activityInfo.name;
                String label = ri.loadLabel(pm).toString();
                map.put(pkg, new AppItem(pkg, act, label, ri.loadIcon(pm)));
            }
        }

        // 2. Query Standard launcher apps
        Intent standardIntent = new Intent(Intent.ACTION_MAIN, null);
        standardIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> standardApps = pm.queryIntentActivities(standardIntent, 0);
        for (ResolveInfo ri : standardApps) {
            if (ri.activityInfo != null && !ri.activityInfo.packageName.equals(context.getPackageName())) {
                String pkg = ri.activityInfo.packageName;
                if (!map.containsKey(pkg)) {
                    String act = ri.activityInfo.name;
                    String label = ri.loadLabel(pm).toString();
                    map.put(pkg, new AppItem(pkg, act, label, ri.loadIcon(pm)));
                }
            }
        }

        List<AppItem> result = new ArrayList<>();

        // Row 1: The dock apps in their exact current home order
        List<String> dockPackages = getSavedDockPackages();
        for (String pkg : dockPackages) {
            String target = normalizePackage(pm, pkg);
            if (map.containsKey(target)) {
                result.add(map.remove(target));
            } else {
                AppItem item = resolveAppItem(pm, target);
                if (item != null) {
                    result.add(item);
                    map.remove(item.packageName);
                }
            }
        }

        // Subsequent rows: All other installed apps sorted alphabetically
        List<AppItem> remaining = new ArrayList<>(map.values());
        Collections.sort(remaining, (a, b) -> a.label.compareToIgnoreCase(b.label));
        result.addAll(remaining);

        return result;
    }

    public List<AppItem> getRemainingApps() {
        PackageManager pm = context.getPackageManager();
        Map<String, AppItem> map = new LinkedHashMap<>();

        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN, null);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> leanbackApps = pm.queryIntentActivities(leanbackIntent, 0);
        for (ResolveInfo ri : leanbackApps) {
            if (ri.activityInfo != null && !ri.activityInfo.packageName.equals(context.getPackageName())) {
                String pkg = ri.activityInfo.packageName;
                String act = ri.activityInfo.name;
                String label = ri.loadLabel(pm).toString();
                map.put(pkg, new AppItem(pkg, act, label, ri.loadIcon(pm)));
            }
        }

        Intent standardIntent = new Intent(Intent.ACTION_MAIN, null);
        standardIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> standardApps = pm.queryIntentActivities(standardIntent, 0);
        for (ResolveInfo ri : standardApps) {
            if (ri.activityInfo != null && !ri.activityInfo.packageName.equals(context.getPackageName())) {
                String pkg = ri.activityInfo.packageName;
                if (!map.containsKey(pkg)) {
                    String act = ri.activityInfo.name;
                    String label = ri.loadLabel(pm).toString();
                    map.put(pkg, new AppItem(pkg, act, label, ri.loadIcon(pm)));
                }
            }
        }

        List<String> dockPackages = getSavedDockPackages();
        for (String pkg : dockPackages) {
            String target = normalizePackage(pm, pkg);
            map.remove(target);
            map.remove(pkg);
        }

        List<AppItem> remaining = new ArrayList<>(map.values());
        Collections.sort(remaining, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return remaining;
    }

    private String normalizePackage(PackageManager pm, String pkg) {
        if ("com.seraphic.openinet.pre".equals(pkg)) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (Exception e) {
                return "com.seraphic.openinet";
            }
        } else if ("com.google.android.youtube.tv".equals(pkg)) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (Exception e) {
                return "org.smarttube.stable";
            }
        }
        return pkg;
    }

    public AppItem resolveAppItem(PackageManager pm, String pkg) {
        // 1. Try Leanback Intent
        Intent leanback = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (leanback != null) {
            ResolveInfo ri = pm.resolveActivity(leanback, 0);
            if (ri != null) {
                String act = ri.activityInfo != null ? ri.activityInfo.name : "";
                String label = ri.loadLabel(pm).toString();
                return new AppItem(pkg, act, label, ri.loadIcon(pm));
            }
        }

        // 2. Try Standard Launch Intent
        Intent launch = pm.getLaunchIntentForPackage(pkg);
        if (launch != null) {
            ResolveInfo ri = pm.resolveActivity(launch, 0);
            if (ri != null) {
                String act = ri.activityInfo != null ? ri.activityInfo.name : "";
                String label = ri.loadLabel(pm).toString();
                return new AppItem(pkg, act, label, ri.loadIcon(pm));
            }
        }

        // 3. Query activities directly
        try {
            Intent query = new Intent(Intent.ACTION_MAIN, null);
            query.setPackage(pkg);
            query.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(query, 0);
            if (list.isEmpty()) {
                query = new Intent(Intent.ACTION_MAIN, null);
                query.setPackage(pkg);
                query.addCategory(Intent.CATEGORY_LAUNCHER);
                list = pm.queryIntentActivities(query, 0);
            }
            if (!list.isEmpty()) {
                ResolveInfo ri = list.get(0);
                String act = ri.activityInfo != null ? ri.activityInfo.name : "";
                String label = ri.loadLabel(pm).toString();
                return new AppItem(pkg, act, label, ri.loadIcon(pm));
            }
        } catch (Exception ignored) {}

        // 4. ApplicationInfo fallback
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(ai);
            Drawable icon = pm.getApplicationIcon(ai);
            return new AppItem(pkg, "", label != null ? label.toString() : pkg, icon);
        } catch (Exception ignored) {}

        return null;
    }
}
