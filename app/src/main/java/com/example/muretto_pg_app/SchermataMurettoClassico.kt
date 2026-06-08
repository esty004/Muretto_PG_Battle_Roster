package com.example.muretto_pg_app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataMurettoClassico(
    tipoTorneo: TipoTorneo,
    onTornaAlMenu: () -> Unit,
    onIniziaBattle: () -> Unit
) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val is2v2 = tipoTorneo != TipoTorneo.SINGOLO

    // --- AGGIORNAMENTO DATI DAL CLOUD ---
    LaunchedEffect(Tema.murettoSelezionato) {
        val murettoId = Tema.ottieniIdMurettoAttivo()
        databaseViewModel.fetchMcsDalCloud(murettoId)
    }

    var testoRicerca by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var mcsSelezionati by remember { mutableStateOf(listOf<String>()) }

    var mostraDialogAggiunta by remember { mutableStateOf(false) }
    var nomeNuovoMc by remember { mutableStateOf("") }

    var mostraNotepad by remember { mutableStateOf(false) }
    var testoNotepad by remember { mutableStateOf("") }

    val listaFiltrata = databaseViewModel.listaMcsCloud.filter { it.nome.contains(testoRicerca, ignoreCase = true) }

    BackHandler(enabled = searchFocused || mostraNotepad) {
        if (mostraNotepad) mostraNotepad = false else focusManager.clearFocus()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // <--- IL FIX MAGICO DELLA TASTIERA!
        color = Tema.coloreSfondo
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- NUOVO SFONDO DINAMICO ---
            SfondoSchermata(Modifier.fillMaxSize(), "Sfondo Muretto Classico")
            // Patina scura (al 50%) sopra lo sfondo per rendere leggibili i testi e le card
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            // ---------------------------------

            Column(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 10.dp)) {
                    Text("SELEZIONA GLI MC", color = Color.White, fontSize = 32.sp, fontFamily = FontFamily(Font(R.font.jackboa)), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                if (tipoTorneo == TipoTorneo.COPPIE_PREDEFINITE) {
                    Text("ℹ️ PREDEFINITE: Seleziona gli MC nell'ordine delle coppie (1° con 2°, 3° con 4°...)", color = Tema.colorePrincipale, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                }

                if (is2v2 && mcsSelezionati.size % 2 != 0) {
                    Text("⚠️ NUMERO DISPARI: L'ultimo MC verrà aggiunto da solo!", color = Color(0xFFFFB300), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }

                OutlinedTextField(
                    value = testoRicerca,
                    onValueChange = { testoRicerca = it },
                    placeholder = { Text("Cerca un MC...", color = Tema.coloreTestoSecondario) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).onFocusChanged { searchFocused = it.isFocused },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Tema.colorePrincipale, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, cursorColor = Tema.colorePrincipale),
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )

                // Rotella di caricamento se Firestore sta ancora scaricando
                if (databaseViewModel.staCaricando.value) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Tema.colorePrincipale)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(listaFiltrata) { mc ->
                            val isSelezionato = mcsSelezionati.contains(mc.id)
                            // Aggiungiamo il calcolo dell'indice
                            val indiceSelezione = if (isSelezionato) mcsSelezionati.indexOf(mc.id) else -1

                            CardFreestylerTorneo(
                                freestyler = mc,
                                isSelezionato = isSelezionato,
                                tipoTorneo = tipoTorneo, // Passiamo il tipo per capire se siamo in 2vs2 Predefinite
                                indiceSelezione = indiceSelezione, // Passiamo l'indice!
                                onClick = { mcsSelezionati = if (isSelezionato) mcsSelezionati - mc.id else mcsSelezionati + mc.id }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { mostraNotepad = true },
                containerColor = Color(0xFF4CAF50), contentColor = Color.White, shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 170.dp)
            ) { Text("TxT", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }

            FloatingActionButton(
                onClick = { mostraDialogAggiunta = true },
                containerColor = Color(0xFF4CAF50), contentColor = Color.White, shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 100.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Aggiungi MC", modifier = Modifier.size(30.dp)) }

            // TASTO INDIETRO IN BASSO A SINISTRA
            FloatingActionButton(
                onClick = { if (searchFocused) focusManager.clearFocus() else onTornaAlMenu() },
                containerColor = Tema.colorePrincipale,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 100.dp)
            ) {
                Text("<", fontSize = 30.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp))
            }

            Button(
                onClick = {
                    GestoreBattle.resetSelezione()
                    val selezionatiVeri = mcsSelezionati.mapNotNull { id -> databaseViewModel.listaMcsCloud.find { it.id == id } }
                    GestoreBattle.mcsSelezionati.addAll(selezionatiVeri)
                    val minimoRichiesto = if (is2v2) 4 else 2
                    if (GestoreBattle.mcsSelezionati.size >= minimoRichiesto) onIniziaBattle()
                },
                enabled = mcsSelezionati.size >= (if (is2v2) 4 else 2),
                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale, disabledContainerColor = Color.DarkGray),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.85f).padding(vertical = 20.dp).height(60.dp)
            ) { Text("INIZIA BATTLE", color = Color.White, fontSize = 22.sp, fontFamily = FontFamily(Font(R.font.jackboa))) }

            AnimatedVisibility(
                visible = mostraNotepad,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }), exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { mostraNotepad = false }) { Text("<", color = Tema.coloreTesto, fontSize = 40.sp, fontFamily = MioFontPersonalizzato) }
                            Text("BLOCCO NOTE", color = Tema.coloreTesto, fontSize = 24.sp, fontFamily = MioFontPersonalizzato)

                            Button(
                                onClick = {
                                    scope.launch {
                                        val lines = testoNotepad.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                                        val nuoviIdSelezionati = mutableListOf<String>()

                                        for (line in lines) {
                                            val nomiDaProcessare = if (is2v2) line.split(Regex("(?i)\\s+e\\s+")).map { it.trim() }.filter { it.isNotEmpty() } else listOf(line)

                                            for (nome in nomiDaProcessare) {
                                                val esistente = databaseViewModel.listaMcsCloud.find { it.nome.equals(nome, ignoreCase = true) }
                                                if (esistente != null) {
                                                    nuoviIdSelezionati.add(esistente.id)
                                                } else {
                                                    // --- MECCANICA GLOBALE ---
                                                    val mcGlobale = databaseViewModel.cercaMcGlobale(nome)
                                                    val nuovoMc = mcGlobale ?: Freestyler(
                                                        id = UUID.randomUUID().toString(),
                                                        nome = nome,
                                                        immagineUrl = "",
                                                        muretto_id = if (Tema.isBarreFaul) "2d0f412c-4e9d-4eab-b886-f7a2226d7b9e" else "09fbe1d3-0022-41b8-ba4b-edc887c145a2"
                                                    )
                                                    databaseViewModel.listaMcsCloud.add(nuovoMc)
                                                    nuoviIdSelezionati.add(nuovoMc.id)
                                                }
                                            }
                                        }

                                        mcsSelezionati = (mcsSelezionati + nuoviIdSelezionati).distinct()
                                        testoNotepad = ""
                                        mostraNotepad = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), shape = CircleShape
                            ) { Text("PROSEGUI", color = Color.White, fontWeight = FontWeight.Bold) }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)) {
                            if (testoNotepad.isEmpty()) {
                                val placeholder = if (is2v2) "Scrivi qui...\n(Es. Mogio e Bisca)" else "Scrivi qui...\n(Un nome per riga)"
                                Text(placeholder, color = Tema.coloreTestoSecondario, fontSize = 20.sp, lineHeight = 30.sp)
                            }
                            BasicTextField(
                                value = testoNotepad, onValueChange = { testoNotepad = it },
                                textStyle = TextStyle(color = Tema.coloreTesto, fontSize = 20.sp, lineHeight = 30.sp),
                                cursorBrush = SolidColor(Tema.coloreTesto), modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostraDialogAggiunta) {
        AlertDialog(
            onDismissRequest = { mostraDialogAggiunta = false }, containerColor = Tema.coloreSfondoCard,
            title = { Text("Nuovo MC", color = Tema.coloreTesto, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nomeNuovoMc, onValueChange = { nomeNuovoMc = it }, placeholder = { Text("Nome del Freestyler", color = Tema.coloreTestoSecondario) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto), singleLine = true
                )
            },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), onClick = {
                    scope.launch {
                        if (nomeNuovoMc.isNotBlank()) {
                            val nome = nomeNuovoMc.trim()
                            val esistenteInLista = databaseViewModel.listaMcsCloud.find { it.nome.equals(nome, ignoreCase = true) }

                            if (esistenteInLista == null) {
                                val mcGlobale = databaseViewModel.cercaMcGlobale(nome)
                                val nuovoMc = mcGlobale ?: Freestyler(
                                    id = UUID.randomUUID().toString(),
                                    nome = nome,
                                    immagineUrl = "",
                                    muretto_id = if (Tema.isBarreFaul) "2d0f412c-4e9d-4eab-b886-f7a2226d7b9e" else "09fbe1d3-0022-41b8-ba4b-edc887c145a2"
                                )
                                databaseViewModel.listaMcsCloud.add(nuovoMc)
                            }
                            nomeNuovoMc = ""
                            mostraDialogAggiunta = false
                        }
                    }
                }) { Text("Aggiungi", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { mostraDialogAggiunta = false; nomeNuovoMc = "" }) { Text("Annulla", color = Tema.coloreTestoSecondario) } }
        )
    }
}

