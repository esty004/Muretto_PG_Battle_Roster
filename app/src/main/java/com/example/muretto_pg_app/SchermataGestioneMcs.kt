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
    val MioFont = FontFamily(Font(R.font.komtit__))

    // Se è admin mostriamo le tab, se è organizzatore mostriamo solo il suo muretto
    val isAdmin = DatabaseMcs.isAdmin
    val murettoOrganizzatore = DatabaseMcs.profiloAttuale?.muretto

    var tabSelezionata by remember { mutableIntStateOf(if (!isAdmin && murettoOrganizzatore == "barre_faul") 1 else 0) }

    LaunchedEffect(tabSelezionata) {
        val murettoId = if (tabSelezionata == 0) "muretto_pg" else "barre_faul"
        DatabaseMcs.fetchMcsDalCloud(murettoId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 10.dp)) {
                    Text("GESTIONE ROSTER", color = Tema.coloreTesto, fontSize = 28.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                if (isAdmin) {
                    TabRow(
                        selectedTabIndex = tabSelezionata, containerColor = Color.Transparent, contentColor = Tema.colorePrincipale,
                        indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[tabSelezionata]), color = Tema.colorePrincipale) },
                        divider = {}
                    ) {
                        Tab(selected = tabSelezionata == 0, onClick = { tabSelezionata = 0 }, text = { Text("MURETTO PG", color = if (tabSelezionata == 0) Tema.coloreTesto else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold) })
                        Tab(selected = tabSelezionata == 1, onClick = { tabSelezionata = 1 }, text = { Text("BARRE FAUL", color = if (tabSelezionata == 1) Tema.coloreTesto else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (DatabaseMcs.staCaricando.value) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Tema.colorePrincipale) }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(DatabaseMcs.listaMcsCloud) { mc ->
                            CardFreestylerTorneo(freestyler = mc, isSelezionato = false) {
                                onModificaMc(mc.id)
                            }
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