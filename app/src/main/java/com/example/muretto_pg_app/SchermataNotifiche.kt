package com.example.muretto_pg_app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

@Composable
fun SchermataNotifiche(onTornaIndietro: () -> Unit) {
    val richieste = DatabaseMcs.richiesteInAttesa
    var richiestaSelezionata by remember { mutableStateOf<RichiestaAccount?>(null) }

    // Aggiorna al caricamento
    LaunchedEffect(Unit) {
        DatabaseMcs.fetchRichiesteInAttesa()
    }

    // Dialog dettaglio richiesta
    richiestaSelezionata?.let { richiesta ->
        DialogDettaglioRichiesta(
            richiesta = richiesta,
            onDismiss = { richiestaSelezionata = null },
            onChiudi = { richiestaSelezionata = null }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1A1A1A)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = onTornaIndietro,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("<", color = Color.White, fontSize = 45.sp, fontFamily = Tema.fontKomtit)
                }
                Text(
                    "NOTIFICHE",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontFamily = Tema.fontKomtit,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                // Badge con numero richieste
                if (richieste.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(28.dp)
                            .background(Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            richieste.size.toString(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (richieste.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Nessuna notifica in attesa",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(richieste) { richiesta ->
                        CardRichiesta(
                            richiesta = richiesta,
                            onClick = { richiestaSelezionata = richiesta }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardRichiesta(richiesta: RichiestaAccount, onClick: () -> Unit) {
    val etichettaTipo = when (richiesta.tipo_account) {
        "organizzatore_muretto" -> "Org. Muretto"
        "organizzatore_eventi" -> "Org. Eventi"
        else -> richiesta.tipo_account
    }
    val coloreTipo = when (richiesta.tipo_account) {
        "organizzatore_muretto" -> Color(0xFFD32F2F)
        "organizzatore_eventi" -> Color(0xFF1E88E5)
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
            .background(Color(0xFF222222))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Pallino rosso notifica
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.Red, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    richiesta.nome_arte.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${richiesta.nome} ${richiesta.cognome}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(coloreTipo.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(1.dp, coloreTipo, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(etichettaTipo, color = coloreTipo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("›", color = Color.Gray, fontSize = 24.sp)
        }
    }
}

@Composable
fun DialogDettaglioRichiesta(
    richiesta: RichiestaAccount,
    onDismiss: () -> Unit,
    onChiudi: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var staElaborando by remember { mutableStateOf(false) }
    var messaggioEsito by remember { mutableStateOf("") }
    var operazioneCompletata by remember { mutableStateOf(false) }

    val etichettaMuretto = when (richiesta.muretto) {
        "muretto_pg" -> "Muretto PG"
        "barre_faul" -> "Barre Faul"
        else -> "-"
    }

    Dialog(onDismissRequest = { if (!staElaborando) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF333333), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "RICHIESTA ACCOUNT",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (operazioneCompletata) {
                    Text(
                        messaggioEsito,
                        color = if (messaggioEsito.contains("Errore")) Color.Red else Color(0xFF4CAF50),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    Button(
                        onClick = onChiudi,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("CHIUDI", color = Color.White) }
                    return@Column
                }

                // Dati richiesta
                RigaDato("Nome", "${richiesta.nome} ${richiesta.cognome}")
                RigaDato("Nome d'arte", richiesta.nome_arte)
                RigaDato("Email", richiesta.email)
                RigaDato("Telefono", richiesta.telefono)
                RigaDato("Tipo account", when (richiesta.tipo_account) {
                    "organizzatore_muretto" -> "Organizzatore Muretto"
                    "organizzatore_eventi" -> "Organizzatore Eventi"
                    else -> richiesta.tipo_account
                })
                if (richiesta.tipo_account == "organizzatore_muretto") {
                    RigaDato("Muretto", etichettaMuretto)
                }

                if (messaggioEsito.isNotEmpty()) {
                    Text(messaggioEsito, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (staElaborando) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                    }
                } else {
                    // Bottoni Accetta / Rifiuta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // RIFIUTA
                        Button(
                            onClick = {
                                staElaborando = true
                                scope.launch {
                                    val ok = DatabaseMcs.rifiutaRichiesta(richiesta.id)
                                    staElaborando = false
                                    operazioneCompletata = true
                                    messaggioEsito = if (ok) "Richiesta rifiutata." else "Errore durante il rifiuto."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("RIFIUTA", color = Color.Red, fontWeight = FontWeight.Bold)
                        }

                        // ACCETTA
                        Button(
                            onClick = {
                                staElaborando = true
                                scope.launch {
                                    val ok = DatabaseMcs.accettaRichiesta(richiesta)
                                    staElaborando = false
                                    if (ok) {
                                        // Apri WhatsApp con messaggio precompilato
                                        val numero = richiesta.telefono
                                            .replace("+", "")
                                            .replace(" ", "")
                                            .replace("-", "")
                                        val messaggio = Uri.encode(
                                            "La tua richiesta di creazione account FreestApp è stata accettata, " +
                                                    "controlla la tua mail (anche nello spam) per completare la registrazione."
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://wa.me/$numero?text=$messaggio")
                                        }
                                        try { context.startActivity(intent) } catch (e: Exception) { }

                                        operazioneCompletata = true
                                        messaggioEsito = "Account creato! WhatsApp aperto con il messaggio."
                                    } else {
                                        messaggioEsito = "Errore durante la creazione dell'account."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("ACCETTA", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { if (!staElaborando) onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Annulla", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun RigaDato(etichetta: String, valore: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(etichetta, color = Color.Gray, fontSize = 12.sp)
        Text(valore, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Divider(color = Color(0xFF333333), thickness = 0.5.dp, modifier = Modifier.padding(top = 4.dp))
    }
}