package com.example.navbattle.game

data class GameState (
    val player: Nave,
    val enemy: Nave,
    val bullets: List<Bala> = listOf(),
    var isGameOver: Boolean = false,
    var winner: String? = null
)




