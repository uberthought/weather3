package com.companyname.weather.services.nws

import com.google.gson.annotations.SerializedName

// https://www.json
// 2kotlin.com/

class NWSPointsStations {
	data class Root(
//		@SerializedName("@context") val context: List<String>,
		val type: String,
		val features: List<Features>,
		val observationStations: List<String>
	)

	data class Features(
		val id: String,
		val type: String,
		val geometry: Geometry,
		val properties: Properties
	)

	data class Geometry(
		val type: String,
		val coordinates: List<Double>
	)

	data class Properties(
		@SerializedName("@id") val id : String,
		@SerializedName("@type") val type : String,
		val elevation: Elevation,
		val stationIdentifier: String,
		val name: String,
		val timeZone: String
	)

	data class Elevation(
		val value: Double,
		val unitCode: String
	)
}