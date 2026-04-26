package com.example.muretto_pg_app

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataMurettoClassico(onTornaAlMenu: () -> Unit, onIniziaBattle: () -> Unit) {
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))

    var listaMcs by remember {
        mutableStateOf(
            listOf(
                Freestyler("1", "Bahmes", R.drawable.bahmes),
                Freestyler("2", "Big", R.drawable.big),
                Freestyler("3", "Bisca", R.drawable.bisca),
                Freestyler("4", "Brage", R.drawable.brage),
                Freestyler("5", "Chapel", R.drawable.chapel),
                Freestyler("6", "Deku", R.drawable.deku),
                Freestyler("7", "Esty", R.drawable.esty),
                Freestyler("8", "Fist", R.drawable.fist),
                Freestyler("9", "Fto", R.drawable.fto),
                Freestyler("10", "Ganesh", R.drawable.ganesh),
                Freestyler("11", "Gross", R.drawable.gross),
                Freestyler("12", "Henker", R.drawable.henker),
                Freestyler("13", "Koko", R.drawable.koko),
                Freestyler("14", "Lil Dik", R.drawable.lil_dik),
                Freestyler("15", "Lordao", R.drawable.lordao),
                Freestyler("16", "Lyl Dark", R.drawable.lyl_dark),
                Freestyler("17", "Madra", R.drawable.madra),
                Freestyler("18", "Mogio", R.drawable.mogio),
                Freestyler("19", "Monkey", R.drawable.monkey),
                Freestyler("20", "Mt", R.drawable.mt),
                Freestyler("21", "Olegan", R.drawable.olegan),
                Freestyler("22", "Rein", R.drawable.rein),
                Freestyler("23", "Samyr", R.drawable.samyr),
                Freestyler("24", "Schiaccia", R.drawable.schiaccia),
                Freestyler("25", "Shock", R.drawable.shock),
                Freestyler("26", "Sockold", R.drawable.sockold),
                Freestyler("27", "Stiwi", R.drawable.stiwi),
                Freestyler("28", "Tchain", R.drawable.tchain),
                Freestyler("29", "Yama", R.drawable.yama)
            )
        )
    }

    var testoRicerca by remember { mutableStateOf("") }
    var mcsSelezionati by remember { mutableStateOf(setOf<String>()) }

    var mostraDialogAggiunta by remember { mutableStateOf(false) }
    var nomeNuovoMc by remember { mutableStateOf("") }

    val listaFiltrata = listaMcs.filter { it.nome.contains(testoRicerca, ignoreCase = true) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 10.dp)) {
                    IconButton(
                        onClick = { onTornaAlMenu() },
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                    ) {
                        Text("<", color = Color.White, fontSize = 45.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "SELEZIONA GLI MC",
                        color = Color.White, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center).offset(x = 15.dp)
                    )
                }

                OutlinedTextField(
                    value = testoRicerca,
                    onValueChange = { testoRicerca = it },
                    placeholder = { Text("Cerca un MC...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD32F2F), unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White),
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
                        val isSelezionato = mcsSelezionati.contains(mc.id)
                        CardFreestylerTorneo(
                            freestyler = mc,
                            isSelezionato = isSelezionato,
                            onClick = {
                                mcsSelezionati = if (isSelezionato) mcsSelezionati - mc.id else mcsSelezionati + mc.id
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
                onClick = {
                    GestoreBattle.resetSelezione()
                    val selezionatiVeri = listaMcs.filter { mcsSelezionati.contains(it.id) }
                    GestoreBattle.mcsSelezionati.addAll(selezionatiVeri)

                    if (GestoreBattle.mcsSelezionati.size >= 2) {
                        onIniziaBattle()
                    }
                },
                enabled = mcsSelezionati.size >= 2,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    disabledContainerColor = Color.DarkGray
                ),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).height(60.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("INIZIA BATTLE", color = Color.White, fontSize = 22.sp, fontFamily = MioFontPersonalizzato)
            }
        }
    }

    if (mostraDialogAggiunta) {
        AlertDialog(
            onDismissRequest = { mostraDialogAggiunta = false },
            containerColor = Color(0xFF222222),
            title = { Text("Nuovo MC", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nomeNuovoMc,
                    onValueChange = { nomeNuovoMc = it },
                    placeholder = { Text("Nome del Freestyler", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    onClick = {
                        if (nomeNuovoMc.isNotBlank()) {
                            val nuovoMc = Freestyler(
                                id = System.currentTimeMillis().toString(),
                                nome = nomeNuovoMc.trim(),
                                immagineId = R.drawable.no_pic
                            )
                            listaMcs = listaMcs + nuovoMc
                            nomeNuovoMc = ""
                            mostraDialogAggiunta = false
                        }
                    }
                ) { Text("Aggiungi", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { mostraDialogAggiunta = false; nomeNuovoMc = "" }) {
                    Text("Annulla", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun CardFreestylerTorneo(freestyler: Freestyler, isSelezionato: Boolean, onClick: () -> Unit) {
    val colorMatrix = remember(isSelezionato) {
        if (isSelezionato) ColorMatrix().apply { setToSaturation(0f) } else null
    }

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
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selezionato", tint = Color.Green, modifier = Modifier.size(60.dp))
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = freestyler.nome.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}