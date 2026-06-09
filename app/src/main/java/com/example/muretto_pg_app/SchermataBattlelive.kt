package com.example.muretto_pg_app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Dati passati prima di entrare nella battle live (nome + eventuale codice giudice)
object IngressoContest { var nome: String? = null; var codiceGiudice: String? = null }

// Estensione: trova una sessione 'in_corso' per il contest (lato giudice/spettatore)
suspend fun GestoreVotazioni.trovaSessioneAttivaEvento(supabase: SupabaseClient, eventoId: String): SessioneVoto? {
    return try {
        val lista = supabase.postgrest["contest_sessione_voto"].select {
            filter { eq("evento_id", eventoId); eq("stato", "in_corso") }
        }.decodeList<SessioneVoto>()
        val s = lista.firstOrNull(); sessioneCorrente = s; s
    } catch (e: Exception) { null }
}

// Scompone un "lato" combinato (id "a_b", nome "A & B", url "u1,u2") nei singoli membri.
fun membriDiLato(lato: Freestyler): List<Freestyler> {
    val ids = lato.id.split("_")
    val nomi = lato.nome.split(" & ")
    val urls = (lato.immagineUrl ?: "").split(",")
    return ids.mapIndexed { i, id ->
        val u = urls.getOrNull(i)?.takeIf { it.isNotBlank() && it != "no_pic" }
        Freestyler(id = id, nome = nomi.getOrElse(i) { id }, immagineUrl = u)
    }
}

@Composable
private fun SfondoLive(stile: StileContest) {
    if (stile.sfondoUrl != null) {
        coil.compose.AsyncImage(model = stile.sfondoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Tema.coloreSfondo))
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
}

@Composable
fun SchermataBattleLive(
    eventoId: String,
    ruolo: String,                 // "organizzatore" | "giudice" | "spettatore"
    onEsci: () -> Unit,
    onProssimaFase: () -> Unit
) {
    val vm = LocalDatabaseViewModel.current
    val evento = vm.eventiApprovati.find { it.id == eventoId }
    val design = evento?.contest_design
    val stile = StileContest.risolvi(design)
    val votoPubblico = design?.voto_pubblico ?: false
    val numeroGiudici = design?.numero_giudici ?: 0

    val votingRole = if (ruolo == "spettatore") "spettatore" else "giudice"
    val mioNome = IngressoContest.nome ?: (vm.profiloAttuale?.nome_arte ?: if (ruolo == "spettatore") "Spettatore" else "Giudice")
    val mioCodice = IngressoContest.codiceGiudice

    val cleanup = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    LaunchedEffect(eventoId) { GestoreContestBattle.caricaDalCloud(vm.supabase, eventoId) }

    LaunchedEffect(Unit) {
        GestoreVotazioni.nuovaIdentita(votingRole)
        while (true) {
            GestoreVotazioni.battitoPresenza(vm.supabase, eventoId, mioNome, mioCodice)
            GestoreVotazioni.aggiornaPresenti(vm.supabase, eventoId)
            val s = if (ruolo == "organizzatore") GestoreVotazioni.sessioneCorrente
            else GestoreVotazioni.trovaSessioneAttivaEvento(vm.supabase, eventoId)
            if (s != null) GestoreVotazioni.aggiornaVoti(vm.supabase, s.id)
            delay(3000)
        }
    }
    DisposableEffect(Unit) { onDispose { cleanup.launch { GestoreVotazioni.esci(vm.supabase, eventoId) } } }

    if (ruolo == "organizzatore")
        VistaOrganizzatore(eventoId, stile, votoPubblico, numeroGiudici, onEsci, onProssimaFase)
    else
        VistaPartecipante(eventoId, ruolo, stile)
}

