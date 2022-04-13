package com.deblead.arsample.ui

import android.R.attr.data
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.xplora.arsample.R
import com.xplora.arsample.databinding.ActivityChromeVideoBinding


// The color to filter out of the video.
private val CHROMA_KEY_COLOR = Color(0.1843f, 1.0f, 0.098f)

// Controls the height of the video in world space.
private const val VIDEO_HEIGHT_METERS = 0.85f
private const val MIN_OPENGL_VERSION = 3.0

class ChromeVideoActivity : AppCompatActivity() {
    private val TAG: String =
        ChromeVideoActivity::class.java.getSimpleName()
    private lateinit var externalTextureView: ExternalTexture
    private var videoRenderable: ModelRenderable? = null
    private lateinit var url: Uri
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var arFragment: ArFragment
    private lateinit var binding: ActivityChromeVideoBinding


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChromeVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        arFragment = supportFragmentManager.findFragmentById(R.id.fragment) as ArFragment
        pickVideo()

    }

    @RequiresApi(VERSION_CODES.N)
    private fun pickVideo() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        videoActivityResultLauncher.launch(intent)
    }

    @RequiresApi(VERSION_CODES.N)
    private val videoActivityResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK && it.data != null && it.data!!.data != null) {
            val selectedImageUri: Uri? = it.data!!.data
            if (selectedImageUri != null) {
                url = selectedImageUri
                initAllObj()
                initDoubleTap()
            } else {
                val toast =
                    Toast.makeText(this, "Unable to load video renderable", Toast.LENGTH_LONG)
            }
        }
    }

    private fun initDoubleTap() {
        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            if (videoRenderable == null) {
                return@setOnTapArPlaneListener
            }
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor).apply {
                setParent(arFragment.arSceneView.scene)
            }

            val videoNode = TransformableNode(arFragment.transformationSystem)
                .apply {
                    setParent(anchorNode)
                }
            val videoWidth = mediaPlayer?.videoWidth
            val videoHeight = mediaPlayer?.videoHeight
            if (videoWidth != null && videoHeight != null) {
                videoNode.localScale = Vector3(
                    VIDEO_HEIGHT_METERS * (videoWidth / videoHeight),
                    VIDEO_HEIGHT_METERS, 1.0f
                )
            }
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer!!.start()

                // Wait to set the renderable until the first frame of the  video becomes available.
                // This prevents the renderable from briefly appearing as a black quad before the video
                // plays.
                externalTextureView
                    .surfaceTexture
                    .setOnFrameAvailableListener {
                        videoNode.renderable = videoRenderable
                        externalTextureView.surfaceTexture.setOnFrameAvailableListener(null)
                    }
            } else {
                videoNode.renderable = videoRenderable
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initAllObj() {
        externalTextureView = ExternalTexture()
        mediaPlayer = MediaPlayer.create(this, url)
            .apply {
                setSurface(externalTextureView.surface)
                isLooping = true
            }
        ModelRenderable.builder()
            .setSource(this, R.raw.chroma_key_video)
            .build()
            .thenAccept {
                videoRenderable = it
                it.material.setExternalTexture("videoTexture", externalTextureView)
                it.material.setFloat4("keyColor", CHROMA_KEY_COLOR)
            }
            .exceptionally {
                val toast =
                    Toast.makeText(this, "Unable to load video renderable", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     *
     * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     *
     * Finishes the activity if Sceneform can not run
     */
    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(
                TAG,
                "Sceneform requires Android N or later"
            )
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        val openGlVersionString = (activity.getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(
                TAG,
                "Sceneform requires OpenGL ES 3.0 later"
            )
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }
}