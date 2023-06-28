package com.vungn.camerax.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StopWatch {
    private var startTime: Long = 0
    private var pauseTime: Long = 0
    private var pauseStartTime: Long = 0
    private var stopTime: Long = 0
    private var running = false

    fun start() {
        this.startTime = System.currentTimeMillis()
        this.running = true
    }

    fun stop() {
        this.stopTime = System.currentTimeMillis()
        this.running = false
    }

    fun pause() {
        this.pauseStartTime = System.currentTimeMillis()
    }

    fun resume() {
        this.pauseTime += System.currentTimeMillis() - this.pauseStartTime
    }

    fun reset() {
        this.running = false
        this.startTime = 0
        this.stopTime = 0
        this.pauseTime = 0
        this.pauseStartTime = 0
    }

    //elaspsed time in milliseconds
    private fun getElapsedTime(): Long = if (running) {
        System.currentTimeMillis() - startTime - pauseTime
    } else {
        stopTime - startTime - pauseTime
    }

    fun getTime(formatString: String = "mm:ss"): String {
        val date = Date(getElapsedTime())
        return SimpleDateFormat(formatString, Locale.ENGLISH).format(date)
    }
}