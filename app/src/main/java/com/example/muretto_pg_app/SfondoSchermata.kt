package com.example.muretto_pg_app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage

/**
 * Sfondo generale della schermata, DB-driven.
 * Usa l'URL del muretto selezionato (Tema.sfondoGeneraleUrl) se presente,
 * altrimenti il drawable storico di fallback (Tema.sfondoGenerale).
 * Sostituisce ovunque il vecchio: Image(painter = painterResource(Tema.sfondoGenerale), ...)
 */
@Composable
fun SfondoSchermata(modifier: Modifier = Modifier.fillMaxSize(), descrizione: String? = null) {
    val url = Tema.sfondoGeneraleUrl
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = descrizione,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = Tema.sfondoGenerale),
            contentDescription = descrizione,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}