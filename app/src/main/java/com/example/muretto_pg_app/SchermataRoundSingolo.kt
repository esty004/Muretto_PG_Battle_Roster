package com.example.muretto_pg_app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ModalitaSpareggio(val nome: String, val colore: Color)

// Le 21 Modalità definitive (senza doppioni)
val listaModalitaSpareggio = listOf(
    ModalitaSpareggio("4/4", Color(0xFFE53935)),
    ModalitaSpareggio("4/4\nArgomenti", Color(0xFFF4511E)),
    ModalitaSpareggio("3/4", Color(0xFFFF9800)),
    ModalitaSpareggio("3/4\nArgomenti", Color(0xFFFFB300)),
    ModalitaSpareggio("8/4", Color(0xFFFDD835)),
    ModalitaSpareggio("8/4\nArgomenti", Color(0xFFC0CA33)),
    ModalitaSpareggio("Kickback\nMinuti", Color(0xFF43A047)),
    ModalitaSpareggio("Kickback\n4/4", Color(0xFF00897B)),
    ModalitaSpareggio("Minuto\nLibero", Color(0xFF00ACC1)),
    ModalitaSpareggio("Minuto Beat\na Scelta", Color(0xFF039BE5)),
    ModalitaSpareggio("Minuto Beat\nArgomento", Color(0xFF1E88E5)),
    ModalitaSpareggio("Linker", Color(0xFF3949AB)),
    ModalitaSpareggio("Taboo", Color(0xFF5E35B1)),
    ModalitaSpareggio("Modalità\nPersonaggi", Color(0xFF8E24AA)),
    ModalitaSpareggio("Modalità\nSituazioni", Color(0xFFD81B60)),
    ModalitaSpareggio("Cypher\nTecniche", Color(0xFFE91E63)),
    ModalitaSpareggio("Oggetti", Color(0xFF9C27B0)),
    ModalitaSpareggio("Immagini", Color(0xFF673AB7)),
    ModalitaSpareggio("4/4 Tecniche\nPerfette", Color(0xFF3F51B5)),
    ModalitaSpareggio("Handicap\nMatch", Color(0xFF009688)),
    ModalitaSpareggio("1 vs 1", Color(0xFF795548))
)

