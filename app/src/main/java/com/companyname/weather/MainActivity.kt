package com.companyname.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.storage.StorageManager
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.*
import com.crashlytics.android.Crashlytics
import com.companyname.weather.services.LogService
import com.companyname.weather.services.NWSService
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    companion object {
        const val LOCATION_REQUEST: Int = 1
    }

    private lateinit var fusedLocationClient:FusedLocationProviderClient
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LogService.update(this)

        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_forecast, R.id.nav_map, R.id.nav_settings), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

//        addCrashButton()
        logDeviceInfo()
        requestLocationUpdates()
        requestRefresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @SuppressLint("SetTextI18n")
    private fun addCrashButton() {
        val crashButton = Button(this)
        crashButton.text = "Crash!"
        crashButton.setOnClickListener {
            Crashlytics.getInstance().crash() // Force a crash
        }

        addContentView(
            crashButton, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun requestRefresh() {
        val constrains = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        LogService().addData("refresh_forecast", "interval ${RefreshService.refreshInterval} minutes")
        val refreshWorkRequest = PeriodicWorkRequest.Builder(RefreshService::class.java, RefreshService.refreshInterval, TimeUnit.MINUTES)
            .setConstraints(constrains)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(refreshWorkRequest)
    }

    private fun logDeviceInfo() {
        val logService = LogService()
        with(DisplayMetrics()) {
            windowManager.defaultDisplay.getMetrics(this)
            val width = this.widthPixels / this.density
            val height = this.heightPixels / this.density
            logService.addData("screen_width", "${width}dp")
            logService.addData("screen_height", "${height}dp")
        }
        with(
            when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                Configuration.SCREENLAYOUT_SIZE_XLARGE -> "xlarge"
                Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
                Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
                Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
                Configuration.SCREENLAYOUT_SIZE_UNDEFINED -> "undefined"
                else -> "unknown"
            }
        ) {
            logService.addData("screen_size", this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            with(getSystemService(StorageManager::class.java) as StorageManager) {
                val quota = getCacheQuotaBytes(getUuidForPath(applicationContext.cacheDir)) / 1024 / 1024
                val allocated = getAllocatableBytes(getUuidForPath(applicationContext.cacheDir)) / 1024 / 1024
                logService.addData("cache_quota", "${quota}mb")
                logService.addData("cache_allocated", "${allocated}mb")
            }
    }

    private fun requestLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        val granularity = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
//        val granularity = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        val priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
//        val priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val interval: Long = 1000 * 60 * 15
        val fastestInterval: Long = 1000 * 60 * 5

        val logService = LogService()
        with(granularity.map {
            when(it) {
                Manifest.permission.ACCESS_FINE_LOCATION -> "fine_location"
                Manifest.permission.ACCESS_COARSE_LOCATION-> "course_location"
                else -> it
            }
        }) { logService.addData("location_granularity", "$this") }

        with(when(priority) {
            LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> "balanced accuracy"
            LocationRequest.PRIORITY_HIGH_ACCURACY -> "high accuracy"
            else -> "$priority"
        }) { logService.addData("location_priority", this) }

        logService.addData("location_refresh", "${interval/1000/60} minutes")

        if (granularity.any { l -> ActivityCompat.checkSelfPermission(applicationContext, l) != PackageManager.PERMISSION_GRANTED })
            ActivityCompat.requestPermissions(this, granularity, LOCATION_REQUEST)
        else {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                    location -> location?.let { NWSService.instance.setLocation(location) }
            }

            val locationRequest = LocationRequest.create()?.apply {
                this.interval = interval
                this.fastestInterval = fastestInterval
                this.priority = priority
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult?.let {
                        LogService().addData("location", "${it.locations[0].latitude},${it.locations[0].longitude}")
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
                LogService().addData("location", "got user permission")
                requestLocationUpdates()
            }
            else ->  LogService().addData("location", "failed to get user permission")
        }
    }

    class RefreshService(appContext: Context, workerParams: WorkerParameters) : ListenableWorker(appContext, workerParams) {
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
                            LogService().addData("refresh_forecast", " after $duration seconds")
                            GlobalScope.launch { NWSService.instance.refresh() }
                        } else
                            LogService().addData("refresh_forecast", " after $duration seconds (too soon)")

                        resolver.set(Result.success())
                    }
                }
            }
        }
    }
}
