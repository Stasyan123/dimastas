package com.sm.stasversion

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
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
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.google.gson.Gson
import com.sm.stasversion.classes.AdjustConfig
import com.sm.stasversion.classes.DBConfig
import com.sm.stasversion.classes.serializedConfigs
import com.sm.stasversion.crop.BitmapUtils
import com.sm.stasversion.imagepicker.listener.OnAssetSelectionListener
import com.sm.stasversion.imagepicker.ui.imagepicker.*
import com.sm.stasversion.imagepicker.util.isVideoFile
import org.wysaid.common.Common
import org.wysaid.myUtils.FileUtil
import org.wysaid.myUtils.ImageUtil
import org.wysaid.myUtils.MsgUtil
import org.wysaid.nativePort.CGEFFmpegNativeLibrary
import org.wysaid.nativePort.CGENativeLibrary
import org.wysaid.texUtils.CropInfo
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

    protected var mThread: Thread? = null
    protected var mShouldStopThread = false

    protected var textureConfig: AdjustConfig? = null
    protected var test = ""

    private val imageClickListener =
        OnAssetClickListener { view, position, isSelected ->
                val size = recyclerViewManager!!.getSelectedSize()

                if(size == 0) {
                    deactivateBar()
                } else {
                    delete!!.background = getResources().getDrawable(R.drawable.ic_delete_colored)
                    save!!.background = getResources().getDrawable(R.drawable.ic_save_colored)

                    if(!copiedConfig.isEmpty()) {
                        paste!!.background = getResources().getDrawable(R.drawable.ic_paste_edit_colored)
                        paste!!.setTag(1)
                    }

                    save!!.setTag(1)
                    delete!!.setTag(1)

                    if(size == 1 && !isNullOrEmpty(recyclerViewManager!!.getAsset(0).correction)) {
                        copy!!.background = getResources().getDrawable(R.drawable.ic_copy_edit_colored)
                        copy!!.setTag(1)
                    } else {
                        copy!!.background = getResources().getDrawable(R.drawable.ic_copy_edit)
                        copy!!.setTag(-1)
                    }

                    if(size == 1 && isNullOrEmpty(recyclerViewManager!!.getAsset(0).correction)
                        && isNullOrEmpty(recyclerViewManager!!.getAsset(0).crop)) {
                            save!!.background = getResources().getDrawable(R.drawable.ic_save)
                            save!!.setTag(-1)
                    }
                }

                recyclerViewManager!!.selectImage()
            }

    private val folderClickListener =
        OnFolderClickListener { folder -> setAssetAdapter(folder.images, folder.folderName) }

    var mLoadImageCallback: CGENativeLibrary.LoadImageCallback =
        object : CGENativeLibrary.LoadImageCallback {

            //Notice: the 'name' passed in is just what you write in the rule, e.g: 1.jpg
            override fun loadImage(name: String, arg: Any?): Bitmap? {

                Log.d(LOG_TAG, " Loading " + test)
                Log.d(Common.LOG_TAG, " Loading " + test)
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

    val LOG_TAG = "mood"

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

    fun isNullOrEmpty(str: String?): Boolean {
        if (str != null && !str.isEmpty())
            return false
        return true
    }

    private fun deactivateBar() {
        copy!!.background = getResources().getDrawable(R.drawable.ic_copy_edit)
        paste!!.background = getResources().getDrawable(R.drawable.ic_paste_edit)
        save!!.background = getResources().getDrawable(R.drawable.ic_save)
        delete!!.background = getResources().getDrawable(R.drawable.ic_delete)

        copy!!.setTag(-1)
        paste!!.setTag(-1)
        save!!.setTag(-1)
        delete!!.setTag(-1)
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
            val selected = recyclerViewManager!!.getSelectedAssets()

            if (selected.size >= 1) {
                selected.forEach() { el ->
                    threadSync()

                    mThread = Thread(Runnable {
                        test = "Start id - " + el.id
                        Log.d(LOG_TAG, "Start id - " + el.id)

                        if (File(el.path).isVideoFile) {
                            //saveVideo(el)
                            editVideo(el, true)
                        } else {
                            saveImage(el)
                        }
                    })

                    mThread!!.start()
                }
            }
        }

        delete!!.setOnClickListener{
            val selected = recyclerViewManager!!.getSelectedAssets()

            /*if (selected.size >= 1) {
                selected.forEach() { el ->
                    threadSync()

                    mThread = Thread(Runnable {
                        test = "Start id - " + el.id
                        Log.d(LOG_TAG, "Start id - " + el.id)

                        if (File(el.path).isVideoFile) {
                            //saveVideo(el)
                            editVideo(el, false)
                        } else {
                            saveImage(el)
                        }
                    })

                    mThread!!.start()
                }
            }*/

            if(delete!!.getTag() == 1) {
                val selected = recyclerViewManager!!.getSelectedAssets()

                if (selected.size >= 1) {
                    selected.forEach() { el ->
                        db!!.configDao().deleteConfig(el.id.toInt())
                        recyclerViewManager!!.removeEl(el.id.toInt())
                    }
                }

                recyclerViewManager!!.invalidate()

                deactivateBar()

                if(recyclerViewManager!!.getItemCount() == 0) {
                    showEmpty()
                }
            }
        }
    }

    private fun saveImage(el: Asset) {
        val futureBitmap = Glide.with(applicationContext)
            .asBitmap()
            .load(el.path)
            .submit()

        var bmp = futureBitmap.get()

        if(!el.crop.isEmpty()) {
            val cropInfo = Gson().fromJson(el.crop, CropInfo::class.java)
            bmp = serializedConfigs.cropImage(bmp, cropInfo)
        }

        if(!el.correction.isEmpty()) {
            val _configs = serializedConfigs.decryptConfigsStatic(el.correction)
            val rules = serializedConfigs.calculateRules(_configs)

            bmp = CGENativeLibrary.filterImage_MultipleEffects(bmp, rules, _configs.get(0).intensity)
        }

        val s = ImageUtil.saveBitmap(bmp);

        showMsg("The filter is applied! See it: /sdcard/mood/rec_*.jpg")
        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://$s")))

        /*Glide.with(this)
            .asBitmap()
            .load(el.path)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(bmp: Bitmap, transition: Transition<in Bitmap>?) {
                    var bitmap: Bitmap? = null
                    var dstImage: Bitmap? = null

                    if(!el.crop.isEmpty()) {
                        val cropInfo = Gson().fromJson(el.crop, CropInfo::class.java)
                        bitmap = serializedConfigs.cropImage(bmp, cropInfo)
                    } else {
                        bitmap = bmp
                    }

                    if(el.correction.isEmpty()) {
                        dstImage = bitmap
                    } else {
                        val _configs = serializedConfigs.decryptConfigsStatic(el.correction)
                        val rules = serializedConfigs.calculateRules(_configs)

                        dstImage = CGENativeLibrary.filterImage_MultipleEffects(bitmap, rules, _configs.get(0).intensity)
                    }

                    val s = ImageUtil.saveBitmap(dstImage);

                    showMsg("The filter is applied! See it: /sdcard/mood/rec_*.jpg")
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://$s")))
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })*/
    }

    private fun saveVideo(el: Asset) {
        val _configs = serializedConfigs.decryptConfigsStatic(el.correction)
        val rules = serializedConfigs.calculateRules(_configs)

        //String outputFilename = "/sdcard/libCGE/blendVideo.mp4";
        //String inputFileName = "android.resource://" + getPackageName() + "/" + R.raw.fish;
        val outputFilename = FileUtil.getPath() + ".mp4"
        //String inputFileName = FileUtil.getTextContent(CameraDemoActivity.lastVideoPathFileName);
        //String inputFileName = "/storage/9016-4EF8/DCIM/Camera/20200402_124813.mp4";
        //String inputFileName = "/storage/9016-4EF8/DCIM/Camera/20200402_124813_001.mp4"; // 2 sec

        //bmp is used for watermark, (just pass null if you don't want that)
        //and ususally the blend mode is CGE_BLEND_ADDREV for watermarks.
        CGEFFmpegNativeLibrary.generateVideoWithFilter(
            outputFilename,
            el.path,
            rules,
            _configs.get(0).intensity,
            null,
            CGENativeLibrary.TextureBlendMode.CGE_BLEND_ADDREV,
            1.0f,
            false
        )
        Log.d(LOG_TAG, "Done! The file is generated at: \$outputFilename")

        sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://$outputFilename")
            )
        )

            /*if(el.crop.isEmpty()) {
                sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse("file://$outputFilename")
                    )
                )
                showMsg("The filter is applied! See it: /sdcard/mood/rec_*.jpg")
            } else {
                editVideo(outputFilename, el)
            }*/

    }

    private fun editVideo(el: Asset, check: Boolean) {
        var scaleRule = ""
        var rotateRule = ""
        var straighteningRule = ""
        var mainRule = ""
        var cropRule = ""
        var flipRule = ""

        var cropInfo: CropInfo? = null;

        val outputFilename = FileUtil.getPath() + ".mp4"
        //val outputFilename = FileUtil.getPath() + "/blendVideo4.mp4";
        //val outputFilename = "/storage/emulated/0/Pictures/Telegram/VID_20200626_125043_721.mp4";
        //val outputFilename1 = FileUtil.getPath() + "/blendVideo7797.mp4";

        /*
        angl = 15
scale = 1.4269915
         */

        if(!el.crop.isEmpty()) {
            cropInfo = Gson().fromJson(el.crop, CropInfo::class.java)
        }

        val ffmpeg = FFmpeg.getInstance(this)

        //510:720
        //"rotate=15*PI/180"
        //"crop=out_w:out_h:x:y"
        //hflip
        //vflip
        //"-filter:v", "scale=" + newWidth + ":" + newHeight + ",rotate=15*PI/180"
        //"-i " + el.path + " rotate=PI/2 -c:a copy " + outputFilename
        //"rotate=15*PI/180:510:1280",
        //"scale=1743.18:980.6,rotate=15*PI/180:1280:720"
        //"scale=1566:882,rotate=7.5*PI/180:1280:720"
        //crop=720:720:0:230

        val b = if(check) "rotate=15*PI/180:hypot(iw,ih):ow" else "rotate=2*PI*t:ow='min(iw,ih)/sqrt(2)':oh=ow:c=none"

        val newWidth = cropInfo!!.videoWidth * cropInfo.scale + cropInfo.postRotate
        val newHeight = cropInfo.videoHeight * cropInfo.scale + cropInfo.postRotate

        val rect =
            BitmapUtils.getRectFromPoints(
                cropInfo.points,
                cropInfo.originalW,
                cropInfo.originalH,
                false,
                1,
                1
            );

        val left = rect.left * (cropInfo.videoWidth / cropInfo.originalW.toFloat())
        val top = rect.top * (cropInfo.videoHeight / cropInfo.originalH.toFloat())
        var width = rect.width() * (cropInfo.videoWidth / cropInfo.originalW.toFloat())
        var height = rect.height() * (cropInfo.videoHeight / cropInfo.originalH.toFloat())

        if(cropInfo.width != width.toInt() || cropInfo.height != height.toInt()) {
            cropRule = "crop=" + width + ":" + height + ":" + left + ":" + top
        }

        if(cropInfo.rotation != 0 || cropInfo.rotation != 360) {
            when (cropInfo.rotation) {
                90 -> {
                    rotateRule = "transpose=1"
                    height = width
                }
                180 -> rotateRule = "transpose=1,transpose=1"
                270 -> {
                    rotateRule = "transpose=1,transpose=1,transpose=1"
                    width = rect.height() * (cropInfo.videoHeight / cropInfo.originalH.toFloat())
                }
            }
        }

        if(cropInfo.postRotate != 0.0f) {
            scaleRule = "scale=" + width * cropInfo.scale + ":" + height * cropInfo.scale
            straighteningRule = "rotate=" + cropInfo.postRotate + "*PI/180:" + width + ":" + height
        }
        if(!isNullOrEmpty(scaleRule)) {
            mainRule += if(isNullOrEmpty(mainRule)) "" else ","
            mainRule += scaleRule + "," + straighteningRule
        }
        if(!isNullOrEmpty(cropRule)) {
            mainRule += if(isNullOrEmpty(mainRule)) "" else "," + cropRule
        }
        if(!isNullOrEmpty(rotateRule)) {
            mainRule += rotateRule
        }
        if(cropInfo.flipVert) {
            flipRule += if(isNullOrEmpty(mainRule)) "" else ",vflip"
        }
        if(cropInfo.flipHor) {
            flipRule += if(isNullOrEmpty(mainRule)) "" else ",hflip"
        }

        if(!isNullOrEmpty(mainRule)) {
            try {
                ffmpeg.execute(arrayOf("-i", el.path, "-filter:v", mainRule, "-c:a", "copy", outputFilename), object: ExecuteBinaryResponseHandler() {
                    override fun onStart() {
                        super.onStart()
                        Log.d(LOG_TAG, "onStart")
                    }

                    override fun onProgress(message: String?) {
                        super.onProgress(message)
                        Log.d(LOG_TAG, message)
                    }

                    override fun onFailure(message: String?) {
                        super.onFailure(message)
                        Log.d(LOG_TAG, "onFailure")
                    }

                    override fun onSuccess(message: String?) {
                        super.onSuccess(message)
                        Log.d(LOG_TAG, "onOkey")
                        //File(name).delete()

                        sendBroadcast(
                            Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse("file://$outputFilename")
                            )
                        )
                    }

                    override fun onFinish() {
                        super.onFinish()
                        Log.d(LOG_TAG, "onFinish")
                    }
                })
            } catch (e: FFmpegCommandAlreadyRunningException) {
                Log.d(LOG_TAG, "onFinish")
            }
        } else {
            sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://$outputFilename")
                )
            )
        }
    }

    protected fun threadSync() {

        if (mThread != null && mThread!!.isAlive()) {
            mShouldStopThread = true

            try {
                mThread!!.join()
                mThread = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        mShouldStopThread = false
    }

    private fun getCameraPhotoOrientation(context: Context, imageUri: String): Float{
        var rotate = 0.0f
        try {
            var exif = ExifInterface(imageUri);
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    rotate = 270.0f;
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    rotate = 180.0f;
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    rotate = 90.0f;
                }
                else -> { // Note the block

                }
            }

            Log.i(LOG_TAG, "Exif orientation: " + orientation);
            Log.i(LOG_TAG, "Rotate value: " + rotate);
        } catch (e: Exception) {
            e.printStackTrace();
        }
        return rotate;
    }

    protected fun showMsg(msg: String) {

        this@MainMenu.runOnUiThread(Runnable {
            MsgUtil.toastMsg(
                this@MainMenu,
                msg,
                Toast.LENGTH_SHORT
            )
        })
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

