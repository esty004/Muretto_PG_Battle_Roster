package com.example.muretto_pg_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
    var mostraPopupRecupero by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))

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
                        navController.navigate("ottavi") // (La rotta di navigazione si chiama sempre così, ma visualizzerà la fase corretta)
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
        composable("benvenuto") {
            SchermataDiBenvenuto(onVaiAlMenu = { navController.navigate("menu") })
        }

        composable("menu") {
            SchermataMenu(
                onTornaIndietro = { navController.popBackStack() },
                onSelezionaModalita = { nome ->
                    if (nome == "Muretto classico") {
                        navController.navigate("muretto_classico")
                    } else if (nome == "Allenamento") {
                        navController.navigate("allenamento")
                    }
                }
            )
        }

        composable("allenamento") {
            SchermataAllenamento(
                onTornaIndietro = { navController.popBackStack() },
                onSelezionaAllenamento = { nome ->
                    when (nome) {
                        "Generatore di argomenti" -> navController.navigate("generatore_argomenti")
                        "Generatore di modalità" -> navController.navigate("generatore_modalita")
                        "Generatore di parole" -> navController.navigate("generatore_parole")
                    }
                }
            )
        }

        composable("generatore_argomenti") {
            SchermataGeneratoreArgomenti(onTornaIndietro = { navController.popBackStack() })
        }

        composable("generatore_modalita") {
            SchermataGeneratoreModalita(onTornaIndietro = { navController.popBackStack() })
        }

        composable("generatore_parole") {
            SchermataGeneratoreParole(onTornaIndietro = { navController.popBackStack() })
        }

        composable("muretto_classico") {
            SchermataMurettoClassico(
                onTornaAlMenu = { navController.popBackStack() },
                onIniziaBattle = {
                    // --- ORA USA LA LOGICA INTELLIGENTE PER SCEGLIERE LA FASE ---
                    GestoreBattle.iniziaTorneo(GestoreBattle.mcsSelezionati)
                    navController.navigate("ottavi")
                }
            )
        }

        composable("ottavi") {
            SchermataOttavi(
                onTornaIndietro = { navController.popBackStack() },
                onVaiAiQuarti = { /* Gestito internamente */ },
                onRoundClick = { roundId ->
                    navController.navigate("round_singolo/$roundId")
                }
            )
        }

        composable("round_singolo/{roundId}") { backStackEntry ->
            val roundId = backStackEntry.arguments?.getString("roundId") ?: ""
            SchermataRoundSingolo(
                roundId = roundId,
                onTornaIndietro = { navController.popBackStack() }
            )
        }
    }
}



@Composable
fun SchermataDiBenvenuto(onVaiAlMenu: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.sfondo_schermata_iniziale),
            contentDescription = "Sfondo a mattoni con logo integrato",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier.fillMaxSize().padding(top = 90.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            TestoBomboletta()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.wrapContentSize()) {
                Spacer(modifier = Modifier.size(180.dp))
                TestoAnimatoStileMinecraft()
            }
            Spacer(modifier = Modifier.height(500.dp))
            PulsanteInizia(onVaiAlMenu)
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
        Text("BATTLE ROSTER", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato)
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
        modifier = Modifier.offset(x = -8.dp, y = 420.dp).graphicsLayer { scaleX = scalaTesto; scaleY = scalaTesto }
    ) {
        Text("WHAT UUU SAYYYNNN!!!", color = Color.Black, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato, style = TextStyle(drawStyle = Stroke(miter = 10f, width = 12f, join = StrokeJoin.Round)))
        Text("WHAT UUU SAYYYNNN!!!", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato)
    }
}

@Composable
fun PulsanteInizia(onVaiAlMenu: () -> Unit) {
    Button(
        onClick = { onVaiAlMenu() },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
        modifier = Modifier.width(220.dp).height(65.dp)
    ) {
        Text(text = "Inizia ora", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}