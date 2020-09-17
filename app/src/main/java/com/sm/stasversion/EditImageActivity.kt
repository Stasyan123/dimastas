package com.sm.stasversion

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.VectorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.opengl.GLSurfaceView
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.forEachIndexed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sm.stasversion.classes.*
import com.sm.stasversion.crop.BitmapUtils
import com.sm.stasversion.crop.CropDemoPreset
import com.sm.stasversion.crop.CropImageOptions
import com.sm.stasversion.crop.CropImageView
import com.sm.stasversion.imageeditor.TextEditorDialogFragment
import com.sm.stasversion.utils.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import com.sm.stasversion.videoUtils.FilterType
import com.sm.stasversion.widget.HorizontalProgressWheelView
import kotlinx.android.synthetic.main.activity_edit_image.*
import kotlinx.android.synthetic.main.activity_main.*
import org.wysaid.common.Common
import org.wysaid.myUtils.FileUtil
import org.wysaid.nativePort.CGEFFmpegNativeLibrary
import org.wysaid.nativePort.CGEImageHandler
import org.wysaid.nativePort.CGENativeLibrary
import org.wysaid.texUtils.CropInfo
import org.wysaid.view.ImageGLSurfaceView

import java.io.InputStream

class EditImageActivity : AppCompatActivity(), OnPhotoEditorListener, EditingToolsAdapter.OnItemSelected, EditingEffectsAdapter.OnItemSelected,
    EditingTextureAdapter.OnItemSelected, MainFragment.OnBitmapReady, SeekBar.OnSeekBarChangeListener {

    val LOG_TAG = "mood"

    private var db: AppDatabase? = null
    private var imgId: Int? = null
    private var uri: Uri? = null
    private var correction: String? = null
    private var crop: String? = null

    private var mEditingToolsAdapter: EditingToolsAdapter? = null
    private var mCurrentFragment: MainFragment? = null
    private var mImageView: ImageView? = null

    private var waterMark: ImageView? = null
    private var joinMood: ConstraintLayout? = null
    private var mainArea: ConstraintLayout? = null

    private var glImageView: ImageGLSurfaceView? = null

    private var canvasHeight: Float = 470.0f
    private var calculatedW: Float = 0.0f
    private var calculatedH: Float = 0.0f
    private var scale: Float = 1.0f
    private var mActiveConfig: AdjustConfig? = null
    private var mAdjustConfigs: MutableList<AdjustConfig>? = null

    var mSeekBar:SeekBar? = null
    var mSeekTemp:SeekBar? = null
    var mSeekTint:SeekBar? = null
    var mSeekHue:SeekBar? = null
    var mSeekSat:SeekBar? = null
    var mSeekLum:SeekBar? = null
    var mSeekTexture:SeekBar? = null
    var rootView: ConstraintLayout? =  null

    var textIntensity: TextView? = null
    var hue_container: ConstraintLayout? = null
    var saturetion_container: ConstraintLayout? = null
    var lum_container: ConstraintLayout? = null
    var temp_container: ConstraintLayout? = null
    var tint_container: ConstraintLayout? = null

    /* effects */
    var mLayoutInflater: LayoutInflater? = null

    var image: Bitmap? = null
    var transImage: Bitmap? = null
    var cropImage: Bitmap? = null
    var position: Int = 0
    var mOnPhotoEditorListener: OnPhotoEditorListener? = null

    var intensity: Float = 1.0f
    val tag: String = "DimaStas"

    var initCrop: Boolean = true
    var licenseBlock: Boolean = false
    var test: Boolean = true

    var imgHandler: CGEImageHandler = CGEImageHandler()

    var mThread: Thread? = null
    var mShouldStopThread = false

    var isEdited = booleanArrayOf(false, false, false)

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

                if(name.equals(mAdjustConfigs!!.get(10).name)) {
                    return BitmapFactory.decodeStream(`is`).changeBmp(mAdjustConfigs!!.get(10).horizontal[1], mAdjustConfigs!!.get(10).vertical[1], mAdjustConfigs!!.get(10).rotate[1])
                } else {
                    return BitmapFactory.decodeStream(`is`)
                }
            }

            override fun loadImageOK(bmp: Bitmap, arg: Any) {
                Log.i(Common.LOG_TAG, "Loading bitmap over, you can choose to recycle or cache")
                bmp.recycle()
            }
        }

    override fun onResume() {
        super.onResume()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_image)

        rootView = findViewById(R.id.root)
        mSeekBar = findViewById(R.id.seekBar)
        mSeekTemp = findViewById(R.id.seekBar_temperature)
        mSeekTint = findViewById(R.id.seekBar_tint)
        mSeekHue = findViewById(R.id.seekBar_hue)
        mSeekSat = findViewById(R.id.seekBar_saturation)
        mSeekLum = findViewById(R.id.seekBar_luminance)
        mSeekTexture = findViewById(R.id.seekBar_texture)

        textIntensity = findViewById(R.id.text_intensity)

        glImageView = findViewById(R.id.gpuimageview) as ImageGLSurfaceView

        waterMark = findViewById(R.id.waterMark)
        joinMood = findViewById(R.id.join_mood)
        mainArea = findViewById<ConstraintLayout>(R.id.main_area)

        val bundle = intent.extras

        uri = intent.getParcelableExtra<Uri>("file")
        imgId = bundle!!.getInt("imgId")
        correction = bundle.getString("configs")
        crop = bundle.getString("crop")

        mOnPhotoEditorListener = this

        mLayoutInflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, 1);

        initDB()
        setPaddings()
        initEffectsNames()
        initEffectsArray()
        initSavebutton()
        initClosebutton()
        initCrop()
        initHslHeight()

        val area = findViewById<ConstraintLayout>(R.id.root)
        area.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if(test) {
                    test = false
                    showSingleImage(uri)
                }

                /*if(transImage != null) {
                    var w = 0f
                    var h = 0f

                    val mainHeight = getMainHeight()

                    if (transImage!!.height > mainHeight.toInt()) {
                        calculatedH = mainHeight
                        calculatedW = transImage!!.width * (mainHeight / transImage!!.height)
                    } else {
                        calculatedW = transImage!!.width.toFloat()
                        calculatedH = transImage!!.height.toFloat()
                    }
                }*/
            }
        })
    }

    private fun initDB() {
        db = Room.databaseBuilder(this.applicationContext, AppDatabase::class.java, "mood_v4")
            .allowMainThreadQueries().build()
    }

    private fun initEffectsNames() {
        hue_container = findViewById<ConstraintLayout>(R.id.hue_container)
        hue_container!!.findViewById<TextView>(R.id.effect_name).text = getString(R.string.hue)

        saturetion_container = findViewById<ConstraintLayout>(R.id.saturation_container)
        saturetion_container!!.findViewById<TextView>(R.id.effect_name).text = getString(R.string.saturation)

        lum_container = findViewById<ConstraintLayout>(R.id.luminance_container)
        lum_container!!.findViewById<TextView>(R.id.effect_name).text = getString(R.string.luminance)

        temp_container = findViewById<ConstraintLayout>(R.id.temp_container)
        temp_container!!.findViewById<TextView>(R.id.effect_name).text = getString(R.string.temperature)

        tint_container = findViewById<ConstraintLayout>(R.id.tint_container)
        tint_container!!.findViewById<TextView>(R.id.effect_name).text = getString(R.string.tint)
    }

    private fun setPaddings() {
        val padding = (20.5 * getScale()).toInt()

        mSeekHue!!.setPadding(padding, 0,  padding, 0)
        mSeekSat!!.setPadding(padding, 0, padding, 0)
        mSeekLum!!.setPadding(padding, 0, padding, 0)
        mSeekBar!!.setPadding(padding, 0, padding, 0)
        mSeekTemp!!.setPadding(padding, 0, padding, 0)
        mSeekTint!!.setPadding(padding, 0, padding, 0)
        mSeekTexture!!.setPadding(padding + 32, 0, padding + 32, 0)
    }

    private fun initCrop() {
        val fragmentManager = getSupportFragmentManager()

        try {
            fragmentManager
                .beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(CropDemoPreset.RECT))
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initTools() {
        val percentTextView = findViewById<TextView>(R.id.text_straightening)
        percentTextView.text = getString(R.string.percent, mCurrentFragment!!.mCropImageView.cropInfo.currentPercent.toString())

        var progressF = mCurrentFragment!!.mCropImageView.cropInfo.currentPercentF
        var progress = mCurrentFragment!!.mCropImageView.cropInfo.currentPercent

        val straightening = findViewById<HorizontalProgressWheelView>(R.id.rotate_scroll_wheel)
        straightening.setValue(progress, progressF)

        straightening.setScrollingListener(object: HorizontalProgressWheelView.ScrollingListener {
            override fun onScrollStart() {

            }
            override fun onScroll(percent: Int, percentF: Float) {
                percentTextView.text = getString(R.string.percent, percent.toString())

                progressF = percentF
                progress = percent

                val angl = percent.toFloat() / 2

                var width = calculatedW;//bm!!.width
                var height = calculatedH;//bm!!.height

                if (width > height) {
                    width = calculatedH
                    height = calculatedW
                }

                val a = Math.atan((height / width).toDouble()).toFloat()

                // the length from the center to the corner of the green
                val len1 =
                    width / 2 / Math.cos(a - Math.abs(Math.toRadians(angl.toDouble()))).toFloat()
                // the length from the center to the corner of the black
                val len2 = Math.sqrt(
                    Math.pow(
                        (width / 2).toDouble(),
                        2.0
                    ) + Math.pow((height / 2).toDouble(), 2.0)
                ).toFloat()
                // compute the scaling factor
                scale = (len2 / len1)

                mCurrentFragment!!.straighten(angl, scale)
            }

            override fun onScrollEnd() {

            }
        })

        val rotate = findViewById<View>(R.id.rotate);

        val rotateEvent = fun (rotateOnly: Boolean) {
            val frame = findViewById<FrameLayout>(R.id.container)
            val civ = findViewById<CropImageView>(R.id.cropImageView)

            val mHeight = frame.measuredHeight
            val mWidth = frame.measuredWidth

            val layoutParams = frame.layoutParams
            layoutParams.height = mWidth
            layoutParams.width = mHeight
            frame.layoutParams = layoutParams

            val imageLayoutParams = civ.layoutParams
            imageLayoutParams.height = mWidth
            imageLayoutParams.width = mHeight
            civ.layoutParams = imageLayoutParams

            if(rotateOnly) {
                mCurrentFragment!!.toolsSelect(rotate)
            }
        }

        rotate.setOnClickListener{
            val t = getMainHeight1()



            initEmptyOverlay()

            //rotateEvent(true)
        }

        val hor = findViewById<View>(R.id.horizontal);
        hor.setOnClickListener{
            val area = findViewById<ConstraintLayout>(R.id.crop_tools)
            val tools = findViewById<ConstraintLayout>(R.id.constr_container)

            val oneRect = calculeRectOnScreen(area)
            val secondRect = calculeRectOnScreen(tools)

            val distance = Math.abs(oneRect.top - secondRect.top) - 20 * getScale()

            val w = transImage!!.width * (distance / transImage!!.height)

            val frame = findViewById<FrameLayout>(R.id.container)

            val l_image = mCurrentFragment!!.mCropImageView.layoutParams
            l_image.width = w.toInt()
            l_image.height = distance.toInt()
            mCurrentFragment!!.mCropImageView.setLayoutParams(l_image)

            val l = frame.getLayoutParams()
            l.width = w.toInt()
            l.height = distance.toInt()
            frame.setLayoutParams(l)



            //mCurrentFragment!!.toolsSelect(hor)
        }

        val vert = findViewById<View>(R.id.vertical);
        vert.setOnClickListener{
            mCurrentFragment!!.toolsSelect(vert)
        }

        val rect = findViewById<View>(R.id.rect)
        rect.setOnClickListener{
            if(mCurrentFragment!!.instaMode) {
                mCurrentFragment!!.mCropImageView.mCropOverlayView.setFixedAspectRatio(false)
                mCurrentFragment!!.mCropImageView.mCropOverlayView.cropWindowRect = RectF(0f, 0f,
                    mCurrentFragment!!.mCropImageView.width.toFloat(), mCurrentFragment!!.mCropImageView.height.toFloat())

                mCurrentFragment!!.instaMode = false
            } else {
                var width = mCurrentFragment!!.mCropImageView.width
                var height = mCurrentFragment!!.mCropImageView.height

                if (width > height) {
                    width = height;
                } else {
                    height = width;
                }

                val lOffset = (mCurrentFragment!!.mCropImageView.width - width) / 2
                val tOffset = (mCurrentFragment!!.mCropImageView.height - height) / 2

                mCurrentFragment!!.mCropImageView.mCropOverlayView.setFixedAspectRatio(true)
                mCurrentFragment!!.mCropImageView.mCropOverlayView.cropWindowRect = RectF(lOffset.toFloat(), tOffset.toFloat(),
                    width + lOffset.toFloat(), height + tOffset.toFloat())

                mCurrentFragment!!.instaMode = true
            }

            mCurrentFragment!!.mCropImageView.mCropOverlayView.invalidate()
        }

        val accept = findViewById<View>(R.id.apply_crop)
        accept.setOnClickListener{
            val overlay = mCurrentFragment!!.mCropImageView.cropWindowRect

            val points = mCurrentFragment!!.mCropImageView.getCropPoints()

            val scaleX = transImage!!.width / overlay.width()
            val scaleY = transImage!!.height / overlay.height()

            mCurrentFragment!!.save(bitmapCrop())
            mCurrentFragment!!.mCropImageView.setCropInfo()

            mCurrentFragment!!.mCropImageView.setCropDimension(scaleX, scaleY, (overlay.width() * scaleX).toInt(),
                (overlay.height() * scaleY).toInt(), mCurrentFragment!!.mCropImageView.width / overlay.width(),
                mCurrentFragment!!.mCropImageView.height / overlay.height(), overlay.left.toInt(), overlay.top.toInt())

            mCurrentFragment!!.mCropImageView.cropInfo.originalH = transImage!!.height
            mCurrentFragment!!.mCropImageView.cropInfo.originalW = transImage!!.width
            mCurrentFragment!!.mCropImageView.cropInfo.instaMode = mCurrentFragment!!.instaMode
            mCurrentFragment!!.mCropImageView.cropInfo.points = points
            mCurrentFragment!!.mCropImageView.cropInfo.overlay = overlay

            mCurrentFragment!!.mCropImageView.cropInfo.currentPercent = progress
            mCurrentFragment!!.mCropImageView.cropInfo.currentPercentF = progressF
            mCurrentFragment!!.mCropImageView.cropInfo.isCropped = true

            straightening.setValue(progress, progressF)

            toggleTopBar(false)
        }

        val cancel = findViewById<View>(R.id.cancel_crop)
        cancel.setOnClickListener{
            if(mCurrentFragment!!.checkRemainder()) {
                rotateEvent(false)
            }

            mCurrentFragment!!.cancelCropInfo(true)
            mCurrentFragment!!.applyCustom()

            val crop = findViewById<ConstraintLayout>(R.id.crop_area)
            crop.visibility = View.GONE

            mainArea!!.visibility = View.VISIBLE

            percentTextView.text = getString(R.string.percent, mCurrentFragment!!.mCropImageView.cropInfo.currentPercent.toString())
            straightening.invalidateValue()
            straightening.postInvalidate()

            toggleTopBar(false)
        }
    }

    private fun Bitmap.changeBmp(x: Float, y: Float, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postScale(x, y, width / 2f, height / 2f) }
        matrix.apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun bitmapCrop(): Bitmap {
        val translateBitmap = Bitmap.createBitmap(
            transImage!!.width,
            transImage!!.height, transImage!!.config
        )

        val translateCanvas = Canvas(translateBitmap)

        val translateMatrix = Matrix()

        translateMatrix.postScale(mCurrentFragment!!.scaleX, mCurrentFragment!!.scaleY, transImage!!.width / 2.0f, transImage!!.height / 2.0f)
        translateMatrix.postRotate(mCurrentFragment!!.postRotate, transImage!!.width / 2.0f, transImage!!.height / 2.0f)

        translateCanvas.drawBitmap(transImage!!, translateMatrix, Paint())

        return translateBitmap
    }

    protected fun threadSync() {
        if (mThread != null && mThread!!.isAlive) {
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

    private fun initSavebutton() {
        val v = findViewById<TextView>(R.id.topSave)

        v.setOnClickListener{
           // glImageView!!.setFilterWithConfig("@adjust lut 1.png")

            if(licenseBlock) {
                licenseOpen()
            } else {
                mAdjustConfigs!!.get(0).intensity = intensity
                mAdjustConfigs!!.get(0).position = position
                mAdjustConfigs!!.get(0).isEdited = isEdited

                var gsonConfig = ""

                if(isEdited[0] != false || isEdited[1] != false || isEdited[2] != false) {
                    gsonConfig = serializedConfigs.encryptConfigs(mAdjustConfigs)
                }

                val gsonCrop = if(mCurrentFragment!!.mCropImageView.getCropInfo().isCropped) Gson().toJson(mCurrentFragment!!.mCropImageView.getCropInfo()) else ""

                db!!.configDao().updateConfig(imgId, gsonConfig, gsonCrop)

                val intent = Intent(this@EditImageActivity, MainMenu::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun getPixels(scale: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 26.0f * scale, this.getResources().getDisplayMetrics())
    }

    private fun initClosebutton() {
        val view = findViewById<ImageView>(R.id.topClose)

        view.setOnClickListener{
            //glImageView!!.setFilterIntensityForIndex(0.5f, 0)
            //glImageView!!.setFilterWithConfig("@adjust lut ping.png 0.0 @adjust exposure 0.0 @adjust brightness 0.0 @adjust contrast 1.0 @adjust shadowhighlight 0.0 0.0 @adjust saturation 1.0 @adjust whitebalance 0.0 1.0 @blend sl oise_light.png 0.0 @adjust sharpen 0.0 @adjust hsl 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0");
            finish()
        }
    }

    private fun showSingleImage(uri: Uri?) {
        initRvEffects()
        initRvTexture()

        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>(300, 300){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    initRvTools(resource);
                }
                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })

        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>(720, 1280){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    initView(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    private fun hideIntensity(texture: Boolean) {
        val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
        val intensity = findViewById<ConstraintLayout>(R.id.intensityLayout)

        if(texture) {
            findViewById<ConstraintLayout>(R.id.texture_container).visibility = View.GONE
        }

        tools.visibility = View.VISIBLE
        intensity.visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.intensity_buttons).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.intensityLayout_effect).visibility = View.GONE
        findViewById<LinearLayout>(R.id.temperature_container).visibility = View.GONE
        findViewById<LinearLayout>(R.id.hsl_container).visibility = View.GONE
    }

    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
    }

    private fun getCameraPhotoOrientation(context: Context, imageUri: Uri): Float{
        var rotate = 0.0f
        try {
            var exif = ExifInterface(imageUri.path);
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

            Log.i("RotateImage", "Exif orientation: " + orientation);
            Log.i("RotateImage", "Rotate value: " + rotate);
        } catch (e: Exception) {
            e.printStackTrace();
        }
        return rotate;
    }

    fun initCropConfig(res: Bitmap): Bitmap {
        if(crop != null && !crop!!.isEmpty()) {
            val cropInfo = Gson().fromJson(crop, CropInfo::class.java)

            mCurrentFragment!!.mCropImageView.cropInfo = cropInfo
            mCurrentFragment!!.mCropImageView.cancelCropInfo(true)

            return serializedConfigs.cropImage(res, cropInfo)
        } else {
            return res
        }
    }

    private fun before_afterEvent() {
        var shouldStop = false
        //val parent = findViewById<RelativeLayout>(R.id.parentView)

        glImageView!!.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if(!shouldStop) {
                        setFilters()
                    }
                    shouldStop = true
                } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    shouldStop = false

                    android.os.Handler().postDelayed(object : Runnable {
                        override fun run() {
                            if(!shouldStop) {
                                glImageView!!.setFilterWithConfig("")
                            }
                        }
                    }, 0, 400)
                }

                return true
            }
        })
    }

    private fun initView(res: Bitmap) {
        initToolsEvents()
        before_afterEvent()

        try {
            image = res.copy(res.getConfig(), true)
            transImage = res.copy(res.getConfig(), true)

            val img = initCropConfig(res)
            cropImage = img.copy(img.getConfig(), true)

            cropSurface(img, image)

            glImageView!!.setSurfaceCreatedCallback( {
                val t = getMainHeight()

                Log.d("Lion", "Height - " + t)

                glImageView!!.setImageBitmap(img)
                setFilters()
            })

            glImageView!!.setDisplayMode(ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FIT)

            startCropping(res, true)

            colorsEvents(true)
            effectsEvents()
            textureEvents()
            initTools()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun textureEvents() {
        mSeekTexture!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                if(mActiveConfig!!.startEditing) {
                    val pr = progress.toFloat() / 100.0f
                    mActiveConfig!!.setTempIntensity(pr, true, glImageView)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        val rotate = findViewById<View>(R.id.texture_rotate)
        val horizontal = findViewById<View>(R.id.texture_hor)
        val vert = findViewById<View>(R.id.texture_vert)
        val diff = findViewById<View>(R.id.texture_diff)

        rotate.setOnClickListener {
            mActiveConfig!!.rotate[1] += 90f

            if(mActiveConfig!!.rotate[1] >= 360f) {
                mActiveConfig!!.rotate[1] -= 360f
            }

            glImageView!!.setFilterWithConfig(calculateRules())
        }
        horizontal.setOnClickListener {
            mActiveConfig!!.horizontal[1] *= -1f
            glImageView!!.setFilterWithConfig(calculateRules())
        }
        vert.setOnClickListener {
            mActiveConfig!!.vertical[1] *= -1f
            glImageView!!.setFilterWithConfig(calculateRules())
        }
        diff.setOnClickListener {
            mActiveConfig!!.diff[1] = !mActiveConfig!!.diff[1]
            mActiveConfig!!.setRule(mActiveConfig!!.diff[1])

            glImageView!!.setFilterWithConfig(calculateRules())
        }

        val cancel = findViewById<View>(R.id.cancel_texture_button)
        val apply = findViewById<View>(R.id.accept_texture_button)

        cancel.setOnClickListener {
            mActiveConfig!!.textureConfig(mActiveConfig!!.rotate[0], mActiveConfig!!.horizontal[0], mActiveConfig!!.vertical[0], mActiveConfig!!.diff[0])
            mActiveConfig!!.setIntensity(mActiveConfig!!.slierIntensity, false, glImageView)

            setFilters()
            hideIntensity(true)
            toggleTopBar(false)
        }
        apply.setOnClickListener {
            mActiveConfig!!.textureConfig(mActiveConfig!!.rotate[1], mActiveConfig!!.horizontal[1], mActiveConfig!!.vertical[1], mActiveConfig!!.diff[1])
            mActiveConfig!!.setIntensity(mSeekTexture!!.progress / 100f, false, glImageView)

            hideIntensity(true)
            toggleTopBar(false)
        }
    }

    private fun setOnClick(position: Int, view: View) {
        view.setOnClickListener{
            colorsEvents(false)

            if(view.id == R.id.all || view.id == R.id.all_empty) {
                mActiveConfig!!.hslPos = 0
                findViewById<View>(R.id.all).alpha = 1.0f
            } else {
                mActiveConfig!!.hslPos = position - 1
                (view.background as LayerDrawable).findDrawableByLayerId(R.id.border).alpha = 255
            }

            mActiveConfig!!.startEditing = false
            initHslSeek(true)
            mActiveConfig!!.startEditing = true
        }
    }

    private fun colorsEvents(start: Boolean) {
        val colors = findViewById<ConstraintLayout>(R.id.colors)

        for (i in 0..(colors.childCount - 1)) {
            val view = colors.getChildAt(i)

            if(start) {
                setOnClick(i, view)
            }

            if((view.id == R.id.all || view.id == R.id.all_empty)) {
                if(!start) {
                    findViewById<View>(R.id.all).alpha = 0.0f
                }
            } else {
                val ld = view.background as LayerDrawable
                ld.findDrawableByLayerId(R.id.border).alpha = 0
            }
        }
    }

    private fun effectsEvents() {
        mSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                if (mActiveConfig!!.type == EffectType.Shadow) {
                    mActiveConfig!!.setTempIntensityWithParam(
                        8,
                        mSeekBar!!.progress / 100f,
                        mActiveConfig!!.calcIntensity(mActiveConfig!!.additionaItem.slierIntensity),
                        0.0f,
                        glImageView
                    )
                } else if (mActiveConfig!!.type == EffectType.Highlight) {
                    mActiveConfig!!.setTempIntensityWithParam(
                        8,
                        mActiveConfig!!.slierIntensity,
                        mActiveConfig!!.calcIntensity(mSeekBar!!.progress / 100f),
                        0.0f,
                        glImageView
                    )
                } else {
                    mActiveConfig!!.setTempIntensity(mSeekBar!!.progress / 100f, true, glImageView)
                }

                textIntensity!!.text = progress.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        val apply_effect = findViewById<View>(R.id.apply_effect_button)
        val cancel_effect = findViewById<View>(R.id.cancel_effect_button)

        mSeekTemp!!.setOnSeekBarChangeListener(this)
        mSeekTint!!.setOnSeekBarChangeListener(this)
        mSeekHue!!.setOnSeekBarChangeListener(this)
        mSeekSat!!.setOnSeekBarChangeListener(this)
        mSeekLum!!.setOnSeekBarChangeListener(this)

        apply_effect.setOnClickListener {
            if(mActiveConfig!!.type != EffectType.Lut) {
                if (mActiveConfig!!.type == EffectType.Temperature) {
                    mActiveConfig!!.setIntensityWithParam(
                        7,
                        mSeekTemp!!.progress / 100.0f,
                        mSeekTint!!.progress / 100.0f,
                        glImageView,
                        false
                    )
                } else if (mActiveConfig!!.type == EffectType.Shadow) {
                    mActiveConfig!!.setIntensityWithParam(
                        8,
                        mSeekBar!!.progress / 100f,
                        mActiveConfig!!.additionaItem.slierIntensity,
                        glImageView,
                        false
                    )
                } else if (mActiveConfig!!.type == EffectType.HSL) {
                    copyArray(mActiveConfig!!.tempHsl, mActiveConfig!!.hsl)

                    setFilters()
                } else if (mActiveConfig!!.type == EffectType.Highlight) {
                    mActiveConfig!!.setIntensityWithParam(
                        8,
                        mActiveConfig!!.slierIntensity,
                        mSeekBar!!.progress / 100f,
                        glImageView,
                        false
                    )
                } else {
                    mActiveConfig!!.setIntensity(mSeekBar!!.progress / 100f, false, glImageView)
                }

                if(mActiveConfig!!.checkOriginal()) {
                    mActiveConfig!!.active = false
                    setFilters()
                }

                mActiveConfig!!.startEditing = false
                scaleArea(false)
            } else {
                intensity = mSeekBar!!.progress / 100f
                mAdjustConfigs!!.get(0).intensity = intensity

                mActiveConfig!!.setIntensity(intensity, false, glImageView)
                scaleArea(false)
            }

            isEdited[1] = true
            toggleTopBar(false)
            hideIntensity(false)

            if(licenseBlock) {
                toggleLicense(true, false)
            }
        }

        cancel_effect.setOnClickListener {
            if(mActiveConfig!!.type != EffectType.Lut) {
                if (mActiveConfig!!.type == EffectType.Temperature) {
                    mActiveConfig!!.setIntensityWithParam(
                        7,
                        mActiveConfig!!.slierIntensity,
                        mActiveConfig!!.additionaItem.slierIntensity,
                        glImageView,
                        true
                    )
                } else if (mActiveConfig!!.type == EffectType.Shadow || mActiveConfig!!.type == EffectType.Highlight) {
                    mActiveConfig!!.setIntensityWithParam(
                        8,
                        mActiveConfig!!.slierIntensity,
                        mActiveConfig!!.additionaItem.slierIntensity,
                        glImageView,
                        true
                    )
                } else if (mActiveConfig!!.type == EffectType.HSL) {
                    copyArray(mActiveConfig!!.hsl, mActiveConfig!!.tempHsl)

                    setFilters()
                } else {
                    mActiveConfig!!.setIntensity(mActiveConfig!!.slierIntensity, true, glImageView)
                }

                if(mActiveConfig!!.checkOriginal()) {
                    mActiveConfig!!.active = false
                    setFilters()
                }

                mActiveConfig!!.startEditing = false
                scaleArea(false)
            } else {
                //glImageView!!.setLutIntensity(intensity)
                mActiveConfig!!.setIntensity(intensity, true, glImageView)

                scaleArea(false)
            }

            toggleTopBar(false)
            hideIntensity(false)

            if(licenseBlock) {
                toggleLicense(true, false)
            }
        }
    }

    override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
        if(mActiveConfig!!.startEditing) {
            if (mActiveConfig!!.type == EffectType.Temperature) {
                temp_container!!.findViewById<TextView>(R.id.effect_value).text = mSeekTemp!!.progress.toString()
                tint_container!!.findViewById<TextView>(R.id.effect_value).text = mSeekTint!!.progress.toString()

                mActiveConfig!!.setTempIntensityWithParam(
                    7,
                    mSeekTemp!!.progress / 100.0f,
                    mActiveConfig!!.additionaItem.calcIntensity(mSeekTint!!.progress / 100.0f),
                    0.0f,
                    glImageView
                )
            } else {
                val sat = mActiveConfig!!.calcIntensity(mSeekSat!!.progress / 100.0f)
                val lum = mActiveConfig!!.calcIntensity(mSeekLum!!.progress / 100.0f)

                hue_container!!.findViewById<TextView>(R.id.effect_value).text = String.format("%.1f", mActiveConfig!!.calcIntensity(mSeekHue!!.progress / 100.0f) * 10)
                saturetion_container!!.findViewById<TextView>(R.id.effect_value).text = String.format("%.1f", sat * 10)
                lum_container!!.findViewById<TextView>(R.id.effect_value).text = String.format("%.1f", lum * 10)

                mActiveConfig!!.setTempIntensityWithParam(
                    mActiveConfig!!.hslPos,
                    mSeekHue!!.progress / 100.0f,
                    sat,
                    lum,
                    glImageView
                )
            }
        }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

    private fun initRvTools(res: Bitmap) {
        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val mTools = findViewById<RecyclerView>(R.id.rvTools)
        mEditingToolsAdapter = EditingToolsAdapter(this, res, this, position)

        mTools.layoutManager = llmTools
        mTools.setAdapter(mEditingToolsAdapter);
    }

    private fun initRvTexture() {
        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val mTextures = findViewById<RecyclerView>(R.id.rvTexture)
        val mEditingTextureAdapter = EditingTextureAdapter(this)

        mTextures.layoutManager = llmTools
        mTextures.setAdapter(mEditingTextureAdapter);
    }

    private fun initRvEffects() {
        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val mEffects = findViewById<RecyclerView>(R.id.rvEffect)
        val mEditingEffectAdapter = EditingEffectsAdapter(this)

        mEffects.layoutManager = llmTools
        mEffects.setAdapter(mEditingEffectAdapter);
    }

    override fun onEffectSelected(eType: EffectType, position: Int) {
        if(eType == EffectType.Crop) {
            startCropping(CGENativeLibrary.filterImage_MultipleEffects(transImage, calculateRules(), intensity, -1), false)
            return
        }

        changeMargin(true)

        mActiveConfig = mAdjustConfigs!!.get(position)
        mActiveConfig!!.type = eType

        if(!mActiveConfig!!.active) {
            mActiveConfig!!.active = true
            setFilters()
        }

        toggleTopBar(true)
        showEffects(eType)

        if(eType == EffectType.HSL) {
            initHslSeek(false)
            mActiveConfig!!.startEditing = true

            changeMargin(false)

            if(licenseBlock) {
                toggleLicense(true, true)
            }
            return;
        }

        if(eType == EffectType.Temperature) {
            changeMargin(false)

            val seek_temp = findViewById<SeekBar>(R.id.seekBar_temperature)
            val seek_tint = findViewById<SeekBar>(R.id.seekBar_tint)

            seek_temp.setProgress((mActiveConfig!!.slierIntensity * seek_temp.max).toInt())
            seek_tint.setProgress((mActiveConfig!!.additionaItem.slierIntensity * seek_tint.max).toInt())

            temp_container!!.findViewById<TextView>(R.id.effect_value).text = ((mActiveConfig!!.slierIntensity * seek_temp.max).toInt()).toString()
            tint_container!!.findViewById<TextView>(R.id.effect_value).text = ((mActiveConfig!!.additionaItem.slierIntensity * seek_tint.max).toInt()).toString()

            mActiveConfig!!.startEditing = true

            if(licenseBlock) {
                toggleLicense(true, true)
            }
        } else if(eType == EffectType.Highlight) {
            textIntensity!!.text = ((mActiveConfig!!.additionaItem.slierIntensity * mSeekBar!!.max).toInt()).toString()
            mSeekBar!!.setProgress((mActiveConfig!!.additionaItem.slierIntensity * mSeekBar!!.max).toInt())
        } else {
            textIntensity!!.text =((mActiveConfig!!.slierIntensity * mSeekBar!!.max).toInt()).toString()
            mSeekBar!!.setProgress((mActiveConfig!!.slierIntensity * mSeekBar!!.max).toInt())
        }
    }

    private fun scaleArea(min: Boolean) {
        val parent = findViewById<ConstraintLayout>(R.id.groupLayout)
        val params = parent.layoutParams

        if(min) {
            params.height = (315.5 * getScale()).toInt()
        } else {
            params.height = (canvasHeight * getScale()).toInt()
        }

        parent.layoutParams = params
    }

    private fun showTexture() {
        findViewById<ConstraintLayout>(R.id.toolsLayout).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.texture_container).visibility = View.VISIBLE
    }

    private fun showEffects(type: EffectType) {
        findViewById<ConstraintLayout>(R.id.toolsLayout).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.intensity_buttons).visibility = View.VISIBLE

        when (type) {
            EffectType.Temperature -> {
                findViewById<ConstraintLayout>(R.id.intensityLayout_effect).visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.temperature_container).visibility = View.VISIBLE

                scaleArea(true)
            }
            EffectType.HSL -> {
                findViewById<ConstraintLayout>(R.id.intensityLayout_effect).visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.hsl_container).visibility = View.VISIBLE
            }
            else -> {
                findViewById<ConstraintLayout>(R.id.intensityLayout).visibility = View.VISIBLE
            }
        }
    }

    private fun getCurrent(min: Float, max: Float, current: Float): Int {
        var currentBar = 0.0f

        if(min > 0) {
            val factor = (max - min) / 100
            currentBar = (current - min) / factor
        } else {
            currentBar = ((current + Math.abs(min)) * 100) / (Math.abs(min) + max)
        }

        return currentBar.toInt()
    }

    override fun onFilterSelected(fType: FilterType, pos: Int, rule: String, color: Int, byLicense: Boolean) {
        changeMargin(true)
        mActiveConfig = mAdjustConfigs!!.get(0)

        licenseBlock = byLicense
        toggleLicense(byLicense, false)

        if(pos != position) {
            intensity = 1.0f
            mActiveConfig!!.mRule = rule
            mActiveConfig!!.intensity = intensity
            mActiveConfig!!.active = true
        }

        setFilters()

        val rec = findViewById<RecyclerView>(R.id.rvTools)
        val item = rec.getLayoutManager()!!.findViewByPosition(position);

        if(pos == position) {
            if(pos != 0) {
                textIntensity!!.text = ((intensity * 100.0f).toInt()).toString()
                mSeekBar!!.setProgress((intensity * 100.0f).toInt())

                findViewById<ConstraintLayout>(R.id.toolsLayout).visibility = View.GONE
                findViewById<ConstraintLayout>(R.id.intensityLayout).visibility = View.VISIBLE
                findViewById<ConstraintLayout>(R.id.intensity_buttons).visibility = View.VISIBLE

                toggleTopBar(true)
            }
        } else {
            if(position != 0) {
                mEditingToolsAdapter!!.switchBorderStatus(position, false)
            }

            if(item != null) {
                item.findViewById<View>(R.id.viewBorder).visibility = View.GONE
                item.findViewById<ImageView>(R.id.intensity_icon).visibility = View.GONE
            }

            if(pos != 0) {
                mEditingToolsAdapter!!.switchBorderStatus(pos, true)
                val itemNew = rec.getLayoutManager()!!.findViewByPosition(pos);
                val intens_new = itemNew!!.findViewById<ImageView>(R.id.intensity_icon)
                intens_new.visibility = View.VISIBLE

                val viewBorder = itemNew.findViewById<View>(R.id.viewBorder)
                viewBorder.visibility = View.VISIBLE

                isEdited[0] = true
            } else {
                mAdjustConfigs!!.get(0).active = false
                isEdited[0] = false
            }

            mEditingToolsAdapter!!.notifyDataSetChanged()
            position = pos
        }
    }

    private fun initHslHeight() {
        val hsl = findViewById<LinearLayout>(R.id.hsl_container)

        hsl.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if(mActiveConfig != null && mActiveConfig!!.type == EffectType.HSL && mActiveConfig!!.startEditing) {
                    val area = findViewById<ConstraintLayout>(R.id.groupLayout)

                    val oneRect = calculeRectOnScreen(area)
                    val secondRect = calculeRectOnScreen(hsl)

                    val distance = Math.abs(oneRect.top - secondRect.top) - 20 * getScale()

                    val params = area.layoutParams
                    params.height = distance.toInt()

                    area.layoutParams = params
                }
            }
        })


    }

    private fun startCropping(arr: Bitmap, initOnly: Boolean) {
        if(!initOnly) {
            val cropArea = findViewById<ConstraintLayout>(R.id.crop_area)

            mainArea!!.visibility = View.GONE
            cropArea.visibility = View.VISIBLE

            val join = findViewById<ConstraintLayout>(R.id.join_mood_crop)
            join.visibility = if(licenseBlock) View.VISIBLE else View.GONE
            findViewById<ImageView>(R.id.waterMark_crop).visibility = if(licenseBlock) View.VISIBLE else View.GONE

            toggleTopBar(false)
        }

        //val bitmap = getInitialImage(arr)

        /*val img = findViewById<ImageView>(R.id.ImageView_image_test)
        if(img.width > calculatedW) {
            val frame = findViewById<FrameLayout>(R.id.container)

            val l = frame.getLayoutParams()
            l.width = (calculatedW + 20).toInt()
            frame.setLayoutParams(l)
        }*/

        /*if(!initOnly) {
            val area = findViewById<ConstraintLayout>(R.id.crop_tools)
            val tools = findViewById<ConstraintLayout>(R.id.constr_container)

            val oneRect = calculeRectOnScreen(area)
            val secondRect = calculeRectOnScreen(tools)

            val distance = Math.abs(oneRect.top - secondRect.top) - 20 * getScale()

            if(mCurrentFragment!!.mCropImageView.height > distance) {
                val frame = findViewById<FrameLayout>(R.id.container)

                val l = frame.getLayoutParams()
                l.width = (l.width * (distance / mCurrentFragment!!.mCropImageView.height)).toInt()
                l.height = distance.toInt()
                frame.setLayoutParams(l)

                val l_image = mCurrentFragment!!.mCropImageView.layoutParams
                l_image.width = l.width
                l_image.height = l.height
                mCurrentFragment!!.mCropImageView.setLayoutParams(l_image)
            }
        }*/

        try{
            if(mCurrentFragment!!.isBmInit()) {
                mImageView!!.setImageBitmap(arr)
            } else {
                mCurrentFragment!!.setFrame(findViewById<FrameLayout>(R.id.container))
                mCurrentFragment!!.setImageBm(arr, findViewById<FrameLayout>(R.id.container))

                mImageView = findViewById<ImageView>(R.id.ImageView_image)
            }
        } catch (e: java.lang.Exception) {
            Log.d(tag, e.message.toString())
        }

        /*if(initCrop && !initOnly) {
            initCrop = false

            if(crop != null && !crop!!.isEmpty()) {
                initCrop = false

                initOverlay()
                rotateImage()
            } else {
                //initEmptyOverlay()
            }
        }*/

        if(!initOnly) {
            initEmptyOverlay()
        } else {
            val area = findViewById<ConstraintLayout>(R.id.crop_tools)
            val tools = findViewById<ConstraintLayout>(R.id.constr_container)

            val oneRect = calculeRectOnScreen(area)
            val secondRect = calculeRectOnScreen(tools)

            val distance = Math.abs(oneRect.top - secondRect.top) - 20 * getScale()

            val w = transImage!!.width * (distance / transImage!!.height)

            val frame = findViewById<FrameLayout>(R.id.container)

            Log.d("vas distance ", distance.toString())
            Log.d("vas w ", w.toString())

            val l_image = mCurrentFragment!!.mCropImageView.layoutParams
            l_image.width = w.toInt()
            l_image.height = distance.toInt()
            mCurrentFragment!!.mCropImageView.setLayoutParams(l_image)

            val l = frame.getLayoutParams()
            l.width = w.toInt()
            l.height = distance.toInt()
            frame.setLayoutParams(l)
        }
    }

    private fun cropHeight3() {
        val area = findViewById<ConstraintLayout>(R.id.crop_tools)
        val tools = findViewById<ConstraintLayout>(R.id.constr_container)

        val oneRect = calculeRectOnScreen(area)
        val secondRect = calculeRectOnScreen(tools)

        val distance = Math.abs(oneRect.top - secondRect.top) - 20 * getScale()

        val img = findViewById<ImageView>(R.id.ImageView_image_test)
        val frame = findViewById<FrameLayout>(R.id.container)
        val l = frame.getLayoutParams()

        val t = calculatedW
        val t1 = calculatedH
        val t2 = l.width
        val t3 = l.height
        val tt = 1
    }

    fun initEmptyOverlay() {
        mCurrentFragment!!.mCropImageView.mCropOverlayView.setFixedAspectRatio(false)

        mCurrentFragment!!.mCropImageView.mCropOverlayView.cropWindowRect = RectF(0f, 0f,
            mCurrentFragment!!.mCropImageView.width.toFloat(), mCurrentFragment!!.mCropImageView.height.toFloat())

        mCurrentFragment!!.mCropImageView.mCropOverlayView.invalidate()
    }

    fun initOverlay() {
        val crop = mCurrentFragment!!.mCropImageView.cropInfo

        if(!crop.isCropped()) {
            val rect =
                BitmapUtils.getRectFromPoints(
                    crop.points,
                    crop.originalW,
                    crop.originalH,
                    false,
                    1,
                    1
                );

            val left = rect.left * (mCurrentFragment!!.mCropImageView.width.toFloat() / crop.originalW.toFloat())
            val top = rect.top * (mCurrentFragment!!.mCropImageView.height.toFloat() / crop.originalH.toFloat())
            val width = rect.width() * (mCurrentFragment!!.mCropImageView.width.toFloat() / crop.originalW.toFloat())
            val height = rect.height() * (mCurrentFragment!!.mCropImageView.height.toFloat() / crop.originalH.toFloat())

            mCurrentFragment!!.mCropImageView.mCropOverlayView.setFixedAspectRatio(mCurrentFragment!!.mCropImageView.cropInfo.instaMode)

            mCurrentFragment!!.mCropImageView.mCropOverlayView.cropWindowRect = RectF(
                left, top,
                left + width, top + height
            )

            mCurrentFragment!!.mCropImageView.mCropOverlayView.invalidate()
        }
    }

    override fun onAreaReady() {

    }

    fun rotateImage() {
        val crop = mCurrentFragment!!.mCropImageView.cropInfo

        if (mCurrentFragment!!.checkRemainder()) {
            val frame = findViewById<FrameLayout>(R.id.container)
            val civ = findViewById<CropImageView>(R.id.cropImageView)

            val mHeight = frame.measuredHeight
            val mWidth = frame.measuredWidth

            val layoutParams = frame.layoutParams
            layoutParams.height = mWidth
            layoutParams.width = mHeight
            frame.layoutParams = layoutParams

            val imageLayoutParams = civ.layoutParams
            imageLayoutParams.height = mWidth
            imageLayoutParams.width = mHeight
            civ.layoutParams = imageLayoutParams
        }

        if (crop.rotation != mCurrentFragment!!.mCropImageView.mDegreesRotated) {
            mCurrentFragment!!.mCropImageView.rotateImage(crop.rotation - mCurrentFragment!!.mCropImageView.mDegreesRotated)
        }
    }

    override fun onBitmapCropped(bm: Bitmap) {
        glImageView!!.setImageBitmap(bm)
        setFilters()

        val crop = findViewById<ConstraintLayout>(R.id.crop_area)
        crop.visibility = View.GONE

        mainArea!!.visibility = View.VISIBLE

        cropSurface(bm, null)
        image = bm
    }

    private fun getWidth(): Int{
        val display = getWindowManager().getDefaultDisplay();
        val size = Point();
        display.getSize(size);

        return size.x
    }

    private fun getHeight(): Int{
        val display = getWindowManager().getDefaultDisplay();
        val size = Point()
        display.getSize(size)

        return size.y
    }

    fun Int.toDp(context: Context):Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,this.toFloat(),context.resources.displayMetrics
    ).toInt()

    private fun getScale(): Float {
        return this.getResources().getDisplayMetrics().density
    }

    private fun getCropHeight():Float {
        val area = findViewById<ConstraintLayout>(R.id.crop_tools)
        val tools = findViewById<ConstraintLayout>(R.id.constr_container)

        val oneRect = calculeRectOnScreen(area)
        val secondRect = calculeRectOnScreen(tools)

        return Math.abs(oneRect.top - secondRect.top) - 20 * getScale()
    }

    private fun getMainHeight():Float {
        val area = findViewById<ConstraintLayout>(R.id.groupLayout)
        val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)

        val oneRect = calculeRectOnScreen(area)
        val secondRect = calculeRectOnScreen(tools)

        return Math.abs(oneRect.top - secondRect.top) - 20 * getScale()
    }

    private fun cropSurface(bOutput: Bitmap, originalImage: Bitmap?) {
        val scale = getScale()

        var w = 0f
        var h = 0f

        val mainHeight = getMainHeight()
        Log.d("Lion", "Height now - " + mainHeight)
        if(bOutput.height > mainHeight.toInt() && mainHeight.toInt() > 0) {
            h = mainHeight
            w = bOutput.width * (mainHeight / bOutput.height)
        } else {
            w = bOutput.width.toFloat()
            h = bOutput.height.toFloat()
        }

        if(originalImage != null) {
            if(originalImage.height > mainHeight.toInt()) {
                calculatedH = mainHeight
                calculatedW = originalImage.width * (mainHeight / originalImage.height)
            } else {
                calculatedW = originalImage.width.toFloat()
                calculatedH = originalImage.height.toFloat()
            }
        }

        val layoutParams = glImageView!!.layoutParams
        layoutParams.height = h.toInt()
        layoutParams.width = w.toInt()
        glImageView!!.layoutParams = layoutParams

        val layoutParamsW = waterMark!!.layoutParams
        layoutParamsW.height = h.toInt()
        layoutParamsW.width = w.toInt()
        waterMark!!.layoutParams = layoutParamsW
    }

    fun setCurrentFragment(fragment: MainFragment) {
        mCurrentFragment = fragment
        mCurrentFragment!!.setBitmapListener(this)
    }

    override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int, font: Typeface, fontName: String) {
        val textEditorDialogFragment = TextEditorDialogFragment.show(this, text, colorCode, fontName)

        textEditorDialogFragment.setOnTextEditorListener(object :
            TextEditorDialogFragment.TextEditor {
            override fun onDone(inputText: String, colorCode: Int, font: Typeface,  fontName: String) {
                editText(rootView, inputText)
            }

            override fun onDestroy() {

            }
        })
    }

    private fun calculateRules(): String {
        var rule = "";
        var i = 0

        mAdjustConfigs!!.forEachIndexed() { index, config ->
            if(config.active) {
                rule += " " + if(config.editorOpen) config.getRuleTemporary() else config.getRule()

                mAdjustConfigs!!.get(index).filterPosition = i
                i++
            }
        }

        return rule
    }

    private fun initEffectsArray() {
        if(correction != null &&!correction!!.isEmpty()) {
            mAdjustConfigs = serializedConfigs.decryptConfigsStatic(correction)

            intensity = mAdjustConfigs!!.get(0).intensity
            position = mAdjustConfigs!!.get(0).position
            isEdited = mAdjustConfigs!!.get(0).isEdited
        } else {
            val temperature = AdjustConfig(
                6,
                -0.5f,
                0.0f,
                0.5f,
                "@adjust whitebalance",
                0.5f,
                true,
                EffectType.Temperature,
                null
            )
            val temp_add =
                AdjustConfig(6, 0.5f, 1.0f, 1.5f, "", 0.5f, true, EffectType.Temperature, null)
            temp_add.parentId = 6
            temperature.setAdditional(temp_add)

            val sh = AdjustConfig(
                4,
                -100.0f,
                0.0f,
                100.0f,
                "@adjust shadowhighlight",
                0.5f,
                true,
                EffectType.Shadow,
                null
            )
            val sh_add =
                AdjustConfig(4, -100.0f, 0.0f, 100.0f, "", 0.5f, true, EffectType.Highlight, null)
            sh_add.parentId = 4
            sh.setAdditional(sh_add)

            val hslConfig =
                AdjustConfig(9, -1.0f, 0.0f, 1.0f, "@adjust hsl", 0.5f, false, EffectType.HSL, null)
            hslConfig.hsl = arrayOf(
                floatArrayOf(0.0f, 0.0f, 0.0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f)
            )
            hslConfig.tempHsl = arrayOf(
                floatArrayOf(0.0f, 0.0f, 0.0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f)
            )

            mAdjustConfigs = mutableListOf(
                AdjustConfig(
                    0,
                    0.0f,
                    0.5f,
                    1.0f,
                    "@adjust lut empty.png",
                    1.0f,
                    false,
                    EffectType.Lut,
                    null
                ),
                AdjustConfig(
                    1,
                    -0.5f,
                    0.0f,
                    0.5f,
                    "@adjust exposure",
                    0.5f,
                    false,
                    EffectType.Exposition,
                    null
                ),
                AdjustConfig(
                    2,
                    -.5f,
                    0.0f,
                    0.5f,
                    "@adjust brightness",
                    0.5f,
                    false,
                    EffectType.Brightness,
                    null
                ),
                AdjustConfig(
                    3,
                    0.5f,
                    1.0f,
                    1.5f,
                    "@adjust contrast",
                    0.5f,
                    false,
                    EffectType.Contrast,
                    null
                ),
                sh,
                AdjustConfig(
                    5,
                    0.0f,
                    1.0f,
                    2.0f,
                    "@adjust saturation",
                    0.5f,
                    false,
                    EffectType.Saturation,
                    null
                ),
                temperature,
                AdjustConfig(
                    7,
                    .0f,
                    0.0f,
                    1.0f,
                    "@blend sl noise.JPG",
                    0f,
                    false,
                    EffectType.Grain,
                    null
                ),
                AdjustConfig(
                    8,
                    0f,
                    0.0f,
                    2.5f,
                    "@adjust sharpen",
                    0f,
                    false,
                    EffectType.Sharpness,
                    null
                ),
                hslConfig,
                AdjustConfig(10, 0f, 0.5f, 1f, "", 1f, false, EffectType.Texture, null)
            )

            mAdjustConfigs!!.get(10).active = false
            mAdjustConfigs!!.get(10).intensity = 1.0f
        }
    }

    private fun copyArray(ar1: Array<FloatArray>, ar2: Array<FloatArray>) {
        for (i in 0..ar1.size - 1) {
            ar2[i] = Arrays.copyOf(ar1[i], ar1[i].size);
        }
    }

    override fun onTextureSelected(name: String, position: Int) {
        mActiveConfig = mAdjustConfigs!!.get(position)

        if(mActiveConfig != null && !name.equals(mActiveConfig!!.name)) {
            mActiveConfig!!.setIntensity(1f, false, glImageView)
            mActiveConfig!!.textureConfig(0f, 1f, 1f, false)
        }

        mActiveConfig!!.startEditing = false

        if(name.equals("def", true)) {
            mActiveConfig!!.textureConfig(0f, 1f, 1f, false)
            mActiveConfig!!.active = false
            mActiveConfig!!.mRule = ""

            isEdited[2] = false
        } else {
            mActiveConfig!!.active = true
            mActiveConfig!!.name = name
            mActiveConfig!!.setRule(mActiveConfig!!.diff[0])

            mSeekTexture!!.setProgress((mActiveConfig!!.slierIntensity * mSeekTexture!!.max).toInt())
            isEdited[2] = true

            showTexture()
        }

        mActiveConfig!!.startEditing = true
        setFilters()

        if(!name.equals("def", true)) {
            toggleTopBar(true)
        }
    }

    private fun getMultiTouchListener(): MultiTouchListener {
        val view = findViewById<RelativeLayout>(R.id.parentView)
        val surface = findViewById<GLSurfaceView>(R.id.gpuimageview)

        return MultiTouchListener(
            null,
            view,
            null,
            true,
            mOnPhotoEditorListener,
            surface
        )
    }

    fun editText(view: View, inputText: String) {

    }

    private fun getLayout(viewType: ViewType): View {
        var rootView: View? = null
        val mTextRobotoTf = ResourcesCompat.getFont(this, R.font.lobster)

        rootView = mLayoutInflater!!.inflate(R.layout.view_photo_editor_text, null)
        val txtText = rootView!!.findViewById<TextView>(R.id.tvPhotoEditorText)
        txtText.typeface = mTextRobotoTf


        if (rootView != null) {
            //We are setting tag as ViewType to identify what type of the view it is
            //when we remove the view from stack i.e onRemoveViewListener(ViewType viewType, int numberOfAddedViews);
            rootView.tag = viewType
            val imgClose = rootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            val finalRootView = rootView
            //imgClose?.setOnClickListener { viewUndo(finalRootView, viewType) }
        }
        return rootView
    }

    override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d(
            tag,
            "onRemoveViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }
    override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d(
            tag,
            "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }
    override fun onStartViewChangeListener(viewType: ViewType) {
        Log.d(tag, "onStartViewChangeListener() called with: viewType = [$viewType]")
    }
    override fun onStopViewChangeListener(viewType: ViewType) {
        Log.d(tag, "onStopViewChangeListener() called with: viewType = [$viewType]")
    }


    /* helpers functions */

    private fun initToolsEvents() {
        val toolsRV = findViewById<RecyclerView>(R.id.rvTools)
        val effectsRV = findViewById<RecyclerView>(R.id.rvEffect)
        val texturesRV = findViewById<RecyclerView>(R.id.rvTexture)

        val filter = findViewById<View>(R.id.filter)
        val effect = findViewById<View>(R.id.effect)
        val textures = findViewById<View>(R.id.textures)

        filter.setOnClickListener{
            hideButtons(filter, effect, textures)
            hideRV(effectsRV, toolsRV, texturesRV)
            toolsRV.visibility = View.VISIBLE

            filter.background = getResources().getDrawable(R.drawable.ic_filters_check)
        }

        effect.setOnClickListener{
            hideButtons(filter, effect, textures)
            hideRV(effectsRV, toolsRV, texturesRV);
            effectsRV.visibility = View.VISIBLE

            effect.background = getResources().getDrawable(R.drawable.ic_effects_check)
        }

        textures.setOnClickListener{
            hideButtons(filter, effect, textures)
            hideRV(effectsRV, toolsRV, texturesRV);
            texturesRV.visibility = View.VISIBLE

            textures.background = getResources().getDrawable(R.drawable.ic_textures_check)
        }

        joinMood!!.setOnClickListener{
            licenseOpen()
        }
        findViewById<ConstraintLayout>(R.id.join_mood_crop).setOnClickListener{
            licenseOpen()
        }
    }

    private fun licenseOpen() {
        val intent = Intent(this@EditImageActivity, SubscribeActivity::class.java)
        startActivity(intent)
    }

    private fun hideButtons(filter: View, effect: View, textures: View) {
        filter.background = getResources().getDrawable(R.drawable.ic_filters)
        effect.background = getResources().getDrawable(R.drawable.ic_effects)
        textures.background = getResources().getDrawable(R.drawable.ic_textures)
    }

    private fun hideRV(effectsRV: RecyclerView, toolsRV: RecyclerView, texturesRV: RecyclerView) {
        effectsRV.visibility = View.GONE
        toolsRV.visibility = View.GONE
        texturesRV.visibility = View.GONE
    }

    private fun toggleLicense(show: Boolean, effect: Boolean) {
        if(show) {
            var rect: RectF? = null

            joinMood!!.visibility = View.VISIBLE
            waterMark!!.visibility = View.VISIBLE

            val constraintSet = ConstraintSet()
            constraintSet.clone(mainArea)

            if(effect) {
                val intensityLayout = findViewById<ConstraintLayout>(R.id.intensityLayout_effect)

                constraintSet.clear(joinMood!!.id, ConstraintSet.TOP)
                constraintSet.connect(joinMood!!.id, ConstraintSet.BOTTOM, intensityLayout.id, ConstraintSet.TOP, 7 * getScale().toInt())
            } else {
                val block = if (effect) findViewById<ConstraintLayout>(R.id.intensityLayout_effect) else findViewById<ConstraintLayout>(R.id.toolsLayout)
                rect = calculeRectOnScreen(block)

                constraintSet.connect(joinMood!!.id, ConstraintSet.TOP, mainArea!!.id, ConstraintSet.TOP, rect.top.toInt() - joinMood!!.height - 32 * getScale().toInt())
                constraintSet.clear(joinMood!!.id, ConstraintSet.BOTTOM)
            }

            constraintSet.applyTo(mainArea);
        } else {
            joinMood!!.visibility = View.GONE
            waterMark!!.visibility = View.GONE
        }
    }

    fun calculeRectOnScreen(view: View ): RectF {
        val location = IntArray(2)
        view.getLocationInWindow(location);

        return RectF(location[0].toFloat(), location[1].toFloat(), location[0].toFloat() + view.getMeasuredWidth(), location[1].toFloat() + view.getMeasuredHeight());
    }

    private fun changeMargin(grow: Boolean) {
        val intensity_block= findViewById<ConstraintLayout>(R.id.intensity_buttons)
        val newLayoutParams = intensity_block.layoutParams as ConstraintLayout.LayoutParams

        if(grow) {
            newLayoutParams.bottomMargin = 34 * getScale().toInt()
        } else {
            newLayoutParams.bottomMargin = 21 * getScale().toInt()
        }

        intensity_block.layoutParams = newLayoutParams
    }

    private fun initHslSeek(temp: Boolean) {
        val config = mActiveConfig!!.getHslConfig(temp)

        val seek_h = findViewById<SeekBar>(R.id.seekBar_hue)
        val seek_s = findViewById<SeekBar>(R.id.seekBar_saturation)
        val seek_l = findViewById<SeekBar>(R.id.seekBar_luminance)

        hue_container!!.findViewById<TextView>(R.id.effect_value).text = String.format("%.1f", config[0] * 10)
        saturetion_container!!.findViewById<TextView>(R.id.effect_value).text = String.format("%.1f", config[1] * 10)
        lum_container!!.findViewById<TextView>(R.id.effect_value).text = String.format("%.1f", config[2] * 10)

        seek_h.setProgress(mActiveConfig!!.calculateProgress(config[0]))
        seek_s.setProgress(mActiveConfig!!.calculateProgress(config[1]))
        seek_l.setProgress(mActiveConfig!!.calculateProgress(config[2]))
    }

    public fun getAnswer(msg: Int) {
        Log.e(Common.LOG_TAG, "Calllllback = " + msg)
    }

    private fun setFilters() {
        glImageView!!.setFilterWithConfig(calculateRules())
        /*if(mAdjustConfigs!!.get(0).active && mAdjustConfigs!!.get(0).type == EffectType.Lut) {
            glImageView!!.setLutIntensity(intensity)
            //glImageView!!.setFilterIntensity(intensity)
        }*/
    }

    private fun toggleTopBar(hide: Boolean) {
        val close = findViewById<ImageView>(R.id.topClose)
        val save = findViewById<TextView>(R.id.topSave)

        if(hide) {
            close.visibility = View.GONE
            save.visibility = View.GONE

            if(mActiveConfig != null) {
                mActiveConfig!!.editorOpen = true;
            }
        } else {
            close.visibility = View.VISIBLE
            save.visibility = View.VISIBLE

            if(mActiveConfig != null) {
                mActiveConfig!!.editorOpen = false;
            }
        }
    }
}
