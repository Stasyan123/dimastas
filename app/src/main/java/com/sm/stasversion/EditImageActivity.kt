package com.sm.stasversion

import android.content.ClipData
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
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
import org.wysaid.common.Common
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
    var rootView: ConstraintLayout? =  null

    /* effects */
    var mLayoutInflater: LayoutInflater? = null

    var image: Bitmap? = null
    var transImage: Bitmap? = null
    var grainImage: Bitmap? = null
    var position: Int = 0
    var gpu: GPUImage? = null
    var isLookup: Boolean = false
    var gpuFilter: GPUImageFilter = GPUImageFilter()
    var gpuLookupFilter: GPUImageLookupFilter = GPUImageLookupFilter()
    var mOnPhotoEditorListener: OnPhotoEditorListener? = null

    var effectsList: MutableList<GPUImageFilter>? = null
    var intensity: Float = 1.0f
    val tag: String = "DimaStas"


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

        grainImage = BitmapFactory.decodeResource(this.getResources(), R.drawable.oise_light)
        val uri = intent.getParcelableExtra<Uri>("file")

        mOnPhotoEditorListener = this

        mLayoutInflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, 1);

        setPaddings()
        initEffectsArray()
        showSingleImage(uri)
        initSavebutton()
        initClosebutton()
        initCrop()
    }

    private fun setPaddings() {
        val right = 13
        val left = 13

        findViewById<SeekBar>(R.id.seekBar_hue).setPadding(left, 0,  right, 0);
        findViewById<SeekBar>(R.id.seekBar_saturation).setPadding(left, 0, right, 0);
        findViewById<SeekBar>(R.id.seekBar_luminance).setPadding(left, 0, right, 0);
        findViewById<SeekBar>(R.id.seekBar).setPadding(left, 0, right, 0);
        findViewById<SeekBar>(R.id.seekBar_temperature).setPadding(left, 0, right, 0);
        findViewById<SeekBar>(R.id.seekBar_tint).setPadding(left, 0, right, 0);
        findViewById<SeekBar>(R.id.seekBar_straightening).setPadding(left, 0, right, 0);
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

        }

        val accept = findViewById<View>(R.id.apply_crop)
        accept.setOnClickListener{
            gpu!!.deleteImage()
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
        val seek = findViewById<SeekBar>(R.id.seekBar_straightening)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                val angl = progress - 15
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

                mCurrentFragment!!.straighten(angl.toFloat(), scale)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })
    }

    private fun initSavebutton() {
        var t: Bitmap? = null
        var test = true
        val v = findViewById<View>(R.id.topSave)

        v.setOnClickListener{
            if(test) {
                glImageView!!.getResultBitmap(object :
                    ImageGLSurfaceView.QueryResultBitmapCallback {
                    override fun get(bmp: Bitmap?) {
                        t = bmp
                        test = false
                    }
                })
            } else {
                glImageView!!.setImageBitmap(t)
            }
        }
    }

    private fun getPixels(scale: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 26.0f * scale, this.getResources().getDisplayMetrics())
    }

    private fun initClosebutton() {
        val view = findViewById<View>(R.id.topClose)

        view.setOnClickListener{
            glImageView!!.setFilterWithConfig("@adjust hsl 0.02 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0");
        }
    }

    private fun showSingleImage(uri: Uri) {
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

    private fun hideIntensity() {
        val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
        val intensity = findViewById<ConstraintLayout>(R.id.intensityLayout)

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

            glImageView!!.setSurfaceCreatedCallback(ImageGLSurfaceView.OnSurfaceCreatedCallback {
                glImageView!!.setImageBitmap(res)
                glImageView!!.setFilterWithConfig(calculateRules());
            })

            glImageView!!.setDisplayMode(ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FIT)

            image = res
            transImage =res

            colorsEvents(true)
            effectsEvents()
        } catch (e: IOException) {
            e.printStackTrace()
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
        var pr = 0.0f

        mSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                if(mActiveConfig != null) {
                    pr = progress.toFloat() / 100.0f

                    if (mActiveConfig!!.type == EffectType.Shadow) {
                        mActiveConfig!!.setTempIntensityWithParam(
                            2,
                            pr,
                            mActiveConfig!!.calcIntensity(mActiveConfig!!.additionaItem.slierIntensity),
                            0.0f,
                            glImageView
                        )
                    } else if (mActiveConfig!!.type == EffectType.Highlight) {
                        mActiveConfig!!.setTempIntensityWithParam(
                            2,
                            mActiveConfig!!.slierIntensity,
                            mActiveConfig!!.calcIntensity(pr),
                            0.0f,
                            glImageView
                        )
                    } else {
                        mActiveConfig!!.setTempIntensity(pr, true, glImageView)
                    }
                } else {
                    pr = progress.toFloat() / 100.0f
                    glImageView!!.setFilterIntensity(pr);
                }
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
                        pr,
                        mActiveConfig!!.additionaItem.slierIntensity,
                        glImageView,
                        false
                    )
                } else if (mActiveConfig!!.type == EffectType.HSL) {
                    copyArray(mActiveConfig!!.tempHsl, mActiveConfig!!.hsl)
                    glImageView!!.setFilterWithConfig(calculateRules())
                } else if (mActiveConfig!!.type == EffectType.Highlight) {
                    mActiveConfig!!.setIntensityWithParam(
                        8,
                        mActiveConfig!!.slierIntensity,
                        pr,
                        glImageView,
                        false
                    )
                } else {
                    mActiveConfig!!.setIntensity(pr, false, glImageView)
                }

                mActiveConfig!!.startEditing = false
                scaleArea(false)
            } else {
                intensity = pr

                scaleArea(false)
            }

            hideIntensity()
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
                    glImageView!!.setFilterWithConfig(calculateRules())
                } else {
                    mActiveConfig!!.setIntensity(mActiveConfig!!.slierIntensity, true, glImageView)
                }

                mActiveConfig!!.startEditing = false
                scaleArea(false)
            } else {
                glImageView!!.setFilterIntensity(intensity)

                scaleArea(false)
            }

            hideIntensity()
        }
    }

    override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
        if(mActiveConfig!!.startEditing) {
            if (mActiveConfig!!.type == EffectType.Temperature) {
                mActiveConfig!!.setTempIntensityWithParam(
                    7,
                    mSeekTemp!!.progress / 100.0f,
                    mActiveConfig!!.additionaItem.calcIntensity(mSeekTint!!.progress / 100.0f),
                    0.0f,
                    glImageView
                )
            } else {
                mActiveConfig!!.setTempIntensityWithParam(
                    mActiveConfig!!.hslPos,
                    mSeekHue!!.progress / 100.0f,
                    mActiveConfig!!.calcIntensity(mSeekSat!!.progress / 100.0f),
                    mActiveConfig!!.calcIntensity(mSeekLum!!.progress / 100.0f),
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

            mActiveConfig!!.startEditing = true
        } else if(eType == EffectType.Highlight) {
            mSeekBar!!.setProgress((mActiveConfig!!.additionaItem.slierIntensity * mSeekBar!!.max).toInt())
        } else {
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

    override fun onFilterSelected(fType: FilterType, pos: Int, rule: String) {
        changeMargin(true)
        mActiveConfig = null;

        if(pos != position) {
            intensity = 1.0f
            mAdjustConfigs!!.get(0).mRule = rule
        }

        glImageView!!.setFilterWithConfig(calculateRules())
        glImageView!!.setFilterIntensity(intensity)

        val rec = findViewById<RecyclerView>(R.id.rvTools)
        val item = rec.getLayoutManager()!!.findViewByPosition(position);

        if(pos == position) {
            if(pos != 0) {
                mSeekBar!!.setProgress((intensity * 100.0f).toInt())

                findViewById<ConstraintLayout>(R.id.toolsLayout).visibility = View.GONE
                findViewById<ConstraintLayout>(R.id.intensityLayout).visibility = View.VISIBLE
                findViewById<ConstraintLayout>(R.id.intensity_buttons).visibility = View.VISIBLE
            }
        } else {
            val intens = item!!.findViewById<ImageView>(R.id.intensity_icon)
            intens.visibility = View.GONE

            if(pos != 0) {
                val itemNew = rec.getLayoutManager()!!.findViewByPosition(pos);
                val intens_new = itemNew!!.findViewById<ImageView>(R.id.intensity_icon)
                intens_new.visibility = View.VISIBLE
            }

            position = pos
        }
    }

    private fun startCropping(arr: Bitmap) {
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
        val crop = findViewById<ConstraintLayout>(R.id.crop_area)
        crop.visibility = View.GONE

        val main = findViewById<ConstraintLayout>(R.id.main_area)
        main.visibility = View.VISIBLE

        cropSurface(bm, false)
        image = bm

        gpu!!.setImage(bm)
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
        val temperature = AdjustConfig(6, -1.0f, 0.0f, 1.0f, "@adjust whitebalance", 0.5f, true, EffectType.Temperature)
        temperature.setAdditional(AdjustConfig(5, 0.0f, 1.0f, 2.0f, "", 0.5f, true, EffectType.Temperature))

        val sh = AdjustConfig(4, -100.0f, 0.0f, 100.0f, "@adjust shadowhighlight", 0.5f, true, EffectType.Shadow)
        sh.setAdditional(AdjustConfig(4, -100.0f, 0.0f, 100.0f, "", 0.5f, true, EffectType.Highlight))

        val hslConfig = AdjustConfig(9, -1.0f, 0.0f, 1.0f, "@adjust hsl", 0.5f, false, EffectType.HSL)
        hslConfig.hsl = arrayOf(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f))
        hslConfig.tempHsl = arrayOf(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f))

        mAdjustConfigs = mutableListOf(
            AdjustConfig(0, -1.0f, 0.0f, 1.0f, "@adjust lut empty.png", 0.5f, false, EffectType.Lut),
            AdjustConfig(1, -1.0f, 0.0f, 1.0f, "@adjust exposure", 0.5f, false, EffectType.Exposition),
            AdjustConfig(2, -.5f, 0.0f, 0.5f, "@adjust brightness", 0.5f, false, EffectType.Brightness),
            AdjustConfig(3, .0f, 1.0f, 2.0f, "@adjust contrast", 0.5f, false, EffectType.Contrast),
            sh,
            AdjustConfig(5, 0.0f, 1.0f, 2.0f, "@adjust saturation", 0.5f, false, EffectType.Saturation),
            temperature,
            AdjustConfig(7, .0f, 0.0f, 1.0f, "@blend sl oise_light.png", 0f, false, EffectType.Grain),
            AdjustConfig(8, 0f, 0.0f, 2.5f, "@adjust sharpen", 0f, false, EffectType.Sharpness),
            hslConfig,
            AdjustConfig(10, 0f, 0.0f, 1.0f, "@adjust sharpen", 0f, false, EffectType.Texture)
        )

        mAdjustConfigs!!.get(10).active = false
    }

    private fun copyArray(ar1: Array<FloatArray>, ar2: Array<FloatArray>) {
        for (i in 0..ar1.size - 1) {
            ar2[i] = Arrays.copyOf(ar1[i], ar1[i].size);
        }
    }

    override fun onTextureSelected(textureType: TextureType?, position: Int?) {
        if(textureType != TextureType.DEFAULT) {
            /*gpu!!.setImage(image)

            val temp_list = effectsList!!.toMutableList()
            if(isLookup != false) {
                temp_list[0] = gpuLookupFilter
            } else {
                temp_list[0] =  gpuFilter
            }

            val filter: GPUImageFilter

            if(pos == 0) {
                filter = GPUImageFilter()
            } else {
                filter = GPUImageScreenBlendFilter()
                filter.bitmap = BitmapFactory.decodeResource(this.getResources(), texture!!)
            }*/
        } else {

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

        seek_h.setProgress(mActiveConfig!!.calculateProgress(config[0]))
        seek_s.setProgress(mActiveConfig!!.calculateProgress(config[1]))
        seek_l.setProgress(mActiveConfig!!.calculateProgress(config[2]))
    }
}
