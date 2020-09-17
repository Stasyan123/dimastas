package com.sm.stasversion.imagepicker.ui.imagepicker;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcelable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;

import com.sm.stasversion.R;
import com.sm.stasversion.imagepicker.adapter.AssetPickerAdapter;
import com.sm.stasversion.imagepicker.adapter.FolderPickerAdapter;
import com.sm.stasversion.imagepicker.listener.OnAssetClickListener;
import com.sm.stasversion.imagepicker.listener.OnAssetSelectionListener;
import com.sm.stasversion.imagepicker.listener.OnBackAction;
import com.sm.stasversion.imagepicker.listener.OnFolderClickListener;
import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.model.Config;
import com.sm.stasversion.imagepicker.model.Folder;
import com.sm.stasversion.imagepicker.widget.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hoanglam on 8/17/17.
 */

public class RecyclerViewManager {

    private Context context;
    private RecyclerView recyclerView;
    private Config config;

    private GridLayoutManager layoutManager;
    private GridSpacingItemDecoration itemOffsetDecoration;

    private AssetPickerAdapter assetAdapter;
    private FolderPickerAdapter folderAdapter;

    private int assetsColumns;
    private int folderColumns;

    private AssetLoader mAssetLoader;

    private Parcelable foldersState;
    private String title;
    private boolean isShowingFolder;


    public RecyclerViewManager(RecyclerView recyclerView, Config config, int orientation) {
        this.recyclerView = recyclerView;
        this.config = config;
        context = recyclerView.getContext();
        changeOrientation(orientation);
        mAssetLoader = new AssetLoader();
        isShowingFolder = false;
    }

    public void setupAdapters(OnAssetClickListener imageClickListener, final OnFolderClickListener folderClickListener) {
        ArrayList<Asset> selectedAssets = null;
        if (config.isMultipleMode() && !config.getSelectedAssets().isEmpty()) {
            selectedAssets = config.getSelectedAssets();
        }

        assetAdapter = new AssetPickerAdapter(context, mAssetLoader, selectedAssets, imageClickListener, config.getStudio());
        folderAdapter = new FolderPickerAdapter(context, mAssetLoader, new OnFolderClickListener() {
            @Override
            public void onFolderClick(Folder folder) {
                foldersState = recyclerView.getLayoutManager().onSaveInstanceState();
                folderClickListener.onFolderClick(folder);
            }
        });
    }

    /**
     * Set item size, column size base on the screen orientation
     */
    public void changeOrientation(int orientation) {
        assetsColumns = orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 5;
        folderColumns = orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 4;

        int columns = isShowingFolder ? folderColumns : assetsColumns;
        layoutManager = new GridLayoutManager(context, columns);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        setItemDecoration(columns);
    }

    private void setItemDecoration(int columns) {
        if (itemOffsetDecoration != null) {
            recyclerView.removeItemDecoration(itemOffsetDecoration);
        }

        int margin = config.getStudio() ? context.getResources().getDimensionPixelSize(R.dimen.menu_margin) : context.getResources().getDimensionPixelSize(R.dimen.imagepicker_item_padding);

        itemOffsetDecoration = new GridSpacingItemDecoration(columns,
                margin,
                false
        );
        recyclerView.addItemDecoration(itemOffsetDecoration);
        layoutManager.setSpanCount(columns);
    }


    public void setOnImageSelectionListener(OnAssetSelectionListener imageSelectionListener) {
        checkAdapterIsInitialized();
        assetAdapter.setOnImageSelectionListener(imageSelectionListener);
    }

    public void updateEl(int position, String correction) {
        assetAdapter.updateEl(position, correction);
    }


    public void removeEl(int position) {
        assetAdapter.removeEl(position);
    }

    public Asset getAsset(int position) {
        return assetAdapter.getAsset(position);
    }

    public int getSelectedSize() {
        return assetAdapter.getSelectedAssets().size();
    }

    public List<Asset> getSelectedAssets() {
        checkAdapterIsInitialized();
        return assetAdapter.getSelectedAssets();
    }

    public Asset getAssetById(int id) {
        checkAdapterIsInitialized();
        return assetAdapter.getAssetById(id);
    }

    public void invalidate() {
        assetAdapter.notifyDataSetChanged();
    }

    public void addSelectedAssets(List<Asset> assets) {
        assetAdapter.addSelected(assets);
    }

    private void checkAdapterIsInitialized() {
        if (assetAdapter == null) {
            throw new IllegalStateException("Must call setupAdapters first!");
        }
    }

    public int getItemCount() {
        return assetAdapter.getItemCount();
    }

    public boolean selectImage() {
        /*if (config.isMultipleMode()) {
            if (assetAdapter.getSelectedAssets().size() >= config.getMaxSize()) {
                String message = String.format(config.getLimitMessage(), config.getMaxSize());
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            if (assetAdapter.getItemCount() > 0) {
                //assetAdapter.removeAllSelected();
            }
        }*/
        return true;
    }

    public void handleBack(OnBackAction action) {
        if (config.isFolderMode() && !isShowingFolder) {
            setFolderAdapter(null);
            action.onBackToFolder();
            return;
        }
        action.onFinishImagePicker();
    }

    public void setAssetAdapter(List<Asset> assets, String title) {
        assetAdapter.setData(assets);
        setItemDecoration(assetsColumns);
        recyclerView.setAdapter(assetAdapter);
        this.title = title;
        isShowingFolder = false;
    }

    public void setFolderAdapter(List<Folder> folders) {
        folderAdapter.setData(folders);
        setItemDecoration(folderColumns);
        recyclerView.setAdapter(folderAdapter);
        isShowingFolder = true;

        if (foldersState != null) {
            layoutManager.setSpanCount(folderColumns);
            recyclerView.getLayoutManager().onRestoreInstanceState(foldersState);
        }
    }

    public String getTitle() {
        if (isShowingFolder) {
            return config.getFolderTitle();
        } else if (config.isFolderMode()) {
            return title;
        } else {
            return config.getImageTitle();
        }
    }

    public boolean isShowDoneButton() {
        return config.isMultipleMode() && (config.isAlwaysShowDoneButton() || assetAdapter.getSelectedAssets().size() > 0);
    }
}
