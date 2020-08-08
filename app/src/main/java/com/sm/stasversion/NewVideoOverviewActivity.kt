package com.sm.stasversion

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.daasuu.gpuv.composer.GPUMp4Composer
import com.daasuu.gpuv.composer.Rotation
import com.daasuu.gpuv.egl.filter.*
import com.daasuu.gpuv.player.GPUPlayerView
import com.daasuu.gpuv.player.PlayerScaleType
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.google.android.exoplayer2.C
import com.google.gson.Gson
import com.sm.stasversion.classes.AdjustConfig
import com.sm.stasversion.classes.AppDatabase
import com.sm.stasversion.classes.serializedConfigs
import com.sm.stasversion.crop.BitmapUtils
import com.sm.stasversion.crop.CropImageOptions
import com.sm.stasversion.crop.CropImageView
import com.sm.stasversion.crop.CropOverlayView
import com.sm.stasversion.utils.*
import com.sm.stasversion.videoClass.GPUPlayerViewO
import com.sm.stasversion.videoUtils.FilterType
import com.sm.stasversion.widget.HorizontalProgressWheelView
import com.sm.stasversion.widget.MovieWrapperView
import kotlinx.android.synthetic.main.activity_edit_image.view.*
import org.wysaid.common.Common
import org.wysaid.myUtils.FileUtil
import org.wysaid.nativePort.CGEFFmpegNativeLibrary
import org.wysaid.nativePort.CGENativeLibrary
import org.wysaid.texUtils.CropInfo
import org.wysaid.view.VideoPlayerGLSurfaceView
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class NewVideoOverviewActivity : AppCompatActivity(), EditingToolsAdapter.OnItemSelected, EditingEffectsAdapter.OnItemSelected,
    EditingTextureAdapter.OnItemSelected, SeekBar.OnSeekBarChangeListener {

    private val TAG = "VideoOverview"

    private var db: AppDatabase? = null
    private var imgId: Int? = null
    private var correction: String? = null
    private var crop: String? = null

    private var canvasHeight: Float = 470.0f
    private var calculatedW: Float = 0.0f
    private var calculatedH: Float = 0.0f
    private var scale: Float = 1.0f

    var mSeekBar: SeekBar? = null
    var mSeekTemp:SeekBar? = null
    var mSeekTint:SeekBar? = null
    var mSeekHue:SeekBar? = null
    var mSeekSat:SeekBar? = null
    var mSeekLum:SeekBar? = null
    var mSeekTexture:SeekBar? = null

    var textIntensity: TextView? = null
    var hue_container: ConstraintLayout? = null
    var saturetion_container: ConstraintLayout? = null
    var lum_container: ConstraintLayout? = null
    var temp_container: ConstraintLayout? = null
    var tint_container: ConstraintLayout? = null

    private var gpuPlayerView: GPUPlayerViewO? = null
    private var gpuWrapper: FrameLayout? = null

    private var mPlayerView: VideoPlayerGLSurfaceView? = null
    private var videoView: VideoPlayerGLSurfaceView? = null

    private val mImagePoints = FloatArray(8)
    private val mScaleImagePoints = FloatArray(8)

    private var GPUMp4Composer: GPUMp4Composer? = null

    var mCropOverlayView: CropOverlayView? = null
    var mDimension = IntArray(2)

    var intensity: Float = 1.0f
    var position: Int = 0

    var effectsList: MutableList<GlFilter>? = null
    var eArray: MutableMap<EffectType, SeekInfo>? = null

    var grainImage: Bitmap? = null

    var addedViews: MutableList<View>? = null

    protected var mThread: Thread? = null
    protected var mShouldStopThread = false

    private var mActiveConfig: AdjustConfig? = null
    private var mAdjustConfigs: MutableList<AdjustConfig>? = null

    private var videoPath: String? = null

    private var uri: Uri? = null;

    val playCompletionCallback: VideoPlayerGLSurfaceView.PlayCompletionCallback =
        object: VideoPlayerGLSurfaceView.PlayCompletionCallback {
            override fun playComplete(player: MediaPlayer) {
                player.start();
            }

            override fun playFailed(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
                return true;
            }
        }

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

    private fun Bitmap.changeBmp(x: Float, y: Float, degrees: Float): Bitmap {

        val matrix = Matrix().apply { postScale(x, y, width / 2f, height / 2f) }
        matrix.apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_video_overview)

        mCropOverlayView = findViewById(R.id.CropOverlayView)
        gpuWrapper = findViewById(R.id.parentView)

        mSeekBar = findViewById(R.id.seekBar)
        mSeekTemp = findViewById(R.id.seekBar_temperature)
        mSeekTint = findViewById(R.id.seekBar_tint)
        mSeekHue = findViewById(R.id.seekBar_hue)
        mSeekSat = findViewById(R.id.seekBar_saturation)
        mSeekLum = findViewById(R.id.seekBar_luminance)
        mSeekTexture = findViewById(R.id.seekBar_texture)

        mPlayerView = findViewById(R.id.videoGLSurfaceView)

        val bundle = intent.extras

        uri = intent.getParcelableExtra<Uri>("file");
        imgId = bundle!!.getInt("imgId")
        correction = bundle.getString("configs")
        crop = bundle.getString("crop")

        textIntensity = findViewById(R.id.text_intensity)

        //val tr = FileUtil.getPath() + "/blendVideo4.mp4"
        //val t = getVideoResolution(uri!!.path);

        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, 1);
        grainImage = BitmapFactory.decodeResource(this.getResources(), R.drawable.oise_light)

        initDB()
        initEffectsNames()
        setPaddings()
        initEffectsArray()
        setUpViews()
        initSavebutton()
        initClosebutton()
        colorsEvents(true)

        mPlayerView!!.setParentView(gpuWrapper)
        mPlayerView!!.setPlayerInitializeCallback(object: VideoPlayerGLSurfaceView.PlayerInitializeCallback {
            override fun initPlayer(player: MediaPlayer) {
                player.setOnBufferingUpdateListener(object: MediaPlayer.OnBufferingUpdateListener {
                    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
                        if (percent == 100) {
                            player.setOnBufferingUpdateListener(null);
                        }
                    }
                })
            }
        });
    }

    fun initCropConfig() {
        if(crop != null && !crop!!.isEmpty()) {
            val cropInfo = Gson().fromJson(crop, CropInfo::class.java)

            mPlayerView!!.cropInfo = cropInfo
            mPlayerView!!.setCrop(true);
            mPlayerView!!.calcDimension(mPlayerView!!.mWrapperWidth.toInt(), mPlayerView!!.mWrapperHeight.toInt(), cropInfo.overlay.width().toInt(), cropInfo.overlay.height().toInt(), true)

            mCropOverlayView!!.cancelCrop()
        }
    }

    private fun initDB() {
        db = Room.databaseBuilder(this.applicationContext, AppDatabase::class.java, "mood_v4")
            .allowMainThreadQueries().build()
    }

    private fun copyArray(ar1: Array<FloatArray>, ar2: Array<FloatArray>) {
        for (i in 0..ar1.size - 1) {
            ar2[i] = Arrays.copyOf(ar1[i], ar1[i].size);
        }
    }

    private fun initEffectsArray() {
        if(correction != null && !correction!!.isEmpty()) {
            val gson = serializedConfigs()
            gson.setPlayer(mPlayerView)

            mAdjustConfigs = gson.decryptConfigs(correction)
        } else {
            val temperature = AdjustConfig(
                6,
                -1.0f,
                0.0f,
                1.0f,
                "@adjust whitebalance",
                0.5f,
                true,
                EffectType.Temperature,
                null
            )
            val temp_add =
                AdjustConfig(6, 0.0f, 1.0f, 2.0f, "", 0.5f, true, EffectType.Temperature, null)
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

            val hslConfig = AdjustConfig(
                9,
                -1.0f,
                0.0f,
                1.0f,
                "@adjust hsl",
                0.5f,
                false,
                EffectType.HSL,
                mPlayerView
            )
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
                    -1.0f,
                    0.0f,
                    1.0f,
                    "@adjust lut empty.png",
                    0.5f,
                    false,
                    EffectType.Lut,
                    mPlayerView
                ),
                AdjustConfig(
                    1,
                    -1.0f,
                    0.0f,
                    1.0f,
                    "@adjust exposure",
                    0.5f,
                    false,
                    EffectType.Exposition,
                    mPlayerView
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
                    mPlayerView
                ),
                AdjustConfig(
                    3,
                    .0f,
                    1.0f,
                    2.0f,
                    "@adjust contrast",
                    0.5f,
                    false,
                    EffectType.Contrast,
                    mPlayerView
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
                    mPlayerView
                ),
                temperature,
                AdjustConfig(
                    7,
                    .0f,
                    0.0f,
                    1.0f,
                    "@blend sl oise_light.png",
                    0f,
                    false,
                    EffectType.Grain,
                    mPlayerView
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
                    mPlayerView
                ),
                hslConfig,
                AdjustConfig(10, 0f, 0.5f, 1f, "", 1f, false, EffectType.Texture, mPlayerView)
            )

            mAdjustConfigs!!.get(10).active = false
            mAdjustConfigs!!.get(10).intensity = 1.0f
        }
    }

    override fun onEffectSelected(eType: EffectType, position: Int) {
        toggleTopBar(true)

        if(eType == EffectType.Crop) {
            startCropping()
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

    override fun onFilterSelected(fType: FilterType, pos: Int, rule: String, color: Int) {
        //changeMargin(true)
        mActiveConfig = null;

        if(pos != position) {
            intensity = 1.0f
            mAdjustConfigs!!.get(0).mRule = rule
            mAdjustConfigs!!.get(0).intensity = intensity
        }

        setFilters()

        val rec = findViewById<RecyclerView>(R.id.rvTools)
        val item = rec.getLayoutManager()!!.findViewByPosition(position);

        if(pos == position) {
            if(pos != 0) {
                //textIntensity!!.text = ((intensity * 100.0f).toInt()).toString()
                mSeekBar!!.setProgress((intensity * 100.0f).toInt())

                findViewById<ConstraintLayout>(R.id.toolsLayout).visibility = GONE
                findViewById<ConstraintLayout>(R.id.intensityLayout).visibility = VISIBLE
                findViewById<ConstraintLayout>(R.id.intensity_buttons).visibility = VISIBLE

                toggleTopBar(true)
            }
        } else {
            item!!.findViewById<View>(R.id.viewBorder).visibility = GONE
            item.findViewById<ImageView>(R.id.intensity_icon).visibility = GONE

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

    override fun onTextureSelected(name: String, position: Int) {
        mActiveConfig = mAdjustConfigs!!.get(position)

        if(mActiveConfig != null && !name.equals(mActiveConfig!!.name)) {
            mActiveConfig!!.setIntensity(1f, false, null)
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

    private fun showTexture() {
        findViewById<ConstraintLayout>(R.id.toolsLayout).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.texture_container).visibility = View.VISIBLE
    }

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

    private fun setUpSeek() {
        mSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                if(mActiveConfig != null) {
                    if (mActiveConfig!!.type == EffectType.Shadow) {
                        mActiveConfig!!.setTempIntensityWithParam(
                            8,
                            mSeekBar!!.progress / 100f,
                            mActiveConfig!!.calcIntensity(mActiveConfig!!.additionaItem.slierIntensity),
                            0.0f,
                            null
                        )
                    } else if (mActiveConfig!!.type == EffectType.Highlight) {
                        mActiveConfig!!.setTempIntensityWithParam(
                            8,
                            mActiveConfig!!.slierIntensity,
                            mActiveConfig!!.calcIntensity(mSeekBar!!.progress / 100f),
                            0.0f,
                            null
                        )
                    } else {
                        mActiveConfig!!.setTempIntensity(mSeekBar!!.progress / 100f, true, null)
                    }
                } else {
                    mPlayerView!!.setFilterIntensity(mSeekBar!!.progress / 100f)
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

        apply_effect.setOnClickListener {
            if(mActiveConfig != null) {
                if (mActiveConfig!!.type == EffectType.Temperature) {
                    mActiveConfig!!.setIntensityWithParam(
                        7,
                        mSeekTemp!!.progress / 100.0f,
                        mSeekTint!!.progress / 100.0f,
                        null,
                        false
                    )
                } else if (mActiveConfig!!.type == EffectType.Shadow) {
                    mActiveConfig!!.setIntensityWithParam(
                        8,
                        mSeekBar!!.progress / 100f,
                        mActiveConfig!!.additionaItem.slierIntensity,
                        null,
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
                        null,
                        false
                    )
                } else {
                    mActiveConfig!!.setIntensity(mSeekBar!!.progress / 100f, false, null)
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
                        null,
                        true
                    )
                } else if (mActiveConfig!!.type == EffectType.Shadow || mActiveConfig!!.type == EffectType.Highlight) {
                    mActiveConfig!!.setIntensityWithParam(
                        8,
                        mActiveConfig!!.slierIntensity,
                        mActiveConfig!!.additionaItem.slierIntensity,
                        null,
                        true
                    )
                } else if (mActiveConfig!!.type == EffectType.HSL) {
                    copyArray(mActiveConfig!!.hsl, mActiveConfig!!.tempHsl)

                    setFilters()
                } else {
                    mActiveConfig!!.setIntensity(mActiveConfig!!.slierIntensity, true, null)
                }

                mActiveConfig!!.startEditing = false
                scaleArea(false)
            } else {
                mPlayerView!!.setFilterIntensity(intensity)

                scaleArea(false)
            }

            toggleTopBar(false)
            hideIntensity(false)
        }
    }

    private fun textureEvents() {
        mSeekTexture!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                if(mActiveConfig!!.startEditing) {
                    val pr = progress.toFloat() / 100.0f
                    mActiveConfig!!.setTempIntensity(pr, true, null)
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

            mPlayerView!!.setFilterWithConfig(calculateRules())
        }
        horizontal.setOnClickListener {
            mActiveConfig!!.horizontal[1] *= -1f
            mPlayerView!!.setFilterWithConfig(calculateRules())
        }
        vert.setOnClickListener {
            mActiveConfig!!.vertical[1] *= -1f
            mPlayerView!!.setFilterWithConfig(calculateRules())
        }
        diff.setOnClickListener {
            mActiveConfig!!.diff[1] = !mActiveConfig!!.diff[1]
            mActiveConfig!!.setRule(mActiveConfig!!.diff[1])

            mPlayerView!!.setFilterWithConfig(calculateRules())
        }

        val cancel = findViewById<View>(R.id.cancel_texture_button)
        val apply = findViewById<View>(R.id.accept_texture_button)

        cancel.setOnClickListener {
            mActiveConfig!!.textureConfig(mActiveConfig!!.rotate[0], mActiveConfig!!.horizontal[0], mActiveConfig!!.vertical[0], mActiveConfig!!.diff[0])
            mActiveConfig!!.setIntensity(mActiveConfig!!.slierIntensity, false, null)

            setFilters()
            hideIntensity(true)
            toggleTopBar(false)
        }
        apply.setOnClickListener {
            mActiveConfig!!.textureConfig(mActiveConfig!!.rotate[1], mActiveConfig!!.horizontal[1], mActiveConfig!!.vertical[1], mActiveConfig!!.diff[1])
            mActiveConfig!!.setIntensity(mSeekTexture!!.progress / 100f, false, null)

            hideIntensity(true)
            toggleTopBar(false)
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
                    null
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
                    null
                )
            }
        }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

    private fun setUpViews() {
        initToolsEvents()
        setUpSeek()

        mSeekTemp!!.setOnSeekBarChangeListener(this)
        mSeekTint!!.setOnSeekBarChangeListener(this)
        mSeekHue!!.setOnSeekBarChangeListener(this)
        mSeekSat!!.setOnSeekBarChangeListener(this)
        mSeekLum!!.setOnSeekBarChangeListener(this)

        textureEvents()
/*
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
*/
        initRvEffects()
        initRvTexture()
        initRvTools();
    }

    private fun initToolsCrop() {
        mCropOverlayView!!.setPlayer(mPlayerView)

        val percentTextView = findViewById<TextView>(R.id.text_straightening)
        percentTextView.text = getString(R.string.percent, 0.toString())

        var progressF = mPlayerView!!.cropInfo.currentPercentF
        var progress = mPlayerView!!.cropInfo.currentPercent

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
//h = 439.4949f
//w = 1400.551f
                var width = gpuWrapper!!.width.toFloat();//bm!!.width
                var height = gpuWrapper!!.height.toFloat();//bm!!.height

                if (width > height) {
                    width = gpuWrapper!!.height.toFloat()
                    height = gpuWrapper!!.width.toFloat()
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

                mCropOverlayView!!.mPostRotate = angl
                mCropOverlayView!!.mPostScale = scale

                mCropOverlayView!!.applyCustom()
            }

            override fun onScrollEnd() {

            }
        })

        val rotate = findViewById<View>(R.id.rotate);
        rotate.setOnClickListener {
            mPlayerView!!.rotateView(90f)
        }

        val flipHor = findViewById<View>(R.id.horizontal)
        flipHor.setOnClickListener{
            mCropOverlayView!!.mFlipHorizontally = !mCropOverlayView!!.mFlipHorizontally
            mCropOverlayView!!.applyCustom()
        }

        val flipVert = findViewById<View>(R.id.vertical)
        flipVert.setOnClickListener{
            mCropOverlayView!!.mFlipVertically = !mCropOverlayView!!.mFlipVertically
            mCropOverlayView!!.applyCustom()
        }

        val rect = findViewById<View>(R.id.rect)
        rect.setOnClickListener{
            val parentLp = gpuWrapper!!.getLayoutParams()

            if(mCropOverlayView!!.instaMode) {
                mCropOverlayView!!.setFixedAspectRatio(false)
                mCropOverlayView!!.cropWindowRect = RectF(0f, 0f,
                    parentLp.width.toFloat(), parentLp.height.toFloat())

                mCropOverlayView!!.instaMode = false
            } else {
                var width = parentLp.width
                var height = parentLp.height

                if (width > height) {
                    width = height;
                } else {
                    height = width;
                }

                val lOffset = (parentLp.width - width) / 2
                val tOffset = (parentLp.height - height) / 2

                mCropOverlayView!!.setFixedAspectRatio(true)
                mCropOverlayView!!.cropWindowRect = RectF(lOffset.toFloat(), tOffset.toFloat(),
                    width + lOffset.toFloat(), height + tOffset.toFloat())

                mCropOverlayView!!.instaMode = true
            }

            mCropOverlayView!!.invalidate()
        }

        val apply = findViewById<View>(R.id.apply_crop)
        apply.setOnClickListener {
            val overlay = mCropOverlayView!!.getCropWindowRect()

            mCropOverlayView!!.visibility = GONE

            val scaleX = mPlayerView!!.width / overlay.width()
            val scaleY = mPlayerView!!.height / overlay.height()

            val y = gpuWrapper!!.height - overlay.bottom

            val cropInfo = floatArrayOf(overlay.width() * gpuWrapper!!.scaleX, overlay.height() * gpuWrapper!!.scaleX, y, overlay.left, scaleX, scaleY,
                gpuWrapper!!.rotation, mCropOverlayView!!.mPostRotate, mCropOverlayView!!.mPostScale)

            mPlayerView!!.cropInfo.flipHor = mCropOverlayView!!.mFlipHorizontally
            mPlayerView!!.cropInfo.flipVert = mCropOverlayView!!.mFlipVertically

            straightening.setValue(progress, progressF)
            mPlayerView!!.setCroppedInfo(cropInfo)

            mPlayerView!!.cropInfo.originalScaleX = gpuWrapper!!.width / overlay.width()
            mPlayerView!!.cropInfo.originalScaleY = gpuWrapper!!.height / overlay.height()
            mPlayerView!!.cropInfo.originalH = gpuWrapper!!.height
            mPlayerView!!.cropInfo.originalW = gpuWrapper!!.width

            val points = mCropOverlayView!!.getPoints(mPlayerView!!.matrix)

            mPlayerView!!.cropInfo.instaMode = mCropOverlayView!!.instaMode
            mPlayerView!!.cropInfo.currentPercent = straightening.currentPercent
            mPlayerView!!.cropInfo.currentPercentF = straightening.currentPercentF
            mPlayerView!!.cropInfo.overlay = overlay
            mPlayerView!!.cropInfo.points = points

            initCropBar(false)
            toggleTopBar(false)

            mPlayerView!!.setCrop(true)
            mPlayerView!!.calcDimension(mPlayerView!!.mWrapperWidth.toInt(), mPlayerView!!.mWrapperHeight.toInt(), overlay.width().toInt(), overlay.height().toInt(), true)
        }

        val cancel = findViewById<View>(R.id.cancel_crop)
        cancel.setOnClickListener {
            mCropOverlayView!!.visibility = GONE
            initCropBar(false)

            mPlayerView!!.cropInfo.percent = straightening.currentPercentF

            percentTextView.text = getString(R.string.percent, straightening.currentPercent.toString())
            straightening.invalidateValue()
            straightening.postInvalidate()

            mPlayerView!!.setCrop(true)
            mPlayerView!!.calcDimension(mPlayerView!!.mWrapperWidth.toInt(), mPlayerView!!.mWrapperHeight.toInt(), mPlayerView!!.cropInfo.width,
                                        mPlayerView!!.cropInfo.height, true)

            mCropOverlayView!!.cancelCrop()

            toggleTopBar(false)
        }
    }

    private fun changeOverlay(wRatio: Int, hRatio: Int) {
        var width = mPlayerView!!.width
        var height = mPlayerView!!.height
        val resizedWidth = (height / hRatio * wRatio)
        val resizedHeight = (width / wRatio) * hRatio

        if (resizedWidth > width) {
            height = resizedHeight;
        } else if (resizedHeight > height) {
            width = resizedWidth;
        }

        val lOffset = (mPlayerView!!.width - width) / 2
        val tOffset = (mPlayerView!!.height - height) / 2

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

    private fun getWidth(): Int{
        val display = getWindowManager().getDefaultDisplay();
        val size = Point();
        display.getSize(size);

        return size.x
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

    private fun initSavebutton() {
        findViewById<TextView>(R.id.topSave).setOnClickListener { v ->
            val gsonStr = serializedConfigs.encryptConfigs(mAdjustConfigs)
            val gsonCrop = Gson().toJson(mPlayerView!!.cropInfo)

            db!!.configDao().updateConfig(imgId, gsonStr, gsonCrop)

            mPlayerView!!.releasePlayer()
            finish()

            /*val outputFilename1 = FileUtil.getPath() + "/blendVideo777.mp4";
            editVideo(outputFilename1)



            threadSync()

            mThread = Thread(Runnable {
                //String outputFilename = "/sdcard/libCGE/blendVideo.mp4";
                //String inputFileName = "android.resource://" + getPackageName() + "/" + R.raw.fish;
                val outputFilename = FileUtil.getPath() + "/blendVideo44.mp4"
                //String inputFileName = FileUtil.getTextContent(CameraDemoActivity.lastVideoPathFileName);
                //String inputFileName = "/storage/9016-4EF8/DCIM/Camera/20200402_124813.mp4";
                //String inputFileName = "/storage/9016-4EF8/DCIM/Camera/20200402_124813_001.mp4"; // 2 sec
                val inputFileName =
                    "/storage/emulated/0/Pictures/Telegram/VID_20200626_125043_721.mp4" // 2 sec

                //bmp is used for watermark, (just pass null if you don't want that)
                //and ususally the blend mode is CGE_BLEND_ADDREV for watermarks.
                CGEFFmpegNativeLibrary.generateVideoWithFilter(
                    outputFilename,
                    inputFileName,
                    "@adjust lut ping.png",
                    1.0f,
                    null,
                    CGENativeLibrary.TextureBlendMode.CGE_BLEND_ADDREV,
                    1.0f,
                    false
                )
                Log.d("Stas", "Done! The file is generated at: \$outputFilename")

                editVideo(outputFilename)
            })

            mThread!!.start()*/
        }
    }

    private fun editVideo(name: String) {
        val cmdArray = ArrayList<String>();
        val empty = arrayOf("","","")

        val outputFilename = FileUtil.getPath() + "/blendVideo4.mp4";
        //val outputFilename = "/storage/emulated/0/Pictures/Telegram/VID_20200626_125043_721.mp4";
        val outputFilename1 = FileUtil.getPath() + "/blendVideo7797.mp4";

        val file = File(outputFilename)
        file.delete()

        cmdArray.add("-i");
        cmdArray.add(outputFilename);
        cmdArray.add("-c");
        cmdArray.add("copy");
        cmdArray.add("-metadata:s:v:0");
        cmdArray.add("rotate=270");
        cmdArray.add(outputFilename1);

        val ffmpeg = FFmpeg.getInstance(this)

        try {
            ffmpeg.execute(arrayOf("-i", name, "-filter:v", "rotate=35*PI/180", "-c:a", "copy", outputFilename1), object: ExecuteBinaryResponseHandler() {
                override fun onStart() {
                    super.onStart()
                    Log.d("Stas", "onStart")
                }

                override fun onProgress(message: String?) {
                    super.onProgress(message)
                    Log.d("Stas", message)
                }

                override fun onFailure(message: String?) {
                    super.onFailure(message)
                    Log.d("Stas", "onFailure")
                }

                override fun onSuccess(message: String?) {
                    super.onSuccess(message)
                    //File(name).delete()

                    sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.parse("file://$outputFilename1")
                        )
                    )
                }

                override fun onFinish() {
                    super.onFinish()
                    Log.d("Stas", "onFinish")
                }
            })
        } catch (e: FFmpegCommandAlreadyRunningException) {
            Log.d("Stas", "onFinish")
        }
    }

    private fun initClosebutton() {
        findViewById<View>(R.id.topClose).setOnClickListener { v ->

        }
    }

    private fun getRelativeLeft(myView: GPUPlayerView): Int {
        if (myView.getParent() == myView.getRootView())
            return myView.getLeft()
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

    fun calculeRectOnScreen(view: View ): RectF {
        val location = IntArray(2)
        view.getLocationInWindow(location);

        return RectF(location[0].toFloat(), location[1].toFloat(), location[0].toFloat() + view.getMeasuredWidth(), location[1].toFloat() + view.getMeasuredHeight());
    }

    fun Int.toDp(context: Context):Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,this.toFloat(),context.resources.displayMetrics
    ).toInt()

    fun cropVideo() {
        val parentLp = gpuWrapper!!.getLayoutParams()

        parentLp.height = mPlayerView!!.mRenderViewport.height
        parentLp.width = mPlayerView!!.mRenderViewport.width

        calculatedW = parentLp.width.toFloat()
        calculatedH = parentLp.height.toFloat()

        gpuWrapper!!.setLayoutParams(parentLp)
    }

    fun calculateDimens() {
        val group = findViewById<ConstraintLayout>(R.id.groupLayout)
        val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)

        val oneRect = calculeRectOnScreen(group);
        val secondRect = calculeRectOnScreen(tools);

        val distance = Math.abs(oneRect.top - secondRect.top) - 12 * getScale()

        canvasHeight = distance

        val lp = group.layoutParams
        lp.height = distance.toInt()
        group.layoutParams = lp

        mPlayerView!!.aetWrapper(oneRect.right - oneRect.left, distance)

        val size = mPlayerView!!.calcDimension(mPlayerView!!.mWrapperWidth.toInt(), mPlayerView!!.mWrapperHeight.toInt(),
                                    mPlayerView!!.mVideoWidth, mPlayerView!!.mVideoHeight, true)

        mPlayerView!!.cropInfo.width = size[0]
        mPlayerView!!.cropInfo.height = size[1]
    }

    override fun onResume() {
        super.onResume()

        val callback: VideoPlayerGLSurfaceView.PlayPreparedCallback =
        object: VideoPlayerGLSurfaceView.PlayPreparedCallback {
            override fun playPrepared(player: MediaPlayer) {
                calculateDimens()
                initToolsCrop()
                initCropConfig()

                player.start();
                mPlayerView!!.setFilterWithConfig(calculateRules())
            }
        }

        mPlayerView!!.setVideoUri(uri, callback, playCompletionCallback)
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

    override fun onPause() {
        super.onPause()
        mPlayerView!!.releasePlayer()
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
        mPlayerView!!.setCrop(false)

        calculatedW = mPlayerView!!.mWrapperWidth
        calculatedH = mPlayerView!!.mWrapperHeight

        mPlayerView!!.calcDimension(mPlayerView!!.mWrapperWidth.toInt(), mPlayerView!!.mWrapperHeight.toInt(),
            mPlayerView!!.mVideoWidth, mPlayerView!!.mVideoHeight, true)

        initCropBar(true)
        initOverlay()
    }

    private fun initCropBar(show: Boolean) {
        val tools = findViewById<ConstraintLayout>(R.id.toolsLayout)
        val crop = findViewById<ConstraintLayout>(R.id.crop_area)

        if(show) {
            tools.visibility = GONE
            crop.visibility = VISIBLE
        } else {
            tools.visibility = VISIBLE
            crop.visibility = GONE
        }
    }

    fun initOverlay() {
        val parentLp = gpuWrapper!!.getLayoutParams()
        val overlay = mCropOverlayView!!.getCropWindowRect()

        if(overlay.isEmpty) {
            val options = CropImageOptions()
            options.fixAspectRatio = true

            mCropOverlayView!!.setInitialAttributeValues(options)

            mImagePoints[0] = 0f
            mImagePoints[1] = 0f
            mImagePoints[2] = parentLp.width.toFloat()
            mImagePoints[3] = 0f
            mImagePoints[4] = parentLp.width.toFloat()
            mImagePoints[5] = parentLp.height.toFloat()
            mImagePoints[6] = 0f
            mImagePoints[7] = parentLp.height.toFloat()

            mCropOverlayView!!.setCropWindowLimits(
                parentLp.width.toFloat(), parentLp.height.toFloat(), 1.0f, 1.0f
            )

            mCropOverlayView!!.setFixedAspectRatio(false)

            mCropOverlayView!!.setBounds(
                mImagePoints,
                parentLp.width,
                parentLp.height
            )

            if(crop != null && !crop!!.isEmpty() && mPlayerView!!.cropInfo.overlay != null) {
                mCropOverlayView!!.cropWindowRect = mPlayerView!!.cropInfo.overlay
            }
        }
        //mCropOverlayView!!.setInitialCropWindowRect(null)
        mCropOverlayView!!.visibility = VISIBLE
    }

    private fun getScale(): Float {
        return this.getResources().getDisplayMetrics().density
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

    private fun setFilters() {
        mPlayerView!!.setFilterWithConfig(calculateRules())
        mPlayerView!!.setFilterIntensity(intensity)
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

    private fun scaleArea(min: Boolean) {
        val parent = findViewById<ConstraintLayout>(R.id.groupLayout)
        val params = parent.layoutParams

        if(min) {
            params.height = (canvasHeight / 1.5).toInt()
        } else {
            params.height = (canvasHeight).toInt()
        }

        parent.layoutParams = params
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
}
