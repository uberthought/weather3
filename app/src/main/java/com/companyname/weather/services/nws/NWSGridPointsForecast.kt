package com.companyname.weather.services.nws

class NWSGridPointsForecast {

    data class Root (
//        val @context : List<String>,
        val type : String,
        val geometry : Geometry,
        val properties : Properties
    )

    data class Geometry (
        val type : String,
        val geometries : List<Geometries>
    )

    data class Properties (
        val updated : String,
        val units : String,
        val forecastGenerator : String,
        val generatedAt : String,
        val updateTime : String,
        val validTimes : String,
        val elevation : Elevation,
        val periods : List<Periods>
    )

    data class Geometries (
        val type : String
//        val coordinates : List<Double?>
    )

    data class Elevation (
        val value : Double?,
        val unitCode : String
    )

    data class Periods (
        val number : Int,
        val name : String,
        val startTime : String,
        val endTime : String,
        val isDaytime : Boolean,
        val temperature : Double?,
        val temperatureUnit : String,
        val temperatureTrend : String,
        val windSpeed : String,
        val windDirection : String,
        val icon : String,
        val shortForecast : String,
        val detailedForecast : String
    )
}