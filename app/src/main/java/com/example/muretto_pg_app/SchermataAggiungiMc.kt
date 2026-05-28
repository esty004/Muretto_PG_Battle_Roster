package com.example.muretto_pg_app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataAggiungiMc(onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val MioFont = FontFamily(Font(R.font.komtit__))

    var nomeMc by remember { mutableStateOf("") }
    var murettoSelezionatoId by remember { mutableStateOf("09fbe1d3-0022-41b8-ba4b-edc887c145a2") } // 1 = Muretto PG, 2 = Barre Faul
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    var staCaricando by remember { mutableStateOf(false) }
    var messaggioEsito by remember { mutableStateOf("") }

    // Launcher per aprire la galleria e selezionare una foto
    val selettoreImmagine = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFont)
                }
                Text("NUOVO MC", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, modifier = Modifier.align(Alignment.Center))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Box Immagine Profilo
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Tema.coloreSfondoCard)
                    .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp))
                    .clickable { selettoreImmagine.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Foto Selezionata",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(id = R.drawable.ic_music_note), contentDescription = null, tint = Tema.coloreTestoSecondario, modifier = Modifier.size(40.dp))
                        Text("Tocca per\ncaricare foto", color = Tema.coloreTestoSecondario, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Campo Nome
            OutlinedTextField(
                value = nomeMc,
                onValueChange = { nomeMc = it },
                label = { Text("Nome Freestyler", color = Tema.coloreTestoSecondario) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Selettore Muretto
            Text("A quale Muretto appartiene?", color = Tema.coloreTesto, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))

            val murettiMap = mapOf(
                "Muretto PG" to "09fbe1d3-0022-41b8-ba4b-edc887c145a2",
                "Barre Faul" to "2d0f412c-4e9d-4eab-b886-f7a2226d7b9e",
                "Fortitudo" to "22ea8a2f-d45d-40b2-a6ee-841058f12f99"
            )
            var nomeMurettoSelezionato by remember { mutableStateOf("Muretto PG") }
            var menuMurettoMc by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(expanded = menuMurettoMc, onExpandedChange = { menuMurettoMc = !menuMurettoMc }) {
                OutlinedTextField(
                    value = nomeMurettoSelezionato, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuMurettoMc) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = menuMurettoMc, onDismissRequest = { menuMurettoMc = false }, modifier = Modifier.background(Tema.coloreSfondoCard)) {
                    murettiMap.keys.forEach { opzione ->
                        DropdownMenuItem(text = { Text(opzione, color = Tema.coloreTesto) }, onClick = { nomeMurettoSelezionato = opzione; murettoSelezionatoId = murettiMap[opzione]!!; menuMurettoMc = false })
                    }
                }
            }

            if (messaggioEsito.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(messaggioEsito, color = if (messaggioEsito.contains("Errore")) Color.Red else Color.Green, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottone Salva
            Button(
                onClick = {
                    if (nomeMc.isBlank()) {
                        messaggioEsito = "Errore: Inserisci il nome!"
                        return@Button
                    }
                    staCaricando = true
                    messaggioEsito = ""

                    scope.launch {
                        // Converte l'URI dell'immagine in ByteArray per Supabase
                        val bytesImmagine = imageUri?.let { uri ->
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }

                        val successo = databaseViewModel.inserisciNuovoMc(nomeMc.trim(), murettoSelezionatoId, bytesImmagine)
                        staCaricando = false
                        if (successo) {
                            messaggioEsito = "Freestyler aggiunto con successo!"
                            nomeMc = ""
                            imageUri = null
                            // Aggiorna la lista globale usando l'ID numerico
                            databaseViewModel.fetchMcsDalCloud(if (Tema.isBarreFaul) "2d0f412c-4e9d-4eab-b886-f7a2226d7b9e" else "09fbe1d3-0022-41b8-ba4b-edc887c145a2")
                        } else {
                            messaggioEsito = "Errore durante il caricamento."
                        }
                    }
                },
                enabled = !staCaricando,
                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth(0.9f).height(60.dp).padding(bottom = 10.dp)
            ) {
                if (staCaricando) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("INSERISCI MC", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
