package com.companyname.weather.services

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.companyname.weather.services.nws.NWSGridPointsForecast
import com.companyname.weather.services.nws.NWSPoints
import com.companyname.weather.services.nws.NWSPointsStations
import com.companyname.weather.services.nws.NWSStationsObservations
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class NWSService {
    companion object {
        val instance = NWSService()

        val mutex = Mutex()
        const val refreshInterval:Long = 15
        var timestamp: Long = 0
    }

    private var stationId: String? = null
    private var gridX: Int? = null
    private var gridY: Int? = null
    private var gridWFO: String? = null
    private var lastLocation:Location? = null
    private var baseURL = "https://api.weather.gov"

    data class Conditions (
        val dewPoint: Double? = null,
        val relativeHumidity: Double? = null,
        val temperature: Double? = null,
        val windDirection: Double? = null,
        val windSpeed: Double? = null,
        val windGust: Double? = null,
        val icon:String? = null,
        val textDescription:String? = null,
        val barometricPressure:Double? = null,
        val visibility:Double? = null,
        val windChill:Double? = null,
        val heatIndex:Double? = null,
        val timestamp:Date? = null
    )

    data class Forecast (
        val name: String? = null,
        val icon: String? = null,
        val shortForecast: String? = null,
        val detailedForecast: String? = null,
        val temperature: Double? = null,
        val windSpeed: String? = null,
        val windDirection: String? = null,
        val temperatureTrend: String? = null,
        val isDaytime: Boolean = false
    )

    var location: MutableLiveData<String> = MutableLiveData()
    var conditions: MutableLiveData<Conditions> = MutableLiveData()
    var forecasts: MutableLiveData<List<Forecast>> = MutableLiveData()

    fun setLocation(location: Location) {
        val logService = LogService()

        logService.addMessage("got update from location service")
        if (lastLocation == null || lastLocation!!.distanceTo(location) > 100  ) {
            logService.addMessage("refreshing since the location changes by greater than 100m")
            logService.addEvent("nws_location", "changed")
            lastLocation = Location(location)
            stationId = null
            timestamp = 0
            GlobalScope.launch { refresh() }
        }
        else
            logService.addMessage("not refreshing since the location changes by at less than 100m")
    }

    suspend fun refresh() {
        val logService = LogService()
        logService.addMessage("starting NWS refresh")

        mutex.withLock {
            val duration = Date().time - timestamp
            if (duration > 1000 * 60 * refreshInterval) {
                timestamp = Date().time
                if ((lastLocation != null)) {
                    logService.addEvent("nws_refresh", "refreshing")
                    getStation()
                    getConditions()
                    getForecast()
                }
                else
                    logService.addMessage("skipping refresh because location isn't set")
            }
            else
                logService.addMessage("skipping refresh because refreshing too soon")
        }
    }

    private fun getStation() {
        val latitude = lastLocation!!.latitude
        val longitude = lastLocation!!.longitude
        val url = URL("$baseURL/points/$latitude,$longitude/stations")
        val response = url.readText()
        val points = Gson().fromJson(response, NWSPointsStations.Root::class.java)
        location.postValue(points.features[0].properties.name)
        stationId = points.features[0].properties.stationIdentifier
    }

    @SuppressLint("SimpleDateFormat")
    private fun getConditions() {
        val url = URL("$baseURL/stations/$stationId/observations/latest?require_qc=false")
        val response = url.readText()
        val stations = Gson().fromJson(response, NWSStationsObservations.Root::class.java)

        this.conditions.postValue(Conditions(
            dewPoint = stations.properties.dewpoint.value,
            relativeHumidity = stations.properties.relativeHumidity.value,
            temperature = stations.properties.temperature.value,
            windDirection = stations.properties.windDirection.value,
            windSpeed = stations.properties.windSpeed.value,
            windGust = stations.properties.windGust.value,
            icon = stations.properties.icon,
            textDescription = stations.properties.textDescription,
            barometricPressure = stations.properties.barometricPressure.value,
            visibility = stations.properties.visibility.value,
            windChill = stations.properties.windChill.value,
            heatIndex = stations.properties.heatIndex.value,
            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sszzzzz").parse(stations.properties.timestamp)
        ))
    }

    private fun getForecast() {
        if (listOf(gridX, gridY, gridWFO).any { it == null })
            getGridPoint()

        val url = URL("$baseURL/gridpoints/$gridWFO/$gridX,$gridY/forecast")
        val response = url.readText()
        val forecast = Gson().fromJson(response, NWSGridPointsForecast.Root::class.java)

        this.forecasts.postValue(forecast.properties.periods.map {
            Forecast(
                name = it.name,
                icon = it.icon,
                shortForecast = it.shortForecast,
                detailedForecast = it.detailedForecast,
                temperature = it.temperature,
                windSpeed = it.windSpeed,
                windDirection = it.windDirection,
                temperatureTrend = it.temperatureTrend,
                isDaytime = it.isDaytime
            )
        })
    }

    private fun getGridPoint() {
        val latitude = lastLocation!!.latitude
        val longitude = lastLocation!!.longitude
        val url = URL("$baseURL/points/$latitude,$longitude")
        val response = url.readText()
        val points = Gson().fromJson(response, NWSPoints.Root::class.java)

        gridWFO = points.properties.cwa
        gridX = points.properties.gridX
        gridY = points.properties.gridY
    }
}