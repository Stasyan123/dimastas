package com.sm.stasversion

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daasuu.gpuv.composer.FillMode
import com.daasuu.gpuv.composer.FillModeCustomItem
import com.daasuu.gpuv.composer.GPUMp4Composer
import com.daasuu.gpuv.composer.Rotation
import com.daasuu.gpuv.egl.filter.*
import com.daasuu.gpuv.player.GPUPlayerView
import com.daasuu.gpuv.player.PlayerScaleType
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.sm.stasversion.crop.BitmapUtils
import com.sm.stasversion.crop.CropImageOptions
import com.sm.stasversion.crop.CropImageView
import com.sm.stasversion.crop.CropOverlayView
import com.sm.stasversion.customFilters.StasFilter
import com.sm.stasversion.customFilters.StasOverlayBlendSample
import com.sm.stasversion.customFilters.StasOverlaySample
import com.sm.stasversion.utils.*
import com.sm.stasversion.videoClass.GPUPlayerViewO
import com.sm.stasversion.videoUtils.FilterType
import com.sm.stasversion.videoUtils.GlBitmapOverlaySample
import com.sm.stasversion.widget.MovieWrapperView
import jp.co.cyberagent.android.gpuimage.filter.*
import kotlinx.android.synthetic.main.activity_edit_image.view.*
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

