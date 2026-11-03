package com.flankes.glasslauncher.ui;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.flankes.glasslauncher.R;
import com.flankes.glasslauncher.model.AppItem;
import java.util.Collections;
import java.util.List;

public class DockAdapter extends RecyclerView.Adapter<DockAdapter.ViewHolder> {
    public interface Listener {
        void onAppFocused(AppItem app);
        void onAppClicked(AppItem app);
        void onAppLongClicked(AppItem app, int position);
        void onOrderChanged(List<AppItem> currentItems);
        void onDownPressed();
        void onUpPressed();
        void onLeftAtStart();
    }

    private final List<AppItem> items;
    private final Listener listener;
    private int selectedPosition = 0;
    private boolean isReorderMode = false;
    private int reorderPosition = -1;

    public DockAdapter(List<AppItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public boolean isReorderMode() {
        return isReorderMode;
    }

    public int getReorderPosition() {
        return reorderPosition;
    }

    public void startReorderMode(int pos) {
        android.util.Log.d("DockAdapter", "startReorderMode pos=" + pos);
        isReorderMode = true;
        reorderPosition = pos;
        selectedPosition = pos;
        notifyItemChanged(pos);
    }

    public void exitReorderMode() {
        if (!isReorderMode) return;
        isReorderMode = false;
        int lastSelected = reorderPosition >= 0 ? reorderPosition : selectedPosition;
        reorderPosition = -1;
        notifyDataSetChanged();
        if (listener != null) {
            listener.onOrderChanged(items);
            if (lastSelected >= 0 && lastSelected < items.size()) {
                listener.onAppFocused(items.get(lastSelected));
            }
        }
    }

    public boolean moveLeft() {
        if (!isReorderMode || reorderPosition <= 0) return false;
        int target = reorderPosition - 1;
        Collections.swap(items, reorderPosition, target);
        reorderPosition = target;
        selectedPosition = target;
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
        selectedPosition = target;
        notifyItemMoved(target - 1, target);
        notifyItemChanged(target);
        notifyItemChanged(target - 1);
        if (listener != null) {
            listener.onOrderChanged(items);
        }
        return true;
    }

    public List<AppItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dock_app, parent, false);
        ViewHolder holder = new ViewHolder(view);
        holder.appIcon.setClipToOutline(true);
        holder.appIcon.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                float r = 13 * view.getResources().getDisplayMetrics().density;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppItem item = items.get(position);
        holder.appIcon.setImageDrawable(item.icon);

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
                startReorderMode(pos);
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

            if (hasFocus) {
                selectedPosition = holder.getBindingAdapterPosition();
                if (listener != null && selectedPosition >= 0 && selectedPosition < items.size()) {
                    listener.onAppFocused(items.get(selectedPosition));
                }
            }
        });

        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            int pos = holder.getBindingAdapterPosition();

            // Long Press detection on physical TV remotes (repeat count >= 3 means holding OK)
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) && !isReorderMode) {
                    if (event.getRepeatCount() >= 3) {
                        startReorderMode(pos);
                        if (listener != null) listener.onAppLongClicked(item, pos);
                        return true;
                    }
                }
            }

            if (isReorderMode) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (reorderPosition > 0) {
                            int target = reorderPosition - 1;
                            Collections.swap(items, reorderPosition, target);
                            reorderPosition = target;
                            selectedPosition = target;
                            notifyItemMoved(target + 1, target);
                            if (listener != null) listener.onOrderChanged(items);
                        }
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (reorderPosition < items.size() - 1) {
                            int target = reorderPosition + 1;
                            Collections.swap(items, reorderPosition, target);
                            reorderPosition = target;
                            selectedPosition = target;
                            notifyItemMoved(target - 1, target);
                            if (listener != null) listener.onOrderChanged(items);
                        }
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BACK) {
                        exitReorderMode();
                        return true;
                    }
                }
                return true;
            }

            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (listener != null) {
                        listener.onDownPressed();
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (listener != null) {
                        listener.onUpPressed();
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos == 0) {
                    if (event.getRepeatCount() == 0 && listener != null) {
                        listener.onLeftAtStart();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView appIcon;
        public final View focusBorder;
        public final ImageView arrowLeft;
        public final ImageView arrowRight;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            focusBorder = itemView.findViewById(R.id.focus_border);
            arrowLeft = itemView.findViewById(R.id.reorder_arrow_left);
            arrowRight = itemView.findViewById(R.id.reorder_arrow_right);
        }
    }
}
