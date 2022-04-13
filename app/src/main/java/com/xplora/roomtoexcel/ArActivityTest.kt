package com.xplora.roomtoexcel

import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.filament.Box
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.xplora.roomtoexcel.data.ArModel
import com.xplora.roomtoexcel.data.ArModelAdapter
import com.xplora.roomtoexcel.databinding.ActivityArTestBinding
import java.util.*
import java.util.concurrent.CompletableFuture

private const val BOTTOM_SHEET_PEEK_HEIGHT = 50f
private const val DOUBLE_TAP_DURATION = 1000L

class ArActivityTest : AppCompatActivity() {

    lateinit var arFragment: ArFragment

    var viewNodes = mutableListOf<Node>()

    private lateinit var bindingView: ActivityArTestBinding
    private val models = mutableListOf(
        ArModel(R.drawable.ico_astronut, "Astronaut", R.raw.astronaut),
        ArModel(R.drawable.ico_box, "Box", R.raw.boom_box),
        ArModel(R.drawable.ico_corset, "Corset", R.raw.corset),
        ArModel(R.drawable.ico_helmet, "Helmet", R.raw.helmet),
        ArModel(R.drawable.ico_horse, "Horse", R.raw.horse),
        ArModel(R.drawable.ico_lalten, "Lalten", R.raw.lalten),
        ArModel(R.drawable.ic_tshirt, "T-Shirt", R.raw.tshirt_glb),
        ArModel(R.drawable.ic_devil, "Statue", R.raw.george_washington)
    )

    private lateinit var selectedModel: ArModel

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingView = ActivityArTestBinding.inflate(layoutInflater)
        setContentView(bindingView.root)
        arFragment = supportFragmentManager.findFragmentById(R.id.fragment) as ArFragment
        setBottomSheet()
        setupRecyclerView()
        setDoubleTapArPlanListener()
        getCurrentScene().addOnUpdateListener {
            rotateViewNodesTowordsUser()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setDoubleTapArPlanListener() {
        var firstTapTime = 0L
        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            Log.d("setDoubleTapArPlanListener 2", "firstTapTime -> ${firstTapTime}")
            when {
                firstTapTime == 0L -> {
                    firstTapTime = System.currentTimeMillis()
                }
                System.currentTimeMillis() - firstTapTime < DOUBLE_TAP_DURATION -> {
                    /*Log.d(
                        "setDoubleTapArPlanListener 3",
                        "firstTapTime -> ${System.currentTimeMillis() - firstTapTime}"
                    )*/
                    loadModel { modelRenderable, viewRenderable ->
                        addNodeToScene(
                            hitResult.createAnchor(),
                            modelRenderable,
                            viewRenderable
                        )
                    }
                }
                else -> {
//                    Log.d("setDoubleTapArPlanListener 4", "firstTapTime -> ${firstTapTime}")
                    firstTapTime = System.currentTimeMillis()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        bindingView.rvModels.run {
            layoutManager =
                LinearLayoutManager(this@ArActivityTest, LinearLayoutManager.HORIZONTAL, false)
            adapter = ArModelAdapter(models).apply {
                selectedModel.observe(this@ArActivityTest, Observer {
                    this@ArActivityTest.selectedModel = it
                    val newTitle = "Models (${it.title})"
                    bindingView.tvModel.text = newTitle
                })
            }
        }
    }

    private fun setBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(bindingView.bottomSheet)
        bottomSheetBehavior.peekHeight =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                BOTTOM_SHEET_PEEK_HEIGHT,
                resources.displayMetrics
            ).toInt()

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                bottomSheet.bringToFront()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadModel(callBack: (ModelRenderable, ViewRenderable) -> Unit) {
        val modelRenderable = ModelRenderable.builder()
//            .setSource(this, Uri.parse("https://admin.bam.goxplora.com/uploads/builds/Xplora/Island_Madeira.glb"))
//            .setSource(this, Uri.parse("https://admin.portsunlight.goxplora.com/uploads/modules/384/horse.glb"))
//            .setSource(this, Uri.parse("https://admin.portsunlight.goxplora.com/uploads/modules/384/camera.gltf"))
            .setSource(
                this,
                Uri.parse("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/astronaut.gltf")
            )
            .build()
        val viewRenderable = ViewRenderable.builder()
            .setView(this, createdDeleteButton())
            .build()

        CompletableFuture.allOf(modelRenderable, viewRenderable)
            .thenAccept {
                callBack(modelRenderable.get(), viewRenderable.get())
            }
            .exceptionally {
                Toast.makeText(this, "Error Loading Model $it", Toast.LENGTH_SHORT).show()
                null
            }
    }

    private fun createdDeleteButton(): Button {
        return Button(this).apply {
            text = "Delete"
            setBackgroundColor(android.graphics.Color.RED)
            setTextColor(android.graphics.Color.WHITE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun addNodeToScene(
        anchor: Anchor,
        modelRenderable: ModelRenderable,
        viewRenderable: ViewRenderable
    ) {

        val anchorNode = AnchorNode(anchor)
        val modelNode = TransformableNode(arFragment.transformationSystem)
            .apply {
                renderable = modelRenderable
                setParent(anchorNode)
                getCurrentScene().addChild(anchorNode)
                select()
            }

        val viewNode = Node().apply {
            renderable = null
            setParent(modelNode)
            val box = modelNode.renderable?.collisionShape as com.google.ar.sceneform.collision.Box
            localPosition = Vector3(0f, box.size.y, 0f)
            (viewRenderable.view as Button).setOnClickListener {
                getCurrentScene().removeChild(anchorNode)
                viewNodes.remove(this)
            }
        }
        viewNodes.add(viewNode)
        modelNode.setOnTapListener { hitTestResult, motionEvent ->
            if (!modelNode.isTransforming) {
                if (viewNode.renderable == null) {
                    viewNode.renderable = viewRenderable
                } else {
                    viewNode.renderable = null
                }
            }
        }

    }

    private fun rotateViewNodesTowordsUser() {
        for (node in viewNodes) {
            node.renderable?.let {
                val camPos = getCurrentScene().camera.worldPosition
                val viewNodePos = node.worldPosition
                val dir = Vector3.subtract(camPos, viewNodePos)
                node.worldRotation = Quaternion.lookRotation(dir, Vector3.up())
            }
        }
    }

    private fun getCurrentScene() = arFragment.arSceneView.scene
}