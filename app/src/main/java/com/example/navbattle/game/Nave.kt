package com.example.navbattle.game

import androidx.compose.ui.geometry.Offset

data class Nave(
    val name: String = "Player",
    var position: Offset,
    var lives: Int = 3,
    var bulletsAvailable: Int = 3,
    var lastShotTime: Long = 0L,
) {
}

