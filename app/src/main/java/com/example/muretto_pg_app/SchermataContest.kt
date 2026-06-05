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
import androidx.compose.material3.*
import io.github.jan.supabase.postgrest.postgrest
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
import coil.compose.AsyncImage

@Composable
fun SchermataContest(isGlobale: Boolean, onTornaIndietro: () -> Unit, onNavigate: (String) -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFont = FontFamily(Font(R.font.komtit__))
    LaunchedEffect(Unit) { databaseViewModel.fetchEventiApprovati() }
    // Filtro per mostrare SOLO i Contest (eventi con design o etichettati come tali)
    val tuttiIContest = databaseViewModel.eventiApprovati.filter { it.contest_design != null }

    // Logica di visualizzazione (Globale vs Locale)
    val listaContest = if (isGlobale) {
        tuttiIContest
    } else {
        val murettoCorrente = Tema.ottieniIdMurettoAttivo()
        tuttiIContest.filter { it.muretto_id == murettoCorrente }
    }

    // Regole per mostrare il bottone "+ CREA CONTEST"
    val puoCreaContest = when {
        databaseViewModel.isAdmin -> true
        isGlobale && databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_EVENTI -> true
        !isGlobale && databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_MURETTO -> true
        else -> false
    }

    // Stato per il PopUp bloccante per i Rapper/Senza Account
    var mostraPopupBlocco by remember { mutableStateOf<Evento?>(null) }
    var statoLiveContest by remember { mutableStateOf<String?>(null) }
    var mostraInserisciCodice by remember { mutableStateOf<Evento?>(null) }
    var codiceGiudiceInput by remember { mutableStateOf("") }
    var erroreCodice by remember { mutableStateOf("") }

    LaunchedEffect(mostraPopupBlocco) {
        statoLiveContest = null
        val c = mostraPopupBlocco
        if (c != null) {
            try {
                val row = databaseViewModel.supabase.postgrest["contest_battle"]
                    .select { filter { eq("evento_id", c.id) } }
                    .decodeList<ContestBattleRow>().firstOrNull()
                statoLiveContest = row?.stato
            } catch (e: Exception) { statoLiveContest = null }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(painter = painterResource(id = Tema.sfondoGenerale), contentDescription = "Sfondo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 20.dp)) {
                    IconButton(onClick = { onTornaIndietro() }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFont)
                    }
                    Text(if (isGlobale) "TUTTI I CONTEST" else "CONTEST LOCALI", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                if (puoCreaContest) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(20.dp)).background(Tema.coloreSfondoCard).border(3.dp, Tema.colorePrincipale, RoundedCornerShape(20.dp)).clickable { onNavigate("aggiungi_contest") },
                        contentAlignment = Alignment.Center
                    ) { Text("+ CREA NUOVO CONTEST", color = Tema.coloreTesto, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = MioFont) }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (listaContest.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            "DEBUG\napprovati = ${databaseViewModel.eventiApprovati.size}\ncon_design = ${databaseViewModel.eventiApprovati.count { it.contest_design != null }}\nisGlobale = $isGlobale",
                            color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                        items(listaContest) { contest ->
                            CardContest(
                                contest = contest,
                                onClick = {
                                    val haPermessi = databaseViewModel.isAdmin ||
                                            databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_MURETTO ||
                                            databaseViewModel.ruoloAttuale == RuoloUtente.ORGANIZZATORE_EVENTI
                                    if (haPermessi) {
                                        // Li mandiamo alla schermata di gestione che creeremo nel Prossimo Step
                                        onNavigate("dettaglio_contest/${contest.id}")
                                    } else {
                                        // Mostriamo l'avviso ai normali utenti
                                        mostraPopupBlocco = contest
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostraPopupBlocco != null) {
        val contest = mostraPopupBlocco!!
        val live = statoLiveContest == "iniziato"
        AlertDialog(
            onDismissRequest = { mostraPopupBlocco = null },
            containerColor = Tema.coloreSfondoCard,
            title = { Text(if (live) "BATTLE IN CORSO" else "CONTEST", color = Tema.coloreTesto, fontFamily = MioFont, fontSize = 24.sp) },
            text = { Text(if (live) "Entra come spettatore o giudice." else "Inizio: ${contest.data_ora}\nApri la trasferta per i dettagli.", color = Tema.coloreTestoSecondario, fontSize = 16.sp) },
            confirmButton = {
                if (live) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), onClick = {
                            IngressoContest.nome = "Spettatore"; IngressoContest.codiceGiudice = null
                            val id = contest.id; mostraPopupBlocco = null; onNavigate("battle_live/$id/spettatore")
                        }) { Text("SPETTATORE", color = Color.White, fontWeight = FontWeight.Bold) }
                        Button(modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), onClick = {
                            mostraInserisciCodice = contest; mostraPopupBlocco = null
                        }) { Text("GIUDICE", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Button(colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), onClick = {
                        val id = contest.id; mostraPopupBlocco = null; onNavigate("trasferte_focus/$id")
                    }) { Text("VAI ALLA TRASFERTA", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = { TextButton(onClick = { mostraPopupBlocco = null }) { Text("CHIUDI", color = Tema.coloreTestoSecondario) } }
        )
    }

    if (mostraInserisciCodice != null) {
        val contest = mostraInserisciCodice!!
        AlertDialog(
            onDismissRequest = { mostraInserisciCodice = null; codiceGiudiceInput = ""; erroreCodice = "" },
            containerColor = Tema.coloreSfondoCard,
            title = { Text("ACCESSO GIUDICE", color = Tema.coloreTesto, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Inserisci il codice giudice ricevuto dall'organizzatore.", color = Tema.coloreTestoSecondario, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = codiceGiudiceInput, onValueChange = { codiceGiudiceInput = it.uppercase() }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale))
                    if (erroreCodice.isNotEmpty()) Text(erroreCodice, color = Color.Red, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), onClick = {
                    val validi = contest.contest_design?.codici_accesso_giudici?.split(",")?.map { it.trim() } ?: emptyList()
                    if (codiceGiudiceInput.trim() in validi) {
                        IngressoContest.nome = "Giudice"; IngressoContest.codiceGiudice = codiceGiudiceInput.trim()
                        val id = contest.id
                        mostraInserisciCodice = null; codiceGiudiceInput = ""; erroreCodice = ""
                        onNavigate("battle_live/$id/giudice")
                    } else erroreCodice = "Codice non valido."
                }) { Text("ENTRA", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { mostraInserisciCodice = null; codiceGiudiceInput = "" }) { Text("Annulla", color = Tema.coloreTestoSecondario) } }
        )
    }
}

@Composable
fun CardContest(contest: Evento, onClick: () -> Unit) {
    // Leggiamo i colori custom se esistono, altrimenti usiamo quelli del muretto base
    val design = contest.contest_design
    val coloreCornice = design?.colore_sfondo?.let { android.graphics.Color.parseColor(it) }?.let { Color(it) } ?: Tema.colorePrincipale

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Tema.coloreSfondoCard).border(3.dp, coloreCornice, RoundedCornerShape(20.dp)).clickable { onClick() }
    ) {
        Column {
            if (contest.immagine_url != null) {
                AsyncImage(model = contest.immagine_url, contentDescription = "Locandina", modifier = Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Crop)
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(contest.titolo.uppercase(), color = Tema.coloreTesto, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.jackboa)))
                Spacer(modifier = Modifier.height(4.dp))
                Text("📍 ${contest.location_nome}", color = Tema.coloreTestoSecondario, fontSize = 14.sp)
                Text("📅 ${contest.data_ora}", color = Tema.coloreTestoSecondario, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth().background(coloreCornice, RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                    Text("TOCCA PER ENTRARE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}