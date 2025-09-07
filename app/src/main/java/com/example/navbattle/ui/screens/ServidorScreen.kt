package com.example.navbattle.ui.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.navbattle.bluetooth.BluetoothCliente
import com.example.navbattle.bluetooth.BluetoothServidor
import com.example.navbattle.bluetooth.Mensaje
import com.example.navbattle.ui.navigation.Screen

@Composable
fun EsperarPartida(navController: NavController, nombre: String?) {
    var jugador2 by remember { mutableStateOf<String?>(null) }
    var conectado by remember { mutableStateOf(false) }

    //val context = LocalContext.current
    val adapter = BluetoothAdapter.getDefaultAdapter()

    // Iniciar servidor al entrar.
    LaunchedEffect(Unit) {
        BluetoothServidor.iniciarServidor(adapter) { socket ->
            conectado = true

            // Escuchar mensajes entrantes.
            BluetoothServidor.escucharMensajes(socket) { mensaje ->
                when (mensaje.tipo) {
                    "join" -> jugador2 = mensaje.data
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(top = 50.dp)
    ) {
        Text("Servidor")
        Spacer(modifier = Modifier.size(15.dp))

        Text("Jugador 1: ${nombre ?: "Player"} (tú)")
        Text("Jugador 2: ${jugador2 ?: "Esperando contrincante..."}")

        Spacer(modifier = Modifier.size(15.dp))

        Button(
            onClick = {
                // Avisar al cliente que inicie juego.
                BluetoothServidor.enviarMensaje(Mensaje(tipo = "iniciar", data = "iniciar"))
                navController.navigate(Screen.Juego.juegoDelUsuario(nombre, true))
            },
            enabled = conectado && jugador2 != null
        ) {
            Text(if (conectado && jugador2 != null) "Jugar" else "Esperando jugador...")
        }
    }
}

@Composable
fun EsperarPartidaPreview() {
    var conectado: Boolean = true
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(top = 50.dp)
    ) {
        Text("Servidor")
        Spacer(modifier = Modifier.size(15.dp))

        Text("Jugador 1: Player1 (tú)")
        Text("Jugador 2: Player2") //"Esperando contrincante..."}")

        Spacer(modifier = Modifier.size(15.dp))

        Button(
            onClick = {
            },
            enabled = conectado //!conectado
        ) {
            Text(if (conectado) "Jugar" else "Esperando jugador...")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ServidorScreenPreview() {
    EsperarPartidaPreview()
}