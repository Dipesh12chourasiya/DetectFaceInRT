package com.example.detectfaceandexpression

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.detectfaceandexpression.databinding.ActivitySessionDetailBinding
import com.example.detectfaceandexpression.roomDB.SessionEntity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class SessionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySessionDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionDetailBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // Toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Get session data from intent
        val session = intent.getParcelableExtra<SessionEntity>("session_data")

        session?.let {
            binding.toolbar.title = it.title
            binding.tvSessionDate.text = "Date: ${it.dateTime}"
            binding.tvSessionDuration.text = "Duration: ${it.duration}"
            binding.tvTotalFaces.text = "Total Faces: ${it.totalFaces}"
            binding.tvTotalFrames.text = "Total Frames: ${it.totalFrames}"
            binding.tvAttentivePercent.text = "Attentive: ${it.attentionPercent}%"

            val inattentivePercent = 100 - it.attentionPercent
            binding.tvInAttentivePercent.text = "Inattentive: $inattentivePercent%"

            binding.tvNoteText.text = it.notes ?: "No notes added"

            setupPieChart(it.attentiveCount, it.totalFrames - it.attentiveCount)

            binding.btnShare.setOnClickListener {
                shareSession(session)
            }

        }
    }

    private fun shareSession(session: SessionEntity) {

        session?.let {
            val shareText = """
            📊 Session Report 📊
            
            📝 Title: ${it.title}
            📅 Date: ${it.dateTime}
            ⏱ Duration: ${it.duration}
            
            👥 Total Faces: ${it.totalFaces}
            🎞 Total Frames: ${it.totalFrames}
            ✅ Attentive: ${it.attentionPercent}%
            ❌ Inattentive: ${100 - it.attentionPercent}%
            
            🗒 Notes: ${it.notes ?: "No notes"}
        """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Session Report: ${it.title}")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            // Launch chooser so user can select any app to share
            startActivity(Intent.createChooser(intent, "Share session via"))
        }
    }


    private fun setupPieChart(attentive: Int, inattentive: Int) {

        // 1. Data Entries
        val entries = listOf(
            PieEntry(attentive.toFloat(), "Attentive"), // Simplified labels, emojis can sometimes be inconsistent
            PieEntry(inattentive.toFloat(), "Inattentive")
        )

        // 2. DataSet Configuration
        val dataSet = PieDataSet(entries, "") // Legend label for dataSet can be empty if legend shows slice labels directly
        dataSet.sliceSpace = 2f // Slightly reduced slice space for a more compact look
        dataSet.selectionShift = 5f // How much a slice moves out when selected (optional, but can add interaction)

        // Professional Color Palette (example - you can define your own in colors.xml)
        // Using a more subdued or complementary palette often looks more professional.
        // Example: Defining custom colors. Make sure these colors are defined in your colors.xml
        val colors = listOf(
            resources.getColor(R.color.chart_color_attentive), // e.g., #4CAF50 (Green)
            resources.getColor(R.color.chart_color_inattentive) // e.g., #F44336 (Red)
            // Or a more neutral palette
            // resources.getColor(R.color.pieChartBlue), // #2196F3
            // resources.getColor(R.color.pieChartOrange) // #FF9800
        )
        dataSet.colors = colors

        // 3. PieData Configuration
        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter()) // Format values as percentages
        pieData.setValueTextSize(12f) // Slightly smaller value text for less clutter
        pieData.setValueTextColor(Color.WHITE) // White text for better contrast on colored slices
        pieData.setValueTypeface(Typeface.DEFAULT_BOLD) // Bold values for emphasis

        // 4. PieChart Customization
        val pieChart = findViewById<PieChart>(R.id.attentionPieChart)
        pieChart.data = pieData

        // General Chart Settings
        pieChart.description.isEnabled = false // Keep description disabled for a cleaner look
        pieChart.setUsePercentValues(true) // Display values as percentages
        pieChart.setExtraOffsets(20f, 20f, 20f, 20f) // Add extra space around the chart

        // Hole and Center Text
        pieChart.isDrawHoleEnabled = true // Enable the hole
        pieChart.holeRadius = 58f // Size of the inner hole (e.g., 58%)
        pieChart.transparentCircleRadius = 61f // Size of the transparent circle outside the hole (adds a subtle glow)
        pieChart.setHoleColor(Color.TRANSPARENT) // Make the hole transparent or a light background color

        // Center text customization
        pieChart.setDrawCenterText(true)
        pieChart.centerText = "Attention\nStatus" // Multi-line center text for better aesthetics
        pieChart.setCenterTextSize(16f)
        pieChart.setCenterTextColor(Color.DKGRAY) // Darker gray for center text
        pieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)

        // Entry Label Customization (labels on the slices)
        pieChart.setDrawEntryLabels(false) // Often, the legend is sufficient for labels, or you can draw them if space allows
        // If you do want entry labels:
        // pieChart.setEntryLabelColor(Color.BLACK)
        // pieChart.setEntryLabelTextSize(10f)
        // pieChart.setEntryLabelTypeface(Typeface.DEFAULT)

        // Animation
        pieChart.animateY(1400) // Slightly longer animation for a smoother feel

        // 5. Legend Customization
        val legend = pieChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false) // Draw legend outside the chart
        legend.xEntrySpace = 8f // Space between legend entries
        legend.yEntrySpace = 0f // No vertical space between entries
        legend.yOffset = 10f // Offset from the bottom of the chart
        legend.textSize = 13f
        legend.textColor = Color.DKGRAY // Darker gray for legend text
        legend.isWordWrapEnabled = true // Enable word wrap for longer labels
        legend.form = Legend.LegendForm.CIRCLE // Use circles as legend forms

        // 6. Interaction (optional, but adds to "professionalism")
        pieChart.setTouchEnabled(true)
        pieChart.isHighlightPerTapEnabled = true // Enable highlighting on tap

        // 7. Refresh
        pieChart.invalidate() // Refresh the chart to apply all changes
    }

}
