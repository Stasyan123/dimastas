package com.sm.stasversion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sm.stasversion.classes.AppDatabase
import com.sm.stasversion.imagepicker.helper.PermissionHelper
import com.sm.stasversion.imagepicker.listener.OnAssetClickListener
import com.sm.stasversion.imagepicker.listener.OnFolderClickListener
import com.sm.stasversion.imagepicker.model.Asset
import com.sm.stasversion.imagepicker.model.Config
import com.sm.stasversion.imagepicker.model.Folder
import com.sm.stasversion.imagepicker.model.SavePath
import androidx.room.Room
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.google.gson.Gson
import com.sm.stasversion.classes.DBConfig
import com.sm.stasversion.imagepicker.listener.OnAssetSelectionListener
import com.sm.stasversion.imagepicker.ui.imagepicker.*
import com.sm.stasversion.imagepicker.util.isVideoFile
import org.wysaid.common.Common
import org.wysaid.nativePort.CGENativeLibrary
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.Collections.list
import kotlin.concurrent.schedule


class MainMenu : AppCompatActivity(), ImagePickerView {

    private var recyclerViewManager: RecyclerViewManager? = null
    private var recyclerViewMenu: RecyclerView? = null
    private var recyclerView: RecyclerView? = null
    private var emptyLayout: View? = null
    private var config: Config = Config()
    private var db: AppDatabase? = null
    private var assetsList = ArrayList<Asset>()

    private var copy: View? = null
    private var paste: View? = null
    private var save: View? = null
    private var delete: View? = null

    public var copiedConfig: String = "";
    private var message: TextView? = null;

    private var presenter: ImagePickerPresenter? = null

    private val imageClickListener =
        OnAssetClickListener { view, position, isSelected ->
                val size = recyclerViewManager!!.getSelectedSize()

                if(size == 0) {
                    copy!!.background = getResources().getDrawable(R.drawable.ic_copy_edit)
                    paste!!.background = getResources().getDrawable(R.drawable.ic_paste_edit)
                    save!!.background = getResources().getDrawable(R.drawable.ic_save)
                    delete!!.background = getResources().getDrawable(R.drawable.ic_delete)

                    copy!!.setTag(-1)
                    paste!!.setTag(-1)
                    save!!.setTag(-1)
                    delete!!.setTag(-1)
                } else {
                    if(size == 1 && !recyclerViewManager!!.getAsset(0).correction.isEmpty()) {
                        copy!!.background = getResources().getDrawable(R.drawable.ic_copy_edit_colored)
                        copy!!.setTag(1)
                    } else {
                        copy!!.background = getResources().getDrawable(R.drawable.ic_copy_edit)
                        copy!!.setTag(-1)
                    }

                    delete!!.background = getResources().getDrawable(R.drawable.ic_delete_colored)
                    save!!.background = getResources().getDrawable(R.drawable.ic_save_colored)

                    if(!copiedConfig.isEmpty()) {
                        paste!!.background = getResources().getDrawable(R.drawable.ic_paste_edit_colored)
                        paste!!.setTag(1)
                    }

                    save!!.setTag(1)
                    delete!!.setTag(1)
                }

                recyclerViewManager!!.selectImage()
            }

    private val folderClickListener =
        OnFolderClickListener { folder -> setAssetAdapter(folder.images, folder.folderName) }

    var mLoadImageCallback: CGENativeLibrary.LoadImageCallback =
        object : CGENativeLibrary.LoadImageCallback {

            //Notice: the 'name' passed in is just what you write in the rule, e.g: 1.jpg
            override fun loadImage(name: String, arg: Any?): Bitmap? {

                Log.i(Common.LOG_TAG, "Loading file: $name")
                val am = assets
                val `is`: InputStream
                try {
                    `is` = am.open(name)
                } catch (e: IOException) {
                    Log.e(Common.LOG_TAG, "Can not open file $name")
                    return null
                }

                return BitmapFactory.decodeStream(`is`)
            }

            override fun loadImageOK(bmp: Bitmap, arg: Any) {
                Log.i(Common.LOG_TAG, "Loading bitmap over, you can choose to recycle or cache")
                bmp.recycle()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, 1)

        initDB()
        setupConfig()
        setupViews()
        setupComponents()
    }

    override fun onResume() {
        super.onResume()
        getDataWithPermission()
    }

    private fun initDB() {
        db = Room.databaseBuilder(this.applicationContext, AppDatabase::class.java, "mood_v4")
            .allowMainThreadQueries().build()
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
                PermissionHelper.openAppSettings(this@MainMenu)
            }

