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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
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

// HELPER: sfondo del contest (immagine custom o drawable del Tema) + patina
@Composable
private fun SfondoContest(stile: StileContest) {
    if (stile.sfondoUrl != null) {
        AsyncImage(model = stile.sfondoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, error = painterResource(stile.sfondoRes))
    } else {
        Image(painter = painterResource(stile.sfondoRes), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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

// HELPER: facce di un gruppo (1..N) ricavate da un Freestyler combinato (url separati da virgola)
@Composable
private fun FacceGruppo(freestyler: Freestyler, dimFaccia: Int) {
    val urls = (freestyler.immagineUrl ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val lista = if (urls.isEmpty()) listOf("no_pic") else urls
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        lista.take(4).forEach { u ->
            val model: Any = if (u == "no_pic") R.drawable.no_pic else u
            AsyncImage(
                model = model, contentDescription = null,
                modifier = Modifier.size(dimFaccia.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop, error = painterResource(R.drawable.no_pic)
            )
        }
        if (lista.size > 4) Text("+${lista.size - 4}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// Box di un lato (coppia/squadra/singolo) nella card del round
@Composable
fun BoxGruppoContest(freestyler: Freestyler, colore: Color, larghezza: Int = 130) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(larghezza.dp).clip(RoundedCornerShape(12.dp)).border(3.dp, colore, RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(8.dp)
    ) {
        FacceGruppo(freestyler, dimFaccia = 54)
        Spacer(modifier = Modifier.height(6.dp))
        Text(freestyler.nome.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
    }
}

// STEP A - Configurazione battle (tipo + accoppiamenti)
@Composable
fun SchermataConfigBattle(stile: StileContest, onProsegui: () -> Unit, onTornaIndietro: () -> Unit) {
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
                if (tipo == "squadra") Text("La dimensione delle squadre la scegli nella selezione del roster.", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))

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

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        GestoreContestBattle.tipoBattle = tipo
                        GestoreContestBattle.accoppiamenti = accoppiamenti
                        // default sensato per le squadre
                        if (tipo == "squadra" && GestoreContestBattle.dimensioneSquadra < 3) GestoreContestBattle.dimensioneSquadra = 3
                        if (tipo == "2v2") GestoreContestBattle.dimensioneSquadra = 2
                        onProsegui()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) { Text("AVANTI: SELEZIONA ROSTER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// STEP B - Selezione roster (tutti i muretti) + dimensione squadra + colori gruppo
@Composable
fun SchermataRosterContest(stile: StileContest, onProsegui: () -> Unit, onTornaIndietro: () -> Unit) {
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

    val grouped = GestoreContestBattle.isAGruppi()
    val isSquadra = GestoreContestBattle.tipoBattle == "squadra"
    val lista = RosterGlobaleContest.lista.filter { it.nome.contains(ricerca, ignoreCase = true) }

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoContest(stile)
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                HeaderContest("SELEZIONA ROSTER", font, onTornaIndietro)

                // Stepper dimensione squadra (solo per "squadra")
                if (isSquadra) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Text("FREESTYLER PER SQUADRA: ", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = font, fontSize = 16.sp)
                        IconButton(onClick = { if (GestoreContestBattle.dimensioneSquadra > 2) GestoreContestBattle.dimensioneSquadra-- }) { Text("-", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                        Text(GestoreContestBattle.dimensioneSquadra.toString(), color = stile.coloreCornici, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { if (GestoreContestBattle.dimensioneSquadra < 6) GestoreContestBattle.dimensioneSquadra++ }) { Text("+", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                val info = if (grouped) "Seleziona in ordine: ogni ${GestoreContestBattle.membriPerLato()} formano un gruppo (stesso colore)." else "Selezionati: ${GestoreContestBattle.rosterSelezionato.size}"
                Text(info, color = stile.coloreCornici, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))

                OutlinedTextField(
                    value = ricerca, onValueChange = { ricerca = it },
                    placeholder = { Text("Cerca un MC...", color = Color.Gray) },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = stile.coloreCornici, unfocusedBorderColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(lista) { mc ->
                        val selez = GestoreContestBattle.rosterSelezionato.any { it.id == mc.id }
                        val indice = if (selez) GestoreContestBattle.rosterSelezionato.indexOfFirst { it.id == mc.id } else -1
                        CardFreestylerTorneo(
                            freestyler = mc,
                            isSelezionato = selez,
                            tipoTorneo = if (grouped) TipoTorneo.COPPIE_PREDEFINITE else TipoTorneo.SINGOLO,
                            indiceSelezione = indice,
                            dimensioneGruppo = GestoreContestBattle.membriPerLato(),
                            onClick = {
                                if (selez) GestoreContestBattle.rosterSelezionato.removeIf { it.id == mc.id }
                                else GestoreContestBattle.rosterSelezionato.add(mc)
                            }
                        )
                    }
                }
            }

            FloatingActionButton(onClick = onTornaIndietro, containerColor = stile.coloreBottoni, contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) { Text("<", fontSize = 28.sp, fontFamily = font, fontWeight = FontWeight.Bold) }
            FloatingActionButton(onClick = { mostraDialog = true }, containerColor = Color(0xFF4CAF50), contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) { Icon(Icons.Default.Add, contentDescription = "Aggiungi MC", modifier = Modifier.size(28.dp)) }
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
            onDismissRequest = { mostraDialog = false }, containerColor = Tema.coloreSfondoCard,
            title = { Text("Nuovo Freestyler", color = Tema.coloreTesto, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondo).border(2.dp, stile.coloreCornici, RoundedCornerShape(12.dp)).clickable { selettoreFoto.launch("image/*") }, contentAlignment = Alignment.Center) {
                        if (nuovaFoto != null) AsyncImage(model = nuovaFoto, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text("Foto\n(opzionale)", color = Tema.coloreTestoSecondario, textAlign = TextAlign.Center, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = nuovoNome, onValueChange = { nuovoNome = it }, singleLine = true, placeholder = { Text("Nome del Freestyler", color = Tema.coloreTestoSecondario) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = stile.coloreCornici))
                }
            },
            confirmButton = {
                Button(enabled = !staSalvando, colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni), onClick = {
                    if (nuovoNome.isBlank()) return@Button
                    staSalvando = true
                    scope.launch {
                        val bytes = nuovaFoto?.let { uri -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                        val murettoId = databaseViewModel.profiloAttuale?.muretto_id ?: "09fbe1d3-0022-41b8-ba4b-edc887c145a2"
                        val ok = databaseViewModel.inserisciNuovoMc(nuovoNome.trim(), murettoId, bytes)
                        if (ok) databaseViewModel.caricaRosterGlobaleContest()
                        staSalvando = false; nuovoNome = ""; nuovaFoto = null; mostraDialog = false
                    }
                }) { if (staSalvando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else Text("AGGIUNGI", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { mostraDialog = false }) { Text("Annulla", color = Tema.coloreTestoSecondario) } }
        )
    }
}

// CARD ROUND con stile contest (sfondo card custom + lati a gruppi colorati)
@Composable
fun RoundCardContest(round: Round, stile: StileContest, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(3.dp, stile.coloreCornici, RoundedCornerShape(24.dp)).clickable { onClick() }) {
        if (stile.sfondoCardUrl != null) {
            AsyncImage(model = stile.sfondoCardUrl, contentDescription = null, modifier = Modifier.matchParentSize().scale(1.05f), contentScale = ContentScale.Crop, error = painterResource(R.drawable.sfondo_round_card))
        } else {
            Image(painter = painterResource(id = R.drawable.sfondo_round_card), contentDescription = null, modifier = Modifier.matchParentSize().scale(1.2f), contentScale = ContentScale.Crop)
        }
        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Round ${round.numero}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 12.dp))
            if (round.partecipanti.isEmpty()) {
                Text("Da comporre", color = Color.LightGray, fontSize = 14.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    round.partecipanti.forEachIndexed { i, lato ->
                        if (i > 0) Image(painter = painterResource(id = R.drawable.versus), contentDescription = null, modifier = Modifier.size(70.dp).padding(horizontal = 8.dp).clip(CircleShape))
                        val primoId = lato.id.split("_").first()
                        BoxGruppoContest(freestyler = lato, colore = GestoreContestBattle.coloreGruppoDi(primoId, stile.coloreCornici))
                    }
                }
            }
        }
    }
}

// STEP C - Builder del tabellone
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataBuilderContest(stile: StileContest, onNuovoRound: (String) -> Unit, onRoundClick: (String) -> Unit, onConferma: () -> Unit, onTornaIndietro: () -> Unit) {
    val font = FontFamily(Font(R.font.komtit__))
    var menuFase by remember { mutableStateOf(false) }
    val tuttiAccoppiati = GestoreContestBattle.tuttiAccoppiati()

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoContest(stile)
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 12.dp)) {
                    IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) { Text("<", color = Color.White, fontSize = 45.sp, fontFamily = font) }
                    ExposedDropdownMenuBox(expanded = menuFase, onExpandedChange = { menuFase = !menuFase }, modifier = Modifier.align(Alignment.Center)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.menuAnchor().clip(RoundedCornerShape(8.dp)).clickable { menuFase = true }.padding(horizontal = 8.dp)) {
                            Text(GestoreContestBattle.fasePartenza.name, color = Color.White, fontSize = 28.sp, fontFamily = font, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = stile.coloreCornici)
                        }
                        ExposedDropdownMenu(expanded = menuFase, onDismissRequest = { menuFase = false }, modifier = Modifier.background(Tema.coloreSfondoCard)) {
                            FaseTorneo.values().forEach { fase -> DropdownMenuItem(text = { Text(fase.name, color = Tema.coloreTesto) }, onClick = { GestoreContestBattle.fasePartenza = fase; menuFase = false }) }
                        }
                    }
                }

                if (!tuttiAccoppiati) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(80.dp).clip(RoundedCornerShape(16.dp)).border(3.dp, stile.coloreCornici, RoundedCornerShape(16.dp)).background(stile.coloreSfondoCard)
                            .clickable { if (GestoreContestBattle.gruppiDisponibili().isNotEmpty()) { val r = GestoreContestBattle.nuovoRound(); onNuovoRound(r.id) } },
                        contentAlignment = Alignment.Center
                    ) { Text("+ NUOVO ROUND", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = font) }
                }

                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(GestoreContestBattle.roundsAttuali) { round -> RoundCardContest(round = round, stile = stile, onClick = { onRoundClick(round.id) }) }
                }
            }

            if (tuttiAccoppiati) {
                Button(onClick = onConferma, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(12.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).height(56.dp)) {
                    Text("CONFERMA TABELLONE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// STEP D - Costruzione del singolo round: si scelgono 2 LATI (gruppi)
@Composable
fun SchermataCostruzioneRound(roundId: String, stile: StileContest, onFatto: () -> Unit, onTornaIndietro: () -> Unit) {
    val font = FontFamily(Font(R.font.komtit__))
    val round = GestoreContestBattle.roundsAttuali.find { it.id == roundId }
    if (round == null) { onTornaIndietro(); return }

    // selezione = lista di gruppi scelti (ognuno con 1..N membri). Servono 2 gruppi.
    val selezione = remember { mutableStateListOf<List<Freestyler>>() }
    val idsSelez = selezione.flatten().map { it.id }.toSet()
    val disponibili = GestoreContestBattle.gruppiDisponibili().filter { g -> g.none { it.id in idsSelez } }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoContest(stile)
            Column(modifier = Modifier.fillMaxSize()) {
                HeaderContest("ROUND ${round.numero}", font, onTornaIndietro)

                // anteprima del round in costruzione
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp).height(200.dp).clip(RoundedCornerShape(24.dp)).border(3.dp, stile.coloreCornici, RoundedCornerShape(24.dp)).background(stile.coloreSfondoCard), contentAlignment = Alignment.Center) {
                    if (selezione.isEmpty()) {
                        Text("Tocca i gruppi qui sotto\nper comporre il round", color = Color.LightGray, textAlign = TextAlign.Center, fontSize = 16.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            selezione.forEachIndexed { i, gruppo ->
                                if (i > 0) Image(painter = painterResource(id = R.drawable.versus), contentDescription = null, modifier = Modifier.size(60.dp).padding(horizontal = 6.dp).clip(CircleShape))
                                val lato = GestoreContestBattle.creaGruppo(gruppo)
                                Box(modifier = Modifier.clickable { selezione.removeIf { it.firstOrNull()?.id == gruppo.firstOrNull()?.id } }) {
                                    BoxGruppoContest(freestyler = lato, colore = GestoreContestBattle.coloreGruppoDi(gruppo.first().id, stile.coloreCornici), larghezza = 110)
                                }
                            }
                        }
                    }
                }

                Text("Sfidanti nel round: ${selezione.size} (minimo 2) - tocca per rimuovere", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))

                // gruppi disponibili
                LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    items(disponibili) { gruppo ->
                        val colore = GestoreContestBattle.coloreGruppoDi(gruppo.first().id, stile.coloreCornici)
                        val lato = GestoreContestBattle.creaGruppo(gruppo)
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(3.dp, colore, RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).clickable { if (selezione.size < 8) selezione.add(gruppo) }.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FacceGruppo(lato, dimFaccia = 48)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(lato.nome.uppercase(), color = Tema.coloreTesto, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                    }
                }

                Button(
                    onClick = { GestoreContestBattle.salvaLatiRound(roundId, selezione.toList()); onFatto() },
                    enabled = selezione.size >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni, disabledContainerColor = Color.DarkGray),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)
                ) { Text(if (selezione.size >= 2) "SALVA ROUND (${selezione.size} sfidanti)" else "SELEZIONA ALMENO 2", color = Color.White, fontWeight = FontWeight.Bold) }            }
        }
    }
}