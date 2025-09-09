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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults.colors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    mensajePermisos,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        },
        containerColor = Color(0xFF738AF2)
    ) { innerPadding ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(innerPadding)
            .padding(50.dp)) {

            Text("NavBattle",
                fontSize = 35.sp,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold)

            Image(
                painter = painterResource(R.drawable.logoapp),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Ingresa tu nombre") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    unfocusedBorderColor = Color.Black,
                    focusedBorderColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.size(15.dp))

            Text("Jugar como:",color = Color.White,
                fontWeight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    if(nombre.trim().isBlank()) {
                        mensaje = "Debes ingresar tu nombre."
                    } else {
                        navController.navigate(Screen.Servidor.servidorUsuario(nombre))
                    }
                },
                    enabled = puedeJugar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB8123F)
                    )) {
                    Text("Servidor",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.size(10.dp))
                Button(onClick = {
                    if(nombre.trim().isBlank()) {
                        mensaje = "Debes ingresar tu nombre."
                    } else {
                        navController.navigate(Screen.Cliente.clienteUsuario(nombre))
                    }
                },
                    enabled = puedeJugar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB8123F)
                    ))
                {
                    Text("Cliente", color = Color.White,
                        fontWeight = FontWeight.ExtraBold)
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

            Text(mensaje,
                textAlign = TextAlign.Center, color = Color.White,
                fontWeight = FontWeight.ExtraBold)

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