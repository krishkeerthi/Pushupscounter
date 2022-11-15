package com.example.pushupscounter

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.pushupscounter.databinding.ActivityMainBinding
import com.example.pushupscounter.java.CameraXViewModel
import com.example.pushupscounter.posedetector.PoseDetectorProcessor
import com.example.pushupscounter.preferences.PreferenceUtils
import com.example.pushupscounter.utils.descending
import com.example.pushupscounter.utils.targetCount
import com.google.mlkit.common.MlKitException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



class MainActivity :
    AppCompatActivity(), AdapterView.OnItemSelectedListener,
    CompoundButton.OnCheckedChangeListener {

    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var videoCaptureUseCase: VideoCapture<Recorder>? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var selectedModel = POSE_DETECTION
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var cameraSelector: CameraSelector? = null

    // Camerax
    private lateinit var cameraExecutor : ExecutorService
    private var recording: Recording? = null


    // binding
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }

        //this.supportActionBar?.hide()

        targetCount = intent.getIntExtra("TARGET_COUNT", 0)
        descending = intent.getBooleanExtra("DESCENDING", false)
        Log.d(TAG, "onCreate: target count: $targetCount, descending : $descending")
        
        binding = ActivityMainBinding.inflate(layoutInflater)

        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, POSE_DETECTION)
        }

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        setContentView(binding.root)

        // Start camera
        cameraExecutor = Executors.newSingleThreadExecutor()
        //startCamera()

        // record button
        binding.videoCapture.setOnClickListener {
            //captureVideo()
        }

        previewView = findViewById(R.id.preview_view)
        if (previewView == null) {
            Log.d(TAG, "previewView is null")
        }
        graphicOverlay = findViewById(R.id.graphic_overlay)
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }
//        val spinner = findViewById<Spinner>(R.id.spinner)
//        val options: MutableList<String> = ArrayList()
//        options.add(OBJECT_DETECTION)
//        options.add(OBJECT_DETECTION_CUSTOM)
//        options.add(CUSTOM_AUTOML_OBJECT_DETECTION)
//        options.add(FACE_DETECTION)
//        options.add(BARCODE_SCANNING)
//        options.add(IMAGE_LABELING)
//        options.add(IMAGE_LABELING_CUSTOM)
//        options.add(CUSTOM_AUTOML_LABELING)
//        options.add(POSE_DETECTION)
//        options.add(SELFIE_SEGMENTATION)
//        options.add(TEXT_RECOGNITION_LATIN)
//        options.add(TEXT_RECOGNITION_CHINESE)
//        options.add(TEXT_RECOGNITION_DEVANAGARI)
//        options.add(TEXT_RECOGNITION_JAPANESE)
//        options.add(TEXT_RECOGNITION_KOREAN)
//        options.add(FACE_MESH_DETECTION)
//
//        // Creating adapter for spinner
//        val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, options)
//        // Drop down layout style - list view with radio button
//        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        // attaching data adapter to spinner
//        spinner.adapter = dataAdapter
//        spinner.onItemSelectedListener = this

//        val facingSwitch = findViewById<ToggleButton>(R.id.facing_switch)
//        facingSwitch.setOnCheckedChangeListener(this)

        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(CameraXViewModel::class.java)
            .processCameraProvider
            .observe(
                this
            ) { provider: ProcessCameraProvider? ->
                cameraProvider = provider
                bindAllCameraUseCases()
            }

