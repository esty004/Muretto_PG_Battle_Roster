package com.example.muretto_pg_app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun SchermataEventi(onTornaIndietro: () -> Unit, onNavigate: (String) -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFont = FontFamily(Font(R.font.komtit__))

    // Controllo dei permessi: Admin, Org. Muretto o Org. Eventi
    val puoCreaEvento = databaseViewModel.isAdmin ||
            databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_MURETTO ||
            databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_EVENTI

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- SFONDO DINAMICO ---
            SfondoSchermata(Modifier.fillMaxSize(), "Sfondo Menu")
            // Patina scura per leggibilità
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

                // HEADER
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 20.dp)) {
                    IconButton(onClick = { onTornaIndietro() }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                        Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFont)
                    }
                    Text("EVENTI", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SE L'UTENTE HA I PERMESSI, MOSTRA LA CARD "CREA EVENTO"
                if (puoCreaEvento) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(135.dp) // Stessa altezza delle card in Home
                            .clip(RoundedCornerShape(24.dp))
                            .background(Tema.coloreSfondoCard)
                            .border(3.dp, Tema.colorePrincipale, RoundedCornerShape(24.dp))
                            .clickable { onNavigate("aggiungi_evento") }, // Porta alla schermata di aggiunta
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CREA EVENTO",
                            color = Tema.coloreTesto,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MioFont,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }

                // MESSAGGIO "ANCORA NESSUN EVENTO"
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    Text(
                        text = "Ancora nessun evento...",
                        color = Tema.coloreTestoSecondario,
                        fontSize = 24.sp,
                        fontFamily = MioFont,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 80.dp)
                    )
                }
            }
        }
    }
}
