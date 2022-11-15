package com.example.pushupscounter

import android.content.ContentValues.TAG
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaMetadata
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.pushupscounter.databinding.ActivityEntryBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileDescriptor
import java.text.SimpleDateFormat
import java.util.*


class EntryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEntryBinding
    private var targetCount = 0
    private var descending = false

    //private val CAST_PERMISSION_CODE = 22
    private val REQUEST_MEDIA_PROJECTION: Int = 1
    private val PERMISSION_CODE = 1
    private var mResultCode = 0
    private var mResultData: Intent? = null

    private lateinit var mDisplayMetrics: DisplayMetrics
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mMediaProjectionManager: MediaProjectionManager? = null
    var mProjectionManager: MediaProjectionManager? = null
    private lateinit var mediaProjectionCallback: MediaProjectionCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //this.supportActionBar?.hide()

        // set up screen record
        //screenRecordSetUp()

        newSetup()

        binding.counterRadioButton.isChecked = true

        binding.modeSelectionRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when(checkedId){
                R.id.counter_radio_button -> {
                    binding.targetEditText.visibility = View.GONE
                    binding.targetCountTextView.visibility = View.GONE
                    targetCount = 0
                    descending = false

                    binding.targetEditText.setText("")
                }
                R.id.target_radio_button -> {
                    binding.targetEditText.visibility = View.VISIBLE
                    binding.targetCountTextView.visibility = View.VISIBLE
                    descending = false
                }
                R.id.target_desc_radio_button -> {
                    binding.targetEditText.visibility = View.VISIBLE
                    binding.targetCountTextView.visibility = View.VISIBLE
                    descending = true
                }
            }
        }

        binding.startButton.setOnClickListener{
            if(!binding.counterRadioButton.isChecked){
                val targetcount = binding.targetEditText.text.toString().toIntOrNull()

                if(targetcount != null){
                    if(targetcount > 0){
                        //startScreenCapture()
                        shareScreen()
                    }
                    else{
                        Snackbar.make(binding.root, "Target field should be greater than 0", Snackbar.LENGTH_SHORT).show()
                    }
                }
                else{
                    Snackbar.make(binding.root, "Target field should not be empty", Snackbar.LENGTH_SHORT).show()
                }
            }
            else {
                //startScreenCapture()
                shareScreen()
            }
        }
    }

    private fun newSetup() {
        val intent = Intent(this, ScreenRecordService::class.java)
        startService(intent)

        mDisplayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(mDisplayMetrics)
        //mScreenDensity = metrics.densityDpi

        initRecorder()
        prepareRecorder()

        mProjectionManager =
            this.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionCallback = MediaProjectionCallback()
    }

    private fun gotoMainActivity(){
        Log.d(TAG, "gotoMainActivity: start recording")

        val target = binding.targetEditText.text.toString().toIntOrNull() ?: 0

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("TARGET_COUNT", target)
            putExtra("DESCENDING", descending)
        }
        startActivity(intent)
    }


   override fun onDestroy() {
        super.onDestroy()
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode)
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
            return
        }
        data?.let {
            mMediaProjection = mProjectionManager?.getMediaProjection(resultCode, data)
            mMediaProjection?.registerCallback(mediaProjectionCallback, null)
            mVirtualDisplay = createVirtualDisplay()
            mMediaRecorder?.start()
        }

    }

