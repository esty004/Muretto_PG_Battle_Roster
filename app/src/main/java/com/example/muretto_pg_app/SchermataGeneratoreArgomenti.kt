package com.example.muretto_pg_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Composable
fun SchermataGeneratoreArgomenti(onTornaIndietro: () -> Unit) {
    val context = LocalContext.current
    val argomentiPool = remember { DatiAllenamento.caricaArgomenti(context) }
    var argomentoCorrente by remember { mutableStateOf("PREMI PER\nGENERARE") }

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
                    Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = Tema.fontKomtit, fontWeight = FontWeight.Bold)
                }
                Text("ARGOMENTI", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = Tema.fontKomtit, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp))
                    .background(Tema.coloreSfondoCard, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = argomentoCorrente.uppercase(),
                    color = Tema.coloreTesto,
                    fontSize = 38.sp,
                    fontFamily = Tema.fontKomtit,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { if (argomentiPool.isNotEmpty()) argomentoCorrente = argomentiPool.random() },
                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("GENERA ARGOMENTO", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1.2f))
        }
    }
}