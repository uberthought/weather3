package com.example.weather.services

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

class LogService {
    companion object {
        var firebaseAnalytics: FirebaseAnalytics? = null

        fun update(context:Context) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    private val enableFirebase: Boolean = false

    var bundle = Bundle()

    fun add(tag:String, message:String) {
        val logTag = Thread.currentThread().stackTrace[3].fileName.split('.')[0]
        Log.d(logTag, "$tag = $message")

        if (enableFirebase)
            bundle.putString(tag, message)
    }

    protected fun finalize() {
        if (enableFirebase) {
            if (bundle.size() == 0) return

            firebaseAnalytics?.let {
                it.logEvent("log_d", bundle)
                bundle.clear()
            }
        }
    }
}