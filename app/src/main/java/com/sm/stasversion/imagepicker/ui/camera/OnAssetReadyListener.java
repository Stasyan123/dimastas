package com.sm.stasversion.imagepicker.ui.camera;


import com.sm.stasversion.imagepicker.model.Asset;

import java.util.List;

public interface OnAssetReadyListener {
    void onAssetReady(List<Asset> assets);
}
