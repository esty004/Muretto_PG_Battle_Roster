package com.example.muretto_pg_app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.jan.supabase.gotrue.auth
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        DatabaseMcs.controllaAdmin()

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

    var mostraPopupRecupero by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (GestoreBattle.haProgresso(context)) mostraPopupRecupero = true }

    if (mostraPopupRecupero) {
        AlertDialog(
            onDismissRequest = { mostraPopupRecupero = false },
            containerColor = Tema.coloreSfondoCard,
            title = { Text("CONTINUARE BATTLE?", color = Tema.coloreTesto, fontFamily = MioFont) },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), shape = CircleShape, onClick = {
                    GestoreBattle.caricaProgresso(context)
                    mostraPopupRecupero = false
                    navController.navigate("ottavi")
                }) { Text("SÌ", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { GestoreBattle.pulisciSalvataggio(context); mostraPopupRecupero = false }) { Text("NO", color = Tema.coloreTestoSecondario) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "mappa") {
            composable("mappa") { SchermataMappa(onPinClick = { navController.navigate(it) }) }
            composable("login") {
                SchermataLogin(
                    onLoginSuccess = { navController.popBackStack() },
                    onTornaIndietro = { navController.popBackStack() }
                )
            }
            composable("aggiungi_mc") { SchermataAggiungiMc(onTornaIndietro = { navController.popBackStack() }) }

            // Aggiunti i tasti indietro dalle modifiche del branch
            composable("benvenuto") { SchermataDiBenvenuto(onTornaIndietro = { navController.popBackStack() }, onVaiAlMenu = { navController.navigate("menu") }) }
            composable("benvenuto_barre_faul") { SchermataDiBenvenutoBarreFaul(onTornaIndietro = { navController.popBackStack() }, onVaiAlMenu = { navController.navigate("menu") }) }

            composable("menu") { SchermataMenu(onTornaIndietro = { navController.popBackStack() }, onSelezionaModalita = { navController.navigate(it) }) }

            // Nuova rotta TRASFERTE
            composable("trasferte") { SchermataTrasferte(onTornaIndietro = { navController.popBackStack() }) }

            composable("muretto_classico") {
                SchermataMurettoClassico(tipoTorneo = TipoTorneo.SINGOLO, onTornaAlMenu = { navController.popBackStack() }, onIniziaBattle = { GestoreBattle.iniziaTorneo(GestoreBattle.mcsSelezionati); navController.navigate("ottavi") })
            }
            composable("due_contro_due/{tipo}") { backStackEntry ->
                val tipoStr = backStackEntry.arguments?.getString("tipo") ?: TipoTorneo.COPPIE_CASUALI.name
                SchermataMurettoClassico(tipoTorneo = TipoTorneo.valueOf(tipoStr), onTornaAlMenu = { navController.popBackStack() }, onIniziaBattle = { GestoreBattle.iniziaTorneo2v2(GestoreBattle.mcsSelezionati, TipoTorneo.valueOf(tipoStr)); navController.navigate("ottavi") })
            }
            composable("ottavi") { SchermataOttavi(onTornaIndietro = { navController.popBackStack() }, onVaiAiQuarti = { }, onRoundClick = { navController.navigate("round_singolo/$it") }) }
            composable("round_singolo/{roundId}") { backStackEntry ->
                SchermataRoundSingolo(roundId = backStackEntry.arguments?.getString("roundId") ?: "", onTornaIndietro = { navController.popBackStack() })
            }
            composable("allenamento") { SchermataAllenamento(onTornaIndietro = { navController.popBackStack() }, onSelezionaAllenamento = { navController.navigate(it.lowercase().replace(" ", "_")) }) }
            composable("generatore_argomenti") { SchermataGeneratoreArgomenti { navController.popBackStack() } }
            composable("generatore_modalita") { SchermataGeneratoreModalita { navController.popBackStack() } }
            composable("generatore_parole") { SchermataGeneratoreParole { navController.popBackStack() } }
        }

        if (rottaCorrente != "benvenuto" && rottaCorrente != "benvenuto_barre_faul" && rottaCorrente != "mappa" && rottaCorrente != "login" && rottaCorrente != "aggiungi_mc" && rottaCorrente != "trasferte") {
            FloatingPlayer(MioFont)
        }
    }
}

