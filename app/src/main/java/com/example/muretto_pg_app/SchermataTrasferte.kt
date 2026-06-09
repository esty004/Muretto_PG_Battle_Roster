package com.example.muretto_pg_app

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun SchermataTrasferte(
    onTornaIndietro: () -> Unit,
    onVaiAllaMappa: () -> Unit,
    onGestisciBattle: (String) -> Unit = {},
    soloPreferiti: Boolean = false,
    eventoIdIniziale: String? = null
) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFont = FontFamily(Font(R.font.komtit__))

    // Filtro la lista in base a cosa stiamo visualizzando
    val eventi = if (soloPreferiti) {
        databaseViewModel.eventiApprovati.filter { databaseViewModel.eventiPreferiti.contains(it.id) }
    } else {
        databaseViewModel.eventiApprovati
    }

    LaunchedEffect(Unit) {
        databaseViewModel.fetchEventiApprovati()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            SfondoSchermata(Modifier.fillMaxSize(), "Sfondo Trasferte")
            // Se siamo in un muretto con gradiente speciale (es. Barre Faul) o sfondi particolari, possiamo gestire la patina
            if (Tema.isBarreFaul) {
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF000000), Tema.colorePrincipale, Tema.coloreSfondo, Tema.colorePrincipale.copy(alpha = 0f))
                    )
                ))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                    val titolo = if (soloPreferiti) "PREFERITI" else "TRASFERTE"
                    Text(titolo, color = Color.White, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                if (eventi.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (soloPreferiti) "Nessuna trasferta salvata." else "Nessuna trasferta programmata.", color = Color.LightGray, textAlign = TextAlign.Center)
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(eventi.size, eventoIdIniziale) {
                        if (eventoIdIniziale != null) {
                            val idx = eventi.indexOfFirst { it.id == eventoIdIniziale }
                            if (idx >= 0) listState.animateScrollToItem(idx)
                        }
                    }
                    LazyColumn(
                        state = listState,                     // <-- AGGIUNTO
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(eventi) { evento ->
                            CardPostEvento(
                                evento = evento,
                                isPreferito = databaseViewModel.eventiPreferiti.contains(evento.id),
                                onTogglePreferito = { databaseViewModel.togglePreferito(evento.id) },
                                isLoggato = databaseViewModel.ruoloAttuale != RuoloUtente.NESSUNO,
                                onGestisciBattle = onGestisciBattle,
                                espansoIniziale = (evento.id == eventoIdIniziale)   // <-- AGGIUNTO
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onTornaIndietro,
                containerColor = Tema.colorePrincipale,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 32.dp)
            ) {
                Text("<", fontSize = 30.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp))
            }

            if (!soloPreferiti) {
                FloatingActionButton(
                    onClick = onVaiAllaMappa,
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 32.dp)
                        .size(70.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.italy_icon),
                        contentDescription = "Mappa Trasferte",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun CardPostEvento(evento: Evento, isPreferito: Boolean, onTogglePreferito: () -> Unit, isLoggato: Boolean, onGestisciBattle: (String) -> Unit, espansoIniziale: Boolean = false) {    val databaseViewModel = LocalDatabaseViewModel.current
    val scope = rememberCoroutineScope()
    var espanso by remember { mutableStateOf(espansoIniziale) }
    val uriHandler = LocalUriHandler.current

    // Dialog di conferma eliminazione
    var mostraDialogElimina by remember { mutableStateOf(false) }

    // Controllo permessi per l'IA
    val puoGestire = databaseViewModel.isAdmin ||
            databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_EVENTI ||
            databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_MURETTO

    // Controllo permessi per l'eliminazione: Admin o l'organizzatore dell'evento stesso
    val puoEliminare = databaseViewModel.isAdmin ||
            (databaseViewModel.profiloAttuale?.id == evento.organizzatore_id)

    if (mostraDialogElimina) {
        AlertDialog(
            onDismissRequest = { mostraDialogElimina = false },
            containerColor = Tema.coloreSfondoCard,
            title = { Text("GESTIONE EVENTO", color = Tema.coloreTesto, fontWeight = FontWeight.Bold) },
            text = { Text("Cosa vuoi fare con questo evento? L'archiviazione lo manterrà nel database per le statistiche.", color = Tema.coloreTestoSecondario) },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            scope.launch {
                                databaseViewModel.archiviaEvento(evento.id)
                                mostraDialogElimina = false
                            }
                        }
                    ) { Text("ARCHIVIA (SOFT DELETE)", color = Color.White, fontWeight = FontWeight.Bold) }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            scope.launch {
                                databaseViewModel.eliminaEventoDefinitivamente(evento.id)
                                mostraDialogElimina = false
                            }
                        }
                    ) { Text("ELIMINA DEFINITIVAMENTE", color = Color.White, fontWeight = FontWeight.Bold) }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = { mostraDialogElimina = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ANNULLA", color = Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { espanso = !espanso }
            .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Tema.coloreSfondoCard)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                if(evento.immagine_url != null) {
                    AsyncImage(
                        model = evento.immagine_url,
                        contentDescription = "Locandina Evento",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Tema.gradienteCard), contentAlignment = Alignment.Center) {
                        Text(evento.titolo.take(1).uppercase(), color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

                Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isLoggato) {
                        IconButton(
                            onClick = onTogglePreferito,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.star_favorite),
                                contentDescription = "Preferito",
                                tint = if (isPreferito) Color.Yellow else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (puoEliminare) {
                        IconButton(
                            onClick = { mostraDialogElimina = true },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Text("🗑️", fontSize = 20.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).align(Alignment.BottomStart).padding(12.dp)
                ) {
                    Text(evento.titolo.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.komtit__)))
                }
            }

            AnimatedVisibility(visible = espanso) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Text("📍 ${evento.location_nome}", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("📅 ${evento.data_ora}", color = Color.DarkGray, fontSize = 16.sp)
                    Text("🎤 ${evento.tipo}", color = Color.DarkGray, fontSize = 16.sp)
                    Text("💰 ${evento.prezzo}", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    if (!evento.descrizione.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(evento.descrizione, color = Color.DarkGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        if (!evento.insta.isNullOrBlank()) {
                            IconButton(onClick = { try { uriHandler.openUri(evento.insta) } catch (e: Exception) {} }) {
                                Image(painterResource(id = R.drawable.instagram), contentDescription = "Instagram", modifier = Modifier.size(55.dp))
                            }
                        }
                        if (!evento.maps.isNullOrBlank()) {
                            IconButton(onClick = { try { uriHandler.openUri(evento.maps) } catch (e: Exception) {} }) {
                                Image(painterResource(id = R.drawable.maps), contentDescription = "Maps", modifier = Modifier.size(55.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}