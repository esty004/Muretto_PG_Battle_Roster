package com.example.muretto_pg_app

import android.os.Bundle
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurazione Full Screen (Immersiva)
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

/**
 * Gestore principale della navigazione dell'app.
 * Include il controllo per il recupero di sessioni di torneo interrotte.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var mostraPopupRecupero by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))

    // Controllo all'avvio: se esiste un torneo salvato, chiede se riprenderlo
    LaunchedEffect(Unit) {
        if (GestoreBattle.haProgresso(context)) {
            mostraPopupRecupero = true
        }
    }

    if (mostraPopupRecupero) {
        AlertDialog(
            onDismissRequest = { mostraPopupRecupero = false },
            containerColor = Color(0xFF222222),
            title = { Text("CONTINUARE BATTLE?", color = Color.White, fontFamily = MioFontPersonalizzato) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        GestoreBattle.caricaProgresso(context)
                        mostraPopupRecupero = false
                        navController.navigate("ottavi") 
                    }) { Text("SÌ", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    GestoreBattle.pulisciSalvataggio(context)
                    mostraPopupRecupero = false
                }) { Text("NO", color = Color.Gray) }
            }
        )
    }

    NavHost(navController = navController, startDestination = "benvenuto") {
        // Schermata Iniziale con logo animato
        composable("benvenuto") {
            SchermataDiBenvenuto(onVaiAlMenu = { navController.navigate("menu") })
        }

        // Menu Principale
        composable("menu") {
            SchermataMenu(
                onTornaIndietro = { navController.popBackStack() },
                onSelezionaModalita = { nome ->
                    when (nome) {
                        "Muretto classico" -> navController.navigate("muretto_classico")
                        "2 VS 2" -> navController.navigate("due_contro_due")
                        "Allenamento" -> navController.navigate("allenamento")
                    }
                }
            )
        }

        // Selezione MC per 2vs2
        composable("due_contro_due") {
            SchermataMurettoClassico(
                onTornaAlMenu = { navController.popBackStack() },
                onIniziaBattle = {
                    GestoreBattle.iniziaTorneo2v2(GestoreBattle.mcsSelezionati)
                    navController.navigate("ottavi")
                }
            )
        }

        // Sezione Allenamento (Matchmaking + Generatori)
        composable("allenamento") {
            SchermataAllenamento(
                onTornaIndietro = { navController.popBackStack() },
                onSelezionaAllenamento = { nome ->
                    when (nome) {
                        "Generatore di argomenti" -> navController.navigate("generatore_argomenti")
                        "Generatore di modalita" -> navController.navigate("generatore_modalita")
                        "Generatore di parole" -> navController.navigate("generatore_parole")
                    }
                }
            )
        }

        // Route per i vari generatori
        composable("generatore_argomenti") {
            SchermataGeneratoreArgomenti(onTornaIndietro = { navController.popBackStack() })
        }

        composable("generatore_modalita") {
            SchermataGeneratoreModalita(onTornaIndietro = { navController.popBackStack() })
        }

        composable("generatore_parole") {
            SchermataGeneratoreParole(onTornaIndietro = { navController.popBackStack() })
        }

        // Selezione MC per Torneo Classico
        composable("muretto_classico") {
            SchermataMurettoClassico(
                onTornaAlMenu = { navController.popBackStack() },
                onIniziaBattle = {
                    // Logica intelligente: sceglie automaticamente se partire da Ottavi, Quarti o Finale
                    GestoreBattle.iniziaTorneo(GestoreBattle.mcsSelezionati)
                    navController.navigate("ottavi")
                }
            )
        }

        // Tabellone del Torneo
        composable("ottavi") {
            SchermataOttavi(
                onTornaIndietro = { navController.popBackStack() },
                onVaiAiQuarti = { /* Gestito internamente tramite GestoreBattle */ },
                onRoundClick = { roundId ->
                    navController.navigate("round_singolo/$roundId")
                }
            )
        }

        // Dettaglio della singola Battle
        composable("round_singolo/{roundId}") { backStackEntry ->
            val roundId = backStackEntry.arguments?.getString("roundId") ?: ""
            SchermataRoundSingolo(
                roundId = roundId,
                onTornaIndietro = { navController.popBackStack() }
            )
        }
    }
}


