package com.example.muretto_pg_app

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colori dinamici per le coppie
val coloriCoppie = listOf(
    Color(0xFF9C27B0), // Viola
    Color(0xFFF44336), // Rosso
    Color(0xFF2196F3), // Blu
    Color(0xFFFF9800), // Arancione
    Color(0xFF00BCD4), // Ciano
    Color(0xFF8BC34A), // Verde Chiaro
    Color(0xFFE91E63), // Rosa
    Color(0xFFFFEB3B), // Giallo
    Color(0xFF795548), // Marrone
    Color(0xFF607D8B)  // Grigio Bluastro
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataMurettoClassico(
    tipoTorneo: TipoTorneo,
    onTornaAlMenu: () -> Unit,
    onIniziaBattle: () -> Unit
) {
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))
    val focusManager = LocalFocusManager.current

    var listaMcs by remember {
        mutableStateOf(
            listOf(
                Freestyler("1", "Bahmes", R.drawable.bahmes), Freestyler("2", "Big", R.drawable.big), Freestyler("3", "Bisca", R.drawable.bisca),
                Freestyler("4", "Brage", R.drawable.brage), Freestyler("5", "Chapel", R.drawable.chapel), Freestyler("6", "Deku", R.drawable.deku),
                Freestyler("7", "Esty", R.drawable.esty), Freestyler("8", "Fist", R.drawable.fist), Freestyler("9", "Fto", R.drawable.fto),
                Freestyler("10", "Ganesh", R.drawable.ganesh), Freestyler("11", "Gross", R.drawable.gross), Freestyler("12", "Henker", R.drawable.henker),
                Freestyler("13", "Koko", R.drawable.koko), Freestyler("14", "Lil Dik", R.drawable.lil_dik), Freestyler("15", "Lordao", R.drawable.lordao),
                Freestyler("16", "Lyl Dark", R.drawable.lyl_dark), Freestyler("17", "Madra", R.drawable.madra), Freestyler("18", "Mogio", R.drawable.mogio),
                Freestyler("19", "Monkey", R.drawable.monkey), Freestyler("20", "Mt", R.drawable.mt), Freestyler("21", "Olegan", R.drawable.olegan),
                Freestyler("22", "Rein", R.drawable.rein), Freestyler("23", "Samyr", R.drawable.samyr), Freestyler("24", "Schiaccia", R.drawable.schiaccia),
                Freestyler("25", "Shock", R.drawable.shock), Freestyler("26", "Sockold", R.drawable.sockold), Freestyler("27", "Stiwi", R.drawable.stiwi),
                Freestyler("28", "Tchain", R.drawable.tchain), Freestyler("29", "Yama", R.drawable.yama)
            )
        )
    }

    var testoRicerca by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) } // Controlla se stai digitando
    var mcsSelezionati by remember { mutableStateOf(listOf<String>()) }
    var mostraDialogAggiunta by remember { mutableStateOf(false) }
    var nomeNuovoMc by remember { mutableStateOf("") }

    val listaFiltrata = listaMcs.filter { it.nome.contains(testoRicerca, ignoreCase = true) }

    // Intercetta il tasto "Indietro" di sistema del telefono per chiudere solo la tastiera
    BackHandler(enabled = searchFocused) {
        focusManager.clearFocus()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 10.dp)) {
                    IconButton(
                        onClick = {
                            // Se la barra di ricerca è attiva, chiude solo la tastiera. Altrimenti torna al menu.
                            if (searchFocused) {
                                focusManager.clearFocus()
                            } else {
                                onTornaAlMenu()
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                    ) {
                        Text("<", color = Color.White, fontSize = 45.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold)
                    }
                    Text("SELEZIONA GLI MC", color = Color.White, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).offset(x = 15.dp))
                }

                if (tipoTorneo == TipoTorneo.COPPIE_PREDEFINITE) {
                    Text("Seleziona gli MC nell'ordine esatto per formare le coppie.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                } else if (tipoTorneo == TipoTorneo.COPPIE_CASUALI) {
                    Text("Gli MC verranno accoppiati in modo totalmente casuale.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }

                if (tipoTorneo != TipoTorneo.SINGOLO && mcsSelezionati.size % 2 != 0) {
                    Text("⚠️ NUMERO DISPARI: L'ultimo MC selezionato verrà escluso!", color = Color(0xFFFFB300), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }

                OutlinedTextField(
                    value = testoRicerca,
                    onValueChange = { testoRicerca = it },
                    placeholder = { Text("Cerca un MC...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .onFocusChanged { searchFocused = it.isFocused }, // Registra quando ci clicchi sopra
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

                        // I colori si attivano a coppie SOLO se siamo nella modalità Predefinita
                        val indiceColore = if (isSelezionato && tipoTorneo == TipoTorneo.COPPIE_PREDEFINITE) {
                            (mcsSelezionati.indexOf(mc.id) / 2) % coloriCoppie.size
                        } else 0

                        val coloreSelezione = if (tipoTorneo == TipoTorneo.COPPIE_PREDEFINITE) coloriCoppie[indiceColore] else Color.Green

                        CardFreestylerTorneo(
                            freestyler = mc,
                            isSelezionato = isSelezionato,
                            coloreSelezione = coloreSelezione,
                            onClick = {
                                mcsSelezionati = if (isSelezionato) {
                                    mcsSelezionati - mc.id
                                } else {
                                    mcsSelezionati + mc.id
                                }
                            }
                        )
                    }
                }
            }

            FloatingActionButton(onClick = { mostraDialogAggiunta = true }, containerColor = Color(0xFF4CAF50), contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 100.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi MC", modifier = Modifier.size(30.dp))
            }

            Button(
                onClick = {
                    GestoreBattle.resetSelezione()
                    val selezionatiVeri = mcsSelezionati.mapNotNull { id -> listaMcs.find { it.id == id } }
                    GestoreBattle.mcsSelezionati.addAll(selezionatiVeri)

                    val minimoRichiesto = if (tipoTorneo == TipoTorneo.SINGOLO) 2 else 4
                    if (GestoreBattle.mcsSelezionati.size >= minimoRichiesto) {
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
                    value = nomeNuovoMc, onValueChange = { nomeNuovoMc = it }, placeholder = { Text("Nome del Freestyler", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true
                )
            },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), onClick = {
                    if (nomeNuovoMc.isNotBlank()) {
                        val nuovoMc = Freestyler(System.currentTimeMillis().toString(), nomeNuovoMc.trim(), R.drawable.no_pic)
                        listaMcs = listaMcs + nuovoMc
                        nomeNuovoMc = ""
                        mostraDialogAggiunta = false
                    }
                }) { Text("Aggiungi", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { mostraDialogAggiunta = false; nomeNuovoMc = "" }) { Text("Annulla", color = Color.Gray) } }
        )
    }
}

@Composable
fun CardFreestylerTorneo(freestyler: Freestyler, isSelezionato: Boolean, coloreSelezione: Color, onClick: () -> Unit) {
    val colorMatrix = remember(isSelezionato) { if (isSelezionato) ColorMatrix().apply { setToSaturation(0f) } else null }

    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(12.dp)).border(3.dp, if(isSelezionato) coloreSelezione else Color(0xFFD32F2F), RoundedCornerShape(12.dp)).background(Color(0xFF111111)).clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(painter = painterResource(id = freestyler.immagineId), contentDescription = null, modifier = Modifier.fillMaxSize(), alignment = Alignment.TopCenter, contentScale = ContentScale.Crop, colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null)

        if (isSelezionato) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selezionato", tint = coloreSelezione, modifier = Modifier.size(60.dp))
            }
        }

        Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text = freestyler.nome.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}