// ─── ORGANIZZATORE ───────────────────────────────────────────────────────────
@Composable
private fun VistaOrganizzatore(
    eventoId: String, stile: StileContest, votoPubblico: Boolean, numeroGiudici: Int,
    onEsci: () -> Unit, onProssimaFase: () -> Unit
) {
    val vm = LocalDatabaseViewModel.current
    val scope = rememberCoroutineScope()
    val font = FontFamily(Font(R.font.komtit__))

    var roundSelId by remember { mutableStateOf<String?>(null) }
    val rounds = GestoreContestBattle.roundsAttuali
    val tuttiDecisi = rounds.isNotEmpty() && rounds.all { it.vincitoreId != null }
    val giudiciAltri = GestoreVotazioni.giudiciPresenti().count { it.partecipante_id != GestoreVotazioni.mioId }
    var rivelaFinale by remember { mutableStateOf(false) }
    val isFinaleUnSolo = GestoreContestBattle.fasePartenza == FaseTorneo.FINALE && tuttiDecisi

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoLive(stile)
            val sel = roundSelId?.let { id -> rounds.find { it.id == id } }
            if (sel == null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 56.dp, bottom = 8.dp)) {
                        IconButton(onClick = onEsci, modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)) { Text("<", color = Color.White, fontSize = 40.sp, fontFamily = font) }
                        Text("BATTLE - ${GestoreContestBattle.fasePartenza.name}", color = Color.White, fontSize = 24.sp, fontFamily = font, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                    }
                    if (giudiciAltri < numeroGiudici) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF332600)).padding(10.dp)) {
                            Text("In attesa dei giudici: $giudiciAltri/$numeroGiudici presenti", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(rounds) { r -> RoundCardContest(round = r, stile = stile, onClick = { roundSelId = r.id }) }
                    }
                    if (tuttiDecisi) {
                        Button(
                            onClick = {
                                if (isFinaleUnSolo) rivelaFinale = true
                                else {
                                    val vincitori = rounds.mapNotNull { r -> r.partecipanti.find { it.id == r.vincitoreId } }
                                    GestoreContestBattle.rosterSelezionato.clear(); GestoreContestBattle.rosterSelezionato.addAll(vincitori)
                                    GestoreContestBattle.roundsAttuali.clear()
                                    GestoreContestBattle.fasePartenza = prossimaFase(GestoreContestBattle.fasePartenza)
                                    scope.launch { GestoreContestBattle.salvaSulCloud(vm.supabase, "configurato") }
                                    onProssimaFase()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)
                        ) { Text(if (isFinaleUnSolo) "RIVELA IL VINCITORE" else "PROSSIMA FASE", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            } else {
                DettaglioRoundOrganizzatore(eventoId, sel, stile, votoPubblico, font, onChiudi = { roundSelId = null })
            }

            if (rivelaFinale) SchermoRivelaVincitore(rounds, stile, onConcludi = {
                scope.launch {
                    GestoreVotazioni.sessioneCorrente?.let { GestoreVotazioni.chiudiSessione(vm.supabase, it.id) }
                    GestoreContestBattle.salvaSulCloud(vm.supabase, "finito")
                    vm.archiviaEvento(eventoId)
                }
                onEsci()
            })
        }
    }
}

@Composable
private fun DettaglioRoundOrganizzatore(
    eventoId: String, round: Round, stile: StileContest, votoPubblico: Boolean,
    font: FontFamily, onChiudi: () -> Unit
) {
    val vm = LocalDatabaseViewModel.current
    val scope = rememberCoroutineScope()

    var iniziato by remember { mutableStateOf(false) }
    var sessioneAperta by remember { mutableStateOf(false) }
    var menuGen by remember { mutableStateOf(false) }
    var generatoreScelto by remember { mutableStateOf<String?>(null) }
    var mostraSottoRound by remember { mutableStateOf(false) }

    val nomiGiudici = GestoreVotazioni.presenti.filter { it.ruolo == "giudice" }.associate { it.partecipante_id to (it.nome ?: "Giudice") }
    val risultato = remember(GestoreVotazioni.voti.size, GestoreVotazioni.presenti.size, sessioneAperta, GestoreVotazioni.sessioneCorrente?.id) {
        GestoreVotazioni.calcolaRisultato(votoPubblico, nomiGiudici)
    }
    // nomi delle opzioni della sessione corrente (Match / Spareggio / sotto-round)
    val opzioni = GestoreVotazioni.sessioneCorrente?.opzioni ?: emptyList()
    fun nomeOpzione(id: String): String = opzioni.find { it.id == id }?.nome ?: id

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 56.dp, bottom = 8.dp)) {
            IconButton(onClick = onChiudi, modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)) { Text("<", color = Color.White, fontSize = 40.sp, fontFamily = font) }
            Text("ROUND ${round.numero}", color = Color.White, fontSize = 24.sp, fontFamily = font, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        }

        if (!iniziato) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Button(onClick = { iniziato = true }, colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text("INIZIA ROUND", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            return@Column
        }

        // lati del round
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            round.partecipanti.forEachIndexed { i, lato ->
                if (i > 0) Image(painter = painterResource(R.drawable.versus), contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape))
                val vincente = round.vincitoreId == lato.id
                Box(modifier = Modifier.border(if (vincente) 4.dp else 0.dp, Color(0xFF4CAF50), RoundedCornerShape(14.dp))) {
                    BoxGruppoContest(freestyler = lato, colore = GestoreContestBattle.coloreGruppoDi(lato.id.split("_").first(), stile.coloreCornici), larghezza = 110)
                }
            }
        }

        // BOTTONI ORGANIZZATORE
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val opz = round.partecipanti.map { OpzioneVoto(it.id, it.nome) }
                    GestoreVotazioni.creaSessione(vm.supabase, eventoId, round.id, "Spareggio", opz); sessioneAperta = true
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(48.dp)) { Text("SPAREGGIO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }

            Box(modifier = Modifier.weight(1f)) {
                Button(onClick = { menuGen = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("GENERATORI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold); Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                }
                DropdownMenu(expanded = menuGen, onDismissRequest = { menuGen = false }, modifier = Modifier.background(Tema.coloreSfondoCard)) {
                    listOf("Parole", "Argomenti", "Modalità", "Taboo", "Linker", "Immagini").forEach { g ->
                        DropdownMenuItem(text = { Text(g, color = Tema.coloreTesto) }, onClick = { generatoreScelto = g; menuGen = false })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // SOTTO-ROUND (1v1 / 1v2 dentro un 2v2 o squadra)
        Button(onClick = { mostraSottoRound = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(46.dp)) {
            Text("SOTTO-ROUND (1v1 / 1v2 ...)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!sessioneAperta) {
            Button(onClick = {
                scope.launch {
                    val opz = round.partecipanti.map { OpzioneVoto(it.id, it.nome) }
                    GestoreVotazioni.creaSessione(vm.supabase, eventoId, round.id, "Match", opz); sessioneAperta = true
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp)) {
                Text("APRI VOTAZIONE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            val etichetta = GestoreVotazioni.sessioneCorrente?.etichetta ?: "Match"
            Text("Votazione: $etichetta — il tuo voto (sei giudice):", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 4.dp), fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                opzioni.forEach { op ->
                    Button(onClick = { scope.launch { GestoreVotazioni.sessioneCorrente?.let { GestoreVotazioni.vota(vm.supabase, it.id, op.id) } } }, colors = ButtonDefaults.buttonColors(containerColor = Tema.coloreSfondoCard), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                        Text(op.nome, color = Color.White, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            Button(onClick = { scope.launch { GestoreVotazioni.sessioneCorrente?.let { GestoreVotazioni.vota(vm.supabase, it.id, "spareggio") } } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2)), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text("Voto: SPAREGGIO", color = Color.White, fontSize = 12.sp) }

            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).padding(12.dp)) {
                Column {
                    Text("VERDETTI ($etichetta)", color = stile.coloreCornici, fontWeight = FontWeight.Bold)
                    risultato.verdettiGiudici.forEach { (nome, scelta) -> Text("• $nome → ${nomeOpzione(scelta)}", color = Color.White, fontSize = 13.sp) }
                    if (votoPubblico) Text("• Spettatori → ${risultato.verdettoSpettatori?.let { nomeOpzione(it) } ?: "—"}", color = Color(0xFF80DEEA), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(if (risultato.spareggio) "RISULTATO: PAREGGIO → spareggio" else "RISULTATO: ${risultato.vincitoreId?.let { nomeOpzione(it) }}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            }
            Button(onClick = { scope.launch { GestoreVotazioni.sessioneCorrente?.let { GestoreVotazioni.chiudiSessione(vm.supabase, it.id) }; sessioneAperta = false } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)) { Text("FINE VOTAZIONI", color = Color.White, fontWeight = FontWeight.Bold) }
        }

        // CONFERMA VINCITORE del round (sempre uno dei lati originali)
        if (!sessioneAperta && iniziato) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Conferma il vincitore del round:", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                round.partecipanti.forEach { lato ->
                    val suggerito = risultato.vincitoreId == lato.id
                    Button(
                        onClick = {
                            val idx = GestoreContestBattle.roundsAttuali.indexOfFirst { it.id == round.id }
                            if (idx != -1) GestoreContestBattle.roundsAttuali[idx] = GestoreContestBattle.roundsAttuali[idx].copy(vincitoreId = lato.id, completato = true)
                            scope.launch { GestoreContestBattle.salvaSulCloud(vm.supabase) }
                            onChiudi()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (suggerito) Color(0xFF4CAF50) else Tema.coloreSfondoCard),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)
                    ) { Text(lato.nome + if (suggerito) " ✓" else "", color = Color.White, fontSize = 11.sp, maxLines = 1) }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // popup generatore
    generatoreScelto?.let { g ->
        Dialog(onDismissRequest = { generatoreScelto = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
                when (g) {
                    "Parole" -> SchermataGeneratoreParole { generatoreScelto = null }
                    "Argomenti" -> SchermataGeneratoreArgomenti { generatoreScelto = null }
                    "Modalità" -> SchermataGeneratoreModalita { generatoreScelto = null }
                    "Taboo" -> SchermataGeneratoreTaboo { generatoreScelto = null }
                    "Linker" -> SchermataGeneratoreLinker { generatoreScelto = null }
                    "Immagini" -> SchermataGeneratoreImmagini { generatoreScelto = null }
                }
            }
        }
    }

    // costruttore sotto-round
    if (mostraSottoRound) {
        CostruttoreSottoRound(round = round, stile = stile, onAnnulla = { mostraSottoRound = false }, onAvvia = { latoA, latoB ->
            mostraSottoRound = false
            scope.launch {
                val a = GestoreContestBattle.creaGruppo(latoA)
                val b = GestoreContestBattle.creaGruppo(latoB)
                val etic = "${latoA.size}v${latoB.size}"
                GestoreVotazioni.creaSessione(vm.supabase, eventoId, round.id, etic, listOf(OpzioneVoto(a.id, a.nome), OpzioneVoto(b.id, b.nome)))
                sessioneAperta = true
            }
        })
    }
}

// Costruttore di un sotto-round: assegna i singoli membri del round a LATO A o LATO B
@Composable
private fun CostruttoreSottoRound(round: Round, stile: StileContest, onAnnulla: () -> Unit, onAvvia: (List<Freestyler>, List<Freestyler>) -> Unit) {
    val font = FontFamily(Font(R.font.komtit__))
    val membri = remember(round.id) { round.partecipanti.flatMap { membriDiLato(it) } }
    // 0 = non assegnato, 1 = A, 2 = B
    val assegnazioni = remember(round.id) { mutableStateMapOf<String, Int>().apply { membri.forEach { put(it.id, 0) } } }
    val latoA = membri.filter { assegnazioni[it.id] == 1 }
    val latoB = membri.filter { assegnazioni[it.id] == 2 }

    Dialog(onDismissRequest = onAnnulla, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(40.dp))
                Text("CREA SOTTO-ROUND", color = Color.White, fontSize = 24.sp, fontFamily = font, fontWeight = FontWeight.Bold)
                Text("Tocca un MC per assegnarlo: 1° tocco → LATO A (verde), 2° → LATO B (azzurro), 3° → libero.", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))

                membri.chunked(2).forEach { riga ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        riga.forEach { mc ->
                            val stato = assegnazioni[mc.id] ?: 0
                            val colore = when (stato) { 1 -> Color(0xFF4CAF50); 2 -> Color(0xFF00B0FF); else -> Color.DarkGray }
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).border(3.dp, colore, RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard)
                                    .clickable { assegnazioni[mc.id] = (stato + 1) % 3 }.padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(mc.nome.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1)
                                    Text(when (stato) { 1 -> "LATO A"; 2 -> "LATO B"; else -> "—" }, color = colore, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (riga.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).padding(12.dp)) {
                    Column {
                        Text("LATO A: ${latoA.joinToString(" & ") { it.nome }.ifBlank { "—" }}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        Text("LATO B: ${latoB.joinToString(" & ") { it.nome }.ifBlank { "—" }}", color = Color(0xFF00B0FF), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onAvvia(latoA, latoB) },
                    enabled = latoA.isNotEmpty() && latoB.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni, disabledContainerColor = Color.DarkGray),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(54.dp)
                ) { Text(if (latoA.isNotEmpty() && latoB.isNotEmpty()) "AVVIA SOTTO-ROUND (${latoA.size}v${latoB.size})" else "ASSEGNA ENTRAMBI I LATI", color = Color.White, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onAnnulla, modifier = Modifier.fillMaxWidth()) { Text("Annulla", color = Tema.coloreTestoSecondario) }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SchermoRivelaVincitore(rounds: List<Round>, stile: StileContest, onConcludi: () -> Unit) {
    var rivelato by remember { mutableStateOf(false) }
    val vincitore = rounds.lastOrNull()?.let { r -> r.partecipanti.find { it.id == r.vincitoreId } }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { rivelato = true }, contentAlignment = Alignment.Center) {
        if (!rivelato) {
            Text("TOCCA PER RIVELARE\nIL VINCITORE", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = FontFamily(Font(R.font.komtit__)))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏆", fontSize = 70.sp)
                Text(vincitore?.nome?.uppercase() ?: "—", color = stile.coloreCornici, fontSize = 38.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = FontFamily(Font(R.font.komtit__)))
                Spacer(modifier = Modifier.height(40.dp))
                Button(onClick = onConcludi, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(12.dp)) { Text("CONCLUDI CONTEST", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ─── GIUDICE / SPETTATORE ──────────────────────────────────────────────────────
@Composable
private fun VistaPartecipante(eventoId: String, ruolo: String, stile: StileContest) {
    val vm = LocalDatabaseViewModel.current
    val scope = rememberCoroutineScope()
    val font = FontFamily(Font(R.font.komtit__))

    val sessione = GestoreVotazioni.sessioneCorrente
    var miaScelta by remember(sessione?.id) { mutableStateOf<String?>(null) }
    var mostraAppunti by remember { mutableStateOf(false) }
    var appunti by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoLive(stile)
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(50.dp))
                Text(if (ruolo == "giudice") "GIUDICE" else "SPETTATORE", color = stile.coloreCornici, fontSize = 20.sp, fontFamily = font, fontWeight = FontWeight.Bold)

                if (sessione == null || sessione.stato != "in_corso") {
                    Spacer(modifier = Modifier.weight(1f))
                    Text("In attesa del prossimo round...", color = Color.White, fontSize = 20.sp, fontFamily = font, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Text(sessione.etichetta, color = Color.White, fontSize = 26.sp, fontFamily = font, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
                    Text("Vota il vincitore:", color = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    sessione.opzioni.forEach { op ->
                        val selOp = miaScelta == op.id
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).border(3.dp, if (selOp) Color(0xFF4CAF50) else stile.coloreCornici, RoundedCornerShape(14.dp)).background(Tema.coloreSfondoCard)
                                .clickable { miaScelta = op.id; scope.launch { GestoreVotazioni.vota(vm.supabase, sessione.id, op.id) } }.padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(op.nome.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(2.dp, Color(0xFF7E57C2), RoundedCornerShape(14.dp))
                            .clickable { miaScelta = "spareggio"; scope.launch { GestoreVotazioni.vota(vm.supabase, sessione.id, "spareggio") } }.padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("SPAREGGIO", color = if (miaScelta == "spareggio") Color(0xFF7E57C2) else Color.White, fontWeight = FontWeight.Bold) }

                    if (miaScelta != null) Text("Voto inviato ✓", color = Color(0xFF4CAF50), modifier = Modifier.padding(top = 12.dp))
                }
            }

            if (ruolo == "giudice") {
                FloatingActionButton(onClick = { mostraAppunti = true }, containerColor = stile.coloreBottoni, contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                    Text("📝", fontSize = 22.sp)
                }
            }
        }
    }

    if (mostraAppunti) {
        AlertDialog(
            onDismissRequest = { mostraAppunti = false }, containerColor = Tema.coloreSfondoCard,
            title = { Text("Appunti (solo tuoi)", color = Tema.coloreTesto, fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = appunti, onValueChange = { appunti = it }, modifier = Modifier.fillMaxWidth().height(220.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = stile.coloreCornici)) },
            confirmButton = { Button(onClick = { mostraAppunti = false }, colors = ButtonDefaults.buttonColors(containerColor = stile.coloreBottoni)) { Text("Chiudi", color = Color.White) } }
        )
    }
}

private fun prossimaFase(f: FaseTorneo): FaseTorneo = when (f) {
    FaseTorneo.OTTAVI -> FaseTorneo.QUARTI
    FaseTorneo.QUARTI -> FaseTorneo.SEMIFINALE
    FaseTorneo.SEMIFINALE -> FaseTorneo.FINALE
    FaseTorneo.FINALE -> FaseTorneo.FINALE
}