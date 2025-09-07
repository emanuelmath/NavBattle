package com.example.navbattle.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.MotionEvent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

    // Forzar orientación horizontal y registrar sensor.
    DisposableEffect(Unit) {
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Sprites.
    val naveServer3 = ImageBitmap.imageResource(R.drawable.naveservidorvida3)
    val naveServer2 = ImageBitmap.imageResource(R.drawable.naveservidorvida2)
    val naveServer1 = ImageBitmap.imageResource(R.drawable.naveservidorvida1)
    val naveServer0 = ImageBitmap.imageResource(R.drawable.naveservidorvida0)

    val naveCliente3 = ImageBitmap.imageResource(R.drawable.naveclientevida3)
    val naveCliente2 = ImageBitmap.imageResource(R.drawable.naveclientevida2)
    val naveCliente1 = ImageBitmap.imageResource(R.drawable.naveclientevida1)
    val naveCliente0 = ImageBitmap.imageResource(R.drawable.naveclientevida0)

    val balaSprite = ImageBitmap.imageResource(R.drawable.bala)

    // Escuchar mensajes Bluetooth.
    LaunchedEffect(Unit) {
        val socket = if (isServer) BluetoothServidor.socketConectado!! else BluetoothCliente.socketConectado!!
        val escuchar: (Mensaje) -> Unit = { mensaje ->
            when (mensaje.tipo) {
                "bala" -> {
                    val balaEnemiga = Gson().fromJson(mensaje.data, Bala::class.java)
                    gameState.value = gameState.value.copy(
                        bullets = gameState.value.bullets + balaEnemiga
                    )
                }
                "hit" -> {
                    val vidas = mensaje.data.toInt()
                    gameState.value = gameState.value.copy(
                        enemy = gameState.value.enemy.copy(lives = vidas)
                    )
                }
                "gameOver" -> {
                    /*val ganador = mensaje.data
                    navController.navigate(Screen.GameOver.ruta){
                        popUpTo(Screen.Juego.ruta) { inclusive = true }
                    }*/
                }
            }
        }
        if (isServer) BluetoothServidor.escucharMensajes(socket, escuchar)
        else BluetoothCliente.escucharMensajes(socket, escuchar)
    }

    // Bucle principal del juego.
    LaunchedEffect(Unit) {
        while (true) {
            val state = gameState.value

            // Limitar nave dentro de los límites 0..1.
            val player = state.player
            val clampedPlayer = player.copy(
                position = player.position.copy(
                    x = player.position.x.coerceIn(0f, 1f),
                    y = player.position.y.coerceIn(0f, 1f)
                )
            )

            // Mover balas.
            val updatedBullets = state.bullets.map { it.copy(position = it.position + it.velocity) }

            // Eliminar balas fuera de pantalla y enviar al enemigo.
            val bulletsInScreen = updatedBullets.filter { bala ->
                val fueraArriba = bala.position.y < 0f
                val fueraAbajo = bala.position.y > 1f

                if (fueraArriba || fueraAbajo) {
                    if (bala.isMine) {
                        val balaParaOtro = bala.copy(isMine = false)
                        val mensaje = Mensaje("bala", Gson().toJson(balaParaOtro))
                        if (isServer) BluetoothServidor.enviarMensaje(mensaje)
                        else BluetoothCliente.enviarMensaje(mensaje)
                    }
                    false
                } else true
            }

            // Actualizar estado
            gameState.value = state.copy(
                player = clampedPlayer,
                bullets = bulletsInScreen
            )


            if (clampedPlayer.lives <= 0 || state.enemy.lives <= 0) break
            //Mejorar para hacer el game over local y avisar a ambos para parar.


            delay(16L) //60fps.
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
                        balas.add(Bala(posNormalized, Offset(0f, -0.05f), true))
                        balas.add(Bala(posNormalized, Offset(-0.02f, -0.08f), true))
                        balas.add(Bala(posNormalized, Offset(0.02f, -0.08f), true))
                    } else {
                        balas.add(Bala(posNormalized, Offset(0f, -0.05f), true))
                    }

                    // Actualizar estado con nuevas balas.
                    gameState.value = state.copy(
                        bullets = state.bullets + balas,
                        player = player.copy(
                            bulletsAvailable = player.bulletsAvailable - 1,
                            lastShotTime = now
                        )
                    )

                    // Enviar balas al otro jugador.
                    balas.forEach { bala ->
                        val mensaje = Mensaje("bala", Gson().toJson(bala.copy(isMine = false)))
                        if (isServer) BluetoothServidor.enviarMensaje(mensaje)
                        else BluetoothCliente.enviarMensaje(mensaje)
                    }

                    // Recargar balas después de delay.
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
            val naveSize = Size(canvasSize.width * 0.1f, canvasSize.height * 0.1f)
            val balaSize = Size(canvasSize.width * 0.05f, canvasSize.height * 0.05f)

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
            val centeredPlayerPos = playerPosPx - Offset(naveSize.width / 2f, naveSize.height / 2f)
            drawImage(playerSprite, dstSize = IntSize(naveSize.width.roundToInt(), naveSize.height.roundToInt()), dstOffset = IntOffset(centeredPlayerPos.x.roundToInt(), centeredPlayerPos.y.roundToInt()))

            // Dibujar balas y manejar colisiones.
            state.bullets.forEach { bala ->
                val balaPx = bala.position.toPx() - Offset(balaSize.width / 2f, balaSize.height / 2f)

                // Colisión del jugador local con balas enemigas.
                if (!bala.isMine && checkCollision(balaPx, balaSize, playerPosPx, naveSize)) {
                    val newLives = state.player.lives - 1
                    gameState.value = state.copy(player = state.player.copy(lives = newLives))

                    // Enviar que se recibió el disparo.
                    val mensajeHit = Mensaje("hit", newLives.toString())
                    if (isServer) BluetoothServidor.enviarMensaje(mensajeHit)
                    else BluetoothCliente.enviarMensaje(mensajeHit)
                    // Luego agregar algo visual o sonoro para avisar al jugador que su bala pegó.


                }

                drawImage(balaSprite, dstSize = IntSize(balaSize.width.roundToInt(), balaSize.height.roundToInt()), dstOffset = IntOffset(balaPx.x.roundToInt(), balaPx.y.roundToInt()))
            }
        }

        // Ver balas disponibles, mejorar decoración y/o agregar otros stats.
        Text("${gameState.value.player.bulletsAvailable}", color = Color.White, fontSize = 50.sp, modifier = Modifier.padding(16.dp))
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

            // Dibujar nave local
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

            // Bujar balas
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
