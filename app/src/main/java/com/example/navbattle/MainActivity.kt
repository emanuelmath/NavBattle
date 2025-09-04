package com.example.navbattle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.navbattle.ui.navigation.NavBattleNavHost
import com.example.navbattle.ui.navigation.Screen
import com.example.navbattle.ui.theme.NavBattleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NavBattleTheme {
                val navController = rememberNavController()
                NavBattleNavHost(
                    navHostController = navController,
                    startDestination = Screen.MenuDelJuego.ruta
                )
            }
        }
    }
}

@Composable
fun MenuDelJuego(navController: NavController){
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(50.dp)) {
        Text("NavBattle", fontWeight = FontWeight.ExtraBold)

        Spacer(Modifier.size(15.dp))

        Text("Jugar como:")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                navController.navigate(Screen.Servidor.ruta)
            }) {
                Text("Servidor")
            }
            Button(onClick = {
                navController.navigate(Screen.Cliente.ruta)
            }) {
                Text("Cliente")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    val navController = rememberNavController()
    NavBattleTheme {
        MenuDelJuego(navController)
    }
}