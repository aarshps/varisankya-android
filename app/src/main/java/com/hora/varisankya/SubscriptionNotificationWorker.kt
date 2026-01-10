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
import com.hora.varisankya.receiver.NotificationActionReceiver
import com.hora.varisankya.widget.UpcomingWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.ComponentName

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
            // Fetch only active subscriptions to avoid unnecessary processing
            val snapshots = firestore.collection("users")
                .document(userId)
                .collection("subscriptions")
                .whereEqualTo("active", true)
                .get()
                .await()

            val subscriptions = snapshots.toObjects(Subscription::class.java)
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            
            // Get user preference for notification window
            val notificationWindow = PreferenceHelper.getNotificationDays(applicationContext)

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

                    // Notify if due today or in the next N days (user preference)
                    if (daysLeft in 0..notificationWindow) {
                        sendNotification(sub, daysLeft)
                    }
                }

            }

            // --- Widget Update Logic ---
            com.hora.varisankya.widget.WidgetUpdateHelper.updateWidgetData(applicationContext, subscriptions)
            // ---------------------------

            return Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error fetching subscriptions", e)
            return Result.retry()
        }
    }

    private fun sendNotification(subscription: Subscription, daysLeft: Int) {
        val context = applicationContext
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // Use a unique but stable ID for the Notification and PendingIntent based on subscription ID
        val notifId = subscription.id?.hashCode() ?: return
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            notifId, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when (daysLeft) {
            0 -> "Due Today"
            1 -> "Due Tomorrow"
            else -> "Due in $daysLeft days"
        }

        val message = "${subscription.name}: ${subscription.currency} ${subscription.cost}"

        val markPaidIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_PAID
            putExtra(NotificationActionReceiver.EXTRA_SUB_ID, subscription.id)
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val markPaidPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId,
            markPaidIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Discrete foreground icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Keeps it out of the status bar
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_check, "Mark Paid", markPaidPendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_SUBSCRIPTIONS)
            // No summary notification here to prevent perceived duplicates in the drawer

        with(NotificationManagerCompat.from(context)) {
            // notify with the same ID will overwrite any existing notification for this subscription
            notify(notifId, builder.build())
        }
    }

    private fun createNotificationChannel() {
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Clean up legacy channels to prevent overlapping notifications or conflicting importance settings
        notificationManager.deleteNotificationChannel("subscription_reminders")
        notificationManager.deleteNotificationChannel("subscription_reminders_v1")
        
        val name = "Subscription Reminders"
        val descriptionText = "Daily reminders for upcoming subscriptions"
        val importance = NotificationManager.IMPORTANCE_LOW // Strict adherence to M3E for non-critical alerts
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableLights(false)
            enableVibration(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "subscription_reminders_v2" // Versioned channel for clean state
        const val GROUP_KEY_SUBSCRIPTIONS = "com.hora.varisankya.SUBSCRIPTIONS"
    }
}