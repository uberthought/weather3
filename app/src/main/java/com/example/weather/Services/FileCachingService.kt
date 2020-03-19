package com.example.weather.Services

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class FileCachingService   {
    private val logTag = javaClass.kotlin.simpleName

    companion object {
        var instance = FileCachingService()
    }

    fun getCachedFile(link: String?, context: Context?): MutableLiveData<String> {
        val data = MutableLiveData<String>()

        if (link != null && context != null)
            GlobalScope.launch {
                val url = URL(link)
                val fileName = url.file
                val cacheDir = context.cacheDir
                val cacheFile = File(cacheDir, fileName)

                if (!cacheFile.exists()) {
                    cacheFile.parent?.let { File(it).mkdirs() }

                    URL(link).openStream().use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                data.postValue(cacheFile.path)
                cacheFile.parent?.let {
                    File(it).listFiles { name ->
                        Log.d(logTag, "cached file $name")
                        true
                    }
                }
            }
        return data
    }

}