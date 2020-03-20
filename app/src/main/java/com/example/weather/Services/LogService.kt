package com.example.weather.Services

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class LogService {
    companion object {
        var firebaseAnalytics: FirebaseAnalytics? = null

        fun update(context:Context) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    var bundle = Bundle()

    fun add(tag:String, message:String) {
        bundle.putString(tag, message)
    }

    fun write() {
        if (bundle.size() == 0) return

        firebaseAnalytics?.let {
            it.logEvent("log_d", bundle)
            bundle.clear()
        }
    }

    protected fun finalize() {
        write()
    }
}