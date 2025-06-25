package com.example.detectfaceandexpression.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = (durationMillis / (1000 * 60 * 60)) % 24

        if(hours > 0){
            return "${hours}h ${minutes}m ${seconds}s"
        }
        if ( minutes > 0){
            return "${minutes}m ${seconds}s"
        }
        return "${seconds}s"
    }

    fun getCurrentDateTime(): String {
//        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())

        return sdf.format(Date())
    }


}