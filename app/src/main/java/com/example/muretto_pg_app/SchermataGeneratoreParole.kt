package com.example.muretto_pg_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import kotlinx.coroutines.delay

@Composable
fun SchermataGeneratoreParole(onTornaIndietro: () -> Unit) {
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))
    var tabSelezionata by remember { mutableIntStateOf(0) } // 0: Classico, 1: A Tempo

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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

            Spacer(modifier = Modifier.height(32.dp))

            if (tabSelezionata == 0) {
                ModalitaClassica(MioFontPersonalizzato)
            } else {
                ModalitaATempo(MioFontPersonalizzato)
            }
        }
    }
}

@Composable
fun ModalitaClassica(font: FontFamily) {
    var parolaCorrente by remember { mutableStateOf("Premi per estrarre") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(2.dp, Color(0xFFD32F2F), RoundedCornerShape(16.dp))
                .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = parolaCorrente.uppercase(),
                color = Color.White,
                fontSize = 32.sp,
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { parolaCorrente = DatiAllenamento.parole.random() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ESTRAI PAROLA", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(1.2f))
    }
}

@Composable
fun ModalitaATempo(font: FontFamily) {
    var parolaCorrente by remember { mutableStateOf("START PER INIZIARE") }
    var attivo by remember { mutableStateOf(false) }

    LaunchedEffect(attivo) {
        if (attivo) {
            while (attivo) {
                parolaCorrente = DatiAllenamento.parole.random()
                delay(10000)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(2.dp, Color(0xFFD32F2F), RoundedCornerShape(16.dp))
                .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = parolaCorrente.uppercase(),
                color = if (attivo) Color.White else Color.Gray,
                fontSize = 32.sp,
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

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

        if (attivo) {
            Text(
                "Nuova parola ogni 10 secondi",
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.weight(1.2f))
    }
}