class NewVideoOverviewActivity : AppCompatActivity(), EditingToolsAdapter.OnItemSelected, EditingEffectsAdapter.OnItemSelected,
    EditingTextureAdapter.OnItemSelected, EditingTexturesAdapter.OnItemSelected {

    private val TAG = "VideoOverview"

    private var calculatedW: Float = 0.0f
    private var calculatedH: Float = 0.0f
    private var scale: Float = 1.0f

    private var gpuPlayerView: GPUPlayerViewO? = null;
    private var gpuWrapper: MovieWrapperView? = null;

    private val mImagePoints = FloatArray(8)
    private val mScaleImagePoints = FloatArray(8)

    private var player: SimpleExoPlayer? = null;
    private var GPUMp4Composer: GPUMp4Composer? = null

    var filterVideo1: GlLookUpTableFilter? = null
    var filterVideo3: GlLookUpTableFilter? = null
    var filterVideo2: GlLookUpTableFilter? = null

    var mCropOverlayView: CropOverlayView? = null
    var mDimension = IntArray(2)

    /* textures */
    var scuffed = GlFilter()
    var glare = GlFilter()
    var rainbow = GlFilter()
    var dust = GlFilter()

    var intensity: Float = 1.0f
    var position: Int = 0

    var effectsList: MutableList<GlFilter>? = null
    var eArray: MutableMap<EffectType, SeekInfo>? = null

    var grainImage: Bitmap? = null

    var addedViews: MutableList<View>? = null

    var sepia: GPUImageSepiaToneFilter = GPUImageSepiaToneFilter()

    var isLookup: Boolean = false

    var gpuLookupFilter: StasFilter? = null
    private var glFilter: StasFilter? = null
    private var emptyFilter = GlFilter()

    private var videoPath: String? = null

    private var uri: Uri? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_video_overview)

        uri = intent.getParcelableExtra<Uri>("file");

        val t = getVideoResolution(uri!!.path);

        grainImage = BitmapFactory.decodeResource(this.getResources(), R.drawable.oise_light)

        initEffectsArray()
        setUpPlayer()
        setUpViews()
        initSavebutton()
        initClosebutton()
    }

    private fun initEffectsArray() {
        addedViews = java.util.ArrayList<View>();

        eArray = mutableMapOf(
            EffectType.Exposition to SeekInfo(-1.0f, 1.0f, 0.0f, "Exposition", EffectType.Exposition),
            EffectType.Brightness to SeekInfo(-0.5f, 0.5f, 0.0f, "Brightness", EffectType.Brightness),
            EffectType.Contrast to SeekInfo(0.0f, 2.0f, 1.0f, "Contrast", EffectType.Contrast),
            EffectType.Shadow to SeekInfo(0.0f, 0.35f, 0.0f, "Shadow", EffectType.Shadow),
            EffectType.Highlight to SeekInfo(0.0f, 1.0f, 1.0f, "Highlight", EffectType.Highlight),
            EffectType.Saturation to SeekInfo(0.0f, 2.0f, 1.0f, "Saturation", EffectType.Saturation),
            EffectType.Sharpness to SeekInfo(-0.4f, 0.4f, 0.0f, "Sharpness", EffectType.Sharpness),
            EffectType.Temperature to SeekInfo(4000.0f, 6000.0f, 5000.0f, "Temperature", EffectType.Temperature),
            EffectType.Grain to SeekInfo(0.0f, 100.0f, 0.0f, "Grain", EffectType.Grain)
        )

       /* val gFilter = GPUImageSoftLightBlendFilter()
        gFilter.bitmap = getOpacityImage(0)*/

        val highlight = GlHighlightShadowFilter()
        highlight.setHighlights(eArray!![EffectType.Highlight]!!.current)
        highlight.setShadows(eArray!![EffectType.Shadow]!!.current)

        val exp = GlExposureFilter()
        exp.setExposure(eArray!![EffectType.Exposition]!!.current)

        val brightness = GlBrightnessFilter()
        brightness.setBrightness(eArray!![EffectType.Brightness]!!.current)

        val saturation = GlSaturationFilter()
        saturation.setSaturation(eArray!![EffectType.Saturation]!!.current)

        val sharpen = GlSharpenFilter()
        sharpen.sharpness = eArray!![EffectType.Sharpness]!!.current

        /*val whiteBalance = GlWhiteBalanceFilter()
        whiteBalance.setTemperature(eArray!![EffectType.Temperature]!!.current)
        whiteBalance.setTint(0.0f)*/

        val gFilter = StasOverlaySample()
        gFilter.setBitmap(getOpacityImage(0))

        val contrast = GlContrastFilter()
        contrast.setContrast(eArray!![EffectType.Contrast]!!.current)

        effectsList = mutableListOf(
            GlFilter(),
            gFilter,
            exp,
            brightness,
            highlight,
            saturation,
            sharpen,
            GlFilter(),
            contrast
        )
    }

    override fun onEffectSelected(eType: EffectType, position: Int?) {
        var intensity = 0.0f

        val temp_list = effectsList!!.toMutableList()
        val exposition = GlExposureFilter()
        val contrast = GlContrastFilter()
        val brightness = GlBrightnessFilter()
        val shadow = GlHighlightShadowFilter()
        shadow.setHighlights(eArray!![EffectType.Highlight]!!.current)
        shadow.setShadows(eArray!![EffectType.Shadow]!!.current)
        val saturation = GlSaturationFilter()
        val sharpness = GlSharpenFilter()
        //val temperature = GlWhiteBalanceFilter()
        val grainFilter = StasOverlaySample()

        if(isLookup != false) {
            temp_list[0] = gpuLookupFilter!!
        } else {
            temp_list[0] =  GlFilter()
        }
        effectsList!![0] = temp_list[0]
        val effect = eArray!![eType]

        val temporary_d = temp_list.toMutableList()
        temporary_d.add(scuffed)
        temporary_d.add(glare)
        temporary_d.add(rainbow)
        temporary_d.add(dust)

        if(eType == EffectType.Crop) {
            startCropping()
            return
        }

        if(effect != null) {
            //gpuPlayerView!!.setGlFilter(GlFilterGroup(temporary_d))


            gpuPlayerView!!.setGlFilter(GlFilterGroup(temporary_d))

            val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
            val intens = findViewById<ConstraintLayout>(R.id.intensityLayout_effect)
            val seek = findViewById<SeekBar>(R.id.seekBar_effect)
            var pr = 0
            if(eType == EffectType.Grain) {
                pr = effect.current.toInt()
            } else {
                pr = getCurrent(effect.min, effect.max, effect.current)
            }
            seek.setOnSeekBarChangeListener(null)
            seek.setProgress(pr)

            val apply = findViewById<View>(R.id.apply_effect_button)
            val cancel = findViewById<View>(R.id.cancel_effect_button)

            apply.setOnClickListener{
                effectsList = temp_list
                effect.current = intensity

                tools.visibility = View.VISIBLE
                intens.visibility = View.GONE
            }

            cancel.setOnClickListener{
                val temporary_c = effectsList!!.toMutableList()
                /*temporary_c.add(scuffed)
                temporary_c.add(glare)
                temporary_c.add(rainbow)
                temporary_c.add(dust)*/
                gpuPlayerView!!.setGlFilter(GlFilterGroup(temporary_d))

                tools.visibility = View.VISIBLE
                intens.visibility = View.GONE
            }

            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    intensity = getIntensity(effect.min, effect.max, progress)

                    val temporary = temp_list.toMutableList()
                    /*temporary.add(scuffed)
                    temporary.add(glare)
                    temporary.add(rainbow)
                    temporary.add(dust)*/

                    when (eType) {
                        EffectType.Grain -> {
                            grainFilter.bitmap = getOpacityImage(progress)
                            temp_list[1] = grainFilter
                            temporary[1] = grainFilter

                            //gpu!!.setFilter(GPUImageFilterGroup(temporary))
                        }
                        EffectType.Exposition -> {
                            exposition.setExposure(intensity)
                            temp_list[2] = exposition
                            temporary[2] = exposition

                            //gpuPlayerView!!.setGlFilter(GlFilterGroup(temporary))
                        }
                        EffectType.Contrast -> {
                            contrast.setContrast(intensity)
                            temp_list[8] = contrast
                            temporary[8] = contrast

                            //gpu!!.setFilter(GPUImageFilterGroup(temporary))
                        }
                        EffectType.Brightness -> {
                            brightness.setBrightness(intensity)
                            temp_list[3] = brightness
                            temporary[3] = brightness

                            //gpu!!.setFilter(GPUImageFilterGroup(temporary))
                        }
                        EffectType.Shadow -> {
                            shadow.setShadows(intensity)
                            temp_list[4] = shadow
                            temporary[4] = shadow

                            //gpu!!.setFilter(GPUImageFilterGroup(temporary))
                        }
                        EffectType.Highlight -> {
                            shadow.setHighlights(intensity)
                            temp_list[4] = shadow
                            temporary[4] = shadow

                            //gpu!!.setFilter(GPUImageFilterGroup(temporary))
                        }
                        EffectType.Saturation -> {
                            saturation.setSaturation(intensity)
                            temp_list[5] = saturation
                            temporary[5] = saturation

                            //gpu!!.setFilter(GPUImageFilterGroup(temporary))
                        }
                        EffectType.Sharpness -> {
                            sharpness.setSharpness(intensity)
                            temp_list[6] = sharpness
                            temporary[6] = sharpness

                            //gpu!!.setFilter(GPUImageFilterGroup(temporary))
                        }
                        EffectType.Temperature -> {
                            /*temperature.setTemperature(intensity)
                            temp_list[7] = temperature
                            temporary[7] = temperature*/

                            //gpu!!.setFilter(GPUImageFilterGroup(temporary))
                        }
                        else -> {
                            // Note the block
                            //gpu!!.setFilter(GPUImageFilter())
                        }
                    }
                    gpuPlayerView!!.setGlFilter(GlFilterGroup(temporary))
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                }

            })

            tools.visibility = View.GONE
            intens.visibility = View.VISIBLE
        }
    }

    override fun onFilterSelected(fType: FilterType, pos: Int, rule: String) {
        if(pos != position) {
            intensity = 1.0f
        }

        if(fType == FilterType.DEFAULT) {
            isLookup = false
            gpuPlayerView!!.setGlFilter(emptyFilter)
        } else {
            isLookup = true

            glFilter = FilterType.createGlFilter(fType, this);
            gpuLookupFilter = FilterType.createGlFilter(fType, this);

            gpuPlayerView!!.setGlFilter(gpuLookupFilter)
            gpuLookupFilter!!.setIntensity(intensity)
        }

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
            intens.visibility = GONE

            if(pos != 0) {
                val itemNew = rec.getLayoutManager()!!.findViewByPosition(pos);
                val intens_new = itemNew!!.findViewById<ImageView>(R.id.intensity_icon)
                intens_new.visibility = VISIBLE
            }

            position = pos
        }
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
            scuffed = emptyFilter
            glare = emptyFilter
            rainbow = emptyFilter
            dust = emptyFilter

            val temp_list = effectsList!!.toMutableList()
            temp_list.add(scuffed)
            temp_list.add(glare)
            temp_list.add(rainbow)
            temp_list.add(dust)

            gpuPlayerView!!.setGlFilter(GlFilterGroup(temp_list))
        }
    }

    override fun onTexturesSelected(tType: TextureType?, texture: Int, position: Int?) {
        var filter: GlFilter? = null;

        val temp_list = effectsList!!.toMutableList()
        if(isLookup != false) {
            temp_list[0] = gpuLookupFilter!!
        } else {
            temp_list[0] = emptyFilter
        }

        if(position == 0) {
            filter = emptyFilter
        } else {
            filter = StasOverlayBlendSample();
            filter.bitmap = BitmapFactory.decodeResource(this.getResources(), texture)
        }

        when (tType) {
            TextureType.SCUFFED -> {
                scuffed = filter
            }
            TextureType.GLARE -> {
                glare = filter
            }
            TextureType.RAINBOW -> {
                rainbow = filter
            }
            TextureType.DUST -> {
                dust = filter
            }
            else -> { // Note the block

            }
        }

        temp_list.add(scuffed)
        temp_list.add(glare)
        temp_list.add(rainbow)
        temp_list.add(dust)

        gpuPlayerView!!.setGlFilter(GlFilterGroup(temp_list))
    }

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

    private fun setUpViews() {
        initToolsEvents()

        gpuWrapper = findViewById(R.id.layout_movie_wrapper)

        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        var temp_filter: StasFilter
        var temp_intensity = 0.0f

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                val pr = progress.toFloat() / 100.0f

                temp_filter = gpuLookupFilter!!
                temp_filter.setIntensity(pr)
                temp_intensity = pr

                gpuPlayerView!!.setGlFilter(temp_filter)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        val cancel = findViewById<View>(R.id.cancel_button)
        val apply = findViewById<View>(R.id.apply_button)

        cancel.setOnClickListener{
            gpuLookupFilter!!.setIntensity(intensity)
            gpuPlayerView!!.setGlFilter(gpuLookupFilter)

            hideIntensity()
        }

        apply.setOnClickListener{
            intensity = temp_intensity
            glFilter!!.intensity = intensity

            hideIntensity()
        }

        mCropOverlayView = findViewById(R.id.CropOverlayView)

        val all = findViewById<View>(R.id.custom)
        all.setOnClickListener{
            mCropOverlayView!!.setFixedAspectRatio(false)
            mCropOverlayView!!.cropWindowRect = RectF(0f, 0f,
                gpuPlayerView!!.width.toFloat(), gpuPlayerView!!.height.toFloat())
            mCropOverlayView!!.invalidate()
        }

        val rect = findViewById<View>(R.id.one)
        rect.setOnClickListener{
            var width = gpuPlayerView!!.width
            var height = gpuPlayerView!!.height

            if (width > height) {
                width = height;
            } else {
                height = width;
            }

            val lOffset = (gpuPlayerView!!.width - width) / 2
            val tOffset = (gpuPlayerView!!.height - height) / 2

            mCropOverlayView!!.setFixedAspectRatio(true)
            mCropOverlayView!!.cropWindowRect = RectF(lOffset.toFloat(), tOffset.toFloat(),
                width + lOffset.toFloat(), height + tOffset.toFloat())
            mCropOverlayView!!.invalidate()
        }

        val threefour = findViewById<View>(R.id.threefour)
        threefour.setOnClickListener{
            changeOverlay(3, 4)
        }

        val fourthree = findViewById<View>(R.id.fourthree)
        fourthree.setOnClickListener{
            changeOverlay(4, 3)
        }

        val acrop = findViewById<View>(R.id.accept)
        acrop.setOnClickListener{
            val overlay = mCropOverlayView!!.getCropWindowRect()

            val lp = gpuWrapper!!.layoutParams
            lp.height = (overlay.bottom - overlay.top).toInt()
            lp.width = (overlay.right - overlay.left).toInt()
            gpuWrapper!!.layoutParams = lp

            gpuPlayerView!!.translationY = -overlay.top
            gpuPlayerView!!.translationX = -overlay.left

            mCropOverlayView!!.visibility = GONE

            val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
            val crop = findViewById<ConstraintLayout>(R.id.crop_area)
            val intensity = findViewById<ConstraintLayout>(R.id.bottom_bar)

            crop.visibility = GONE
            intensity.visibility = GONE
            tools.visibility = VISIBLE
        }

        val dcrop = findViewById<View>(R.id.cancel)
        dcrop.setOnClickListener{
            val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
            val crop = findViewById<ConstraintLayout>(R.id.crop_area)
            val intensity = findViewById<ConstraintLayout>(R.id.bottom_bar)

            val lp = gpuWrapper!!.layoutParams

            lp.width = mDimension[0]
            lp.height = mDimension[1]

            gpuWrapper!!.layoutParams = lp

            mCropOverlayView!!.visibility = GONE
            crop.visibility = GONE
            intensity.visibility = GONE
            tools.visibility = VISIBLE
        }

        initRvEffects()
        initRvTexture()
        initRvTools();
    }

    private fun changeOverlay(wRatio: Int, hRatio: Int) {
        var width = gpuPlayerView!!.width
        var height = gpuPlayerView!!.height
        val resizedWidth = (height / hRatio * wRatio)
        val resizedHeight = (width / wRatio) * hRatio

        if (resizedWidth > width) {
            height = resizedHeight;
        } else if (resizedHeight > height) {
            width = resizedWidth;
        }

        val lOffset = (gpuPlayerView!!.width - width) / 2
        val tOffset = (gpuPlayerView!!.height - height) / 2

        mCropOverlayView!!.setFixedAspectRatio(false)
        mCropOverlayView!!.cropWindowRect = RectF(lOffset.toFloat(), tOffset.toFloat(),
            width + lOffset.toFloat(), height + tOffset.toFloat())
        mCropOverlayView!!.invalidate()
    }

    private fun gcd(p: Int, q: Int): Int {
        if (q == 0) return p;
        else return gcd(q, p % q);
    }

    private fun ratio(a: Int, b: Int): IntArray {
        val gcd = gcd(a,b);
        val retArr = IntArray(2)

        if(a > b) {
            retArr[0] = a/gcd;
            retArr[1] = b/gcd;
        } else {
            retArr[0] = b/gcd;
            retArr[1] = a/gcd;
        }

        return retArr
    }

    private fun hideIntensity() {
        val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
        val intensity = findViewById<ConstraintLayout>(R.id.intensityLayout)

        tools.visibility = View.VISIBLE
        intensity.visibility = View.GONE
    }

    private fun initRvEffects() {
        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val mEffects = findViewById<RecyclerView>(R.id.rvEffect)
        val mEditingEffectAdapter = EditingEffectsAdapter(this)

        mEffects.layoutManager = llmTools
        mEffects.setAdapter(mEditingEffectAdapter);
    }

    private fun initRvTexture() {
        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val mTextures = findViewById<RecyclerView>(R.id.rvTexture)
        val mEditingTextureAdapter = EditingTextureAdapter(this)

        mTextures.layoutManager = llmTools
        mTextures.setAdapter(mEditingTextureAdapter);
    }

    private fun initRvTools() {
        val retriever = MediaMetadataRetriever();
        retriever.setDataSource(this, uri);
        //Get one "frame"/bitmap - * NOTE - no time was set, so the first available frame will be used
        val bmp = retriever.getFrameAtTime();

        val filter1 = GPUImageLookupFilter()
        val flt1 = BitmapFactory.decodeResource(this.getResources(), R.drawable.filter1)
        filter1.bitmap = flt1
        filter1.setIntensity(1.0f)

        filterVideo1 = GlLookUpTableFilter(BitmapFactory.decodeResource(this.getResources(), R.drawable.filter_video1))

        val filter3 = GPUImageLookupFilter()
        val flt3 = BitmapFactory.decodeResource(this.getResources(), R.drawable.filter3)
        filter3.bitmap = flt3
        filter3.setIntensity(1.0f)

        filterVideo3 = GlLookUpTableFilter(BitmapFactory.decodeResource(this.getResources(), R.drawable.filter_video3))

        val filter2 = GPUImageLookupFilter()
        val flt2 = BitmapFactory.decodeResource(this.getResources(), R.drawable.filter2)
        filter2.bitmap = flt2
        filter2.setIntensity(1.0f)

        filterVideo2 = GlLookUpTableFilter(BitmapFactory.decodeResource(this.getResources(), R.drawable.filter_video3))

        var w = 0.0f;
        var h = 0.0f;
        val factor = bmp.height / bmp.width.toFloat()

        if(factor > 1) {
            w = 200.0f
            h = w * factor
        } else {
            h = 200.0f
            w = h / factor
        }

        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val mTools = findViewById<RecyclerView>(R.id.rvTools)
        val mEditingToolsAdapter = EditingToolsAdapter(this, Bitmap.createScaledBitmap(bmp, w.toInt(), h.toInt(), false), this)

        mTools.layoutManager = llmTools
        mTools.setAdapter(mEditingToolsAdapter);
    }

    private fun setUpSimpleExoPlayer() {
        val trackSelector = DefaultTrackSelector();

        // Measures bandwidth during playback. Can be null if not required.
        val defaultBandwidthMeter = DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"), defaultBandwidthMeter);
        val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);

        // SimpleExoPlayer
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        /*val playerView = findViewById<SimpleExoPlayerView>(R.id.player_view);
            playerView.setPlayer(player);
            playerView.setKeepScreenOn(true);*/
        //player!!.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        player!!.repeatMode = Player.REPEAT_MODE_ONE
        player!!.volume = 0.0f
        //player!!.setVideoSurfaceView(gpuPlayerView)
        // Prepare the player with the source.
        player!!.prepare(mediaSource);
        player!!.setPlayWhenReady(true)
    }

    private fun getWidth(): Int{
        val display = getWindowManager().getDefaultDisplay();
        val size = Point();
        display.getSize(size);

        return size.x
    }

    private fun setUoGlPlayerView() {
        //player!!.videoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        gpuPlayerView!!.setSimpleExoPlayer(player)

        val retriever = MediaMetadataRetriever();
        retriever.setDataSource(this, uri);
        //Get one "frame"/bitmap - * NOTE - no time was set, so the first available frame will be used
        val bmp = retriever.getFrameAtTime();

        //Get the bitmap width and height
        val videoWidth = bmp.getWidth();
        val videoHeight = bmp.getHeight();

        if(videoHeight > videoWidth) {
            gpuPlayerView!!.setPlayerScaleType(PlayerScaleType.RESIZE_FIT_HEIGHT)
        }
        //gpuPlayerView!!.setPlayerScaleType(PlayerScaleType.RESIZE_NONE)
        //(findViewById<MovieWrapperView>(R.id.layout_movie_wrapper)).addView(gpuPlayerView);
        gpuPlayerView!!.onResume();
    }

    private fun initSavebutton() {
        findViewById<TextView>(R.id.topSave).setOnClickListener { v ->
            startCodec()
        }
    }

    private fun initClosebutton() {
        findViewById<View>(R.id.topClose).setOnClickListener { v ->
            val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
            val crop = findViewById<ConstraintLayout>(R.id.crop_area)

            crop.visibility = GONE
            tools.visibility = VISIBLE
        }
    }

    private fun getRelativeLeft(myView: GPUPlayerView): Int {
        if (myView.getParent() == myView.getRootView())
            return myView.getLeft();
        else {
            return  myView.left - findViewById<MovieWrapperView>(R.id.layout_movie_wrapper).left;
        }
    }

    private fun getRelativeTop(myView: GPUPlayerView): Int {
        if (myView.getParent() == myView.getRootView())
            return myView.getTop();
        else
            return findViewById<MovieWrapperView>(R.id.layout_movie_wrapper).top - myView.top
    }

    /** Get width of the bounding rectangle of the given points.  */
    internal fun getRectWidth(points: FloatArray): Float {
        return getRectRight(points) - getRectLeft(points)
    }

    /** Get height of the bounding rectangle of the given points.  */
    internal fun getRectHeight(points: FloatArray): Float {
        return getRectBottom(points) - getRectTop(points)
    }

    /** Get left value of the bounding rectangle of the given points.  */
    internal fun getRectLeft(points: FloatArray): Float {
        return Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6])
    }

    /** Get top value of the bounding rectangle of the given points.  */
    internal fun getRectTop(points: FloatArray): Float {
        return Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7])
    }

    /** Get right value of the bounding rectangle of the given points.  */
    internal fun getRectRight(points: FloatArray): Float {
        return Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6])
    }

    /** Get bottom value of the bounding rectangle of the given points.  */
    internal fun getRectBottom(points: FloatArray): Float {
        return Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7])
    }

    private fun setUpPlayer() {
        gpuPlayerView = findViewById(R.id.gpuimageview)
        //val m = gpuPlayerView!!.matrix
    }

    override fun onResume() {
        super.onResume()
        setUpSimpleExoPlayer()
        setUoGlPlayerView()
    }

    public fun getVideoResolution(path: String?): Size {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path);
        val width = Integer.valueOf(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        );
        val height = Integer.valueOf(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        );
        retriever.release();

        return Size(width, height)
    }

    private fun calculateDimension() {
        val overlay = mCropOverlayView!!.getCropWindowRect()
        val h = mCropOverlayView!!.height
        val w = mCropOverlayView!!.width
        val h2 = gpuPlayerView!!.height;
        val w2 = gpuPlayerView!!.width
        val f = 1;
    }

    private fun startCodec() {
        videoPath = getVideoFilePath()

        releasePlayer()

        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.max = 100

        if(isLookup) {
            effectsList!![0] = glFilter!!
        } else {
            effectsList!![0] = emptyFilter
        }

        var mode = FillMode.PRESERVE_ASPECT_FIT

        calculateDimension();
        val resolution = getVideoResolution(uri!!.getPath());

        var w = 0; var h = 0;

        if(gpuPlayerView!!.height > gpuPlayerView!!.width) {
            if(resolution.height > resolution.width) {
                w = resolution.width
                h = resolution.height
            } else {
                h = resolution.width
                w = resolution.height
            }
        } else {
            if(resolution.height < resolution.width) {
                w = resolution.width
                h = resolution.height
            } else {
                h = resolution.width
                w = resolution.height
            }
        }

        var fillItem = FillModeCustomItem(0f, 0f, 0f, 0f, 0f, 0f);

        if(!mCropOverlayView!!.getCropWindowRect().isEmpty) {
            mode = FillMode.CUSTOM
        } else {
            val scale = FillMode.getScaleAspectCrop(90, resolution.width, resolution.height, w, h);
            val g = (1 / scale[0]) + .17f;
            fillItem = FillModeCustomItem(
                g,
                gpuPlayerView!!.rotation,
                0f,
                0f,
                resolution.width.toFloat(),
                resolution.height.toFloat())
        }



        GPUMp4Composer = null
        GPUMp4Composer = GPUMp4Composer(uri!!.getPath(), videoPath)
            .size(w, h)
            .filter(GlFilterGroup(effectsList))
            .fillMode(mode)
            //.customFillMode(fillItem)
            .listener(object : GPUMp4Composer.Listener {
                override fun onProgress(progress: Double) {
                    Log.d(TAG, "onProgress = $progress")
                    runOnUiThread { progressBar.progress = (progress * 100).toInt() }
                }

                override fun onCompleted() {
                    Log.d(TAG, "onCompleted()")
                    exportMp4ToGallery(applicationContext, videoPath!!)
                    runOnUiThread {
                        progressBar.progress = 100
                        Toast.makeText(
                            this@NewVideoOverviewActivity,
                            "codec complete path =$videoPath",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCanceled() {
                    Log.d(TAG, "onCanceled()")
                }

                override fun onFailed(exception: Exception) {
                    Log.d(TAG, "onFailed()")
                }
            })
            .start()


    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    private fun releasePlayer() {
        if(player != null) {
            gpuPlayerView!!.onPause();
            //(findViewById<MovieWrapperView>(R.id.layout_movie_wrapper)).removeAllViews();
            player!!.stop();
            player!!.release();
            player = null;
        }
    }

    fun getAndroidMoviesFolder(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
    }

    fun getVideoFilePath(): String {
        return getAndroidMoviesFolder().absolutePath + "/" + SimpleDateFormat("yyyyMM_dd-HHmmss").format(
            Date()
        ) + "filter_apply.mp4"
    }

    private fun getOpacityImage(opacity: Int): Bitmap {
        val newBitmap = Bitmap.createBitmap(grainImage!!.getWidth(), grainImage!!.getHeight(), Bitmap.Config.ARGB_8888);

        val canvas = Canvas(newBitmap);
        // create a paint instance with alpha
        val alphaPaint = Paint();
        alphaPaint.setAlpha(opacity);
        // now lets draw using alphaPaint instance
        if(grainImage != null) {
            canvas.drawBitmap(grainImage!!, 0.0f, 0.0f, alphaPaint)
        }

        return newBitmap
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

    private fun getIntensity(min: Float, max: Float, progress: Int): Float {
        var intensity = 0.0f

        if(min > 0) {
            val factor = (max - min) / 100
            intensity = (progress * factor) + min
        } else {
            intensity = ((progress * (Math.abs(min) + max)) / 100) - Math.abs(min)
        }

        val res = BigDecimal(intensity.toDouble()).setScale(2, RoundingMode.HALF_EVEN)

        return res.toFloat()
    }

    fun startCropping() {
        val lp = gpuWrapper!!.layoutParams

        mDimension[0] = lp.width
        mDimension[1] = lp.height

        lp.width = gpuPlayerView!!.width
        lp.height = gpuPlayerView!!.height

        val params = FrameLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            gpuPlayerView!!.height
        ).apply {
            gravity = Gravity.START
        }

        gpuPlayerView!!.layoutParams = params

        gpuPlayerView!!.translationY = 0f
        gpuPlayerView!!.translationX = 0f

        gpuWrapper!!.layoutParams = lp

        initCropBar()
        initOverlay()
    }

    private fun initCropBar() {
        val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
        val crop = findViewById<ConstraintLayout>(R.id.crop_area)
        val intensity = findViewById<ConstraintLayout>(R.id.bottom_bar)
        val tab_name = intensity.findViewById<TextView>(R.id.tab_name)

        tab_name.text = getResources().getString(R.string.crop);

        tools.visibility = GONE
        crop.visibility = VISIBLE
        intensity.visibility = VISIBLE
    }

    fun initOverlay() {
        val overlay = mCropOverlayView!!.getCropWindowRect()

        if(overlay.isEmpty) {
            val options = CropImageOptions()
            options.fixAspectRatio = true

            mCropOverlayView!!.setInitialAttributeValues(options)

            mImagePoints[0] = 0f
            mImagePoints[1] = 0f
            mImagePoints[2] = gpuPlayerView!!.width.toFloat()
            mImagePoints[3] = 0f
            mImagePoints[4] = gpuPlayerView!!.width.toFloat()
            mImagePoints[5] = gpuPlayerView!!.height.toFloat()
            mImagePoints[6] = 0f
            mImagePoints[7] = gpuPlayerView!!.height.toFloat()

            mCropOverlayView!!.setCropWindowLimits(
                gpuPlayerView!!.width.toFloat(), gpuPlayerView!!.height.toFloat(), 1.0f, 1.0f
            )

            mCropOverlayView!!.setFixedAspectRatio(false)

            mCropOverlayView!!.setBounds(
                mImagePoints,
                gpuPlayerView!!.width,
                gpuPlayerView!!.height
            )
        }
        //mCropOverlayView!!.setInitialCropWindowRect(null)
        mCropOverlayView!!.visibility = VISIBLE
    }

    fun exportMp4ToGallery(context: Context, filePath: String) {
        // ビデオのメタデータを作成する
        val values = ContentValues(2)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATA, filePath)
        // MediaStoreに登録
        context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        )
        context.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://$filePath")
            )
        )
    }
}
