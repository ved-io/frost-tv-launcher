package com.flankes.glasslauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.flankes.glasslauncher.data.AppListRepository;
import com.flankes.glasslauncher.data.WatchNextRepository;
import com.flankes.glasslauncher.data.WeatherService;
import com.flankes.glasslauncher.model.AppItem;
import com.flankes.glasslauncher.model.WatchNextItem;
import com.flankes.glasslauncher.ui.DockAdapter;
import com.flankes.glasslauncher.ui.DrawerDockAdapter;
import com.flankes.glasslauncher.ui.DrawerRemainingAdapter;
import com.flankes.glasslauncher.ui.FrostedGlassDrawable;
import com.flankes.glasslauncher.ui.GlassClockView;
import com.flankes.glasslauncher.ui.ShelfAdapter;
import com.flankes.glasslauncher.util.BlurManager;
import com.flankes.glasslauncher.util.Localization;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private ImageView wallpaperBg;
    private ImageView wallpaperFg;
    private GlassClockView clockText;
    private TextView dateWeatherText;
    private View settingsButton;
    private FrameLayout shelfContainer;
    private RecyclerView shelfRecycler;
    private FrameLayout dockContainer;
    private RecyclerView dockRecycler;
    private FrameLayout drawerContainer;
    private RecyclerView drawerDockRecycler;
    private RecyclerView drawerRemainingRecycler;

    // Gear Popup Dropdown
    private LinearLayout gearPopup;
    private TextView btnPopupLauncherSettings;
    private TextView btnPopupSystemSettings;

    // In-Place Settings Panel
    private LinearLayout settingsPanelContainer;
    private TextView tvSidebarHeader;
    private TextView tabCategoryLang;
    private TextView tabCategoryWallpaper;
    private TextView tabCategoryWeather;
    private TextView tabCategoryAbout;
    private TextView btnCloseSettings;
    private int currentSettingsTab = 0;

    private View panelViewLanguage;
    private View panelViewWallpaper;
    private View panelViewWeather;
    private View panelViewAbout;

    // Language views
    private TextView tvLangTitle, tvLangDesc;
    private TextView btnLangDe, btnLangEn, btnLangEs, btnLangFr, btnLangPt, btnLangIt;
    private TextView tvLangStatus;

    // Wallpaper views
    private TextView tvWpTitle, tvWpDepthLabel, tvWpSourceLabel;
    private TextView btnDepthOn, btnDepthOff;
    private TextView btnWpDefault, btnWpCustomReload, btnWpOpenFiles;
    private TextView tvWpStatus;

    // Weather views
    private TextView tvWeatherTitle, tvWeatherToggleLabel, tvWeatherUnitLabel, tvWeatherIntervalLabel;
    private TextView btnWeatherOn, btnWeatherOff;
    private TextView btnUnitC, btnUnitF;
    private TextView btnInterval15, btnInterval30, btnInterval60;
    private TextView tvCurrentCityLabel;
    private TextView tvWeatherSearchLabel;
    private EditText etSearchCity;
    private TextView btnSearchCity;
    private TextView tvSearchStatus;
    private LinearLayout cityResultsContainer;

    // About views
    private TextView tvAboutTitle, tvAboutVersion, tvAboutArch, tvAboutDesc;

    private WatchNextRepository watchNextRepo;
    private AppListRepository appListRepo;
    private WeatherService weatherService;

    private ShelfAdapter shelfAdapter;
    private DockAdapter dockAdapter;
    private DrawerDockAdapter drawerDockAdapter;
    private DrawerRemainingAdapter drawerRemainingAdapter;

    private int currentShelfPos = 0;

    private final Handler keyHandler = new Handler(Looper.getMainLooper());
    private Runnable holdRunnable = null;
    private boolean holdFired = false;

    private SharedPreferences prefs;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private final Handler weatherHandler = new Handler(Looper.getMainLooper());
    private String weatherSummary = "";

    private SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", Locale.GERMANY);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMMM", Locale.GERMANY);

    private final Runnable timeTicker = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            clockText.setText(clockFormat.format(now));
            updateDateWeather(now);
            timeHandler.postDelayed(this, 1000);
        }
    };

    private final Runnable weatherTicker = new Runnable() {
        @Override
        public void run() {
            refreshWeather();
            int intervalMins = prefs != null ? prefs.getInt("weather_interval_minutes", 30) : 30;
            weatherHandler.postDelayed(this, intervalMins * 60L * 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("glass_launcher_prefs", Context.MODE_PRIVATE);
        watchNextRepo = new WatchNextRepository(this);
        appListRepo = new AppListRepository(this);
        weatherService = new WeatherService(this);

        String lang = prefs.getString("app_language", "de");
        applyLanguageLocale(lang);

        initViews();

        if (checkSelfPermission("android.permission.READ_TV_LISTINGS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.READ_TV_LISTINGS"}, 101);
        }

        Date now = new Date();
        clockText.setText(clockFormat.format(now));
        updateDateWeather(now);

        setupWallpaper();
        setupDock();
        setupShelf();
        setupDrawer();
        setupSettings();

        refreshWeather();
        startWeatherTimer();
    }

    private void initViews() {
        wallpaperBg = findViewById(R.id.wallpaper_bg);
        wallpaperFg = findViewById(R.id.wallpaper_fg);
        clockText = findViewById(R.id.clock_text);
        dateWeatherText = findViewById(R.id.date_weather_text);
        settingsButton = findViewById(R.id.settings_button);
        shelfContainer = findViewById(R.id.shelf_container);
        shelfRecycler = findViewById(R.id.shelf_recycler);
        dockContainer = findViewById(R.id.dock_container);
        dockRecycler = findViewById(R.id.dock_recycler);
        drawerContainer = findViewById(R.id.drawer_container);
        drawerDockRecycler = findViewById(R.id.drawer_dock_recycler);
        drawerRemainingRecycler = findViewById(R.id.drawer_remaining_recycler);

        // Gear Popup
        gearPopup = findViewById(R.id.gear_popup);
        btnPopupLauncherSettings = findViewById(R.id.btn_popup_launcher_settings);
        btnPopupSystemSettings = findViewById(R.id.btn_popup_system_settings);

        // In-Place Settings Panel
        settingsPanelContainer = findViewById(R.id.settings_panel_container);
        tvSidebarHeader = findViewById(R.id.tv_sidebar_header);
        tabCategoryLang = findViewById(R.id.tab_category_lang);
        tabCategoryWallpaper = findViewById(R.id.tab_category_wallpaper);
        tabCategoryWeather = findViewById(R.id.tab_category_weather);
        tabCategoryAbout = findViewById(R.id.tab_category_about);
        btnCloseSettings = findViewById(R.id.btn_close_settings);

        panelViewLanguage = findViewById(R.id.panel_view_language);
        panelViewWallpaper = findViewById(R.id.panel_view_wallpaper);
        panelViewWeather = findViewById(R.id.panel_view_weather);
        panelViewAbout = findViewById(R.id.panel_view_about);

        // Language views
        tvLangTitle = findViewById(R.id.tv_lang_title);
        tvLangDesc = findViewById(R.id.tv_lang_desc);
        btnLangDe = findViewById(R.id.btn_lang_de);
        btnLangEn = findViewById(R.id.btn_lang_en);
        btnLangEs = findViewById(R.id.btn_lang_es);
        btnLangFr = findViewById(R.id.btn_lang_fr);
        btnLangPt = findViewById(R.id.btn_lang_pt);
        btnLangIt = findViewById(R.id.btn_lang_it);
        tvLangStatus = findViewById(R.id.tv_lang_status);

        // Wallpaper views
        tvWpTitle = findViewById(R.id.tv_wp_title);
        tvWpDepthLabel = findViewById(R.id.tv_wp_depth_label);
        btnDepthOn = findViewById(R.id.btn_depth_on);
        btnDepthOff = findViewById(R.id.btn_depth_off);
        tvWpSourceLabel = findViewById(R.id.tv_wp_source_label);
        btnWpDefault = findViewById(R.id.btn_wp_default);
        btnWpCustomReload = findViewById(R.id.btn_wp_custom_reload);
        btnWpOpenFiles = findViewById(R.id.btn_wp_open_files);
        tvWpStatus = findViewById(R.id.tv_wp_status);

        // Weather views
        tvWeatherTitle = findViewById(R.id.tv_weather_title);
        tvWeatherToggleLabel = findViewById(R.id.tv_weather_toggle_label);
        btnWeatherOn = findViewById(R.id.btn_weather_on);
        btnWeatherOff = findViewById(R.id.btn_weather_off);
        tvWeatherUnitLabel = findViewById(R.id.tv_weather_unit_label);
        btnUnitC = findViewById(R.id.btn_unit_c);
        btnUnitF = findViewById(R.id.btn_unit_f);
        tvWeatherIntervalLabel = findViewById(R.id.tv_weather_interval_label);
        btnInterval15 = findViewById(R.id.btn_interval_15);
        btnInterval30 = findViewById(R.id.btn_interval_30);
        btnInterval60 = findViewById(R.id.btn_interval_60);
        tvCurrentCityLabel = findViewById(R.id.tv_current_city_label);
        tvWeatherSearchLabel = findViewById(R.id.tv_weather_search_label);
        etSearchCity = findViewById(R.id.et_search_city);
        btnSearchCity = findViewById(R.id.btn_search_city);
        tvSearchStatus = findViewById(R.id.tv_search_status);
        cityResultsContainer = findViewById(R.id.city_results_container);

        // About views
        tvAboutTitle = findViewById(R.id.tv_about_title);
        tvAboutVersion = findViewById(R.id.tv_about_version);
        tvAboutArch = findViewById(R.id.tv_about_arch);
        tvAboutDesc = findViewById(R.id.tv_about_desc);

        float density = getResources().getDisplayMetrics().density;
        dateWeatherText.setShadowLayer(10f * density, 0, 2f * density, 0x99000000);
        dockContainer.setBackground(new FrostedGlassDrawable(dockContainer, 24f * density, 0x551E2433, 1.0f * density, 0x48DCE6F2));
        drawerContainer.setBackground(new FrostedGlassDrawable(drawerContainer, 20f * density, 0x88161C28, 1.0f * density, 0x48DCE6F2));
        gearPopup.setBackground(new FrostedGlassDrawable(gearPopup, 16f * density, 0x88161B28, 1.0f * density, 0x48DCE6F2));
        settingsPanelContainer.setBackground(new FrostedGlassDrawable(settingsPanelContainer, 22f * density, 0x88161B28, 1.0f * density, 0x48DCE6F2));

        File customDir = new File(Environment.getExternalStorageDirectory(), "Wallpapers");
        if (!customDir.exists()) {
            customDir.mkdirs();
        }
    }

    private void syncShelfWidth() {
        int w = dockContainer.getWidth();
        if (w > 0) {
            ViewGroup.LayoutParams lp = shelfContainer.getLayoutParams();
            if (lp != null && lp.width != w) {
                lp.width = w;
                shelfContainer.setLayoutParams(lp);
            }
        }
    }

    private void applyLanguageLocale(String langCode) {
        Locale loc;
        String pattern;
        switch (langCode.toLowerCase()) {
            case "en":
                loc = Locale.US;
                pattern = "EEEE, MMMM d";
                break;
            case "es":
                loc = new Locale("es");
                pattern = "EEEE, d 'de' MMMM";
                break;
            case "fr":
                loc = Locale.FRANCE;
                pattern = "EEEE d MMMM";
                break;
            case "pt":
                loc = new Locale("pt");
                pattern = "EEEE, d 'de' MMMM";
                break;
            case "it":
                loc = Locale.ITALY;
                pattern = "EEEE d MMMM";
                break;
            case "de":
            default:
                loc = Locale.GERMANY;
                pattern = "EEEE, d. MMMM";
                break;
        }
        clockFormat = new SimpleDateFormat("HH:mm", loc);
        dateFormat = new SimpleDateFormat(pattern, loc);
    }

    private void updateDateWeather(Date date) {
        String dateStr = dateFormat.format(date);
        if (dateStr.length() > 0) {
            dateStr = Character.toUpperCase(dateStr.charAt(0)) + dateStr.substring(1);
        }

        if (weatherSummary != null && !weatherSummary.isEmpty()) {
            dateWeatherText.setText(dateStr + "  •  " + weatherSummary);
        } else {
            dateWeatherText.setText(dateStr);
        }
    }

    private void refreshWeather() {
        weatherService.fetchWeather(summary -> {
            weatherSummary = summary;
            updateDateWeather(new Date());
        });
    }

    private void startWeatherTimer() {
        weatherHandler.removeCallbacks(weatherTicker);
        int intervalMins = prefs != null ? prefs.getInt("weather_interval_minutes", 30) : 30;
        weatherHandler.postDelayed(weatherTicker, intervalMins * 60L * 1000L);
    }

    private void setupWallpaper() {
        String scene = prefs.getString("selected_scene", "custom");
        boolean depthEnabled = prefs.getBoolean("depth_effect_enabled", true);

        File customDir = new File(Environment.getExternalStorageDirectory(), "Wallpapers");
        File customBgJpg = new File(customDir, "wallpaper.jpg");
        File customBgPng = new File(customDir, "wallpaper.png");
        File customFg = new File(customDir, "wallpaper_fg.png");

        File customBg = customBgJpg.exists() ? customBgJpg : (customBgPng.exists() ? customBgPng : null);

        if ("custom".equals(scene) && customBg != null) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(customBg.getAbsolutePath());
                wallpaperBg.setImageBitmap(bmp);
                BlurManager.setWallpaper(bmp);
                if (depthEnabled && customFg.exists()) {
                    Bitmap fgBmp = BitmapFactory.decodeFile(customFg.getAbsolutePath());
                    wallpaperFg.setImageBitmap(fgBmp);
                    wallpaperFg.setVisibility(View.VISIBLE);
                } else {
                    wallpaperFg.setVisibility(View.GONE);
                }
                return;
            } catch (Exception ignored) {}
        }

        int bgResId = R.drawable.wallpaper_custom_bg;
        int fgResId = R.drawable.wallpaper_custom_fg;

        wallpaperBg.setImageResource(bgResId);
        try {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), bgResId);
            BlurManager.setWallpaper(bmp);
        } catch (Exception ignored) {}

        if (depthEnabled) {
            wallpaperFg.setImageResource(fgResId);
            wallpaperFg.setVisibility(View.VISIBLE);
        } else {
            wallpaperFg.setVisibility(View.GONE);
        }
    }

    private void setupDock() {
        dockRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<AppItem> dockApps = appListRepo.getDockApps();

        dockAdapter = new DockAdapter(dockApps, new DockAdapter.Listener() {
            @Override
            public void onAppFocused(AppItem app) {
                if (dockAdapter != null && dockAdapter.isReorderMode()) return;
                updateShelfForApp(app.packageName);
            }

            @Override
            public void onAppClicked(AppItem app) {
                launchPackage(app.packageName);
            }

            @Override
            public void onAppLongClicked(AppItem app, int position) {
                shelfContainer.setVisibility(View.GONE);
                dockAdapter.startReorderMode(position);
            }

            @Override
            public void onOrderChanged(List<AppItem> currentItems) {
                List<String> pkgs = new ArrayList<>();
                for (AppItem it : currentItems) {
                    pkgs.add(it.packageName);
                }
                appListRepo.saveDockPackages(pkgs);
                setupDrawer();
                int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
                if (pos >= 0 && pos < currentItems.size()) {
                    updateShelfForApp(currentItems.get(pos).packageName);
                }
            }

            @Override
            public void onDownPressed() {
                openDrawer();
            }

            @Override
            public void onUpPressed() {
                if (shelfContainer.getVisibility() == View.VISIBLE && shelfAdapter.getItemCount() > 0) {
                    focusShelfItem(0);
                } else {
                    settingsButton.requestFocus();
                }
            }

            @Override
            public void onLeftAtStart() {
                openSystemQuickSettings();
            }
        });

        dockRecycler.setAdapter(dockAdapter);

        dockRecycler.postDelayed(() -> {
            syncShelfWidth();
            if (drawerContainer.getVisibility() != View.VISIBLE && settingsPanelContainer.getVisibility() != View.VISIBLE) {
                focusDockItem(0);
            }
        }, 150);
    }

    private void focusDockItem(int pos) {
        dockRecycler.post(() -> {
            RecyclerView.ViewHolder vh = dockRecycler.findViewHolderForAdapterPosition(pos);
            if (vh != null) {
                vh.itemView.requestFocus();
            } else {
                View child = dockRecycler.getChildAt(pos);
                if (child != null) {
                    child.requestFocus();
                } else {
                    dockRecycler.scrollToPosition(pos);
                    dockRecycler.postDelayed(() -> {
                        RecyclerView.ViewHolder h = dockRecycler.findViewHolderForAdapterPosition(pos);
                        if (h != null) h.itemView.requestFocus();
                    }, 50);
                }
            }
        });
    }

    private void focusShelfItem(int pos) {
        navigateShelfTo(pos);
    }

    private void navigateShelfTo(int targetPos) {
        if (shelfAdapter == null || shelfAdapter.getItemCount() == 0) return;
        int max = shelfAdapter.getItemCount() - 1;
        int target = Math.max(0, Math.min(targetPos, max));
        currentShelfPos = target;

        LinearLayoutManager lm = (LinearLayoutManager) shelfRecycler.getLayoutManager();
        if (lm != null) {
            lm.scrollToPositionWithOffset(target, 0);
        }

        shelfRecycler.setHorizontalFadingEdgeEnabled(target < max);

        shelfRecycler.post(() -> {
            RecyclerView.ViewHolder vh = shelfRecycler.findViewHolderForAdapterPosition(target);
            if (vh != null) {
                vh.itemView.requestFocus();
            } else {
                shelfRecycler.postDelayed(() -> {
                    RecyclerView.ViewHolder h = shelfRecycler.findViewHolderForAdapterPosition(target);
                    if (h != null) h.itemView.requestFocus();
                }, 30);
            }
        });
    }

    private void setupShelf() {
        shelfRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        shelfAdapter = new ShelfAdapter(new ShelfAdapter.Listener() {
            @Override
            public void onCardClicked(WatchNextItem item) {
                launchShow(item);
            }

            @Override
            public void onDownPressed() {
                int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
                focusDockItem(pos);
            }

            @Override
            public void onUpPressed() {
                settingsButton.requestFocus();
            }

            @Override
            public void onNavigateRight(int currentPos) {
                if (currentPos < shelfAdapter.getItemCount() - 1) {
                    navigateShelfTo(currentPos + 1);
                }
            }

            @Override
            public void onNavigateLeft(int currentPos) {
                if (currentPos > 0) {
                    navigateShelfTo(currentPos - 1);
                }
            }
        });
        shelfRecycler.setAdapter(shelfAdapter);
    }

    private void updateShelfForApp(String packageName) {
        new Thread(() -> {
            List<WatchNextItem> shows = watchNextRepo.getShowsForPackage(packageName);
            runOnUiThread(() -> {
                shelfAdapter.setItems(shows);
                if (shows.isEmpty()) {
                    shelfContainer.setVisibility(View.GONE);
                } else {
                    shelfContainer.setVisibility(View.VISIBLE);
                    syncShelfWidth();
                    currentShelfPos = 0;
                    shelfRecycler.setHorizontalFadingEdgeEnabled(shows.size() > 1);
                }
            });
        }).start();
    }

    private void setupDrawer() {
        drawerDockRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        drawerRemainingRecycler.setLayoutManager(new GridLayoutManager(this, 9));

        List<AppItem> dockApps = appListRepo.getDockApps();
        List<AppItem> remainingApps = appListRepo.getRemainingApps();

        drawerDockAdapter = new DrawerDockAdapter(dockApps, new DrawerDockAdapter.Listener() {
            @Override
            public void onAppClicked(AppItem app) {
                closeDrawer();
                launchPackage(app.packageName);
            }

            @Override
            public void onAppLongClicked(AppItem app, int position) {
                if (drawerRemainingAdapter != null && drawerRemainingAdapter.isReorderMode()) {
                    drawerRemainingAdapter.exitReorderMode();
                }
                drawerDockAdapter.startReorderMode(position);
            }

            @Override
            public void onUpPressed() {
                closeDrawer();
            }

            @Override
            public void onDownPressed(int currentPos) {
                focusRemainingItem(Math.min(currentPos, drawerRemainingAdapter.getItemCount() - 1));
            }

            @Override
            public void onOrderChanged(List<AppItem> currentItems) {
                List<String> pkgs = new ArrayList<>();
                for (AppItem it : currentItems) pkgs.add(it.packageName);
                appListRepo.saveDockPackages(pkgs);
                setupDock();
            }

            @Override
            public void onMoveDownToRemaining(AppItem app, int position) {
                removeAppFromDock(app.packageName);
                drawerRemainingRecycler.postDelayed(() -> focusRemainingItem(0), 60);
            }

            @Override
            public void onReorderFinished(int finalPos) {
                focusDrawerDockItem(finalPos);
            }
        });

        drawerRemainingAdapter = new DrawerRemainingAdapter(remainingApps, new DrawerRemainingAdapter.Listener() {
            @Override
            public void onAppClicked(AppItem app) {
                closeDrawer();
                launchPackage(app.packageName);
            }

            @Override
            public void onAppLongClicked(AppItem app, int position) {
                if (drawerDockAdapter != null && drawerDockAdapter.isReorderMode()) {
                    drawerDockAdapter.exitReorderMode();
                }
                drawerRemainingAdapter.startReorderMode(position);
            }

            @Override
            public void onUpPressed(int currentPos) {
                focusDrawerDockItem(Math.min(currentPos, drawerDockAdapter.getItemCount() - 1));
            }

            @Override
            public void onMoveUpToDock(AppItem app, int position) {
                addAppToDock(app.packageName);
                drawerDockRecycler.postDelayed(() -> focusDrawerDockItem(drawerDockAdapter.getItemCount() - 1), 60);
            }

            @Override
            public void onOrderChanged(List<AppItem> currentItems) {}

            @Override
            public void onReorderFinished(int finalPos) {
                focusRemainingItem(finalPos);
            }
        });

        drawerDockRecycler.setAdapter(drawerDockAdapter);
        drawerRemainingRecycler.setAdapter(drawerRemainingAdapter);
    }

    private void focusDrawerDockItem(int pos) {
        if (drawerDockAdapter == null || drawerDockAdapter.getItemCount() == 0) return;
        int target = Math.max(0, Math.min(pos, drawerDockAdapter.getItemCount() - 1));
        drawerDockRecycler.post(() -> {
            RecyclerView.ViewHolder vh = drawerDockRecycler.findViewHolderForAdapterPosition(target);
            if (vh != null) {
                vh.itemView.requestFocus();
            } else {
                View child = drawerDockRecycler.getChildAt(target);
                if (child != null) child.requestFocus();
            }
        });
    }

    private void focusRemainingItem(int pos) {
        if (drawerRemainingAdapter == null || drawerRemainingAdapter.getItemCount() == 0) return;
        int target = Math.max(0, Math.min(pos, drawerRemainingAdapter.getItemCount() - 1));
        drawerRemainingRecycler.post(() -> {
            RecyclerView.ViewHolder vh = drawerRemainingRecycler.findViewHolderForAdapterPosition(target);
            if (vh != null) {
                vh.itemView.requestFocus();
            } else {
                View child = drawerRemainingRecycler.getChildAt(target);
                if (child != null) child.requestFocus();
            }
        });
    }

    private void removeAppFromDock(String pkg) {
        List<String> dockPkgs = new ArrayList<>(appListRepo.getSavedDockPackages());
        if (dockPkgs.contains(pkg)) {
            dockPkgs.remove(pkg);
            appListRepo.saveDockPackages(dockPkgs);
            setupDock();
            setupDrawer();
        }
    }

    private void addAppToDock(String pkg) {
        List<String> dockPkgs = new ArrayList<>(appListRepo.getSavedDockPackages());
        if (!dockPkgs.contains(pkg)) {
            if (dockPkgs.size() < 9) {
                dockPkgs.add(pkg);
            } else {
                dockPkgs.remove(dockPkgs.size() - 1);
                dockPkgs.add(pkg);
            }
            appListRepo.saveDockPackages(dockPkgs);
            setupDock();
            setupDrawer();
        }
    }

    private void openDrawer() {
        shelfContainer.setVisibility(View.GONE);
        dockContainer.setVisibility(View.GONE);
        settingsPanelContainer.setVisibility(View.GONE);
        gearPopup.setVisibility(View.GONE);
        drawerContainer.setVisibility(View.VISIBLE);
        drawerDockRecycler.scrollToPosition(0);
        drawerDockRecycler.postDelayed(() -> focusDrawerDockItem(0), 50);
    }

    private void closeDrawer() {
        if (drawerDockAdapter != null && drawerDockAdapter.isReorderMode()) {
            drawerDockAdapter.exitReorderMode();
        }
        if (drawerRemainingAdapter != null && drawerRemainingAdapter.isReorderMode()) {
            drawerRemainingAdapter.exitReorderMode();
        }
        drawerContainer.setVisibility(View.GONE);
        dockContainer.setVisibility(View.VISIBLE);
        int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
        dockContainer.post(() -> {
            focusDockItem(pos);
            List<AppItem> dockApps = appListRepo.getDockApps();
            if (pos >= 0 && pos < dockApps.size()) {
                updateShelfForApp(dockApps.get(pos).packageName);
            }
        });
    }

    private void setupSettings() {
        settingsButton.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.10f : 1.0f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(80).start();
        });

        // Only slide settings panel up when soft keyboard (IME) is actually displayed
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            float density = getResources().getDisplayMetrics().density;
            if (settingsPanelContainer.getVisibility() == View.VISIBLE) {
                if (imeVisible) {
                    settingsPanelContainer.animate().translationY(-155f * density).setDuration(220).start();
                } else {
                    settingsPanelContainer.animate().translationY(0f).setDuration(220).start();
                }
            }
            return ViewCompat.onApplyWindowInsets(v, insets);
        });

        // Click on gear button toggles dropdown popup directly beneath it
        settingsButton.setOnClickListener(v -> toggleGearPopup());

        settingsButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (gearPopup.getVisibility() == View.VISIBLE) {
                        btnPopupLauncherSettings.requestFocus();
                    } else if (shelfContainer.getVisibility() == View.VISIBLE && shelfAdapter.getItemCount() > 0) {
                        focusShelfItem(0);
                    } else {
                        int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
                        focusDockItem(pos);
                    }
                    return true;
                }
            }
            return false;
        });

        // Setup Gear Popup items
        btnPopupLauncherSettings.setOnClickListener(v -> {
            gearPopup.setVisibility(View.GONE);
            openLauncherSettings();
        });

        btnPopupSystemSettings.setOnClickListener(v -> {
            gearPopup.setVisibility(View.GONE);
            try {
                Intent fullSettingsIntent = new Intent(Settings.ACTION_SETTINGS);
                fullSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fullSettingsIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Systemeinstellungen konnten nicht geöffnet werden.", Toast.LENGTH_SHORT).show();
            }
        });

        btnPopupLauncherSettings.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    settingsButton.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    btnPopupSystemSettings.requestFocus();
                    return true;
                }
            }
            return false;
        });

        btnPopupSystemSettings.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    btnPopupLauncherSettings.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    gearPopup.setVisibility(View.GONE);
                    int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
                    focusDockItem(pos);
                    return true;
                }
            }
            return false;
        });

        // Setup In-Place Settings Category Tabs
        tabCategoryLang.setOnClickListener(v -> selectSettingsTab(0));
        tabCategoryWallpaper.setOnClickListener(v -> selectSettingsTab(1));
        tabCategoryWeather.setOnClickListener(v -> selectSettingsTab(2));
        tabCategoryAbout.setOnClickListener(v -> selectSettingsTab(3));
        btnCloseSettings.setOnClickListener(v -> closeLauncherSettings());

        tabCategoryLang.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectSettingsTab(0); });
        tabCategoryWallpaper.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectSettingsTab(1); });
        tabCategoryWeather.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectSettingsTab(2); });
        tabCategoryAbout.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) selectSettingsTab(3); });

        setupTabKeyNavigation(tabCategoryLang, 0);
        setupTabKeyNavigation(tabCategoryWallpaper, 1);
        setupTabKeyNavigation(tabCategoryWeather, 2);
        setupTabKeyNavigation(tabCategoryAbout, 3);

        // Language Buttons
        btnLangDe.setOnClickListener(v -> setLanguage("de"));
        btnLangEn.setOnClickListener(v -> setLanguage("en"));
        btnLangEs.setOnClickListener(v -> setLanguage("es"));
        btnLangFr.setOnClickListener(v -> setLanguage("fr"));
        btnLangPt.setOnClickListener(v -> setLanguage("pt"));
        btnLangIt.setOnClickListener(v -> setLanguage("it"));

        setupLeftExitToTab(btnLangDe, tabCategoryLang);

        // Wallpaper & Depth Buttons
        btnDepthOn.setOnClickListener(v -> setDepthEffect(true));
        btnDepthOff.setOnClickListener(v -> setDepthEffect(false));
        btnWpDefault.setOnClickListener(v -> setWallpaperSource("builtin"));
        btnWpCustomReload.setOnClickListener(v -> setWallpaperSource("custom"));
        btnWpOpenFiles.setOnClickListener(v -> openFileManager());

        setupLeftExitToTab(btnDepthOn, tabCategoryWallpaper);
        setupLeftExitToTab(btnWpDefault, tabCategoryWallpaper);

        // Weather Buttons
        btnWeatherOn.setOnClickListener(v -> setWeatherEnabled(true));
        btnWeatherOff.setOnClickListener(v -> setWeatherEnabled(false));
        btnUnitC.setOnClickListener(v -> setWeatherUnit("c"));
        btnUnitF.setOnClickListener(v -> setWeatherUnit("f"));
        btnInterval15.setOnClickListener(v -> setWeatherInterval(15));
        btnInterval30.setOnClickListener(v -> setWeatherInterval(30));
        btnInterval60.setOnClickListener(v -> setWeatherInterval(60));

        // Weather City Search
        etSearchCity.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performCitySearch();
                return true;
            }
            return false;
        });
        etSearchCity.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearchCity.getWindowToken(), 0);
                }
                settingsPanelContainer.animate().translationY(0f).setDuration(200).start();
            }
        });
        etSearchCity.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearchCity, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        btnSearchCity.setOnClickListener(v -> performCitySearch());

        setupLeftExitToTab(btnWeatherOn, tabCategoryWeather);
        setupLeftExitToTab(etSearchCity, tabCategoryWeather);

        View.OnKeyListener downToSearchListener = (v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                etSearchCity.requestFocus();
                return true;
            }
            return false;
        };
        btnWeatherOn.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    tabCategoryWeather.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    etSearchCity.requestFocus();
                    return true;
                }
            }
            return false;
        });
        btnWeatherOff.setOnKeyListener(downToSearchListener);
        btnUnitC.setOnKeyListener(downToSearchListener);
        btnUnitF.setOnKeyListener(downToSearchListener);
        btnInterval15.setOnKeyListener(downToSearchListener);
        btnInterval30.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                btnSearchCity.requestFocus();
                return true;
            }
            return false;
        });
        btnInterval60.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                btnSearchCity.requestFocus();
                return true;
            }
            return false;
        });

        etSearchCity.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    tabCategoryWeather.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    btnSearchCity.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    btnWeatherOn.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (cityResultsContainer.getVisibility() == View.VISIBLE && cityResultsContainer.getChildCount() > 0) {
                        cityResultsContainer.getChildAt(0).requestFocus();
                        return true;
                    }
                }
            }
            return false;
        });

        btnSearchCity.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    etSearchCity.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    btnInterval30.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (cityResultsContainer.getVisibility() == View.VISIBLE && cityResultsContainer.getChildCount() > 0) {
                        cityResultsContainer.getChildAt(0).requestFocus();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void toggleGearPopup() {
        if (gearPopup.getVisibility() == View.VISIBLE) {
            gearPopup.setVisibility(View.GONE);
        } else {
            gearPopup.setVisibility(View.VISIBLE);
            btnPopupLauncherSettings.requestFocus();
        }
    }

    private void openLauncherSettings() {
        gearPopup.setVisibility(View.GONE);
        drawerContainer.setVisibility(View.GONE);
        shelfContainer.setVisibility(View.GONE);
        dockContainer.setVisibility(View.GONE);

        updateSettingsUIStates();
        settingsPanelContainer.setVisibility(View.VISIBLE);
        selectSettingsTab(currentSettingsTab);
        tabCategoryLang.requestFocus();
    }

    private void openSystemQuickSettings() {
        // 1. Google TV Dashboard / Quick Settings Side Panel via TV Helper Service
        if (com.flankes.glasslauncher.service.TvHelperService.openQuickSettings()) {
            return;
        }

        // 2. Fallback to standard full TV settings
        try {
            Intent fullSettingsIntent = new Intent(Settings.ACTION_SETTINGS);
            fullSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(fullSettingsIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Systemeinstellungen konnten nicht geöffnet werden.", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeLauncherSettings() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etSearchCity != null) {
            imm.hideSoftInputFromWindow(etSearchCity.getWindowToken(), 0);
        }
        settingsPanelContainer.setTranslationY(0f);
        settingsPanelContainer.setVisibility(View.GONE);
        dockContainer.setVisibility(View.VISIBLE);
        int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
        dockContainer.post(() -> {
            focusDockItem(pos);
            List<AppItem> dockApps = appListRepo.getDockApps();
            if (pos >= 0 && pos < dockApps.size()) {
                updateShelfForApp(dockApps.get(pos).packageName);
            }
        });
    }

    private void selectSettingsTab(int tabIndex) {
        currentSettingsTab = tabIndex;

        panelViewLanguage.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        panelViewWallpaper.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        panelViewWeather.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        panelViewAbout.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        tabCategoryLang.setBackgroundResource(tabIndex == 0 ? R.drawable.btn_settings_tab_active_selector : R.drawable.btn_settings_tab_selector);
        tabCategoryWallpaper.setBackgroundResource(tabIndex == 1 ? R.drawable.btn_settings_tab_active_selector : R.drawable.btn_settings_tab_selector);
        tabCategoryWeather.setBackgroundResource(tabIndex == 2 ? R.drawable.btn_settings_tab_active_selector : R.drawable.btn_settings_tab_selector);
        tabCategoryAbout.setBackgroundResource(tabIndex == 3 ? R.drawable.btn_settings_tab_active_selector : R.drawable.btn_settings_tab_selector);
    }

    private void setupTabKeyNavigation(View tabView, int tabIndex) {
        tabView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    selectSettingsTab(tabIndex);
                    focusFirstControlInTab(tabIndex);
                    return true;
                }
            }
            return false;
        });
    }

    private void setupLeftExitToTab(View controlView, View targetTab) {
        controlView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                targetTab.requestFocus();
                return true;
            }
            return false;
        });
    }

    private void focusFirstControlInTab(int tabIndex) {
        switch (tabIndex) {
            case 0:
                String curLang = prefs.getString("app_language", "de");
                TextView langTarget = getLangButtonForCode(curLang);
                if (langTarget != null) langTarget.requestFocus();
                else btnLangDe.requestFocus();
                break;
            case 1:
                btnDepthOn.requestFocus();
                break;
            case 2:
                btnWeatherOn.requestFocus();
                break;
            case 3:
                btnCloseSettings.requestFocus();
                break;
        }
    }

    private TextView getLangButtonForCode(String code) {
        if ("en".equalsIgnoreCase(code)) return btnLangEn;
        if ("es".equalsIgnoreCase(code)) return btnLangEs;
        if ("fr".equalsIgnoreCase(code)) return btnLangFr;
        if ("pt".equalsIgnoreCase(code)) return btnLangPt;
        if ("it".equalsIgnoreCase(code)) return btnLangIt;
        return btnLangDe;
    }

    private void updateSettingsUIStates() {
        String lang = prefs.getString("app_language", "de");
        applyLanguageToUI(lang);
        updateLangButtonStyles(lang);

        boolean depth = prefs.getBoolean("depth_effect_enabled", true);
        updateDepthButtonStyles(depth);
        String scene = prefs.getString("selected_scene", "custom");
        updateWallpaperButtonStyles(scene);

        boolean weatherOn = prefs.getBoolean("weather_enabled", true);
        updateWeatherToggleStyles(weatherOn);
        String unit = prefs.getString("weather_unit", "c");
        updateUnitButtonStyles(unit);
        int interval = prefs.getInt("weather_interval_minutes", 30);
        updateIntervalButtonStyles(interval);
        updateCityStatusDisplay();
    }

    private void setLanguage(String code) {
        prefs.edit().putString("app_language", code).apply();
        applyLanguageLocale(code);
        updateDateWeather(new Date());
        updateLangButtonStyles(code);
        applyLanguageToUI(code);
        refreshWeather();
    }

    private void applyLanguageToUI(String langCode) {
        if (langCode == null) langCode = "de";

        // Popup
        if (btnPopupLauncherSettings != null) btnPopupLauncherSettings.setText(Localization.get(langCode, "popup_launcher_settings"));
        if (btnPopupSystemSettings != null) btnPopupSystemSettings.setText(Localization.get(langCode, "popup_tv_settings"));

        // Sidebar
        if (tvSidebarHeader != null) tvSidebarHeader.setText(Localization.get(langCode, "sidebar_header"));
        if (tabCategoryLang != null) tabCategoryLang.setText(Localization.get(langCode, "tab_lang"));
        if (tabCategoryWallpaper != null) tabCategoryWallpaper.setText(Localization.get(langCode, "tab_wallpaper"));
        if (tabCategoryWeather != null) tabCategoryWeather.setText(Localization.get(langCode, "tab_weather"));
        if (tabCategoryAbout != null) tabCategoryAbout.setText(Localization.get(langCode, "tab_about"));
        if (btnCloseSettings != null) btnCloseSettings.setText(Localization.get(langCode, "btn_close"));

        // Language Panel
        if (tvLangTitle != null) tvLangTitle.setText(Localization.get(langCode, "lang_title"));
        if (tvLangDesc != null) tvLangDesc.setText(Localization.get(langCode, "lang_desc"));
        if (tvLangStatus != null) tvLangStatus.setText(Localization.get(langCode, "lang_status_prefix") + getLanguageDisplayName(langCode));

        // Wallpaper Panel
        if (tvWpTitle != null) tvWpTitle.setText(Localization.get(langCode, "wp_title"));
        if (tvWpDepthLabel != null) tvWpDepthLabel.setText(Localization.get(langCode, "wp_depth_label"));
        if (btnDepthOn != null) btnDepthOn.setText(Localization.get(langCode, "btn_enabled"));
        if (btnDepthOff != null) btnDepthOff.setText(Localization.get(langCode, "btn_disabled"));
        if (tvWpSourceLabel != null) tvWpSourceLabel.setText(Localization.get(langCode, "wp_source_label"));
        if (btnWpDefault != null) btnWpDefault.setText(Localization.get(langCode, "wp_source_default"));
        if (btnWpCustomReload != null) btnWpCustomReload.setText(Localization.get(langCode, "wp_source_custom"));
        if (btnWpOpenFiles != null) btnWpOpenFiles.setText(Localization.get(langCode, "wp_open_files"));
        if (tvWpStatus != null) tvWpStatus.setText(Localization.get(langCode, "wp_hint"));

        // Weather Panel
        if (tvWeatherTitle != null) tvWeatherTitle.setText(Localization.get(langCode, "weather_title"));
        if (tvWeatherToggleLabel != null) tvWeatherToggleLabel.setText(Localization.get(langCode, "weather_toggle_label"));
        if (btnWeatherOn != null) btnWeatherOn.setText(Localization.get(langCode, "btn_on"));
        if (btnWeatherOff != null) btnWeatherOff.setText(Localization.get(langCode, "btn_off"));
        if (tvWeatherUnitLabel != null) tvWeatherUnitLabel.setText(Localization.get(langCode, "weather_unit_label"));
        if (tvWeatherIntervalLabel != null) tvWeatherIntervalLabel.setText(Localization.get(langCode, "weather_interval_label"));
        if (tvWeatherSearchLabel != null) tvWeatherSearchLabel.setText(Localization.get(langCode, "weather_search_label"));
        if (etSearchCity != null) etSearchCity.setHint(Localization.get(langCode, "weather_search_hint"));
        if (btnSearchCity != null) btnSearchCity.setText(Localization.get(langCode, "btn_search"));

        updateCityStatusDisplay();

        // About Panel
        if (tvAboutTitle != null) tvAboutTitle.setText(Localization.get(langCode, "about_title"));
        if (tvAboutVersion != null) tvAboutVersion.setText(Localization.get(langCode, "about_version"));
        if (tvAboutArch != null) tvAboutArch.setText(Localization.get(langCode, "about_arch"));
        if (tvAboutDesc != null) tvAboutDesc.setText(Localization.get(langCode, "about_desc"));
    }

    private void updateCityStatusDisplay() {
        if (tvCurrentCityLabel == null) return;
        String lang = prefs.getString("app_language", "de");
        String prefix = Localization.get(lang, "weather_current_city_prefix");
        String name = prefs.getString("weather_city_name", "Berlin");
        String country = prefs.getString("weather_country", "DE");
        float lat = prefs.getFloat("weather_lat", 52.52f);
        float lon = prefs.getFloat("weather_lon", 13.405f);

        String latStr = String.format(Locale.US, "%.2f° %s", Math.abs(lat), lat >= 0 ? "N" : "S");
        String lonStr = String.format(Locale.US, "%.2f° %s", Math.abs(lon), lon >= 0 ? "E" : "W");

        String display = prefix + name + (country.isEmpty() ? "" : " (" + country + ")") + " • " + latStr + ", " + lonStr;
        tvCurrentCityLabel.setText(display);
    }

    private void performCitySearch() {
        if (etSearchCity == null) return;
        String query = etSearchCity.getText().toString().trim();
        if (query.isEmpty()) return;

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearchCity.getWindowToken(), 0);
        }
        settingsPanelContainer.animate().translationY(0f).setDuration(220).start();

        String lang = prefs.getString("app_language", "de");
        tvSearchStatus.setVisibility(View.VISIBLE);
        tvSearchStatus.setText(Localization.get(lang, "weather_searching"));
        cityResultsContainer.setVisibility(View.GONE);
        cityResultsContainer.removeAllViews();

        WeatherService.searchCities(query, lang, cities -> {
            if (isFinishing() || isDestroyed()) return;
            if (cities == null || cities.isEmpty()) {
                tvSearchStatus.setText(Localization.get(lang, "weather_no_results"));
                return;
            }

            tvSearchStatus.setVisibility(View.GONE);
            cityResultsContainer.removeAllViews();
            cityResultsContainer.setVisibility(View.VISIBLE);

            float density = getResources().getDisplayMetrics().density;
            for (int i = 0; i < Math.min(cities.size(), 5); i++) {
                final WeatherService.CityInfo city = cities.get(i);
                TextView pill = new TextView(this);
                pill.setText(city.toString());
                pill.setTextColor(getResources().getColorStateList(R.color.btn_settings_text_selector));
                pill.setTextSize(12f);
                pill.setGravity(android.view.Gravity.CENTER);
                pill.setBackgroundResource(R.drawable.btn_settings_pill_selector);
                pill.setFocusable(true);
                pill.setClickable(true);
                pill.setPadding((int)(12 * density), (int)(4 * density), (int)(12 * density), (int)(4 * density));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (int)(30 * density)
                );
                lp.setMarginEnd((int)(8 * density));
                pill.setLayoutParams(lp);

                final int pillIndex = i;
                pill.setOnClickListener(v -> {
                    setCity(city.name, city.country, city.lat, city.lon);
                    for (int c = 0; c < cityResultsContainer.getChildCount(); c++) {
                        View child = cityResultsContainer.getChildAt(c);
                        child.setBackgroundResource(c == pillIndex ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
                    }
                    Toast.makeText(this, city.name + (city.country.isEmpty() ? "" : " (" + city.country + ")"), Toast.LENGTH_SHORT).show();
                });

                pill.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pillIndex == 0) {
                            tabCategoryWeather.requestFocus();
                            return true;
                        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            etSearchCity.requestFocus();
                            return true;
                        }
                    }
                    return false;
                });

                cityResultsContainer.addView(pill);
            }

            if (cityResultsContainer.getChildCount() > 0) {
                cityResultsContainer.getChildAt(0).requestFocus();
            }
        });
    }

    private void updateLangButtonStyles(String activeCode) {
        btnLangDe.setBackgroundResource("de".equalsIgnoreCase(activeCode) ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnLangEn.setBackgroundResource("en".equalsIgnoreCase(activeCode) ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnLangEs.setBackgroundResource("es".equalsIgnoreCase(activeCode) ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnLangFr.setBackgroundResource("fr".equalsIgnoreCase(activeCode) ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnLangPt.setBackgroundResource("pt".equalsIgnoreCase(activeCode) ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnLangIt.setBackgroundResource("it".equalsIgnoreCase(activeCode) ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
    }

    private String getLanguageDisplayName(String code) {
        if ("en".equalsIgnoreCase(code)) return "English";
        if ("es".equalsIgnoreCase(code)) return "Español";
        if ("fr".equalsIgnoreCase(code)) return "Français";
        if ("pt".equalsIgnoreCase(code)) return "Português";
        if ("it".equalsIgnoreCase(code)) return "Italiano";
        return "Deutsch";
    }

    private void setDepthEffect(boolean enabled) {
        prefs.edit().putBoolean("depth_effect_enabled", enabled).apply();
        updateDepthButtonStyles(enabled);
        setupWallpaper();
    }

    private void updateDepthButtonStyles(boolean enabled) {
        btnDepthOn.setBackgroundResource(enabled ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnDepthOff.setBackgroundResource(!enabled ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
    }

    private void setWallpaperSource(String source) {
        prefs.edit().putString("selected_scene", source).apply();
        updateWallpaperButtonStyles(source);
        setupWallpaper();
    }

    private void updateWallpaperButtonStyles(String source) {
        boolean isBuiltin = "builtin".equalsIgnoreCase(source);
        btnWpDefault.setBackgroundResource(isBuiltin ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnWpCustomReload.setBackgroundResource(!isBuiltin ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
    }

    private void openFileManager() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Wallpapers"), "resource/folder");
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setType("image/*");
                startActivity(fallback);
            } catch (Exception ignored) {
                Toast.makeText(this, "Eigene Bilder in /sdcard/Wallpapers/ ablegen.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setWeatherEnabled(boolean enabled) {
        prefs.edit().putBoolean("weather_enabled", enabled).apply();
        updateWeatherToggleStyles(enabled);
        if (enabled) {
            refreshWeather();
            startWeatherTimer();
        } else {
            weatherHandler.removeCallbacks(weatherTicker);
            weatherSummary = "";
            updateDateWeather(new Date());
        }
    }

    private void updateWeatherToggleStyles(boolean enabled) {
        btnWeatherOn.setBackgroundResource(enabled ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnWeatherOff.setBackgroundResource(!enabled ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
    }

    private void setWeatherUnit(String unit) {
        prefs.edit().putString("weather_unit", unit).apply();
        updateUnitButtonStyles(unit);
        refreshWeather();
    }

    private void updateUnitButtonStyles(String unit) {
        boolean isC = "c".equalsIgnoreCase(unit);
        btnUnitC.setBackgroundResource(isC ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnUnitF.setBackgroundResource(!isC ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
    }

    private void setWeatherInterval(int minutes) {
        prefs.edit().putInt("weather_interval_minutes", minutes).apply();
        updateIntervalButtonStyles(minutes);
        startWeatherTimer();
    }

    private void updateIntervalButtonStyles(int minutes) {
        btnInterval15.setBackgroundResource(minutes == 15 ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnInterval30.setBackgroundResource(minutes == 30 ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
        btnInterval60.setBackgroundResource(minutes == 60 ? R.drawable.btn_settings_pill_active_selector : R.drawable.btn_settings_pill_selector);
    }

    private void setCity(String name, String country, float lat, float lon) {
        prefs.edit()
            .putString("weather_city_name", name)
            .putString("weather_country", country)
            .putFloat("weather_lat", lat)
            .putFloat("weather_lon", lon)
            .apply();
        updateCityStatusDisplay();
        refreshWeather();
    }

    private void launchPackage(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (intent == null) {
                intent = pm.getLaunchIntentForPackage(pkg);
            }
            if (intent != null) {
                startActivity(intent);
            }
        } catch (Exception ignored) {}
    }

    private void launchShow(WatchNextItem item) {
        try {
            if (item.intentUri != null && !item.intentUri.isEmpty()) {
                Intent intent = Intent.parseUri(item.intentUri, Intent.URI_INTENT_SCHEME);
                startActivity(intent);
                return;
            }
        } catch (Exception ignored) {}
        launchPackage(item.packageName);
    }

    private boolean isDescendantOf(View child, View parent) {
        if (child == null || parent == null) return false;
        android.view.ViewParent p = child.getParent();
        while (p != null) {
            if (p == parent) return true;
            p = p.getParent();
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        final int action = event.getAction();
        final boolean isConfirm = (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER);

        // 0. If Gear Popup is open
        if (gearPopup.getVisibility() == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (action == KeyEvent.ACTION_UP) {
                    gearPopup.setVisibility(View.GONE);
                    settingsButton.requestFocus();
                }
                return true;
            }
        }

        // 0b. If In-Place Settings Panel is open
        if (settingsPanelContainer.getVisibility() == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (action == KeyEvent.ACTION_UP) {
                    View current = getCurrentFocus();
                    if (current == etSearchCity) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(etSearchCity.getWindowToken(), 0);
                        settingsPanelContainer.animate().translationY(0f).setDuration(200).start();
                        tabCategoryWeather.requestFocus();
                        return true;
                    }
                    View contentFrame = findViewById(R.id.settings_content_frame);
                    if (current != null && isDescendantOf(current, contentFrame)) {
                        focusCurrentCategoryTab();
                    } else {
                        closeLauncherSettings();
                    }
                }
                return true;
            }

            if (action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    View current = getCurrentFocus();
                    if (current != null && isDescendantOf(current, settingsPanelContainer)) {
                        View next = FocusFinder.getInstance().findNextFocus(settingsPanelContainer, current, View.FOCUS_UP);
                        if (next == null || !isDescendantOf(next, settingsPanelContainer)) {
                            // Trap focus: never allow focus to jump out of settingsPanelContainer to the home screen!
                            return true;
                        }
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    View current = getCurrentFocus();
                    if (current != null && isDescendantOf(current, settingsPanelContainer)) {
                        View next = FocusFinder.getInstance().findNextFocus(settingsPanelContainer, current, View.FOCUS_DOWN);
                        if (next == null || !isDescendantOf(next, settingsPanelContainer)) {
                            // Trap focus at bottom of settingsPanelContainer
                            return true;
                        }
                    }
                }
            }
        }

        // 1. If Reorder Mode is active in Home Dock
        if (dockAdapter != null && dockAdapter.isReorderMode()) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (action == KeyEvent.ACTION_UP) {
                    dockAdapter.exitReorderMode();
                }
                return true;
            }
            if (isConfirm) {
                if (action == KeyEvent.ACTION_DOWN) return true;
                if (action == KeyEvent.ACTION_UP) {
                    if (holdFired) {
                        holdFired = false;
                    } else {
                        dockAdapter.exitReorderMode();
                    }
                }
                return true;
            }
            if (action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    dockAdapter.moveLeft();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    dockAdapter.moveRight();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    int pos = dockAdapter.getReorderPosition();
                    dockAdapter.exitReorderMode();
                    if (pos >= 0 && pos < dockAdapter.getItemCount()) {
                        AppItem it = dockAdapter.getItems().get(pos);
                        removeAppFromDock(it.packageName);
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    return true;
                }
            }
            return true;
        }

        // 1b. If Reorder Mode is active in Drawer Dock Row
        if (drawerContainer.getVisibility() == View.VISIBLE && drawerDockAdapter != null && drawerDockAdapter.isReorderMode()) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (action == KeyEvent.ACTION_UP) {
                    drawerDockAdapter.exitReorderMode();
                }
                return true;
            }
            if (isConfirm) {
                if (action == KeyEvent.ACTION_DOWN) return true;
                if (action == KeyEvent.ACTION_UP) {
                    if (holdFired) {
                        holdFired = false;
                    } else {
                        drawerDockAdapter.exitReorderMode();
                    }
                }
                return true;
            }
            if (action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    drawerDockAdapter.moveLeft();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    drawerDockAdapter.moveRight();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    drawerDockAdapter.moveDown();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    return true;
                }
            }
            return true;
        }

        // 1c. If Reorder Mode is active in Drawer Remaining Apps
        if (drawerContainer.getVisibility() == View.VISIBLE && drawerRemainingAdapter != null && drawerRemainingAdapter.isReorderMode()) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (action == KeyEvent.ACTION_UP) {
                    drawerRemainingAdapter.exitReorderMode();
                }
                return true;
            }
            if (isConfirm) {
                if (action == KeyEvent.ACTION_DOWN) return true;
                if (action == KeyEvent.ACTION_UP) {
                    if (holdFired) {
                        holdFired = false;
                    } else {
                        drawerRemainingAdapter.exitReorderMode();
                    }
                }
                return true;
            }
            if (action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    drawerRemainingAdapter.moveLeft();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    drawerRemainingAdapter.moveRight();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    drawerRemainingAdapter.moveUp();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    drawerRemainingAdapter.moveDown();
                    return true;
                }
            }
            return true;
        }

        // 2. Hardware OK / Center Long Press detection (timer + auto-repeat detection + FLAG_LONG_PRESS)
        if (isConfirm) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (event.isLongPress() || (event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0) {
                    if (holdRunnable != null) keyHandler.removeCallbacks(holdRunnable);
                    holdFired = true;
                    onConfirmHeld();
                    return true;
                } else if (event.getRepeatCount() == 0) {
                    holdFired = false;
                    if (holdRunnable != null) keyHandler.removeCallbacks(holdRunnable);
                    holdRunnable = () -> {
                        holdFired = true;
                        onConfirmHeld();
                    };
                    keyHandler.postDelayed(holdRunnable, 350);
                } else if (event.getRepeatCount() > 0) {
                    if (!holdFired) {
                        if (holdRunnable != null) keyHandler.removeCallbacks(holdRunnable);
                        holdFired = true;
                        onConfirmHeld();
                        return true;
                    }
                }
            } else if (action == KeyEvent.ACTION_UP) {
                if (holdRunnable != null) {
                    keyHandler.removeCallbacks(holdRunnable);
                    holdRunnable = null;
                }
                if (holdFired) {
                    holdFired = false;
                    return true;
                }
            }
        }

        // 3. Close drawer on Back key
        if (keyCode == KeyEvent.KEYCODE_BACK && drawerContainer.getVisibility() == View.VISIBLE) {
            if (action == KeyEvent.ACTION_UP) {
                closeDrawer();
            }
            return true;
        }

        // 4. Default focus restore if nothing is focused or root layout grabbed focus
        if (action == KeyEvent.ACTION_DOWN) {
            View current = getCurrentFocus();
            if (current == null || current.getId() == R.id.root_layout || current.getId() == android.R.id.content) {
                if (settingsPanelContainer.getVisibility() == View.VISIBLE) {
                    focusCurrentCategoryTab();
                } else if (drawerContainer.getVisibility() == View.VISIBLE) {
                    focusDrawerDockItem(0);
                } else {
                    int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
                    focusDockItem(pos);
                }
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private void focusCurrentCategoryTab() {
        switch (currentSettingsTab) {
            case 0: tabCategoryLang.requestFocus(); break;
            case 1: tabCategoryWallpaper.requestFocus(); break;
            case 2: tabCategoryWeather.requestFocus(); break;
            case 3: tabCategoryAbout.requestFocus(); break;
            default: tabCategoryLang.requestFocus(); break;
        }
    }

    private void onConfirmHeld() {
        if (settingsPanelContainer.getVisibility() == View.VISIBLE || gearPopup.getVisibility() == View.VISIBLE) {
            return;
        }

        if (drawerContainer.getVisibility() == View.VISIBLE) {
            View focused = getCurrentFocus();
            if (focused != null) {
                View dockItem = drawerDockRecycler.findContainingItemView(focused);
                if (dockItem != null) {
                    int pos = drawerDockRecycler.getChildAdapterPosition(dockItem);
                    if (pos >= 0 && pos < drawerDockAdapter.getItemCount()) {
                        drawerDockAdapter.startReorderMode(pos);
                        return;
                    }
                }

                View remainingItem = drawerRemainingRecycler.findContainingItemView(focused);
                if (remainingItem != null) {
                    int pos = drawerRemainingRecycler.getChildAdapterPosition(remainingItem);
                    if (pos >= 0 && pos < drawerRemainingAdapter.getItemCount()) {
                        drawerRemainingAdapter.startReorderMode(pos);
                        return;
                    }
                }
            }
        } else if (dockContainer.getVisibility() == View.VISIBLE) {
            if (dockAdapter != null && !dockAdapter.isReorderMode()) {
                View focused = getCurrentFocus();
                int pos = -1;
                if (focused != null) {
                    View itemView = dockRecycler.findContainingItemView(focused);
                    if (itemView != null) {
                        pos = dockRecycler.getChildAdapterPosition(itemView);
                    }
                }
                if (pos < 0) {
                    pos = dockAdapter.getSelectedPosition();
                }
                if (pos >= 0 && pos < dockAdapter.getItemCount()) {
                    shelfContainer.setVisibility(View.GONE);
                    dockAdapter.startReorderMode(pos);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            watchNextRepo.refresh();
            int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
            List<AppItem> dockApps = appListRepo.getDockApps();
            if (pos >= 0 && pos < dockApps.size()) {
                updateShelfForApp(dockApps.get(pos).packageName);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        timeHandler.post(timeTicker);
        startWeatherTimer();
        watchNextRepo.refresh();
        int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
        List<AppItem> dockApps = appListRepo.getDockApps();
        if (pos >= 0 && pos < dockApps.size()) {
            updateShelfForApp(dockApps.get(pos).packageName);
        }
        dockContainer.post(this::syncShelfWidth);
        if (settingsPanelContainer.getVisibility() != View.VISIBLE && drawerContainer.getVisibility() != View.VISIBLE) {
            focusDockItem(pos);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        timeHandler.removeCallbacks(timeTicker);
        weatherHandler.removeCallbacks(weatherTicker);
    }

    @Override
    public void onBackPressed() {
        if (gearPopup.getVisibility() == View.VISIBLE) {
            gearPopup.setVisibility(View.GONE);
            settingsButton.requestFocus();
            return;
        }
        if (settingsPanelContainer.getVisibility() == View.VISIBLE) {
            closeLauncherSettings();
            return;
        }
        if (dockAdapter != null && dockAdapter.isReorderMode()) {
            dockAdapter.exitReorderMode();
            int pos = dockAdapter.getSelectedPosition();
            List<AppItem> dockApps = appListRepo.getDockApps();
            if (pos >= 0 && pos < dockApps.size()) {
                updateShelfForApp(dockApps.get(pos).packageName);
            }
            return;
        }
        if (drawerContainer.getVisibility() == View.VISIBLE) {
            closeDrawer();
            return;
        }
        int pos = dockAdapter != null ? dockAdapter.getSelectedPosition() : 0;
        focusDockItem(pos);
    }
}
