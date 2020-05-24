package com.sm.stasversion.imagepicker.listener;


import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.model.Folder;

import java.util.List;

/**
 * Created by hoanglam on 8/17/17.
 */

public interface OnAssetLoaderListener {
    void onAssetLoaded(List<Asset> assets, List<Folder> folders);

    void onFailed(Throwable throwable);
}
