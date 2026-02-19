package com.hora.varisankya.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object DateHelper {

    /**
     * MaterialDatePicker returns the selected day as UTC midnight millis.
     * Convert that day into a stable local date value (stored at local noon).
     */
    fun fromPickerSelectionMillis(selectionUtcMillis: Long): Date {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = selectionUtcMillis
        }
        return localNoon(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Convert a stored due date into UTC midnight millis for MaterialDatePicker selection.
     */
    fun toPickerSelectionMillis(date: Date): Long {
        val normalized = normalizeDueDate(date)
        val local = Calendar.getInstance().apply { time = normalized }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, local.get(Calendar.YEAR))
            set(Calendar.MONTH, local.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utc.timeInMillis
    }

    /**
     * Normalize stored due dates to a stable local-date representation.
     * Legacy records saved from picker UTC-midnight are converted using UTC Y/M/D.
     * Current records are treated using local Y/M/D.
     */
    fun normalizeDueDate(date: Date): Date {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }
        val isUtcMidnight = utc.get(Calendar.HOUR_OF_DAY) == 0 &&
            utc.get(Calendar.MINUTE) == 0 &&
            utc.get(Calendar.SECOND) == 0 &&
            utc.get(Calendar.MILLISECOND) == 0

        return if (isUtcMidnight) {
            localNoon(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH)
            )
        } else {
            val local = Calendar.getInstance().apply { time = date }
            localNoon(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    fun calculateNextDueDate(fromDate: Date, recurrence: String): Date? {
        val cal = Calendar.getInstance().apply {
            time = normalizeDueDate(fromDate)
        }
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

    private fun localNoon(year: Int, month: Int, dayOfMonth: Int): Date {
        val local = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return local.time
    }
}