@Composable
fun SchermataMappa(onPinClick: (String) -> Unit) {
    val context = LocalContext.current
    val italyBounds = BoundingBox(47.1, 18.3, 35.5, 6.6)
    val centroMappa = GeoPoint(42.764, 12.244)
    val scope = rememberCoroutineScope()
    var mostraMenuAdmin by remember { mutableStateOf(false) }

    BackHandler(enabled = mostraMenuAdmin) { mostraMenuAdmin = false }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 6.5
                    controller.setZoom(8.0)
                    controller.setCenter(centroMappa)
                    setScrollableAreaLimitDouble(italyBounds)

                    val trueDarkModeMatrix = ColorMatrix(floatArrayOf(
                        0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                        0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                        -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                        0.0f,  0.0f,  0.0f, 1.0f, 0.0f
                    ))
                    overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(trueDarkModeMatrix))

                    val m1 = Marker(this).apply {
                        position = GeoPoint(43.112056, 12.388439)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        val bmp = android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.logo_muretto)
                        icon = android.graphics.drawable.BitmapDrawable(ctx.resources, android.graphics.Bitmap.createScaledBitmap(bmp, 200, 140, true))
                        setOnMarkerClickListener { _, _ ->
                            Tema.isBarreFaul = false
                            onPinClick("benvenuto")
                            true
                        }
                    }
                    overlays.add(m1)

                    val m2 = Marker(this).apply {
                        position = GeoPoint(42.416669, 12.100123)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        val bmp = android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.pin_barre_faul)
                        icon = android.graphics.drawable.BitmapDrawable(ctx.resources, android.graphics.Bitmap.createScaledBitmap(bmp, 200, 140, true))
                        setOnMarkerClickListener { _, _ ->
                            Tema.isBarreFaul = true
                            onPinClick("benvenuto_barre_faul")
                            true
                        }
                    }
                    overlays.add(m2)
                }
            }
        )

        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, end = 20.dp), contentAlignment = Alignment.TopEnd) {
            Image(
                painter = painterResource(id = R.drawable.login_logo),
                contentDescription = "Profilo",
                modifier = Modifier.size(45.dp).clip(CircleShape).clickable {
                    if (DatabaseMcs.isAdmin) mostraMenuAdmin = true else onPinClick("login")
                }
            )
        }

        if (mostraMenuAdmin) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { mostraMenuAdmin = false }
            )
        }

        AnimatedVisibility(
            visible = mostraMenuAdmin,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(0.7f).background(Tema.coloreSfondoCard)
                    .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Tema.colorePrincipale), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painter = painterResource(id = R.drawable.login_logo), contentDescription = null, modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White).padding(10.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("ADMIN PANEL", color = Color.White, fontSize = 22.sp, fontFamily = FontFamily(Font(R.font.komtit__)), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                MenuItem(titolo = "INSERISCI FREESTYLER", icona = R.drawable.ic_music_note) {
                    mostraMenuAdmin = false
                    onPinClick("aggiungi_mc")
                }

                Spacer(modifier = Modifier.weight(1f))

                MenuItem(titolo = "LOGOUT", icona = R.drawable.versus, coloreTesto = Color.Red) {
                    mostraMenuAdmin = false
                    scope.launch {
                        DatabaseMcs.supabase.auth.clearSession()
                        DatabaseMcs.isAdmin = false
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun MenuItem(titolo: String, icona: Int, coloreTesto: Color = Tema.coloreTesto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(id = icona), contentDescription = null, tint = coloreTesto, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = titolo, color = coloreTesto, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.komtit__)))
    }
}

@Composable
fun FloatingPlayer(font: FontFamily) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - 160f) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx / 2f - 100f) }
    var mostraMiniPlayer by remember { mutableStateOf(false) }
    var titoloCanzone by remember { mutableStateOf("Nessun Beat") }
    var artistaCanzone by remember { mutableStateOf("In riproduzione...") }
    var copertinaCanzone by remember { mutableStateOf<Bitmap?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durataCanzone by remember { mutableLongStateOf(0L) }
    var posizioneCorrente by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var activeController by remember { mutableStateOf<MediaController?>(null) }
    val mediaManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        return String.format(Locale.getDefault(), "%02d:%02d", totalSecs / 60, totalSecs % 60)
    }

    LaunchedEffect(Unit) {
        while(true) {
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
                    durataCanzone = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                    if (!isDraggingSlider) posizioneCorrente = playbackState?.position ?: 0L
                }
            } catch (e: Exception) { }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); offsetX += dragAmount.x; offsetY += dragAmount.y } }
            .size(50.dp).clip(CircleShape).background(Tema.colorePrincipale.copy(alpha = 0.9f)).border(2.dp, Color.White, CircleShape).clickable { mostraMiniPlayer = true },
        contentAlignment = Alignment.Center
    ) { Icon(painter = painterResource(id = R.drawable.ic_music_note), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) }

    if (mostraMiniPlayer) {
        Dialog(onDismissRequest = { mostraMiniPlayer = false }) {
            Box(modifier = Modifier.fillMaxWidth(0.95f).clip(RoundedCornerShape(28.dp)).background(Tema.coloreSfondoCard).border(2.dp, Tema.colorePrincipale, RoundedCornerShape(28.dp)).padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray)) {
                        if (copertinaCanzone != null) Image(bitmap = copertinaCanzone!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(painterResource(id = R.drawable.due_contro_due), null, tint = Color.Gray, modifier = Modifier.size(100.dp).align(Alignment.Center))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(titoloCanzone, color = Tema.coloreTesto, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                    Text(artistaCanzone, color = Tema.coloreTestoSecondario, fontSize = 16.sp, maxLines = 1)
                    if (durataCanzone > 0L) {
                        Slider(value = posizioneCorrente.toFloat(), onValueChange = { isDraggingSlider = true; posizioneCorrente = it.toLong() },
                            onValueChangeFinished = { isDraggingSlider = false; activeController?.transportControls?.seekTo(posizioneCorrente) },
                            valueRange = 0f..durataCanzone.toFloat(), colors = SliderDefaults.colors(thumbColor = Tema.coloreTesto, activeTrackColor = Tema.coloreTesto))
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).offset(y = (-10).dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime(posizioneCorrente), color = Tema.coloreTestoSecondario, fontSize = 12.sp)
                            Text(formatTime(durataCanzone), color = Tema.coloreTestoSecondario, fontSize = 12.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { mandaComando(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS) }) { Text("I◀", color = Tema.coloreTesto) }
                        FloatingActionButton(onClick = { mandaComando(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }, containerColor = Tema.colorePrincipale, shape = CircleShape) {
                            Text(if (isPlaying) "❚❚" else "▶", color = Color.White)
                        }
                        IconButton(onClick = { mandaComando(context, KeyEvent.KEYCODE_MEDIA_NEXT) }) { Text("▶I", color = Tema.coloreTesto) }
                    }
                }
            }
        }
    }
}

