package com.dodhi.worker

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.dodhi.MainActivity
import com.dodhi.R
import com.dodhi.data.DodhiDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val database = DodhiDatabase.getDatabase(applicationContext)
        val dao = database.dodhiDao()

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Get records for today
        val records = try {
            dao.getRecordsInPeriod(todayStart, todayEnd).first()
        } catch (e: Exception) {
            emptyList()
        }

        // If no records have been filled, trigger the charming notification
        if (records.isEmpty()) {
            val titles = applicationContext.resources.getStringArray(R.array.reminder_titles)
            val messages = applicationContext.resources.getStringArray(R.array.reminder_messages)
            
            if (titles.isNotEmpty() && messages.isNotEmpty()) {
                val randomIndex = (titles.indices).random()
                val title = titles[randomIndex]
                val message = messages[randomIndex]
                
                showNotification(title, message)
            } else {
                showNotification("ہزاروں میں ایک! ✨", "کھاتہ درج کرنا نہ بھولیں۔")
            }
        }

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "dodhi_daily_reminders"
        
        // Intent to launch MainActivity when notification is clicked
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Dodhi Daily Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminds to enter daily milk records at 5 PM if forgotten"
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250, 250, 250) // charming multi-tap pattern
                
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(com.dodhi.R.mipmap.ic_launcher) // standard app icon
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 250, 250, 250, 250))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
            
        notificationManager.notify(101, notification)
    }
}
