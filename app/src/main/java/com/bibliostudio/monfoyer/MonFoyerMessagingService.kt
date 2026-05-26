package com.bibliostudio.monfoyer

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MonFoyerMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        updateToken(FirebaseFirestore.getInstance(), uid, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Mon Foyer"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        ReminderReceiver.showNow(
            applicationContext,
            (message.messageId?.hashCode() ?: System.currentTimeMillis().toInt()),
            title,
            body
        )
    }

    companion object {
        fun updateToken(db: FirebaseFirestore, uid: String) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                updateToken(db, uid, token)
            }
        }

        fun updateToken(db: FirebaseFirestore, uid: String, token: String) {
            db.collection("users").document(uid)
                .set(
                    mapOf(
                        "fcmToken" to token,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
        }
    }
}
