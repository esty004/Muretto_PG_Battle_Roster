package com.example.muretto_pg_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun SfondoSchermata(modifier: Modifier = Modifier.fillMaxSize(), descrizione: String? = null) {
    val url = Tema.sfondoGeneraleUrl
    if (!url.isNullOrBlank()) {
        AsyncImage(model = url, contentDescription = descrizione, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        // Fallback a colore pieno (niente drawable dei muretti)
        Box(modifier = modifier.background(Tema.coloreSfondo))
    }
}