package com.netheal.ui

data class UsageInfo(val sent: Long, val recv: Long, val packets: Long)

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val k = bytes / 1024
    if (k < 1024) return "$k KB"
    val m = k / 1024
    return "$m MB"
}
