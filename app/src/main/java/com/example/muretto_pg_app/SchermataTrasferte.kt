package com.example.muretto_pg_app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun SchermataTrasferte(onTornaIndietro: () -> Unit, onVaiAllaMappa: () -> Unit, soloPreferiti: Boolean = false) {
    val MioFont = FontFamily(Font(R.font.komtit__))

    // Filtro la lista in base a cosa stiamo visualizzando
    val eventi = if (soloPreferiti) {
        DatabaseMcs.eventiApprovati.filter { DatabaseMcs.eventiPreferiti.contains(it.id) }
    } else {
        DatabaseMcs.eventiApprovati
    }

    LaunchedEffect(Unit) {
        DatabaseMcs.fetchEventiApprovati()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFFF80000),
                        Color(0xFFF2F2F2),
                        Color(0xFFF80000).copy(alpha = 0f)
                    )
                )
            ))

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                    val titolo = if (soloPreferiti) "PREFERITI" else "TRASFERTE"
                    Text(titolo, color = Color.White, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                if (eventi.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (soloPreferiti) "Nessuna trasferta salvata." else "Nessuna trasferta programmata.", color = Color.LightGray, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(eventi) { evento ->
                            CardPostEvento(evento = evento)
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onTornaIndietro,
                containerColor = Tema.colorePrincipale,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 32.dp)
            ) {
                Text("<", fontSize = 30.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp))
            }

            // Mostriamo la mappa solo se non siamo nella pagina dei preferiti puri
            if (!soloPreferiti) {
                FloatingActionButton(
                    onClick = onVaiAllaMappa,
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 32.dp)
                        .size(70.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.italy_icon),
                        contentDescription = "Mappa Trasferte",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun CardPostEvento(evento: Evento) {
    val isLoggato = DatabaseMcs.ruoloAttuale != RuoloUtente.NESSUNO
    val isPreferito = DatabaseMcs.eventiPreferiti.contains(evento.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, Color.White, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Tema.coloreSfondoCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                if (evento.immagine_url != null) {
                    AsyncImage(
                        model = evento.immagine_url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Tema.gradienteCard), contentAlignment = Alignment.Center) {
                        Text(evento.titolo.take(1).uppercase(), color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // STELLINA PREFERITI (Visibile solo se loggato)
                if (isLoggato) {
                    IconButton(
                        onClick = { DatabaseMcs.togglePreferito(evento.id) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPreferito) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Preferito",
                            tint = if (isPreferito) Color.Yellow else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = evento.titolo.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = Tema.colorePrincipale, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = evento.location_nome.uppercase(), color = Color.LightGray, fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Tema.colorePrincipale, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = evento.data_ora.uppercase(), color = Color.LightGray, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(text = "PREZZO: ", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = evento.prezzo.uppercase(), color = Tema.colorePrincipale, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}