            override fun onPermissionGranted() {
                getData()
            }
        })
    }

    private fun getData() {
        presenter!!.abortLoading()
        presenter!!.loadConfigAssets(db)
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerViewMenu = findViewById(R.id.recyclerViewMenu)
        emptyLayout = findViewById(R.id.layout_empty)

        copy = findViewById(R.id.copy_edit_icon)
        paste = findViewById(R.id.paste_edit_icon)
        save = findViewById(R.id.save_icon)
        delete = findViewById(R.id.delete_icon)

        message = findViewById(R.id.bottom_message)

        val add = findViewById<ImageView>(R.id.topAdd)
        add.setOnClickListener{
            val t = db!!.configDao().size()

            AssetPicker.with(this)
                .setFolderMode(true)
                .setIncludeVideos(true)
                .setVideoOrImagePickerTitle("Capture image or video")
                .setCameraOnly(false)
                .setFolderTitle(resources.getString(R.string.all))
                .setMultipleMode(false)
                .setSelectedImages(assetsList)
                .setMaxSize(10)
                .setBackgroundColor("#000000")
                .setToolbarColor("#ffffff")
                .setToolbarTextColor("#000000")
                .setAlwaysShowDoneButton(false)
                .setRequestCode(100)
                .setKeepScreenOn(true)
                .start()
        }

        val menu = findViewById<ImageView>(R.id.topMenu)
        menu.setOnClickListener{
            val intent = Intent(this@MainMenu, SubscribeActivity::class.java)
            startActivity(intent)
        }

        copy!!.setOnClickListener{
            if(copy!!.getTag() == 1) {
                val selected = recyclerViewManager!!.getSelectedAssets()

                if(selected.size == 1) {
                    copiedConfig = selected.get(0).correction

                    message!!.setText(getResources().getString(R.string.copy))
                    message!!.alpha = 0.9f

                    Timer("popup", false).schedule(700) {
                        message!!.alpha = 0f
                    }
                }
            }
        }

        paste!!.setOnClickListener{
            if(paste!!.getTag() == 1) {
                val selected = recyclerViewManager!!.getSelectedAssets()

                if (selected.size >= 1 && !copiedConfig.isEmpty()) {
                    selected.forEach() { el ->
                        db!!.configDao().updateConfig(el.id.toInt(), copiedConfig)
                        recyclerViewManager!!.updateEl(el.position.toInt(), copiedConfig)
                    }
                }

                message!!.setText(getResources().getString(R.string.paste))
                message!!.alpha = 0.9f

                Timer("popup", false).schedule(700) {
                    message!!.alpha = 0f
                }

                recyclerViewManager!!.invalidate()
            }
        }

        save!!.setOnClickListener{

        }

        delete!!.setOnClickListener{
            if(delete!!.getTag() == 1) {
                val selected = recyclerViewManager!!.getSelectedAssets()

                if (selected.size >= 1) {
                    selected.forEach() { el ->
                        db!!.configDao().deleteConfig(el.id.toInt())
                        recyclerViewManager!!.removeEl(el.id.toInt())
                    }
                }

                recyclerViewManager!!.invalidate()

                if(recyclerViewManager!!.getItemCount() == 0) {
                    showEmpty()
                }
            }
        }
    }

    private fun setupComponents() {
        recyclerViewManager =
            RecyclerViewManager(recyclerView, config, resources.configuration.orientation)

        recyclerViewManager!!.setupAdapters(imageClickListener, folderClickListener)
        recyclerViewManager!!.setOnImageSelectionListener(OnAssetSelectionListener { assets ->
            val selected = recyclerViewManager!!.getSelectedAssets()

            if(selected.size == 0 || (selected.size == 1 && assets[0].id == selected[0].id)) {
                var intent = Intent(this@MainMenu, EditImageActivity::class.java)

                val f = File(assets[0].path)
                val uri = Uri.fromFile(f)

                if (f.isVideoFile) { // If file is an Image
                    intent = Intent(this@MainMenu, NewVideoOverviewActivity::class.java)
                }

                if(!assets[0].correction.isEmpty()) {

                }

                intent.putExtra("file", uri)
                intent.putExtra("imgId", assets[0].id.toInt())
                intent.putExtra("configs", assets[0].correction)
                intent.putExtra("crop", assets[0].crop)

                startActivity(intent)
            }
        })

        presenter = ImagePickerPresenter(AssetFileLoader(this))
        presenter!!.attachView(this)
    }

    private fun setAssetAdapter(assets: List<Asset>, title: String) {
        recyclerViewManager!!.setAssetAdapter(assets, title)
    }

    override fun showLoading(isLoading: Boolean) {
        recyclerView!!.setVisibility(if (isLoading) View.GONE else View.VISIBLE)
    }

    override fun finishPickAssets(assets: MutableList<Asset>?) {

    }

    override fun showCapturedAsset(assets: MutableList<Asset>?) {

    }

    public fun hideEmpty() {
        emptyLayout!!.setVisibility(View.GONE)
        recyclerView!!.setVisibility(View.VISIBLE)
    }

    override fun showEmpty() {
        recyclerView!!.setVisibility(View.GONE)
        emptyLayout!!.setVisibility(View.VISIBLE)
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
        config.setMaxSize(10)
        config.setDoneTitle(resources.getString(R.string.imagepicker_action_done))
        config.setFolderTitle(resources.getString(R.string.all))
        config.setImageTitle(resources.getString(R.string.all))
        config.setLimitMessage(resources.getString(R.string.imagepicker_msg_limit_images))
        config.setSavePath(SavePath.DEFAULT)
        config.setAlwaysShowDoneButton(false)
        config.setKeepScreenOn(false)
        config.setSelectedAssets(ArrayList())
        config.setIsStudio(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Config.RC_PICK_ASSETS && resultCode == Activity.RESULT_OK && data != null) {
            assetsList = data.getParcelableArrayListExtra(Config.EXTRA_ASSETS)

            val configs = ArrayList<DBConfig>()

            assetsList.forEach() {el ->
                val config = DBConfig()

                config.path = el.path
                config.name = el.name

                configs.add(config)
            }

            val ids = db!!.configDao().configsIds

            db!!.configDao().insertConfigs(configs)

            if(recyclerViewManager!!.getItemCount() == 0) {
                hideEmpty()
            }
        }
        //super.onActivityResult(requestCode, resultCode, data);
    }
}

