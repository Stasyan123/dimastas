package com.sm.stasversion.imagepicker.ui.camera;

import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.ui.common.MvpView;

import java.util.List;

/**
 * Created by hoanglam on 8/22/17.
 */

public interface CameraView extends MvpView {

    void finishPickAssets(List<Asset> assets);
}
