package com.example.navbattle.ui.navigation

sealed class Screen(val ruta: String) {
    object MenuDelJuego : Screen("menu")
    object Servidor : Screen("servidor/{nombre}") {
        fun servidorUsuario(nombre: String?) : String {
            return "servidor/$nombre"
        }
    }
    object Cliente : Screen("cliente/{nombre}") {
        fun clienteUsuario(nombre: String?) : String {
            return "cliente/$nombre"
        }
    }
    object Juego : Screen("juego/{nombre}/{isServer}") {
        fun juegoDelUsuario(nombre: String?, isServer: Boolean): String {
            val safeNombre = nombre?.takeIf { it.isNotBlank() } ?: "Player"
            return "juego/$safeNombre/$isServer"
        }
    }

    object GameOver : Screen("gameover/{isServer}/{ganador}") {
        fun gameOverDelUsuario(isServer: Boolean, ganador: String): String {
            return "gameover/$isServer/$ganador"
        }
    }

}