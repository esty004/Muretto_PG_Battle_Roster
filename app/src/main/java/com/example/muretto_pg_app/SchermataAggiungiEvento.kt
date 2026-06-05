package com.example.muretto_pg_app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Calendar
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataAggiungiEvento(onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val MioFont = FontFamily(Font(R.font.komtit__))

    var titolo by remember { mutableStateOf("") }
    var indirizzoTesto by remember { mutableStateOf("") }
    var dataSelezionata by remember { mutableStateOf("") }
    var oraSelezionata by remember { mutableStateOf("") }
    var prezzo by remember { mutableStateOf("Gratis") }
    var tipo by remember { mutableStateOf("Battle 1v1") }
    var menuTipoAperto by remember { mutableStateOf(false) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var staCaricando by remember { mutableStateOf(false) }
    var messaggioEsito by remember { mutableStateOf("") }

    // NUOVI CAMPI
    var insta by remember { mutableStateOf("") }
    var maps by remember { mutableStateOf("") }
    var descrizione by remember { mutableStateOf("") }

    // MAPPA E RICERCA
    var pinMarker by remember { mutableStateOf<GeoPoint?>(null) }
    var mostraMappaIntera by remember { mutableStateOf(false) } // <--- Variabile per il FullScreen

    val selettoreImmagine = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> dataSelezionata = "$dayOfMonth/${month + 1}/$year" },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute -> oraSelezionata = String.format("%02d:%02d", hourOfDay, minute) },
        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // <--- IL FIX MAGICO DELLA TASTIERA!
        color = Tema.coloreSfondo
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFont)
                }
                Text("NUOVO EVENTO", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, modifier = Modifier.align(Alignment.Center))
            }

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Tema.coloreSfondoCard)
                    .border(4.dp, Tema.colorePrincipale, CircleShape)
                    .clickable { selettoreImmagine.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(model = imageUri, contentDescription = "Locandina", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(id = R.drawable.ic_music_note), contentDescription = null, tint = Tema.coloreTestoSecondario, modifier = Modifier.size(40.dp))
                        Text("Immagine Pin", color = Tema.coloreTestoSecondario, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- RIQUADRO PER APRIRE LA MAPPA ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Tema.coloreSfondoCard)
                    .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(12.dp))
                    .clickable { mostraMappaIntera = true },
                contentAlignment = Alignment.Center
            ) {
                if (pinMarker != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Tema.colorePrincipale, modifier = Modifier.size(30.dp))
                        Text("📍 Posizione salvata con successo!", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Tocca per modificare", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(30.dp))
                        Text("Tocca qui per aprire la mappa", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("e posizionare l'evento", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = titolo, onValueChange = { titolo = it },
                label = { Text("Titolo Evento", color = Tema.coloreTestoSecondario) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = indirizzoTesto, onValueChange = { indirizzoTesto = it },
                label = { Text("Nome della Location (Es. Gallery Caffè)", color = Tema.coloreTestoSecondario) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dataSelezionata, onValueChange = {}, readOnly = true,
                    label = { Text("Data", color = Tema.coloreTestoSecondario) },
                    modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { datePickerDialog.show() },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale, disabledTextColor = Tema.coloreTesto),
                    enabled = false
                )
                OutlinedTextField(
                    value = oraSelezionata, onValueChange = {}, readOnly = true,
                    label = { Text("Ora", color = Tema.coloreTestoSecondario) },
                    modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { timePickerDialog.show() },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale, disabledTextColor = Tema.coloreTesto),
                    enabled = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = menuTipoAperto, onExpandedChange = { menuTipoAperto = !menuTipoAperto }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = tipo, onValueChange = {}, readOnly = true, label = { Text("Tipo", color = Tema.coloreTestoSecondario) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuTipoAperto) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = menuTipoAperto, onDismissRequest = { menuTipoAperto = false }, modifier = Modifier.background(Tema.coloreSfondoCard)) {
                        listOf("Battle 1v1", "Battle 2v2", "Jam / Cypher", "Open Mic", "Evento Misto").forEach { opzione ->
                            DropdownMenuItem(text = { Text(opzione, color = Tema.coloreTesto) }, onClick = { tipo = opzione; menuTipoAperto = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = prezzo, onValueChange = { prezzo = it }, label = { Text("Prezzo", color = Tema.coloreTestoSecondario) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale), modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // NUOVI CAMPI: Insta, Maps, Descrizione
            OutlinedTextField(
                value = insta, onValueChange = { insta = it },
                label = { Text("Link Instagram (opzionale)", color = Tema.coloreTestoSecondario) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                singleLine = true
            )

            OutlinedTextField(
                value = maps, onValueChange = { maps = it },
                label = { Text("Link Google Maps (opzionale)", color = Tema.coloreTestoSecondario) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                singleLine = true
            )

            OutlinedTextField(
                value = descrizione, onValueChange = { descrizione = it },
                label = { Text("Descrizione", color = Tema.coloreTestoSecondario) },
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                maxLines = 5
            )

            if (messaggioEsito.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(messaggioEsito, color = if (messaggioEsito.contains("Errore")) Color.Red else Color.Green, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (titolo.isBlank() || indirizzoTesto.isBlank() || dataSelezionata.isBlank() || oraSelezionata.isBlank()) {
                        messaggioEsito = "Errore: Compila tutti i campi obbligatori!"
                        return@Button
                    }
                    if (pinMarker == null) {
                        messaggioEsito = "Errore: Tocca la mappa per posizionare il pin dell'evento!"
                        return@Button
                    }

                    staCaricando = true
                    messaggioEsito = ""

                    scope.launch {
                        val bytesImmagine = imageUri?.let { uri -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }

                        // INVIA EVENTO INCLUSI I NUOVI CAMPI
                        val successo = databaseViewModel.inserisciNuovoEvento(
                            titolo = titolo.trim(), locationNome = indirizzoTesto.trim(), lat = pinMarker!!.latitude, lng = pinMarker!!.longitude,
                            dataOra = "$dataSelezionata, $oraSelezionata", tipo = tipo, prezzo = prezzo.trim(), scalaPin = 1.0f, imageBytes = bytesImmagine,
                            insta = insta.trim(), maps = maps.trim(), descrizione = descrizione.trim()
                        )

                        staCaricando = false
                        if (successo) {
                            messaggioEsito = "Evento inviato in attesa di approvazione Admin!"
                            titolo = ""; indirizzoTesto = ""; dataSelezionata = ""; oraSelezionata = ""; imageUri = null; pinMarker = null; insta = ""; maps = ""; descrizione = ""
                        } else {
                            messaggioEsito = "Errore durante l'invio dell'evento."
                        }
                    }
                },
                enabled = !staCaricando,
                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth(0.9f).height(60.dp)
            ) {
                if (staCaricando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("INVIA EVENTO", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // --- DIALOG MAPPA A SCHERMO INTERO ---
    if (mostraMappaIntera) {
        var searchQueryLocale by remember { mutableStateOf("") }
        var staCercandoIndirizzo by remember { mutableStateOf(false) }
        var mapError by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { mostraMappaIntera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false) // Occupa tutto lo schermo
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

                // MAPPA ANDROIDVIEW A SCHERMO INTERO
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(6.0)
                            // Se c'è già un pin, la mappa si apre su quel pin
                            if (pinMarker != null) {
                                controller.setCenter(pinMarker)
                                controller.setZoom(12.0)
                            } else {
                                controller.setCenter(GeoPoint(42.5, 12.5)) // Centro Italia
                            }

                            val trueDarkModeMatrix = android.graphics.ColorMatrix(floatArrayOf(
                                0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                                0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                                -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                            ))
                            overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(trueDarkModeMatrix))

                            val receiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    if (p != null) {
                                        pinMarker = p
                                        mapError = ""
                                    }
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }
                            overlays.add(MapEventsOverlay(receiver))
                        }
                    },
                    update = { map ->
                        map.overlays.removeAll { it is Marker }
                        pinMarker?.let { pt ->
                            val marker = Marker(map)
                            marker.position = pt
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            val defaultBmp = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.pin_muretto_pg)
                            marker.icon = android.graphics.drawable.BitmapDrawable(context.resources, android.graphics.Bitmap.createScaledBitmap(defaultBmp, 100, 70, true))
                            map.overlays.add(marker)
                            map.controller.animateTo(pt)
                        }
                        map.invalidate()
                    }
                )

                // OVERLAY IN ALTO: BARRA DI RICERCA
                Column(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Tasto Indietro
                        IconButton(
                            onClick = { mostraMappaIntera = false },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Chiudi", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Barra di Ricerca
                        OutlinedTextField(
                            value = searchQueryLocale, onValueChange = { searchQueryLocale = it },
                            placeholder = { Text("Cerca città o via...", color = Color.Gray) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (searchQueryLocale.isNotBlank()) {
                                        staCercandoIndirizzo = true
                                        mapError = ""
                                        scope.launch {
                                            val coordinate = ottieniCoordinate(searchQueryLocale.trim())
                                            staCercandoIndirizzo = false
                                            if (coordinate != null) {
                                                pinMarker = GeoPoint(coordinate.first, coordinate.second)
                                            } else {
                                                mapError = "Posizione non trovata."
                                            }
                                        }
                                    }
                                }) {
                                    if (staCercandoIndirizzo) {
                                        CircularProgressIndicator(color = Tema.colorePrincipale, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Search, contentDescription = "Cerca", tint = Tema.colorePrincipale)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black.copy(alpha = 0.8f), unfocusedContainerColor = Color.Black.copy(alpha = 0.8f),
                                focusedBorderColor = Tema.colorePrincipale, unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (mapError.isNotEmpty()) {
                        Text(mapError, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, start = 56.dp).background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(4.dp))
                    }
                }

                // PULSANTE DI CONFERMA IN BASSO
                Button(
                    onClick = { mostraMappaIntera = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).fillMaxWidth(0.85f).height(55.dp)
                ) {
                    Text(if (pinMarker != null) "CONFERMA POSIZIONE" else "CHIUDI MAPPA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

suspend fun ottieniCoordinate(indirizzo: String): Pair<Double, Double>? {
    return withContext(Dispatchers.IO) {
        try {
            val encodedAddress = URLEncoder.encode(indirizzo, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encodedAddress&format=json&limit=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "FreestApp/1.0")
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    val firstResult = jsonArray.getJSONObject(0)
                    val lat = firstResult.getString("lat").toDouble()
                    val lon = firstResult.getString("lon").toDouble()
                    return@withContext Pair(lat, lon)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}