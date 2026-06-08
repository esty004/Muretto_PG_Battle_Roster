package com.example.muretto_pg_app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun SchermataGestioneMuretti(onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val font = FontFamily(Font(R.font.komtit__))

    // campi testo
    var nome by remember { mutableStateOf("") }
    var descrizione by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var orari by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var scalaPin by remember { mutableStateOf("1.0") }

    // colori (hex #AARRGGBB)
    var coloreCornici by remember { mutableStateOf(Color(0xFFD32F2F)) }
    var coloreBottoni by remember { mutableStateOf(Color(0xFFD32F2F)) }

    // immagini: chiave -> uri scelto
    val immagini = remember { mutableStateMapOf<String, Uri>() }
    var targetCorrente by remember { mutableStateOf("") }
    val selettore = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && targetCorrente.isNotEmpty()) immagini[targetCorrente] = uri
    }

    var staSalvando by remember { mutableStateOf(false) }
    var messaggio by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { databaseViewModel.fetchMuretti() }

    fun Color.toHex(): String {
        val a = (alpha * 255).toInt(); val r = (red * 255).toInt(); val g = (green * 255).toInt(); val b = (blue * 255).toInt()
        return String.format("#%02X%02X%02X%02X", a, r, g, b)
    }

    val coloriInput = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
        focusedBorderColor = Tema.colorePrincipale, unfocusedBorderColor = Color.Gray,
        focusedLabelColor = Tema.colorePrincipale, unfocusedLabelColor = Color.LightGray, cursorColor = Tema.colorePrincipale
    )
    val palette = listOf(Color(0xFFD32F2F), Color(0xFF1E88E5), Color(0xFFFF9800), Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFB71C1C), Color(0xFF00BCD4), Color.White, Color.Black)

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                    IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) { Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = font) }
                    Text("GESTIONE MURETTI", color = Tema.coloreTesto, fontSize = 26.sp, fontFamily = font, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                Text("NUOVO MURETTO", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome (unico)") }, colors = coloriInput, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = descrizione, onValueChange = { descrizione = it }, label = { Text("Descrizione") }, colors = coloriInput, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = instagram, onValueChange = { instagram = it }, label = { Text("Instagram") }, colors = coloriInput, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = orari, onValueChange = { orari = it }, label = { Text("Orari di ritrovo") }, colors = coloriInput, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, colors = coloriInput, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Spacer(modifier = Modifier.height(16.dp))
                Text("POSIZIONE SULLA MAPPA", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Lat") }, colors = coloriInput, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = lng, onValueChange = { lng = it }, label = { Text("Lng") }, colors = coloriInput, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = scalaPin, onValueChange = { scalaPin = it }, label = { Text("Scala pin") }, colors = coloriInput, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("COLORI", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                PaletteColori("Cornici card", palette, coloreCornici) { coloreCornici = it }
                Spacer(modifier = Modifier.height(8.dp))
                PaletteColori("Bottone indietro / player", palette, coloreBottoni) { coloreBottoni = it }

                Spacer(modifier = Modifier.height(16.dp))
                Text("IMMAGINI", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val pick: (String) -> Unit = { key -> targetCorrente = key; selettore.launch("image/*") }
                SelettoreImmagine("Logo (pin/elenco)", immagini["logo"], pick, "logo")
                SelettoreImmagine("Icona pin mappa", immagini["pin"], pick, "pin")
                SelettoreImmagine("Sfondo schermata iniziale", immagini["sfondo_iniziale"], pick, "sfondo_iniziale")
                SelettoreImmagine("Sfondo generale", immagini["sfondo"], pick, "sfondo")
                SelettoreImmagine("Sfondo card Muretto classico", immagini["card_muretto"], pick, "card_muretto")
                SelettoreImmagine("Sfondo card 2 vs 2", immagini["card_2v2"], pick, "card_2v2")
                SelettoreImmagine("Sfondo card Contest", immagini["card_contest"], pick, "card_contest")

                if (messaggio.isNotEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(messaggio, color = if (messaggio.startsWith("Errore")) Color.Red else Color(0xFF4CAF50), fontWeight = FontWeight.Bold) }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (nome.isBlank()) { messaggio = "Errore: il nome è obbligatorio."; return@Button }
                        staSalvando = true; messaggio = ""
                        scope.launch {
                            val bytes = mutableMapOf<String, ByteArray>()
                            immagini.forEach { (k, uri) ->
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.let { bytes[k] = it }
                            }
                            val ok = databaseViewModel.creaMuretto(
                                nome = nome, descrizione = descrizione, instagram = instagram, orari = orari, location = location,
                                lat = lat.toDoubleOrNull(), lng = lng.toDoubleOrNull(), scalaPin = scalaPin.toFloatOrNull() ?: 1.0f,
                                coloreCornici = coloreCornici.toHex(), coloreBottoni = coloreBottoni.toHex(),
                                immagini = bytes
                            )
                            staSalvando = false
                            if (ok) {
                                messaggio = "Muretto creato!"
                                nome = ""; descrizione = ""; instagram = ""; orari = ""; location = ""; lat = ""; lng = ""; scalaPin = "1.0"; immagini.clear()
                            } else messaggio = "Errore nel salvataggio (nome già esistente?)."
                        }
                    },
                    enabled = !staSalvando,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { if (staSalvando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp)) else Text("CREA MURETTO", color = Color.White, fontWeight = FontWeight.Bold) }

                Spacer(modifier = Modifier.height(28.dp))
                Text("MURETTI ESISTENTI (${databaseViewModel.murettiCloud.size})", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                databaseViewModel.murettiCloud.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).padding(10.dp)) {
                        if (!m.immagineURL.isNullOrBlank()) AsyncImage(model = m.immagineURL, contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(m.name, color = Tema.coloreTesto, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun PaletteColori(label: String, palette: List<Color>, selezionato: Color, onScegli: (Color) -> Unit) {
    Text(label, color = Color.White, fontSize = 13.sp)
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        palette.forEach { c ->
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(c).border(3.dp, if (c == selezionato) Color.White else Color.Transparent, CircleShape).clickable { onScegli(c) }, contentAlignment = Alignment.Center) {
                if (c == selezionato) Icon(Icons.Default.Check, contentDescription = null, tint = if (c == Color.White) Color.Black else Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SelettoreImmagine(label: String, uri: Uri?, onPick: (String) -> Unit, key: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(10.dp)).background(Tema.coloreSfondoCard).border(2.dp, if (uri != null) Color(0xFF4CAF50) else Color.Gray, RoundedCornerShape(10.dp)).clickable { onPick(key) },
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            else Text("+", color = Color.Gray, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Tema.coloreTesto, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (uri != null) Text("✓", color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}