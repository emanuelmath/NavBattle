package com.example.navbattle.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.system.Os.socket
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.navbattle.game.Bala
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
import kotlin.toString

object BluetoothServidor {
    private const val NOMBRE_SERVICIO = "NavBattleGame"
    private val UUID_SERVICIO: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var serverSocket: BluetoothServerSocket? = null
    @Volatile
    var socketConectado: BluetoothSocket? = null
        private set

    fun iniciarServidor(adapter: BluetoothAdapter, onConectado: (BluetoothSocket) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(NOMBRE_SERVICIO, UUID_SERVICIO)
                val socket = serverSocket!!.accept()
                try { serverSocket?.close() } catch (_: Exception) {}
                socketConectado = socket
                onConectado(socket)
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: IOException) {
                Log.e("Servidor", "Error al iniciar servidor: ${e.message}")
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
            Log.e("Servidor", "Error al enviar: ${e.message}")
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
                        Log.e("Servidor", "Error parseando mensaje: ${e.message}")
                    }
                }
            } catch (e: IOException) {
                Log.e("Servidor", "Error al leer mensajes: ${e.message}")
            } finally {
                try { socket.close() } catch (_: Exception) {}
                if (socketConectado === socket) socketConectado = null
            }
        }
    }
}


