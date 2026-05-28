package com.example.muretto_pg_app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.UUID

@Composable
fun SchermataAllenamento(onTornaIndietro: () -> Unit, onSelezionaAllenamento: (String) -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))

    // Usiamo rememberSaveable così l'app ricorda in che tab eravamo anche se cambiamo pagina
    var tabSelezionata by rememberSaveable { mutableIntStateOf(0) }

    // Sincronizza l'allenamento col Cloud all'avvio (SENZA resettare le scelte!)
    LaunchedEffect(Tema.isBarreFaul) {
        val murettoId = Tema.ottieniIdMurettoAttivo()
        databaseViewModel.fetchMcsDalCloud(murettoId)
    }

    // Gestione intelligente del tasto "Indietro" del telefono
    BackHandler {
        if (tabSelezionata == 0 && GestoreAllenamento.staMostrandoRisultati) {
            GestoreAllenamento.staMostrandoRisultati = false
        } else {
            GestoreAllenamento.mcsSelezionatiIds = setOf()
            GestoreAllenamento.staMostrandoRisultati = false
            onTornaIndietro()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 10.dp)) {
                IconButton(
                    onClick = {
                        // Gestione intelligente del tasto freccia <
                        if (tabSelezionata == 0 && GestoreAllenamento.staMostrandoRisultati) {
                            GestoreAllenamento.staMostrandoRisultati = false
                        } else {
                            GestoreAllenamento.mcsSelezionatiIds = setOf()
                            GestoreAllenamento.staMostrandoRisultati = false
                            onTornaIndietro()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                ) { Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold) }
                Text("ALLENAMENTO", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = FontFamily(Font(R.font.jackboa)), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            TabRow(
                selectedTabIndex = tabSelezionata, containerColor = Color.Transparent, contentColor = Tema.colorePrincipale,
                indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[tabSelezionata]), color = Tema.colorePrincipale) },
                divider = {}
            ) {
                Tab(selected = tabSelezionata == 0, onClick = { tabSelezionata = 0 }, text = { Text("MATCHMAKING", color = if (tabSelezionata == 0) Tema.coloreTesto else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold, fontSize = 16.sp) })
                Tab(selected = tabSelezionata == 1, onClick = { tabSelezionata = 1 }, text = { Text("GENERATORI", color = if (tabSelezionata == 1) Tema.coloreTesto else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold, fontSize = 16.sp) })
            }

            Box(modifier = Modifier.weight(1f)) {
                if (tabSelezionata == 0) {
                    SezioneMatchmaking(font = MioFontPersonalizzato)
                } else {
                    SezioneGeneratori(MioFontPersonalizzato, onSelezionaAllenamento)
                }
            }
        }
    }
}

