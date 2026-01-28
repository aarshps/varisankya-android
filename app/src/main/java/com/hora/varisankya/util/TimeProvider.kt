package com.hora.varisankya.util

import java.util.Calendar
import java.util.Date

/**
 * Interface to provide the current time.
 * Allows switching between Real Time and Simulated (Sandbox) Time.
 */
interface TimeProvider {
    fun now(): Date
    fun calendar(): Calendar
}

/**
 * Standard implementation returning System time.
 */
class RealTimeProvider : TimeProvider {
    override fun now(): Date {
        return Date()
    }

    override fun calendar(): Calendar {
        return Calendar.getInstance()
    }
}

/**
 * Sandbox implementation allowing time travel.
 */
class SandboxTimeProvider : TimeProvider {
    private var simulatedTime: Long = System.currentTimeMillis()

    fun setTime(date: Date) {
        simulatedTime = date.time
    }
    
    fun setTime(timestamp: Long) {
        simulatedTime = timestamp
    }
    
    fun advanceTime(days: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = simulatedTime
        cal.add(Calendar.DAY_OF_YEAR, days)
        simulatedTime = cal.timeInMillis
    }

    override fun now(): Date {
        return Date(simulatedTime)
    }

    override fun calendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.timeInMillis = simulatedTime
        return cal
    }
}
