package com.companyname.weather.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.companyname.weather.services.NWSService
import java.text.SimpleDateFormat

class ConditionsViewModel : ViewModel() {
    val timestamp: MutableLiveData<String> = MutableLiveData()
    val details: MutableLiveData<DetailedConditionsViewModel.Details> = MutableLiveData()

    init { NWSService.instance.conditions.observeForever { conditions -> onConditionsChanged(conditions) } }

    private fun onConditionsChanged(conditions: NWSService.Conditions) {
        this.details.postValue(
            DetailedConditionsViewModel.Details(
            title = "Current Conditions",
            dewPoint = conditions.dewPoint?.let { "Dew Point %.0f℉".format(conditions.dewPoint.toFahrenheit) },
            relativeHumidity = conditions.relativeHumidity?.let { "Humidity %.0f%%".format(conditions.relativeHumidity) },
            temperature = conditions.temperature?.let { "%.0f℉".format(conditions.temperature.toFahrenheit) },
            wind = getWind(conditions),
            windGust = conditions.windGust?.let { "Gusts %.1f MPH".format(conditions.windGust.toMPH) },
            icon = conditions.icon,
            shortDescription = conditions.textDescription,
            visibility = conditions.visibility?.let { "Visibility %.1f  Miles".format(conditions.visibility.toMiles) },
            pressure = conditions.barometricPressure?.let { "Pressure %.2f HG".format(conditions.barometricPressure.toHG) },
            windChill = conditions.windChill?.let { "Wind Chill %.0f℉".format(conditions.windChill.toFahrenheit) },
            heatIndex = conditions.heatIndex?.let { "Heat Index %.0f℉".format(conditions.heatIndex.toFahrenheit) }
        ))
        this.timestamp.postValue(conditions.timestamp?.let { "Update ${SimpleDateFormat.getDateTimeInstance().format(conditions.timestamp)}" })
    }

    private val Double.toFahrenheit: Double get() = this * 9.0 / 5.0 + 32.0
    private val Double.toMPH: Double get() = this * 2.237
    private val Double.toMiles: Double get() = this / 1609.0
    private val Double.toHG: Double get() = this / 3386.0

    private fun getWind(conditions: NWSService.Conditions):String? {
        conditions.windDirection ?: return null
        conditions.windSpeed ?: return null

        val windDirection = when {
            conditions.windDirection > 337.5 || conditions.windDirection <= 22.5 -> "N"
            conditions.windDirection > 22.5 && conditions.windDirection <= 67.5 -> "NE"
            conditions.windDirection > 67.5 && conditions.windDirection <= 112.5 -> "E"
            conditions.windDirection > 112.5 && conditions.windDirection <= 157.5 -> "SE"
            conditions.windDirection > 157.5 && conditions.windDirection <= 202.5 -> "S"
            conditions.windDirection > 202.5 && conditions.windDirection <= 247.5 -> "SW"
            conditions.windDirection > 247.5 && conditions.windDirection <= 292.5 -> "W"
            conditions.windDirection > 292.5 && conditions.windDirection <= 337.5 -> "NW"
            else -> ""
        }

        return "Wind $windDirection %.1f MPH".format(conditions.windSpeed.toMPH)
    }
}
