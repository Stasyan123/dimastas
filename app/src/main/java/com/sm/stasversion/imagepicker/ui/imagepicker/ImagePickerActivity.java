package com.sm.stasversion.imagepicker.ui.imagepicker;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.Toast;

import com.sm.stasversion.R;
import com.sm.stasversion.imagepicker.adapter.FolderPickerAdapter;
import com.sm.stasversion.imagepicker.adapter.FolderPickerMenuAdapter;
import com.sm.stasversion.imagepicker.helper.CameraHelper;
import com.sm.stasversion.imagepicker.helper.LogHelper;
import com.sm.stasversion.imagepicker.helper.PermissionHelper;
import com.sm.stasversion.imagepicker.listener.OnAssetClickListener;
import com.sm.stasversion.imagepicker.listener.OnAssetSelectionListener;
import com.sm.stasversion.imagepicker.listener.OnBackAction;
import com.sm.stasversion.imagepicker.listener.OnFolderClickListener;
import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.model.Config;
import com.sm.stasversion.imagepicker.model.Folder;
import com.sm.stasversion.imagepicker.widget.ImagePickerToolbar;
import com.sm.stasversion.imagepicker.widget.ProgressWheel;
import com.sm.stasversion.imagepicker.widget.SnackBarView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hoanglam on 7/31/16.
 */
public class ImagePickerActivity extends AppCompatActivity implements ImagePickerView {

    private ImagePickerToolbar toolbar;
    private RecyclerViewManager recyclerViewManager;
    private RecyclerView recyclerViewMenu;
    private RecyclerView recyclerView;
    private ProgressWheel progressWheel;
    private View emptyLayout;
    private View overlay;
    private View menu;
    private ScrollView scroll_folders;
    private SnackBarView snackBar;

    private Config config;
    private Handler handler;
    private ContentObserver observer;
    private ImagePickerPresenter presenter;
    private LogHelper logger = LogHelper.getInstance();


    private OnAssetClickListener imageClickListener = new OnAssetClickListener() {
        @Override
        public boolean onAssetClick(View view, int position, boolean isSelected) {
            return recyclerViewManager.selectImage();
        }
    };

    private OnFolderClickListener folderClickListener = new OnFolderClickListener() {
        @Override
        public void onFolderClick(Folder folder) {
            setAssetAdapter(folder.getImages(), folder.getFolderName());
        }
    };