//    public void onToggleScreenShare(View view) {
//        if (((ToggleButton) view).isChecked()) {
//            shareScreen();
//        } else {
//            mMediaRecorder.stop();
//            mMediaRecorder.reset();
//            Log.v(TAG, "Recording Stopped");
//            stopScreenSharing();
//        }
//    }

    private fun shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager?.createScreenCaptureIntent(), PERMISSION_CODE)
            return
        }
        mVirtualDisplay = createVirtualDisplay()
        try {
            mMediaRecorder?.start()
        } catch (e: Exception) {
            Log.e(TAG, "shareScreen: mediarecorder start error ${e.stackTrace}")
        }
    }

    private fun stopScreenSharing(){
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay?.release()
        //mMediaRecorder.release();
    }

    private fun createVirtualDisplay(): VirtualDisplay?{
        val surface = binding.surface.holder.surface
        return mMediaProjection?.createVirtualDisplay("MainActivity",
            mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels, mDisplayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface, null /*Callbacks*/, null /*Handler*/);
    }

    inner class MediaProjectionCallback: MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            mMediaProjection = null
        }
    }

    private fun prepareRecorder() {
        try {
            mMediaRecorder?.prepare()
            Log.d(TAG, "prepareRecorder: prepare success")
        } catch (e: Exception) {
            Log.d(TAG, "prepareRecorder: prepare failed")
            e.printStackTrace()
//            finish()
        }
    }

    private fun getFilePath(): String {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath + File.separator + "Recordings"
//        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
//            Toast.makeText(this, "Failed to get External Storage", Toast.LENGTH_SHORT).show();
//            return null;
//        }

        var filePath = ""

        val folder = File(directory)
        if(!folder.exists())
            folder.mkdirs()


        val  success = true

        if (success) {
            val  videoName = ("capture_" + getCurSysDate() + ".mp4")
            filePath = File(directory, videoName).absolutePath
            //val createdFile = File(filePath).createNewFile()

            //Log.d(TAG, "getFilePath: file already exists $createdFile")

            Log.d(TAG, "getFilePath: filepath is $filePath")
        } else {
            Toast.makeText(this, "Failed to create Recordings directory", Toast.LENGTH_SHORT).show()
            return filePath
        }
        return filePath
    }

    private fun getCurSysDate(): String {
        return SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
    }

    private fun initRecorder() {
        if (mMediaRecorder == null) {
            mMediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mMediaRecorder?.setVideoEncodingBitRate(512 * 1000)
            mMediaRecorder?.setVideoFrameRate(60)
            mMediaRecorder?.setVideoSize(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels)
            val path = getFilePath()
            val x = FileDescriptor()

            Log.d(TAG, "initRecorder: path $path")
            //Log.d("Path", path)
            mMediaRecorder?.setOutputFile(path)



            //.d(TAG, "initRecorder: file path ${getFilePath()}")
        }
    }
}

