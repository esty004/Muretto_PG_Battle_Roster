package com.example.muretto_pg_app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun SchermataAllenamento(onTornaIndietro: () -> Unit, onSelezionaAllenamento: (String) -> Unit) {
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))

    // Sincronizza l'allenamento col Cloud
    LaunchedEffect(Tema.isBarreFaul) {
        val murettoId = if (Tema.isBarreFaul) "barre_faul" else "muretto_pg"
        DatabaseMcs.fetchMcsDalCloud(murettoId)
        GestoreAllenamento.mcsSelezionatiIds = setOf()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 20.dp)) {
                IconButton(
                    onClick = { onTornaIndietro() },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                ) { Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold) }
                Text("ALLENAMENTO", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = FontFamily(Font(R.font.jackboa)), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            TabRow(
                selectedTabIndex = GestoreAllenamento.tabSelezionata, containerColor = Color.Transparent, contentColor = Tema.colorePrincipale,
                indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[GestoreAllenamento.tabSelezionata]), color = Tema.colorePrincipale) },
                divider = {}
            ) {
                Tab(selected = GestoreAllenamento.tabSelezionata == 0, onClick = { GestoreAllenamento.tabSelezionata = 0 }, text = { Text("MATCHMAKING", color = if (GestoreAllenamento.tabSelezionata == 0) Tema.coloreTesto else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold, fontSize = 16.sp) })
                Tab(selected = GestoreAllenamento.tabSelezionata == 1, onClick = { GestoreAllenamento.tabSelezionata = 1 }, text = { Text("GENERATORI", color = if (GestoreAllenamento.tabSelezionata == 1) Tema.coloreTesto else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold, fontSize = 16.sp) })
            }

            Box(modifier = Modifier.weight(1f)) {
                if (GestoreAllenamento.tabSelezionata == 0) {
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
    var mostraDialogAggiunta by remember { mutableStateOf(false) }
    var nomeNuovoMc by remember { mutableStateOf("") }

    val listaFiltrata = DatabaseMcs.listaMcsCloud.filter { it.nome.contains(GestoreAllenamento.testoRicerca, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!GestoreAllenamento.mostraRisultatiMatchmaking) {
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                Text("SELEZIONA GLI MC", color = Tema.coloreTesto, fontSize = 24.sp, fontFamily = FontFamily(Font(R.font.jackboa)), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center)

                OutlinedTextField(
                    value = GestoreAllenamento.testoRicerca, onValueChange = { GestoreAllenamento.testoRicerca = it }, placeholder = { Text("Cerca un MC...", color = Tema.coloreTestoSecondario) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Tema.colorePrincipale, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto), singleLine = true, shape = RoundedCornerShape(12.dp)
                )

                if (DatabaseMcs.staCaricando.value) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Tema.colorePrincipale)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(listaFiltrata) { mc ->
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
                onClick = { generaBattleMatchmaking() }, enabled = GestoreAllenamento.mcsSelezionatiIds.size >= 2, colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale, disabledContainerColor = Color.DarkGray),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).height(60.dp), shape = RoundedCornerShape(12.dp)
            ) { Text("GENERA BATTLE", color = Color.White, fontSize = 22.sp, fontFamily = font) }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("ACCOPPIAMENTI", color = Tema.coloreTesto, fontSize = 28.sp, fontFamily = font, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center)

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(GestoreAllenamento.battleGenerate) { pair -> BattleCardMatchmaking(mc1 = pair.first, mc2 = pair.second) }
                    if (GestoreAllenamento.mcSingolo != null) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp).background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("RIPOSA: ", color = Tema.coloreTestoSecondario, fontSize = 18.sp, fontFamily = font)
                                BoxMC(mc = GestoreAllenamento.mcSingolo!!, width = 60.dp, height = 80.dp)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { GestoreAllenamento.mostraRisultatiMatchmaking = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.weight(1f).height(60.dp), shape = RoundedCornerShape(12.dp)) { Text("INDIETRO", color = Color.White, fontSize = 18.sp, fontFamily = font) }
                    Button(onClick = { generaBattleMatchmaking() }, colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), modifier = Modifier.weight(1.5f).height(60.dp), shape = RoundedCornerShape(12.dp)) { Text("REROLL", color = Color.White, fontSize = 20.sp, fontFamily = font) }
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
                            val esistenteInLista = DatabaseMcs.listaMcsCloud.find { it.nome.equals(nome, ignoreCase = true) }

                            if (esistenteInLista == null) {
                                val mcGlobale = DatabaseMcs.cercaMcGlobale(nome)
                                val nuovoMc = mcGlobale ?: Freestyler(System.currentTimeMillis().toString(), nome, "", if (Tema.isBarreFaul) "barre_faul" else "muretto_pg")
                                DatabaseMcs.listaMcsCloud.add(nuovoMc)
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

private fun generaBattleMatchmaking() {
    val selezionati = DatabaseMcs.listaMcsCloud.filter { GestoreAllenamento.mcsSelezionatiIds.contains(it.id) }.shuffled()
    if (selezionati.size >= 2) {
        val pairs = mutableListOf<Pair<Freestyler, Freestyler>>()
        for (i in 0 until (selezionati.size / 2)) pairs.add(selezionati[i * 2] to selezionati[i * 2 + 1])
        GestoreAllenamento.battleGenerate = pairs
        GestoreAllenamento.mcSingolo = if (selezionati.size % 2 != 0) selezionati.last() else null
        GestoreAllenamento.mostraRisultatiMatchmaking = true
    }
}

@Composable
fun BattleCardMatchmaking(mc1: Freestyler, mc2: Freestyler) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(3.dp, Tema.colorePrincipale, RoundedCornerShape(24.dp)).background(brush = Tema.gradienteCard).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            BoxMC(mc = mc1, width = 90.dp, height = 120.dp)
            Image(painter = painterResource(id = R.drawable.versus), contentDescription = "Versus", modifier = Modifier.size(70.dp).padding(horizontal = 8.dp))
            BoxMC(mc = mc2, width = 90.dp, height = 120.dp)
        }
    }
}

@Composable
fun CardFreestyler(freestyler: Freestyler, isSelezionato: Boolean, onClick: () -> Unit) {
    val colorMatrix = remember(isSelezionato) { 
        if (isSelezionato) ColorMatrix().apply { setToSaturation(0f) } else null 
    }
    val imageModel: Any = if (freestyler.immagineUrl.isBlank()) R.drawable.no_pic else freestyler.immagineUrl

    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(12.dp)).border(3.dp, if(isSelezionato) Color.Green else Tema.colorePrincipale, RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageModel)
                .crossfade(true)
                .build(),
            contentDescription = "Foto di ${freestyler.nome}",
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.no_pic),
            error = painterResource(R.drawable.no_pic),
            colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null
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
    val opzioniAllenamento = listOf("Generatore di argomenti", "Generatore di modalita", "Generatore di parole")
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize()) {
        items(opzioniAllenamento) { nome -> CardAllenamento(nomeOpzione = nome, onClick = { onSelezionaAllenamento(nome) }, font = font) }
    }
}

@Composable
fun CardAllenamento(nomeOpzione: String, onClick: () -> Unit, font: FontFamily) {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).border(3.dp, Tema.colorePrincipale, RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text = nomeOpzione.uppercase(), color = Tema.coloreTesto, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = font) }
}