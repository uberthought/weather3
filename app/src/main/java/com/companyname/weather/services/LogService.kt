package com.companyname.weather.services

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

    var bundle = Bundle()

    fun addMessage(message:String) {
        val logTag = Thread.currentThread().stackTrace[3].fileName.split('.')[0]
        Log.d(logTag, message)
    }

    fun addData(name:String, value:String) {
        val logTag = Thread.currentThread().stackTrace[3].fileName.split('.')[0]
        Log.d(logTag, "$name = $value")
    }

    fun addEvent(name:String, message:String) {
        val logTag = Thread.currentThread().stackTrace[3].fileName.split('.')[0]
        Log.d(logTag, "$name = $message")

        bundle.putString(name, message)
    }

    protected fun finalize() {
        if (bundle.size() == 0) return

        firebaseAnalytics?.let {
            it.logEvent("log_d", bundle)
            bundle.clear()
        }
    }
}