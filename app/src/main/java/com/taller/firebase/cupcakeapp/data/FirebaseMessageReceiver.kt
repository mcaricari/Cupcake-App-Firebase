package com.taller.firebase.cupcakeapp.data

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService

class FirebaseMessageReceiver : FirebaseMessagingService()  {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
    }
    private companion object {
        const val TAG = "FirebaseMessageReceiver"
    }
}