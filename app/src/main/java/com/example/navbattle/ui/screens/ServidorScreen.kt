package com.example.navbattle.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EsperarPartida() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(top = 50.dp)) {
        Text("Servidor")
        Spacer(modifier = Modifier.size(15.dp))

        Text("Jugador 1 (tú): {Nombre del dispositivo)")
        Text("Jugador 2: Esperando contrincante | {Nombre de dispositivo}")

        Spacer(modifier = Modifier.size(15.dp))

        Button(onClick = {

        }) {
            Text("Jugar | Esperando contricante")
        }

    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ServidorScreenPreview() {
    EsperarPartida()
}