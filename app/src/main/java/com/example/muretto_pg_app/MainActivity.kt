package com.example.muretto_pg_app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val MioFont = FontFamily(Font(R.font.komtit__))
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rottaCorrente = navBackStackEntry?.destination?.route

    // Dialog Recupero Battle
    var mostraPopupRecupero by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (GestoreBattle.haProgresso(context)) mostraPopupRecupero = true }
    if (mostraPopupRecupero) {
        AlertDialog(
            onDismissRequest = { mostraPopupRecupero = false },
            containerColor = Color(0xFF222222),
            title = { Text("CONTINUARE BATTLE?", color = Color.White, fontFamily = MioFont) },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = CircleShape, onClick = {
                    GestoreBattle.caricaProgresso(context)
                    mostraPopupRecupero = false
                    navController.navigate("ottavi")
                }) { Text("SÌ", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { GestoreBattle.pulisciSalvataggio(context); mostraPopupRecupero = false }) { Text("NO", color = Color.Gray) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "benvenuto") {
            composable("benvenuto") { SchermataDiBenvenuto(onVaiAlMenu = { navController.navigate("menu") }) }
            composable("menu") { SchermataMenu(onTornaIndietro = { navController.popBackStack() }, onSelezionaModalita = { navController.navigate(it) }) }

            composable("muretto_classico") {
                SchermataMurettoClassico(
                    tipoTorneo = TipoTorneo.SINGOLO,
                    onTornaAlMenu = { navController.popBackStack() },
                    onIniziaBattle = { GestoreBattle.iniziaTorneo(GestoreBattle.mcsSelezionati); navController.navigate("ottavi") }
                )
            }

            composable("due_contro_due/{tipo}") { backStackEntry ->
                val tipoStr = backStackEntry.arguments?.getString("tipo") ?: TipoTorneo.COPPIE_CASUALI.name
                val tipoSelezionato = TipoTorneo.valueOf(tipoStr)

                SchermataMurettoClassico(
                    tipoTorneo = tipoSelezionato,
                    onTornaAlMenu = { navController.popBackStack() },
                    onIniziaBattle = { GestoreBattle.iniziaTorneo2v2(GestoreBattle.mcsSelezionati, tipoSelezionato); navController.navigate("ottavi") }
                )
            }

            composable("ottavi") { SchermataOttavi(onTornaIndietro = { navController.popBackStack() }, onVaiAiQuarti = { }, onRoundClick = { navController.navigate("round_singolo/$it") }) }
            composable("round_singolo/{roundId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("roundId") ?: ""
                SchermataRoundSingolo(roundId = id, onTornaIndietro = { navController.popBackStack() })
            }
            composable("allenamento") { SchermataAllenamento(onTornaIndietro = { navController.popBackStack() }, onSelezionaAllenamento = {
                when (it) {
                    "Generatore di argomenti" -> navController.navigate("generatore_argomenti")
                    "Generatore di modalita" -> navController.navigate("generatore_modalita")
                    "Generatore di parole" -> navController.navigate("generatore_parole")
                }
            }) }
            composable("generatore_argomenti") { SchermataGeneratoreArgomenti { navController.popBackStack() } }
            composable("generatore_modalita") { SchermataGeneratoreModalita { navController.popBackStack() } }
            composable("generatore_parole") { SchermataGeneratoreParole { navController.popBackStack() } }
        }

        if (rottaCorrente != "benvenuto") {
            FloatingPlayer(MioFont)
        }
    }
}

