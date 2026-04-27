package com.example.muretto_pg_app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay

/**
 * Carica la lista delle parole da un file JSON situato nelle risorse raw.
 */
fun caricaParoleDaJson(context: Context): List<String> {
    return try {
        val jsonString = context.resources.openRawResource(R.raw.common_words).bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<String>>() {}.type
        Gson().fromJson(jsonString, listType)
    } catch (e: Exception) {
        listOf("Errore", "Caricamento")
    }
}

/**
 * Schermata del generatore di parole.
 * Permette l'estrazione manuale (Classico) o automatica (A Tempo).
 */
@Composable
fun SchermataGeneratoreParole(onTornaIndietro: () -> Unit) {
    val context = LocalContext.current
    val paroleDaJson = remember { caricaParoleDaJson(context) }
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))
    var tabSelezionata by remember { mutableIntStateOf(0) } // 0: Classico, 1: A Tempo
    var quantitaParole by remember { mutableIntStateOf(1) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                IconButton(
                    onClick = { onTornaIndietro() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("<", color = Color.White, fontSize = 45.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "PAROLE",
                    color = Color.White, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Tabs per cambiare modalità di generazione
            TabRow(
                selectedTabIndex = tabSelezionata,
                containerColor = Color.Transparent,
                contentColor = Color(0xFFD32F2F),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabSelezionata]),
                        color = Color(0xFFD32F2F)
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = tabSelezionata == 0,
                    onClick = { tabSelezionata = 0 },
                    text = { Text("CLASSICO", color = if (tabSelezionata == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = tabSelezionata == 1,
                    onClick = { tabSelezionata = 1 },
                    text = { Text("A TEMPO", color = if (tabSelezionata == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold) }
                )
            }

            // Selettore comune per quante parole estrarre contemporaneamente
            SelettoreQuantita(
                quantita = quantitaParole,
                onQuantitaChange = { quantitaParole = it },
                font = MioFontPersonalizzato
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (tabSelezionata == 0) {
                ModalitaClassica(MioFontPersonalizzato, quantitaParole, paroleDaJson)
            } else {
                ModalitaATempo(MioFontPersonalizzato, quantitaParole, paroleDaJson)
            }
        }
    }
}

@Composable
fun SelettoreQuantita(quantita: Int, onQuantitaChange: (Int) -> Unit, font: FontFamily) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text("PAROLE: ", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = font, fontSize = 18.sp)
        
        IconButton(onClick = { if (quantita > 1) onQuantitaChange(quantita - 1) }) {
            Text("-", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
        }
        
        Text(
            quantita.toString(),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontFamily = font
        )
        
        IconButton(onClick = { if (quantita < 10) onQuantitaChange(quantita + 1) }) {
            Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
        }
    }
}

/**
 * Modalità Classica: l'utente preme un tasto per estrarre nuove parole.
 */
@Composable
fun ModalitaClassica(font: FontFamily, quantita: Int, parolePool: List<String>) {
    var paroleCorrenti by remember { mutableStateOf(listOf("PREMI PER", "ESTRARRE")) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, Color(0xFFD32F2F), RoundedCornerShape(16.dp))
                .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                paroleCorrenti.forEach { parola ->
                    Text(
                        text = parola.uppercase(),
                        color = Color.White,
                        fontSize = if (quantita > 6) 32.sp else 46.sp,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = if (quantita > 6) 40.sp else 54.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { 
                paroleCorrenti = parolePool.shuffled().take(quantita)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ESTRAI PAROLA", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Modalità A Tempo: le parole cambiano automaticamente basandosi su un intervallo
 * fisso (10s) o sul ritmo (BPM).
 */
@Composable
fun ModalitaATempo(font: FontFamily, quantita: Int, parolePool: List<String>) {
    var paroleCorrenti by remember { mutableStateOf(listOf("START PER", "INIZIARE")) }
    var attivo by remember { mutableStateOf(false) }

    // Stati per la gestione dinamica del tempo
    var tipoTempo by remember { mutableIntStateOf(0) } // 0: 10 secondi fisso, 1: Basato su BPM
    var bpm by remember { mutableIntStateOf(90) }
    var tapTimes by remember { mutableStateOf(listOf<Long>()) }

    // Calcolo dell'intervallo di attesa
    // Se BPM: 60.000ms / BPM * 16 (per cambiare parola ogni 4 battute in 4/4)
    val intervalloMillis = if (tipoTempo == 0) 10000L else (60000L / bpm.coerceAtLeast(1)) * 16

    // Loop di estrazione automatica
    LaunchedEffect(attivo, quantita, intervalloMillis) {
        if (attivo) {
            while (attivo) {
                paroleCorrenti = parolePool.shuffled().take(quantita)
                delay(intervalloMillis)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Switcher tra Secondi fissi e BPM
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { tipoTempo = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tipoTempo == 0) Color(0xFFD32F2F) else Color.DarkGray
                ),
                modifier = Modifier.weight(1f).height(45.dp).padding(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("10 SECONDI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { tipoTempo = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tipoTempo == 1) Color(0xFFD32F2F) else Color.DarkGray
                ),
                modifier = Modifier.weight(1f).height(45.dp).padding(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("BPM (4/4)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Sezione interattiva per il TAP dei BPM
        if (tipoTempo == 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text("BPM: $bpm", color = Color.White, fontFamily = font, fontSize = 20.sp, modifier = Modifier.padding(end = 16.dp))
                Button(
                    onClick = {
                        val ora = System.currentTimeMillis()
                        val nuoviTap = (tapTimes + ora).takeLast(4) // Media degli ultimi 4 tap
                        tapTimes = nuoviTap
                        if (nuoviTap.size >= 2) {
                            val diffs = nuoviTap.zipWithNext { a, b -> b - a }
                            val media = diffs.average()
                            bpm = (60000 / media).toInt().coerceIn(40, 220)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("TAP BPM", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Box visualizzazione parole
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, Color(0xFFD32F2F), RoundedCornerShape(16.dp))
                .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                paroleCorrenti.forEach { parola ->
                    Text(
                        text = parola.uppercase(),
                        color = if (attivo) Color.White else Color.Gray,
                        fontSize = if (quantita > 6) 32.sp else 46.sp,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = if (quantita > 6) 40.sp else 54.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tasto Start/Stop
        Button(
            onClick = { attivo = !attivo },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (attivo) Color.Gray else Color(0xFFD32F2F)
            ),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (attivo) "STOP" else "START",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Info testuale sullo stato corrente
        if (attivo) {
            Text(
                text = if (tipoTempo == 0) "Cambio ogni 10 secondi" else "Cambio ogni 4 misure ($bpm BPM)",
                color = Color.Gray,
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
