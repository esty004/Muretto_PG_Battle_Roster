package com.example.muretto_pg_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchermataGeneratoreTaboo(onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))
    var tabooCorrente by remember { mutableStateOf<Topic?>(null) }
    val scope = rememberCoroutineScope()
    var staCaricando by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 40.dp)) {
                IconButton(
                    onClick = { onTornaIndietro() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold)
                }
                Text("TABOO", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp))
                    .background(Tema.coloreSfondoCard, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (staCaricando) {
                    CircularProgressIndicator(color = Tema.colorePrincipale)
                } else if (tabooCorrente == null) {
                    Text(
                        text = "PREMI PER\nGENERARE",
                        color = Tema.coloreTesto,
                        fontSize = 34.sp,
                        fontFamily = MioFontPersonalizzato,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tabooCorrente!!.valore.uppercase(),
                            color = Color.White,
                            fontSize = 38.sp,
                            fontFamily = MioFontPersonalizzato,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Badge per le parole vietate
                        val vietate = tabooCorrente!!.parole_vietate?.split(",")?.map { it.trim() } ?: emptyList()
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            maxItemsInEachRow = 3
                        ) {
                            vietate.forEach { parola ->
                                Surface(
                                    modifier = Modifier.padding(4.dp),
                                    color = Color(0xFFD32F2F), // bg-red-600 logic
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = parola.uppercase(),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    scope.launch {
                        staCaricando = true
                        tabooCorrente = databaseViewModel.fetchRandomTaboo()
                        staCaricando = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("GENERA TABOO", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1.2f))
        }
    }
}
