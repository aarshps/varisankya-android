package com.hora.varisankya.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.hora.varisankya.Subscription
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object WidgetUpdateHelper {

    fun updateWidgetData(context: Context, subscriptions: List<Subscription>) {
        val today = java.time.LocalDate.now()
        val windowDays = com.hora.varisankya.PreferenceHelper.getNotificationDays(context)
        val maxDate = today.plusDays(windowDays.toLong())

        // Filter for active subscriptions within the notification window
        val upcomingFiltered = subscriptions.filter { sub ->
            if (!sub.active || sub.dueDate == null) return@filter false
            
            // Convert stored UTC date to LocalDate
            val dueInstant = sub.dueDate!!.toInstant()
            val dueZoned = dueInstant.atZone(java.time.ZoneId.of("UTC"))
            val dueLocalDate = dueZoned.toLocalDate()
            
            // Include if due date is NOT after maxDate (so past dates + dates up to limit are included)
            !dueLocalDate.isAfter(maxDate)
        }.sortedBy { it.dueDate!!.time }

        val totalUpcomingCount = upcomingFiltered.size

        // Take only top 3 for display
        val displayList = upcomingFiltered.take(3)

        val widgetItems = displayList.map { sub ->
            val dueInstant = sub.dueDate!!.toInstant()
            val dueZoned = dueInstant.atZone(java.time.ZoneId.of("UTC"))
            val dueLocalDate = dueZoned.toLocalDate()
            
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, dueLocalDate).toInt()

            val dateStr = when {
                daysDiff < 0 -> "${-daysDiff}d Overdue"
                daysDiff == 0 -> "Today"
                daysDiff == 1 -> "Tomorrow"
                else -> "$daysDiff Days"
            }
            Pair(sub.name, dateStr)
        }

        // Save data to preferences with real total count
        UpcomingWidgetProvider.saveData(context, widgetItems, totalUpcomingCount)

        // Trigger update to the widget views
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, UpcomingWidgetProvider::class.java))
        
        if (ids.isNotEmpty()) {
            val intent = Intent(context, UpcomingWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    fun refreshFromFirestore(context: Context) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        firestore.collection("users").document(userId).collection("subscriptions")
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snapshots ->
                val subscriptions = snapshots.toObjects(com.hora.varisankya.Subscription::class.java)
                updateWidgetData(context, subscriptions)
            }
    }
}
