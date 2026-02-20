package com.hora.varisankya.util

import java.util.Calendar
import java.util.Date

object DateHelper {

    // ThreadLocal caches the Calendar instance per thread, completely eliminating
    // massive allocation overhead when this is called hundreds of times in loops
    private val calendarPool = object : ThreadLocal<Calendar>() {
        override fun initialValue(): Calendar = Calendar.getInstance()
    }

    fun calculateNextDueDate(fromDate: Date, recurrence: String): Date? {
        val cal = calendarPool.get()!!
        cal.time = fromDate
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        if (recurrence == "Custom") return null

        if (recurrence.startsWith("Every ")) {
            val parts = recurrence.split(" ")
            if (parts.size >= 3) {
                val freq = parts[1].toIntOrNull() ?: 1
                val unit = parts[2]
                when (unit) {
                    "Months", "Month" -> cal.add(Calendar.MONTH, freq)
                    "Years", "Year" -> cal.add(Calendar.YEAR, freq)
                    "Weeks", "Week" -> cal.add(Calendar.WEEK_OF_YEAR, freq)
                    "Days", "Day" -> cal.add(Calendar.DAY_OF_YEAR, freq)
                    else -> cal.add(Calendar.MONTH, freq)
                }
            }
        } else {
             when (recurrence) {
                "Monthly" -> cal.add(Calendar.MONTH, 1)
                "Yearly" -> cal.add(Calendar.YEAR, 1)
                "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "Daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                else -> cal.add(Calendar.MONTH, 1)
            }
        }
        return cal.time
    }
}
