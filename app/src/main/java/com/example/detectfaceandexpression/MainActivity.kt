package com.example.detectfaceandexpression

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
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
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var binding : ActivityMainBinding

    var totalFaces = 0
    var inattentiveCount = 0
    var attentiveCount = 0
    var percentAttentive = 0

//    var duration = formatDuration(System.currentTimeMillis() - sessionStartTime)

    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var cameraProvider: ProcessCameraProvider


    private var isSessionRunning = false
    private var sessionStartTime: Long = 0L
    private val handler = android.os.Handler()

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - sessionStartTime
            binding.tvTimer.text = " " + Utils.formatDuration(elapsed)
            handler.postDelayed(this, 1000)
        }
    }

 // on create method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestCameraPermission() // to ask camera permission


        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))

        binding.btnSwitchCamera.setOnClickListener {
            // Toggle front/back
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                CameraSelector.LENS_FACING_BACK
            else
                CameraSelector.LENS_FACING_FRONT

            // Rebind with new camera
            bindCameraUseCases()
        }

        binding.btnViewReport.setOnClickListener {
            if (!isSessionRunning) {
                Toast.makeText(this, "Session not started yet!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val durationMillis = System.currentTimeMillis() - sessionStartTime
            val duration = Utils.formatDuration(durationMillis)

            val formatter = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            val timestamp = formatter.format(java.util.Date(sessionStartTime))

            val intent = Intent(this, StatsActivity::class.java).apply {
                putExtra("totalFaces", totalFaces)
                putExtra("attentiveCount", attentiveCount)
                putExtra("attentionPercent", percentAttentive)
                putExtra("sessionDuration", duration)
                putExtra("sessionTimestamp", timestamp)
            }
            startActivity(intent)
        }


//        binding.btnStartSession.setOnClickListener {
//            if (!isSessionRunning) {
//                isSessionRunning = true
//                sessionStartTime = System.currentTimeMillis()
//                handler.post(timerRunnable)
//
//                Toast.makeText(this, "Session started!", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "Session already running", Toast.LENGTH_SHORT).show()
//            }
//        }

     binding.btnStartSession.setOnClickListener {
         isSessionRunning = true
         sessionStartTime = System.currentTimeMillis()
         handler.post (timerRunnable)

         binding.btnStartSession.visibility = View.GONE
         binding.btnEndSession.visibility = View.VISIBLE

         Toast.makeText(this, "📸 Session Started", Toast.LENGTH_SHORT).show()
     }

     binding.btnEndSession.setOnClickListener {
         isSessionRunning = false
         val sessionEndTime = System.currentTimeMillis()

         val durationMillis = sessionEndTime - sessionStartTime
         val sessionDurationFormatted = Utils.formatDuration(durationMillis)
         val sessionTimestamp = Utils.getCurrentDateTime()

         val intent = Intent(this, StatsActivity::class.java).apply {
             putExtra("totalFaces", totalFaces)
             putExtra("attentiveCount", attentiveCount)
             putExtra("attentionPercent", percentAttentive)
             putExtra("sessionDuration", sessionDurationFormatted)
             putExtra("sessionTimestamp", sessionTimestamp)
         }

         startActivity(intent)

         // Optional: Reset UI
         binding.btnStartSession.visibility = View.VISIBLE
         binding.btnEndSession.visibility = View.GONE
     }

 }

    private fun bindCameraUseCases() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), FaceAnalyzer(this))
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
    }


    private val CAMERA_PERMISSION_CODE = 101
    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        )  {
            // Permission not granted → request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }


    inner class FaceAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {
        private val faceAttentionMap = mutableMapOf<Int, Boolean>()
        private val eyeClosedFrames = mutableMapOf<Int, Int>()

        private val attentiveFramesPerFace = mutableMapOf<Int, Int>()
        private val inattentiveFramesPerFace = mutableMapOf<Int, Int>()

        private val detector by lazy {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking() // ✅ Enable tracking
                .build()

            FaceDetection.getClient(options)
        }

        private var lastPlayedFaceId = -1
        private var lastPlayedTime = 0L

        private var mediaPlayer: MediaPlayer? = null


        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->

                    if (!isSessionRunning) {
                        imageProxy.close()
                        return@addOnSuccessListener
                    }

                    if (faces.isNotEmpty()) {
                        // Show stats for the first face only (for simplicity)
                        val firstFace = faces[0]
                        val smileProb = firstFace.smilingProbability?.times(100)?.toInt() ?: 0
                        val leftEye = firstFace.leftEyeOpenProbability?.times(100)?.toInt() ?: 0
                        val rightEye = firstFace.rightEyeOpenProbability?.times(100)?.toInt() ?: 0

                        runOnUiThread {
                            binding.tvStatsFaceEye.text = "Smile: $smileProb%\nLeft Eye: $leftEye%\nRight Eye: $rightEye%"
                            binding.faceOverlay.updateFaces(faces, image.width, image.height, imageProxy.imageInfo.rotationDegrees)
                        }
                    }

                    // Check all faces for attention

                    totalFaces = faces.size
                    inattentiveCount = 0

                    for (face in faces) {
                        val faceId = face.trackingId ?: continue
                        val smile = face.smilingProbability ?: 0f
                        val leftEye = face.leftEyeOpenProbability ?: 0f
                        val rightEye = face.rightEyeOpenProbability ?: 0f
                        val avgEyeOpen = (leftEye + rightEye) / 2

                        // 👁️ Frame counter for eyes
                        if (avgEyeOpen < 0.4f) {
                            eyeClosedFrames[faceId] = eyeClosedFrames.getOrDefault(faceId, 0) + 1
                        } else {
                            eyeClosedFrames[faceId] = 0
                        }

                        val eyesClosedLongEnough = eyeClosedFrames[faceId] ?: 0 > 15
                        val isInattentive = smile < 0.3f && eyesClosedLongEnough

                        if (isInattentive) {
                            inattentiveFramesPerFace[faceId] = inattentiveFramesPerFace.getOrDefault(faceId, 0) + 1
                        } else {
                            attentiveFramesPerFace[faceId] = attentiveFramesPerFace.getOrDefault(faceId, 0) + 1
                        }


                        val wasPreviouslyAttentive = faceAttentionMap[faceId] != true

                        if (isInattentive && wasPreviouslyAttentive) {
                            playAlertSound()
                            faceAttentionMap[faceId] = true
                        }

                        if (!isInattentive) {
                            faceAttentionMap[faceId] = false
                            stopAlertSound()
                        }
                    }

                   // 📊 Update stats after loop
                    val totalAttentiveFrames = attentiveFramesPerFace.values.sum()
                    val totalInattentiveFrames = inattentiveFramesPerFace.values.sum()
                    val totalFrames = totalAttentiveFrames + totalInattentiveFrames

                    attentiveCount = totalAttentiveFrames
                    inattentiveCount = totalInattentiveFrames

                    percentAttentive = if (totalFrames > 0) {
                        (totalAttentiveFrames * 100 / totalFrames)
                    } else {
                        0
                    }


                    runOnUiThread {
                        val color = when {
                            percentAttentive >= 80 -> "#8FE693" // Green
                            percentAttentive >= 50 -> "#FF5722" // Orange
                            else -> "#F44336" // Red
                        }

                        binding.tvStats.text = "Total: $totalFaces | Total Frames: ${attentiveCount + inattentiveCount}"
                        binding.tvAttentivePercents.text = "$percentAttentive%"
                        binding.tvAttentivePercents.setTextColor(Color.parseColor(color))

                        binding.tvStats.setBackgroundColor(Color.parseColor("#3300AA00")) // transparent green
                        binding.tvStats.setTextColor(Color.WHITE)
                    }

                }
                .addOnFailureListener {
                    // Optional: Log or toast
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

        private fun playAlertSound() {
            stopAlertSound()

            mediaPlayer = MediaPlayer.create(context, R.raw.attention_sound)
            mediaPlayer?.start()
        }

        private fun stopAlertSound() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    it.release()
                }
            }
            mediaPlayer = null
        }
    }
}