//        val settingsButton = findViewById<ImageView>(R.id.settings_button)
//        settingsButton.setOnClickListener {
//            val intent = Intent(applicationContext, SettingsActivity::class.java)
//            intent.putExtra(SettingsActivity.EXTRA_LAUNCH_SOURCE, LaunchSource.CAMERAX_LIVE_PREVIEW)
//            startActivity(intent)
//        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(STATE_SELECTED_MODEL, selectedModel)
    }

    @Synchronized
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
//        selectedModel = parent?.getItemAtPosition(pos).toString()
//        Log.d(TAG, "Selected model: $selectedModel")
//        bindAnalysisUseCase()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing.
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
//        if (cameraProvider == null) {
//            return
//        }
//        val newLensFacing =
//            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
//                CameraSelector.LENS_FACING_BACK
//            } else {
//                CameraSelector.LENS_FACING_FRONT
//            }
//        val newCameraSelector = CameraSelector.Builder().requireLensFacing(newLensFacing).build()
//        try {
//            if (cameraProvider!!.hasCamera(newCameraSelector)) {
//                Log.d(TAG, "Set facing to " + newLensFacing)
//                lensFacing = newLensFacing
//                cameraSelector = newCameraSelector
//                bindAllCameraUseCases()
//                return
//            }
//        } catch (e: CameraInfoUnavailableException) {
//            // Falls through
//        }
//        Toast.makeText(
//            applicationContext,
//            "This device does not have lens with facing: $newLensFacing",
//            Toast.LENGTH_SHORT
//        )
//            .show()
    }

    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onPause() {
        super.onPause()

        imageProcessor?.run { this.stop() }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }

    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()

            bindPreviewUseCase()
            bindAnalysisUseCase()
            // video capture analysis
            //bindVideoCaptureUseCase()
        }
    }

    private fun bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return
        }
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        val builder = Preview.Builder()
        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(previewView!!.getSurfaceProvider())
        cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */ this,
            cameraSelector!!,
            previewUseCase
        )
    }

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor!!.stop()
        }
        imageProcessor =
            try {
                when (selectedModel) {
//                    OBJECT_DETECTION -> {
//                        Log.i(TAG, "Using Object Detector Processor")
//                        val objectDetectorOptions = PreferenceUtils.getObjectDetectorOptionsForLivePreview(this)
//                        ObjectDetectorProcessor(this, objectDetectorOptions)
//                    }
//                    OBJECT_DETECTION_CUSTOM -> {
//                        Log.i(TAG, "Using Custom Object Detector (with object labeler) Processor")
//                        val localModel =
//                            LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build()
//                        val customObjectDetectorOptions =
//                            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel)
//                        ObjectDetectorProcessor(this, customObjectDetectorOptions)
//                    }
//                    CUSTOM_AUTOML_OBJECT_DETECTION -> {
//                        Log.i(TAG, "Using Custom AutoML Object Detector Processor")
//                        val customAutoMLODTLocalModel =
//                            LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build()
//                        val customAutoMLODTOptions =
//                            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
//                                this,
//                                customAutoMLODTLocalModel
//                            )
//                        ObjectDetectorProcessor(this, customAutoMLODTOptions)
//                    }
//                    TEXT_RECOGNITION_LATIN -> {
//                        Log.i(TAG, "Using on-device Text recognition Processor for Latin")
//                        TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build())
//                    }
//                    TEXT_RECOGNITION_CHINESE -> {
//                        Log.i(TAG, "Using on-device Text recognition Processor for Latin and Chinese")
//                        TextRecognitionProcessor(this, ChineseTextRecognizerOptions.Builder().build())
//                    }
//                    TEXT_RECOGNITION_DEVANAGARI -> {
//                        Log.i(TAG, "Using on-device Text recognition Processor for Latin and Devanagari")
//                        TextRecognitionProcessor(this, DevanagariTextRecognizerOptions.Builder().build())
//                    }
//                    TEXT_RECOGNITION_JAPANESE -> {
//                        Log.i(TAG, "Using on-device Text recognition Processor for Latin and Japanese")
//                        TextRecognitionProcessor(this, JapaneseTextRecognizerOptions.Builder().build())
//                    }
//                    TEXT_RECOGNITION_KOREAN -> {
//                        Log.i(TAG, "Using on-device Text recognition Processor for Latin and Korean")
//                        TextRecognitionProcessor(this, KoreanTextRecognizerOptions.Builder().build())
//                    }
//                    FACE_DETECTION -> {
//                        Log.i(TAG, "Using Face Detector Processor")
//                        val faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(this)
//                        FaceDetectorProcessor(this, faceDetectorOptions)
//                    }
//                    BARCODE_SCANNING -> {
//                        Log.i(TAG, "Using Barcode Detector Processor")
//                        BarcodeScannerProcessor(this)
//                    }
//                    IMAGE_LABELING -> {
//                        Log.i(TAG, "Using Image Label Detector Processor")
//                        LabelDetectorProcessor(this, ImageLabelerOptions.DEFAULT_OPTIONS)
//                    }
//                    IMAGE_LABELING_CUSTOM -> {
//                        Log.i(TAG, "Using Custom Image Label (Birds) Detector Processor")
//                        val localClassifier =
//                            LocalModel.Builder().setAssetFilePath("custom_models/bird_classifier.tflite").build()
//                        val customImageLabelerOptions =
//                            CustomImageLabelerOptions.Builder(localClassifier).build()
//                        LabelDetectorProcessor(this, customImageLabelerOptions)
//                    }
//                    CUSTOM_AUTOML_LABELING -> {
//                        Log.i(TAG, "Using Custom AutoML Image Label Detector Processor")
//                        val customAutoMLLabelLocalModel =
//                            LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build()
//                        val customAutoMLLabelOptions =
//                            CustomImageLabelerOptions.Builder(customAutoMLLabelLocalModel)
//                                .setConfidenceThreshold(0f)
//                                .build()
//                        LabelDetectorProcessor(this, customAutoMLLabelOptions)
//                    }
                    POSE_DETECTION -> {
                        val poseDetectorOptions =
                            PreferenceUtils.getPoseDetectorOptionsForLivePreview(this)
                        val shouldShowInFrameLikelihood =
                            PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this)
                        val visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this)
                        val rescaleZ =
                            PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this)
                        val runClassification =
                            PreferenceUtils.shouldPoseDetectionRunClassification(this)
                        PoseDetectorProcessor(
                            this,
                            poseDetectorOptions,
                            shouldShowInFrameLikelihood,
                            visualizeZ,
                            rescaleZ,
                            runClassification,
                            /* isStreamMode = */ true
                        )
                    }
