package com.example.navbattle.game

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset

data class NaveAccelerometerListener(
    val gameState: MutableState<GameState>
    ) : SensorEventListener {

    override fun onSensorChanged(event: SensorEvent?) {
        val veloNave = 0.8f
        event?.let {
            val x = it.values[0] * veloNave
            val y = it.values[1] * veloNave
            val state = gameState.value
            val currentPos = state.player.position
            val newX = (currentPos.x - x * 0.02f).coerceIn(0f, 1f)
            val newY = (currentPos.y + y * 0.02f).coerceIn(0f, 1f)
            gameState.value = state.copy(
                player = state.player.copy(position = Offset(newX, newY))
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

}