    private View.OnClickListener backClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            onBackPressed();
        }
    };

    private View.OnClickListener cameraClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            captureImageWithPermission();
        }
    };

    private View.OnClickListener doneClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            onDone();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        config = intent.getParcelableExtra(Config.EXTRA_CONFIG);
        if (config.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.imagepicker_activity_picker);

        setupView();
        setupComponents();
        setupToolbar();
    }

    private void setupView() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerViewMenu = findViewById(R.id.recyclerViewMenu);
        progressWheel = findViewById(R.id.progressWheel);
        emptyLayout = findViewById(R.id.layout_empty);
        snackBar = findViewById(R.id.snackbar);
        overlay = findViewById(R.id.overlay);
        menu = findViewById(R.id.image_toolbar_menu);
        scroll_folders = findViewById(R.id.scroll_folders);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(config.getStatusBarColor());
        }

        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlay.setVisibility(View.GONE);
                scroll_folders.animate()
                        .translationY(-scroll_folders.getHeight())
                        .alpha(0.0f)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                //view.setVisibility(View.GONE);
                            }
                        });
            }
        });

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlay.setVisibility(View.VISIBLE);
                scroll_folders.animate()
                        .translationY(0)
                        .alpha(1.0f)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                //view.setVisibility(View.GONE);
                            }
                        });
            }
        });

        progressWheel.setBarColor(config.getProgressBarColor());
        findViewById(R.id.container).setBackgroundColor(config.getBackgroundColor());
    }

    private void setupComponents() {
        LinearLayoutManager llmTools = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        recyclerViewMenu.setLayoutManager(llmTools);

        recyclerViewManager = new RecyclerViewManager(recyclerView, config, getResources().getConfiguration().orientation);
        recyclerViewManager.setupAdapters(imageClickListener, folderClickListener);
        recyclerViewManager.setOnImageSelectionListener(new OnAssetSelectionListener() {
            @Override
            public void onSelectionUpdate(List<Asset> assets) {
                invalidateToolbar();
                if (!config.isMultipleMode() && !assets.isEmpty()) {
                    onDone();
                }
            }
        });

        presenter = new ImagePickerPresenter(new AssetFileLoader(this));
        presenter.attachView(this);
    }

    private void setupToolbar() {
        toolbar.config(config);
        toolbar.setOnBackClickListener(backClickListener);
        toolbar.setOnCameraClickListener(cameraClickListener);
        toolbar.setOnDoneClickListener(doneClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDataWithPermission();
    }


    private void setAssetAdapter(List<Asset> assets, String title) {
        recyclerViewManager.setAssetAdapter(assets, title);
        invalidateToolbar();
    }

    private void setFolderAdapter(List<Folder> folders) {

        AssetLoader mAssetLoader = new AssetLoader();

        FolderPickerMenuAdapter folderAdapter = new FolderPickerMenuAdapter(this, mAssetLoader, new OnFolderClickListener() {
            @Override
            public void onFolderClick(Folder folder) {
                setAssetAdapter(folder.getImages(), folder.getFolderName());
            }
        });
        folderAdapter.setData(folders);
        recyclerViewMenu.setAdapter(folderAdapter);

        recyclerViewManager.setFolderAdapter(folders);
        invalidateToolbar();
    }

    private void invalidateToolbar() {
        toolbar.setTitle(recyclerViewManager.getTitle());
        toolbar.showDoneButton(recyclerViewManager.isShowDoneButton());

    }

    private void onDone() {
        presenter.onDoneSelectAssets(recyclerViewManager.getSelectedAssets());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recyclerViewManager.changeOrientation(newConfig.orientation);
    }


    private void getDataWithPermission() {

        final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        PermissionHelper.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, new PermissionHelper.PermissionAskListener() {
            @Override
            public void onNeedPermission() {
                PermissionHelper.requestAllPermissions(ImagePickerActivity.this, permissions, Config.RC_WRITE_EXTERNAL_STORAGE_PERMISSION);
            }

            @Override
            public void onPermissionPreviouslyDenied() {
                PermissionHelper.requestAllPermissions(ImagePickerActivity.this, permissions, Config.RC_WRITE_EXTERNAL_STORAGE_PERMISSION);
            }

            @Override
            public void onPermissionDisabled() {
                snackBar.show(R.string.imagepicker_msg_no_write_external_storage_permission, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PermissionHelper.openAppSettings(ImagePickerActivity.this);
                    }
                });
            }

            @Override
            public void onPermissionGranted() {
                getData();
            }
        });
    }

    private void getData() {
        presenter.abortLoading();
        presenter.loadAssets(config.isIncludeVideos(), config.isFolderMode());
    }


    private void captureImageWithPermission() {

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        PermissionHelper.checkPermission(this, Manifest.permission.CAMERA, new PermissionHelper.PermissionAskListener() {
            @Override
            public void onNeedPermission() {
                PermissionHelper.requestAllPermissions(ImagePickerActivity.this, permissions, Config.RC_CAMERA_PERMISSION);
            }

            @Override
            public void onPermissionPreviouslyDenied() {
                PermissionHelper.requestAllPermissions(ImagePickerActivity.this, permissions, Config.RC_CAMERA_PERMISSION);
            }

            @Override
            public void onPermissionDisabled() {
                snackBar.show(R.string.imagepicker_msg_no_camera_permission, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PermissionHelper.openAppSettings(ImagePickerActivity.this);
                    }
                });
            }

            @Override
            public void onPermissionGranted() {
                captureImage();
            }
        });
    }


    private void captureImage() {
        if (!CameraHelper.checkCameraAvailability(this)) {
            return;
        }
        presenter.captureImage(this, config, Config.RC_CAPTURE_IMAGE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Config.RC_CAPTURE_IMAGE && resultCode == RESULT_OK) {
            presenter.finishCaptureImage(this, data, config);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case Config.RC_WRITE_EXTERNAL_STORAGE_PERMISSION: {
                if (PermissionHelper.hasGranted(grantResults)) {
                    logger.d("Write External permission granted");
                    getData();
                    return;
                }
                logger.e("Permission not granted: results len = " + grantResults.length +
                        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
                finish();
            }
            case Config.RC_CAMERA_PERMISSION: {
                if (PermissionHelper.hasGranted(grantResults)) {
                    logger.d("Camera permission granted");
                    captureImage();
                    return;
                }
                logger.e("Permission not granted: results len = " + grantResults.length +
                        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
                break;
            }
            default: {
                logger.d("Got unexpected permission result: " + requestCode);
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (handler == null) {
            handler = new Handler();
        }
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                getData();
            }
        };
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, observer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (presenter != null) {
            presenter.abortLoading();
            presenter.detachView();
        }

        if (observer != null) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    /*@Override
    public void onBackPressed() {
        moveTaskToBack(true);
        finish();
        System.exit(-1);
    }*/

    /**
     * MVP view methods
     */

    @Override
    public void showLoading(boolean isLoading) {
        progressWheel.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);
    }

    @Override
    public void showFetchCompleted(List<Asset> assets, List<Folder> folders) {
        /*if (config.isFolderMode()) {
            setFolderAdapter(folders);
        } else {*/
            setAssetAdapter(assets, config.getImageTitle());

        AssetLoader mAssetLoader = new AssetLoader();

        FolderPickerMenuAdapter folderAdapter = new FolderPickerMenuAdapter(this, mAssetLoader, new OnFolderClickListener() {
            @Override
            public void onFolderClick(Folder folder) {
                overlay.setVisibility(View.GONE);
                scroll_folders.animate()
                        .translationY(-scroll_folders.getHeight())
                        .alpha(0.0f)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                //view.setVisibility(View.GONE);
                            }
                        });

                setAssetAdapter(folder.getImages(), folder.getFolderName());
            }
        });
        folderAdapter.setData(folders);
        recyclerViewMenu.setAdapter(folderAdapter);
        //}
    }

    @Override
    public void showError(Throwable throwable) {
        String message = getString(R.string.imagepicker_error_unknown);
        if (throwable != null && throwable instanceof NullPointerException) {
            message = getString(R.string.imagepicker_error_images_not_exist);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showEmpty() {
        progressWheel.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void showCapturedAsset(List<Asset> assets) {
        boolean shouldSelect = recyclerViewManager.selectImage();
        if (shouldSelect) {
            recyclerViewManager.addSelectedAssets(assets);
        }
        getDataWithPermission();
    }

    @Override
    public void finishPickAssets(List<Asset> assets) {
        Intent data = new Intent();
        data.putParcelableArrayListExtra(Config.EXTRA_ASSETS, (ArrayList<? extends Parcelable>) assets);
        setResult(RESULT_OK, data);
        finish();
    }
}