//                    SELFIE_SEGMENTATION -> SegmenterProcessor(this)
//                    FACE_MESH_DETECTION -> FaceMeshDetectorProcessor(this)
                    else -> throw IllegalStateException("Invalid model name")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Can not create image processor: $selectedModel", e)
                Toast.makeText(
                    applicationContext,
                    "Can not create image processor: " + e.localizedMessage,
                    Toast.LENGTH_LONG
                )
                    .show()
                return
            }

        val builder = ImageAnalysis.Builder()
        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this),
            ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                if (needUpdateGraphicOverlayImageSourceInfo) {
                    val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.width,
                            imageProxy.height,
                            isImageFlipped
                        )
                    } else {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.height,
                            imageProxy.width,
                            isImageFlipped
                        )
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false
                }
                try {
                    imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
                } catch (e: MlKitException) {
                    Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )
        cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */ this,
            cameraSelector!!,
            analysisUseCase
        )
    }

    private fun bindVideoCaptureUseCase(){
        if (cameraProvider == null) {
            return
        }
        if (videoCaptureUseCase != null) {
            cameraProvider!!.unbind(videoCaptureUseCase)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCaptureUseCase = VideoCapture.withOutput(recorder)

        cameraProvider!!.bindToLifecycle(
            this,
            cameraSelector!!,
            videoCaptureUseCase
        )
    }
    
    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

//            // Preview
//            val preview = Preview.Builder()
//                .build()
//                .also {
//                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
//                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCaptureUseCase = VideoCapture.withOutput(recorder)

            /*
            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }
            */

            // Select back camera as a default
            //val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider
                    .bindToLifecycle(this, cameraSelector!!, videoCaptureUseCase)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun captureVideo(){
        val videoCapture = this.videoCaptureUseCase ?: return

        binding.videoCapture.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val fileFormat = "dd-MM-yyyy"
        val name = "PushupCounter" + SimpleDateFormat(fileFormat, Locale.UK)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@MainActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.videoCapture.apply {
                            text = getString(R.string.stop)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        binding.videoCapture.apply {
                            text = getString(R.string.record)
                            isEnabled = true
                        }
                    }
                }
            }

    }

    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

    companion object {
        private const val TAG = "CameraXLivePreview"

        //        private const val OBJECT_DETECTION = "Object Detection"
//        private const val OBJECT_DETECTION_CUSTOM = "Custom Object Detection"
//        private const val CUSTOM_AUTOML_OBJECT_DETECTION = "Custom AutoML Object Detection (Flower)"
//        private const val FACE_DETECTION = "Face Detection"
//        private const val TEXT_RECOGNITION_LATIN = "Text Recognition Latin"
//        private const val TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese (Beta)"
//        private const val TEXT_RECOGNITION_DEVANAGARI = "Text Recognition Devanagari (Beta)"
//        private const val TEXT_RECOGNITION_JAPANESE = "Text Recognition Japanese (Beta)"
//        private const val TEXT_RECOGNITION_KOREAN = "Text Recognition Korean (Beta)"
//        private const val BARCODE_SCANNING = "Barcode Scanning"
//        private const val IMAGE_LABELING = "Image Labeling"
//        private const val IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)"
//        private const val CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)"
        private const val POSE_DETECTION = "Pose Detection"
//        private const val SELFIE_SEGMENTATION = "Selfie Segmentation"
//        private const val FACE_MESH_DETECTION = "Face Mesh Detection (Beta)";

        private const val STATE_SELECTED_MODEL = "selected_model"

        private const val PERMISSION_REQUESTS = 1

        private val REQUIRED_RUNTIME_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )

    }
}
