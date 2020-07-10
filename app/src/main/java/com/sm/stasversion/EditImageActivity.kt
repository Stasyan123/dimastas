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
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.forEachIndexed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.sm.stasversion.classes.*
import com.sm.stasversion.crop.CropDemoPreset
import com.sm.stasversion.crop.CropImageView
import com.sm.stasversion.customFilters.GpuFilterShadowHighlight
import com.sm.stasversion.imageeditor.TextEditorDialogFragment
import com.sm.stasversion.utils.*
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import com.sm.stasversion.videoUtils.FilterType
import com.sm.stasversion.widget.HorizontalProgressWheelView
import org.wysaid.common.Common
import org.wysaid.myUtils.FileUtil
import org.wysaid.nativePort.CGEFFmpegNativeLibrary
import org.wysaid.nativePort.CGEImageHandler
import org.wysaid.nativePort.CGENativeLibrary
import org.wysaid.view.ImageGLSurfaceView

import java.io.InputStream

class EditImageActivity : AppCompatActivity(), OnPhotoEditorListener, EditingToolsAdapter.OnItemSelected, EditingEffectsAdapter.OnItemSelected,
    EditingTextureAdapter.OnItemSelected, MainFragment.OnBitmapReady, SeekBar.OnSeekBarChangeListener {

    protected var BASIC_FILTER_CONFIG: String = "@adjust lut edgy_amber.png";
    protected var CONFIG_RULES: String = "";
    protected var LOG_TAG: String = "DimaStas";

    private var mCurrentFragment: MainFragment? = null
    private var mImageView: ImageView? = null
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
    var grainImage: Bitmap? = null
    var position: Int = 0
    var mOnPhotoEditorListener: OnPhotoEditorListener? = null

    var intensity: Float = 1.0f
    val tag: String = "DimaStas"

    var imgHandler: CGEImageHandler = CGEImageHandler()

    var mThread: Thread? = null
    var mShouldStopThread = false

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

                if(mActiveConfig != null && name.equals(mActiveConfig!!.name)) {
                    return BitmapFactory.decodeStream(`is`).changeBmp(mActiveConfig!!.horizontal[1], mActiveConfig!!.vertical[1], mActiveConfig!!.rotate[1])
                } else {
                    return BitmapFactory.decodeStream(`is`)
                }

            }

            override fun loadImageOK(bmp: Bitmap, arg: Any) {
                Log.i(Common.LOG_TAG, "Loading bitmap over, you can choose to recycle or cache")
                bmp.recycle()
            }
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

        grainImage = BitmapFactory.decodeResource(this.getResources(), R.drawable.oise_light)
        val uri = intent.getParcelableExtra<Uri>("file")

        mOnPhotoEditorListener = this

        mLayoutInflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, 1);

        setPaddings()
        initEffectsNames()
        initEffectsArray()
        showSingleImage(uri)
        initSavebutton()
        initClosebutton()
        initCrop()
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
        val padding = (6.5 * getScale()).toInt()

        mSeekHue!!.setPadding(padding, 0,  padding, 0)
        mSeekSat!!.setPadding(padding, 0, padding, 0)
        mSeekLum!!.setPadding(padding, 0, padding, 0)
        mSeekBar!!.setPadding(padding, 0, padding, 0)
        mSeekTemp!!.setPadding(padding, 0, padding, 0)
        mSeekTint!!.setPadding(padding, 0, padding, 0)
        mSeekTexture!!.setPadding(padding, 0, padding, 0)
    }

    private fun initCrop() {
        val fragmentManager = getSupportFragmentManager()

        try {
            fragmentManager
                .beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(CropDemoPreset.RECT))
                .commit();
        } catch (e: Exception) {
            e.printStackTrace()
        }

        initTools()
        initProgress()
    }

    private fun initTools() {
        val rotate = findViewById<View>(R.id.rotate);
        rotate.setOnClickListener{
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

            mCurrentFragment!!.toolsSelect(rotate)
        }

        val hor = findViewById<View>(R.id.horizontal);
        hor.setOnClickListener{
            mCurrentFragment!!.toolsSelect(hor)
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
            mCurrentFragment!!.save(bitmapCrop())
        }

        val cancel = findViewById<View>(R.id.cancel_crop)
        cancel.setOnClickListener{
            val crop = findViewById<ConstraintLayout>(R.id.crop_area)
            crop.visibility = View.GONE

            val main = findViewById<ConstraintLayout>(R.id.main_area)
            main.visibility = View.VISIBLE
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

    private fun initProgress() {
        val percentTextView = findViewById<TextView>(R.id.text_straightening)
            percentTextView.text = getString(R.string.percent, 0.toString())

        val straightening = findViewById<HorizontalProgressWheelView>(R.id.rotate_scroll_wheel)
        straightening.setScrollingListener(object: HorizontalProgressWheelView.ScrollingListener {
            override fun onScrollStart() {

            }
            override fun onScroll(percent: Int) {
                percentTextView.text = getString(R.string.percent, percent.toString())

                val angl = percent.toFloat() / 2
//h = 439.4949f
//w = 1400.551f
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
            glImageView!!.setFilterWithConfig("@adjust sharpen 1.0")
        }
    }

    private fun getPixels(scale: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 26.0f * scale, this.getResources().getDisplayMetrics())
    }

    private fun initClosebutton() {
        val view = findViewById<ImageView>(R.id.topClose)

        view.setOnClickListener{
            //glImageView!!.setFilterWithConfig("@adjust lut ping.png 0.0 @adjust exposure 0.0 @adjust brightness 0.0 @adjust contrast 1.0 @adjust shadowhighlight 0.0 0.0 @adjust saturation 1.0 @adjust whitebalance 0.0 1.0 @blend sl oise_light.png 0.0 @adjust sharpen 0.0 @adjust hsl 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0");
            glImageView!!.setFilterIntensityForIndex(0.0f, 0)
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

    private fun updateView() {
        //rootView?.invalidate()
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

    private fun initView(res: Bitmap) {
        updateView()
        initToolsEvents()

        try {
            glImageView = findViewById(R.id.gpuimageview) as ImageGLSurfaceView

            cropSurface(res, true)

            glImageView!!.setSurfaceCreatedCallback( {
                glImageView!!.setImageBitmap(res)
                glImageView!!.setFilterWithConfig(calculateRules())
            })

            glImageView!!.setDisplayMode(ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FIT)

            image = res
            transImage = res

            colorsEvents(true)
            effectsEvents()
            textureEvents()
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
                if(mActiveConfig != null) {
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
                } else {
                    glImageView!!.setFilterIntensity(mSeekBar!!.progress / 100f)
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
            if(mActiveConfig != null) {
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

                mActiveConfig!!.startEditing = false
                scaleArea(false)
            } else {
                intensity = mSeekBar!!.progress / 100f
                mAdjustConfigs!!.get(0).intensity = intensity

                scaleArea(false)
            }

            toggleTopBar(false)
            hideIntensity(false)
        }

        cancel_effect.setOnClickListener {
            if(mActiveConfig != null) {
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

                mActiveConfig!!.startEditing = false
                scaleArea(false)
            } else {
                glImageView!!.setFilterIntensity(intensity)

                scaleArea(false)
            }

            toggleTopBar(false)
            hideIntensity(false)
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
        val mEditingToolsAdapter = EditingToolsAdapter(this, res, this)

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
        toggleTopBar(true)

        if(eType == EffectType.Crop) {
            startCropping(CGENativeLibrary.filterImage_MultipleEffects(transImage, calculateRules(), intensity))
            return
        }

        changeMargin(true)

        mActiveConfig = mAdjustConfigs!!.get(position)
        mActiveConfig!!.type = eType

        showEffects(eType)
		
        if(eType == EffectType.HSL) {
            initHslSeek(false)
            mActiveConfig!!.startEditing = true

            changeMargin(false)
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

                scaleArea(true)
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

    override fun onFilterSelected(fType: FilterType, pos: Int, rule: String, color: Int) {
        changeMargin(true)
        mActiveConfig = null;

        if(pos != position) {
            intensity = 1.0f
            mAdjustConfigs!!.get(0).mRule = rule
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
            item!!.findViewById<View>(R.id.viewBorder).visibility = View.GONE
            item.findViewById<ImageView>(R.id.intensity_icon).visibility = View.GONE

            if(pos != 0) {
                val itemNew = rec.getLayoutManager()!!.findViewByPosition(pos);
                val intens_new = itemNew!!.findViewById<ImageView>(R.id.intensity_icon)
                intens_new.visibility = View.VISIBLE

                val viewBorder = itemNew.findViewById<View>(R.id.viewBorder)

                if(viewBorder.tag != 1) {
                    val border = viewBorder.background as LayerDrawable
                    val shape = border.findDrawableByLayerId(R.id.shapeBorder) as GradientDrawable
                    shape.setStroke(4 * getScale().toInt(), color)
                    viewBorder.background = shape
                    viewBorder.tag = 1
                }

                viewBorder.visibility = View.VISIBLE
            }

            position = pos
        }
    }

    private fun startCropping(arr: Bitmap?) {
        val main = findViewById<ConstraintLayout>(R.id.main_area)
        val crop = findViewById<ConstraintLayout>(R.id.crop_area)

        main.visibility = View.GONE
        crop.visibility = View.VISIBLE

        //val bitmap = getInitialImage(arr)

        val img = findViewById<ImageView>(R.id.ImageView_image_test)
        if(img.width > calculatedW) {
            val frame = findViewById<FrameLayout>(R.id.container)

            val l = frame.getLayoutParams()
            l.width = (calculatedW + 20).toInt()
            frame.setLayoutParams(l)
        }

        if(mCurrentFragment!!.isBmInit()) {
            mImageView!!.setImageBitmap(arr)
        } else {
            mCurrentFragment!!.setFrame(findViewById<FrameLayout>(R.id.container))

            mCurrentFragment!!.setImageBm(arr, findViewById<FrameLayout>(R.id.container))
            mImageView = findViewById<ImageView>(R.id.ImageView_image)
        }
    }

    override fun onBitmapCropped(bm: Bitmap) {
        glImageView!!.setImageBitmap(bm)
        setFilters()

        val crop = findViewById<ConstraintLayout>(R.id.crop_area)
        crop.visibility = View.GONE

        val main = findViewById<ConstraintLayout>(R.id.main_area)
        main.visibility = View.VISIBLE

        cropSurface(bm, false)
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

    private fun cropSurface(bOutput: Bitmap, changeDimens: Boolean) {
        val scale = getScale()
        val pixels = (30.0f * scale + 0.5f)

        val w = getWidth()
        val h = canvasHeight * scale

        val factor_w = h / bOutput.height.toFloat()
        val width = (bOutput.width * factor_w) - 20

        val factor_h = (w - pixels) / bOutput.width.toFloat()
        val height = (bOutput.height * factor_h) - 5

        if(changeDimens) {
            calculatedW = width
            calculatedH = height
        }

        val layoutParams = glImageView!!.layoutParams
        layoutParams.height = height.toInt()
        layoutParams.width = width.toInt()
        glImageView!!.layoutParams = layoutParams
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

        mAdjustConfigs!!.forEachIndexed() { index, config ->
            if(config.active) {
                rule += " " + config.getRule()
            }
        }

        return rule
    }

    private fun initEffectsArray() {
        val temperature = AdjustConfig(6, -1.0f, 0.0f, 1.0f, "@adjust whitebalance", 0.5f, true, EffectType.Temperature, null)
        temperature.setAdditional(AdjustConfig(5, 0.0f, 1.0f, 2.0f, "", 0.5f, true, EffectType.Temperature, null))

        val sh = AdjustConfig(4, -100.0f, 0.0f, 100.0f, "@adjust shadowhighlight", 0.5f, true, EffectType.Shadow, null)
        sh.setAdditional(AdjustConfig(4, -100.0f, 0.0f, 100.0f, "", 0.5f, true, EffectType.Highlight, null))

        val hslConfig = AdjustConfig(9, -1.0f, 0.0f, 1.0f, "@adjust hsl", 0.5f, false, EffectType.HSL, null)
        hslConfig.hsl = arrayOf(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f))
        hslConfig.tempHsl = arrayOf(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f))

        mAdjustConfigs = mutableListOf(
            AdjustConfig(0, -1.0f, 0.0f, 1.0f, "@adjust lut empty.png", 0.5f, false, EffectType.Lut, null),
            AdjustConfig(1, -1.0f, 0.0f, 1.0f, "@adjust exposure", 0.5f, false, EffectType.Exposition, null),
            AdjustConfig(2, -.5f, 0.0f, 0.5f, "@adjust brightness", 0.5f, false, EffectType.Brightness, null),
            AdjustConfig(3, .0f, 1.0f, 2.0f, "@adjust contrast", 0.5f, false, EffectType.Contrast, null),
            sh,
            AdjustConfig(5, 0.0f, 1.0f, 2.0f, "@adjust saturation", 0.5f, false, EffectType.Saturation, null),
            temperature,
            AdjustConfig(7, .0f, 0.0f, 1.0f, "@blend sl oise_light.png", 0f, false, EffectType.Grain, null),
            AdjustConfig(8, 0f, 0.0f, 2.5f, "@adjust sharpen", 0f, false, EffectType.Sharpness, null),
            hslConfig,
            AdjustConfig(10, 0f, 0.5f, 1f, "", 1f, false, EffectType.Texture, null)
        )

        mAdjustConfigs!!.get(10).active = false
        mAdjustConfigs!!.get(10).intensity = 1.0f
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
        } else {
            mActiveConfig!!.active = true
            mActiveConfig!!.name = name
            mActiveConfig!!.setRule(mActiveConfig!!.diff[0])

            mSeekTexture!!.setProgress((mActiveConfig!!.slierIntensity * mSeekTexture!!.max).toInt())

            toggleTopBar(true)
            showTexture()
        }

        mActiveConfig!!.startEditing = true

        setFilters()
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

    private fun setFilters() {
        glImageView!!.setFilterWithConfig(calculateRules())
        glImageView!!.setFilterIntensity(intensity)
    }

    private fun toggleTopBar(hide: Boolean) {
        val close = findViewById<ImageView>(R.id.topClose)
        val save = findViewById<TextView>(R.id.topSave)

        if(hide) {
            close.visibility = View.GONE
            save.visibility = View.GONE
        } else {
            close.visibility = View.VISIBLE
            save.visibility = View.VISIBLE
        }
    }
}
