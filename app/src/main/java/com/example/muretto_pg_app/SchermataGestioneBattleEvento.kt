package com.example.muretto_pg_app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun SchermataGestioneBattleEvento(
    eventoId: String,
    onTornaIndietro: () -> Unit,
    onNavigate: (String) -> Unit = {}     // <-- NUOVO: per aprire le schermate del builder
) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val MioFont = FontFamily(Font(R.font.komtit__))

    val evento = databaseViewModel.eventiApprovati.find { it.id == eventoId }
    val design = evento?.contest_design

    var tabSelezionata by remember { mutableIntStateOf(1) } // parte da Verdetti

    // VERDETTI
    var sistemaVoti by remember { mutableStateOf(design?.sistema_voti ?: "Verdetto Singolo") }
    var votoPubblico by remember { mutableStateOf(design?.voto_pubblico ?: false) }
    var numeroGiudici by remember { mutableFloatStateOf((design?.numero_giudici ?: 0).toFloat()) }
    var staSalvando by remember { mutableStateOf(false) }

    fun generaCodice(lunghezza: Int = 6): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..lunghezza).map { chars.random() }.joinToString("")
    }

    // Carica lo stato del tabellone di questo contest (gestore separato)
    LaunchedEffect(eventoId) {
        if (GestoreContestBattle.eventoIdCorrente != eventoId) {
            GestoreContestBattle.caricaDalCloud(databaseViewModel.supabase, eventoId)
        }
    }

    if (evento == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Tema.colorePrincipale) }
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth().background(Tema.coloreSfondoCard).padding(top = 50.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)) {
                IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("<", color = Tema.coloreTesto, fontSize = 40.sp, fontFamily = MioFont)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                    Text("GESTIONE CONTEST", color = Tema.colorePrincipale, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(evento.titolo.uppercase(), color = Tema.coloreTesto, fontSize = 24.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold)
                }
            }

            TabRow(
                selectedTabIndex = tabSelezionata, containerColor = Tema.coloreSfondoCard, contentColor = Tema.colorePrincipale,
                indicator = { tp -> TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tp[tabSelezionata]), color = Tema.colorePrincipale) }
            ) {
                Tab(selected = tabSelezionata == 0, onClick = { tabSelezionata = 0 }, text = { Text("ESTETICA", color = if (tabSelezionata == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold) })
                Tab(selected = tabSelezionata == 1, onClick = { tabSelezionata = 1 }, text = { Text("VERDETTI", color = if (tabSelezionata == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold) })
                Tab(selected = tabSelezionata == 2, onClick = { tabSelezionata = 2 }, text = { Text("BATTLE", color = if (tabSelezionata == 2) Color.White else Color.Gray, fontWeight = FontWeight.Bold) })
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (tabSelezionata) {

                    // ─────────────────── ESTETICA ───────────────────
                    0 -> EditorEstetica(eventoId = eventoId, design = design, font = MioFont)

                    // ─────────────────── VERDETTI ───────────────────
                    1 -> {
                        Text("SISTEMA DI VOTAZIONE", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Verdetto: ogni giudice sceglie il vincitore (o spareggio).", color = Tema.coloreTestoSecondario, fontSize = 14.sp)
                        // sistemaVoti resta fisso a "Verdetto Singolo"
                        LaunchedEffect(Unit) { sistemaVoti = "Verdetto Singolo" }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Voto del Pubblico", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Consenti al pubblico di votare tramite QR", color = Color.Gray, fontSize = 12.sp)
                            }
                            Switch(checked = votoPubblico, onCheckedChange = { votoPubblico = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Tema.colorePrincipale))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).padding(16.dp)) {
                            Text("Giudici extra: ${numeroGiudici.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Tu (organizzatore) sei già conteggiato come giudice. Totale: ${numeroGiudici.toInt() + 1}.", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(value = numeroGiudici, onValueChange = { numeroGiudici = it }, valueRange = 0f..5f, steps = 4, colors = SliderDefaults.colors(thumbColor = Tema.colorePrincipale, activeTrackColor = Tema.colorePrincipale))
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Button(
                            onClick = {
                                staSalvando = true
                                scope.launch {
                                    val codPubblico = if (votoPubblico) "PUB-${generaCodice(6)}" else null
                                    val linkPubblico = if (votoPubblico) "https://freestapp.com/vote?code=$codPubblico" else null
                                    val qrPubblico = if (votoPubblico) "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=$linkPubblico" else null
                                    val listaGiudici = mutableListOf<String>()
                                    for (i in 1..numeroGiudici.toInt()) listaGiudici.add("GIU$i-${generaCodice(4)}")
                                    val codGiudici = if (listaGiudici.isNotEmpty()) listaGiudici.joinToString(",") else null
                                    databaseViewModel.salvaImpostazioniVerdetti(eventoId, sistemaVoti, votoPubblico, numeroGiudici.toInt(), codGiudici, codPubblico, qrPubblico, linkPubblico)
                                    staSalvando = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(12.dp)
                        ) {
                            if (staSalvando) CircularProgressIndicator(color = Color.White)
                            else Text(if (design?.codice_accesso_pubblico == null && design?.codici_accesso_giudici == null) "GENERA CODICI E SALVA" else "AGGIORNA E RIGENERA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        if (design?.codice_accesso_pubblico != null || design?.codici_accesso_giudici != null) {
                            Divider(color = Color.DarkGray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("CODICI DI ACCESSO", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 22.sp, fontFamily = MioFont)

                            // QR PUBBLICO
                            if (design.qr_code_pubblico != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                BloccoCodice(
                                    titolo = "VOTO PUBBLICO",
                                    sottotitolo = "Fai inquadrare questo QR ai fan",
                                    qrUrl = design.qr_code_pubblico,
                                    codice = "Codice App: ${design.codice_accesso_pubblico}",
                                    link = design.link_pubblico,
                                    font = MioFont
                                )
                            }

                            // GIUDICI: ora ognuno ha codice + link + QR (derivati dal codice)
                            if (!design.codici_accesso_giudici.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("PASS GIURIA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Consegna codice/link/QR a ciascun giudice:", color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                design.codici_accesso_giudici.split(",").forEachIndexed { index, codice ->
                                    val linkG = "https://freestapp.com/vote?code=$codice"
                                    val qrG = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=$linkG"
                                    Spacer(modifier = Modifier.height(12.dp))
                                    BloccoCodice(
                                        titolo = "GIUDICE ${index + 1}",
                                        sottotitolo = "Pass privato del giudice",
                                        qrUrl = qrG,
                                        codice = "Codice: $codice",
                                        link = linkG,
                                        font = MioFont
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }

                    // ─────────────────── BATTLE (Parte Tre) ───────────────────
                    2 -> {
                        val haTabellone = GestoreContestBattle.roundsAttuali.isNotEmpty() || GestoreContestBattle.stato == "configurato"
                        Box(modifier = Modifier.fillMaxSize().padding(top = 30.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Icon(painter = painterResource(id = R.drawable.versus), contentDescription = null, tint = Tema.colorePrincipale, modifier = Modifier.size(90.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("ZONA BATTLE", color = Color.White, fontSize = 28.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(20.dp))

                                if (!haTabellone) {
                                    Button(
                                        onClick = {
                                            GestoreContestBattle.eventoIdCorrente = eventoId
                                            onNavigate("config_battle/$eventoId")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.85f).height(56.dp)
                                    ) { Text("CONFIGURA BATTLE", color = Color.White, fontWeight = FontWeight.Bold) }
                                } else {
                                    Button(
                                        onClick = { onNavigate("builder_contest/$eventoId") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.85f).height(54.dp)
                                    ) { Text("MODIFICA ROUND", color = Color.White, fontWeight = FontWeight.Bold) }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            // modifica roster ⇒ i round vanno rifatti
                                            GestoreContestBattle.roundsAttuali.clear()
                                            GestoreContestBattle.stato = "bozza"
                                            onNavigate("roster_contest/$eventoId")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.85f).height(54.dp)
                                    ) { Text("MODIFICA ROSTER", color = Color.White, fontWeight = FontWeight.Bold) }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                GestoreContestBattle.salvaSulCloud(databaseViewModel.supabase, "iniziato")
                                                onNavigate("battle_live/$eventoId/organizzatore")
                                            }
                                        },
                                        enabled = GestoreContestBattle.stato == "configurato",
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), disabledContainerColor = Color.DarkGray),
                                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.85f).height(56.dp)
                                    ) { Text("INIZIA BATTLE", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Blocco riutilizzabile per mostrare un QR + codice + link (pubblico o giudice)
@Composable
private fun BloccoCodice(titolo: String, sottotitolo: String, qrUrl: String, codice: String, link: String?, font: FontFamily) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).border(2.dp, Tema.colorePrincipale, RoundedCornerShape(12.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(titolo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(sottotitolo, color = Color.Gray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        AsyncImage(model = qrUrl, contentDescription = "QR", modifier = Modifier.size(180.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(8.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(codice, color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = font)
        if (!link.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(link, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

// ─── EDITOR ESTETICA (tab 0) ───────────────────────────────────────────────
@Composable
private fun EditorEstetica(eventoId: String, design: ContestDesign?, font: FontFamily) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun parse(hex: String?, fb: Color) = try { if (hex.isNullOrBlank()) fb else Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { fb }
    fun Color.toHex(): String {
        val a = (alpha * 255).toInt(); val r = (red * 255).toInt(); val g = (green * 255).toInt(); val b = (blue * 255).toInt()
        return String.format("#%02X%02X%02X%02X", a, r, g, b)
    }

    var tipoStile by remember { mutableStateOf(design?.tipo_stile ?: "DEFAULT") }
    var coloreCornici by remember { mutableStateOf(parse(design?.colore_sfondo, Color.White)) }
    var coloreBottoni by remember { mutableStateOf(parse(design?.colore_primario, Tema.colorePrincipale)) }
    var sfondoUri by remember { mutableStateOf<Uri?>(null) }
    var sfondoCardUri by remember { mutableStateOf<Uri?>(null) }
    var staSalvando by remember { mutableStateOf(false) }
    var esito by remember { mutableStateOf("") }

    val selettoreSfondo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { sfondoUri = it }
    val selettoreSfondoCard = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { sfondoCardUri = it }
    val palette = listOf(Color.White, Color.Black, Color(0xFFD32F2F), Color(0xFF1E88E5), Color(0xFFFF9800), Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFFFEB3B), Color(0xFF00BCD4))

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("STILE DEL CONTEST", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("DEFAULT", "CUSTOM", "DELEGA").forEach { opt ->
                val sel = tipoStile == opt
                Box(modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) Tema.colorePrincipale else Tema.coloreSfondoCard).border(2.dp, if (sel) Color.White else Color.Transparent, RoundedCornerShape(12.dp)).clickable { tipoStile = opt }, contentAlignment = Alignment.Center) {
                    Text(opt, color = if (sel) Color.White else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (tipoStile == "CUSTOM") {
            Spacer(modifier = Modifier.height(20.dp))
            Text("SFONDO GENERALE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).border(2.dp, coloreCornici, RoundedCornerShape(12.dp)).clickable { selettoreSfondo.launch("image/*") }, contentAlignment = Alignment.Center) {
                if (sfondoUri != null) AsyncImage(model = sfondoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else if (!design?.sfondo_custom_url.isNullOrBlank()) AsyncImage(model = design?.sfondo_custom_url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text("Tocca per scegliere lo sfondo", color = Tema.coloreTestoSecondario)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("SFONDO CARD DEL ROUND", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).border(2.dp, coloreCornici, RoundedCornerShape(12.dp)).clickable { selettoreSfondoCard.launch("image/*") }, contentAlignment = Alignment.Center) {
                if (sfondoCardUri != null) AsyncImage(model = sfondoCardUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else if (!design?.sfondo_card_url.isNullOrBlank()) AsyncImage(model = design?.sfondo_card_url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text("Tocca per lo sfondo delle card round", color = Tema.coloreTestoSecondario)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("COLORE CORNICI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                palette.forEach { c ->
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(c).border(3.dp, if (coloreCornici == c) Color.White else Color.Transparent, CircleShape).clickable { coloreCornici = c }, contentAlignment = Alignment.Center) {
                        if (coloreCornici == c) Icon(Icons.Default.Check, contentDescription = null, tint = if (c == Color.White) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("COLORE BOTTONI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                palette.forEach { c ->
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(c).border(3.dp, if (coloreBottoni == c) Color.White else Color.Transparent, CircleShape).clickable { coloreBottoni = c }, contentAlignment = Alignment.Center) {
                        if (coloreBottoni == c) Icon(Icons.Default.Check, contentDescription = null, tint = if (c == Color.White) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (tipoStile == "DELEGA") {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Lo stile è delegato agli admin. La descrizione fornita in creazione è visibile nelle notifiche admin.", color = Color.LightGray, fontSize = 13.sp)
            if (!design?.descrizione_delega.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).padding(12.dp)) {
                    Text("Richiesta: ${design?.descrizione_delega}", color = Tema.coloreTesto, fontSize = 14.sp)
                }
            }
        }

        if (esito.isNotEmpty()) { Spacer(modifier = Modifier.height(12.dp)); Text(esito, color = if (esito.contains("Errore")) Color.Red else Color.Green, fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                staSalvando = true; esito = ""
                scope.launch {
                    val bytes = sfondoUri?.let { uri -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                    val bytesCard = sfondoCardUri?.let { uri -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                    val ok = databaseViewModel.aggiornaDesignContest(
                        eventoId = eventoId,
                        tipoStile = tipoStile,
                        colorePrimario = if (tipoStile == "CUSTOM") coloreBottoni.toHex() else null,
                        coloreSfondo = if (tipoStile == "CUSTOM") coloreCornici.toHex() else null,
                        sfondoBytes = bytes,
                        sfondoCardBytes = bytesCard,
                        delegaCompletata = if (tipoStile != "DELEGA") true else null
                    )
                    staSalvando = false
                    esito = if (ok) "Estetica salvata!" else "Errore nel salvataggio."
                }
            },
            enabled = !staSalvando,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { if (staSalvando) CircularProgressIndicator(color = Color.White) else Text("SALVA ESTETICA", color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(20.dp))
    }
}