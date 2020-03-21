package com.companyname.weather.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.companyname.weather.R
import com.companyname.weather.services.NWSService

class ForecastViewModel : ViewModel() {
    val forecasts: MutableLiveData<List<DetailedConditionsViewModel.Details>> = MutableLiveData()

    init { NWSService.instance.forecasts.observeForever { forecasts -> onForecastsChanged(forecasts) } }

    private fun onForecastsChanged(forecasts: List<NWSService.Forecast>) {
        this.forecasts.postValue(forecasts.map {
            DetailedConditionsViewModel.Details (
                title = it.name,
                icon = it.icon,
                shortDescription = it.shortForecast,
                detailedDescription = it.detailedForecast,
                temperatureLabel = if (it.isDaytime) "Hi " else "Low ",
                temperature = "%.0f℉".format(it.temperature),
                wind = "Wind ${it.windDirection} ${it.windSpeed}".replace("mph", "MPH"),
                temperatureTrend = if (it.temperatureTrend == null) null else "(${it.temperatureTrend})",
                backgroundColor = if (it.isDaytime) R.color.primaryColor else R.color.secondaryColor
            )
        })
    }
}