package com.example.navbattle.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavHost
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.navbattle.MenuDelJuego
import com.example.navbattle.ui.navigation.Screen
import com.example.navbattle.ui.screens.BuscarPartida
import com.example.navbattle.ui.screens.EsperarPartida


@Composable
fun NavBattleNavHost(navHostController: NavHostController, startDestination: String) {
    NavHost(
        navController = navHostController, startDestination = startDestination
    ) {
        composable(Screen.MenuDelJuego.ruta) {
            MenuDelJuego(navHostController)
        }
        composable(Screen.Servidor.ruta) {
            EsperarPartida()
        }
        composable(Screen.Cliente.ruta) {
            BuscarPartida()
        }
    }
}