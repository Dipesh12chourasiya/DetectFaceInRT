package com.example.detectfaceandexpression.roomDB

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateTime: String,
    val duration: String,
    val attentionPercent: Int,

    val totalFaces: Int = 0,          // Total faces frames
    val totalFrames: Int = 0,          // Total frames frames
    val inattentiveCount: Int = 0,    // Frames marked as inattentive
    val attentiveCount: Int = 0,      // Frames marked as attentive
    val maxInattentiveStreak: Int = 0, // Longest inattentive period (in seconds/frames)

    val notes: String? = null          // Teacher/observer notes
) : Parcelable