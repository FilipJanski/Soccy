package com.example.soccy.model

data class Player(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val height: Int = 0,
    val jerseyNumber: Int = 0,
    val dominantFoot: String = "",
    val country: String = "",
    val club: String = "",
    val position: String = "",
    val goals: Int = 0,
    val matches: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val assists: Int = 0,
    val photoUri: String? = null
)

