package com.flankes.glasslauncher.util;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;

public class BlurManager {

    private static Bitmap sBlurredWallpaper = null;
    private static final List<Runnable> sListeners = new ArrayList<>();

    public static synchronized void setWallpaper(Bitmap original) {
        if (original == null) return;
        new Thread(() -> {
            try {
                Bitmap blurred = FastBlur.blur(original, 10);
                synchronized (BlurManager.class) {
                    if (sBlurredWallpaper != null && !sBlurredWallpaper.isRecycled()) {
                        sBlurredWallpaper.recycle();
                    }
                    sBlurredWallpaper = blurred;
                }
                notifyListeners();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static synchronized Bitmap getBlurredWallpaper() {
        return sBlurredWallpaper;
    }

    public static synchronized void addListener(Runnable listener) {
        if (!sListeners.contains(listener)) {
            sListeners.add(listener);
        }
    }

    public static synchronized void removeListener(Runnable listener) {
        sListeners.remove(listener);
    }

    private static synchronized void notifyListeners() {
        List<Runnable> copy = new ArrayList<>(sListeners);
        for (Runnable r : copy) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
