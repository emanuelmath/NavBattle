package com.example.navbattle.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.compose.rememberNavController
import com.example.navbattle.R
import com.example.navbattle.ui.navigation.Screen

@Composable
fun GameOver(navController: NavController, isServer: Boolean, ganador: String) {
    val medallaServidor = painterResource(R.drawable.medallaservidor)
    val medallaCliente = painterResource(R.drawable.medallacliente)
    
    Scaffold(containerColor = Color(0xFF738AF2)) { innerPadding ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().
        padding(innerPadding).padding(top = 35.dp)) {
            Image(if(isServer) medallaServidor else medallaCliente, null,
            modifier = Modifier.size(120.dp))
            Text("¡Fin del Juego!",
                color = Color.White,
                fontSize = 35.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.size(15.dp))
            Text("Terminó la guerra espacial.",
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold)

            Spacer(Modifier.size(15.dp))

            Button(onClick = {
                navController.navigate(Screen.MenuDelJuego.ruta) {
                    popUpTo(Screen.GameOver.gameOverDelUsuario(isServer, ganador)) {
                        inclusive = true
                    }
                }
            },colors = ButtonDefaults.buttonColors(
                containerColor = if(isServer) Color(0xFFB8123F) else Color(0xFF352DB6)
            )) {
                Text("Regresar al menú",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold)
            }
        }

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GameOverPreview() {
    val navController = rememberNavController()
    GameOver(navController, true, "David")
}