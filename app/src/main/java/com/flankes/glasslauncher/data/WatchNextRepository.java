package com.flankes.glasslauncher.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import androidx.tvprovider.media.tv.TvContractCompat;
import com.flankes.glasslauncher.model.WatchNextItem;
import java.util.*;

public class WatchNextRepository {
    private static final String TAG = "WatchNextRepository";

    private static final Set<String> BLOCKED_PACKAGES = new HashSet<>(Arrays.asList(
        "com.seraphic.openinet.pre",
        "com.seraphic.openinet",
        "com.dazn"
    ));

    private final Context context;
    private final List<WatchNextItem> allRealPrograms = new ArrayList<>();
    private long lastFetchTime = 0;

    public WatchNextRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized List<WatchNextItem> getShowsForPackage(String targetPkg) {
        long now = System.currentTimeMillis();
        if (allRealPrograms.isEmpty() || (now - lastFetchTime > 15000)) {
            refresh();
        }

        if (targetPkg == null || targetPkg.isEmpty()) {
            return Collections.emptyList();
        }

        String filter = targetPkg;
        if ("org.smarttube.stable".equals(filter) || filter.contains("smarttube")) {
            filter = "com.google.android.youtube.tv";
        }

        List<WatchNextItem> matchingShows = new ArrayList<>();
        for (WatchNextItem item : allRealPrograms) {
            String p = item.packageName;
            if ("org.smarttube.stable".equals(p) || (p != null && p.contains("smarttube"))) {
                p = "com.google.android.youtube.tv";
            }
            if (filter.equals(p)) {
                matchingShows.add(item);
            }
        }

        // Return ONLY real programs published by this app.
        // If the app has no active continue-watching programs, return empty list so the shelf hides.
        return matchingShows;
    }

    public synchronized List<WatchNextItem> getAllRealPrograms() {
        long now = System.currentTimeMillis();
        if (allRealPrograms.isEmpty() || (now - lastFetchTime > 15000)) {
            refresh();
        }
        return new ArrayList<>(allRealPrograms);
    }

