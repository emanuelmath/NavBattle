package com.example.navbattle.ui.screens

import android.bluetooth.BluetoothAdapter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.navbattle.R
import com.example.navbattle.bluetooth.BluetoothServidor
import com.example.navbattle.bluetooth.Mensaje
import com.example.navbattle.ui.navigation.Screen


@Composable
fun EsperarPartida(navController: NavController, nombre: String?) {
    var jugador2 by remember { mutableStateOf<String?>(null) }
    var conectado by remember { mutableStateOf(false) }
    val adapter = BluetoothAdapter.getDefaultAdapter()

    // Iniciar servidor.
    LaunchedEffect(Unit) {
        BluetoothServidor.iniciarServidor(adapter) { socket ->
            conectado = true

            BluetoothServidor.escucharMensajes(socket) { mensaje ->
                when (mensaje.tipo) {
                    "join" -> {
                        jugador2 = mensaje.data

                    }
                }
            }
        }
    }

   Scaffold(containerColor = Color(0xFF738AF2)) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 50.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.mando),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
            Text("Servidor",
                fontSize = 35.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.size(15.dp))

            Text("Jugador 1: ${nombre ?: "Player"} (tú)",
                color = Color.White,
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.size(10.dp))
            Text("Jugador 2: ${jugador2 ?: "Esperando contrincante..."}",
                color = Color.White,
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.size(15.dp))

            Button(
                onClick = {
                    BluetoothServidor.enviarMensaje(Mensaje(tipo = "iniciar", data = "iniciar"))
                    navController.navigate(Screen.Juego.juegoDelUsuario(nombre, true))
                },
                enabled = conectado && jugador2 != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB8123F)
                )
            ) {
                Text(if (conectado && jugador2 != null) "Jugar" else "Esperando jugador...",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun EsperarPartidaPreview() {
    val conectado: Boolean = true
    Scaffold(containerColor = Color(0xFF738AF2)) {
        innerPadding ->  Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(top = 50.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.mando),
            contentDescription = null,
            modifier = Modifier.size(100.dp)
        )
        Text("Servidor",
            fontSize = 35.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold)

        Spacer(modifier = Modifier.size(15.dp))

        Text("Jugador 1: Player1 (tú)",
            color = Color.White,
            fontSize = 25.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.size(10.dp))
        Text("Jugador 2: Player2",
            color = Color.White,
            fontSize = 25.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold) //"Esperando contrincante..."}")

        Spacer(modifier = Modifier.size(15.dp))

        Button(
            onClick = {
            },
            enabled = conectado, //!conectado
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB8123F)
            )
        ) {
            Text(if (conectado) "Jugar" else "Esperando jugador...")
        }
    }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ServidorScreenPreview() {
    EsperarPartidaPreview()
}