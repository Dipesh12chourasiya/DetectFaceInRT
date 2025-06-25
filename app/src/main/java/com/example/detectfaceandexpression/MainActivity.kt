package com.example.detectfaceandexpression

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.TimeUtils.formatDuration
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.detectfaceandexpression.databinding.ActivityMainBinding
import com.example.detectfaceandexpression.utils.Utils
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private var isSessionRunning = false
    private var sessionStartTime: Long = 0L
    private val handler = Handler()

    private var totalFaces = 0
    private var inattentiveCount = 0
    private var attentiveCount = 0
    private var percentAttentive = 0

    private val CAMERA_PERMISSION_CODE = 101

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - sessionStartTime
            binding.tvTimer.text = " " + Utils.formatDuration(elapsed)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestCameraPermission()

        binding.btnSwitchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                CameraSelector.LENS_FACING_BACK
            else
                CameraSelector.LENS_FACING_FRONT
            bindCameraUseCases()
        }

        binding.btnStartSession.setOnClickListener {
            isSessionRunning = true
            sessionStartTime = System.currentTimeMillis()
            handler.post(timerRunnable)

            binding.btnStartSession.visibility = View.GONE
            binding.btnEndSession.visibility = View.VISIBLE

            Toast.makeText(this, "📸 Session Started", Toast.LENGTH_SHORT).show()
        }

        binding.btnEndSession.setOnClickListener {
            isSessionRunning = false
            val durationMillis = System.currentTimeMillis() - sessionStartTime
            val formattedDuration = Utils.formatDuration(durationMillis)
            val sessionTimestamp = Utils.getCurrentDateTime()

            val intent = Intent(this, StatsActivity::class.java).apply {
                putExtra("totalFaces", totalFaces)
                putExtra("attentiveCount", attentiveCount)
                putExtra("attentionPercent", percentAttentive)
                putExtra("sessionDuration", formattedDuration)
                putExtra("sessionTimestamp", sessionTimestamp)
                putExtra("totalFrames", attentiveCount + inattentiveCount)
            }

            startActivity(intent)

            binding.btnStartSession.visibility = View.VISIBLE
            binding.btnEndSession.visibility = View.GONE
        }
    }

    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), FaceAnalyzer(this))
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, analyzer)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_LONG).show()
        }
    }

    inner class FaceAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {
        private val detector by lazy {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
            FaceDetection.getClient(options)
        }

        private val eyeClosedFrames = mutableMapOf<Int, Int>()
        private val attentiveFrames = mutableMapOf<Int, Int>()
        private val inattentiveFrames = mutableMapOf<Int, Int>()
        private val attentionMap = mutableMapOf<Int, Boolean>()

        private var mediaPlayer: MediaPlayer? = null

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: return imageProxy.close()

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (!isSessionRunning) {
                        imageProxy.close()
                        return@addOnSuccessListener
                    }

                    if (faces.isNotEmpty()) {
                        val first = faces[0]
                        val smile = first.smilingProbability?.times(100)?.toInt() ?: 0
                        val lEye = first.leftEyeOpenProbability?.times(100)?.toInt() ?: 0
                        val rEye = first.rightEyeOpenProbability?.times(100)?.toInt() ?: 0

                        runOnUiThread {
                            binding.tvStatsFaceEye.text = "Smile: $smile%\nLeft Eye: $lEye%\nRight Eye: $rEye%"
                            binding.faceOverlay.updateFaces(faces, image.width, image.height, imageProxy.imageInfo.rotationDegrees)
                        }
                    }

                    totalFaces = faces.size
                    inattentiveCount = 0

                    for (face in faces) {
                        val id = face.trackingId ?: continue
                        val smile = face.smilingProbability ?: 0f
                        val avgEye = ((face.leftEyeOpenProbability ?: 0f) + (face.rightEyeOpenProbability ?: 0f)) / 2

                        if (avgEye < 0.4f) {
                            eyeClosedFrames[id] = eyeClosedFrames.getOrDefault(id, 0) + 1
                        } else {
                            eyeClosedFrames[id] = 0
                        }

                        val inattentive = smile < 0.3f && (eyeClosedFrames[id] ?: 0) > 15

                        if (inattentive) {
                            inattentiveFrames[id] = inattentiveFrames.getOrDefault(id, 0) + 1
                        } else {
                            attentiveFrames[id] = attentiveFrames.getOrDefault(id, 0) + 1
                        }

                        val wasAttentive = attentionMap[id] != true
                        if (inattentive && wasAttentive) {
                            playAlertSound()
                            attentionMap[id] = true
                        }

                        if (!inattentive) {
                            attentionMap[id] = false
                            stopAlertSound()
                        }
                    }

                    val totalAtt = attentiveFrames.values.sum()
                    val totalInatt = inattentiveFrames.values.sum()
                    val totalFrames = totalAtt + totalInatt

                    attentiveCount = totalAtt
                    inattentiveCount = totalInatt
                    percentAttentive = if (totalFrames > 0) totalAtt * 100 / totalFrames else 0

                    runOnUiThread {
                        val color = when {
                            percentAttentive >= 80 -> "#8FE693"
                            percentAttentive >= 50 -> "#FF5722"
                            else -> "#F44336"
                        }
                        binding.tvStats.text = "Total: $totalFaces | Total Frames: $totalFrames"
                        binding.tvAttentivePercents.text = "$percentAttentive%"
                        binding.tvAttentivePercents.setTextColor(Color.parseColor(color))
                        binding.tvStats.setBackgroundColor(Color.parseColor("#3300AA00"))
                        binding.tvStats.setTextColor(Color.WHITE)
                    }
                }
                .addOnFailureListener { }
                .addOnCompleteListener { imageProxy.close() }
        }

        private fun playAlertSound() {
            stopAlertSound()
            mediaPlayer = MediaPlayer.create(context, R.raw.attention_sound)
            mediaPlayer?.start()
        }

        private fun stopAlertSound() {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        }
    }
}


