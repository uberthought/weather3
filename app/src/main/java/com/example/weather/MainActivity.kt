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
import com.example.weather.Services.NWSService
import com.example.weather.databinding.MainActivityBinding
import com.google.android.gms.location.*
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {
    private val logTag = javaClass.kotlin.simpleName

    lateinit var locationViewModel: LocationViewModel
    lateinit var conditionsViewModel: ConditionsViewModel

    companion object {
        const val LOCATION_REQUEST: Int = 1
    }

    private lateinit var fusedLocationClient:FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(logTag, "onCreate")


        // debugging
        with (DisplayMetrics()) {
            val  metrics  = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val width = metrics.widthPixels/metrics.density
            val height = metrics.heightPixels/metrics.density
            Log.d(logTag, "width = ${width}dp height = ${height}dp")
        }
        with (when (resources.configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) {
            android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE -> "xlarge"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_UNDEFINED -> "undefined"
            else -> "unknown"
        }) { Log.d(logTag, "screenSize = $this") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            with (getSystemService(StorageManager::class.java) as StorageManager) {
                val quota = getCacheQuotaBytes(getUuidForPath(applicationContext.cacheDir))/1024/1024
                val allocated = getAllocatableBytes(getUuidForPath(applicationContext.cacheDir))/1024/1024
              Log.d(logTag, "cache quota = ${quota}mb allocated = ${allocated}mb")
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

        Log.d(logTag, "forecast refresh = ${RefreshService.refreshInterval} minutes")
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

        with(granularity.map {
            when(it) {
                Manifest.permission.ACCESS_FINE_LOCATION -> "fine_location"
                Manifest.permission.ACCESS_COARSE_LOCATION-> "course_location"
                else -> it
            }
        }) { Log.d(logTag, "location granularity = $this") }

        with(when(priority) {
            LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> "balanced"
            else -> "$priority"
        }) { Log.d(logTag, "location priority = $this") }

        Log.d(logTag, "location refresh = ${interval/1000/60} minutes")


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
                        Log.d(logTag, "location got location ${it.locations[0].latitude},${it.locations[0].longitude}")
                        locationResult.locations.map { NWSService.instance.setLocation(it)
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
                Log.d(logTag, "location got user permission")
                requestLocationUpdates()
            }
            else -> {
                Log.d(logTag, "location failed to get user permission")
            }
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
                            Log.d(logTag, "forecast refresh duration = $duration seconds")
                            GlobalScope.launch { NWSService.instance.refresh() }
                        } else
                            Log.d(logTag, "forecast refresh too soon duration = $duration seconds")

                        resolver.set(Result.success())
                    }
                }
            }
        }
    }
}
