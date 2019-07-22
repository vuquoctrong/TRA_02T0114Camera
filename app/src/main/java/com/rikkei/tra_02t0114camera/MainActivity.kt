@file:Suppress("DEPRECATION")

package com.rikkei.tra_02t0114camera

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.provider.MediaStore
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.R.attr.data
import android.content.Context
import android.media.CamcorderProfile
import android.media.MediaRecorder
import com.rikkei.tra_02t0114camera.constant.Define


class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private var mcamera: Camera? = null
    private var msurfaceView: SurfaceView? = null
    private var msurfaceHolder: SurfaceHolder? = null

    private var captureImageCallback: Camera.PictureCallback? = null
    private var rawCallback: Camera.PictureCallback? = null
    private var shutterCallback: Camera.ShutterCallback? = null


    private var frontCam: Boolean = false
    private val mediaRecoder by lazy {
        MediaRecorder()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()

    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun init() {
        msurfaceView = findViewById(R.id.surfaceView)
        msurfaceHolder = msurfaceView!!.holder
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO),
                Define.MY_CAMERA_REQUEST_CODE
            )
        } else {
            msurfaceHolder!!.addCallback(this)
            msurfaceHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
        captureImage()
        ivRotateCamera.setOnClickListener { roTateCamera() }
        ivCamera.setOnClickListener { saveImage() }
        ivVideo.setOnClickListener { openVideo() }
        frontCam = false
    }

    private fun refreshCamera() {
        if (msurfaceHolder!!.surface == null) return

        try {
            mcamera!!.stopPreview()
        } catch (e: Exception) {
        }

        try {
            mcamera!!.setPreviewDisplay(msurfaceHolder)
            mcamera!!.startPreview()
        } catch (e: Exception) {
        }

    }

    private fun roTateCamera() {
        if (frontCam) {
            val cameraId = isBackCameraExisted()
            if (cameraId >= 0) {
                try {
                    mcamera!!.stopPreview()
                    mcamera!!.release()

                    mcamera = Camera.open(cameraId)
                    mcamera!!.setPreviewDisplay(msurfaceHolder)
                    mcamera!!.startPreview()

                    frontCam = false

                    changeOrientation()
                } catch (e: RuntimeException) {
                } catch (e: Exception) {
                }

                val param = mcamera!!.parameters

                param.setPreviewSize(msurfaceView!!.width, msurfaceView!!.height)
                param.previewFrameRate = 50
                param.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }
        } else {
            val cameraId = isFrontCameraExisted()
            if (cameraId >= 0) {
                try {
                    mcamera!!.stopPreview()
                    mcamera!!.release()

                    mcamera = Camera.open(cameraId)
                    mcamera!!.setPreviewDisplay(msurfaceHolder)
                    mcamera!!.startPreview()

                    frontCam = true

                    changeOrientation()
                } catch (e: RuntimeException) {
                } catch (e: Exception) {
                }

                val param = mcamera!!.parameters

                param.setPreviewSize(msurfaceView!!.width, msurfaceView!!.height)
                param.previewFrameRate = 30
                param.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }
        }
    }

    private fun captureImage() {
        rawCallback = Camera.PictureCallback { data, camera -> Log.d("Log", "onPictureTaken - raw") }
        /** Handles data for jpeg picture  */
        shutterCallback = Camera.ShutterCallback { Log.i("Log", "onShutter'd") }
        captureImageCallback = Camera.PictureCallback { data, camera ->
            var pictureFile: File? = getOutputMediaFile() ?: return@PictureCallback
            try {
                val fos = FileOutputStream(pictureFile)
                fos.write(data)
                fos.close()
            } catch (e: FileNotFoundException) {

            } catch (e: IOException) {
            }
        }


    }
    private fun  saveImage(){
        try {
            mcamera!!.takePicture(shutterCallback, rawCallback, captureImageCallback)
            refreshCamera()
        } catch (e: Exception) {
        }

    }

    private fun getOutputMediaFile(): File? {
        val mediaStorageDir = File(
            Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MyCameraApp"
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory")
                return null
            }
        }
        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss")
            .format(Date())
        val mediaFile: File
        mediaFile = File(
            mediaStorageDir.path + File.separator
                    + "IMG_" + timeStamp + ".jpg"
        )
        return mediaFile
    }


    private fun isBackCameraExisted(): Int {
        var cameraId = -1
        val numberOfCameras = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i
                break
            }
        }
        return cameraId
    }

    private fun changeOrientation() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            mcamera!!.setDisplayOrientation(0)
        else
            mcamera!!.setDisplayOrientation(90)
    }

    private fun isFrontCameraExisted(): Int {
        var cameraId = -1
        val numberOfCameras = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i
                break
            }
        }
        return cameraId
    }


    private fun createVideoFileName(): String {
        val timeFile = SimpleDateFormat("yyMMđ_HHmmss").format(Date())
        return "VIDEPO_$timeFile.mp4"
    }

    private fun openVideo() {
        mcamera = Camera.open()
        mcamera?.unlock()
        var outputFilemp4 = Environment.getExternalStorageDirectory().absolutePath + "/recording4.mp4"
        mediaRecoder.apply {
            setCamera(mcamera)
            setVideoEncodingBitRate(5000000)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
         //   setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            //setVideoFrameRate(24)
            setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
            setOutputFile(outputFilemp4)
        }
        msurfaceView?.background.apply { null }
        mediaRecoder.setPreviewDisplay(msurfaceHolder?.surface)
        try {
            mediaRecoder.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun stopVideo(){
        mediaRecoder.apply {
            stop()
            reset()
            release()
        }
        mcamera?.release()
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        mcamera = Camera.open()
        val parameters: Camera.Parameters = mcamera!!.parameters
        mcamera!!.setDisplayOrientation(90)
        parameters.previewFrameRate = 30
        parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        try {
            mcamera!!.setPreviewDisplay(holder)

        } catch (e: IOException) {
            e.printStackTrace()
        }
        mcamera!!.startPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        try {
            mcamera!!.setPreviewDisplay(msurfaceHolder)
            mcamera!!.startPreview()
        } catch (e: Exception) {
            // intentionally left blank for a test
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mcamera!!.stopPreview()
        mcamera!!.release()
        mcamera = null
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        changeOrientation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Define.MY_CAMERA_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                msurfaceHolder!!.addCallback(this)
                msurfaceHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
            } else {
                Toast.makeText(this, "Please provide the permission", Toast.LENGTH_SHORT).show()
            }
            else -> {
            }
        }
    }

}
