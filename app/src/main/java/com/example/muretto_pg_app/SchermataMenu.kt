package com.example.muretto_pg_app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
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
fun SchermataMenu(onTornaIndietro: () -> Unit, onSelezionaModalita: (String) -> Unit) {
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))
    // RIMOSSO "Allenamento" dalla lista
    val listaModalita = listOf("Muretto classico", "2 VS 2", "Contest")

    var mostraDialog2vs2 by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- SFONDO DINAMICO ---
            SfondoSchermata(Modifier.fillMaxSize(), "Sfondo Menu")
            // Patina scura per leggibilità
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 20.dp)) {
                    Text(
                        "SELEZIONA MODALITA'",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontFamily = FontFamily(Font(R.font.jackboa)),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(listaModalita) { nome ->
                        CardModalita(nomeModalita = nome, onClick = {
                            when (nome) {
                                "Muretto classico" -> onSelezionaModalita("muretto_classico")
                                "2 VS 2" -> mostraDialog2vs2 = true
                                "Contest" -> onSelezionaModalita("contest")
                            }
                        })
                    }
                }
            }

            FloatingActionButton(
                onClick = onTornaIndietro,
                containerColor = Tema.coloreBottoni,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 32.dp)
            ) {
                Text("<", fontSize = 30.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp))
            }
        }
    }

    if (mostraDialog2vs2) {
        AlertDialog(
            onDismissRequest = { mostraDialog2vs2 = false },
            containerColor = Tema.coloreSfondoCard,
            title = { Text("MODALITÀ 2 VS 2", color = Tema.coloreTesto, fontFamily = MioFontPersonalizzato, fontSize = 24.sp) },
            text = { Text("Come vuoi formare le coppie?", color = Tema.coloreTestoSecondario, fontSize = 16.sp) },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), onClick = {
                    mostraDialog2vs2 = false
                    onSelezionaModalita("due_contro_due/${TipoTorneo.COPPIE_CASUALI.name}")
                }) { Text("CASUALI", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), onClick = {
                    mostraDialog2vs2 = false
                    onSelezionaModalita("due_contro_due/${TipoTorneo.COPPIE_PREDEFINITE.name}")
                }) { Text("PREDEFINITE", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
fun CardModalita(nomeModalita: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(4.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp))
            .background(Tema.coloreSfondoCard)
            .clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1) URL dal DB in base alla modalità
        val urlCard = when (nomeModalita) {
            "Muretto classico" -> Tema.sfondoCardMurettoUrl
            "2 VS 2" -> Tema.sfondoCard2v2Url
            "Contest" -> Tema.sfondoCardContestUrl
            else -> null
        }

        // Mostra l'immagine dal DB se c'è, altrimenti riempimento a colore pieno
        if (!urlCard.isNullOrBlank()) {
            AsyncImage(model = urlCard, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Tema.colorePrincipale.copy(alpha = 0.35f)))
        }

        Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Text(text = nomeModalita.uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}