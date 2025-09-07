package com.example.navbattle.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

object BluetoothCliente {

    private val UUID_SERVICIO: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    var socketConectado: BluetoothSocket? = null
        private set

    fun conectar(device: BluetoothDevice, onConectado: (BluetoothSocket) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = device.createRfcommSocketToServiceRecord(UUID_SERVICIO)
                socket.connect()
                socketConectado = socket
                onConectado(socket)
            } catch (e: IOException) {
                Log.e("Cliente", "Error al conectar: ${e.message}")
            }
        }
    }

    fun enviarMensaje(mensaje: Mensaje) {
        try {
            val output = socketConectado?.outputStream ?: return
            val gson = Gson()
            val mensajeJson = gson.toJson(mensaje)
            output.write(mensajeJson.toByteArray(Charsets.UTF_8))
            output.flush()
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
                    val mensaje = Gson().fromJson(line, Mensaje::class.java)
                    onMensaje(mensaje)
                }
            } catch (e: IOException) {
                Log.e("Cliente", "Error al leer mensajes: ${e.message}")
            }
        }
    }


}