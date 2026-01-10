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
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayTime = today.timeInMillis
        
        // Get notification window from settings
        val windowDays = com.hora.varisankya.PreferenceHelper.getNotificationDays(context)
        // Add 23h 59m to make the window inclusive of the target day's end
        val windowMillis = (windowDays * 24L * 60L * 60L * 1000L) + (23L * 60L * 60L * 1000L) + (59L * 60L * 1000L)
        val maxDueTime = todayTime + windowMillis

        // Filter for active subscriptions within the notification window
        val upcomingFiltered = subscriptions.filter { sub ->
            sub.active && sub.dueDate != null && sub.dueDate!!.time <= maxDueTime
        }.sortedBy { it.dueDate!!.time }

        val totalUpcomingCount = upcomingFiltered.size

        // Take only top 3 for display
        val displayList = upcomingFiltered.take(3)

        val widgetItems = displayList.map { sub ->
            val dueTime = sub.dueDate!!.time
            val diff = dueTime - todayTime
            val daysDiff = (diff / (1000 * 60 * 60 * 24)).toInt()

            val dateStr = when {
                daysDiff < 0 -> {
                     val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                     dateFormat.format(sub.dueDate!!)
                }
                daysDiff == 0 -> "Today"
                daysDiff == 1 -> "Tomorrow"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    dateFormat.format(sub.dueDate!!)
                }
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
