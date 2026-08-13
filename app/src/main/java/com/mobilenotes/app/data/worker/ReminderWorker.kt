package com.mobilenotes.app.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mobilenotes.app.data.local.dao.NoteDao
import com.mobilenotes.app.data.local.entity.NoteEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that checks for notes whose reminder time has passed and
 * shows a notification for each, then clears the reminder so it is not repeated.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val noteDao: NoteDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val due = noteDao.getDueReminders(now).let { flow ->
            // Collect the latest emission synchronously.
            kotlinx.coroutines.flow.first(flow)
        }
        for (noteWithTags in due) {
            showNotification(noteWithTags.note)
            // Clear the reminder so it fires only once.
            noteDao.updateNote(
                noteWithTags.note.copy(reminderTimestamp = null)
            )
        }
        return Result.success()
    }

    private fun showNotification(note: NoteEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(note.title.ifBlank { "Reminder" })
            .setContentText(note.content.take(160))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(note.id.hashCode(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val WORK_NAME = "reminder_periodic"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Note reminders"
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }
    }
}
