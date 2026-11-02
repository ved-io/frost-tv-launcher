package com.flankes.glasslauncher.ui;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.flankes.glasslauncher.util.BlurManager;

public class FrostedGlassDrawable extends Drawable {

    private final View ownerView;
    private final float cornerRadius;
    private final int baseColor;
    private final float strokeWidth;
    private final int strokeColor;

    private final Paint blurPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF boundsRect = new RectF();
    private final RectF strokeRect = new RectF();
    private final Matrix shaderMatrix = new Matrix();
    private final int[] screenPos = new int[2];

    private boolean isFocused = false;
    private final Runnable blurListener;

    public FrostedGlassDrawable(View ownerView, float cornerRadiusPx, int baseColor, float strokeWidthPx, int strokeColor) {
        this.ownerView = ownerView;
        this.cornerRadius = cornerRadiusPx;
        this.baseColor = baseColor;
        this.strokeWidth = strokeWidthPx;
        this.strokeColor = strokeColor;

        tintPaint.setStyle(Paint.Style.FILL);
        tintPaint.setColor(baseColor);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidthPx);
        strokePaint.setColor(strokeColor);

        blurListener = () -> {
            if (ownerView != null) {
                ownerView.postInvalidate();
            }
        };
        BlurManager.addListener(blurListener);
    }

    public void setFocused(boolean focused) {
        if (this.isFocused != focused) {
            this.isFocused = focused;
            invalidateSelf();
        }
    }

    public boolean isFocused() {
        return isFocused;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        boundsRect.set(bounds);
        float inset = strokeWidth / 2f;
        strokeRect.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);

        LinearGradient gradient = new LinearGradient(
            0, 0, bounds.width(), bounds.height(),
            new int[]{0x25FFFFFF, 0x0CFFFFFF, 0x02FFFFFF},
            new float[]{0f, 0.40f, 1f},
            Shader.TileMode.CLAMP
        );
        gradientPaint.setShader(gradient);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Bitmap blurredWallpaper = BlurManager.getBlurredWallpaper();
        if (blurredWallpaper != null && !blurredWallpaper.isRecycled() && ownerView != null) {
            ownerView.getLocationOnScreen(screenPos);
            shaderMatrix.reset();

            int screenW = ownerView.getResources().getDisplayMetrics().widthPixels;
            int screenH = ownerView.getResources().getDisplayMetrics().heightPixels;
            float sx = (float) screenW / blurredWallpaper.getWidth();
            float sy = (float) screenH / blurredWallpaper.getHeight();
            shaderMatrix.postScale(sx, sy);
            shaderMatrix.postTranslate(-screenPos[0], -screenPos[1]);

            BitmapShader shader = new BitmapShader(blurredWallpaper, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            shader.setLocalMatrix(shaderMatrix);
            blurPaint.setShader(shader);

            canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, blurPaint);
        }

        // Frosted milky tint (semi-transparent diffused glass)
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, tintPaint);

        // Specular light reflection from top-left
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, gradientPaint);

        // Highlight rim or focus border
        if (isFocused) {
            strokePaint.setColor(Color.WHITE);
            strokePaint.setStrokeWidth(strokeWidth * 1.8f);
        } else {
            strokePaint.setColor(strokeColor);
            strokePaint.setStrokeWidth(strokeWidth);
        }
        canvas.drawRoundRect(strokeRect, cornerRadius, cornerRadius, strokePaint);
    }

    @Override
    public void setAlpha(int alpha) {
        tintPaint.setAlpha(alpha);
        blurPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        tintPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
