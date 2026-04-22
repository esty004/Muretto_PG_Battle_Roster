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

fun caricaParoleDaJson(context: Context): List<String> {
    return try {
        val jsonString = context.resources.openRawResource(R.raw.common_words).bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<String>>() {}.type
        Gson().fromJson(jsonString, listType)
    } catch (e: Exception) {
        listOf("Errore", "Caricamento")
    }
}

@Composable
fun SchermataGeneratoreParole(onTornaIndietro: () -> Unit) {
    val context = LocalContext.current
    val paroleDaJson = remember { caricaParoleDaJson(context) }
    val MioFontPersonalizzato = FontFamily(Font(R.font.jackboa))
    var tabSelezionata by remember { mutableIntStateOf(0) } // 0: Classico, 1: A Tempo
    var quantitaParole by remember { mutableIntStateOf(1) }

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
                paroleCorrenti = List(quantita) { parolePool.random() }
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

@Composable
fun ModalitaATempo(font: FontFamily, quantita: Int, parolePool: List<String>) {
    var paroleCorrenti by remember { mutableStateOf(listOf("START PER", "INIZIARE")) }
    var attivo by remember { mutableStateOf(false) }

    LaunchedEffect(attivo, quantita) {
        if (attivo) {
            while (attivo) {
                paroleCorrenti = List(quantita) { parolePool.random() }
                delay(10000)
            }
        }
    }

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

        Spacer(modifier = Modifier.height(24.dp))

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
                "Nuove parole ogni 10 secondi",
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
