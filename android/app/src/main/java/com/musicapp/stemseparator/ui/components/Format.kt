package com.musicapp.stemseparator.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.log10
import kotlin.math.pow

/** Mirrors app.js's fmtBytes(): human-readable file size. */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return if (digitGroups == 0) "${bytes} B" else "%.1f %s".format(value, units[digitGroups])
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

/** Short date/time for compact list rows -- mirrors app.js's toLocaleString() in
 * spirit, but omits the year since this is a personal-scale app where history rarely
 * spans multiple years. */
fun formatDateTime(instant: Instant): String =
    dateTimeFormatter.withZone(ZoneId.systemDefault()).format(instant)

/** Mirrors the mixer's fmtTime() for the transport's time display. */
fun formatDuration(totalSeconds: Double): String {
    if (totalSeconds.isNaN() || totalSeconds < 0) return "0:00"
    val totalSecondsInt = totalSeconds.toInt()
    val minutes = totalSecondsInt / 60
    val seconds = totalSecondsInt % 60
    return "%d:%02d".format(minutes, seconds)
}
