package com.sm.stasversion

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sm.stasversion.imagepicker.helper.PermissionHelper
import com.sm.stasversion.imagepicker.listener.OnAssetClickListener
import com.sm.stasversion.imagepicker.listener.OnFolderClickListener
import com.sm.stasversion.imagepicker.model.Asset
import com.sm.stasversion.imagepicker.model.Config
import com.sm.stasversion.imagepicker.model.Folder
import com.sm.stasversion.imagepicker.model.SavePath
import com.sm.stasversion.imagepicker.ui.imagepicker.AssetFileLoader
import com.sm.stasversion.imagepicker.ui.imagepicker.ImagePickerPresenter
import com.sm.stasversion.imagepicker.ui.imagepicker.ImagePickerView
import com.sm.stasversion.imagepicker.ui.imagepicker.RecyclerViewManager
import com.sm.stasversion.imagepicker.widget.SnackBarView
import java.util.ArrayList

class MainMenu : AppCompatActivity(), ImagePickerView {

    private var recyclerViewManager: RecyclerViewManager? = null
    private var recyclerViewMenu: RecyclerView? = null
    private var recyclerView: RecyclerView? = null
    private var config: Config = Config()

    private var snackBar: SnackBarView? = null

    private var presenter: ImagePickerPresenter? = null

    private val imageClickListener =
        OnAssetClickListener { view, position, isSelected -> recyclerViewManager!!.selectImage() }

    private val folderClickListener =
        OnFolderClickListener { folder -> setAssetAdapter(folder.images, folder.folderName) }

    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)


        setupConfig()
        setupViews()
        setupComponents()
    }

    override fun onResume() {
        super.onResume()
        getDataWithPermission()
    }

    private fun getDataWithPermission() {

        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        PermissionHelper.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, object :
            PermissionHelper.PermissionAskListener {
            override fun onNeedPermission() {
                PermissionHelper.requestAllPermissions(
                    this@MainMenu,
                    permissions,
                    Config.RC_WRITE_EXTERNAL_STORAGE_PERMISSION
                )
            }

            override fun onPermissionPreviouslyDenied() {
                PermissionHelper.requestAllPermissions(
                    this@MainMenu,
                    permissions,
                    Config.RC_WRITE_EXTERNAL_STORAGE_PERMISSION
                )
            }

            override fun onPermissionDisabled() {
                snackBar!!.show(R.string.imagepicker_msg_no_write_external_storage_permission,
                    View.OnClickListener { PermissionHelper.openAppSettings(this@MainMenu) })
            }

            override fun onPermissionGranted() {
                getData()
            }
        })
    }

    private fun getData() {
        presenter!!.abortLoading()
        presenter!!.loadAssets(true, false)
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerViewMenu = findViewById(R.id.recyclerViewMenu)
        snackBar = findViewById<SnackBarView>(R.id.snackbar)
    }

    private fun setupComponents() {
        recyclerViewManager =
            RecyclerViewManager(recyclerView, config, resources.configuration.orientation)

        recyclerViewManager!!.setupAdapters(imageClickListener, folderClickListener)

        presenter = ImagePickerPresenter(AssetFileLoader(this))
        presenter!!.attachView(this)
    }

    private fun setAssetAdapter(assets: List<Asset>, title: String) {
        recyclerViewManager!!.setAssetAdapter(assets, title)
    }

    override fun showLoading(isLoading: Boolean) {

    }

    override fun finishPickAssets(assets: MutableList<Asset>?) {

    }

    override fun showCapturedAsset(assets: MutableList<Asset>?) {

    }

    override fun showEmpty() {
    }

    override fun showError(throwable: Throwable?) {

    }

    override fun showFetchCompleted(assets: MutableList<Asset>, folders: MutableList<Folder>?) {
        setAssetAdapter(assets, "")
    }

    private fun setupConfig() {
        config.setCameraOnly(false)
        config.setMultipleMode(true)
        config.setFolderMode(true)
        config.setIncludeVideos(true)
        config.setShowCamera(true)
        config.setMaxSize(Config.MAX_SIZE)
        config.setDoneTitle(resources.getString(R.string.imagepicker_action_done))
        config.setFolderTitle(resources.getString(R.string.all))
        config.setImageTitle(resources.getString(R.string.all))
        config.setLimitMessage(resources.getString(R.string.imagepicker_msg_limit_images))
        config.setSavePath(SavePath.DEFAULT)
        config.setAlwaysShowDoneButton(false)
        config.setKeepScreenOn(false)
        config.setSelectedAssets(ArrayList())
    }
}