@Composable
fun FloatingPlayer(font: FontFamily) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Calcoliamo la dimensione dello schermo per piazzarlo a Destra e al Centro
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Offset iniziale: Margine destro e centro verticale
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - 160f) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx / 2f - 100f) }

    var mostraMiniPlayer by remember { mutableStateOf(false) }

    // Stati per i Metadati e la Durata
    var titoloCanzone by remember { mutableStateOf("Nessun Beat") }
    var artistaCanzone by remember { mutableStateOf("In riproduzione...") }
    var copertinaCanzone by remember { mutableStateOf<Bitmap?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durataCanzone by remember { mutableLongStateOf(0L) }
    var posizioneCorrente by remember { mutableLongStateOf(0L) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var activeController by remember { mutableStateOf<MediaController?>(null) }

    val mediaManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    // Formattatore del tempo (00:00)
    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    fun aggiornaInfo() {
        try {
            val sessions = mediaManager.getActiveSessions(ComponentName(context, NotificationListener::class.java))
            if (sessions.isNotEmpty()) {
                val controller = sessions[0]
                activeController = controller
                val metadata = controller.metadata
                val playbackState = controller.playbackState

                titoloCanzone = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Beat Sconosciuto"
                artistaCanzone = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Artista"
                copertinaCanzone = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

                // Aggiorniamo durata e posizione
                durataCanzone = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                if (!isDraggingSlider) {
                    posizioneCorrente = playbackState?.position ?: 0L
                }
            } else {
                activeController = null
            }
        } catch (e: Exception) { /* Permesso mancante */ }
    }

    LaunchedEffect(Unit) {
        while(true) {
            aggiornaInfo()
            delay(1000) // Aggiorna ogni secondo per far scorrere la barra
        }
    }

    // IL PULSANTE FLOTTANTE (Ora 50.dp, più piccolo)
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); offsetX += dragAmount.x; offsetY += dragAmount.y } }
            .size(50.dp) // Leggermente più piccolo (prima era 65)
            .clip(CircleShape)
            .background(Color(0xFFD32F2F).copy(alpha = 0.9f))
            .border(2.dp, Color.White, CircleShape)
            .clickable { mostraMiniPlayer = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_music_note),
            contentDescription = "Apri Player",
            tint = Color.White,
            modifier = Modifier.size(24.dp) // Icona ridimensionata per starci bene
        )
    }

    if (mostraMiniPlayer) {
        Dialog(onDismissRequest = { mostraMiniPlayer = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f) // Schermata più grande
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(2.dp, Color(0xFFD32F2F), RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // COPERTINA (Aumentata di dimensione)
                    Box(modifier = Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray)) {
                        if (copertinaCanzone != null) {
                            Image(bitmap = copertinaCanzone!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(painterResource(id = R.drawable.due_contro_due), null, tint = Color.Gray, modifier = Modifier.size(100.dp).align(Alignment.Center))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(titoloCanzone, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                    Text(artistaCanzone, color = Color.Gray, fontSize = 16.sp, maxLines = 1)
                    Spacer(modifier = Modifier.height(16.dp))

                    // BARRA DELLA DURATA (SLIDER)
                    if (durataCanzone > 0L) {
                        Slider(
                            value = posizioneCorrente.toFloat(),
                            onValueChange = {
                                isDraggingSlider = true
                                posizioneCorrente = it.toLong()
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                activeController?.transportControls?.seekTo(posizioneCorrente)
                            },
                            valueRange = 0f..durataCanzone.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFFD32F2F),
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).offset(y = (-10).dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime(posizioneCorrente), color = Color.LightGray, fontSize = 12.sp)
                            Text(formatTime(durataCanzone), color = Color.LightGray, fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // CONTROLLI (Icone ridotte di dimensione)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { mandaComando(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS) }, modifier = Modifier.size(45.dp)) {
                            Text("I◀", color = Color.White, fontSize = 20.sp)
                        }

                        FloatingActionButton(
                            onClick = { mandaComando(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                            containerColor = Color(0xFFD32F2F),
                            shape = CircleShape,
                            modifier = Modifier.size(60.dp) // Prima era 80
                        ) {
                            Text(if (isPlaying) "❚❚" else "▶", color = Color.White, fontSize = 22.sp)
                        }

                        IconButton(onClick = { mandaComando(context, KeyEvent.KEYCODE_MEDIA_NEXT) }, modifier = Modifier.size(45.dp)) {
                            Text("▶I", color = Color.White, fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                        Text("Attiva info copertina e Slider", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// Funzione helper per mandare i tasti
fun mandaComando(context: Context, key: Int) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
}

// Classe fittizia necessaria per MediaSessionManager
class NotificationListener : android.service.notification.NotificationListenerService()

@Composable
fun SchermataDiBenvenuto(onVaiAlMenu: () -> Unit) {
    var inTransizione by remember { mutableStateOf(false) }

    val spostamentoVerticale = (-50).dp
    val dimensioneAura = 300.dp
    val dimensioneAreaClick = 250.dp
    val dimensionePallinoNero = 90.dp

    val scalaSfondo by animateFloatAsState(targetValue = if (inTransizione) 2.8f else 1f, animationSpec = tween(1300, easing = FastOutSlowInEasing), label = "")
    val scalaEspansione by animateFloatAsState(targetValue = if (inTransizione) 40f else 0f, animationSpec = tween(1100, easing = FastOutSlowInEasing), label = "", finishedListener = { onVaiAlMenu() })
    val alphaContenuto by animateFloatAsState(targetValue = if (inTransizione) 0f else 1f, animationSpec = tween(700), label = "")
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scalaAura by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "")
    val alphaAura by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 0.6f, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scalaSfondo; scaleY = scalaSfondo; alpha = alphaContenuto }) {

            Image(
                painter = painterResource(id = R.drawable.sfondo_schermata_iniziale),
                contentDescription = "Sfondo Muretto",
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )

            Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) { TestoBomboletta() }
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.BottomCenter) { TestoAnimatoStileMinecraft() }
        }

        if (!inTransizione) {
            Box(modifier = Modifier.offset(y = spostamentoVerticale).size(dimensioneAura).graphicsLayer { scaleX = scalaAura; scaleY = scalaAura; alpha = alphaAura }.background(Color.Black.copy(alpha = 0.4f), CircleShape).border(4.dp, Color.Black.copy(alpha = 0.6f), CircleShape))
        }

        Box(modifier = Modifier.offset(y = spostamentoVerticale).size(dimensioneAreaClick).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!inTransizione) inTransizione = true })

        if (inTransizione) {
            Box(modifier = Modifier.offset(y = spostamentoVerticale).size(dimensionePallinoNero).graphicsLayer { scaleX = scalaEspansione; scaleY = scalaEspansione }.background(Color.Black, CircleShape))
        }
    }
}

@Composable
fun TestoBomboletta() {
    var visibile by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(500); visibile = true }
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))

    AnimatedVisibility(visible = visibile, enter = expandHorizontally(animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing), expandFrom = Alignment.Start) + fadeIn(animationSpec = tween(durationMillis = 2000))) {
        Box(contentAlignment = Alignment.Center) {
            Text("BATTLE ROSTER", color = Color.Black, fontSize = 42.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato, style = TextStyle(drawStyle = Stroke(miter = 10f, width = 10f, join = StrokeJoin.Round)))
            Text("BATTLE ROSTER", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato)
        }
    }
}

@Composable
fun TestoAnimatoStileMinecraft() {
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scalaTesto by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "")

    Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer { scaleX = scalaTesto; scaleY = scalaTesto }) {
        Text("WHAT UUU SAYYYNNN!!!", color = Color.Black, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato, style = TextStyle(drawStyle = Stroke(miter = 10f, width = 12f, join = StrokeJoin.Round)))
        Text("WHAT UUU SAYYYNNN!!!", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato)
    }
}