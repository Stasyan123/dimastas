package com.sm.stasversion;

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.media.MediaMetadataRetriever
import android.widget.*
import com.daasuu.gpuv.egl.filter.*
import com.daasuu.gpuv.player.GPUPlayerView
import com.daasuu.gpuv.player.PlayerScaleType
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.sm.stasversion.widget.FilterAdjuster
import com.sm.stasversion.widget.PlayerTimer

import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter


class VideoOverviewActivity : AppCompatActivity() {

    private val STREAM_URL_MP4_VOD_LONG = "https://www.radiantmediaplayer.com/media/bbb-360p.mp4"
    private var gpuPlayerView: GPUPlayerView? = null;
    private var gpuPlayerViewTest: GPUPlayerView? = null;
    private var player: SimpleExoPlayer? = null;
    private var button: Button? = null;
    private var timeSeekBar: SeekBar? = null;
    private var filterSeekBar: SeekBar? = null
    private var playerTimer: PlayerTimer? = null;
    private var filter: GlFilter? = null;
    private var adjuster: FilterAdjuster? = null;
    private var uri: Uri? = null;

    private var glFilter: GlFilter = GlFilterGroup(GlMonochromeFilter(), GlVignetteFilter())

    var intensity: Float = 1.0f
    var position: Int = -1

    var isLookup: Boolean = false
    var gpuFilter: GlFilter = GlFilter()
    var gpuLookupFilter: GlLookUpTableFilter? = null

    var sepia: GPUImageSepiaToneFilter = GPUImageSepiaToneFilter()

    var filterVideo1: GlLookUpTableFilter? = null
    var filterVideo3: GlLookUpTableFilter? = null
    var filterVideo2: GlLookUpTableFilter? = null
    var sepiaVideo: GlSepiaFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_overview)

        uri = intent.getParcelableExtra<Uri>("file");

        setUpPlayer()
        //setUpViews()
    }

    override fun onResume() {
        super.onResume()
        setUpSimpleExoPlayer()
        setUoGlPlayerView()
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

    private fun setUpPlayer() {
        gpuPlayerView = findViewById(R.id.gpuimageview)
        //val m = gpuPlayerView!!.matrix
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
        if (playerTimer != null) {
            playerTimer!!.stop()
            playerTimer!!.removeMessages(0)
        }
    }

    private fun releasePlayer() {
        gpuPlayerView!!.onPause();
        //(findViewById<MovieWrapperView>(R.id.layout_movie_wrapper)).removeAllViews();
        player!!.stop();
        player!!.release();
        player = null;
    }
}
