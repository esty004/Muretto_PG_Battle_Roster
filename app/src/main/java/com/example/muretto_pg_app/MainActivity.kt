package com.example.muretto_pg_app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
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
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    private val databaseViewModel: DatabaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityGuard.attivaAutodistruzioneSeManomesso(this)
        databaseViewModel.inizializzaSupabase(this)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        if (!isNotificationListenerPermissionGranted(this)) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        setContent {
            MaterialTheme {
                LaunchedEffect(Unit) {
                    databaseViewModel.inizializzaSessione()
                }

                CompositionLocalProvider(LocalDatabaseViewModel provides databaseViewModel) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        AppNavigation()

                        val uiState by databaseViewModel.uiState.collectAsState()
                        uiState.error?.let { errorMsg ->
                            AlertDialog(
                                onDismissRequest = { databaseViewModel.clearError() },
                                containerColor = Tema.coloreSfondoCard,
                                title = { Text("ATTENZIONE", color = Tema.coloreTesto) },
                                text = { Text(errorMsg, color = Tema.coloreTestoSecondario) },
                                confirmButton = {
                                    Button(
                                        colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                                        onClick = { databaseViewModel.clearError() }
                                    ) { Text("OK", color = Color.White) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun isNotificationListenerPermissionGranted(context: Context): Boolean {
    val packageName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(packageName)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val mioFont = FontFamily(Font(R.font.komtit__))
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rottaCorrente = navBackStackEntry?.destination?.route

    var mostraPopupRecupero by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (GestoreBattle.haProgresso(context)) mostraPopupRecupero = true }

    var mostraPopupPermesso by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!isNotificationListenerPermissionGranted(context)) {
            mostraPopupPermesso = true
        }
    }

    if (mostraPopupPermesso) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Tema.coloreSfondoCard,
            title = { Text("PERMESSO PLAYER", color = Tema.coloreTesto, fontFamily = mioFont) },
            text = { Text("Per usare il mini player musicale, abilita l'accesso alle notifiche.", color = Tema.coloreTestoSecondario) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                    onClick = { mostraPopupPermesso = false; context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                ) { Text("ABILITA", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostraPopupPermesso = false }) { Text("DOPO", color = Tema.coloreTestoSecondario) }
            }
        )
    }

    if (mostraPopupRecupero) {
        AlertDialog(
            onDismissRequest = { mostraPopupRecupero = false },
            containerColor = Tema.coloreSfondoCard,
            title = { Text("CONTINUARE BATTLE?", color = Tema.coloreTesto, fontFamily = mioFont) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), shape = CircleShape,
                    onClick = { GestoreBattle.caricaProgresso(context); mostraPopupRecupero = false; navController.navigate("ottavi") }
                ) { Text("SÌ", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { GestoreBattle.pulisciSalvataggio(context); mostraPopupRecupero = false }) { Text("NO", color = Tema.coloreTestoSecondario) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {

            composable("home") { SchermataHome(onNavigate = { navController.navigate(it) }) }
            composable("mappa") { SchermataMappa(onPinClick = { navController.navigate(it) }, onTornaIndietro = { navController.popBackStack() }) }
            composable("login") { SchermataLogin(onLoginSuccess = { navController.popBackStack() }, onTornaIndietro = { navController.popBackStack() }, onVaiARegistrazione = { navController.navigate("registrazione") }) }
            composable("registrazione") { SchermataRegistrazione(onTornaIndietro = { navController.popBackStack() }) }
            composable("notifiche") { SchermataNotifiche(onTornaIndietro = { navController.popBackStack() }) }
            composable("aggiungi_mc") { SchermataAggiungiMc(onTornaIndietro = { navController.popBackStack() }) }
            composable("aggiungi_evento") { SchermataAggiungiEvento(onTornaIndietro = { navController.popBackStack() }) }

            composable("benvenuto") { SchermataDiBenvenuto(onTornaIndietro = { navController.popBackStack() }, onVaiAlMenu = { navController.navigate("menu") }) }
            composable("benvenuto_barre_faul") { SchermataDiBenvenutoBarreFaul(onTornaIndietro = { navController.popBackStack() }, onVaiAlMenu = { navController.navigate("menu") }) }
            composable("benvenuto_ateneo") { SchermataDiBenvenutoAteneo(onTornaIndietro = { navController.popBackStack() }, onVaiAlMenu = { navController.navigate("menu") }) }
            composable("benvenuto_grosseto") { SchermataDiBenvenutoGrosseto(onTornaIndietro = { navController.popBackStack() }, onVaiAlMenu = { navController.navigate("menu") }) }
            
            composable("menu") { SchermataMenu(onTornaIndietro = { navController.popBackStack() }, onSelezionaModalita = { navController.navigate(it) }) }

            composable("trasferte") {
                SchermataTrasferte(
                    onTornaIndietro = { navController.popBackStack() },
                    onVaiAllaMappa = { navController.navigate("mappa_trasferte") },
                    onGestisciBattle = { eventoId -> navController.navigate("gestione_battle_evento/$eventoId") }
                )
            }
            composable("mappa_trasferte") { SchermataMappaTrasferte(onTornaIndietro = { navController.popBackStack() }) }
            composable("trasferte_preferite") { 
                SchermataTrasferte(
                    onTornaIndietro = { navController.popBackStack() }, 
                    onVaiAllaMappa = { navController.navigate("mappa_trasferte") }, 
                    onGestisciBattle = { eventoId -> navController.navigate("gestione_battle_evento/$eventoId") },
                    soloPreferiti = true
                ) 
            }

            composable("gestione_mcs") { SchermataGestioneMcs(onTornaIndietro = { navController.popBackStack() }, onModificaMc = { id -> navController.navigate("modifica_mc/$id") }, onAggiungiMc = { navController.navigate("aggiungi_mc") }) }
            composable("modifica_mc/{id}") { backStackEntry -> val mcId = backStackEntry.arguments?.getString("id") ?: ""; SchermataModificaMc(mcId = mcId, onTornaIndietro = { navController.popBackStack() }) }

            composable("muretto_classico") {
                SchermataMurettoClassico(
                    tipoTorneo = TipoTorneo.SINGOLO,
                    onTornaAlMenu = { navController.popBackStack() },
                    onIniziaBattle = { GestoreBattle.iniziaTorneo(GestoreBattle.mcsSelezionati); navController.navigate("ottavi") }
                )
            }
            composable("due_contro_due/{tipo}") { backStackEntry ->
                val tipoStr = backStackEntry.arguments?.getString("tipo") ?: TipoTorneo.COPPIE_CASUALI.name
                SchermataMurettoClassico(
                    tipoTorneo = TipoTorneo.valueOf(tipoStr),
                    onTornaAlMenu = { navController.popBackStack() },
                    onIniziaBattle = { GestoreBattle.iniziaTorneo2v2(GestoreBattle.mcsSelezionati, TipoTorneo.valueOf(tipoStr)); navController.navigate("ottavi") }
                )
            }
            composable("ottavi") { SchermataOttavi(onTornaIndietro = { navController.popBackStack() }, onVaiAiQuarti = { }, onRoundClick = { navController.navigate("round_singolo/$it") }) }
            composable("round_singolo/{roundId}") { backStackEntry -> SchermataRoundSingolo(roundId = backStackEntry.arguments?.getString("roundId") ?: "", onTornaIndietro = { navController.popBackStack() }) }

            composable("allenamento") { SchermataAllenamento(onTornaIndietro = { navController.popBackStack() }, onSelezionaAllenamento = { navController.navigate(it.lowercase().replace(" ", "_")) }) }
            composable("generatore_argomenti") { SchermataGeneratoreArgomenti { navController.popBackStack() } }
            composable("generatore_modalita") { SchermataGeneratoreModalita { navController.popBackStack() } }
            composable("generatore_parole") { SchermataGeneratoreParole { navController.popBackStack() } }
            composable("generatore_taboo") { SchermataGeneratoreTaboo { navController.popBackStack() } }
            composable("generatore_linker") { SchermataGeneratoreLinker { navController.popBackStack() } }
            composable("generatore_immagini") { SchermataGeneratoreImmagini { navController.popBackStack() } }
            
            composable("evento") {
                SchermataEventi(
                    onTornaIndietro = { navController.popBackStack() },
                    onNavigate = { navController.navigate(it) }
                )
            }
            composable("gestione_battle_evento/{eventoId}") { backStackEntry ->
                val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
                SchermataGestioneBattleEvento(eventoId = eventoId, onTornaIndietro = { navController.popBackStack() })
            }
            composable("contest") {
                SchermataContest(
                    isGlobale = false,
                    onTornaIndietro = { navController.popBackStack() },
                    onNavigate = { navController.navigate(it) }
                )
            }
            composable("dettaglio_contest/{id}") { backStackEntry ->
                val contestId = backStackEntry.arguments?.getString("id") ?: ""
                SchermataGestioneBattleEvento(eventoId = contestId, onTornaIndietro = { navController.popBackStack() })
            }
            composable("aggiungi_contest") {
                SchermataCreaContest(onTornaIndietro = { navController.popBackStack() })
            }
            composable("lista_contest_globali") {
                SchermataContest(
                    isGlobale = true,
                    onTornaIndietro = { navController.popBackStack() },
                    onNavigate = { navController.navigate(it) }
                )
            }
            composable("trasferte_focus/{eventoId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("eventoId")
                SchermataTrasferte(
                    onTornaIndietro = { navController.popBackStack() },
                    onVaiAllaMappa = { navController.navigate("mappa_trasferte") },
                    onGestisciBattle = { e -> navController.navigate("gestione_battle_evento/$e") },
                    eventoIdIniziale = id
                )
            }
            composable("config_battle/{eventoId}") { backStackEntry ->
                val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
                val vm = LocalDatabaseViewModel.current
                val stile = StileContest.risolvi(vm.eventiApprovati.find { it.id == eventoId }?.contest_design)
                LaunchedEffect(eventoId) { if (GestoreContestBattle.eventoIdCorrente != eventoId) GestoreContestBattle.caricaDalCloud(vm.supabase, eventoId) }
                SchermataConfigBattle(
                    stile = stile,
                    onProsegui = { navController.navigate("roster_contest/$eventoId") },
                    onTornaIndietro = { navController.popBackStack() }
                )
            }
            composable("roster_contest/{eventoId}") { backStackEntry ->
                val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
                val vm = LocalDatabaseViewModel.current
                val scope = rememberCoroutineScope()
                val stile = StileContest.risolvi(vm.eventiApprovati.find { it.id == eventoId }?.contest_design)
                SchermataRosterContest(
                    stile = stile,
                    onProsegui = {
                        if (GestoreContestBattle.accoppiamenti == "casuali") {
                            GestoreContestBattle.generaCasuale()
                            scope.launch { GestoreContestBattle.salvaSulCloud(vm.supabase, "configurato") }
                            navController.popBackStack("config_battle/$eventoId", inclusive = true)
                        } else {
                            navController.navigate("builder_contest/$eventoId")
                        }
                    },
                    onTornaIndietro = { navController.popBackStack() }
                )
            }
            composable("builder_contest/{eventoId}") { backStackEntry ->
                val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
                val vm = LocalDatabaseViewModel.current
                val scope = rememberCoroutineScope()
                val stile = StileContest.risolvi(vm.eventiApprovati.find { it.id == eventoId }?.contest_design)
                SchermataBuilderContest(
                    stile = stile,
                    onNuovoRound = { roundId -> navController.navigate("costruzione_round/$eventoId/$roundId") },
                    onRoundClick = { roundId -> navController.navigate("costruzione_round/$eventoId/$roundId") },
                    onConferma = { scope.launch { GestoreContestBattle.salvaSulCloud(vm.supabase, "configurato") }; navController.popBackStack() },
                    onTornaIndietro = { scope.launch { GestoreContestBattle.salvaSulCloud(vm.supabase) }; navController.popBackStack() }
                )
            }
            composable("costruzione_round/{eventoId}/{roundId}") { backStackEntry ->
                val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
                val roundId = backStackEntry.arguments?.getString("roundId") ?: ""
                val vm = LocalDatabaseViewModel.current
                val scope = rememberCoroutineScope()
                val stile = StileContest.risolvi(vm.eventiApprovati.find { it.id == eventoId }?.contest_design)
                SchermataCostruzioneRound(
                    roundId = roundId,
                    stile = stile,
                    onFatto = { scope.launch { GestoreContestBattle.salvaSulCloud(vm.supabase) }; navController.popBackStack() },
                    onTornaIndietro = { navController.popBackStack() }
                )
            }
            composable("battle_live/{eventoId}/{ruolo}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("eventoId") ?: ""
                val ruolo = backStackEntry.arguments?.getString("ruolo") ?: "spettatore"
                SchermataBattleLive(
                    eventoId = id,
                    ruolo = ruolo,
                    onEsci = { navController.popBackStack() },
                    onProssimaFase = { navController.navigate("builder_contest/$id") }
                )
            }
        }

        val schermateSenzaPlayer = setOf("home", "benvenuto", "benvenuto_barre_faul", "benvenuto_ateneo", "benvenuto_grosseto", "mappa", "login", "aggiungi_mc", "aggiungi_evento", "trasferte", "trasferte_preferite", "mappa_trasferte", "registrazione", "gestione_mcs", "modifica_mc")
        if (rottaCorrente != null && !schermateSenzaPlayer.any { rottaCorrente.startsWith(it) }) {
            FloatingPlayer()
        }
    }
}

// ─── SCHERMATA HOME ───

@Composable
fun SchermataHome(onNavigate: (String) -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val numNotifiche = databaseViewModel.richiesteInAttesa.size + databaseViewModel.eventiInAttesa.size
    val scope = rememberCoroutineScope()
    var mostraMenuAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Tema.murettoSelezionato = MurettoAttivo.PG
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, end = 20.dp), contentAlignment = Alignment.TopEnd) {
            Box {
                Image(
                    painter = painterResource(id = R.drawable.login_logo), contentDescription = "Profilo",
                    modifier = Modifier.size(45.dp).clip(CircleShape).clickable {
                        if (databaseViewModel.ruoloAttuale != RuoloUtente.NESSUNO) mostraMenuAdmin = true
                        else onNavigate("login")
                    }
                )
                if (numNotifiche > 0 && databaseViewModel.isAdmin) {
                    Box(
                        modifier = Modifier.size(16.dp).background(Color.Red, CircleShape).align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) { Text(numNotifiche.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Box(contentAlignment = Alignment.Center) {
                Text(
                    "FREESTAPP",
                    color = Color.Black,
                    fontSize = 45.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.komtit__)),
                    style = TextStyle(drawStyle = Stroke(miter = 10f, width = 12f, join = StrokeJoin.Round))
                )
                Text(
                    "FREESTAPP",
                    color = Color.White,
                    fontSize = 45.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.komtit__))
                )
            }

            Spacer(modifier = Modifier.weight(0.8f))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(135.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(3.dp, Tema.colorePrincipale, RoundedCornerShape(24.dp))
                    .clickable { onNavigate("mappa") },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "MURETTI", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.komtit__)), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(135.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(3.dp, Tema.colorePrincipale, RoundedCornerShape(24.dp))
                    .clickable { onNavigate("trasferte") },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.trasferte),
                    contentDescription = "Trasferte Sfondo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                Text(text = "TRASFERTE", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.komtit__)), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(135.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(3.dp, Tema.colorePrincipale, RoundedCornerShape(24.dp))
                    .clickable {
                        Tema.murettoSelezionato = MurettoAttivo.PG
                        onNavigate("allenamento")
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.allenamento),
                    contentDescription = "Allenamento Sfondo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                Text(text = "ALLENAMENTO", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.komtit__)), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.weight(1.2f))
        }

        if (mostraMenuAdmin) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { mostraMenuAdmin = false })
        }
        AnimatedVisibility(
            visible = mostraMenuAdmin, enter = slideInHorizontally(initialOffsetX = { it }), exit = slideOutHorizontally(targetOffsetX = { it }), modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            MenuLaterale(onNavigate = { mostraMenuAdmin = false; onNavigate(it) }, onChiudi = { mostraMenuAdmin = false }, scope = scope)
        }
    }
}

