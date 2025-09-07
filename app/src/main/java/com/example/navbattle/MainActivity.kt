package com.example.navbattle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
        var puedeJugar by remember { mutableStateOf(false)}
        var permisosLanzados by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val permissionsToRequest = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }

        val allPermissionsGranted = permissionsToRequest.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }

        var showGrantedToast by remember { mutableStateOf(false) }
        var showDeniedToast by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.values.all { it }
            if (granted) {
                showGrantedToast = true
                puedeJugar = true
            } else {
                showDeniedToast = true
            }
        }
        var nombre by remember { mutableStateOf("") }
        var mensaje by remember { mutableStateOf("") }
        var mensajePermisos by remember { mutableStateOf("") }


        Text("NavBattle", fontWeight = FontWeight.ExtraBold)

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Ingresa tu nombre") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.size(15.dp))

        Text("Jugar como:")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                if(nombre.trim().isBlank()) {
                    mensaje = "Debes ingresar tu nombre."
                } else {
                    navController.navigate(Screen.Servidor.servidorUsuario(nombre))
                }
            },
                enabled = puedeJugar) {
                Text("Servidor")
            }
            Spacer(Modifier.size(10.dp))
            Button(onClick = {
                if(nombre.trim().isBlank()) {
                    mensaje = "Debes ingresar tu nombre."
                } else {
                navController.navigate(Screen.Cliente.clienteUsuario(nombre))
                }
            },
                enabled = puedeJugar) {
                Text("Cliente")
            }

            Spacer(modifier = Modifier.size(10.dp))

        }

        LaunchedEffect(puedeJugar) {
            mensajePermisos = if (puedeJugar) {
                "¡Disfruta de tu partida!"
            } else {
                "Necesitas permitir el uso de bluetooth para jugar."
            }
        }

        Text(mensaje)
        Spacer(Modifier.size(10.dp))
        Text(mensajePermisos)




        LaunchedEffect(allPermissionsGranted) {
            if (!allPermissionsGranted && !permisosLanzados) {
                permisosLanzados = true
                permissionLauncher.launch(permissionsToRequest)
            } else if (allPermissionsGranted) {
                puedeJugar = true
            }
        }

        LaunchedEffect(showGrantedToast) {
            if (showGrantedToast) {
                Toast.makeText(context, "Permisos concedidos.", Toast.LENGTH_SHORT).show()
                showGrantedToast = false
                puedeJugar = true
            }
        }

        LaunchedEffect(showDeniedToast) {
            if (showDeniedToast) {
                Toast.makeText(context, "Se requieren permisos Bluetooth.", Toast.LENGTH_LONG).show()
                showDeniedToast = false
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