package com.flankes.glasslauncher.ui;

import android.graphics.Outline;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.flankes.glasslauncher.R;
import com.flankes.glasslauncher.model.AppItem;
import java.util.Collections;
import java.util.List;

public class DrawerDockAdapter extends RecyclerView.Adapter<DrawerDockAdapter.ViewHolder> {
    public interface Listener {
        void onAppClicked(AppItem app);
        void onAppLongClicked(AppItem app, int position);
        void onUpPressed();
        void onDownPressed(int currentPos);
        void onOrderChanged(List<AppItem> currentItems);
        void onMoveDownToRemaining(AppItem app, int position);
        void onReorderFinished(int finalPos);
    }

    private final List<AppItem> items;
    private final Listener listener;
    private boolean isReorderMode = false;
    private int reorderPosition = -1;

    public DrawerDockAdapter(List<AppItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public List<AppItem> getItems() {
        return items;
    }

    public boolean isReorderMode() {
        return isReorderMode;
    }

    public int getReorderPosition() {
        return reorderPosition;
    }

    public void startReorderMode(int pos) {
        if (pos < 0 || pos >= items.size()) return;
        isReorderMode = true;
        reorderPosition = pos;
        notifyDataSetChanged();
    }

    public void exitReorderMode() {
        if (!isReorderMode) return;
        isReorderMode = false;
        int target = reorderPosition >= 0 ? reorderPosition : 0;
        reorderPosition = -1;
        notifyDataSetChanged();
        if (listener != null) {
            listener.onReorderFinished(target);
        }
    }

    public boolean moveLeft() {
        if (!isReorderMode || reorderPosition <= 0) return false;
        int target = reorderPosition - 1;
        Collections.swap(items, reorderPosition, target);
        reorderPosition = target;
        notifyItemMoved(target + 1, target);
        notifyItemChanged(target);
        notifyItemChanged(target + 1);
        if (listener != null) {
            listener.onOrderChanged(items);
        }
        return true;
    }

    public boolean moveRight() {
        if (!isReorderMode || reorderPosition >= items.size() - 1) return false;
        int target = reorderPosition + 1;
        Collections.swap(items, reorderPosition, target);
        reorderPosition = target;
        notifyItemMoved(target - 1, target);
        notifyItemChanged(target);
        notifyItemChanged(target - 1);
        if (listener != null) {
            listener.onOrderChanged(items);
        }
        return true;
    }

    public boolean moveDown() {
        if (!isReorderMode || reorderPosition < 0 || reorderPosition >= items.size()) return false;
        AppItem app = items.get(reorderPosition);
        exitReorderMode();
        if (listener != null) {
            listener.onMoveDownToRemaining(app, reorderPosition);
        }
        return true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawer_app, parent, false);
        ViewHolder holder = new ViewHolder(view);
        holder.icon.setClipToOutline(true);
        holder.icon.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float r = 13 * view.getResources().getDisplayMetrics().density;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppItem item = items.get(position);
        holder.icon.setImageDrawable(item.icon);

        boolean isMovingThisItem = isReorderMode && (position == reorderPosition);

        if (isMovingThisItem) {
            holder.focusBorder.setBackgroundResource(R.drawable.glass_reorder_focus);
            holder.focusBorder.setVisibility(View.VISIBLE);
            holder.arrowLeft.setVisibility(position > 0 ? View.VISIBLE : View.GONE);
            holder.arrowRight.setVisibility(position < items.size() - 1 ? View.VISIBLE : View.GONE);
            holder.itemView.setScaleX(1.10f);
            holder.itemView.setScaleY(1.10f);
            holder.itemView.setElevation(12f);
        } else {
            holder.arrowLeft.setVisibility(View.GONE);
            holder.arrowRight.setVisibility(View.GONE);
            holder.focusBorder.setBackgroundResource(R.drawable.glass_focus_border);
            holder.focusBorder.setVisibility(holder.itemView.hasFocus() ? View.VISIBLE : View.GONE);
            holder.itemView.setElevation(0f);
            if (!isReorderMode) {
                holder.itemView.setScaleX(holder.itemView.hasFocus() ? 1.08f : 1.0f);
                holder.itemView.setScaleY(holder.itemView.hasFocus() ? 1.08f : 1.0f);
            } else {
                holder.itemView.setScaleX(1.0f);
                holder.itemView.setScaleY(1.0f);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (isReorderMode) {
                exitReorderMode();
                return;
            }
            if (listener != null) listener.onAppClicked(item);
        });

        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos >= 0 && pos < items.size()) {
                if (listener != null) listener.onAppLongClicked(items.get(pos), pos);
                return true;
            }
            return false;
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (isReorderMode) return;
            float scale = hasFocus ? 1.08f : 1.0f;
            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(80)
                .setInterpolator(new DecelerateInterpolator())
                .start();

            holder.focusBorder.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });

        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (isReorderMode) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (listener != null) {
                        listener.onUpPressed();
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (listener != null) {
                        int pos = holder.getBindingAdapterPosition();
                        listener.onDownPressed(pos);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    public AppItem getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final View focusBorder;
        public final ImageView arrowLeft;
        public final ImageView arrowRight;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.drawer_app_icon);
            focusBorder = itemView.findViewById(R.id.drawer_focus_border);
            arrowLeft = itemView.findViewById(R.id.drawer_arrow_left);
            arrowRight = itemView.findViewById(R.id.drawer_arrow_right);
        }
    }
}
