package com.example.detectfaceandexpression

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.detectfaceandexpression.databinding.ActivityHomeBinding
import com.example.detectfaceandexpression.viewmodels.UserViewModel
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {


    private lateinit var binding: ActivityHomeBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter : SessionAdapter

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

    /// ON create method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestCameraPermission()
        getAllSessions(this)

        binding.topAppBar.setNavigationOnClickListener {
            // Show a menu, drawer, or dialog here
            showPopupMenu()
        }


        binding.fabStartSession.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }


    private fun getAllSessions(context: Context) {

        adapter = SessionAdapter (
            onItemClick = { session ->
                val intent = Intent(this, SessionDetailActivity::class.java)
                intent.putExtra("session_data", session)
                startActivity(intent)
            },
            onItemLongClick = { session ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Session")
                    .setMessage("Are you sure you want to delete \"${session.title}\"?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.deleteSession(session)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )
        binding.rvSessions.adapter = adapter

        lifecycleScope.launch {
            viewModel.getAllSessions().collect { sessionList ->

                adapter.differ.submitList(sessionList.reversed())
                binding.rvSessions.adapter = adapter
                binding.rvSessions.layoutManager = LinearLayoutManager(context)
            }
        }
    }


    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage("Student Attention Tracker helps monitor and analyze student attentiveness during classes using real-time face detection.\n\nVersion 1.0")
            .setPositiveButton("OK", null)
            .show()
    }


    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Sessions?")
            .setMessage("This will delete all recorded sessions. Are you sure?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteAllSessions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, binding.topAppBar)
        popupMenu.menuInflater.inflate(R.menu.top_app_bar_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_about -> {
                    showAboutDialog()
                    true
                }

                R.id.menu_feedback -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("your-email@example.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback for Student Attention Tracker")
                    }
                    startActivity(Intent.createChooser(intent, "Send feedback via"))
                    true
                }
                R.id.menu_clear_data -> {
                    showClearConfirmationDialog()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }
}