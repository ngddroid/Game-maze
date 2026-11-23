package com.ngstudio.maze.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

/** Firebase Cloud Messaging setup. */
object Push {
    /** Every install joins this topic, so the Firebase console can broadcast to all players. */
    const val TOPIC_ALL = "all"
    const val TAG = "MazePush"

    fun init(debug: Boolean) {
        runCatching {
            val fcm = FirebaseMessaging.getInstance()
            fcm.isAutoInitEnabled = true
            fcm.subscribeToTopic(TOPIC_ALL)
            if (debug) {
                // Debug builds print the token so you can target this device from
                // Firebase console > Messaging > "Send test message".
                fcm.token.addOnSuccessListener { Log.i(TAG, "FCM token: $it") }
            }
        }
    }
}
