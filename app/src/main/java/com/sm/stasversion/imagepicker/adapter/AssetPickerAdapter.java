package com.sm.stasversion.imagepicker.adapter;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;
import com.sm.stasversion.R;
import com.sm.stasversion.classes.DoubleClickListener;
import com.sm.stasversion.imagepicker.helper.ImageHelper;
import com.sm.stasversion.imagepicker.listener.OnAssetClickListener;
import com.sm.stasversion.imagepicker.listener.OnAssetSelectionListener;
import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.model.Image;
import com.sm.stasversion.imagepicker.model.Video;
import com.sm.stasversion.imagepicker.ui.common.BaseRecyclerViewAdapter;
import com.sm.stasversion.imagepicker.ui.imagepicker.AssetLoader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by hoanglam on 7/31/16.
 */
public class AssetPickerAdapter extends BaseRecyclerViewAdapter<AssetPickerAdapter.ImageViewHolder> {

    private List<Asset> assets = new ArrayList<>();
    private List<Asset> selectedAssets = new ArrayList<>();
    private OnAssetClickListener itemClickListener;
    private OnAssetSelectionListener assetSelectionListener;
    private Boolean studio = false;

    public AssetPickerAdapter(Context context, AssetLoader assetLoader, List<Asset> selectedAssets, OnAssetClickListener itemClickListener, Boolean _studio) {
        super(context, assetLoader);

        this.studio = _studio;

        this.itemClickListener = itemClickListener;

        if (selectedAssets != null && !selectedAssets.isEmpty()) {
            this.selectedAssets.addAll(selectedAssets);
        }
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if(studio) {
            itemView = getInflater().inflate(R.layout.studio_item, parent, false);
        } else {
            itemView = getInflater().inflate(R.layout.imagepicker_item_image, parent, false);
        }

        return new ImageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ImageViewHolder viewHolder, final int position) {
        final Asset asset = assets.get(position);
        final boolean isSelected = isSelected(asset);

        assets.get(position).setPosition(position);

        if(studio) {
            getAssetLoader().loadConfigAsset(asset, viewHolder.image);
        } else {
            getAssetLoader().loadAsset(asset, viewHolder.image);
        }

        if (asset instanceof Image) {
            viewHolder.gifIndicator.setVisibility(ImageHelper.isGifFormat((Image)asset) ? View.VISIBLE : View.GONE);
        } else {
            viewHolder.gifIndicator.setVisibility(View.GONE);
            // If video, show video indicator
        }
        viewHolder.videoIndicator.setVisibility(asset instanceof Video ? View.VISIBLE : View.GONE);

        if(!studio) {
            viewHolder.alphaView.setAlpha(isSelected ? 0.5f : 0.0f);

            viewHolder.container.setForeground(isSelected
                    ? ContextCompat.getDrawable(getContext(), R.drawable.imagepicker_ic_selected)
                    : null);
        } else {
            viewHolder.border.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            viewHolder.alphaView.setAlpha(0.0f);
        }

        viewHolder.itemView.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onDoubleClick() {
                if(studio) {
                    List<Asset> assets = new ArrayList<>();
                    assets.add(asset);

                    assetSelectionListener.onSelectionUpdate(assets, asset);
                }
            }
            @Override
            public void onClick() {
                //boolean shouldSelect = itemClickListener.onAssetClick(view, viewHolder.getAdapterPosition(), !isSelected);

                if(viewHolder.border != null) {
                    if (viewHolder.border.getVisibility() == View.VISIBLE) {
                        removeSelected(asset, position);
                        viewHolder.border.setVisibility(View.GONE);
                    } else {
                        selectedAssets.add(asset);
                        viewHolder.border.setVisibility(View.VISIBLE);
                    }
                } else {
                    addSelected(asset, position);
                }

                itemClickListener.onAssetClick(null, viewHolder.getAdapterPosition(), !isSelected);
            }
        });

        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(studio) {
                    List<Asset> assets = new ArrayList<>();
                    assets.add(asset);

                    assetSelectionListener.onSelectionUpdate(assets, asset);
                    return true;
                }

                return true;
            }
        });
    }

    private boolean isSelected(Asset asset) {
        for (Asset selectedImage : selectedAssets) {
            if (selectedImage.getId() == asset.getId()) {
                return true;
            }
        }
        return false;
    }

    public void setOnImageSelectionListener(OnAssetSelectionListener assetSelectedListener) {
        this.assetSelectionListener = assetSelectedListener;
    }

    @Override
    public int getItemCount() {
        return assets.size();
    }

    public void updateEl(int position, String correction) {
        for (Asset selectedImage : selectedAssets) {
            if (selectedImage.getId() != this.assets.get(position).getId()) {
                selectedImage.setCorrection(correction);
            }
        }

        if (this.assets.get(position) != null) {
            this.assets.get(position).setCorrection(correction);
        }
    }

    public void removeEl(int id) {
        List<Asset> newSelected = new ArrayList<>();
        List<Asset> newAssets = new ArrayList<>();

        for (Asset selectedImage : selectedAssets) {
            if (selectedImage.getId() != id) {
                newSelected.add(selectedImage);
            }
        }

        selectedAssets = newSelected;

        for (Asset selectedImage : assets) {
            if (selectedImage.getId() != id) {
                newAssets.add(selectedImage);
            }
        }

        assets = newAssets;
    }

    public void setData(List<Asset> assets) {
        if (assets != null) {
            this.assets.clear();
            this.assets.addAll(assets);
        }
        notifyDataSetChanged();
    }

    public void addSelected(List<Asset> assets) {
        selectedAssets.addAll(assets);
        notifySelectionChanged();
    }

    public void addSelected(Asset asset, int position) {
        selectedAssets.add(asset);
        notifyItemChanged(position);
        notifySelectionChanged();
    }

    public void removeSelected(Asset asset, int position) {
        Iterator<Asset> itr = selectedAssets.iterator();
        while (itr.hasNext()) {
            Asset itrImage = itr.next();
            if (itrImage.getId() == asset.getId()) {
                itr.remove();
                break;
            }
        }
        //notifyItemChanged(position);
        //notifySelectionChanged();
    }

    public void removeAllSelected() {
        selectedAssets.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (assetSelectionListener != null) {
            assetSelectionListener.onSelectionUpdate(selectedAssets, null);
        }
    }

    public void invalidate() {
        for (int i = 0; i < selectedAssets.size(); i++) {
            notifyItemChanged(i);
        }
    }

    public Asset getAsset(int position) {
        return selectedAssets.get(position);
    }

    public List<Asset> getSelectedAssets() {
        return selectedAssets;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {

        private FrameLayout container;
        private ConstraintLayout containerStudio;
        private ImageView image;
        private View alphaView;
        private View gifIndicator;
        private ImageView videoIndicator;
        private View border;

        public ImageViewHolder(View itemView) {
            super(itemView);

            if(itemView instanceof FrameLayout) {
                container = (FrameLayout) itemView;
            } else {
                containerStudio = (ConstraintLayout) itemView;
            }

            image = itemView.findViewById(R.id.image_thumbnail);
            alphaView = itemView.findViewById(R.id.view_alpha);
            gifIndicator = itemView.findViewById(R.id.gif_indicator);
            videoIndicator = itemView.findViewById(R.id.image_video_icon);
            border = itemView.findViewById(R.id.view_border);
        }

    }

}
