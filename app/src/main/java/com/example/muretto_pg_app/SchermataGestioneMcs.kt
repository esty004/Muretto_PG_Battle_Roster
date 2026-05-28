package com.example.muretto_pg_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SchermataGestioneMcs(onTornaIndietro: () -> Unit, onModificaMc: (String) -> Unit, onAggiungiMc: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFont = FontFamily(Font(R.font.komtit__))

    // Se è admin mostriamo le tab, se è organizzatore mostriamo solo il suo muretto
    val isAdmin = databaseViewModel.isAdmin
    val murettoOrganizzatore = databaseViewModel.profiloAttuale?.muretto_id

    var tabSelezionata by remember { mutableIntStateOf(if (!isAdmin && murettoOrganizzatore == "barre_faul") 1 else 0) }

    // SchermataGestioneMcs.kt
    LaunchedEffect(tabSelezionata) {
        val murettoId = when(tabSelezionata) {
            0 -> "09fbe1d3-0022-41b8-ba4b-edc887c145a2" // PG
            1 -> "2d0f412c-4e9d-4eab-b886-f7a2226d7b9e" // Barre Faul
            2 -> "INSERISCI-QUI-UUID-ATENEO"
            3 -> "22ea8a2f-d45d-40b2-a6ee-841058f12f99" // Fortitudo
            else -> "09fbe1d3-0022-41b8-ba4b-edc887c145a2"
        }
        databaseViewModel.fetchMcsDalCloud(murettoId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 10.dp)) {
                    Text("GESTIONE ROSTER", color = Tema.coloreTesto, fontSize = 28.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                if (isAdmin) {
                    ScrollableTabRow(
                        selectedTabIndex = tabSelezionata, containerColor = Color.Transparent, contentColor = Tema.colorePrincipale, edgePadding = 0.dp,
                        indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[tabSelezionata]), color = Tema.colorePrincipale) },
                        divider = {}
                    ) {
                        val tabs = listOf("MURETTO PG", "BARRE FAUL", "ATENEO", "FORTITUDO")
                        tabs.forEachIndexed { index, titolo ->
                            Tab(selected = tabSelezionata == index, onClick = { tabSelezionata = index }, text = { Text(titolo, color = if (tabSelezionata == index) Tema.coloreTesto else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (databaseViewModel.staCaricando.value) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Tema.colorePrincipale) }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(databaseViewModel.listaMcsCloud) { mc ->
                            CardFreestylerTorneo(
                                freestyler = mc,
                                isSelezionato = false,
                                tipoTorneo = TipoTorneo.SINGOLO, // Valore di default per non dare errore
                                indiceSelezione = -1,           // Valore di default per non dare errore
                                onClick = { onModificaMc(mc.id) }
                            )
                        }
                    }
                }
            }

            // FAB Indietro
            FloatingActionButton(onClick = onTornaIndietro, containerColor = Tema.colorePrincipale, contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 32.dp)) {
                Text("<", fontSize = 30.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp))
            }

            // FAB Aggiungi MC
            FloatingActionButton(onClick = onAggiungiMc, containerColor = Color(0xFF4CAF50), contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi", modifier = Modifier.size(30.dp))
            }
        }
    }
}