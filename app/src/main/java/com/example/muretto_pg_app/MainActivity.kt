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
        composable("benvenuto") {
            SchermataDiBenvenuto(onVaiAlMenu = { navController.navigate("menu") })
        }

        composable("menu") {
            SchermataMenu(
                onTornaIndietro = { navController.popBackStack() },
                onSelezionaModalita = { rotta ->
                    navController.navigate(rotta)
                }
            )
        }

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

        composable("generatore_argomenti") { SchermataGeneratoreArgomenti(onTornaIndietro = { navController.popBackStack() }) }
        composable("generatore_modalita") { SchermataGeneratoreModalita(onTornaIndietro = { navController.popBackStack() }) }
        composable("generatore_parole") { SchermataGeneratoreParole(onTornaIndietro = { navController.popBackStack() }) }

        composable("muretto_classico") {
            SchermataMurettoClassico(
                is2v2 = false,
                onTornaAlMenu = { navController.popBackStack() },
                onIniziaBattle = {
                    GestoreBattle.iniziaTorneo(GestoreBattle.mcsSelezionati)
                    navController.navigate("ottavi")
                }
            )
        }

        composable("due_contro_due") {
            SchermataMurettoClassico(
                is2v2 = true,
                onTornaAlMenu = { navController.popBackStack() },
                onIniziaBattle = {
                    GestoreBattle.iniziaTorneo2v2(GestoreBattle.mcsSelezionati)
                    navController.navigate("ottavi")
                }
            )
        }

        composable("ottavi") {
            SchermataOttavi(
                onTornaIndietro = { navController.popBackStack() },
                onVaiAiQuarti = { },
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
    var inTransizione by remember { mutableStateOf(false) }

    val scalaSfondo by animateFloatAsState(targetValue = if (inTransizione) 2.8f else 1f, animationSpec = tween(1300, easing = FastOutSlowInEasing), label = "")
    val scalaEspansione by animateFloatAsState(targetValue = if (inTransizione) 40f else 0f, animationSpec = tween(1100, easing = FastOutSlowInEasing), label = "", finishedListener = { onVaiAlMenu() })
    val alphaContenuto by animateFloatAsState(targetValue = if (inTransizione) 0f else 1f, animationSpec = tween(700), label = "")
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scalaAura by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "")
    val alphaAura by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 0.6f, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "")
    val offsetLogo = (-72).dp

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scalaSfondo; scaleY = scalaSfondo; alpha = alphaContenuto }) {
            Image(painter = painterResource(id = R.drawable.sfondo_schermata_iniziale), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) { TestoBomboletta() }
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.BottomCenter) { TestoAnimatoStileMinecraft() }
        }

        if (!inTransizione) {
            Box(modifier = Modifier.offset(y = offsetLogo).size(280.dp).graphicsLayer { scaleX = scalaAura; scaleY = scalaAura; alpha = alphaAura }.background(Color.Black.copy(alpha = 0.4f), CircleShape).border(4.dp, Color.Black.copy(alpha = 0.6f), CircleShape))
        }

        Box(modifier = Modifier.offset(y = offsetLogo).size(250.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!inTransizione) inTransizione = true })

        if (inTransizione) {
            Box(modifier = Modifier.offset(y = offsetLogo).size(90.dp).graphicsLayer { scaleX = scalaEspansione; scaleY = scalaEspansione }.background(Color.Black, CircleShape))
        }
    }
}

@Composable
fun TestoBomboletta() {
    var visibile by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(500); visibile = true }
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))

    AnimatedVisibility(visible = visibile, enter = expandHorizontally(animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing), expandFrom = Alignment.Start) + fadeIn(animationSpec = tween(durationMillis = 2000))) {
        Box(contentAlignment = Alignment.Center) {
            Text("BATTLE ROSTER", color = Color.Black, fontSize = 42.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato, style = TextStyle(drawStyle = Stroke(miter = 10f, width = 10f, join = StrokeJoin.Round)))
            Text("BATTLE ROSTER", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato)
        }
    }
}

@Composable
fun TestoAnimatoStileMinecraft() {
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scalaTesto by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "")

    Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer { scaleX = scalaTesto; scaleY = scalaTesto }) {
        Text("WHAT UUU SAYYYNNN!!!", color = Color.Black, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato, style = TextStyle(drawStyle = Stroke(miter = 10f, width = 12f, join = StrokeJoin.Round)))
        Text("WHAT UUU SAYYYNNN!!!", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontPersonalizzato)
    }
}