// OLD CODE
//    override fun onRestart() {
//        super.onRestart()
//        Log.d(TAG, "onRestart: stop recording")
//        //stopRecording()
//        stopScreenCapture()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        stopScreenCapture()
//        tearDownMediaProjection()
//    }
//
//    private fun screenRecordSetUp(){
//        val intent = Intent(this, ScreenRecordService::class.java)
//        startService(intent)
//
//        mDisplayMetrics = DisplayMetrics()
//        windowManager.defaultDisplay.getMetrics(mDisplayMetrics)
//
//        mMediaRecorder =  if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
//            MediaRecorder(applicationContext)
//        else
//            MediaRecorder()
//
//        mMediaProjectionManager = this.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//
//        prepareRecording()
//    }
//
//    private fun startRecording() {
//        //prepareRecording()
//        // If mMediaProjection is null that means we didn't get a context, lets ask the user
////        if (mMediaProjection == null) {
////            // This asks for user permissions to capture the screen
////            mediaProjectionLauncher.launch(mProjectionManager?.createScreenCaptureIntent())
//////            startActivityForResult(
//////                mProjectionManager?.createScreenCaptureIntent(),
//////                CAST_PERMISSION_CODE
//////            )
////            return
////        }
////        setUpVirtualDisplay()
////        mMediaRecorder?.start()
//    }
//    private fun startScreenCapture() {
//        if (mMediaProjection != null) {
//            setUpVirtualDisplay()
//            mMediaRecorder?.start()
//            gotoMainActivity()
//        }
//        else if (mResultCode != 0 && mResultData != null) {
//            setUpMediaProjection()
//            setUpVirtualDisplay()
//            mMediaRecorder?.start()
//            gotoMainActivity()
//        } else {
//            // This initiates a prompt dialog for the user to confirm screen projection.
//            startActivityForResult(
//                mMediaProjectionManager?.createScreenCaptureIntent(),
//                REQUEST_MEDIA_PROJECTION
//            )
//        }
//    }
//
//    private fun stopRecording() {
////        if (mMediaRecorder != null) {
////            mMediaRecorder?.stop()
////            mMediaRecorder?.reset()
////        }
////        if (mVirtualDisplay != null) {
////            mVirtualDisplay?.release()
////        }
////        mMediaProjection?.stop()
////        prepareRecording()
//    }
//
//
//
//    private fun stopScreenCapture() {
//        if (mMediaRecorder != null) {
//            mMediaRecorder?.stop()
//            mMediaRecorder?.reset()
//        }
//        if (mVirtualDisplay != null) {
//            mVirtualDisplay?.release()
//        }
//        mMediaProjection?.stop()
//        prepareRecording()
//
//        // stop screen capture without recording
////        if (mVirtualDisplay == null) {
////            return
////        }
////        mVirtualDisplay!!.release()
////        mVirtualDisplay = null
//    }
//
//    private fun setUpMediaProjection() {
//        mResultData?.let {
//            mMediaProjection = mMediaProjectionManager?.getMediaProjection(mResultCode, it)
//        }
//    }
//
//    private fun tearDownMediaProjection() {
//        if (mMediaProjection != null) {
//            mMediaProjection!!.stop()
//            mMediaProjection = null
//        }
//    }
//
//    private fun getCurSysDate(): String {
//        return SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())
//    }
//
//    private fun prepareRecording() {
////        try {
////            mMediaRecorder?.prepare()
////        } catch (e: Exception) {
////            e.printStackTrace()
////            return
////        }
//
//        val directory =
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) //+ File.separator.toString() + "Recordings"
////        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
////            Toast.makeText(this, "Failed to get External Storage", Toast.LENGTH_SHORT).show()
////            return
////        }
//
//        if(!directory.exists())
//            directory.mkdirs()
//
////        val folder = File(directory)
////        var success = true
////        if (!folder.exists()) {
////            success = folder.mkdir()
////        }
//
////        val filePath: String = if (success) {
////            val videoName = "capture_" + getCurSysDate() + ".mp4"
////            directory + File.separator.toString() + videoName
////        } else {
////            Toast.makeText(this, "Failed to create Recordings directory", Toast.LENGTH_SHORT).show()
////            return
////        }
//
//        val fileName = "capture-" + getCurSysDate() + ".mp4"
//        val filePath = File(directory, fileName)
//
//        val width: Int = mDisplayMetrics.widthPixels // null error may happen
//        val height: Int = mDisplayMetrics.heightPixels
//
//        mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
//        mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
//        mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//        mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//        mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//        mMediaRecorder?.setVideoEncodingBitRate(512 * 1000)
//        mMediaRecorder?.setVideoFrameRate(30)
//        mMediaRecorder?.setVideoSize(width, height)
//        mMediaRecorder?.setOutputFile(filePath.absolutePath)
//       // mMediaRecorder?.prepare()
//    }
//
//    private fun setUpVirtualDisplay(){
//        val screenDensity = mDisplayMetrics.densityDpi
//        val width = mDisplayMetrics.widthPixels
//        val height = mDisplayMetrics.heightPixels
//        val surface = binding.surface.holder.surface
//        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
//            "ScreenCapture",
//            width, height, screenDensity,
//            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//            surface /* mMediaRecorder!!. */, null /*Callbacks*/, null /*Handler*/
//        )
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_MEDIA_PROJECTION) {
//            if (resultCode != RESULT_OK) {
//                Log.i(TAG, "User cancelled")
//                Toast.makeText(this, "User denied screen sharing permission", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            Log.i(
//                TAG,
//                "Starting screen capture"
//            )
//            mResultCode = resultCode
//            mResultData = data
//            setUpMediaProjection()
//            setUpVirtualDisplay()
//
//            //mMediaRecorder?.prepare()
//            try {
//                mMediaRecorder?.prepare()
//                mMediaRecorder?.start()
//            }
//            catch (e: Exception){
//                Log.d(TAG, "onActivityResult: media recorder start exception ${e.stackTraceToString()}")
//            }
//
//
//            gotoMainActivity()
//        }
//    }


//    private val mediaProjectionLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if(result.resultCode != RESULT_OK) {
//                Toast.makeText(this, "Screen Cast Permission Denied :(", Toast.LENGTH_SHORT).show()
//            }
//            else {
//                mMediaProjection = mProjectionManager?.getMediaProjection(result.resultCode, result.data!!) // error possible
//                // TODO Register a callback that will listen onStop and release & prepare the recorder for next recording
//                //mMediaProjection.registerCallback(callback, null);
//                setUpVirtualDisplay()
//                mMediaRecorder!!.start()
//            }
//
//        }
//            override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//                super.onActivityResult(requestCode, resultCode, data)
//                if (requestCode != CAST_PERMISSION_CODE) {
//                    // Where did we get this request from ? -_-
//                    Log.w(TAG, "Unknown request code: $requestCode")
//                    return
//                }
//                if (resultCode != RESULT_OK) {
//                    Toast.makeText(this, "Screen Cast Permission Denied :(", Toast.LENGTH_SHORT).show()
//                    return
//                }
//                mMediaProjection = mProjectionManager?.getMediaProjection(resultCode, data)
//                // TODO Register a callback that will listen onStop and release & prepare the recorder for next recording
//                // mMediaProjection.registerCallback(callback, null);
//                mVirtualDisplay = getVirtualDisplay()
//                mMediaRecorder!!.start()
//            }
//        }
//}