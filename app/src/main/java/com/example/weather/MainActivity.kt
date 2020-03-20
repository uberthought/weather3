package com.example.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.storage.StorageManager
import android.util.DisplayMetrics
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.example.weather.Services.LogService
import com.example.weather.Services.NWSService
import com.example.weather.databinding.MainActivityBinding
import com.google.android.gms.location.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {
    private val logTag = javaClass.kotlin.simpleName!!

    lateinit var locationViewModel: LocationViewModel
    lateinit var conditionsViewModel: ConditionsViewModel

    companion object {
        const val LOCATION_REQUEST: Int = 1
    }

    private lateinit var fusedLocationClient:FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LogService.update(this)

        // debugging
        val logService = LogService()
        with (DisplayMetrics()) {
            windowManager.defaultDisplay.getMetrics(this)
            val width = this.widthPixels/this.density
            val height = this.heightPixels/this.density
            logService.add("screen_width",  "${width}dp")
            logService.add("screen_height",  "${height}dp")
        }
        with (when (resources.configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) {
            android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE -> "xlarge"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_UNDEFINED -> "undefined"
            else -> "unknown"
        }) {
            logService.add("screen_size", this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            with (getSystemService(StorageManager::class.java) as StorageManager) {
                val quota = getCacheQuotaBytes(getUuidForPath(applicationContext.cacheDir))/1024/1024
                val allocated = getAllocatableBytes(getUuidForPath(applicationContext.cacheDir))/1024/1024
                logService.add("cache_quota", "${quota}mb")
                logService.add("cache_allocated", "${allocated}mb")
            }

        val binding = DataBindingUtil.setContentView<MainActivityBinding>(this, R.layout.main_activity)

        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]
        conditionsViewModel = ViewModelProvider(this)[ConditionsViewModel::class.java]
        binding.lifecycleOwner = this
        binding.locationViewModel = locationViewModel
        binding.conditionsViewModel = conditionsViewModel

        locationViewModel.location.observe(this, androidx.lifecycle.Observer { location.invalidate() })
        conditionsViewModel.details.observe(this, androidx.lifecycle.Observer { timestamp.invalidate() })

        // location

        requestLocationUpdates()

        // refresh timer

        val constrains = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

        logService.add("refresh_forecast", "interval ${RefreshService.refreshInterval} minutes")
        val refreshWorkRequest = PeriodicWorkRequest.Builder(RefreshService::class.java, RefreshService.refreshInterval, TimeUnit.MINUTES)
            .setConstraints(constrains)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(refreshWorkRequest)
    }

    private fun requestLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

//        val granularity = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val granularity = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        val priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        val interval: Long = 1000 * 60 * 15
        val fastestInterval: Long = 1000 * 60 * 5

        val logService = LogService()
        with(granularity.map {
            when(it) {
                Manifest.permission.ACCESS_FINE_LOCATION -> "fine_location"
                Manifest.permission.ACCESS_COARSE_LOCATION-> "course_location"
                else -> it
            }
        }) { logService.add("location_granularity", "$this") }

        with(when(priority) {
            LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> "balanced"
            else -> "$priority"
        }) { logService.add("location_priority", this) }

        logService.add("location_refresh", "${interval/1000/60} minutes")


        if (granularity.any { l -> ActivityCompat.checkSelfPermission(applicationContext, l) != PackageManager.PERMISSION_GRANTED })
            ActivityCompat.requestPermissions(this, granularity, LOCATION_REQUEST)
        else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location -> location?.let { NWSService.instance.setLocation(location) } }

            val locationRequest = LocationRequest.create()?.apply {
                this.interval = interval
                this.fastestInterval = fastestInterval
                this.priority = priority
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult?.let {
                        LogService().add("location", "${it.locations[0].latitude},${it.locations[0].longitude}")
                        locationResult.locations.map { location -> NWSService.instance.setLocation(location)
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper() )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_REQUEST -> {
                LogService().add("location", "got user permission")
                requestLocationUpdates()
            }
            else ->  LogService().add("location", "failed to get user permission")
        }
    }

    class RefreshService(appContext: Context, workerParams: WorkerParameters) : ListenableWorker(appContext, workerParams) {
        private val logTag = javaClass.kotlin.simpleName

        companion object {
            val mutex = Mutex()
            var refreshInterval:Long = 15
            var timestamp: Long = 0
        }

        override fun startWork(): ListenableFuture<Result> {
            return CallbackToFutureAdapter.getFuture { resolver ->
                GlobalScope.launch {
                    mutex.withLock {
                        val duration = (Date().time - timestamp) / 1000.0
                        if (duration > 60 * 5) {
                            timestamp = Date().time
                            LogService().add("refresh_forecast", " after $duration seconds")
                            GlobalScope.launch { NWSService.instance.refresh() }
                        } else
                            LogService().add("refresh_forecast", " after $duration seconds (too soon)")

                        resolver.set(Result.success())
                    }
                }
            }
        }
    }
}
