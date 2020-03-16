package com.example.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.work.*
import com.example.weather.Services.NWSService
import com.google.android.gms.location.*
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : FragmentActivity() {
    private val logTag = javaClass.kotlin.simpleName

    companion object {
        const val LOCATION_REQUEST: Int = 1
    }

    private lateinit var fusedLocationClient:FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

//        if (savedInstanceState == null) {
//            val fragment = SideBySideFragment()
////            val fragment = SingleFragment()
//            supportFragmentManager.beginTransaction()
//                .add(R.id.mainFragment, fragment)
//                .commit()
//        }

        applicationContext?.resources?.displayMetrics?.let {
            val dpHeight = it.heightPixels / it.density
            val dpWidth = it.widthPixels / it.density

            Log.d(logTag, "dpHeight=$dpHeight, dpWidth=$dpWidth")
        }

        // location

        requestLocationUpdates()

        // refresh timer

        Log.d(logTag, "setup constraints so we don't hammer the device")
        val constrains = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

        Log.d(logTag, "request refresh every ${RefreshService.refreshInterval} minutes")
        val refreshWorkRequest = PeriodicWorkRequest.Builder(RefreshService::class.java, RefreshService.refreshInterval, TimeUnit.MINUTES)
                .setConstraints(constrains)
                .build()
        WorkManager.getInstance(applicationContext).enqueue(refreshWorkRequest)
    }

    fun requestLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        val fineLocation = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ActivityCompat.checkSelfPermission(this, fineLocation) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, coarseLocation) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(fineLocation, coarseLocation), LOCATION_REQUEST)
        }
        else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location -> location?.let { NWSService.instance.setLocation(location) } }

            val locationRequest = LocationRequest.create()?.apply {
                interval = 1000 * 60 * 15
                fastestInterval = 1000 * 60 * 5
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult?.let { locationResult.locations.map { NWSService.instance.setLocation(it) } }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper() )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_REQUEST -> {
                Log.d(logTag, "got user permission")
                requestLocationUpdates()
            }
            else -> {
                Log.d(logTag, "failed to get user permission")
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
                        val duration = Date().time - timestamp
                        if (duration > 1000 * 60 * 5) {
                            timestamp = Date().time
                            Log.d(logTag, "refresh NWS after $duration")
                            GlobalScope.launch { NWSService.instance.refresh() }
                        } else
                            Log.d(logTag, "trying to refresh NWS too soon $duration")

                        resolver.set(Result.success())
                    }
                }
            }
        }
    }
}
