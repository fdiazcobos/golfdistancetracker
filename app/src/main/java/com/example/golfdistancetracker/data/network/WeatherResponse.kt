package com.example.golfdistancetracker.data.network

data class WeatherResponse(
    val wind: Wind,
    val main: Main
)

data class Wind(
    val speed: Double,
    val deg: Int
)

data class Main(
    val temp: Double
)
