package com.example.muretto_pg_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
fun SchermataGeneratoreImmagini(onTornaIndietro: () -> Unit) {
    val MioFontPersonalizzato = FontFamily(Font(R.font.komtit__))
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var key by remember { mutableIntStateOf(0) } // Per forzare il ricaricamento dell'immagine random

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
                Text("IMMAGINI", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFontPersonalizzato, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp))
                    .background(Tema.coloreSfondoCard),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl == null) {
                    Text(
                        text = "PREMI PER\nGENERARE",
                        color = Tema.coloreTesto,
                        fontSize = 34.sp,
                        fontFamily = MioFontPersonalizzato,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Random Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = null,
                        error = painterResource(R.drawable.no_pic)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    key = (1..1000000).random()
                    imageUrl = "https://picsum.photos/800/800?random=$key"
                },
                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("GENERA IMMAGINE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