@Composable
fun CardFreestylerTorneo(
    freestyler: Freestyler,
    isSelezionato: Boolean,
    tipoTorneo: TipoTorneo,
    indiceSelezione: Int,
    dimensioneGruppo: Int = 2,   // <-- NUOVO: 2 = coppie, N = squadre
    onClick: () -> Unit
) {
    val colorMatrix = remember(isSelezionato) { if (isSelezionato) ColorMatrix().apply { setToSaturation(0f) } else null }
    val safeImageUrl = freestyler.immagineUrl ?: ""
    val imageModel: Any = if (safeImageUrl.isBlank()) R.drawable.no_pic else safeImageUrl

    // Logica per i bordi colorati a coppie!
    val coloreBordo = remember(isSelezionato, indiceSelezione, tipoTorneo) {
        if (!isSelezionato) return@remember Tema.colorePrincipale

        // Se siamo in 2VS2 PREDEFINITE, coloro le coppie a due a due
        if (tipoTorneo == TipoTorneo.COPPIE_PREDEFINITE && indiceSelezione >= 0) {
            val indiceCoppia = indiceSelezione / dimensioneGruppo.coerceAtLeast(1)
            val coloriSquadre = listOf(
                Color(0xFFFF3D00), // Squadra 1 (Rosso/Arancio vivo)
                Color(0xFF00B0FF), // Squadra 2 (Azzurro vivo)
                Color(0xFF00E676), // Squadra 3 (Verde vivo)
                Color(0xFFFFEA00), // Squadra 4 (Giallo)
                Color(0xFFD500F9), // Squadra 5 (Viola)
                Color(0xFF1DE9B6)  // Squadra 6 (Teal)
            )
            coloriSquadre[indiceCoppia % coloriSquadre.size]
        } else {
            // Se è torneo singolo o coppie casuali, bordo standard verde se selezionato
            Color.Green
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, coloreBordo, RoundedCornerShape(12.dp)) // Uso il nuovo coloreBordo
            .background(Tema.coloreSfondoCard)
            .clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.no_pic),
            error = painterResource(R.drawable.no_pic),
            colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null
        )

        if (isSelezionato) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selezionato", tint = coloreBordo, modifier = Modifier.size(60.dp))
            }
        }

        Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text = freestyler.nome.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}