package com.ngstudio.mazebrush.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ngstudio.mazebrush.R
import com.ngstudio.mazebrush.notifications.Notifications

/**
 * Receives FCM pushes.
 *
 * - App in the background + a "notification" message: Android shows it for us, using the default
 *   icon, colour and channel declared in the manifest. This method is not called.
 * - App in the foreground, or a data-only message (keys "title" and "body"): this method is called
 *   and posts the notification itself, so it looks the same either way.
 */
class PushService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        if (title.isNullOrBlank() && body.isNullOrBlank()) return
        Notifications.show(
            this,
            id = (message.messageId ?: "${System.currentTimeMillis()}").hashCode(),
            channel = Notifications.CHANNEL_NEWS,
            title = title.orEmpty().ifBlank { getString(R.string.app_name) },
            body = body.orEmpty(),
            source = "push",
        )
    }

    override fun onNewToken(token: String) {
        // Topic subscriptions carry over to the new token; there is no server to tell.
        Log.i(Push.TAG, "FCM token refreshed")
    }
}