@Composable
fun SchermataRoundSingolo(roundId: String, onTornaIndietro: () -> Unit) {
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))
    val context = LocalContext.current

    val indiceRound = GestoreBattle.roundsAttuali.indexOfFirst { it.id == roundId }
    val round = GestoreBattle.roundsAttuali.getOrNull(indiceRound)

    if (round == null) {
        onTornaIndietro()
        return
    }

    var vincitoreTemporaneoId by remember { mutableStateOf(round.vincitoreId) }

    var mostraDialogRuota by remember { mutableStateOf(false) }
    var mostraPopupRisultato by remember { mutableStateOf(false) }
    var risultatoSpareggio by remember { mutableStateOf<ModalitaSpareggio?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 20.dp)) {
                IconButton(onClick = { onTornaIndietro() }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                    Text("<", color = Color.White, fontSize = 45.sp, fontFamily = MioFontPersonalizzato)
                }
                Text("ROUND ${round.numero}", color = Color.White, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))

                if (vincitoreTemporaneoId != null && GestoreBattle.faseAttuale != FaseTorneo.FINALE) {
                    IconButton(
                        onClick = {
                            val roundAggiornato = round.copy(completato = true, vincitoreId = vincitoreTemporaneoId)
                            GestoreBattle.roundsAttuali[indiceRound] = roundAggiornato
                            GestoreBattle.salvaProgresso(context)
                            onTornaIndietro()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Conferma", tint = Color.Green, modifier = Modifier.size(40.dp))
                    }
                }
            }

            val backgroundBrush = Brush.horizontalGradient(colors = listOf(Color(0xFF3A0000), Color(0xFF00003A)))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp).clip(RoundedCornerShape(32.dp)).border(4.dp, Color(0xFFD32F2F), RoundedCornerShape(32.dp)).background(brush = backgroundBrush).padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {

                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        round.partecipanti.forEachIndexed { index, team ->
                            Box(
                                modifier = Modifier.clickable { vincitoreTemporaneoId = if (vincitoreTemporaneoId == team.id) null else team.id }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BoxTeam(team = team, isVincitore = vincitoreTemporaneoId == team.id, isSconfitto = vincitoreTemporaneoId != null && vincitoreTemporaneoId != team.id, width = 160.dp, height = 200.dp)
                            }
                            if (index < round.partecipanti.size - 1) {
                                Image(painter = painterResource(id = R.drawable.versus), contentDescription = "Versus", modifier = Modifier.size(60.dp).padding(vertical = 4.dp))
                            }
                        }
                    }

                    Button(
                        onClick = { mostraDialogRuota = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth(0.95f).height(65.dp).padding(top = 16.dp).border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    ) {
                        Text("SPAREGGINO/MODALITÀ", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    if (GestoreBattle.faseAttuale == FaseTorneo.FINALE && vincitoreTemporaneoId != null) {
                        var mostraMessaggioFine by remember { mutableStateOf(false) }

                        Button(
                            onClick = { mostraMessaggioFine = true },
                            modifier = Modifier.fillMaxWidth(0.8f).height(60.dp).padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                        ) {
                            Text("FINE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }

                        if (mostraMessaggioFine) {
                            val nomeVincitore = round.partecipanti.find { it.id == vincitoreTemporaneoId }?.nome ?: ""
                            val finalisti = round.partecipanti.joinToString(" VS ") { it.nome }

                            AlertDialog(
                                onDismissRequest = { mostraMessaggioFine = false },
                                containerColor = Color(0xFF111111),
                                title = {
                                    Text("VIVA L'HIP HOP!", color = Color.White, fontSize = 28.sp, fontFamily = MioFontPersonalizzato, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Text("FINALISTI:", color = Color.Gray, fontSize = 16.sp)
                                        Text(finalisti.uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("VINCITORE:", color = Color.Green, fontSize = 18.sp)
                                        Text(nomeVincitore.uppercase(), color = Color.Green, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        onClick = {
                                            val roundAggiornato = round.copy(completato = true, vincitoreId = vincitoreTemporaneoId)
                                            GestoreBattle.roundsAttuali[indiceRound] = roundAggiornato
                                            GestoreBattle.salvaProgresso(context)
                                            mostraMessaggioFine = false
                                            onTornaIndietro()
                                        }
                                    ) { Text("CONFERMA VITTORIA", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostraDialogRuota) {
        val rotazione = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        var staGirando by remember { mutableStateOf(false) }

        val sweepAngle = 360f / listaModalitaSpareggio.size

        Dialog(onDismissRequest = { if (!staGirando) mostraDialogRuota = false }) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFF222222)).border(2.dp, Color.White, RoundedCornerShape(24.dp)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ESTRAZIONE MODALITÀ", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.size(240.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape).rotate(rotazione.value),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                listaModalitaSpareggio.forEachIndexed { i, mod ->
                                    drawArc(color = mod.colore, startAngle = -90f - (sweepAngle / 2) + (i * sweepAngle), sweepAngle = sweepAngle, useCenter = true)
                                }
                            }
                            listaModalitaSpareggio.forEachIndexed { i, mod ->
                                Box(modifier = Modifier.matchParentSize().rotate(i * sweepAngle)) {
                                    Text(
                                        // Font ridotto a 7.sp per far stare comodamente le 21 modalità
                                        text = mod.nome, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 7.sp, lineHeight = 8.sp, textAlign = TextAlign.Center,
                                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                                    )
                                }
                            }
                        }

                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Puntatore", tint = Color.White, modifier = Modifier.size(60.dp).align(Alignment.TopCenter).offset(y = (-30).dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (staGirando) return@Button
                            staGirando = true
                            scope.launch {
                                val giriCasuali = (5..10).random()
                                val angoloCasuale = (0..359).random().toFloat()
                                val targetFinale = rotazione.value + (giriCasuali * 360) + angoloCasuale

                                rotazione.animateTo(targetValue = targetFinale, animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing))

                                val rotazioneNormalizzata = targetFinale % 360f
                                val gradiEffettivi = (360f - rotazioneNormalizzata) % 360f
                                val indiceVincente = (gradiEffettivi / sweepAngle).roundToInt() % listaModalitaSpareggio.size

                                risultatoSpareggio = listaModalitaSpareggio[indiceVincente]
                                mostraDialogRuota = false
                                mostraPopupRisultato = true
                                staGirando = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(if (staGirando) "ESTRAENDO..." else "GIRA LA RUOTA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (mostraPopupRisultato && risultatoSpareggio != null) {
        Dialog(onDismissRequest = { mostraPopupRisultato = false }) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(risultatoSpareggio!!.colore).border(4.dp, Color.White, RoundedCornerShape(24.dp)).padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        IconButton(onClick = { mostraPopupRisultato = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                        }
                    }

                    Text("MODALITÀ:", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = risultatoSpareggio!!.nome.replace("\n", " ").uppercase(),
                        color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}