@Composable
fun MenuLaterale(onNavigate: (String) -> Unit, onChiudi: () -> Unit, scope: kotlinx.coroutines.CoroutineScope) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val numNotifiche = databaseViewModel.richiesteInAttesa.size + databaseViewModel.eventiInAttesa.size
    Column(
        modifier = Modifier.fillMaxHeight().fillMaxWidth(0.7f).background(Tema.coloreSfondoCard)
            .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Tema.colorePrincipale), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(id = R.drawable.login_logo), contentDescription = null, modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White).padding(10.dp))
                Spacer(modifier = Modifier.height(8.dp))
                val etichettaPanel = when (databaseViewModel.ruoloAttuale) {
                    RuoloUtente.ADMIN -> "ADMIN PANEL"
                    RuoloUtente.ORGANIZZATORE_MURETTO -> "ORG. MURETTO"
                    RuoloUtente.ORGANIZZATORE_EVENTI -> "ORG. EVENTI"
                    RuoloUtente.RAPPER -> "RAPPER"
                    else -> "PANNELLO"
                }
                Text(etichettaPanel, color = Color.White, fontSize = 20.sp, fontFamily = FontFamily(Font(R.font.komtit__)), fontWeight = FontWeight.Bold)
                databaseViewModel.profiloAttuale?.nome_arte?.let { Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (databaseViewModel.isAdmin || databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_MURETTO) {
            MenuItem(titolo = "GESTIONE MC'S", icona = R.drawable.add) { onNavigate("gestione_mcs") }
        }
        if (databaseViewModel.isAdmin || databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_MURETTO || databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_EVENTI) {
            MenuItem(titolo = "AGGIUNGI EVENTO", icona = R.drawable.evento_icon) { onNavigate("aggiungi_evento") }
        }
        if (databaseViewModel.ruoloAttuale != RuoloUtente.NESSUNO) {
            MenuItem(titolo = "TRASFERTE PREFERITE", icona = R.drawable.star_favorite) { onNavigate("trasferte_preferite") }
        }
        if (databaseViewModel.isAdmin) {
            Box(modifier = Modifier.fillMaxWidth()) {
                MenuItem(titolo = "NOTIFICHE", icona = R.drawable.notice) { onNavigate("notifiche") }
                if (numNotifiche > 0) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(22.dp).background(Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                        Text(numNotifiche.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        MenuItem(titolo = "LOGOUT", icona = R.drawable.versus, coloreTesto = Color.Red) {
            onChiudi()
            scope.launch {
                databaseViewModel.supabase.auth.clearSession()
                databaseViewModel.isAdmin = false
                databaseViewModel.ruoloAttuale = RuoloUtente.NESSUNO
                databaseViewModel.profiloAttuale = null
                databaseViewModel.eventiPreferiti.clear()
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ─── FLOATING PLAYER ───

@Composable
fun FloatingPlayer() {
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
    var permessoMancante by remember { mutableStateOf(false) }

    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        return String.format(Locale.getDefault(), "%02d:%02d", totalSecs / 60, totalSecs % 60)
    }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                if (!isNotificationListenerPermissionGranted(context)) {
                    permessoMancante = true
                } else {
                    permessoMancante = false
                    val mediaManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
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
                    } else {
                        titoloCanzone = "Nessun Beat"; artistaCanzone = "Avvia la musica"; copertinaCanzone = null; isPlaying = false; durataCanzone = 0L; posizioneCorrente = 0L; activeController = null
                    }
                }
            } catch (e: Exception) { }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - 150f)
                    offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx - 150f)
                }
            }
            .size(50.dp)
            .clip(CircleShape)
            .background(if (isPlaying) Tema.colorePrincipale.copy(alpha = 0.95f) else Tema.colorePrincipale.copy(alpha = 0.6f))
            .border(2.dp, Color.White, CircleShape)
            .clickable { mostraMiniPlayer = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_music_note), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }

    if (mostraMiniPlayer) {
        Dialog(onDismissRequest = { mostraMiniPlayer = false }) {
            Box(
                modifier = Modifier.fillMaxWidth(0.95f).clip(RoundedCornerShape(28.dp))
                    .background(Tema.coloreSfondoCard).border(2.dp, Tema.colorePrincipale, RoundedCornerShape(28.dp)).padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (permessoMancante) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Icon(painterResource(id = R.drawable.ic_music_note), contentDescription = null, tint = Tema.coloreTestoSecondario, modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Permesso mancante", color = Tema.coloreTesto, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Abilita l'accesso alle notifiche.", color = Tema.coloreTestoSecondario, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { mostraMiniPlayer = false; context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                        ) { Text("ABILITA", color = Color.White, fontWeight = FontWeight.Bold) }
                    } else {
                        Box(modifier = Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray)) {
                            if (copertinaCanzone != null) Image(bitmap = copertinaCanzone!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(painterResource(id = R.drawable.ic_music_note), contentDescription = null, tint = Color.Gray, modifier = Modifier.size(80.dp).align(Alignment.Center))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(titoloCanzone, color = Tema.coloreTesto, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                        Text(artistaCanzone, color = Tema.coloreTestoSecondario, fontSize = 15.sp, maxLines = 1, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (durataCanzone > 0L) {
                            Slider(
                                value = posizioneCorrente.toFloat(),
                                onValueChange = { isDraggingSlider = true; posizioneCorrente = it.toLong() },
                                onValueChangeFinished = { isDraggingSlider = false; activeController?.transportControls?.seekTo(posizioneCorrente) },
                                valueRange = 0f..durataCanzone.toFloat(),
                                colors = SliderDefaults.colors(thumbColor = Tema.colorePrincipale, activeTrackColor = Tema.colorePrincipale)
                            )
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).offset(y = (-8).dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatTime(posizioneCorrente), color = Tema.coloreTestoSecondario, fontSize = 12.sp)
                                Text(formatTime(durataCanzone), color = Tema.coloreTestoSecondario, fontSize = 12.sp)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { mandaComando(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS) }) { Text("⏮", color = Tema.coloreTesto, fontSize = 24.sp) }
                            FloatingActionButton(onClick = { mandaComando(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }, containerColor = Tema.colorePrincipale, shape = CircleShape, modifier = Modifier.size(64.dp)) {
                                Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 26.sp)
                            }
                            IconButton(onClick = { mandaComando(context, KeyEvent.KEYCODE_MEDIA_NEXT) }) { Text("⏭", color = Tema.coloreTesto, fontSize = 24.sp) }
                        }
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

// ─── MAPPA MURETTI ───

@Composable
fun SchermataMappa(onPinClick: (String) -> Unit, onTornaIndietro: () -> Unit) {
    val italyBounds = BoundingBox(47.1, 18.3, 35.5, 6.6)
    val centroMappa = GeoPoint(42.764, 12.244)
    val mioFont = FontFamily(Font(R.font.komtit__))

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
                        0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                        -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                    ))
                    overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(trueDarkModeMatrix))

                    // 1. Muretto PG
                    overlays.add(Marker(this).apply {
                        position = GeoPoint(43.112056, 12.388439)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        val bmp = android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.pin_muretto_pg)
                        icon = BitmapDrawable(ctx.resources, Bitmap.createScaledBitmap(bmp, 200, 140, true))
                        setOnMarkerClickListener { _, _ -> Tema.murettoSelezionato = MurettoAttivo.PG; onPinClick("benvenuto"); true }
                    })

                    // 2. Barre Faul
                    overlays.add(Marker(this).apply {
                        position = GeoPoint(42.416669, 12.100123)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        val bmp = android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.pin_barre_faul)
                        icon = BitmapDrawable(ctx.resources, Bitmap.createScaledBitmap(bmp, 200, 140, true))
                        setOnMarkerClickListener { _, _ -> Tema.murettoSelezionato = MurettoAttivo.BARRE_FAUL; onPinClick("benvenuto_barre_faul"); true }
                    })

                    // 3. Ateneo
                    overlays.add(Marker(this).apply {
                        position = GeoPoint(41.878340, 12.521054)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        val bmp = android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.pin_ateneo)
                        icon = BitmapDrawable(ctx.resources, Bitmap.createScaledBitmap(bmp, 200, 140, true))
                        setOnMarkerClickListener { _, _ -> Tema.murettoSelezionato = MurettoAttivo.ATENEO; onPinClick("benvenuto_ateneo"); true }
                    })

                    // 4. Grosseto
                    overlays.add(Marker(this).apply {
                        position = GeoPoint(42.76, 11.11)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        val bmp = android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.pin_grosseto)
                        // Ridotto la larghezza da 200 a 140 per correggere lo schiacciamento
                        icon = BitmapDrawable(ctx.resources, Bitmap.createScaledBitmap(bmp, 140, 140, true))
                        setOnMarkerClickListener { _, _ -> Tema.murettoSelezionato = MurettoAttivo.GROSSETO; onPinClick("benvenuto_grosseto"); true }
                    })
                }
            }
        )

        FloatingActionButton(
            onClick = { onTornaIndietro() }, containerColor = Color.DarkGray, contentColor = Color.White, shape = CircleShape,
            modifier = Modifier.padding(top = 40.dp, start = 16.dp)
        ) { Text("<", fontSize = 30.sp, fontFamily = mioFont, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun MenuItem(titolo: String, icona: Int, coloreTesto: Color = Tema.coloreTesto, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(id = icona), contentDescription = null, tint = coloreTesto, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = titolo, color = coloreTesto, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.komtit__)))
    }
}

