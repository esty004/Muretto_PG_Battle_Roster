package com.example.muretto_pg_app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// ───────────────────────────────────────────────────────────────────────────
// HELPER: sfondo del contest (immagine custom oppure drawable del Tema) + patina
// ───────────────────────────────────────────────────────────────────────────
@Composable
private fun SfondoContest(stile: StileContest) {
    if (stile.sfondoUrl != null) {
        AsyncImage(
            model = stile.sfondoUrl, contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            error = painterResource(stile.sfondoRes)
        )
    } else {
        Image(
            painter = painterResource(stile.sfondoRes), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
}

@Composable
private fun HeaderContest(titolo: String, font: FontFamily, onTornaIndietro: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 16.dp)) {
        IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
            Text("<", color = Color.White, fontSize = 45.sp, fontFamily = font)
        }
        Text(titolo, color = Color.White, fontSize = 30.sp, fontFamily = font, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}

// ───────────────────────────────────────────────────────────────────────────
// STEP A — Configurazione battle (tipo + accoppiamenti)
// ───────────────────────────────────────────────────────────────────────────
@Composable
fun SchermataConfigBattle(
    stile: StileContest,
    onProsegui: () -> Unit,
    onTornaIndietro: () -> Unit
) {
    val font = FontFamily(Font(R.font.komtit__))
    var tipo by remember { mutableStateOf(GestoreContestBattle.tipoBattle) }
    var accoppiamenti by remember { mutableStateOf(GestoreContestBattle.accoppiamenti) }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoContest(stile)
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                HeaderContest("CONFIGURA BATTLE", font, onTornaIndietro)

                Text("TIPO DI BATTLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("1v1" to "1 VS 1", "2v2" to "2 VS 2", "squadra" to "SQUADRA").forEach { (val0, label) ->
                        val sel = tipo == val0
                        Box(
                            modifier = Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (sel) stile.coloreBottoni else stile.coloreSfondoCard)
                                .border(2.dp, if (sel) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { tipo = val0 },
                            contentAlignment = Alignment.Center
                        ) { Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }

                if (tipo == "squadra") {
                    Text("⚠️ Modalità Squadra non ancora definita: per ora si comporta come stub.",
                        color = Color(0xFFFFB300), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("ACCOPPIAMENTI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("predefiniti" to "PREDEFINITI", "casuali" to "CASUALI").forEach { (val0, label) ->
                        val sel = accoppiamenti == val0
                        Box(
                            modifier = Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (sel) stile.coloreBottoni else stile.coloreSfondoCard)
                                .border(2.dp, if (sel) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { accoppiamenti = val0 },
                            contentAlignment = Alignment.Center
                        ) { Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
                Text(
                    if (accoppiamenti == "predefiniti") "Deciderai tu gli accoppiamenti round per round."
                    else "Gli accoppiamenti verranno generati a caso all'avvio.",
                    color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        GestoreContestBattle.tipoBattle = tipo
                        GestoreContestBattle.accoppiamenti = accoppiamenti
                        onProsegui()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 0.dp)
                ) { Text("AVANTI: SELEZIONA ROSTER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// STEP B — Selezione roster (tutti i muretti) + aggiunta veloce
// ───────────────────────────────────────────────────────────────────────────
@Composable
fun SchermataRosterContest(
    stile: StileContest,
    onProsegui: () -> Unit,   // il chiamante decide: predefiniti→builder, casuali→genera+salva+esci
    onTornaIndietro: () -> Unit
) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val font = FontFamily(Font(R.font.komtit__))

    var ricerca by remember { mutableStateOf("") }
    var mostraDialog by remember { mutableStateOf(false) }
    var nuovoNome by remember { mutableStateOf("") }
    var nuovaFoto by remember { mutableStateOf<Uri?>(null) }
    var staSalvando by remember { mutableStateOf(false) }

    val selettoreFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { nuovaFoto = it }

    LaunchedEffect(Unit) { databaseViewModel.caricaRosterGlobaleContest() }

    val lista = RosterGlobaleContest.lista.filter { it.nome.contains(ricerca, ignoreCase = true) }

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoContest(stile)
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                HeaderContest("SELEZIONA ROSTER", font, onTornaIndietro)

                Text("Selezionati: ${GestoreContestBattle.rosterSelezionato.size}",
                    color = stile.coloreCornici, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp))

                OutlinedTextField(
                    value = ricerca, onValueChange = { ricerca = it },
                    placeholder = { Text("Cerca un MC...", color = Color.Gray) },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = stile.coloreCornici, unfocusedBorderColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(lista) { mc ->
                        val selez = GestoreContestBattle.rosterSelezionato.any { it.id == mc.id }
                        CardFreestylerTorneo(
                            freestyler = mc,
                            isSelezionato = selez,
                            tipoTorneo = TipoTorneo.SINGOLO,
                            indiceSelezione = -1,
                            onClick = {
                                if (selez) GestoreContestBattle.rosterSelezionato.removeIf { it.id == mc.id }
                                else GestoreContestBattle.rosterSelezionato.add(mc)
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onTornaIndietro, containerColor = stile.coloreBottoni, contentColor = Color.White,
                shape = CircleShape, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) { Text("<", fontSize = 28.sp, fontFamily = font, fontWeight = FontWeight.Bold) }

            FloatingActionButton(
                onClick = { mostraDialog = true }, containerColor = Color(0xFF4CAF50), contentColor = Color.White,
                shape = CircleShape, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Aggiungi MC", modifier = Modifier.size(28.dp)) }

            Button(
                onClick = { if (GestoreContestBattle.rosterSelezionato.isNotEmpty()) onProsegui() },
                enabled = GestoreContestBattle.rosterSelezionato.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).height(56.dp)
            ) { Text("AVANTI", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }

    if (mostraDialog) {
        AlertDialog(
            onDismissRequest = { mostraDialog = false },
            containerColor = Tema.coloreSfondoCard,
            title = { Text("Nuovo Freestyler", color = Tema.coloreTesto, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondo)
                            .border(2.dp, stile.coloreCornici, RoundedCornerShape(12.dp)).clickable { selettoreFoto.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (nuovaFoto != null) AsyncImage(model = nuovaFoto, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text("Foto\n(opzionale)", color = Tema.coloreTestoSecondario, textAlign = TextAlign.Center, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nuovoNome, onValueChange = { nuovoNome = it }, singleLine = true,
                        placeholder = { Text("Nome del Freestyler", color = Tema.coloreTestoSecondario) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = stile.coloreCornici)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !staSalvando,
                    colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni),
                    onClick = {
                        if (nuovoNome.isBlank()) return@Button
                        staSalvando = true
                        scope.launch {
                            val bytes = nuovaFoto?.let { uri -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                            // l'MC del contest viene creato nel muretto dell'organizzatore (o PG di default)
                            val murettoId = databaseViewModel.profiloAttuale?.muretto_id ?: "09fbe1d3-0022-41b8-ba4b-edc887c145a2"
                            val ok = databaseViewModel.inserisciNuovoMc(nuovoNome.trim(), murettoId, bytes)
                            if (ok) databaseViewModel.caricaRosterGlobaleContest()
                            staSalvando = false
                            nuovoNome = ""; nuovaFoto = null; mostraDialog = false
                        }
                    }
                ) { if (staSalvando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else Text("AGGIUNGI", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { mostraDialog = false }) { Text("Annulla", color = Tema.coloreTestoSecondario) } }
        )
    }
}

// ───────────────────────────────────────────────────────────────────────────
// CARD ROUND con stile contest (clone tematizzato di RoundCard, riusa BoxMC)
// ───────────────────────────────────────────────────────────────────────────
@Composable
fun RoundCardContest(round: Round, stile: StileContest, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .border(3.dp, stile.coloreCornici, RoundedCornerShape(24.dp)).clickable { onClick() }
    ) {
        Image(painter = painterResource(id = R.drawable.sfondo_round_card), contentDescription = null,
            modifier = Modifier.fillMaxSize().scale(1.2f), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Round ${round.numero}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 12.dp))

            if (round.partecipanti.isEmpty()) {
                Text("Da comporre", color = Color.LightGray, fontSize = 14.sp)
            } else {
                round.partecipanti.chunked(2).forEachIndexed { idx, riga ->
                    if (idx > 0) Image(painter = painterResource(id = R.drawable.versus), contentDescription = null, modifier = Modifier.size(46.dp).padding(vertical = 6.dp).clip(CircleShape))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        BoxMC(mc = riga[0], width = 120.dp, height = 160.dp, coloreBordoCustom = stile.coloreCornici)
                        if (riga.size == 2) {
                            Image(painter = painterResource(id = R.drawable.versus), contentDescription = null, modifier = Modifier.size(90.dp).padding(horizontal = 12.dp).clip(CircleShape))
                            BoxMC(mc = riga[1], width = 120.dp, height = 160.dp, coloreBordoCustom = stile.coloreCornici)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// STEP C — Builder del tabellone (dropdown fase, + NUOVO ROUND / CONFERMA)
// ───────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataBuilderContest(
    stile: StileContest,
    onNuovoRound: (String) -> Unit,   // riceve l'id del round appena creato
    onRoundClick: (String) -> Unit,
    onConferma: () -> Unit,
    onTornaIndietro: () -> Unit
) {
    val font = FontFamily(Font(R.font.komtit__))
    var menuFase by remember { mutableStateOf(false) }
    val tuttiAccoppiati = GestoreContestBattle.tuttiAccoppiati()

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoContest(stile)
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {

                // HEADER con dropdown della fase di partenza
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 12.dp)) {
                    IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                        Text("<", color = Color.White, fontSize = 45.sp, fontFamily = font)
                    }
                    ExposedDropdownMenuBox(expanded = menuFase, onExpandedChange = { menuFase = !menuFase }, modifier = Modifier.align(Alignment.Center)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.menuAnchor().clip(RoundedCornerShape(8.dp)).clickable { menuFase = true }.padding(horizontal = 8.dp)) {
                            Text(GestoreContestBattle.fasePartenza.name, color = Color.White, fontSize = 28.sp, fontFamily = font, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = stile.coloreCornici)
                        }
                        ExposedDropdownMenu(expanded = menuFase, onDismissRequest = { menuFase = false }, modifier = Modifier.background(Tema.coloreSfondoCard)) {
                            FaseTorneo.values().forEach { fase ->
                                DropdownMenuItem(text = { Text(fase.name, color = Tema.coloreTesto) }, onClick = { GestoreContestBattle.fasePartenza = fase; menuFase = false })
                            }
                        }
                    }
                }

                if (!tuttiAccoppiati) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(80.dp).clip(RoundedCornerShape(16.dp))
                            .border(3.dp, stile.coloreCornici, RoundedCornerShape(16.dp)).background(stile.coloreSfondoCard)
                            .clickable {
                                if (GestoreContestBattle.rosterDisponibile().isNotEmpty()) {
                                    val r = GestoreContestBattle.nuovoRound()
                                    onNuovoRound(r.id)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text("+ NUOVO ROUND", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = font) }
                }

                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(GestoreContestBattle.roundsAttuali) { round ->
                        RoundCardContest(round = round, stile = stile, onClick = { onRoundClick(round.id) })
                    }
                }
            }

            if (tuttiAccoppiati) {
                Button(
                    onClick = onConferma,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).height(56.dp)
                ) { Text("CONFERMA TABELLONE", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// STEP D — Costruzione del singolo round (1v1 = 2 MC, 2v2 = 4 MC)
// ───────────────────────────────────────────────────────────────────────────
@Composable
fun SchermataCostruzioneRound(
    roundId: String,
    stile: StileContest,
    onFatto: () -> Unit,
    onTornaIndietro: () -> Unit
) {
    val font = FontFamily(Font(R.font.komtit__))
    val round = GestoreContestBattle.roundsAttuali.find { it.id == roundId }
    if (round == null) { onTornaIndietro(); return }

    val perRound = GestoreContestBattle.mcPerRound()
    val selezione = remember { mutableStateListOf<Freestyler>() }

    // roster ancora libero, escludendo chi è già in questa selezione
    val disponibili = GestoreContestBattle.rosterDisponibile().filter { d -> selezione.none { it.id == d.id } }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoContest(stile)
            Column(modifier = Modifier.fillMaxSize()) {
                HeaderContest("ROUND ${round.numero}", font, onTornaIndietro)

                // CARD round in costruzione
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(220.dp).clip(RoundedCornerShape(24.dp))
                        .border(3.dp, stile.coloreCornici, RoundedCornerShape(24.dp)).background(stile.coloreSfondoCard),
                    contentAlignment = Alignment.Center
                ) {
                    if (selezione.isEmpty()) {
                        Text("Tocca gli MC qui sotto\nper comporre il round", color = Color.LightGray, textAlign = TextAlign.Center, fontSize = 16.sp)
                    } else {
                        // mostra a coppie: per 2v2 ogni 2 selezionati = una squadra
                        val gruppi = if (GestoreContestBattle.tipoBattle == "2v2") selezione.chunked(2) else selezione.chunked(1)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            gruppi.forEachIndexed { i, gruppo ->
                                if (i > 0) Image(painter = painterResource(id = R.drawable.versus), contentDescription = null, modifier = Modifier.size(70.dp).padding(horizontal = 8.dp).clip(CircleShape))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    gruppo.forEach { mc ->
                                        Box(modifier = Modifier.padding(2.dp).clickable { selezione.removeIf { it.id == mc.id } }) {
                                            BoxMC(mc = mc, width = 80.dp, height = 110.dp, coloreBordoCustom = stile.coloreCornici)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text("Selezionati ${selezione.size}/$perRound — tocca una faccia per rimuoverla",
                    color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))

                // roster disponibile
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(disponibili) { mc ->
                        CardFreestylerTorneo(
                            freestyler = mc, isSelezionato = false,
                            tipoTorneo = TipoTorneo.SINGOLO, indiceSelezione = -1,
                            onClick = { if (selezione.size < perRound) selezione.add(mc) }
                        )
                    }
                }

                Button(
                    onClick = {
                        GestoreContestBattle.salvaPartecipantiRound(roundId, selezione.toList())
                        onFatto()
                    },
                    enabled = selezione.size == perRound,
                    colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni, disabledContainerColor = Color.DarkGray),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)
                ) { Text(if (selezione.size == perRound) "SALVA ROUND" else "SELEZIONA $perRound MC", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
