package com.flankes.glasslauncher.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import com.flankes.glasslauncher.MainActivity;

/**
 * Lightweight helper service that:
 * 1. Provides instant access to the native Google TV Quick Settings side panel
 *    via performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS).
 * 2. Redirects stock Google TV HomeActivity back to Frost TV Launcher if Home is pressed.
 */
public class TvHelperService extends AccessibilityService {

    private static TvHelperService instance;

    public static boolean isAvailable() {
        return instance != null;
    }

    public static boolean openQuickSettings() {
        if (instance != null) {
            boolean success = instance.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
            if (!success) {
                success = instance.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
            }
            return success;
        }
        return false;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 50;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = event.getPackageName();
            CharSequence cls = event.getClassName();
            if (pkg != null && "com.google.android.apps.tv.launcherx".contentEquals(pkg)) {
                if (cls != null && cls.toString().contains("HomeActivity")) {
                    Intent home = new Intent(this, MainActivity.class);
                    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(home);
                }
            }
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }
}
