package com.companyname.weather.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.companyname.weather.R.*

class DetailedConditionsViewModel : ViewModel() {
    data class Details (
        // general
        val title: String? = null,
        val shortDescription: String? = null,
        val detailedDescription: String? = null,
        val temperatureLabel: String? = null,
        val temperature: String? = null,
        val wind: String? = null,
        val icon: String? = null,
        val backgroundColor: Int = color.primaryColor,

        // conditions
        val dewPoint: String? = null,
        val relativeHumidity: String? = null,
        val windGust: String? = null,
        val visibility: String? = null,
        val pressure: String? = null,
        val windChill: String? = null,
        val heatIndex: String? = null,

        // forecast
        val temperatureTrend: String? = null
    )

    val details: MutableLiveData<Details> = MutableLiveData(Details())
}