
package com.example.falldetectorapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.falldetectorapp.R
import com.example.falldetectorapp.activities.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
/**
 * Serwis Firebase Messaging odpowiedzialny za odbiór wiadomości push (FCM)
 * i wyświetlanie lokalnych powiadomień użytkownikowi.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val CHANNEL_ID = "fall_alerts"
    /**
     * Metoda wywoływana przy odebraniu nowej wiadomości FCM.
     * Wyciąga dane z wiadomości i pokazuje lokalne powiadomienie.
     *
     * @param remoteMessage Obiekt reprezentujący odebraną wiadomość FCM.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Odebrano wiadomość: ${remoteMessage.data}")
        Log.d("FCM", "Notification: ${remoteMessage.notification?.title} - ${remoteMessage.notification?.body}")

        val title = remoteMessage.notification?.title ?: "Brak tytułu"
        val body = remoteMessage.notification?.body ?: "Brak treści"

        showNotification(title, body)
    }
    /**
     * Metoda wywoływana, gdy generowany jest nowy token FCM dla urządzenia.
     * Można tu np. przesłać token do backendu.
     *
     * @param token Nowy token FCM.
     */
    override fun onNewToken(token: String) {
        Log.d("FCM", "Nowy token: $token")
        // Kiedys zapis token do bazy
    }
    /**
     * Tworzy i wyświetla lokalne powiadomienie systemowe.
     *
     * @param title Tytuł powiadomienia.
     * @param message Treść powiadomienia.
     */
    private fun showNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Powiadomienia o upadkach"
            val descriptionText = "Kanał dla alertów FCM"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        with(NotificationManagerCompat.from(this)) {
            notify(1234, notification)
        }
    }
}