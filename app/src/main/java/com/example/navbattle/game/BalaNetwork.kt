package com.example.navbattle.game

data class BalaNetwork(
    val offsetX: Float,
    val offsetY: Float,
    val velX: Float,
    val velY: Float,
    val senderIsServer: Boolean,
    val isTriple: Boolean = false,
    val tripleOffsets: List<Float>?
)
