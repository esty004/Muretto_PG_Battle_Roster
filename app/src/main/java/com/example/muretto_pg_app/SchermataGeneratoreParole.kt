package com.example.muretto_pg_app

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
import kotlinx.coroutines.delay

@Composable
fun SchermataGeneratoreParole(onTornaIndietro: () -> Unit) {
    val context = LocalContext.current
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))
    val paroleDaJson = remember { DatiAllenamento.caricaParole(context) }
    var tabSelezionata by remember { mutableIntStateOf(0) }
    var quantitaParole by remember { mutableIntStateOf(1) }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                IconButton(
                    onClick = { onTornaIndietro() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold)
                }
                Text("PAROLE", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            TabRow(
                selectedTabIndex = tabSelezionata,
                containerColor = Color.Transparent,
                contentColor = Tema.colorePrincipale,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabSelezionata]),
                        color = Tema.colorePrincipale
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = tabSelezionata == 0,
                    onClick = { tabSelezionata = 0 },
                    text = { Text("CLASSICO", color = if (tabSelezionata == 0) Tema.coloreTesto else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = tabSelezionata == 1,
                    onClick = { tabSelezionata = 1 },
                    text = { Text("A TEMPO", color = if (tabSelezionata == 1) Tema.coloreTesto else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold) }
                )
            }

            SelettoreQuantita(quantita = quantitaParole, onQuantitaChange = { quantitaParole = it }, font = MioFontPersonalizzato)

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
        Text("PAROLE: ", color = Tema.coloreTesto, fontWeight = FontWeight.Bold, fontFamily = font, fontSize = 18.sp)
        IconButton(onClick = { if (quantita > 1) onQuantitaChange(quantita - 1) }) {
            Text("-", color = Tema.coloreTesto, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
        }
        Text(quantita.toString(), color = Tema.coloreTesto, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp), fontFamily = font)
        IconButton(onClick = { if (quantita < 10) onQuantitaChange(quantita + 1) }) {
            Text("+", color = Tema.coloreTesto, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
        }
    }
}

@Composable
fun ModalitaClassica(font: FontFamily, quantita: Int, parolePool: List<String>) {
    var paroleCorrenti by remember { mutableStateOf(listOf("PREMI PER", "ESTRARRE")) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp))
                .background(Tema.coloreSfondoCard, RoundedCornerShape(16.dp))
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
                        color = Tema.coloreTesto,
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
            onClick = { if (parolePool.isNotEmpty()) paroleCorrenti = parolePool.shuffled().take(quantita) },
            colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ESTRAI PAROLA", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ModalitaATempo(font: FontFamily, quantita: Int, parolePool: List<String>) {
    var paroleCorrenti by remember { mutableStateOf(listOf("START PER", "INIZIARE")) }
    var attivo by remember { mutableStateOf(false) }
    var tipoTempo by remember { mutableIntStateOf(0) }
    var bpm by remember { mutableIntStateOf(90) }
    var tapTimes by remember { mutableStateOf(listOf<Long>()) }

    val intervalloMillis = if (tipoTempo == 0) 10000L else (60000L / bpm.coerceAtLeast(1)) * 16

    LaunchedEffect(attivo, quantita, intervalloMillis) {
        if (attivo) {
            while (attivo) {
                if (parolePool.isNotEmpty()) paroleCorrenti = parolePool.shuffled().take(quantita)
                delay(intervalloMillis)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
            Button(
                onClick = { tipoTempo = 0 },
                colors = ButtonDefaults.buttonColors(containerColor = if (tipoTempo == 0) Tema.colorePrincipale else Color.DarkGray),
                modifier = Modifier.weight(1f).height(45.dp).padding(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) { Text("10 SECONDI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }

            Button(
                onClick = { tipoTempo = 1 },
                colors = ButtonDefaults.buttonColors(containerColor = if (tipoTempo == 1) Tema.colorePrincipale else Color.DarkGray),
                modifier = Modifier.weight(1f).height(45.dp).padding(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) { Text("BPM (4/4)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        }

        if (tipoTempo == 1) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Text("BPM: $bpm", color = Tema.coloreTesto, fontFamily = font, fontSize = 20.sp, modifier = Modifier.padding(end = 16.dp))
                Button(
                    onClick = {
                        val ora = System.currentTimeMillis()
                        val nuoviTap = (tapTimes + ora).takeLast(4)
                        tapTimes = nuoviTap
                        if (nuoviTap.size >= 2) {
                            val diffs = nuoviTap.zipWithNext { a, b -> b - a }
                            val media = diffs.average()
                            bpm = (60000 / media).toInt().coerceIn(40, 220)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("TAP BPM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp))
                .background(Tema.coloreSfondoCard, RoundedCornerShape(16.dp))
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
                        color = if (attivo) Tema.coloreTesto else Tema.coloreTestoSecondario,
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

        Button(
            onClick = { attivo = !attivo },
            colors = ButtonDefaults.buttonColors(containerColor = if (attivo) Color.Gray else Tema.colorePrincipale),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (attivo) "STOP" else "START", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (attivo) {
            Text(
                text = if (tipoTempo == 0) "Cambio ogni 10 secondi" else "Cambio ogni 4 misure ($bpm BPM)",
                color = Tema.coloreTestoSecondario,
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}