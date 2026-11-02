package com.flankes.glasslauncher.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.flankes.glasslauncher.R;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsDialog extends Dialog {
    public interface Listener {
        void onWallpaperChanged(String sceneKey);
        void onDepthToggled(boolean enabled);
    }

    private static final String[] BASE_SCENES = {"scene_custom"};
    private static final String[] BASE_NAMES = {"Alpenpanorama (Standard)"};

    private final Listener listener;
    private final SharedPreferences prefs;

    public SettingsDialog(@NonNull Context context, Listener listener) {
        super(context);
        this.listener = listener;
        this.prefs = context.getSharedPreferences("glass_launcher_prefs", Context.MODE_PRIVATE);
    }

    private List<String> getAvailableScenes() {
        List<String> list = new ArrayList<>(Arrays.asList(BASE_SCENES));
        File dir = new File(Environment.getExternalStorageDirectory(), "Wallpapers");
        if (new File(dir, "wallpaper.jpg").exists() || new File(dir, "wallpaper.png").exists()) {
            list.add("custom");
        }
        return list;
    }

    private String getSceneName(String key) {
        if ("custom".equals(key)) return "Eigener Speicher (/sdcard/Wallpapers/)";
        return BASE_NAMES[0];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_settings);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        TextView wallpaperStatus = findViewById(R.id.opt_wallpaper_current);
        TextView depthStatus = findViewById(R.id.opt_depth_status);

        updateLabels(wallpaperStatus, depthStatus);

        View optWallpaper = findViewById(R.id.opt_wallpaper);
        View optDepth = findViewById(R.id.opt_depth);
        View optSystem = findViewById(R.id.opt_system_settings);
        Button btnClose = findViewById(R.id.btn_close_settings);

        View[] focusables = {optWallpaper, optDepth, optSystem, btnClose};
        for (View item : focusables) {
            if (item == null) continue;
            item.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? 1.03f : 1.0f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(80).start();
                v.setBackgroundResource(hasFocus ? R.drawable.glass_card_focus : R.drawable.glass_card_bg);
            });
        }

        optWallpaper.setOnClickListener(v -> {
            List<String> scenes = getAvailableScenes();
            String current = prefs.getString("selected_scene", "scene_custom");
            int idx = 0;
            for (int i = 0; i < scenes.size(); i++) {
                if (scenes.get(i).equals(current)) {
                    idx = (i + 1) % scenes.size();
                    break;
                }
            }
            String next = scenes.get(idx);
            prefs.edit().putString("selected_scene", next).apply();
            updateLabels(wallpaperStatus, depthStatus);
            if (listener != null) listener.onWallpaperChanged(next);
        });

        optDepth.setOnClickListener(v -> {
            boolean enabled = prefs.getBoolean("depth_effect_enabled", true);
            boolean next = !enabled;
            prefs.edit().putBoolean("depth_effect_enabled", next).apply();
            updateLabels(wallpaperStatus, depthStatus);
            if (listener != null) listener.onDepthToggled(next);
        });

        optSystem.setOnClickListener(v -> {
            dismiss();
            try {
                getContext().startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {}
        });

        btnClose.setOnClickListener(v -> dismiss());
    }

    private void updateLabels(TextView wallpaperText, TextView depthText) {
        String current = prefs.getString("selected_scene", "scene_custom");
        wallpaperText.setText(getSceneName(current) + "  •  Drücken zum Wechseln");

        boolean enabled = prefs.getBoolean("depth_effect_enabled", true);
        depthText.setText(enabled ? "Aktiviert (Uhr hinter Bergkamm)" : "Deaktiviert (Uhr im Vordergrund)");
    }
}
