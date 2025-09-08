package com.example.navbattle.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.navbattle.bluetooth.BluetoothCliente
import com.example.navbattle.bluetooth.Mensaje
import com.example.navbattle.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun BuscarPartida(nombre: String?, navController: NavController) {
    val context = LocalContext.current
    val adapter = BluetoothAdapter.getDefaultAdapter()
    val devices = remember { mutableStateListOf<BluetoothDevice>() }
    var conectado by remember { mutableStateOf(false) }
    var elegido by remember { mutableStateOf(false) }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val allPermissionsGranted = bluetoothPermissions.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    // Solicitar permisos.
    LaunchedEffect(Unit) {
        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(
                context as Activity,
                bluetoothPermissions.toTypedArray(),
                1
            )
        }
    }

    // Descubrir dispositivos.
    DisposableEffect(Unit) {
        if (allPermissionsGranted) {
            adapter?.bondedDevices?.forEach { device ->
                if (!devices.contains(device)) {
                    devices.add(device)
                }
            }

            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            if (!devices.contains(it)) {
                                devices.add(it)
                            }
                        }
                    }
                }
            }

            context.registerReceiver(receiver, filter)
            adapter?.startDiscovery()

            onDispose {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
                adapter?.cancelDiscovery()
            }
        } else {
            onDispose { }
        }
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡Hola ${nombre ?: "Player"}! Elige una partida:")
        Spacer(Modifier.size(10.dp))

        devices.forEach { device ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(device.name ?: "Dispositivo desconocido")
                    Spacer(Modifier.size(10.dp))
                    Button(
                        onClick = {
                            elegido = true
                            conectado = true

                            BluetoothCliente.conectar(context, adapter, device, nombre ?: "Player") { mensaje ->
                                if (mensaje.tipo == "iniciar") {

                                    Handler(Looper.getMainLooper()).post {
                                        navController.navigate(Screen.Juego.juegoDelUsuario(nombre, false)) {
                                            popUpTo(Screen.Cliente.ruta) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !elegido
                    ) {
                        Text("Conectar")
                    }
                }
                Spacer(Modifier.size(10.dp))
            }
        }

        if (conectado) {
            Text("¡Conectado! Esperando a que el servidor inicie la partida...")
        }

        Spacer(modifier = Modifier.size(10.dp))

        Button(onClick = {
            adapter?.bondedDevices?.forEach { device ->
                if (!devices.contains(device)) {
                    devices.add(device)
                }
            }
        } ) {
            Text("Refrescar")
        }
    }
}




@Composable
fun BuscarPartidaPreview(nombre: String?) {
    val devices = mutableListOf<DevicesBluetoothPrueba>()
    devices.add(DevicesBluetoothPrueba("Teléfono1"))
    devices.add(DevicesBluetoothPrueba("Teléfono2"))
    devices.add(DevicesBluetoothPrueba("Teléfono3"))
    var elegido = false
    var conectado = false


    Column(
        modifier = Modifier.fillMaxSize().padding(top=20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡Hola ${nombre ?: "Player"}! Elige una partida:")
        Spacer(Modifier.size(10.dp))

        devices.forEach { device ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(device.name)//?: "Dispositivo desconocido")
                    Spacer(Modifier.size(10.dp))
                    Button(
                        onClick = {
                            elegido = !elegido
                            conectado = !conectado
                        },
                        enabled = !elegido
                    ) {
                        Text("Conectar")
                    }
                }
                Spacer(Modifier.size(10.dp))
            }
        }

        if (conectado) {
            Text("¡Conectado! Esperando a que el servidor inicie la partida...")
        }

        Button(onClick = {
        } ) {
            Text("Refrescar")
        }

    }
}

data class DevicesBluetoothPrueba(val name: String) {
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ClienteScreenPreview() {
    BuscarPartidaPreview("Nombre")
}
