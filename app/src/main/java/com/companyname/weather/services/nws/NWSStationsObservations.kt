package com.companyname.weather.services.nws

import com.google.gson.annotations.SerializedName

class NWSStationsObservations {
    data class Root (
//        val @context : List<String>,
        val id : String,
        val type : String,
        val geometry : Geometry,
        val properties : Properties
    )

    data class Properties (
        @SerializedName("@id") val id : String,
        @SerializedName("@type") val type : String,
        val elevation : Elevation,
        val station : String,
        val timestamp : String,
        val rawMessage : String,
        val textDescription : String,
        val icon : String,
        val presentWeather : List<String>,
        val temperature : Temperature,
        val dewpoint : Dewpoint,
        val windDirection : WindDirection,
        val windSpeed : WindSpeed,
        val windGust : WindGust,
        val barometricPressure : BarometricPressure,
        val seaLevelPressure : SeaLevelPressure,
        val visibility : Visibility,
        val maxTemperatureLast24Hours : MaxTemperatureLast24Hours,
        val minTemperatureLast24Hours : MinTemperatureLast24Hours,
        val precipitationLastHour : PrecipitationLastHour,
        val precipitationLast3Hours : PrecipitationLast3Hours,
        val precipitationLast6Hours : PrecipitationLast6Hours,
        val relativeHumidity : RelativeHumidity,
        val windChill : WindChill,
        val heatIndex : HeatIndex,
        val cloudLayers : List<CloudLayers>
    )

    data class RelativeHumidity (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class WindChill (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class MinTemperatureLast24Hours (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class MaxTemperatureLast24Hours (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class Visibility (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class PrecipitationLast3Hours (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class PrecipitationLastHour (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class PrecipitationLast6Hours (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class Temperature (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class WindDirection (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class SeaLevelPressure (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class HeatIndex (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class Geometry (
        val type : String,
        val coordinates : List<Double>
    )

    data class Elevation (
        val value : Double?,
        val unitCode : String
    )

    data class Dewpoint (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class CloudLayers (
        val base : Base,
        val amount : String
    )

    data class Base (
        val value : Double?,
        val unitCode : String
    )

    data class BarometricPressure (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class WindSpeed (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )

    data class WindGust (
        val value : Double?,
        val unitCode : String,
        val qualityControl : String
    )
}