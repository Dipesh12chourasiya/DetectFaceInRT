package com.example.detectfaceandexpression

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.detectfaceandexpression.databinding.ActivityStatsBinding


class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val totalFaces = intent.getIntExtra("totalFaces", 0)
        val attentiveCount = intent.getIntExtra("attentiveCount", 0)
        val attentionPercent = intent.getIntExtra("attentionPercent", 0)
        val sessionDuration = intent.getStringExtra("sessionDuration") ?: "0s"
        val sessionTimestamp = intent.getStringExtra("sessionTimestamp") ?: "N/A"

        binding.tvTotalFaces.text = "👥 Total Faces: $totalFaces"
        binding.tvAttentivePercent.text = "✅ Attentive: $attentionPercent%"
        binding.tvSessionDuration.text = "⏱ Duration: $sessionDuration"
        binding.tvSessionDate.text = " $sessionTimestamp"

        binding.btnExport.setOnClickListener {
            exportSessionAsText(totalFaces, attentiveCount, attentionPercent, sessionTimestamp)
        }
    }

    private fun exportSessionAsText(total: Int, attentive: Int, percent: Int, date: String) {
        val report = """
            🧠 Session Report
            ------------------------
            👥 Total Faces: $total
            ✅ Attentive: $attentive
            📊 Attention: $percent%
            📅 Date: $date
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, report)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Export Report via"))
    }
}
