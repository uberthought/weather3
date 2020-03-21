package com.companyname.weather.services.nws

import com.google.gson.annotations.SerializedName

class NWSPoints {

    data class Root (
//        val @context : List<String>,
        val id : String,
        val type : String,
        val geometry : Geometry,
        val properties : Properties
    )

    data class Geometry (
        val type : String,
        val coordinates : List<Double>
    )

    data class Properties (
        @SerializedName("@id") val id : String,
        @SerializedName("@type") val type : String,
        val cwa : String,
        val forecastOffice : String,
        val gridX : Int,
        val gridY : Int,
        val forecast : String,
        val forecastHourly : String,
        val forecastGridData : String,
        val observationStations : String,
        val relativeLocation : RelativeLocation,
        val forecastZone : String,
        val county : String,
        val fireWeatherZone : String,
        val timeZone : String,
        val radarStation : String
    )

    data class RelativeLocation (
        val type : String,
        val geometry : Geometry,
        val properties : Properties
    )

//    data class Bearing (
//        val value : Double?,
//        val unitCode : String
//    )
//
//    data class Distance (
//        val value : Double?,
//        val unitCode : String
//    )
}