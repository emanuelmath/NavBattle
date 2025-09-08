package com.example.navbattle.ui.navigation

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.navigation.NavHostController
import androidx.navigation.NavHost
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.navbattle.MenuDelJuego
import com.example.navbattle.game.Bala
import com.example.navbattle.game.GameState
import com.example.navbattle.game.Nave
import com.example.navbattle.game.NaveAccelerometerListener
import com.example.navbattle.ui.navigation.Screen
import com.example.navbattle.ui.screens.BuscarPartida
import com.example.navbattle.ui.screens.EsperarPartida
import com.example.navbattle.ui.screens.GameOver
import com.example.navbattle.ui.screens.GameScreen



@Composable
fun NavBattleNavHost(navHostController: NavHostController, startDestination: String) {
    NavHost(
        navController = navHostController, startDestination = startDestination
    ) {
        composable(Screen.MenuDelJuego.ruta) {
            MenuDelJuego(navHostController)
        }
        composable(Screen.Servidor.ruta,
            arguments = listOf( navArgument("nombre") {type = NavType.StringType})) { backStackEntry ->
            val nombre = backStackEntry.arguments?.getString("nombre")
            EsperarPartida(navHostController, nombre)
        }
        composable(Screen.Cliente.ruta, arguments = listOf( navArgument("nombre") {type = NavType.StringType})) { backStackEntry ->
            val nombre = backStackEntry.arguments?.getString("nombre")
            BuscarPartida(nombre, navHostController)
        }
        composable(Screen.Juego.ruta, arguments = listOf(
            navArgument("nombre") { type = NavType.StringType },
            navArgument("isServer") {type = NavType.BoolType}
        )) { backStackEntry ->
            val nombre = backStackEntry.arguments?.getString("nombre") ?: "Player"
            val isServer = backStackEntry.arguments?.getBoolean("isServer") ?: true
            val modifier = Modifier
            val gameState = remember {
                mutableStateOf(
                    GameState(
                        player = if (nombre.trim().isBlank()) {
                            Nave(
                                position = if (isServer) Offset(0.5f, 0.8f) else Offset(0.5f, 0.2f)
                            )
                        }
                        else {
                            Nave(
                                position = if (isServer) Offset(0.5f, 0.8f) else Offset(0.5f, 0.2f),
                                name = nombre
                            )
                        },
                        enemy = Nave(
                            position = if (isServer) Offset(0.5f, 0.2f) else Offset(0.5f, 0.8f)
                        )
                    )
                )
            }

            val naveListener = NaveAccelerometerListener(gameState)

            GameScreen(isServer = isServer, navController = navHostController, nombre = nombre, gameState = gameState, sensorEventListener = naveListener, modifier = modifier)
        }
        composable(Screen.GameOver.ruta) {
            GameOver()
        }
    }
}