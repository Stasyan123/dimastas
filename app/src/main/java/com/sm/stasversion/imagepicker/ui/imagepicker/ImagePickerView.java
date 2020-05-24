package com.sm.stasversion.imagepicker.ui.imagepicker;


import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.model.Folder;
import com.sm.stasversion.imagepicker.ui.common.MvpView;

import java.util.List;

/**
 * Created by hoanglam on 8/17/17.
 */

public interface ImagePickerView extends MvpView {

    void showLoading(boolean isLoading);

    void showFetchCompleted(List<Asset> assets, List<Folder> folders);

    void showError(Throwable throwable);

    void showEmpty();

    void showCapturedAsset(List<Asset> assets);

    void finishPickAssets(List<Asset> assets);

}