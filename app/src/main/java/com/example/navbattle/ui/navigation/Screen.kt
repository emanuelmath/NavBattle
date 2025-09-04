package com.example.navbattle.ui.navigation

sealed class Screen(val ruta: String) {
    object MenuDelJuego : Screen("menu")
    object Servidor : Screen("servidor")
    object Cliente : Screen("cliente")
    object GameOver : Screen("gameover")

}