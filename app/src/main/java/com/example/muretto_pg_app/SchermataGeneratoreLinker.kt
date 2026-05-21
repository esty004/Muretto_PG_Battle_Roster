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

@Composable
fun SchermataGeneratoreLinker(onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))
    var data by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
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
                Text("LINKER", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp))
                    .background(Tema.coloreSfondoCard, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (staCaricando) {
                    CircularProgressIndicator(color = Tema.colorePrincipale)
                } else if (data == null) {
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
                            text = "TOPIC:",
                            color = Tema.colorePrincipale,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MioFontPersonalizzato
                        )
                        Text(
                            text = data!!.first.uppercase(),
                            color = Tema.coloreTesto,
                            fontSize = 32.sp,
                            fontFamily = MioFontPersonalizzato,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "COLLEGA QUESTE PAROLE:",
                            color = Tema.colorePrincipale,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MioFontPersonalizzato
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        data!!.second.forEach { parola ->
                            Text(
                                text = parola.uppercase(),
                                color = Tema.coloreTesto,
                                fontSize = 24.sp,
                                fontFamily = MioFontPersonalizzato,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    scope.launch {
                        staCaricando = true
                        data = databaseViewModel.fetchLinkerData()
                        staCaricando = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("GENERA LINKER", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
