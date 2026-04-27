package com.example.muretto_pg_app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataOttavi(
    onTornaIndietro: () -> Unit,
    onVaiAiQuarti: () -> Unit,
    onRoundClick: (String) -> Unit
) {
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))
    val context = LocalContext.current

    val listaRounds = GestoreBattle.roundsAttuali
    val tuttiFiniti = listaRounds.isNotEmpty() && listaRounds.all { it.completato }

    val titoloSchermata = if (GestoreBattle.is2v2) {
        "2 VS 2 - ${GestoreBattle.faseAttuale.name}"
    } else {
        GestoreBattle.faseAttuale.name
    }

    var mostraDialogAggiunta by remember { mutableStateOf(false) }
    var nomeNuovoMc by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 20.dp)) {
                    IconButton(onClick = { onTornaIndietro() }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                        Text("<", color = Color.White, fontSize = 45.sp, fontFamily = MioFontPersonalizzato)
                    }
                    Text(GestoreBattle.faseAttuale.name, color = Color.White, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).offset(x = 15.dp))
                }

                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(listaRounds) { round -> RoundCard(round = round, onClick = { onRoundClick(round.id) }) }
                }
            }

            if (!tuttiFiniti) {
                FloatingActionButton(onClick = { mostraDialogAggiunta = true }, containerColor = Color.White, contentColor = Color.Black, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi", modifier = Modifier.size(30.dp))
                }
            }

            if (tuttiFiniti) {
                val vincitori = GestoreBattle.roundsAttuali.mapNotNull { r ->
                    r.partecipanti.find { it.id == r.vincitoreId }
                }
                val prossimaFase = GestoreBattle.determinaFase(vincitori.size)

                val testoBottone = if (GestoreBattle.faseAttuale == FaseTorneo.FINALE) {
                    "CONCLUDI TORNEO"
                } else {
                    when(prossimaFase) {
                        FaseTorneo.QUARTI -> "PASSAGGIO AI QUARTI"
                        FaseTorneo.SEMIFINALE -> "PASSAGGIO IN SEMIFINALE"
                        FaseTorneo.FINALE -> "VAI ALLA FINALE"
                        else -> "PROSSIMA FASE"
                    }
                }

                Button(
                    onClick = {
                        if (GestoreBattle.faseAttuale == FaseTorneo.FINALE) {
                            GestoreBattle.pulisciSalvataggio(context)
                            GestoreBattle.resetSelezione()
                            onTornaIndietro()
                        } else {
                            val vincitori = GestoreBattle.roundsAttuali.mapNotNull { r -> r.partecipanti.find { it.id == r.vincitoreId } }
                            when(GestoreBattle.faseAttuale) {
                                FaseTorneo.OTTAVI -> GestoreBattle.generaFase(FaseTorneo.QUARTI, vincitori)
                                FaseTorneo.QUARTI -> GestoreBattle.generaFase(FaseTorneo.SEMIFINALE, vincitori)
                                FaseTorneo.SEMIFINALE -> GestoreBattle.generaFase(FaseTorneo.FINALE, vincitori)
                                else -> {}
                            }
                            GestoreBattle.salvaProgresso(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.8f).padding(bottom = 20.dp).height(60.dp), shape = RoundedCornerShape(12.dp)
                ) { Text(testoBottone, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (mostraDialogAggiunta) {
        AlertDialog(
            onDismissRequest = { mostraDialogAggiunta = false },
            containerColor = Color(0xFF222222),
            title = { Text("Aggiungi partecipante", color = Color.White) },
            text = {
                OutlinedTextField(value = nomeNuovoMc, onValueChange = { nomeNuovoMc = it }, placeholder = { Text("Nome Freestyler o Team", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true)
            },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), onClick = {
                    if (nomeNuovoMc.isNotBlank()) {
                        val nuovoMembro = Freestyler(UUID.randomUUID().toString(), nomeNuovoMc.trim(), R.drawable.no_pic)
                        val nuovoTeam = Team(UUID.randomUUID().toString(), nomeNuovoMc.trim(), listOf(nuovoMembro))
                        aggiungiTeamARoundIncompleto(nuovoTeam)
                        nomeNuovoMc = ""
                        mostraDialogAggiunta = false
                    }
                }) { Text("Aggiungi", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { mostraDialogAggiunta = false }) { Text("Annulla", color = Color.Gray) } }
        )
    }
}

fun aggiungiTeamARoundIncompleto(team: Team) {
    val roundAperti = GestoreBattle.roundsAttuali.filter { !it.completato }
    if (roundAperti.isNotEmpty()) {
        val roundPiuVuoto = roundAperti.minByOrNull { it.partecipanti.size }
        val indice = GestoreBattle.roundsAttuali.indexOfFirst { it.id == roundPiuVuoto?.id }
        if (indice != -1) {
            val roundModificato = GestoreBattle.roundsAttuali[indice].copy(partecipanti = GestoreBattle.roundsAttuali[indice].partecipanti + team)
            GestoreBattle.roundsAttuali[indice] = roundModificato
        }
    }
}

@Composable
fun RoundCard(round: Round, onClick: () -> Unit) {
    val backgroundBrush = Brush.horizontalGradient(colors = listOf(Color(0xFF3A0000), Color(0xFF00003A)))

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(3.dp, Color(0xFFD32F2F), RoundedCornerShape(24.dp)).background(brush = backgroundBrush).clickable { onClick() }.padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Round ${round.numero}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 16.dp))

            if (round.partecipanti.isNotEmpty()) {
                // SUDDIVIDE GLI MC A GRUPPI DI 2 (Righe)
                val righe = round.partecipanti.chunked(2)

                righe.forEachIndexed { index, riga ->
                    // Aggiunge il VS tra una riga e l'altra se ci sono più di 2 partecipanti
                    if (index > 0) {
                        Image(painter = painterResource(id = R.drawable.versus), contentDescription = "Versus", modifier = Modifier.size(40.dp).padding(vertical = 8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        BoxTeam(team = riga[0], isVincitore = round.vincitoreId == riga[0].id, isSconfitto = round.completato && round.vincitoreId != riga[0].id)

                        if (riga.size == 2) {
                            Image(painter = painterResource(id = R.drawable.versus), contentDescription = "Versus", modifier = Modifier.size(85.dp).padding(horizontal = 10.dp))
                            BoxTeam(team = riga[1], isVincitore = round.vincitoreId == riga[1].id, isSconfitto = round.completato && round.vincitoreId != riga[1].id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxTeam(team: Team, isVincitore: Boolean = false, isSconfitto: Boolean = false, width: Dp = 100.dp, height: Dp = 130.dp) {
    val colorMatrix = remember(isSconfitto) { if (isSconfitto) ColorMatrix().apply { setToSaturation(0f) } else null }
    val fontSize = if (team.membri.size > 1) 11.sp else 13.sp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.width(width).height(height).clip(RoundedCornerShape(16.dp)).border(width = if (isVincitore) 4.dp else 1.dp, color = if (isVincitore) Color.Green else Color.Gray, shape = RoundedCornerShape(16.dp)).background(Color.DarkGray)
        ) {
            if (team.membri.size == 1) {
                Image(painter = painterResource(id = team.membri[0].immagineId), contentDescription = null, modifier = Modifier.fillMaxSize(), alignment = Alignment.TopCenter, contentScale = ContentScale.Crop, colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null)
            } else if (team.membri.size >= 2) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Image(painter = painterResource(id = team.membri[0].immagineId), contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), alignment = Alignment.TopCenter, contentScale = ContentScale.Crop, colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null)
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                    Image(painter = painterResource(id = team.membri[1].immagineId), contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), alignment = Alignment.TopCenter, contentScale = ContentScale.Crop, colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null)
                }
            }

            if (isVincitore) Box(modifier = Modifier.matchParentSize().background(Color.Green.copy(alpha = 0.3f)))
            if (isSconfitto) Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.fillMaxSize().padding(16.dp))
        }
        Text(text = team.nome.uppercase(), color = if (isVincitore) Color.Green else if(isSconfitto) Color.Gray else Color.White, fontSize = fontSize, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
    }
}