/**
 * Schermata d'apertura cinematografica.
 * Implementa una transizione a "Buco Nero" che inghiotte il logo quando cliccato.
 */
@Composable
fun SchermataDiBenvenuto(onVaiAlMenu: () -> Unit) {
    var inTransizione by remember { mutableStateOf(false) }

    // Animazione Zoom Sfondo
    val scalaSfondo by animateFloatAsState(
        targetValue = if (inTransizione) 2.8f else 1f,
        animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
        label = "zoom_sfondo"
    )

    // Animazione Espansione Buco Nero (centrata sul logo)
    val scalaEspansione by animateFloatAsState(
        targetValue = if (inTransizione) 40f else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "espansione_buco",
        finishedListener = { onVaiAlMenu() } // Naviga solo quando l'animazione è finita
    )

    // Dissolvenza contenuti
    val alphaContenuto by animateFloatAsState(
        targetValue = if (inTransizione) 0f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "dissolvenza_contenuto"
    )

    // Aura pulsante attorno al logo (Feedback interattivo)
    val infiniteTransition = rememberInfiniteTransition(label = "aura_pulsante")
    val scalaAura by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "scala_aura"
    )
    val alphaAura by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "alpha_aura"
    )

    // Offset calcolato per centrare l'animazione sul logo della giacca (y = -72dp)
    val offsetLogo = (-72).dp 

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 1. Elementi Visuali: Sfondo + Testi
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scalaSfondo
                scaleY = scalaSfondo
                alpha = alphaContenuto
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.sfondo_schermata_iniziale),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Titolo Superiore
            Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
                TestoBomboletta()
            }

            // Call to action inferiore
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.BottomCenter) {
                TestoAnimatoStileMinecraft()
            }
        }

        // 2. Aura Pulsante (Centrata sul logo)
        if (!inTransizione) {
            Box(
                modifier = Modifier
                    .offset(y = offsetLogo)
                    .size(280.dp)
                    .graphicsLayer {
                        scaleX = scalaAura
                        scaleY = scalaAura
                        alpha = alphaAura
                    }
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape) 
                    .border(4.dp, Color.Black.copy(alpha = 0.6f), CircleShape) 
            )
        }

        // 3. Area di click invisibile sul logo
        Box(
            modifier = Modifier
                .offset(y = offsetLogo)
                .size(250.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!inTransizione) inTransizione = true
                }
        )

        // 4. L'effetto Buco Nero che copre tutto
        if (inTransizione) {
            Box(
                modifier = Modifier
                    .offset(y = offsetLogo)
                    .size(90.dp)
                    .graphicsLayer {
                        scaleX = scalaEspansione
                        scaleY = scalaEspansione
                    }
                    .background(Color.Black, CircleShape)
            )
        }
    }
}

@Composable
fun TestoBomboletta() {
    var visibile by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(500); visibile = true }
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))

    AnimatedVisibility(
        visible = visibile,
        enter = expandHorizontally(animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing), expandFrom = Alignment.Start) + fadeIn(animationSpec = tween(durationMillis = 2000))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "BATTLE ROSTER",
                color = Color.Black,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MioFontPersonalizzato,
                style = TextStyle(drawStyle = Stroke(miter = 10f, width = 10f, join = StrokeJoin.Round))
            )
            Text(
                "BATTLE ROSTER",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MioFontPersonalizzato
            )
        }
    }
}

@Composable
fun TestoAnimatoStileMinecraft() {
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))
    val infiniteTransition = rememberInfiniteTransition(label = "animazione_pulsante")
    val scalaTesto by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "scala_testo"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.graphicsLayer { scaleX = scalaTesto; scaleY = scalaTesto }
    ) {
        Text("WHAT UUU SAYYYNNN!!!", color = Color.Black, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato, style = TextStyle(drawStyle = Stroke(miter = 10f, width = 12f, join = StrokeJoin.Round)))
        Text("WHAT UUU SAYYYNNN!!!", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato)
    }
}
