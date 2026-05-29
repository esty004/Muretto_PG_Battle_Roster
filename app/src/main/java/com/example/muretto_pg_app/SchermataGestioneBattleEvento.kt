package com.example.muretto_pg_app

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun SchermataGestioneBattleEvento(eventoId: String, onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val MioFont = FontFamily(Font(R.font.komtit__))

    // Recupera l'evento aggiornato
    val evento = databaseViewModel.eventiApprovati.find { it.id == eventoId }
    val design = evento?.contest_design

    var tabSelezionata by remember { mutableIntStateOf(1) } // Parte da Verdetti

    // --- VARIABILI STATO VERDETTI ---
    var sistemaVoti by remember { mutableStateOf(design?.sistema_voti ?: "Verdetto Singolo") }
    var votoPubblico by remember { mutableStateOf(design?.voto_pubblico ?: false) }
    var numeroGiudici by remember { mutableFloatStateOf((design?.numero_giudici ?: 0).toFloat()) }
    var staSalvando by remember { mutableStateOf(false) }

    // Funzione per generare codici casuali
    fun generaCodice(lunghezza: Int = 6): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..lunghezza).map { chars.random() }.joinToString("")
    }

    if (evento == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Tema.colorePrincipale) }
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(modifier = Modifier.fillMaxSize()) {

            // HEADER
            Box(modifier = Modifier.fillMaxWidth().background(Tema.coloreSfondoCard).padding(top = 50.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)) {
                IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("<", color = Tema.coloreTesto, fontSize = 40.sp, fontFamily = MioFont)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                    Text("GESTIONE CONTEST", color = Tema.colorePrincipale, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(evento.titolo.uppercase(), color = Tema.coloreTesto, fontSize = 24.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold)
                }
            }

            // TABS (ESTETICA, VERDETTI, BATTLE)
            TabRow(
                selectedTabIndex = tabSelezionata,
                containerColor = Tema.coloreSfondoCard,
                contentColor = Tema.colorePrincipale,
                indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[tabSelezionata]), color = Tema.colorePrincipale) }
            ) {
                Tab(selected = tabSelezionata == 0, onClick = { tabSelezionata = 0 }, text = { Text("ESTETICA", color = if (tabSelezionata == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold) })
                Tab(selected = tabSelezionata == 1, onClick = { tabSelezionata = 1 }, text = { Text("VERDETTI", color = if (tabSelezionata == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold) })
                Tab(selected = tabSelezionata == 2, onClick = { tabSelezionata = 2 }, text = { Text("BATTLE", color = if (tabSelezionata == 2) Color.White else Color.Gray, fontWeight = FontWeight.Bold) })
            }

            // CONTENUTO TABS
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

                when (tabSelezionata) {
                    0 -> {
                        // ESTETICA (Placeholder per modifica futura)
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Tema.coloreSfondoCard).border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Text("Modifica Estetica in arrivo...", color = Tema.coloreTestoSecondario, fontSize = 18.sp, fontFamily = MioFont)
                        }
                    }

                    1 -> {
                        // VERDETTI E VOTAZIONI
                        Text("SISTEMA DI VOTAZIONE", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // SCELTA SISTEMA VOTI
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Verdetto Singolo", "Punteggio").forEach { opzione ->
                                val isSelected = sistemaVoti == opzione
                                Box(
                                    modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) Tema.colorePrincipale else Tema.coloreSfondoCard).clickable { sistemaVoti = opzione }.border(2.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Text(opzione, color = if (isSelected) Color.White else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold) }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // VOTO PUBBLICO
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Voto del Pubblico", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Consenti al pubblico di votare tramite QR", color = Color.Gray, fontSize = 12.sp)
                            }
                            Switch(checked = votoPubblico, onCheckedChange = { votoPubblico = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Tema.colorePrincipale))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // NUMERO GIUDICI
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).padding(16.dp)) {
                            Text("Giudici Ufficiali: ${numeroGiudici.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = numeroGiudici,
                                onValueChange = { numeroGiudici = it },
                                valueRange = 0f..5f,
                                steps = 4,
                                colors = SliderDefaults.colors(thumbColor = Tema.colorePrincipale, activeTrackColor = Tema.colorePrincipale)
                            )
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // SALVATAGGIO E GENERAZIONE CODICI
                        Button(
                            onClick = {
                                staSalvando = true
                                scope.launch {
                                    val codPubblico = if (votoPubblico) "PUB-${generaCodice(6)}" else null
                                    val linkPubblico = if (votoPubblico) "https://freestapp.com/vote?code=$codPubblico" else null
                                    val qrPubblico = if (votoPubblico) "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=$linkPubblico" else null

                                    val listaGiudici = mutableListOf<String>()
                                    for (i in 1..numeroGiudici.toInt()) listaGiudici.add("GIU$i-${generaCodice(4)}")
                                    val codGiudici = if (listaGiudici.isNotEmpty()) listaGiudici.joinToString(",") else null

                                    databaseViewModel.salvaImpostazioniVerdetti(
                                        eventoId = eventoId, sistemaVoti = sistemaVoti, votoPubblico = votoPubblico,
                                        numeroGiudici = numeroGiudici.toInt(), codiciGiudici = codGiudici,
                                        codicePubblico = codPubblico, qrCodePubblico = qrPubblico, linkPubblico = linkPubblico
                                    )
                                    staSalvando = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(12.dp)
                        ) {
                            if (staSalvando) CircularProgressIndicator(color = Color.White)
                            else Text(if (design?.codice_accesso_pubblico == null && design?.codici_accesso_giudici == null) "GENERA CODICI E SALVA" else "AGGIORNA E RIGENERA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // MOSTRA I CODICI GENERATI (Se presenti)
                        if (design?.codice_accesso_pubblico != null || design?.codici_accesso_giudici != null) {
                            Divider(color = Color.DarkGray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("CODICI DI ACCESSO", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 22.sp, fontFamily = MioFont)

                            // QR CODE PUBBLICO
                            if (design.qr_code_pubblico != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).border(2.dp, Tema.colorePrincipale, RoundedCornerShape(12.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("VOTO PUBBLICO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("Fai inquadrare questo QR ai fan", color = Color.Gray, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    // LA MAGIA: QR Code generato al volo via URL!
                                    AsyncImage(model = design.qr_code_pubblico, contentDescription = "QR Pubblico", modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(8.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Codice Accesso App: ${design.codice_accesso_pubblico}", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }

                            // CODICI GIUDICI
                            if (!design.codici_accesso_giudici.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("PASS GIURIA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Consegna questi codici privati ai giudici:", color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                val codiciList = design.codici_accesso_giudici.split(",")
                                codiciList.forEachIndexed { index, codice ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(8.dp)).background(Tema.coloreSfondoCard).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Giudice ${index + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(codice, color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = MioFont)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }

                    2 -> {
                        // BATTLE (La Terza Parte!)
                        Box(modifier = Modifier.fillMaxSize().padding(top = 50.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painter = painterResource(id = R.drawable.versus), contentDescription = null, tint = Tema.colorePrincipale, modifier = Modifier.size(100.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("ZONA BATTLE", color = Color.White, fontSize = 28.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Qui configureremo il tabellone\ne gestiremo i voti in tempo reale.", color = Tema.coloreTestoSecondario, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { /* Pronto per la TERZA PARTE! */ }, colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale)) {
                                    Text("VAI ALLA PARTE 3", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}