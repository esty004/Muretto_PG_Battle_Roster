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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SchermataAllenamento(onTornaIndietro: () -> Unit, onSelezionaAllenamento: (String) -> Unit) {
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))
    
    LaunchedEffect(Unit) {
        GestoreAllenamento.inizializzaSeVuoto()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 10.dp)) {
                IconButton(
                    onClick = { onTornaIndietro() },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                ) {
                    Text("<", color = Color.White, fontSize = 45.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "ALLENAMENTO",
                    color = Color.White, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            TabRow(
                selectedTabIndex = GestoreAllenamento.tabSelezionata,
                containerColor = Color.Transparent,
                contentColor = Color(0xFFD32F2F),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[GestoreAllenamento.tabSelezionata]),
                        color = Color(0xFFD32F2F)
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = GestoreAllenamento.tabSelezionata == 0,
                    onClick = { GestoreAllenamento.tabSelezionata = 0 },
                    text = { Text("MATCHMAKING", color = if (GestoreAllenamento.tabSelezionata == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                )
                Tab(
                    selected = GestoreAllenamento.tabSelezionata == 1,
                    onClick = { GestoreAllenamento.tabSelezionata = 1 },
                    text = { Text("GENERATORI", color = if (GestoreAllenamento.tabSelezionata == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                )
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
    
    val listaFiltrata = GestoreAllenamento.listaMcsAllenamento.filter { 
        it.nome.contains(GestoreAllenamento.testoRicerca, ignoreCase = true) 
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!GestoreAllenamento.mostraRisultatiMatchmaking) {
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                Text(
                    text = "SELEZIONA GLI MC",
                    color = Color.White, fontSize = 24.sp, fontFamily = font, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = GestoreAllenamento.testoRicerca,
                    onValueChange = { GestoreAllenamento.testoRicerca = it },
                    placeholder = { Text("Cerca un MC...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD32F2F),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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

            FloatingActionButton(
                onClick = { mostraDialogAggiunta = true },
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 100.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi MC", modifier = Modifier.size(30.dp))
            }

            Button(
                onClick = { generaBattleMatchmaking() },
                enabled = GestoreAllenamento.mcsSelezionatiIds.size >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), disabledContainerColor = Color.DarkGray),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).height(60.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("GENERA BATTLE", color = Color.White, fontSize = 22.sp, fontFamily = font)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "ACCOPPIAMENTI",
                    color = Color.White, fontSize = 28.sp, fontFamily = font,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(GestoreAllenamento.battleGenerate) { pair ->
                        BattleCardMatchmaking(mc1 = pair.first, mc2 = pair.second)
                    }
                    if (GestoreAllenamento.mcSingolo != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp).background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("RIPOSA: ", color = Color.LightGray, fontSize = 18.sp, fontFamily = font)
                                BoxMC(mc = GestoreAllenamento.mcSingolo!!, width = 60.dp, height = 80.dp)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { GestoreAllenamento.mostraRisultatiMatchmaking = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("INDIETRO", color = Color.White, fontSize = 18.sp, fontFamily = font)
                    }
                    Button(
                        onClick = { generaBattleMatchmaking() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(1.5f).height(60.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("REROLL", color = Color.White, fontSize = 20.sp, fontFamily = font)
                    }
                }
            }
        }
    }

    if (mostraDialogAggiunta) {
        AlertDialog(
            onDismissRequest = { mostraDialogAggiunta = false },
            containerColor = Color(0xFF222222),
            title = { Text("NUOVO MC", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = font) },
            text = {
                OutlinedTextField(
                    value = nomeNuovoMc,
                    onValueChange = { nomeNuovoMc = it },
                    placeholder = { Text("Nome del Freestyler", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFD32F2F)),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        if (nomeNuovoMc.isNotBlank()) {
                            GestoreAllenamento.listaMcsAllenamento.add(Freestyler(System.currentTimeMillis().toString(), nomeNuovoMc.trim(), R.drawable.no_pic))
                            nomeNuovoMc = ""
                            mostraDialogAggiunta = false
                        }
                    }
                ) { Text("AGGIUNGI", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { mostraDialogAggiunta = false; nomeNuovoMc = "" }) { Text("ANNULLA", color = Color.Gray) }
            }
        )
    }
}

private fun generaBattleMatchmaking() {
    val selezionati = GestoreAllenamento.listaMcsAllenamento.filter { 
        GestoreAllenamento.mcsSelezionatiIds.contains(it.id) 
    }.shuffled()
    
    if (selezionati.size >= 2) {
        val pairs = mutableListOf<Pair<Freestyler, Freestyler>>()
        for (i in 0 until (selezionati.size / 2)) {
            pairs.add(selezionati[i * 2] to selezionati[i * 2 + 1])
        }
        GestoreAllenamento.battleGenerate = pairs
        GestoreAllenamento.mcSingolo = if (selezionati.size % 2 != 0) selezionati.last() else null
        GestoreAllenamento.mostraRisultatiMatchmaking = true
    }
}

@Composable
fun BattleCardMatchmaking(mc1: Freestyler, mc2: Freestyler) {
    val backgroundBrush = Brush.horizontalGradient(colors = listOf(Color(0xFF3A0000), Color(0xFF00003A)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(3.dp, Color(0xFFD32F2F), RoundedCornerShape(24.dp))
            .background(brush = backgroundBrush)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BoxMC(mc = mc1, width = 90.dp, height = 120.dp)
            Image(
                painter = painterResource(id = R.drawable.versus),
                contentDescription = "Versus",
                modifier = Modifier.size(70.dp).padding(horizontal = 8.dp)
            )
            BoxMC(mc = mc2, width = 90.dp, height = 120.dp)
        }
    }
}

@Composable
fun BoxMC(mc: Freestyler, isVincitore: Boolean = false, isSconfitto: Boolean = false, width: Dp = 100.dp, height: Dp = 130.dp) {
    val colorMatrix = remember(isSconfitto) { if (isSconfitto) ColorMatrix().apply { setToSaturation(0f) } else null }
    // Rimpicciolisce il testo se è una coppia 2vs2 (es. "Mogio & Bisca")
    val fontSize = if (mc.nome.contains("&")) 11.sp else 13.sp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(width).height(height)
                .clip(RoundedCornerShape(16.dp))
                .border(width = if (isVincitore) 4.dp else 1.dp, color = if (isVincitore) Color.Green else Color.Gray, shape = RoundedCornerShape(16.dp))
                .background(Color.DarkGray)
        ) {
            Image(painter = painterResource(id = mc.immagineId), contentDescription = null, modifier = Modifier.fillMaxSize(), alignment = Alignment.TopCenter, contentScale = ContentScale.Crop, colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null)
            if (isVincitore) Box(modifier = Modifier.matchParentSize().background(Color.Green.copy(alpha = 0.3f)))
            if (isSconfitto) Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.fillMaxSize().padding(16.dp))
        }
        Text(text = mc.nome.uppercase(), color = if (isVincitore) Color.Green else if(isSconfitto) Color.Gray else Color.White, fontSize = fontSize, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
    }
}

@Composable
fun CardFreestyler(freestyler: Freestyler, isSelezionato: Boolean, onClick: () -> Unit) {
    val colorMatrix = remember(isSelezionato) { if (isSelezionato) ColorMatrix().apply { setToSaturation(0f) } else null }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, if(isSelezionato) Color.Green else Color(0xFFD32F2F), RoundedCornerShape(12.dp))
            .background(Color(0xFF111111))
            .clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = freestyler.immagineId),
            contentDescription = "Foto di ${freestyler.nome}",
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Crop,
            colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null
        )

        if (isSelezionato) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selezionato", tint = Color.Green, modifier = Modifier.size(60.dp))
            }
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
        items(opzioniAllenamento) { nome ->
            CardAllenamento(nomeOpzione = nome, onClick = { onSelezionaAllenamento(nome) }, font = font)
        }
    }
}

@Composable
fun CardAllenamento(nomeOpzione: String, onClick: () -> Unit, font: FontFamily) {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).border(3.dp, Color(0xFFD32F2F), RoundedCornerShape(12.dp)).background(Color(0xFF111111))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = nomeOpzione.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = font)
    }
}
