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
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.google.gson.Gson
import com.sm.stasversion.classes.*
import com.sm.stasversion.crop.BitmapUtils
import com.sm.stasversion.imagepicker.listener.OnAssetSelectionListener
import com.sm.stasversion.imagepicker.ui.imagepicker.*
import com.sm.stasversion.imagepicker.util.isVideoFile
import com.sm.stasversion.interfaces.CGEImageResponseHandler
import kotlinx.android.synthetic.main.activity_edit_image.*
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
import java.util.Collections.replaceAll
import kotlin.concurrent.schedule
import java.util.regex.Pattern


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
    public var progressBar: ProgressBar? = null

    public var items: MutableList<ProgressItem>? = null

    protected var mThread: Thread? = null
    protected var mShouldStopThread = false

    protected var textureConfig: AdjustConfig? = null
    protected var framesCount = 0
    protected var selectedSize = 0
    protected var savedCount = 0

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

    val mSaveVideoCallback: CGEFFmpegNativeLibrary.SaveVideoCallback =
        object : CGEFFmpegNativeLibrary.SaveVideoCallback {
            override fun progress(pr: Int, id: Int) {
                changeProgress(id.toLong(), pr, false)

                /*val progress = (Math.min(
                    1.0f,
                    pr.toFloat() / framesCount.toFloat()
                ) * 100.0)

                progressBar!!.setProgress(progress.toInt())*/
            }
        }

    val mSaveImageCallback: CGENativeLibrary.SaveImageCallback =
        object : CGENativeLibrary.SaveImageCallback {
            override fun progress(pr: Int) {

            }
        }

    var mLoadImageCallback: CGENativeLibrary.LoadImageCallback =
        object : CGENativeLibrary.LoadImageCallback {

            //Notice: the 'name' passed in is just what you write in the rule, e.g: 1.jpg
            override fun loadImage(name: String, arg: Any?, id: Int): Bitmap? {

                Log.i(Common.LOG_TAG, "Loading file: $name")
                val am = assets
                val `is`: InputStream
                try {
                    `is` = am.open(name)
                } catch (e: IOException) {
                    Log.e(Common.LOG_TAG, "Can not open file $name")
                    return null
                }

                var textConfig: AdjustConfig? = null

                if(id != -1) {
                    val asset = recyclerViewManager!!.getAssetById(id)

                    val _configs = serializedConfigs.decryptConfigsStatic(asset.correction)

                    textConfig = _configs.get(10)
                }

                if(textConfig != null && name.equals(textConfig.name)) {
                    return BitmapFactory.decodeStream(`is`).changeBmp(textConfig.horizontal[1], textConfig.vertical[1], textConfig.rotate[1])
                } else {
                    return BitmapFactory.decodeStream(`is`)
                }
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
        CGENativeLibrary.setImageCallback(mSaveImageCallback)
        CGEFFmpegNativeLibrary.setSaveVideoCallback(mSaveVideoCallback)

        initDB()
        setupConfig()
        setupViews()
        setupComponents()
    }

    override fun onResume() {
        super.onResume()

        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, 1)
        getDataWithPermission()
    }

    fun isNullOrEmpty(str: String?): Boolean {
        if (str != null && !str.isEmpty())
            return false
        return true
    }

    private fun Bitmap.changeBmp(x: Float, y: Float, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postScale(x, y, width / 2f, height / 2f) }
        matrix.apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
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
        progressBar = findViewById(R.id.savedProgress)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerViewMenu = findViewById(R.id.recyclerViewMenu)
        emptyLayout = findViewById(R.id.layout_empty)

        copy = findViewById(R.id.copy_edit_icon)
        paste = findViewById(R.id.paste_edit_icon)
        save = findViewById(R.id.save_icon)
        delete = findViewById(R.id.delete_icon)

        message = findViewById(R.id.bottom_message)

        val title = findViewById<View>(R.id.new_title)
        title.setOnClickListener{
            startGallery()
        }

        val add = findViewById<ImageView>(R.id.topAdd)
        add.setOnClickListener{
            startGallery()
        }

        val menu = findViewById<ImageView>(R.id.topMenu)
        menu.setOnClickListener{
            val intent = Intent(this@MainMenu, MenuActivity::class.java)
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
                switchOverlay(true)
                initProgressList(selected)

                selected.forEach() { el ->
                    threadSync()

                    mThread = Thread(Runnable {
                        Log.d(LOG_TAG, "Start id - " + el.id)

                        if (File(el.path).isVideoFile) {
                            if(!isNullOrEmpty(el.correction) || !isNullOrEmpty(el.crop)) {
                                getFramesCount(el)
                            } else {
                                checkCount()
                                progressBar!!.max -= 100
                            }
                        } else {
                            setMax(el.id.toInt(), 5)
                            saveImage(el)
                        }
                    })

                    mThread!!.start()
                }
            }
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

                deactivateBar()

                if(recyclerViewManager!!.getItemCount() == 0) {
                    showEmpty()
                }
            }
        }
    }

    private fun startGallery() {
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

    private fun switchOverlay(show: Boolean) {
        val overlay = findViewById<View>(R.id.overlay)

        if(show) {
            overlay.visibility = View.VISIBLE
        } else {
            overlay.visibility = View.GONE
        }
    }

    private fun showImagesSaved() {
        progressBar!!.setProgress(0)
        progressBar!!.visibility = View.GONE

        val overlay = findViewById<View>(R.id.overlay)
        val saved = findViewById<TextView>(R.id.savedButton)
        saved.visibility = View.VISIBLE

        Handler().postDelayed({
            saved.visibility = View.GONE
            overlay.visibility = View.GONE
        }, 400)
    }

    private fun initProgressList(assets: MutableList<Asset>) {
        progressBar!!.progress = 0
        progressBar!!.visibility = View.VISIBLE

        selectedSize = assets.size
        savedCount = 0

        items = mutableListOf()

        assets.forEach() { el ->
            items!!.add(ProgressItem(el.id.toInt(), 0, 0 ,0))
        }

        progressBar!!.max = 100 * selectedSize

        save!!.isClickable = false
        delete!!.isClickable = false
        copy!!.isClickable = false
        paste!!.isClickable = false
    }

    private fun setMax(id: Int, max: Int) {
        items!!.forEach() { el ->
            if(el.id == id) {
                el.max = max
            }
        }
    }

    private fun setCurrent(id: Int) {
        items!!.forEach() { el ->
            if(el.id == id) {
                el.correctionCurrent = el.current
            }
        }
    }

    private fun getProgressItem(id: Int): ProgressItem? {
        items!!.forEach() { el ->
            if(el.id == id) {
                return el
            }
        }

        return null
    }

    private fun changeProgress(id: Long, progress: Int, isMax: Boolean) {
        var allProgress = 0

        items!!.forEach() { el ->
            if(el.id == id.toInt()) {
                if(isMax) {
                    el.current = el.max
                } else {
                    el.current = if(el.correctionCurrent != 0) progress + el.correctionCurrent else progress
                }
            }

            allProgress += (Math.min(
                1.0f,
                el.current.toFloat() / el.max.toFloat()
            ) * 100.0).toInt()
        }

        Log.d(LOG_TAG, "id - " + id)
        Log.d(LOG_TAG, "progress - " + progress)

        progressBar!!.setProgress(allProgress)
    }

    private fun saveImage(el: Asset) {

        val callback = object: CGEImageResponseHandler {
            override fun onSuccess(s: String) {
                Log.d(LOG_TAG, "on Success Image Filter")
                sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse("file://$s")
                    )
                )

                Log.d(LOG_TAG, "Finish id - " + el.id)

                checkCount()
            }

            override fun onProgress(id: Long, progress: Int) {
                changeProgress(id, progress, false)
            }

            override fun onFailure(message: String?) {

            }
        }

        val cge_image = CGEImageAcyncTask(applicationContext, el.path, el, callback, el.id.toInt())

        try {
            cge_image.execute()
        } catch (e: java.lang.Exception) {
            Log.d(LOG_TAG, "onError")
        } finally {

        }
    }

    private fun checkCount() {
        savedCount++
        if(savedCount == selectedSize) {
            runOnUiThread{
                showImagesSaved()
            }

            copy!!.isClickable = true
            paste!!.isClickable = true
            delete!!.isClickable = true
            save!!.isClickable = true
        }
    }

    private fun saveVideo(el: Asset) {
        val _configs = serializedConfigs.decryptConfigsStatic(el.correction)
        val rules = serializedConfigs.calculateRules(_configs)

        val outputFilename = FileUtil.getPath() + ".mp4"

        val callback = object: ExecuteBinaryResponseHandler() {
            override fun onStart() {
                super.onStart()
                Log.d(LOG_TAG, "onStart")
            }

            override fun onProgress(message: String) {
                super.onProgress(message)
            }

            override fun onFailure(message: String?) {
                super.onFailure(message)
                Log.d(LOG_TAG, "onFailure")
            }

            override fun onSuccess(message: String) {
                super.onSuccess(message)
                Log.d(LOG_TAG, "on Success VIdeo Filter")
                //File(name).delete()
            }

            override fun onFinish() {
                super.onFinish()

                if(isNullOrEmpty(el.crop)) {
                    sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.parse("file://$outputFilename")
                        )
                    )

                    Log.d(LOG_TAG, "Finish id - " + el.id)

                    checkCount()
                } else {
                    setCurrent(el.id.toInt())
                    editVideo(el, outputFilename)
                }
            }
        }

        val cge_ffmpeg = CGEFFmpegAcyncTask(outputFilename, el.path, rules, _configs.get(0).intensity, callback, el.id.toInt())

        try {
            cge_ffmpeg.execute()
        } catch (e: java.lang.Exception) {
            Log.d(LOG_TAG, "onError")
        }
    }

    private fun getFramesCount(el: Asset) {
        val outputFilename = FileUtil.getPath() + ".mp4"
        val ffmpeg = FFmpeg.getInstance(this)
        var fCount = 0

        try {
            ffmpeg.execute(arrayOf("-i", el.path, "-map", "0:v:0", "-c", "copy", outputFilename), object: ExecuteBinaryResponseHandler() {
                override fun onStart() {
                    super.onStart()
                    Log.d(LOG_TAG, "onStart")
                }

                override fun onProgress(message: String) {
                    super.onProgress(message)

                    val frames = matchFrames(message)
                    if(!isNullOrEmpty(frames)) {
                        fCount = frames.toInt()
                        setMax(el.id.toInt(), fCount)
                        Log.i(Common.LOG_TAG, "Frames $framesCount")
                    }
                }

                override fun onFailure(message: String?) {
                    super.onFailure(message)
                    Log.d(LOG_TAG, "onFailure")
                }

                override fun onSuccess(message: String?) {
                    super.onSuccess(message)
                    Log.d(LOG_TAG, "Frames Count Success")
                }

                override fun onFinish() {
                    super.onFinish()

                    if(!isNullOrEmpty(el.correction) && !isNullOrEmpty(el.crop)) {
                        setMax(el.id.toInt(), fCount * 2)
                        saveVideo(el)
                    } else {
                        if(!isNullOrEmpty(el.correction)) {
                            saveVideo(el)
                        } else if(!isNullOrEmpty(el.crop)) {
                            editVideo(el, "")
                        }
                    }
                }
            })
        } catch (e: FFmpegCommandAlreadyRunningException) {
            Log.d(LOG_TAG, "onFinish")
        }
    }

    private fun editVideo(el: Asset, inputName: String) {
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

        if(!el.crop.isEmpty()) {
            cropInfo = Gson().fromJson(el.crop, CropInfo::class.java)
        }

        val ffmpeg = FFmpeg.getInstance(this)

        val rect =
            BitmapUtils.getRectFromPoints(
                cropInfo!!.points,
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
            mainRule += if(isNullOrEmpty(mainRule)) "" else ","
            mainRule += cropRule
        }
        if(!isNullOrEmpty(rotateRule)) {
            mainRule += if(isNullOrEmpty(mainRule)) "" else ","
            mainRule += rotateRule
        }
        if(cropInfo.flipVert) {
            flipRule += if(isNullOrEmpty(mainRule)) "" else ",vflip"
        }
        if(cropInfo.flipHor) {
            flipRule += if(isNullOrEmpty(mainRule)) "" else ",hflip"
        }

        if(cropInfo.flipHor || cropInfo.flipVert) {
            mainRule += if(isNullOrEmpty(mainRule)) "" else ","
            mainRule += flipRule
        }

        val startName = if(isNullOrEmpty(inputName)) el.path else inputName

        if(!isNullOrEmpty(mainRule)) {
            try {
                ffmpeg.execute(arrayOf("-i", startName, "-filter:v", mainRule, "-c:a", "copy", outputFilename), object: ExecuteBinaryResponseHandler() {
                    override fun onStart() {
                        super.onStart()
                        Log.d(LOG_TAG, "onStart")
                    }

                    override fun onProgress(message: String) {
                        super.onProgress(message)

                        val frames = matchFrames(message)
                        if(!isNullOrEmpty(frames)) {
                            changeProgress(el.id, frames.toInt(), false)
                        }
                    }

                    override fun onFailure(message: String?) {
                        super.onFailure(message)
                        Log.d(LOG_TAG, "onFailure")
                    }

                    override fun onSuccess(message: String?) {
                        super.onSuccess(message)
                        Log.d(LOG_TAG, "onOkey")
                    }

                    override fun onFinish() {
                        super.onFinish()

                        changeProgress(el.id, 0, true)

                        if(!isNullOrEmpty(inputName)) {
                            File(inputName).delete()
                        }

                        sendBroadcast(
                            Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse("file://$outputFilename")
                            )
                        )

                        checkCount()
                    }
                })
            } catch (e: FFmpegCommandAlreadyRunningException) {
                Log.d(LOG_TAG, "onFinish")
            }
        } else {
            runOnUiThread { progressBar!!.setProgress(100) }

            sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://$outputFilename")
                )
            )

            checkCount()
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

    private fun matchFrames(message: String): String {
        val strPat = "frame=.+fps"
        var match = ""

        val patternStr = Pattern.compile(strPat)
        val patternNumber = Pattern.compile("\\d+")

        val matcher = patternStr.matcher(message)

        if(matcher.find()) {
            val findedStr = message.substring(matcher.start(),matcher.end())
            val findedMatcher = patternNumber.matcher(findedStr)

            if(findedMatcher.find()) {
                match = findedStr.substring(findedMatcher.start(),findedMatcher.end())
            }
        }

        return match
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
        recyclerViewManager!!.setOnImageSelectionListener(OnAssetSelectionListener { assets, asset ->
            var intent = Intent(this@MainMenu, EditImageActivity::class.java)

            val f = File(asset.path)
            val uri = Uri.fromFile(f)

            if (f.isVideoFile) { // If file is an Image
                intent = Intent(this@MainMenu, NewVideoOverviewActivity::class.java)
            }

            intent.putExtra("file", uri)
            intent.putExtra("imgId", asset.id.toInt())
            intent.putExtra("configs", asset.correction)
            intent.putExtra("crop", asset.crop)

            startActivity(intent)
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

