package com.sm.stasversion

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
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
    EditingTextureAdapter.OnItemSelected, EditingTexturesAdapter.OnItemSelected, MainFragment.OnBitmapReady {

    protected var BASIC_FILTER_CONFIG: String = "@adjust lut edgy_amber.png";
    protected var CONFIG_RULES: String = "";
    protected var LOG_TAG: String = "DimaStas";

    private var mCurrentFragment: MainFragment? = null
    private var mImageView: ImageView? = null
    private var glImageView: ImageGLSurfaceView? = null

    private var calculatedW: Float = 0.0f
    private var calculatedH: Float = 0.0f
    private var scale: Float = 1.0f
    private var mActiveConfig: AdjustConfig? = null
    private var mAdjustConfigs: MutableList<AdjustConfig>? = null

    var mSeekBar:SeekBar? = null
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
    var eArray: MutableMap<EffectType, SeekInfo>? = null
    var intensity: Float = 1.0f

    var addedViews: MutableList<View>? = null
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

        rootView = findViewById<ConstraintLayout>(R.id.root)
        mSeekBar = findViewById<SeekBar>(R.id.seekBar_effect)

        grainImage = BitmapFactory.decodeResource(this.getResources(), R.drawable.oise_light)
        val uri = intent.getParcelableExtra<Uri>("file")

        mOnPhotoEditorListener = this

        mLayoutInflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, 1);

        initEffectsArray()
        showSingleImage(uri)
        initSavebutton()
        initClosebutton()
        initCrop()
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
        val v = findViewById<View>(R.id.topSave)

        v.setOnClickListener{
            glImageView!!.setFilterWithConfig("@adjust hsl 0.02 -0.31 -0.17");
        }
    }

    private fun getPixels(scale: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 26.0f * scale, this.getResources().getDisplayMetrics())
    }

    private fun initClosebutton() {
        val view = findViewById<View>(R.id.topClose)

        view.setOnClickListener{
            glImageView!!.setFilterWithConfig("@adjust hsl 0.0 0.0 0.0");
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

            })

            glImageView!!.setDisplayMode(ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FIT)

            image = res
            transImage = res

            val seekBar = findViewById<SeekBar>(R.id.seekBar)
            var temp_intensity = 0.0f

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    temp_intensity = progress.toFloat() / 100.0f
                    glImageView!!.setFilterIntensity(temp_intensity);
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            })

            val cancel = findViewById<View>(R.id.cancel_button)
            val apply = findViewById<View>(R.id.apply_button)

            cancel.setOnClickListener{
                glImageView!!.setFilterIntensity(intensity)
                hideIntensity()

                scaleArea(false)
            }
            apply.setOnClickListener{
                intensity = temp_intensity
                hideIntensity()

                scaleArea(false)
            }

            effectsEvents()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    protected fun calcIntensity(_intensity: Float): Float {
        val result: Float
        if (_intensity <= 0.0f) {
            result = -100f
        } else if (_intensity >= 1.0f) {
            result = 200f
        } else if (_intensity <= 0.5f) {
            result = -100f + (0 - -100f) * _intensity * 2.0f
        } else {
            result = 200f + (0 - 200f) * (1.0f - _intensity) * 2.0f
        }
        return result
    }

    private fun effectsEvents() {
        var pr = 0.0f

        mSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                pr = progress.toFloat() / 100.0f

                if(mActiveConfig!!.type == EffectType.Shadow) {
                    mActiveConfig!!.setTempIntensityWithParam(2,  pr, mActiveConfig!!.additionaItem.slierIntensity, glImageView)
                } else if(mActiveConfig!!.type == EffectType.Highlight) {
                    mActiveConfig!!.setTempIntensityWithParam(2, mActiveConfig!!.slierIntensity, pr, glImageView)
                } else {
                    mActiveConfig!!.setTempIntensity(pr, true, glImageView)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        val apply_effect = findViewById<View>(R.id.apply_effect_button)
        val cancel_effect = findViewById<View>(R.id.cancel_effect_button)
        val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
        val intens = findViewById<ConstraintLayout>(R.id.intensityLayout_effect)

        val seek_temp = findViewById<SeekBar>(R.id.seekBar_temperature)
        val seek_tint = findViewById<SeekBar>(R.id.seekBar_tint)
        seek_temp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                pr = progress.toFloat() / 100.0f
                if(mActiveConfig!!.startEditing) {
                    mActiveConfig!!.setTempIntensityWithParam(
                        1,
                        pr,
                        seek_tint.progress / 100.0f,
                        glImageView
                    )
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        seek_tint.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                pr = progress.toFloat() / 100.0f
                mActiveConfig!!.setTempIntensityWithParam(1, seek_temp.progress / 100.0f, pr, glImageView)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        apply_effect.setOnClickListener {
            if(mActiveConfig!!.type == EffectType.Temperature) {
                mActiveConfig!!.setIntensityWithParam(1, seek_temp.progress / 100.0f, seek_tint.progress / 100.0f, glImageView, false)
            } else if (mActiveConfig!!.type == EffectType.Shadow) {
                mActiveConfig!!.setIntensityWithParam(2, pr, mActiveConfig!!.additionaItem.slierIntensity, glImageView, false)
            } else if (mActiveConfig!!.type == EffectType.Highlight) {
                mActiveConfig!!.setIntensityWithParam(2, mActiveConfig!!.slierIntensity, pr, glImageView, false)
            } else {
                mActiveConfig!!.setIntensity(pr, false, glImageView)
            }

            tools.visibility = View.VISIBLE
            intens.visibility = View.GONE

            mActiveConfig!!.startEditing = false
            scaleArea(false)
        }

        cancel_effect.setOnClickListener {
            if(mActiveConfig!!.type == EffectType.Temperature) {
                mActiveConfig!!.setIntensityWithParam(1, mActiveConfig!!.slierIntensity, mActiveConfig!!.additionaItem.slierIntensity, glImageView, true)
            } else if (mActiveConfig!!.type == EffectType.Shadow || mActiveConfig!!.type == EffectType.Highlight) {
                mActiveConfig!!.setIntensityWithParam(2, mActiveConfig!!.slierIntensity, mActiveConfig!!.additionaItem.slierIntensity, glImageView, true)
            } else {
                mActiveConfig!!.setIntensity(mActiveConfig!!.slierIntensity, true, glImageView)
            }

            tools.visibility = View.VISIBLE
            intens.visibility = View.GONE

            mActiveConfig!!.startEditing = false
            scaleArea(false)
        }
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
        mActiveConfig = mAdjustConfigs!!.get(position)
        mActiveConfig!!.type = eType

        showEffects(eType)

        if(eType == EffectType.Temperature) {

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
            params.height = (435 * getScale()).toInt()
        } else {
            params.height = (470 * getScale()).toInt()
        }

        parent.layoutParams = params
    }

    private fun showEffects(type: EffectType) {
        val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
        val intens = findViewById<ConstraintLayout>(R.id.intensityLayout_effect)

        val temp = findViewById<ConstraintLayout>(R.id.container_temperature)
        val tint = findViewById<ConstraintLayout>(R.id.container_tint)
        val effects = findViewById<SeekBar>(R.id.seekBar_effect)

        tools.visibility = View.GONE
        intens.visibility = View.VISIBLE

        when (type) {
            EffectType.Temperature -> {
                temp.visibility = View.VISIBLE
                tint.visibility = View.VISIBLE
                effects.visibility = View.GONE

                scaleArea(true)
            }
            else -> {
                temp.visibility = View.GONE
                tint.visibility = View.GONE
                effects.visibility = View.VISIBLE
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
                val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
                val intens = findViewById<ConstraintLayout>(R.id.intensityLayout)
                val seek = findViewById<SeekBar>(R.id.seekBar)
                seek.setProgress((intensity * 100.0f).toInt())

                tools.visibility = View.GONE
                intens.visibility = View.VISIBLE
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

    private fun getScale(): Float {
        return this.getResources().getDisplayMetrics().density
    }

    private fun cropSurface(bOutput: Bitmap, changeDimens: Boolean) {
        val scale = getScale()
        val pixels = (30.0f * scale + 0.5f)

        val w = getWidth()
        val h = 470 * scale

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
            rule += " " + config.getRule()
        }

        return rule
    }

    private fun initEffectsArray() {
        addedViews = java.util.ArrayList<View>();

        //AdjustConfig(4, -200.0f, 0.0f, 100.0f, "@adjust shadowhighlight", 0.5f, true),
//            AdjustConfig(5, -100.0f, 0.0f, 200.0f, "@adjust shadowhighlight", 0.5f, true),

        val temperature = AdjustConfig(6, -1.0f, 0.0f, 1.0f, "@adjust whitebalance", 0.5f, true, EffectType.Temperature)
        temperature.setAdditional(AdjustConfig(5, 0.0f, 1.0f, 2.0f, "", 0.5f, true, EffectType.Temperature))

        val sh = AdjustConfig(4, -100.0f, 0.0f, 100.0f, "@adjust shadowhighlight", 0.5f, true, EffectType.Shadow)
        sh.setAdditional(AdjustConfig(4, -100.0f, 0.0f, 100.0f, "", 0.5f, true, EffectType.Highlight))

        mAdjustConfigs = mutableListOf(
            AdjustConfig(0, -1.0f, 0.0f, 1.0f, "@adjust lut empty.png", 0.5f, false, EffectType.Lut),
            AdjustConfig(1, -1.0f, 0.0f, 1.0f, "@adjust exposure", 0.5f, false, EffectType.Exposition),
            AdjustConfig(2, -.5f, 0.0f, 0.5f, "@adjust brightness", 0.5f, false, EffectType.Brightness),
            AdjustConfig(3, .0f, 1.0f, 2.0f, "@adjust contrast", 0.5f, false, EffectType.Contrast),
            sh,
            AdjustConfig(5, 0.0f, 1.0f, 2.0f, "@adjust saturation", 0.5f, false, EffectType.Saturation),
            temperature,
            AdjustConfig(7, .0f, 0.0f, 1.0f, "@blend sl oise_light.png", 0f, false, EffectType.Grain),
            AdjustConfig(8, 0f, 0.0f, 2.5f, "@adjust sharpen", 0f, false, EffectType.Sharpness)
        )
    }

    override fun onTextureSelected(textureType: TextureType?, position: Int?) {
        if(textureType != TextureType.DEFAULT) {
            val toolsRV = findViewById<RecyclerView>(R.id.rvTools)
            val effectsRV = findViewById<RecyclerView>(R.id.rvEffect)
            val texturesRV = findViewById<RecyclerView>(R.id.rvTexture)
            val texturesKindRV = findViewById<RecyclerView>(R.id.rvTextures)

            hideRV(effectsRV, toolsRV, texturesRV, texturesKindRV)

            val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            val mTexturesKind = findViewById<RecyclerView>(R.id.rvTextures)
            val adapter = EditingTexturesAdapter(this, textureType)

            mTexturesKind.layoutManager = llmTools
            mTexturesKind.setAdapter(adapter)

            texturesKindRV.visibility = View.VISIBLE
        } else {

        }
    }

    override fun onTexturesSelected(tType: TextureType?, texture: Int?, pos: Int?) {
        gpu!!.setImage(image)

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
        val inputTextView = view.findViewById<TextView>(R.id.tvPhotoEditorText)
        val parentView = findViewById<RelativeLayout>(R.id.parentView)

        if (inputTextView != null && addedViews!!.contains(view) && !TextUtils.isEmpty(inputText)) {
            inputTextView.setText(inputText);

            parentView.updateViewLayout(view, view.getLayoutParams())
            val i = addedViews!!.indexOf(view)
            if (i > -1) addedViews!!.set(i, view)
        }
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
        val texturesKindRV = findViewById<RecyclerView>(R.id.rvTextures)

        val filter = findViewById<View>(R.id.filter)
        val effect = findViewById<View>(R.id.effect)
        val textures = findViewById<View>(R.id.textures)

        filter.setOnClickListener{
            hideButtons(filter, effect, textures)
            hideRV(effectsRV, toolsRV, texturesRV, texturesKindRV)
            toolsRV.visibility = View.VISIBLE

            filter.background = getResources().getDrawable(R.drawable.ic_filters_check)
        }

        effect.setOnClickListener{
            hideButtons(filter, effect, textures)
            hideRV(effectsRV, toolsRV, texturesRV, texturesKindRV);
            effectsRV.visibility = View.VISIBLE

            effect.background = getResources().getDrawable(R.drawable.ic_effects_check)
        }

        textures.setOnClickListener{
            hideButtons(filter, effect, textures)
            hideRV(effectsRV, toolsRV, texturesRV, texturesKindRV);
            texturesRV.visibility = View.VISIBLE

            textures.background = getResources().getDrawable(R.drawable.ic_textures_check)
        }
    }

    private fun hideButtons(filter: View, effect: View, textures: View) {
        filter.background = getResources().getDrawable(R.drawable.ic_filters)
        effect.background = getResources().getDrawable(R.drawable.ic_effects)
        textures.background = getResources().getDrawable(R.drawable.ic_textures)
    }

    private fun hideRV(effectsRV: RecyclerView, toolsRV: RecyclerView, texturesRV: RecyclerView, texturesKindRV: RecyclerView) {
        effectsRV.visibility = View.GONE
        toolsRV.visibility = View.GONE
        texturesRV.visibility = View.GONE
        texturesKindRV.visibility = View.GONE
    }
}