    public synchronized void refresh() {
        List<ProgramRecord> records = new ArrayList<>();

        // SOURCE 1: Official TvContractCompat.WatchNextPrograms (content://android.media.tv/watch_next_program)
        try {
            readFromUri(TvContractCompat.WatchNextPrograms.CONTENT_URI, records, true);
        } catch (Exception e) {
            Log.e(TAG, "Error querying WatchNextPrograms", e);
        }

        // SOURCE 2: Dedicated "Continue Watching" channels published by apps (e.g. Stremio, etc.)
        try {
            List<Long> continueChannelIds = queryContinueChannels();
            for (Long channelId : continueChannelIds) {
                Uri channelUri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId);
                readFromUri(channelUri, records, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying Continue Channels", e);
        }

        // Sort by last engagement time (newest first)
        Collections.sort(records, (a, b) -> Long.compare(b.engagementTime, a.engagementTime));

        // Deduplicate by intentUri or title
        Map<String, WatchNextItem> dedupMap = new LinkedHashMap<>();
        for (ProgramRecord rec : records) {
            String key = (rec.item.intentUri != null && !rec.item.intentUri.isEmpty())
                ? rec.item.intentUri
                : (rec.item.packageName + "/" + rec.item.title);

            if (!dedupMap.containsKey(key)) {
                dedupMap.put(key, rec.item);
            }
        }

        allRealPrograms.clear();
        allRealPrograms.addAll(dedupMap.values());
        lastFetchTime = System.currentTimeMillis();
        Log.i(TAG, "WatchNext refresh completed. Total real programs found: " + allRealPrograms.size());
        for (int i = 0; i < Math.min(10, allRealPrograms.size()); i++) {
            WatchNextItem it = allRealPrograms.get(i);
            Log.i(TAG, "  -> Program: " + it.packageName + " | " + it.title);
        }
    }

    private List<Long> queryContinueChannels() {
        List<Long> ids = new ArrayList<>();
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                TvContractCompat.Channels.CONTENT_URI,
                new String[]{TvContractCompat.Channels._ID, TvContractCompat.Channels.COLUMN_DISPLAY_NAME, TvContractCompat.Channels.COLUMN_PACKAGE_NAME},
                null,
                null,
                null
            );
            if (c != null) {
                int idCol = c.getColumnIndex(TvContractCompat.Channels._ID);
                int nameCol = c.getColumnIndex(TvContractCompat.Channels.COLUMN_DISPLAY_NAME);
                int pkgCol = c.getColumnIndex(TvContractCompat.Channels.COLUMN_PACKAGE_NAME);

                while (c.moveToNext()) {
                    String name = nameCol >= 0 ? c.getString(nameCol) : "";
                    String pkg = pkgCol >= 0 ? c.getString(pkgCol) : "";
                    long id = idCol >= 0 ? c.getLong(idCol) : -1;

                    if (name != null && !context.getPackageName().equals(pkg) && id >= 0) {
                        String lower = name.toLowerCase(Locale.ROOT);
                        if (lower.contains("continue") || lower.contains("weiterschauen") || lower.contains("fortsetzen") || lower.contains("next")) {
                            ids.add(id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying Channels", e);
        } finally {
            if (c != null) c.close();
        }
        return ids;
    }

    private void readFromUri(Uri uri, List<ProgramRecord> records, boolean isWatchNext) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                uri,
                null,
                null,
                null,
                isWatchNext ? "last_engagement_time_utc_millis DESC" : null
            );

            if (cursor == null) return;

            int pkgCol = cursor.getColumnIndex("package_name");
            int browCol = cursor.getColumnIndex("browsable");
            int idCol = cursor.getColumnIndex("_id");
            int titleCol = cursor.getColumnIndex("title");
            int epTitleCol = cursor.getColumnIndex("episode_title");
            int sCol = cursor.getColumnIndex("season_display_number");
            int eCol = cursor.getColumnIndex("episode_display_number");
            int tCol = cursor.getColumnIndex("thumbnail_uri");
            int pCol = cursor.getColumnIndex("poster_art_uri");
            int iCol = cursor.getColumnIndex("intent_uri");
            int posCol = cursor.getColumnIndex("last_playback_position_millis");
            int durCol = cursor.getColumnIndex("duration_millis");
            int engCol = isWatchNext ? cursor.getColumnIndex("last_engagement_time_utc_millis") : -1;

            while (cursor.moveToNext()) {
                String pkg = (pkgCol >= 0 && !cursor.isNull(pkgCol)) ? cursor.getString(pkgCol) : "";
                if (pkg == null || pkg.isEmpty() || BLOCKED_PACKAGES.contains(pkg)) {
                    continue;
                }

                if (browCol >= 0 && !cursor.isNull(browCol)) {
                    if (cursor.getInt(browCol) == 0) continue;
                }

                String title = (titleCol >= 0 && !cursor.isNull(titleCol)) ? cursor.getString(titleCol) : "";
                if (title == null || title.isEmpty()) continue;

                long id = idCol >= 0 ? cursor.getLong(idCol) : 0;
                String epTitle = (epTitleCol >= 0 && !cursor.isNull(epTitleCol)) ? cursor.getString(epTitleCol) : "";
                String season = (sCol >= 0 && !cursor.isNull(sCol)) ? cursor.getString(sCol) : "";
                String episode = (eCol >= 0 && !cursor.isNull(eCol)) ? cursor.getString(eCol) : "";

                String sub = "";
                if (!season.isEmpty() && !episode.isEmpty()) {
                    sub = "S" + season + " E" + episode;
                }
                if (!epTitle.isEmpty()) {
                    sub = sub.isEmpty() ? epTitle : sub + " • " + epTitle;
                }

                String poster = (tCol >= 0 && !cursor.isNull(tCol)) ? cursor.getString(tCol) : "";
                if (poster == null || poster.isEmpty()) {
                    if (pCol >= 0 && !cursor.isNull(pCol)) poster = cursor.getString(pCol);
                }

                String intentUri = (iCol >= 0 && !cursor.isNull(iCol)) ? cursor.getString(iCol) : "";

                double progress = 0.0;
                if (posCol >= 0 && durCol >= 0 && !cursor.isNull(posCol) && !cursor.isNull(durCol)) {
                    long pos = cursor.getLong(posCol);
                    long dur = cursor.getLong(durCol);
                    if (dur > 0 && pos > 0 && pos < dur) {
                        double frac = (double) pos / (double) dur;
                        if (frac > 0.01 && frac < 0.99) {
                            progress = frac;
                        }
                    }
                }

                long engagement = 0;
                if (engCol >= 0 && !cursor.isNull(engCol)) {
                    engagement = cursor.getLong(engCol);
                }

                WatchNextItem item = new WatchNextItem(id, pkg, title, sub, poster, intentUri, progress);
                records.add(new ProgramRecord(engagement, item));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading programs from URI: " + uri, e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static class ProgramRecord {
        final long engagementTime;
        final WatchNextItem item;

        ProgramRecord(long engagementTime, WatchNextItem item) {
            this.engagementTime = engagementTime;
            this.item = item;
        }
    }
}
