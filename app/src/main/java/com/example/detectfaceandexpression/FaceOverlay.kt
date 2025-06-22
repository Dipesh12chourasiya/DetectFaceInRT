package com.example.detectfaceandexpression

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.common.InputImage


class FaceOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var faceBounds: List<Rect> = listOf()
    private var rotY: Float = 0f
    private var rotZ: Float = 0f

    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 40f
    }

    fun updateFaces(faces: List<Face>, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) {
        val scaleX = width.toFloat() / imageHeight.toFloat() // imageHeight used for rotation
        val scaleY = height.toFloat() / imageWidth.toFloat()

        faceBounds = faces.map {
            val box = it.boundingBox
            Rect(
                (box.left * scaleX).toInt(),
                (box.top * scaleY).toInt(),
                (box.right * scaleX).toInt(),
                (box.bottom * scaleY).toInt()
            )
        }

        rotY = faces.firstOrNull()?.headEulerAngleY ?: 0f
        rotZ = faces.firstOrNull()?.headEulerAngleZ ?: 0f
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        faceBounds.forEachIndexed { index, rect ->
            canvas.drawRect(rect, paint)
            canvas.drawText("Face ${index + 1}", rect.left.toFloat(), rect.top.toFloat() - 20, textPaint)
        }
        canvas.drawText("Yaw (rotY): $rotY°", 50f, height - 100f, textPaint)
        canvas.drawText("Tilt (rotZ): $rotZ°", 50f, height - 50f, textPaint)
    }
}
