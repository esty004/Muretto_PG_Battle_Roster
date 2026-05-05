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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun SchermataTrasferte(onTornaIndietro: () -> Unit, onVaiAllaMappa: () -> Unit) {
    val MioFont = FontFamily(Font(R.font.komtit__))
    val contest = LocalContext.current
    val scope = rememberCoroutineScope()

    val eventi = DatabaseMcs.eventiApprovati

    LaunchedEffect(Unit) {
        DatabaseMcs.fetchEventiApprovati()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {

            // SFONDO SFUMATO (Nero-Rosso-Bianco)
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

                // HEADER
                Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                    IconButton(
                        onClick = onTornaIndietro,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold)
                    }
                    Text("TRASFERTE", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                if (eventi.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nessuna trasferta programmata al momento.", color = Tema.coloreTestoSecondario, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
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

            // PULSANTE MAPPA CON IMMAGINE DIRETTA (italy_icon)
            FloatingActionButton(
                onClick = onVaiAllaMappa,
                containerColor = Tema.colorePrincipale, // Colore di sfondo del bottone
                contentColor = Color.Unspecified, // Evita tinte sovrapposte
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 32.dp)
                    .size(65.dp) // Leggermente più grande per far risaltare l'immagine
            ) {
                // Rimosso il padding interno e usato Crop/Fit in modo che riempia il bottone
                Image(
                    painter = painterResource(id = R.drawable.italy_icon),
                    contentDescription = "Mappa Trasferte",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // Crop riempirà tutto il cerchio
                )
            }
        }
    }
}

@Composable
fun CardPostEvento(evento: Evento) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, Color.White, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Tema.coloreSfondoCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            if (evento.immagine_url != null) {
                AsyncImage(
                    model = evento.immagine_url,
                    contentDescription = "Locandina ${evento.titolo}",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.no_pic),
                    error = painterResource(R.drawable.no_pic)
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Tema.gradienteCard), contentAlignment = Alignment.Center) {
                    Text(evento.titolo.take(1).uppercase(), color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.jackboa)))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = evento.titolo.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.komtit__)))

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
                // Aggiunto "PREZZO: " in grigio e il valore in rosso/blu (colore principale)
                Row {
                    Text(text = "PREZZO: ", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = evento.prezzo.uppercase(), color = Tema.colorePrincipale, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}