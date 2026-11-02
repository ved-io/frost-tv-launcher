package com.flankes.glasslauncher.ui;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView that preserves horizontal fading edge only on the right side,
 * ensuring the leftmost card at offset 0 is always 100% opaque and fully visible.
 */
public class RightFadingRecyclerView extends RecyclerView {

    public RightFadingRecyclerView(@NonNull Context context) {
        super(context);
    }

    public RightFadingRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RightFadingRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        // Never fade the left edge: card in index 0 is always completely visible
        return 0.0f;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        return super.getRightFadingEdgeStrength();
    }
}
