package com.example.muretto_pg_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataGestioneBattleEvento(eventoId: String, onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFont = FontFamily(Font(R.font.komtit__))
    val scope = rememberCoroutineScope()

    val evento = databaseViewModel.eventiApprovati.find { it.id == eventoId }

    // Variabili di stato per i campi di testo
    var inputMcs by remember { mutableStateOf("") }
    var inputTema by remember { mutableStateOf("") }

    // Variabili per l'output dell'Intelligenza Artificiale
    var rispostaIA by remember { mutableStateOf("L'IA è pronta. Cosa vuoi generare per l'evento?") }
    var staCaricando by remember { mutableStateOf(false) }

    // Inizializzazione di Google Gemini
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyBRi8cb5BqPjxFwdzIUXUpdHjJAdMA7ivQ" // La tua API Key di Google
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER
            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFont)
                }
                Text("BATTLE IA", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            if (evento != null) {
                Text(evento.titolo.uppercase(), color = Tema.colorePrincipale, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = MioFont)
                Spacer(modifier = Modifier.height(24.dp))

                // --- SEZIONE 1: TABELLONE SCONTRI ---
                OutlinedTextField(
                    value = inputMcs,
                    onValueChange = { inputMcs = it },
                    label = { Text("Inserisci gli MC (es: Ensi, Shade, Morbo...)", color = Tema.coloreTestoSecondario) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Tema.coloreTesto,
                        unfocusedTextColor = Tema.coloreTesto,
                        focusedBorderColor = Tema.colorePrincipale,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (inputMcs.isNotBlank()) {
                            staCaricando = true
                            scope.launch {
                                try {
                                    val prompt = "Sei il direttore artistico di una battle di freestyle rap. Crea un tabellone degli ottavi di finale avvincente usando questi MC: $inputMcs. Genera accoppiamenti interessanti e casuali e scrivi una breve frase ad effetto per presentare l'evento al pubblico. Evita lunghi discorsi, sii diretto e hip-hop."
                                    val response = generativeModel.generateContent(prompt)
                                    rispostaIA = response.text ?: "Errore: risposta vuota."
                                } catch (e: Exception) {
                                    rispostaIA = "Errore di connessione a Gemini: ${e.message}"
                                } finally {
                                    staCaricando = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !staCaricando && inputMcs.isNotBlank()
                ) {
                    Text("🎲 GENERA ACCOPPIAMENTI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SEZIONE 2: MODALITÀ A TEMA ---
                OutlinedTextField(
                    value = inputTema,
                    onValueChange = { inputTema = it },
                    label = { Text("Tema Evento (es: Cyberpunk, Horror, Anni 90)", color = Tema.coloreTestoSecondario) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Tema.coloreTesto,
                        unfocusedTextColor = Tema.coloreTesto,
                        focusedBorderColor = Tema.colorePrincipale,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (inputTema.isNotBlank()) {
                            staCaricando = true
                            scope.launch {
                                try {
                                    val prompt = "Crea delle modalità di battle freestyle rap (per Ottavi, Quarti, Semifinale e Finale) basate rigorosamente su questo tema: '$inputTema'. Per i turni a 4/4, includi un elenco di 10 parole o argomenti specifici legati al tema che i rapper dovranno usare. Sii creativo, originale e formatta il testo in modo chiaro."
                                    val response = generativeModel.generateContent(prompt)
                                    rispostaIA = response.text ?: "Errore: risposta vuota."
                                } catch (e: Exception) {
                                    rispostaIA = "Errore di connessione a Gemini: ${e.message}"
                                } finally {
                                    staCaricando = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !staCaricando && inputTema.isNotBlank()
                ) {
                    Text("💡 GENERA MODALITÀ E ARGOMENTI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- SCHERMO OUTPUT DELL'IA ---
                Card(
                    modifier = Modifier.fillMaxWidth().border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Tema.coloreSfondoCard)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("🤖 OUTPUT ASSISTENTE IA:", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (staCaricando) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Tema.colorePrincipale)
                            }
                        } else {
                            Text(
                                text = rispostaIA,
                                color = Tema.coloreTesto,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

            } else {
                Text("Errore: Evento non trovato", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}