// ─── SCHERMATE DI BENVENUTO ───

@Composable
fun SchermataDiBenvenuto(onTornaIndietro: () -> Unit, onVaiAlMenu: () -> Unit) {
    var inTransizione by remember { mutableStateOf(false) }
    val scalaSfondo by animateFloatAsState(targetValue = if (inTransizione) 2.8f else 1f, animationSpec = tween(1300, easing = FastOutSlowInEasing))
    val scalaEspansione by animateFloatAsState(targetValue = if (inTransizione) 40f else 0f, animationSpec = tween(1100, easing = FastOutSlowInEasing), finishedListener = { onVaiAlMenu() })
    val alphaContenuto by animateFloatAsState(targetValue = if (inTransizione) 0f else 1f, animationSpec = tween(700))

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scalaSfondo; scaleY = scalaSfondo; alpha = alphaContenuto }) {
            Image(painter = painterResource(id = R.drawable.sfondo_schermata_iniziale), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (!inTransizione) {
            IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp, start = 16.dp)) {
                Text("<", color = Color.White, fontSize = 45.sp, fontFamily = FontFamily(Font(R.font.komtit__)), fontWeight = FontWeight.Bold)
            }
        }
        Box(modifier = Modifier.offset(y = (-50).dp).size(250.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!inTransizione) inTransizione = true })
        if (inTransizione) Box(modifier = Modifier.offset(y = (-50).dp).size(90.dp).graphicsLayer { scaleX = scalaEspansione; scaleY = scalaEspansione }.background(Color.Black, CircleShape))
    }
}

