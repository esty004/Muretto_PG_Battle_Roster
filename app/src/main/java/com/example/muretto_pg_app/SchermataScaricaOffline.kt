package com.example.muretto_pg_app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SchermataScaricaOffline(onTornaIndietro: () -> Unit) {
    val vm = LocalDatabaseViewModel.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val font = FontFamily(Font(R.font.komtit__))

    var inCorso by remember { mutableStateOf(false) }
    var progresso by remember { mutableStateOf(0f) }
    var stato by remember {
        mutableStateOf(
            if (GestoreOffline.datiDisponibili(ctx)) "Versione offline presente. Puoi aggiornarla."
            else "Nessuna versione offline scaricata."
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 30.dp)) {
                    IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) { Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = font) }
                    Text("VERSIONE OFFLINE", color = Tema.coloreTesto, fontSize = 24.sp, fontFamily = font, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Scarica muretti, MC, parole e generatori (più le immagini) per usare le sezioni Muretti e Allenamento anche senza connessione.\n\nIl generatore di immagini resta disponibile solo online.",
                    color = Tema.coloreTestoSecondario, fontSize = 15.sp, textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))
                Text(stato, color = Tema.coloreTesto, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                if (inCorso) {
                    LinearProgressIndicator(progress = { progresso }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Tema.colorePrincipale)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${(progresso * 100).toInt()}%", color = Tema.coloreTestoSecondario)
                }

                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = {
                        if (inCorso) return@Button
                        inCorso = true; progresso = 0f
                        scope.launch {
                            try {
                                vm.scaricaVersioneOffline { msg, p -> stato = msg; progresso = p }
                            } catch (e: Exception) {
                                stato = "Errore: serve la connessione per scaricare. (${e.message})"
                            }
                            inCorso = false
                        }
                    },
                    enabled = !inCorso,
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(if (GestoreOffline.datiDisponibili(ctx)) "AGGIORNA VERSIONE OFFLINE" else "SCARICA VERSIONE OFFLINE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}