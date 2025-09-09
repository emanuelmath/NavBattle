package com.example.navbattle.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
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
import com.example.navbattle.game.BalaNetwork
import com.example.navbattle.game.GameState
import com.example.navbattle.game.Nave
import com.example.navbattle.game.NaveAccelerometerListener
import com.example.navbattle.ui.navigation.Screen
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

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

    val naveWidth = 0.1f
    val naveHeight = 0.14f

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

    fun safeSendMensaje(mensaje: Mensaje) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isServer) BluetoothServidor.enviarMensaje(mensaje) else BluetoothCliente.enviarMensaje(mensaje)
            } catch (e: Exception) {
                Log.e("GameScreen", "safeSendMensaje error: ${e.message}")
            }
        }
    }

    suspend fun sendWithRetries(mensaje: Mensaje, retries: Int = 3, delayMs: Long = 200L) {
        repeat(retries) { attempt ->
            try {
                withContext(Dispatchers.IO) {
                    if (isServer) BluetoothServidor.enviarMensaje(mensaje) else BluetoothCliente.enviarMensaje(mensaje)
                }
                return
            } catch (e: Exception) {
                Log.w("GameScreen", "sendWithRetries intento ${attempt + 1} falló: ${e.message}")
                delay(delayMs)
            }
        }
        Log.w("GameScreen", "sendWithRetries: todos los intentos fallaron.")
    }

    // Escuchar mensajes bluetooth.
    LaunchedEffect(Unit) {
        val socket = if (isServer) BluetoothServidor.socketConectado else BluetoothCliente.socketConectado
        if (socket != null) {
            val escuchar: (Mensaje) -> Unit = { mensaje ->
                when (mensaje.tipo) {
                    "bala" -> {
                        val balaNet = try {
                            Gson().fromJson(mensaje.data, BalaNetwork::class.java)
                        } catch (e: Exception) {
                            Log.e("GameScreen", "Error parseando BalaNetwork: ${e.message}")
                            null
                        }
                        balaNet?.let { bn ->
                            val posX = bn.offsetX.coerceIn(0f, 1f)
                            val posY = (1f - bn.offsetY).coerceIn(0f, 1f)
                            val velX = bn.velX
                            val velY = -bn.velY

                            fun postAdd(b: Bala) {
                                Handler(Looper.getMainLooper()).post {
                                    gameState.value = gameState.value.copy(bullets = gameState.value.bullets + b)
                                }
                            }

                            if (bn.isTriple) {
                                val tripleOffsets = bn.tripleOffsets?.ifEmpty { listOf(-0.03f, 0f, 0.03f) }
                                tripleOffsets?.forEach { dx ->
                                    val dxMapped = if (bn.senderIsServer != isServer) -dx else dx
                                    val xMapped = (posX + dxMapped).coerceIn(0f, 1f)
                                    val balaRecibida = Bala(Offset(xMapped, posY), Offset(velX, velY), false)
                                    postAdd(balaRecibida)
                                }
                            } else {
                                val balaRecibida = Bala(Offset(posX, posY), Offset(velX, velY), false)
                                postAdd(balaRecibida)
                            }
                        }
                    }

                    "hit" -> {
                        val vidas = mensaje.data.toIntOrNull()
                        vidas?.let {
                            Handler(Looper.getMainLooper()).post {
                                gameState.value = gameState.value.copy(enemy = gameState.value.enemy.copy(lives = it))
                            }
                        }
                    }

                    "gameOver" -> {
                        val ganadorId = mensaje.data
                        Handler(Looper.getMainLooper()).post {
                            if (!gameState.value.isGameOver) {
                                val winnerText = when (ganadorId) {
                                    "server" -> if (isServer) nombre else gameState.value.enemy.name
                                    "client" -> if (!isServer) nombre  else gameState.value.enemy.name
                                    else -> ganadorId
                                }
                                gameState.value = gameState.value.copy(isGameOver = true, winner = winnerText)
                                try {
                                    val isServerWinner = gameState.value.player.lives > gameState.value.enemy.lives
                                    navController.navigate(Screen.GameOver.gameOverDelUsuario(isServerWinner, winnerText)) {
                                        popUpTo(Screen.Juego.ruta) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Log.e("GameScreen", "Error navegando a GameOver al recibir: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }

            try {
                if (isServer) BluetoothServidor.escucharMensajes(socket, escuchar)
                else BluetoothCliente.escucharMensajes(socket, escuchar)
            } catch (e: Exception) {
                Log.e("GameScreen", "Error iniciando escucha socket: ${e.message}")
            }
        } else {
            Log.w("GameScreen", "Socket nulo en LaunchedEffect inicial.")
        }
    }

    var gameOverNotificado by remember { mutableStateOf(false) }

    // Bucle principal del juego.
    LaunchedEffect(Unit) {
        while (true) {
            val state = gameState.value

            val player = state.player
            val minX = naveWidth / 2f
            val maxX = 1f - naveWidth / 2f
            val minY = naveHeight / 2f
            val maxY = 1f - naveHeight / 2f

            fun applyBounce(coord: Float, min: Float, max: Float): Float {
                return when {
                    coord < min -> {
                        val penetration = min - coord
                        (min + penetration * 0.3f).coerceAtMost(max)
                    }
                    coord > max -> {
                        val penetration = coord - max
                        (max - penetration * 0.3f).coerceAtLeast(min)
                    }
                    else -> coord
                }
            }

            val bouncedX = applyBounce(player.position.x, minX, maxX)
            val bouncedY = applyBounce(player.position.y, minY, maxY)
            val clampedPlayer = player.copy(position = Offset(bouncedX, bouncedY))

            val updatedBullets = state.bullets.map { it.copy(position = it.position + it.velocity) }

            val bulletsInScreen = updatedBullets.filter { it.position.y in -0.2f..1.2f && it.position.x in -0.2f..1.2f }

            val naveSize = Size(naveWidth, naveHeight)
            val balaSize = Size(0.035f, 0.045f)

            val survivingBullets = mutableListOf<Bala>()
            var hitDetected = false

            for (bala in bulletsInScreen) {
                if (!bala.isMine && checkCollision(bala.position, balaSize, clampedPlayer.position, naveSize)) {
                    hitDetected = true
                    try { vibrator.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE)) } catch (_: Exception) {}
                } else {
                    survivingBullets.add(bala)
                }
            }

            var updatedPlayer = clampedPlayer
            if (hitDetected) {
                val newLives = (clampedPlayer.lives - 1).coerceAtLeast(0)
                updatedPlayer = clampedPlayer.copy(lives = newLives)
                val mensajeHit = Mensaje("hit", newLives.toString())
                safeSendMensaje(mensajeHit)
            }

            gameState.value = state.copy(player = updatedPlayer, bullets = survivingBullets)

            if (!gameOverNotificado) {
                if (updatedPlayer.lives <= 0) {
                    gameOverNotificado = true
                    val winnerId = if (isServer) "client" else "server"
                    val mensajeGameOver = Mensaje("gameOver", winnerId)
                    scope.launch {
                        try { sendWithRetries(mensajeGameOver, retries = 3, delayMs = 250L) } catch (e: Exception) { Log.e("GameScreen", "Error en sendWithRetries: ${e.message}") }
                    }
                    withContext(Dispatchers.Main) {
                        try {
                            gameState.value = gameState.value.copy(isGameOver = true, winner = "Enemy")
                            val isServerWinner = gameState.value.player.lives > gameState.value.enemy.lives
                            navController.navigate(Screen.GameOver.gameOverDelUsuario(isServerWinner, nombre)) {
                                popUpTo(Screen.Juego.ruta) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Log.e("GameScreen", "Error navegando a GameOver local: ${e.message}")
                        }
                    }
                    break
                } else if (state.enemy.lives <= 0) {
                    gameOverNotificado = true
                    val winnerId = if (isServer) "server" else "client"
                    val mensajeGameOver = Mensaje("gameOver", winnerId)
                    scope.launch {
                        try { sendWithRetries(mensajeGameOver, retries = 3, delayMs = 250L) } catch (e: Exception) { Log.e("GameScreen", "Error en sendWithRetries (enemy<=0): ${e.message}") }
                    }
                    withContext(Dispatchers.Main) {
                        try {
                            gameState.value = gameState.value.copy(isGameOver = true, winner = nombre)
                            val ganador = gameState.value.winner
                            val isServerWinner = gameState.value.player.lives > gameState.value.enemy.lives
                            navController.navigate(Screen.GameOver.gameOverDelUsuario(isServerWinner, ganador ?: "Player")) {
                                popUpTo(Screen.Juego.ruta) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Log.e("GameScreen", "Error navegando a GameOver (enemy<=0): ${e.message}")
                        }
                    }
                    break
                }
            }

            delay(16L) //60 fps.
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter { event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val now = System.currentTimeMillis()
                    val state = gameState.value
                    val player = state.player

                    if (now - player.lastShotTime < 300 || player.bulletsAvailable <= 0) return@pointerInteropFilter true

                    val balas = mutableListOf<Bala>()
                    val isTripleLocal = (player.bulletsAvailable == 1)

                    if (isTripleLocal) {
                        val tripleOffsets = listOf(-0.03f, 0f, 0.03f)
                        tripleOffsets.forEach { dx ->
                            val balaPos = Offset((player.position.x + dx).coerceIn(0f, 1f), player.position.y)
                            balas.add(Bala(balaPos, Offset(0f, -0.035f), true))
                        }
                        val bn = BalaNetwork(
                            offsetX = balas[1].position.x,
                            offsetY = balas[1].position.y,
                            velX = 0f,
                            velY = -0.035f,
                            senderIsServer = isServer,
                            isTriple = true,
                            tripleOffsets = tripleOffsets
                        )
                        safeSendMensaje(Mensaje("bala", Gson().toJson(bn)))
                    } else {
                        val bala = Bala(player.position, Offset(0f, -0.035f), true)
                        balas.add(bala)
                        val bn = BalaNetwork(
                            offsetX = bala.position.x,
                            offsetY = bala.position.y,
                            velX = bala.velocity.x,
                            velY = bala.velocity.y,
                            senderIsServer = isServer,
                            isTriple = false,
                            tripleOffsets = listOf()
                        )
                        safeSendMensaje(Mensaje("bala", Gson().toJson(bn)))
                    }


                    gameState.value = state.copy(
                        bullets = state.bullets + balas,
                        player = player.copy(
                            bulletsAvailable = player.bulletsAvailable - 1,
                            lastShotTime = now
                        )
                    )

                    try { vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)) } catch (_: Exception) {}

                    if (isTripleLocal) {
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
        Image(
            painter = painterResource(id = R.drawable.espaciofondo),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val state = gameState.value
            val canvasSize = size
            fun Offset.toPx(): Offset = Offset(x * canvasSize.width, y * canvasSize.height)

            val naveSizePx = Size(canvasSize.width * naveWidth, canvasSize.height * naveHeight)
            val balaSizePx = Size(canvasSize.width * 0.035f, canvasSize.height * 0.05f)

            val playerPosPx = state.player.position.toPx()
            val playerSprite = if (isServer) {
                naveServerSprites.getOrElse(state.player.lives) { naveServerSprites.first() }
            } else {
                naveClienteSprites.getOrElse(state.player.lives) { naveClienteSprites.first() }
            }
            val centeredPlayerPos = playerPosPx - Offset(naveSizePx.width / 2f, naveSizePx.height / 2f)
            drawImage(
                playerSprite,
                dstSize = IntSize(naveSizePx.width.roundToInt(), naveSizePx.height.roundToInt()),
                dstOffset = IntOffset(centeredPlayerPos.x.roundToInt(), centeredPlayerPos.y.roundToInt())
            )

            state.bullets.forEach { bala ->
                val balaPx = bala.position.toPx() - Offset(balaSizePx.width / 2f, balaSizePx.height / 2f)
                val balaSpriteToUse = if (bala.isMine) balaSprite else balaSpriteDown
                drawImage(
                    balaSpriteToUse,
                    dstSize = IntSize(balaSizePx.width.roundToInt(), balaSizePx.height.roundToInt()),
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
        Image(
            painter = painterResource(id = R.drawable.espaciofondo),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
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
