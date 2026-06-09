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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private fun hexToColor(s: String?, fallback: Color): Color = try {
    if (s.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(s))
} catch (e: Exception) { fallback }

private fun Color.toHex(): String {
    val a = (alpha * 255).toInt(); val r = (red * 255).toInt(); val g = (green * 255).toInt(); val b = (blue * 255).toInt()
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}

@Composable
fun SchermataGestioneMuretti(onTornaIndietro: () -> Unit) {
    val vm = LocalDatabaseViewModel.current
    val font = FontFamily(Font(R.font.komtit__))

    val isAdmin = vm.isAdmin
    val mioMurettoId = vm.profiloAttuale?.muretto_id
    val murettiVisibili = if (isAdmin) vm.murettiCloud.toList()
    else vm.murettiCloud.filter { it.id == mioMurettoId }

    var murettoInModifica by remember { mutableStateOf<Muretto?>(null) }
    var creaNuovo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.fetchMuretti() }

    // Vista FORM (creazione o modifica)
    if (creaNuovo || murettoInModifica != null) {
        FormMuretto(
            esistente = murettoInModifica,
            onIndietro = { creaNuovo = false; murettoInModifica = null }
        )
        return
    }

    // Vista LISTA
    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) { Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = font) }
                Text("GESTIONE MURETTI", color = Tema.coloreTesto, fontSize = 26.sp, fontFamily = font, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            if (isAdmin) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(16.dp)).background(Tema.coloreSfondoCard).border(3.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp)).clickable { creaNuovo = true },
                    contentAlignment = Alignment.Center
                ) { Text("+ CREA NUOVO MURETTO", color = Tema.coloreTesto, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = font) }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(if (isAdmin) "MURETTI (${murettiVisibili.size}) — tocca per modificare" else "IL TUO MURETTO", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (murettiVisibili.isEmpty()) {
                Text(if (isAdmin) "Nessun muretto." else "Nessun muretto associato al tuo account.", color = Tema.coloreTestoSecondario)
            } else {
                murettiVisibili.forEach { m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).clickable { murettoInModifica = m }.padding(10.dp)
                    ) {
                        if (!m.immagineURL.isNullOrBlank()) AsyncImage(model = m.immagineURL, contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(m.name, color = Tema.coloreTesto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("MODIFICA >", color = Tema.colorePrincipale, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun FormMuretto(esistente: Muretto?, onIndietro: () -> Unit) {
    val vm = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val font = FontFamily(Font(R.font.komtit__))
    val inModifica = esistente != null

    var nome by remember { mutableStateOf(esistente?.name ?: "") }
    var descrizione by remember { mutableStateOf(esistente?.description ?: "") }
    var instagram by remember { mutableStateOf(esistente?.instagram ?: "") }
    var orari by remember { mutableStateOf(esistente?.meeting_schedule ?: "") }
    var location by remember { mutableStateOf(esistente?.location ?: "") }
    var lat by remember { mutableStateOf(esistente?.lat?.toString() ?: "") }
    var lng by remember { mutableStateOf(esistente?.lng?.toString() ?: "") }
    var scalaPin by remember { mutableStateOf((esistente?.scala_pin ?: 1.0f).toString()) }

    var coloreCornici by remember { mutableStateOf(hexToColor(esistente?.colore_cornici, Color(0xFFD32F2F))) }
    var coloreBottoni by remember { mutableStateOf(hexToColor(esistente?.colore_bottoni, Color(0xFFD32F2F))) }

    val immagini = remember { mutableStateMapOf<String, Uri>() }
    var targetCorrente by remember { mutableStateOf("") }
    val selettore = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && targetCorrente.isNotEmpty()) immagini[targetCorrente] = uri
    }

    var staSalvando by remember { mutableStateOf(false) }
    var messaggio by remember { mutableStateOf("") }

    val coloriInput = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
        focusedBorderColor = Tema.colorePrincipale, unfocusedBorderColor = Color.Gray,
        focusedLabelColor = Tema.colorePrincipale, unfocusedLabelColor = Color.LightGray, cursorColor = Tema.colorePrincipale
    )
    val palette = listOf(Color(0xFFD32F2F), Color(0xFF1E88E5), Color(0xFFFF9800), Color(0xFFFFD600), Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFB71C1C), Color(0xFF00BCD4), Color.White, Color.Black)

    Surface(modifier = Modifier.fillMaxSize().imePadding(), color = Tema.coloreSfondo) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                IconButton(onClick = onIndietro, modifier = Modifier.align(Alignment.CenterStart)) { Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = font) }
                Text(if (inModifica) "MODIFICA MURETTO" else "NUOVO MURETTO", color = Tema.coloreTesto, fontSize = 24.sp, fontFamily = font, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome (unico)") }, colors = coloriInput, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !inModifica)
            if (inModifica) Text("Il nome non è modificabile.", color = Tema.coloreTestoSecondario, fontSize = 11.sp)
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
            if (inModifica) Text("Lascia vuoto per mantenere l'immagine attuale.", color = Tema.coloreTestoSecondario, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val pick: (String) -> Unit = { key -> targetCorrente = key; selettore.launch("image/*") }
            SelettoreImmagine("Logo", immagini["logo"], esistente?.immagineURL, pick, "logo")
            SelettoreImmagine("Icona pin mappa", immagini["pin"], esistente?.pin_url, pick, "pin")
            SelettoreImmagine("Sfondo schermata iniziale", immagini["sfondo_iniziale"], esistente?.sfondo_iniziale_url, pick, "sfondo_iniziale")
            SelettoreImmagine("Sfondo generale", immagini["sfondo"], esistente?.sfondo_url, pick, "sfondo")
            SelettoreImmagine("Sfondo card Muretto classico", immagini["card_muretto"], esistente?.sfondo_card_muretto_url, pick, "card_muretto")
            SelettoreImmagine("Sfondo card 2 vs 2", immagini["card_2v2"], esistente?.sfondo_card_2v2_url, pick, "card_2v2")
            SelettoreImmagine("Sfondo card Contest", immagini["card_contest"], esistente?.sfondo_card_contest_url, pick, "card_contest")

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
                        val ok = if (inModifica) {
                            vm.aggiornaMuretto(
                                id = esistente!!.id,
                                nome = nome, descrizione = descrizione, instagram = instagram, orari = orari, location = location,
                                lat = lat.toDoubleOrNull(), lng = lng.toDoubleOrNull(), scalaPin = scalaPin.toFloatOrNull() ?: 1.0f,
                                coloreCornici = coloreCornici.toHex(), coloreBottoni = coloreBottoni.toHex(),
                                nuoveImmagini = bytes
                            )
                        } else {
                            vm.creaMuretto(
                                nome = nome, descrizione = descrizione, instagram = instagram, orari = orari, location = location,
                                lat = lat.toDoubleOrNull(), lng = lng.toDoubleOrNull(), scalaPin = scalaPin.toFloatOrNull() ?: 1.0f,
                                coloreCornici = coloreCornici.toHex(), coloreBottoni = coloreBottoni.toHex(),
                                immagini = bytes
                            )
                        }
                        staSalvando = false
                        if (ok) onIndietro() else messaggio = "Errore nel salvataggio."
                    }
                },
                enabled = !staSalvando,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { if (staSalvando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp)) else Text(if (inModifica) "SALVA MODIFICHE" else "CREA MURETTO", color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(60.dp))
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
private fun SelettoreImmagine(label: String, uri: Uri?, urlEsistente: String?, onPick: (String) -> Unit, key: String) {
    val haQualcosa = uri != null || !urlEsistente.isNullOrBlank()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(10.dp)).background(Tema.coloreSfondoCard).border(2.dp, if (uri != null) Color(0xFF4CAF50) else Color.Gray, RoundedCornerShape(10.dp)).clickable { onPick(key) },
            contentAlignment = Alignment.Center
        ) {
            when {
                uri != null -> AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                !urlEsistente.isNullOrBlank() -> AsyncImage(model = urlEsistente, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                else -> Text("+", color = Color.Gray, fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Tema.coloreTesto, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (uri != null) Text("nuova ✓", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        else if (haQualcosa) Text("attuale", color = Tema.coloreTestoSecondario, fontSize = 12.sp)
    }
}