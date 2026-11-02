package com.flankes.glasslauncher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;
import com.flankes.glasslauncher.R;

public class GlassClockView extends TextView {

    public GlassClockView(Context context) {
        super(context);
        init();
    }

    public GlassClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GlassClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setIncludeFontPadding(false);
        setLetterSpacing(-0.02f);

        // Load the exact Antonio Light 300 font converted directly from Couch TV
        try {
            Typeface tf = getResources().getFont(R.font.antonio_light);
            setTypeface(tf);
        } catch (Exception e) {
            try {
                Typeface tf = getResources().getFont(R.font.antonio);
                setTypeface(tf);
            } catch (Exception ignored) {}
        }

        // Elongated slender aspect ratio (subtly stretched vertically, 0.94f)
        getPaint().setTextScaleX(0.94f);

        // High contrast crisp frosted white (matches Soll screenshot)
        setTextColor(Color.parseColor("#F8FFFFFF"));

        // Soft, rich drop shadow for strong contrast against bright sky and dawn clouds
        float density = getResources().getDisplayMetrics().density;
        setShadowLayer(14f * density, 0, 3f * density, 0x55000000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Ensure paint retains the elongated 0.94f text scale
        getPaint().setTextScaleX(0.94f);
        super.onDraw(canvas);
    }
}
