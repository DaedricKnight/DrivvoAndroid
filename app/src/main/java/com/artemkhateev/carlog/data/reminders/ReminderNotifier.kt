package com.artemkhateev.carlog.data.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.artemkhateev.carlog.MainActivity
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.model.Reminder
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.domain.DueStatus
import com.artemkhateev.carlog.ui.format.Formats

class ReminderNotifier(private val context: Context, private val formats: Formats) {

    /** false — уведомления запрещены: тогда срок не отмечается показанным и напомним позже. */
    fun show(vehicle: Vehicle, reminder: Reminder, title: String, status: DueStatus): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel_reminders))
                .build(),
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_reminder_title, title.ifBlank { context.getString(R.string.entry_reminder) }, vehicle.name))
            .setContentText(statusText(status))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(TAG, reminder.id.toInt(), notification)
        return true
    }

    private fun statusText(status: DueStatus): String {
        val resources = context.resources
        return buildList {
            status.remainingDistance?.let { distance ->
                add(
                    when {
                        distance > 0 -> resources.getString(R.string.due_in_distance, formats.distance(distance))
                        distance == 0L -> resources.getString(R.string.due_now)
                        else -> resources.getString(R.string.due_distance_ago, formats.distance(-distance))
                    },
                )
            }
            status.remainingDays?.let { days ->
                add(
                    when {
                        days > 0 -> resources.getQuantityString(R.plurals.due_in_days, days.toInt(), days.toInt())
                        days == 0L -> resources.getString(R.string.due_today)
                        else -> resources.getQuantityString(R.plurals.due_days_ago, (-days).toInt(), (-days).toInt())
                    },
                )
            }
        }.joinToString(" · ")
    }

    private companion object {
        const val CHANNEL_ID = "reminders"
        const val TAG = "reminder"
    }
}