fun mandaComando(context: Context, key: Int) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
}

class NotificationListener : android.service.notification.NotificationListenerService()

@Composable
fun SchermataDiBenvenuto(onTornaIndietro: () -> Unit, onVaiAlMenu: () -> Unit) {
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
            IconButton(
                onClick = { onTornaIndietro() },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp, start = 16.dp)
            ) {
                Text("<", color = Color.White, fontSize = 45.sp, fontFamily = FontFamily(Font(R.font.komtit__)), fontWeight = FontWeight.Bold)
            }

            Box(modifier = Modifier.offset(y = spostamentoVerticale).size(dimensioneAura).graphicsLayer { scaleX = scalaAura; scaleY = scalaAura; alpha = alphaAura }.background(Color.Black.copy(alpha = 0.4f), CircleShape).border(4.dp, Color.Black.copy(alpha = 0.6f), CircleShape))
        }

        Box(modifier = Modifier.offset(y = spostamentoVerticale).size(dimensioneAreaClick).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!inTransizione) inTransizione = true })

        if (inTransizione) {
            Box(modifier = Modifier.offset(y = spostamentoVerticale).size(dimensionePallinoNero).graphicsLayer { scaleX = scalaEspansione; scaleY = scalaEspansione }.background(Color.Black, CircleShape))
        }
    }
}

@Composable
fun SchermataDiBenvenutoBarreFaul(onTornaIndietro: () -> Unit, onVaiAlMenu: () -> Unit) {
    var inTransizione by remember { mutableStateOf(false) }

    val scalaSfondo by animateFloatAsState(targetValue = if (inTransizione) 2.8f else 1f, animationSpec = tween(1300, easing = FastOutSlowInEasing), label = "")
    val alphaContenuto by animateFloatAsState(targetValue = if (inTransizione) 0f else 1f, animationSpec = tween(700), label = "", finishedListener = { onVaiAlMenu() })

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scalaVt by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!inTransizione) inTransizione = true
            },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scalaSfondo; scaleY = scalaSfondo; alpha = alphaContenuto }) {

            Image(
                painter = painterResource(id = R.drawable.sfondo_schermata_iniziale_barre_faul),
                contentDescription = "Sfondo Barre Faul",
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )

            Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
                TestoBomboletta()
            }

            Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.BottomCenter) {
                Image(
                    painter = painterResource(id = R.drawable.vt),
                    contentDescription = "Logo VT",
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer { scaleX = scalaVt; scaleY = scalaVt }
                )
            }
        }

        if (!inTransizione) {
            IconButton(
                onClick = { onTornaIndietro() },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp, start = 16.dp)
            ) {
                Text("<", color = Color.White, fontSize = 45.sp, fontFamily = FontFamily(Font(R.font.komtit__)), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TestoBomboletta() {
    var visibile by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(500); visibile = true }
    val FontTitoli = FontFamily(Font(R.font.jackboa))

    AnimatedVisibility(visible = visibile, enter = expandHorizontally(animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing), expandFrom = Alignment.Start) + fadeIn(animationSpec = tween(durationMillis = 2000))) {
        Box(contentAlignment = Alignment.Center) {
            Text("BATTLE ROSTER", color = Color.Black, fontSize = 42.sp, fontWeight = FontWeight.Bold, fontFamily = FontTitoli, style = TextStyle(drawStyle = Stroke(miter = 10f, width = 10f, join = StrokeJoin.Round)))
            Text("BATTLE ROSTER", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, fontFamily = FontTitoli)
        }
    }
}

@Composable
fun TestoAnimatoStileMinecraft() {
    val FontTitoli = FontFamily(Font(R.font.jackboa))
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scalaTesto by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "")

    Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer { scaleX = scalaTesto; scaleY = scalaTesto }) {
        Text("WHAT UUU SAYYYNNN!!!", color = Color.Black, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontTitoli, style = TextStyle(drawStyle = Stroke(miter = 10f, width = 12f, join = StrokeJoin.Round)))
        Text("WHAT UUU SAYYYNNN!!!", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontTitoli)
    }
}