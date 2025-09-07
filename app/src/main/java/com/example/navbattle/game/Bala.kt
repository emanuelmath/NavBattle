package com.example.navbattle.game

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset

data class Bala(
    var position: Offset,
    val velocity: Offset,
    val isMine: Boolean
    ) {
}