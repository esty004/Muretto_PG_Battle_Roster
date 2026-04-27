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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@Composable
fun SchermataOttavi(
    onTornaIndietro: () -> Unit,
    onVaiAiQuarti: () -> Unit,
    onRoundClick: (String) -> Unit
) {
    val MioFont = FontFamily(Font(R.font.komtit__))
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
                        Text("<", color = Color.White, fontSize = 45.sp, fontFamily = MioFont)
                    }
                    Text(
                        titoloSchermata,
                        color = Color.White, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center).offset(x = 15.dp)
                    )
                }

                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(listaRounds) { round -> RoundCard(round = round, onClick = { onRoundClick(round.id) }) }
                }
            }

            if (!tuttiFiniti) {
                FloatingActionButton(
                    onClick = { mostraDialogAggiunta = true },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi", modifier = Modifier.size(30.dp))
                }
            }

            if (tuttiFiniti) {
                val testoBottone = if (GestoreBattle.faseAttuale == FaseTorneo.FINALE) "CONCLUDI TORNEO" else "PROSSIMA FASE"

                Button(
                    onClick = {
                        if (GestoreBattle.faseAttuale == FaseTorneo.FINALE) {
                            GestoreBattle.pulisciSalvataggio(context)
                            GestoreBattle.resetSelezione()
                            onTornaIndietro()
                        } else {
                            val vincitori = GestoreBattle.roundsAttuali.mapNotNull { r -> r.partecipanti.find { it.id == r.vincitoreId } }
                            GestoreBattle.generaFase(GestoreBattle.determinaFase(vincitori.size), vincitori)
                            GestoreBattle.salvaProgresso(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = CircleShape, // Applica lo stile arrotondato
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 20.dp)
                        .height(60.dp)
                ) {
                    Text(testoBottone, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (mostraDialogAggiunta) {
        AlertDialog(
            onDismissRequest = { mostraDialogAggiunta = false },
            containerColor = Color(0xFF222222),
            title = { Text("Aggiungi partecipante", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = nomeNuovoMc, onValueChange = { nomeNuovoMc = it },
                    placeholder = { Text("Nome Freestyler o Team", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), onClick = {
                    if (nomeNuovoMc.isNotBlank()) {
                        val nuovoMembro = Freestyler(UUID.randomUUID().toString(), nomeNuovoMc.trim(), R.drawable.no_pic)

                        val roundAperti = GestoreBattle.roundsAttuali.filter { !it.completato }
                        if (roundAperti.isNotEmpty()) {
                            val roundPiuVuoto = roundAperti.minByOrNull { it.partecipanti.size }
                            val indice = GestoreBattle.roundsAttuali.indexOfFirst { it.id == roundPiuVuoto?.id }
                            if (indice != -1) {
                                val roundModificato = GestoreBattle.roundsAttuali[indice].copy(partecipanti = GestoreBattle.roundsAttuali[indice].partecipanti + nuovoMembro)
                                GestoreBattle.roundsAttuali[indice] = roundModificato
                            }
                        }

                        nomeNuovoMc = ""
                        mostraDialogAggiunta = false
                    }
                }) { Text("Aggiungi", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { mostraDialogAggiunta = false }) { Text("Annulla", color = Color.Gray) } }
        )
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
                val righe = round.partecipanti.chunked(2)

                righe.forEachIndexed { index, riga ->
                    if (index > 0) {
                        Image(painter = painterResource(id = R.drawable.versus), contentDescription = "Versus", modifier = Modifier.size(40.dp).padding(vertical = 8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        BoxMC(mc = riga[0], isVincitore = round.vincitoreId == riga[0].id, isSconfitto = round.completato && round.vincitoreId != riga[0].id)

                        if (riga.size == 2) {
                            Image(painter = painterResource(id = R.drawable.versus), contentDescription = "Versus", modifier = Modifier.size(85.dp).padding(horizontal = 10.dp))
                            BoxMC(mc = riga[1], isVincitore = round.vincitoreId == riga[1].id, isSconfitto = round.completato && round.vincitoreId != riga[1].id)
                        }
                    }
                }
            }
        }
    }
}