@Composable
fun SchermataDiBenvenutoBarreFaul(onTornaIndietro: () -> Unit, onVaiAlMenu: () -> Unit) {
    var inTransizione by remember { mutableStateOf(false) }
    val scalaSfondo by animateFloatAsState(targetValue = if (inTransizione) 2.8f else 1f, animationSpec = tween(1300, easing = FastOutSlowInEasing))
    val alphaContenuto by animateFloatAsState(targetValue = if (inTransizione) 0f else 1f, animationSpec = tween(700), finishedListener = { onVaiAlMenu() })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!inTransizione) inTransizione = true }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scalaSfondo; scaleY = scalaSfondo; alpha = alphaContenuto }) {
            Image(painter = painterResource(id = R.drawable.sfondo_schermata_iniziale_barre_faul), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (!inTransizione) {
            IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp, start = 16.dp)) {
                Text("<", color = Color.White, fontSize = 45.sp, fontFamily = FontFamily(Font(R.font.komtit__)), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SchermataDiBenvenutoAteneo(onTornaIndietro: () -> Unit, onVaiAlMenu: () -> Unit) {
    var inTransizione by remember { mutableStateOf(false) }
    val scalaSfondo by animateFloatAsState(targetValue = if (inTransizione) 2.8f else 1f, animationSpec = tween(1300, easing = FastOutSlowInEasing))
    val alphaContenuto by animateFloatAsState(targetValue = if (inTransizione) 0f else 1f, animationSpec = tween(700), finishedListener = { onVaiAlMenu() })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!inTransizione) inTransizione = true }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scalaSfondo; scaleY = scalaSfondo; alpha = alphaContenuto }) {
            Image(painter = painterResource(id = R.drawable.sfondo_schermata_iniziale_ateneo), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (!inTransizione) {
            IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp, start = 16.dp)) {
                Text("<", color = Color.White, fontSize = 45.sp, fontFamily = FontFamily(Font(R.font.komtit__)), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SchermataDiBenvenutoGrosseto(onTornaIndietro: () -> Unit, onVaiAlMenu: () -> Unit) {
    var inTransizione by remember { mutableStateOf(false) }
    val scalaSfondo by animateFloatAsState(targetValue = if (inTransizione) 2.8f else 1f, animationSpec = tween(1300, easing = FastOutSlowInEasing))
    val alphaContenuto by animateFloatAsState(targetValue = if (inTransizione) 0f else 1f, animationSpec = tween(700), finishedListener = { onVaiAlMenu() })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!inTransizione) inTransizione = true }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scalaSfondo; scaleY = scalaSfondo; alpha = alphaContenuto }) {
            Image(painter = painterResource(id = R.drawable.sfondo_schermata_iniziale_grosseto), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (!inTransizione) {
            IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp, start = 16.dp)) {
                Text("<", color = Color.White, fontSize = 45.sp, fontFamily = FontFamily(Font(R.font.komtit__)), fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ─── SICUREZZA ───

object SecurityGuard {
    private const val FIRMA_AUTORIZZATA_PRODUZIONE = "1932025762"
    fun attivaAutodistruzioneSeManomesso(context: Context) {
        if (isDebuggerConnesso() || isFirmaManomessa(context)) {
            android.util.Log.e("SECURITY", "☠️ MANOMISSIONE RILEVATA! ☠️")
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(1)
        }
    }
    private fun isDebuggerConnesso(): Boolean = android.os.Debug.isDebuggerConnected() || android.os.Debug.waitingForDebugger()
    private fun isFirmaManomessa(context: Context): Boolean {
        try {
            val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebuggable) return false
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            val signatures = packageInfo.signatures
            if (signatures == null || signatures.isEmpty()) return true
            if (signatures[0].hashCode().toString() != FIRMA_AUTORIZZATA_PRODUZIONE) return true
        } catch (e: Exception) { return true }
        return false
    }
}
