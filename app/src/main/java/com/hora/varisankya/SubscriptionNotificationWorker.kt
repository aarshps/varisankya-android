package com.hora.varisankya

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SubscriptionNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("NotificationWorker", "Worker started")
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.success()
        val firestore = FirebaseFirestore.getInstance()

        try {
            val snapshots = firestore.collection("users")
                .document(userId)
                .collection("subscriptions")
                .get()
                .await()

            val subscriptions = snapshots.toObjects(Subscription::class.java)
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val sevenDaysLater = Calendar.getInstance()
            sevenDaysLater.time = today.time
            sevenDaysLater.add(Calendar.DAY_OF_YEAR, 7)

            var notificationCount = 0

            subscriptions.forEach { sub ->
                sub.dueDate?.let { dueDate ->
                    val dueCal = Calendar.getInstance()
                    dueCal.time = dueDate
                    dueCal.set(Calendar.HOUR_OF_DAY, 0)
                    dueCal.set(Calendar.MINUTE, 0)
                    dueCal.set(Calendar.SECOND, 0)
                    dueCal.set(Calendar.MILLISECOND, 0)

                    val diff = dueCal.timeInMillis - today.timeInMillis
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(diff).toInt()

                    // Notify if due today or in the next 7 days
                    if (daysLeft in 0..7) {
                        sendNotification(sub, daysLeft)
                        notificationCount++
                    }
                }
            }

            if (notificationCount > 0) {
                sendSummaryNotification(notificationCount)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error fetching subscriptions", e)
            return Result.retry()
        }
    }

    private fun sendNotification(subscription: Subscription, daysLeft: Int) {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 
            subscription.id.hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (daysLeft) {
            0 -> "Subscription Due Today!"
            1 -> "Upcoming: Tomorrow"
            else -> "Upcoming Due"
        }

        val message = when (daysLeft) {
            0 -> "Your ${subscription.name} payment of ${subscription.currency} ${subscription.cost} is due today."
            1 -> "Don't forget: ${subscription.name} is due tomorrow."
            else -> "${subscription.name} is due in $daysLeft days."
        }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Lower priority as requested
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_SUBSCRIPTIONS)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(subscription.id.hashCode(), builder.build())
        }
    }

    private fun sendSummaryNotification(count: Int) {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val summaryNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Subscription Reminders")
            .setContentText("You have $count payments due soon.")
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("$count subscriptions due"))
            .setPriority(NotificationCompat.PRIORITY_LOW) // Lower priority as requested
            .setGroup(GROUP_KEY_SUBSCRIPTIONS)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(SUMMARY_ID, summaryNotification)
        }
    }

    private fun createNotificationChannel() {
        val name = "Subscription Reminders"
        val descriptionText = "Daily reminders for upcoming subscriptions"
        val importance = NotificationManager.IMPORTANCE_LOW // Lower importance to hide from status bar
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableLights(false)
            enableVibration(false)
        }
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Optionally delete the old channel if it exists with high importance
        // notificationManager.deleteNotificationChannel("subscription_reminders")
        
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "subscription_reminders_v1" // Changed to ensure importance change takes effect
        const val GROUP_KEY_SUBSCRIPTIONS = "com.hora.varisankya.SUBSCRIPTION_UPDATES"
        const val SUMMARY_ID = 0
    }
}