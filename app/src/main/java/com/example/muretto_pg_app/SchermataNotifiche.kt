package com.example.muretto_pg_app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun SchermataNotifiche(onTornaIndietro: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFont = FontFamily(Font(R.font.komtit__))

    // Divisione chirurgica delle notifiche!
    val richieste = databaseViewModel.richiesteInAttesa
    val eventiNormali = databaseViewModel.eventiInAttesa.filter { it.contest_design == null }
    val contestInAttesa = databaseViewModel.eventiInAttesa.filter { it.contest_design != null }
    val contestInDelega = StatoDelegaContest.lista
    var tabSelezionata by remember { mutableIntStateOf(0) }
    var richiestaSelezionata by remember { mutableStateOf<ProfiloUtente?>(null) }
    var eventoSelezionato by remember { mutableStateOf<Evento?>(null) }

    LaunchedEffect(Unit) {
        databaseViewModel.fetchRichiesteInAttesa()
        databaseViewModel.fetchEventiInAttesa()
        databaseViewModel.fetchContestInDelega()
    }

    richiestaSelezionata?.let { richiesta -> DialogDettaglioRichiesta(richiesta = richiesta, onDismiss = { richiestaSelezionata = null }, onChiudi = { richiestaSelezionata = null }) }
    eventoSelezionato?.let { evento -> DialogDettaglioEvento(evento = evento, onDismiss = { eventoSelezionato = null }, onChiudi = { eventoSelezionato = null }) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1A1A1A)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth().padding(top = 56.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)) {
                IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) { Text("<", color = Color.White, fontSize = 45.sp, fontFamily = MioFont) }
                Text("NOTIFICHE", color = Color.White, fontSize = 28.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))

                val totaleNotifiche = richieste.size + eventiNormali.size + contestInAttesa.size
                if (totaleNotifiche > 0) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd).size(28.dp).background(Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                        Text(totaleNotifiche.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // TABS A TRE SCHEDE!
            TabRow(
                selectedTabIndex = tabSelezionata,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF4CAF50),
                indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[tabSelezionata]), color = Color(0xFF4CAF50)) },
                divider = {}
            ) {
                Tab(selected = tabSelezionata == 0, onClick = { tabSelezionata = 0 }, text = { Text("ACCOUNT (${richieste.size})", color = if (tabSelezionata == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) })
                Tab(selected = tabSelezionata == 1, onClick = { tabSelezionata = 1 }, text = { Text("EVENTI (${eventiNormali.size})", color = if (tabSelezionata == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) })
                Tab(selected = tabSelezionata == 2, onClick = { tabSelezionata = 2 }, text = { Text("CONTEST (${contestInAttesa.size})", color = if (tabSelezionata == 2) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) })
                Tab(selected = tabSelezionata == 3, onClick = { tabSelezionata = 3 }, text = { Text("ESTETICHE (${contestInDelega.size})", color = if (tabSelezionata == 3) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) })
            }

            // LISTA DINAMICA
            val listaCorrenteVuota = (tabSelezionata == 0 && richieste.isEmpty()) || (tabSelezionata == 1 && eventiNormali.isEmpty()) || (tabSelezionata == 2 && contestInAttesa.isEmpty()) || (tabSelezionata == 3 && contestInDelega.isEmpty())
            if (listaCorrenteVuota) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Nessuna notifica in attesa", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (tabSelezionata) {
                        0 -> items(richieste) { r -> CardRichiesta(richiesta = r, onClick = { richiestaSelezionata = r }) }
                        1 -> items(eventiNormali) { e -> CardEventoInAttesa(evento = e, onClick = { eventoSelezionato = e }) }
                        2 -> items(contestInAttesa) { c -> CardEventoInAttesa(evento = c, isContest = true, onClick = { eventoSelezionato = c }) }
                        3 -> items(contestInDelega) { c ->
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, Color(0xFF9C27B0), RoundedCornerShape(16.dp)).background(Color(0xFF222222)).clickable { onNavigate("dettaglio_contest/${c.id}") }.padding(16.dp)) {
                                Column {
                                    Text(c.titolo.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("Richiesta stile: ${c.contest_design?.descrizione_delega ?: "—"}", color = Color.Gray, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("TOCCA PER IMPOSTARE L'ESTETICA ›", color = Color(0xFF9C27B0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── CARDS ────────────────────────────────Fprofilo───────────────────────────────────

@Composable
fun CardRichiesta(richiesta: ProfiloUtente, onClick: () -> Unit) {
    val etichettaTipo = when (richiesta.tipo_account) { "organizzatore_muretto" -> "Org. Muretto"; "organizzatore_eventi" -> "Org. Eventi"; else -> richiesta.tipo_account }
    val coloreTipo = when (richiesta.tipo_account) { "organizzatore_muretto" -> Color(0xFFD32F2F); "organizzatore_eventi" -> Color(0xFF1E88E5); else -> Color.Gray }
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, Color(0xFF333333), RoundedCornerShape(16.dp)).background(Color(0xFF222222)).clickable { onClick() }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(richiesta.nome_arte.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${richiesta.nome} ${richiesta.cognome}", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.background(coloreTipo.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).border(1.dp, coloreTipo, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(etichettaTipo, color = coloreTipo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("›", color = Color.Gray, fontSize = 24.sp)
        }
    }
}

@Composable
fun CardEventoInAttesa(evento: Evento, isContest: Boolean = false, onClick: () -> Unit) {
    val coloreTag = if (isContest) Color(0xFFFF9800) else Color(0xFF4CAF50)
    val etichetta = if (isContest) "NUOVO CONTEST" else "NUOVO EVENTO"

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, Color(0xFF333333), RoundedCornerShape(16.dp)).background(Color(0xFF222222)).clickable { onClick() }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(evento.titolo.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${evento.data_ora} • ${evento.location_nome}", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.background(coloreTag.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).border(1.dp, coloreTag, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(etichetta, color = coloreTag, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("›", color = Color.Gray, fontSize = 24.sp)
        }
    }
}

// ─── DIALOGS ─────────────────────────────────────────────────────────────────

@Composable
fun DialogDettaglioRichiesta(richiesta: ProfiloUtente, onDismiss: () -> Unit, onChiudi: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var staElaborando by remember { mutableStateOf(false) }
    var messaggioEsito by remember { mutableStateOf("") }
    var operazioneCompletata by remember { mutableStateOf(false) }

    val etichettaMuretto = when (richiesta.muretto_id) {
        "09fbe1d3-0022-41b8-ba4b-edc887c145a2" -> "Muretto PG"
        "2d0f412c-4e9d-4eab-b886-f7a2226d7b9e" -> "Barre Faul"
        "22ea8a2f-d45d-40b2-a6ee-841058f12f99" -> "Fortitudo"
        "IL_TUO_UUID_DI_ATENEO" -> "Ateneo"
        else -> "-"
    }

    Dialog(onDismissRequest = { if (!staElaborando) onDismiss() }) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp)).border(2.dp, Color(0xFF333333), RoundedCornerShape(20.dp)).padding(24.dp)) {
            Column {
                Text("RICHIESTA ACCOUNT", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                if (operazioneCompletata) {
                    Text(messaggioEsito, color = if (messaggioEsito.contains("Errore")) Color.Red else Color(0xFF4CAF50), fontSize = 16.sp, modifier = Modifier.padding(vertical = 16.dp))
                    Button(onClick = onChiudi, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)), modifier = Modifier.fillMaxWidth()) { Text("CHIUDI", color = Color.White) }
                    return@Column
                }

                RigaDato("Nome", "${richiesta.nome} ${richiesta.cognome}")
                RigaDato("Nome d'arte", richiesta.nome_arte)
                RigaDato("Email", richiesta.email)
                RigaDato("Telefono", richiesta.telefono)
                RigaDato("Tipo account", when (richiesta.tipo_account) { "organizzatore_muretto" -> "Organizzatore Muretto"; "organizzatore_eventi" -> "Organizzatore Eventi"; else -> richiesta.tipo_account })
                if (richiesta.tipo_account == "organizzatore_muretto") RigaDato("Muretto", etichettaMuretto)

                if (messaggioEsito.isNotEmpty()) Text(messaggioEsito, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))

                Spacer(modifier = Modifier.height(20.dp))

                if (staElaborando) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF4CAF50)) }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                staElaborando = true
                                scope.launch {
                                    val ok = databaseViewModel.rifiutaRichiesta(richiesta.id)
                                    staElaborando = false; operazioneCompletata = true
                                    messaggioEsito = if (ok) "Richiesta rifiutata." else "Errore durante il rifiuto."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(50.dp)
                        ) { Text("RIFIUTA", color = Color.Red, fontWeight = FontWeight.Bold) }

                        Button(
                            onClick = {
                                staElaborando = true
                                scope.launch {
                                    val ok = databaseViewModel.accettaRichiesta(richiesta)
                                    staElaborando = false
                                    if (ok) {
                                        val numero = richiesta.telefono.replace("+", "").replace(" ", "").replace("-", "")
                                        val messaggio = Uri.encode("La tua richiesta per FreestApp è stata accettata! 🎉\nOra puoi accedere subito all'app usando la tua email e la password che hai scelto.")
                                        val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://wa.me/$numero?text=$messaggio") }
                                        try { context.startActivity(intent) } catch (e: Exception) { }
                                        operazioneCompletata = true; messaggioEsito = "Account creato e attivato!"
                                    } else {
                                        messaggioEsito = "Errore durante la creazione dell'account."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(50.dp)
                        ) { Text("ACCETTA", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { if (!staElaborando) onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Annulla", color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
fun DialogDettaglioEvento(evento: Evento, onDismiss: () -> Unit, onChiudi: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val scope = rememberCoroutineScope()
    var staElaborando by remember { mutableStateOf(false) }
    var messaggioEsito by remember { mutableStateOf("") }
    var operazioneCompletata by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!staElaborando) onDismiss() }) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp)).border(2.dp, Color(0xFF333333), RoundedCornerShape(20.dp)).padding(24.dp)) {
            Column {
                Text("APPROVAZIONE EVENTO", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                if (operazioneCompletata) {
                    Text(messaggioEsito, color = if (messaggioEsito.contains("Errore")) Color.Red else Color(0xFF4CAF50), fontSize = 16.sp, modifier = Modifier.padding(vertical = 16.dp))
                    Button(onClick = onChiudi, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)), modifier = Modifier.fillMaxWidth()) { Text("CHIUDI", color = Color.White) }
                    return@Column
                }

                if (evento.immagine_url != null) {
                    AsyncImage(model = evento.immagine_url, contentDescription = "Locandina", modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                RigaDato("Titolo", evento.titolo)
                RigaDato("Data e Ora", evento.data_ora)
                RigaDato("Location", evento.location_nome)
                RigaDato("Tipo Evento", evento.tipo)
                RigaDato("Prezzo", evento.prezzo)

                if (messaggioEsito.isNotEmpty()) Text(messaggioEsito, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))

                Spacer(modifier = Modifier.height(20.dp))

                if (staElaborando) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF4CAF50)) }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                staElaborando = true
                                scope.launch {
                                    val ok = databaseViewModel.rifiutaEvento(evento.id)
                                    staElaborando = false; operazioneCompletata = true
                                    messaggioEsito = if (ok) "Evento scartato e rimosso." else "Errore."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(50.dp)
                        ) { Text("SCARTA", color = Color.Red, fontWeight = FontWeight.Bold) }

                        Button(
                            onClick = {
                                staElaborando = true
                                scope.launch {
                                    val ok = databaseViewModel.accettaEvento(evento.id)
                                    staElaborando = false; operazioneCompletata = true
                                    messaggioEsito = if (ok) "Evento Pubblicato!" else "Errore."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(50.dp)
                        ) { Text("PUBBLICA", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { if (!staElaborando) onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Annulla", color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
fun RigaDato(etichetta: String, valore: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(etichetta, color = Color.Gray, fontSize = 12.sp)
        Text(valore, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)

        // MODIFICA SOLO QUESTA RIGA QUI SOTTO:
        Divider(color = Color(0xFF333333), thickness = 0.5.dp, modifier = Modifier.padding(top = 4.dp))
    }
}