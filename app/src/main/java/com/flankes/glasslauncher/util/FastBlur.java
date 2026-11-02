package com.flankes.glasslauncher.util;

import android.graphics.Bitmap;

public class FastBlur {

    public static Bitmap blur(Bitmap src, int radius) {
        if (src == null || radius < 1) return src;

        // Downscale to 480x270 (4x) for instantaneous blur processing and wider blur radius spread
        int targetW = Math.max(1, src.getWidth() / 4);
        int targetH = Math.max(1, src.getHeight() / 4);
        Bitmap downscaled = Bitmap.createScaledBitmap(src, targetW, targetH, true);

        Bitmap blurred = boxBlur(downscaled, radius);
        if (blurred != downscaled) {
            downscaled.recycle();
        }

        // Scale back up to 1920x1080 with smooth bilinear filtering
        Bitmap result = Bitmap.createScaledBitmap(blurred, 1920, 1080, true);
        if (result != blurred) {
            blurred.recycle();
        }
        return result;
    }

    private static Bitmap boxBlur(Bitmap src, int radius) {
        if (radius < 1) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pix = new int[w * h];
        src.getPixels(pix, 0, w, 0, 0, w, h);

        int[] tmp = new int[w * h];
        int div = 2 * radius + 1;

        // Horizontal pass
        for (int y = 0; y < h; y++) {
            int row = y * w;
            int rSum = 0, gSum = 0, bSum = 0;
            for (int i = -radius; i <= radius; i++) {
                int px = pix[row + Math.min(Math.max(i, 0), w - 1)];
                rSum += (px >> 16) & 0xff;
                gSum += (px >> 8) & 0xff;
                bSum += px & 0xff;
            }
            for (int x = 0; x < w; x++) {
                tmp[row + x] = 0xff000000 | ((rSum / div) << 16) | ((gSum / div) << 8) | (bSum / div);
                int p1 = pix[row + Math.max(0, x - radius)];
                int p2 = pix[row + Math.min(w - 1, x + radius + 1)];
                rSum += ((p2 >> 16) & 0xff) - ((p1 >> 16) & 0xff);
                gSum += ((p2 >> 8) & 0xff) - ((p1 >> 8) & 0xff);
                bSum += (p2 & 0xff) - (p1 & 0xff);
            }
        }

        // Vertical pass
        for (int x = 0; x < w; x++) {
            int rSum = 0, gSum = 0, bSum = 0;
            for (int i = -radius; i <= radius; i++) {
                int px = tmp[Math.min(Math.max(i, 0), h - 1) * w + x];
                rSum += (px >> 16) & 0xff;
                gSum += (px >> 8) & 0xff;
                bSum += px & 0xff;
            }
            for (int y = 0; y < h; y++) {
                pix[y * w + x] = 0xff000000 | ((rSum / div) << 16) | ((gSum / div) << 8) | (bSum / div);
                int p1 = tmp[Math.max(0, y - radius) * w + x];
                int p2 = tmp[Math.min(h - 1, y + radius + 1) * w + x];
                rSum += ((p2 >> 16) & 0xff) - ((p1 >> 16) & 0xff);
                gSum += ((p2 >> 8) & 0xff) - ((p1 >> 8) & 0xff);
                bSum += (p2 & 0xff) - (p1 & 0xff);
            }
        }

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(pix, 0, w, 0, 0, w, h);
        return out;
    }
}
