package com.example.muretto_pg_app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SchermataContest(onTornaIndietro: () -> Unit, onNavigate: (String) -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFont = FontFamily(Font(R.font.komtit__))

    // Controllo dei permessi: Admin, Org. Muretto o Org. Eventi
    val puoCreaContest = databaseViewModel.isAdmin ||
            databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_MURETTO ||
            databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_EVENTI

    // Recupero i contest. Per ora stiamo usando la lista 'eventiApprovati'
    // in futuro potrai filtrare solo quelli di tipo "Contest"
    val listaContest = databaseViewModel.eventiApprovati

    LaunchedEffect(Unit) {
        databaseViewModel.fetchEventiApprovati()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- SFONDO DINAMICO ---
            Image(
                painter = painterResource(id = Tema.sfondoGenerale),
                contentDescription = "Sfondo Menu",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Patina scura per leggibilità
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                // HEADER
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 20.dp)) {
                    IconButton(onClick = { onTornaIndietro() }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFont)
                    }
                    Text("CONTEST", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SE L'UTENTE HA I PERMESSI, MOSTRA LA CARD "CREA CONTEST"
                if (puoCreaContest) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp) // Altezza ridotta per far spazio alla lista
                            .clip(RoundedCornerShape(24.dp))
                            .background(Tema.coloreSfondoCard)
                            .border(3.dp, Tema.colorePrincipale, RoundedCornerShape(24.dp))
                            .clickable { onNavigate("aggiungi_contest") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ CREA CONTEST", color = Tema.coloreTesto, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = MioFont, textAlign = TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // LISTA DEI CONTEST
                if (listaContest.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Ancora nessun contest attivo...", color = Tema.coloreTestoSecondario, fontSize = 20.sp, fontFamily = MioFont, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(listaContest) { contest ->
                            // Riutilizzo il tuo design perfetto della CardPostEvento!
                            CardPostEvento(
                                evento = contest,
                                isPreferito = databaseViewModel.eventiPreferiti.contains(contest.id),
                                onTogglePreferito = { databaseViewModel.togglePreferito(contest.id) },
                                isLoggato = databaseViewModel.ruoloAttuale != RuoloUtente.NESSUNO,
                                onGestisciBattle = { eventoId -> onNavigate("gestione_battle_evento/$eventoId") }
                            )
                        }
                    }
                }
            }
        }
    }
}