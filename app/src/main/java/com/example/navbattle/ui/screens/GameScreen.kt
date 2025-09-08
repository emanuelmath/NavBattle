package com.example.navbattle.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.navbattle.R
import com.example.navbattle.bluetooth.BluetoothCliente
import com.example.navbattle.bluetooth.BluetoothServidor
import com.example.navbattle.bluetooth.Mensaje
import com.example.navbattle.game.Bala
import com.example.navbattle.game.GameState
import com.example.navbattle.game.Nave
import com.example.navbattle.game.NaveAccelerometerListener
import com.example.navbattle.ui.navigation.Screen
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun GameScreen(
    isServer: Boolean,
    nombre: String = "",
    navController: NavController,
    gameState: MutableState<GameState>,
    sensorEventListener: NaveAccelerometerListener,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Bloquear botón atrás.
    BackHandler { }

    DisposableEffect(Unit) {
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Sprites naves.
    val naveServerSprites = listOf(
        ImageBitmap.imageResource(R.drawable.naveservidorvida0),
        ImageBitmap.imageResource(R.drawable.naveservidorvida1),
        ImageBitmap.imageResource(R.drawable.naveservidorvida2),
        ImageBitmap.imageResource(R.drawable.naveservidorvida3)
    )
    val naveClienteSprites = listOf(
        ImageBitmap.imageResource(R.drawable.naveclientevida0),
        ImageBitmap.imageResource(R.drawable.naveclientevida1),
        ImageBitmap.imageResource(R.drawable.naveclientevida2),
        ImageBitmap.imageResource(R.drawable.naveclientevida3)
    )

    // Sprites balas.
    val balaSprite = ImageBitmap.imageResource(R.drawable.bala)
    val balaSpriteDown = ImageBitmap.imageResource(R.drawable.balaabajo)

    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


    // Escuchar mensajes bluetooth.
    LaunchedEffect(Unit) {
        val socket = if (isServer) BluetoothServidor.socketConectado else BluetoothCliente.socketConectado

        if (socket != null) {
            val escuchar: (Mensaje) -> Unit = { mensaje ->
                when (mensaje.tipo) {
                    "bala" -> {
                        val balaEnemiga = try {
                            Gson().fromJson(mensaje.data, Bala::class.java)
                        } catch (e: Exception) {
                            Log.e("GameScreen", "Error parseando bala: ${e.message}")
                            null
                        }
                        balaEnemiga?.let {
                            val mirrored = it.copy(
                                position = Offset(it.position.x, 1f - it.position.y),
                                velocity = Offset(it.velocity.x, -it.velocity.y),
                                isMine = false
                            )

                            gameState.value = gameState.value.copy(
                                bullets = gameState.value.bullets + mirrored
                            )
                        }
                    }
                    "hit" -> {
                        val vidas = mensaje.data.toIntOrNull()
                        if (vidas == null) {
                            Log.w("GameScreen", "hit recibido con data no válida: '${mensaje.data}'")
                        } else {
                            gameState.value = gameState.value.copy(
                                enemy = gameState.value.enemy.copy(lives = vidas)
                            )
                        }
                    }
                    "gameOver" -> {
                        val ganador = mensaje.data
                        Handler(Looper.getMainLooper()).post {
                            navController.navigate(Screen.GameOver.ruta) {
                                popUpTo(Screen.Juego.ruta) { inclusive = true }
                            }
                        }
                    }
                }
            }

            if (isServer) BluetoothServidor.escucharMensajes(socket, escuchar)
            else BluetoothCliente.escucharMensajes(socket, escuchar)
        } else {
            Log.w("GameScreen", "Socket aún no conectado al entrar a GameScreen")
        }
    }

    var gameOverNotificado by remember { mutableStateOf(false) }

    // Bucle principal del juego.
    LaunchedEffect(Unit) {
        while (true) {
            val state = gameState.value
            val player = state.player
            val clampedPlayer = player.copy(
                position = player.position.copy(
                    x = player.position.x.coerceIn(0f, 1f),
                    y = player.position.y.coerceIn(0f, 1f)
                )
            )

            val updatedBullets = state.bullets.map { it.copy(position = it.position + it.velocity) }
            val bulletsInScreen = updatedBullets.filter { it.position.y in 0f..1f }

            val myPlayer = clampedPlayer
            val naveSize = Size(0.08f, 0.12f)
            val balaSize = Size(0.025f, 0.035f)

            val survivingBullets = mutableListOf<Bala>()
            var hitDetected = false

            for (bala in bulletsInScreen) {
                if (!bala.isMine && checkCollision(bala.position, balaSize, myPlayer.position, naveSize)) {
                    hitDetected = true
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    survivingBullets.add(bala)
                }
            }

            var updatedPlayer = myPlayer
            if (hitDetected) {
                val newLives = (myPlayer.lives - 1).coerceAtLeast(0)
                updatedPlayer = myPlayer.copy(lives = newLives)

                val mensajeHit = Mensaje("hit", newLives.toString())
                if (isServer) BluetoothServidor.enviarMensaje(mensajeHit)
                else BluetoothCliente.enviarMensaje(mensajeHit)
            }

            gameState.value = state.copy(
                player = updatedPlayer,
                bullets = survivingBullets
            )


            if (!gameOverNotificado && (clampedPlayer.lives <= 0 || state.enemy.lives <= 0)) {
                gameOverNotificado = true
                val ganador = if (clampedPlayer.lives > 0) nombre else "Enemy"
                val mensajeGameOver = Mensaje("gameOver", ganador)
                if (isServer) BluetoothServidor.enviarMensaje(mensajeGameOver)
                else BluetoothCliente.enviarMensaje(mensajeGameOver)

                navController.navigate(Screen.GameOver.ruta) {
                    popUpTo(Screen.Juego.ruta) { inclusive = true }
                }
                break
            }

            delay(16L) //60 fps.
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInteropFilter { event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val now = System.currentTimeMillis()
                    val state = gameState.value
                    val player = state.player

                    if (now - player.lastShotTime < 500 || player.bulletsAvailable <= 0) return@pointerInteropFilter true

                    val balas = mutableListOf<Bala>()
                    val posNormalized = player.position

                    if (player.bulletsAvailable == 1) {
                        balas.add(Bala(posNormalized, Offset(0f, -0.025f), true))
                        balas.add(Bala(posNormalized, Offset(-0.015f, -0.035f), true))
                        balas.add(Bala(posNormalized, Offset(0.015f, -0.035f), true))

                    } else {
                        balas.add(Bala(posNormalized, Offset(0f, -0.05f), true))
                    }

                    gameState.value = state.copy(
                        bullets = state.bullets + balas,
                        player = player.copy(
                            bulletsAvailable = player.bulletsAvailable - 1,
                            lastShotTime = now
                        )
                    )

                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))

                    balas.forEach { bala ->
                        val mensaje = Mensaje("bala", Gson().toJson(bala))
                        if (isServer) BluetoothServidor.enviarMensaje(mensaje)
                        else BluetoothCliente.enviarMensaje(mensaje)
                    }

                    if (player.bulletsAvailable == 1) {
                        scope.launch {
                            delay(1000)
                            val updatedPlayer = gameState.value.player.copy(bulletsAvailable = 3)
                            gameState.value = gameState.value.copy(player = updatedPlayer)
                        }
                    }



                    true
                } else false
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val state = gameState.value
            val canvasSize = size
            fun Offset.toPx(): Offset = Offset(x * canvasSize.width, y * canvasSize.height)
            val naveSize = Size(canvasSize.width * 0.12f, canvasSize.height * 0.16f)
            val balaSize = Size(canvasSize.width * 0.035f, canvasSize.height * 0.05f)


            val playerPosPx = state.player.position.toPx()
            val playerSprite = if (isServer) {
                naveServerSprites.getOrElse(state.player.lives) { naveServerSprites.first() }
            } else {
                naveClienteSprites.getOrElse(state.player.lives) { naveClienteSprites.first() }
            }

            val centeredPlayerPos = playerPosPx - Offset(naveSize.width / 2f, naveSize.height / 2f)
            drawImage(
                playerSprite,
                dstSize = IntSize(naveSize.width.roundToInt(), naveSize.height.roundToInt()),
                dstOffset = IntOffset(centeredPlayerPos.x.roundToInt(), centeredPlayerPos.y.roundToInt())
            )

            state.bullets.forEach { bala ->
                val balaPx = bala.position.toPx() - Offset(balaSize.width / 2f, balaSize.height / 2f)

                if (!bala.isMine && checkCollision(balaPx, balaSize, playerPosPx, naveSize)) {
                    val newLives = (state.player.lives - 1).coerceAtLeast(0)
                    gameState.value = state.copy(player = state.player.copy(lives = newLives))

                    val mensajeHit = Mensaje("hit", newLives.toString())
                    if (isServer) BluetoothServidor.enviarMensaje(mensajeHit)
                    else BluetoothCliente.enviarMensaje(mensajeHit)
                }

                val balaSpriteToUse = if (bala.isMine) balaSprite else balaSpriteDown
                drawImage(
                    balaSpriteToUse,
                    dstSize = IntSize(balaSize.width.roundToInt(), balaSize.height.roundToInt()),
                    dstOffset = IntOffset(balaPx.x.roundToInt(), balaPx.y.roundToInt())
                )
            }
        }

        Text(
            "${gameState.value.player.bulletsAvailable}",
            color = Color.White,
            fontSize = 50.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}



// Funciones de colisión.
fun getNaveCollider(position: Offset, size: Size): Rect {
    val shrinkFactor = 0.7f
    val colliderWidth = size.width * shrinkFactor
    val colliderHeight = size.height * shrinkFactor
    return Rect(
        position.x - colliderWidth / 2f,
        position.y - colliderHeight / 2f,
        position.x + colliderWidth / 2f,
        position.y + colliderHeight / 2f
    )
}

fun getBalaCollider(position: Offset, size: Size): Rect {
    val expandFactor = 1.2f
    val colliderWidth = size.width * expandFactor
    val colliderHeight = size.height * expandFactor
    return Rect(
        position.x - colliderWidth / 2f,
        position.y - colliderHeight / 2f,
        position.x + colliderWidth / 2f,
        position.y + colliderHeight / 2f
    )
}

fun checkCollision(balaPos: Offset, balaSize: Size, navePos: Offset, naveSize: Size): Boolean {
    val balaRect = getBalaCollider(balaPos, balaSize)
    val naveRect = getNaveCollider(navePos, naveSize)
    return balaRect.overlaps(naveRect)
}


@Composable
fun GameScreenPreview(
    isServer: Boolean,
    gameState: GameState,
    screenSize: MutableState<IntSize>,
    modifier: Modifier = Modifier
) {

    val naveServer3 = ImageBitmap.imageResource(R.drawable.naveservidorvida3)
    val naveServer2 = ImageBitmap.imageResource(R.drawable.naveservidorvida2)
    val naveServer1 = ImageBitmap.imageResource(R.drawable.naveservidorvida1)
    val naveServer0 = ImageBitmap.imageResource(R.drawable.naveservidorvida0)

    val naveCliente3 = ImageBitmap.imageResource(R.drawable.naveclientevida3)
    val naveCliente2 = ImageBitmap.imageResource(R.drawable.naveclientevida2)
    val naveCliente1 = ImageBitmap.imageResource(R.drawable.naveclientevida1)
    val naveCliente0 = ImageBitmap.imageResource(R.drawable.naveclientevida0)

    val balaSprite = ImageBitmap.imageResource(R.drawable.bala)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val state = gameState
        Text("${state.player.bulletsAvailable}", color = Color.White, fontSize = 50.sp)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { screenSize.value = it }
        ) {
            val state = gameState

            fun Offset.toPx(): Offset = Offset(x * size.width, y * size.height)

            // Dibujar nave local.
            val playerPosPx = state.player.position.toPx()
            val playerSprite = if (isServer) {
                when (state.player.lives) {
                    3 -> naveServer3
                    2 -> naveServer2
                    1 -> naveServer1
                    else -> naveServer0
                }
            } else {
                when (state.player.lives) {
                    3 -> naveCliente3
                    2 -> naveCliente2
                    1 -> naveCliente1
                    else -> naveCliente0
                }
            }
            drawImage(playerSprite, topLeft = playerPosPx)

            // Dibujar balas.
            state.bullets.forEach { bala ->
                drawImage(balaSprite, topLeft = bala.position)
            }
        }
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GamePreview() {
    val screenSize = remember { mutableStateOf(IntSize(1080, 1920)) }
    val gameState = GameState(
        player = Nave(
            position = Offset(0.5f, 0.8f),
            lives = 3
        ),
        enemy = Nave(
            position = Offset(0.5f, 0.2f),
            lives = 3
        ),
        bullets = mutableListOf(
            Bala(position = Offset(500f, 1500f), velocity = Offset(0f, -25f), isMine = true)
        )
    )

    GameScreenPreview(
        isServer = true,
        gameState = gameState,
        screenSize = screenSize,
        modifier = Modifier.fillMaxSize()
    )
}