@Composable
fun SezioneMatchmaking(font: FontFamily) {
    val databaseViewModel = LocalDatabaseViewModel.current
    var mostraDialogAggiunta by remember { mutableStateOf(false) }
    var nomeNuovoMc by remember { mutableStateOf("") }

    // rememberSaveable per non perdere la ricerca quando si torna indietro
    var testoRicerca by rememberSaveable { mutableStateOf("") }

    val listaFiltrata = databaseViewModel.tuttiMcsCloud.filter { it.nome.contains(testoRicerca, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!GestoreAllenamento.staMostrandoRisultati) {
            // SCHERMATA SELEZIONE MC
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                Text("SELEZIONA GLI MC", color = Tema.coloreTesto, fontSize = 24.sp, fontFamily = FontFamily(Font(R.font.jackboa)), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center)

                OutlinedTextField(
                    value = testoRicerca, onValueChange = { testoRicerca = it }, placeholder = { Text("Cerca un MC...", color = Tema.coloreTestoSecondario) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Tema.colorePrincipale, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto), singleLine = true, shape = RoundedCornerShape(12.dp)
                )

                if (databaseViewModel.staCaricando.value) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Tema.colorePrincipale)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        gridItems(listaFiltrata) { mc ->
                            CardFreestyler(
                                freestyler = mc,
                                isSelezionato = GestoreAllenamento.mcsSelezionatiIds.contains(mc.id),
                                onClick = {
                                    val ids = GestoreAllenamento.mcsSelezionatiIds
                                    GestoreAllenamento.mcsSelezionatiIds = if (ids.contains(mc.id)) ids - mc.id else ids + mc.id
                                }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(onClick = { mostraDialogAggiunta = true }, containerColor = Color(0xFF4CAF50), contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 100.dp)) { Icon(Icons.Default.Add, contentDescription = "Aggiungi MC", modifier = Modifier.size(30.dp)) }

            Button(
                onClick = {
                    GestoreAllenamento.generaMatchmaking(databaseViewModel.tuttiMcsCloud)
                    GestoreAllenamento.staMostrandoRisultati = true
                },
                enabled = GestoreAllenamento.mcsSelezionatiIds.size >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale, disabledContainerColor = Color.DarkGray),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).height(60.dp), shape = RoundedCornerShape(12.dp)
            ) { Text("GENERA BATTLE", color = Color.White, fontSize = 22.sp, fontFamily = font) }

        } else {
            // SCHERMATA RISULTATI BATTLE
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("ACCOPPIAMENTI", color = Tema.coloreTesto, fontSize = 28.sp, fontFamily = font, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center)

                val battaglie = GestoreAllenamento.battleGenerate.filter { it.size > 1 }
                val riposanti = GestoreAllenamento.battleGenerate.filter { it.size == 1 }.flatten()

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    listItems(battaglie) { gruppo -> CardBattleAllenamento(gruppo) }

                    if (riposanti.isNotEmpty()) {
                        listItems(riposanti) { mc ->
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp).background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("RIPOSA: ", color = Tema.coloreTestoSecondario, fontSize = 18.sp, fontFamily = font)
                                Spacer(modifier = Modifier.width(12.dp))
                                BoxMC(mc = mc, width = 60.dp, height = 80.dp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { GestoreAllenamento.generaMatchmaking(databaseViewModel.tuttiMcsCloud) },
                        colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(55.dp).weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RELOAD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    var menuAperto by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1.2f)) {
                        Button(
                            onClick = { menuAperto = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(GestoreAllenamento.modoAttuale.etichetta, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("▼", color = Color.White, fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = menuAperto,
                            onDismissRequest = { menuAperto = false },
                            modifier = Modifier.background(Tema.coloreSfondoCard)
                        ) {
                            ModoMatchmaking.entries.forEach { modo ->
                                DropdownMenuItem(
                                    text = { Text(modo.etichetta, color = Tema.coloreTesto) },
                                    onClick = {
                                        GestoreAllenamento.modoAttuale = modo
                                        GestoreAllenamento.generaMatchmaking(databaseViewModel.tuttiMcsCloud)
                                        menuAperto = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostraDialogAggiunta) {
        AlertDialog(
            onDismissRequest = { mostraDialogAggiunta = false }, containerColor = Tema.coloreSfondoCard,
            title = { Text("NUOVO MC", color = Tema.coloreTesto, fontWeight = FontWeight.Bold, fontFamily = font) },
            text = {
                OutlinedTextField(
                    value = nomeNuovoMc, onValueChange = { nomeNuovoMc = it }, placeholder = { Text("Nome del Freestyler", color = Tema.coloreTestoSecondario) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale), singleLine = true
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                    onClick = {
                        if (nomeNuovoMc.isNotBlank()) {
                            val nome = nomeNuovoMc.trim()
                            val esistenteInLista = databaseViewModel.tuttiMcsCloud.find { it.nome.equals(nome, ignoreCase = true) }

                            if (esistenteInLista == null) {
                                val mcGlobale = databaseViewModel.cercaMcGlobale(nome)
                                val nuovoMc = mcGlobale ?: Freestyler(System.currentTimeMillis().toString(), nome, "", if (Tema.isBarreFaul) "barre_faul" else "muretto_pg")
                                databaseViewModel.tuttiMcsCloud.add(nuovoMc)
                                GestoreAllenamento.mcsSelezionatiIds = GestoreAllenamento.mcsSelezionatiIds + nuovoMc.id
                            } else {
                                GestoreAllenamento.mcsSelezionatiIds = GestoreAllenamento.mcsSelezionatiIds + esistenteInLista.id
                            }
                            nomeNuovoMc = ""
                            mostraDialogAggiunta = false
                        }
                    }
                ) { Text("AGGIUNGI", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { mostraDialogAggiunta = false; nomeNuovoMc = "" }) { Text("ANNULLA", color = Tema.coloreTestoSecondario) } }
        )
    }
}

@Composable
fun CardBattleAllenamento(mcs: List<Freestyler>) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(3.dp, Tema.colorePrincipale, RoundedCornerShape(24.dp)).background(brush = Tema.gradienteCard).padding(horizontal = 8.dp, vertical = 16.dp)) {
        if (GestoreAllenamento.modoAttuale == ModoMatchmaking.DUE_VS_DUE && mcs.size >= 3) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                // SQUADRA 1
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BoxMC(mc = mcs[0], width = 65.dp, height = 90.dp)
                    BoxMC(mc = mcs[1], width = 65.dp, height = 90.dp)
                }

                Image(painter = painterResource(id = R.drawable.versus), contentDescription = "Versus", modifier = Modifier.size(40.dp).padding(horizontal = 4.dp))

                // SQUADRA 2
                if (mcs.size == 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BoxMC(mc = mcs[2], width = 65.dp, height = 90.dp)
                        BoxMC(mc = mcs[3], width = 65.dp, height = 90.dp)
                    }
                } else {
                    BoxMC(mc = mcs[2], width = 65.dp, height = 90.dp) // Caso dispari (2 vs 1)
                }
            }
        } else {
            // MODO 1v1 o RUMBLE 1v1v1
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                mcs.forEachIndexed { index, mc ->
                    val isTre = mcs.size == 3
                    val dimCardWidth = if (isTre) 85.dp else 100.dp
                    val dimCardHeight = if (isTre) 110.dp else 130.dp

                    BoxMC(mc = mc, width = dimCardWidth, height = dimCardHeight)

                    if (index < mcs.size - 1) {
                        val vsSize = if (isTre) 35.dp else 50.dp
                        Image(painter = painterResource(id = R.drawable.versus), contentDescription = "Versus", modifier = Modifier.size(vsSize).padding(horizontal = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CardFreestyler(freestyler: Freestyler, isSelezionato: Boolean, onClick: () -> Unit) {
    val colorMatrix = remember(isSelezionato) { if (isSelezionato) ColorMatrix().apply { setToSaturation(0f) } else null }

    // Disinneschiamo il null pointer per l'immagine
    val safeImageUrl = freestyler.immagineUrl ?: ""
    val imageModel: Any = if (safeImageUrl.isBlank()) R.drawable.no_pic else safeImageUrl

    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(12.dp)).border(3.dp, if(isSelezionato) Color.Green else Tema.colorePrincipale, RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = imageModel, contentDescription = "Foto di ${freestyler.nome}", modifier = Modifier.fillMaxSize(), alignment = Alignment.TopCenter, contentScale = ContentScale.Crop, placeholder = painterResource(R.drawable.no_pic), error = painterResource(R.drawable.no_pic), colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null
        )

        if (isSelezionato) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, contentDescription = "Selezionato", tint = Color.Green, modifier = Modifier.size(60.dp)) }
        }

        Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text = freestyler.nome.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SezioneGeneratori(font: FontFamily, onSelezionaAllenamento: (String) -> Unit) {
    val opzioniAllenamento = listOf(
        "Generatore parole",
        "Generatore argomenti",
        "Generatore modalita",
        "Generatore taboo",
        "Generatore linker",
        "Generatore immagini"
    )
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize()) {
        listItems(opzioniAllenamento) { nome -> CardAllenamento(nomeOpzione = nome, onClick = { onSelezionaAllenamento(nome) }, font = font) }
    }
}

@Composable
fun CardAllenamento(nomeOpzione: String, onClick: () -> Unit, font: FontFamily) {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).border(3.dp, Tema.colorePrincipale, RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text = nomeOpzione.uppercase(), color = Tema.coloreTesto, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = font) }
}