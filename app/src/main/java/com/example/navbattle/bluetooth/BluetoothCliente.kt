package com.example.navbattle.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.system.Os.socket
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import kotlin.concurrent.thread

object BluetoothCliente {
    private val UUID_SERVICIO: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    @Volatile
    var socketConectado: BluetoothSocket? = null
        private set

    fun conectar(
        context: Context,
        adapter: BluetoothAdapter,
        device: BluetoothDevice,
        nombre: String,
        onMensaje: (Mensaje) -> Unit
    ) {
        thread {
            var tmpSocket: BluetoothSocket? = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (adapter.isDiscovering) adapter.cancelDiscovery()
                    }
                } else {
                    if (adapter.isDiscovering) adapter.cancelDiscovery()
                }


                tmpSocket = try {
                    device.createInsecureRfcommSocketToServiceRecord(UUID_SERVICIO)
                } catch (t: Throwable) {
                    device.createRfcommSocketToServiceRecord(UUID_SERVICIO)
                }

                tmpSocket.connect()
                socketConectado = tmpSocket

                escucharMensajes(tmpSocket, onMensaje)

                enviarMensaje(Mensaje(tipo = "join", data = nombre))

            } catch (e: Exception) {
                e.printStackTrace()
                try { tmpSocket?.close() } catch (_: Exception) {}
            }
        }
    }

    fun enviarMensaje(mensaje: Mensaje) {
        try {
            val socket = socketConectado ?: return
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
            val gson = Gson()
            writer.write(gson.toJson(mensaje))
            writer.write("\n")
            writer.flush()
        } catch (e: IOException) {
            Log.e("Cliente", "Error al enviar: ${e.message}")
        }
    }

    fun escucharMensajes(socket: BluetoothSocket, onMensaje: (Mensaje) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                while (true) {
                    val line = reader.readLine() ?: break
                    try {
                        val mensaje = Gson().fromJson(line, Mensaje::class.java)
                        onMensaje(mensaje)
                    } catch (e: Exception) {
                        Log.e("Cliente", "Error parseando mensaje: ${e.message}")
                    }
                }
            } catch (e: IOException) {
                Log.e("Cliente", "Error al leer mensajes: ${e.message}")
            } finally {
                try { socket.close() } catch (_: Exception) {}
                if (socketConectado === socket) socketConectado = null
            }
        }
    }
}

