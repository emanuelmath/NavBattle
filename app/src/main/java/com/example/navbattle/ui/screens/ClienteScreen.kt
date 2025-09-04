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
fun BuscarPartida() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(top = 50.dp)) {
        Text("Elegir partida")
        Text("Lista de dispositivos")

        Spacer(modifier = Modifier.size(20.dp))

        Text("Te uniste a:")
        Text("{Nombre del dispositivo}")
        Text("Esperando para continuar... | Iniciando partida...")
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ClienteScreenPreview() {
    BuscarPartida()
}
