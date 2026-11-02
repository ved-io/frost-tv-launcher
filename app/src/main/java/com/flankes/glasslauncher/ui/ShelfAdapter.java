package com.flankes.glasslauncher.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.LruCache;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.flankes.glasslauncher.R;
import com.flankes.glasslauncher.model.WatchNextItem;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ShelfAdapter extends RecyclerView.Adapter<ShelfAdapter.ViewHolder> {
    public interface Listener {
        void onCardClicked(WatchNextItem item);
        void onDownPressed();
        void onUpPressed();
        void onNavigateRight(int currentPos);
        void onNavigateLeft(int currentPos);
    }

    private final List<WatchNextItem> items = new ArrayList<>();
    private final Listener listener;
    private final LruCache<String, Bitmap> memoryCache;

    public ShelfAdapter(Listener listener) {
        this.listener = listener;

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void setItems(List<WatchNextItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shelf_card, parent, false);
        ViewHolder holder = new ViewHolder(view);
        holder.cardThumb.setClipToOutline(true);
        holder.cardThumb.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                float r = 8 * view.getResources().getDisplayMetrics().density;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });
        holder.cardAppIcon.setClipToOutline(true);
        holder.cardAppIcon.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                float r = 3 * view.getResources().getDisplayMetrics().density;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });

        float density = parent.getResources().getDisplayMetrics().density;
        holder.cardContentContainer.setBackground(new FrostedGlassDrawable(holder.cardContentContainer, 12f * density, 0x551E2433, 1.0f * density, 0x48DCE6F2));

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchNextItem item = items.get(position);
        Context context = holder.itemView.getContext();
        PackageManager pm = context.getPackageManager();

        ViewGroup.LayoutParams genericLp = holder.itemView.getLayoutParams();
        if (genericLp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) genericLp;
            int marginEnd = (int) (14 * context.getResources().getDisplayMetrics().density);
            if (lp.leftMargin != 0 || lp.rightMargin != marginEnd) {
                lp.leftMargin = 0;
                lp.rightMargin = marginEnd;
                holder.itemView.setLayoutParams(lp);
            }
        }

        holder.cardTitle.setText(item.title);
        holder.cardSubtitle.setText(item.subtitle);

        // App badge (Icon + uppercase label)
        try {
            ApplicationInfo ai = pm.getApplicationInfo(item.packageName, 0);
            CharSequence appLabel = pm.getApplicationLabel(ai);
            holder.cardAppName.setText(appLabel != null ? appLabel.toString().toUpperCase() : "");
            Drawable icon = pm.getApplicationIcon(ai);
            holder.cardAppIcon.setImageDrawable(icon);
            holder.cardAppIcon.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            holder.cardAppName.setText("");
            holder.cardAppIcon.setVisibility(View.GONE);
        }

        // Progress bar (only for active in-progress items)
        if (item.progress > 0.01 && item.progress < 0.99) {
            holder.cardProgress.setVisibility(View.VISIBLE);
            holder.cardProgress.setProgress((int) Math.round(item.progress * 100));
        } else {
            holder.cardProgress.setVisibility(View.GONE);
        }

        // Thumbnail poster art
        holder.cardThumb.setImageDrawable(null);
        holder.cardThumb.setTag(item.posterUri);
        if (item.posterUri != null && !item.posterUri.isEmpty()) {
            loadThumbnail(context, item.posterUri, holder.cardThumb);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClicked(item);
        });

        // Exact Couch TV focus behavior: No distortion zoom, crisp 1.5dp white border & elevation
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            holder.cardFocusBorder.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            holder.itemView.setElevation(hasFocus ? 16f : 0f);
            if (holder.cardContentContainer.getBackground() instanceof FrostedGlassDrawable) {
                ((FrostedGlassDrawable) holder.cardContentContainer.getBackground()).setFocused(hasFocus);
            }
        });

        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
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
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    int pos = holder.getBindingAdapterPosition();
                    if (listener != null) {
                        listener.onNavigateRight(pos);
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    int pos = holder.getBindingAdapterPosition();
                    if (listener != null) {
                        listener.onNavigateLeft(pos);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void loadThumbnail(Context context, String uriStr, ImageView imageView) {
        Bitmap cached = memoryCache.get(uriStr);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        new Thread(() -> {
            try {
                Uri uri = Uri.parse(uriStr);
                InputStream is = null;
                if ("content".equals(uri.getScheme())) {
                    is = context.getContentResolver().openInputStream(uri);
                } else if ("file".equals(uri.getScheme())) {
                    is = new java.io.FileInputStream(uri.getPath());
                } else if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                    java.net.URL url = new java.net.URL(uriStr);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(2500);
                    conn.setReadTimeout(3000);
                    conn.connect();
                    is = conn.getInputStream();
                }

                if (is != null) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    if (bmp != null) {
                        memoryCache.put(uriStr, bmp);
                        imageView.post(() -> {
                            if (uriStr.equals(imageView.getTag())) {
                                imageView.setImageBitmap(bmp);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View cardContentContainer;
        public final ImageView cardThumb;
        public final ImageView cardAppIcon;
        public final TextView cardAppName;
        public final TextView cardTitle;
        public final TextView cardSubtitle;
        public final ProgressBar cardProgress;
        public final View cardFocusBorder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContentContainer = itemView.findViewById(R.id.card_content_container);
            cardThumb = itemView.findViewById(R.id.card_thumb);
            cardAppIcon = itemView.findViewById(R.id.card_app_icon);
            cardAppName = itemView.findViewById(R.id.card_app_name);
            cardTitle = itemView.findViewById(R.id.card_title);
            cardSubtitle = itemView.findViewById(R.id.card_subtitle);
            cardProgress = itemView.findViewById(R.id.card_progress);
            cardFocusBorder = itemView.findViewById(R.id.card_focus_border);
        }
    }
}
