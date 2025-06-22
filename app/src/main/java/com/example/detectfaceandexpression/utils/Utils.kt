package com.example.detectfaceandexpression.utils

object Utils {
     fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return "${minutes}m ${seconds}s"
    }

//    fun formatDuration(durationMillis: Long): String {
//        val seconds = (durationMillis / 1000) % 60
//        val minutes = (durationMillis / (1000 * 60)) % 60
//        val hours = (durationMillis / (1000 * 60 * 60)) % 24
//        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
//    }

    fun getCurrentDateTime(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }


}