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

    // MAPPA E RICERCA
    var pinMarker by remember { mutableStateOf<GeoPoint?>(null) }
    var searchQuery by remember { mutableStateOf("") }

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

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
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

            // RICERCA E MAPPA INTERATTIVA
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                label = { Text("Cerca città o indirizzo...", color = Tema.coloreTestoSecondario) },
                trailingIcon = {
                    IconButton(onClick = {
                        if (searchQuery.isNotBlank()) {
                            staCaricando = true
                            scope.launch {
                                val coordinate = ottieniCoordinate(searchQuery.trim())
                                staCaricando = false
                                if (coordinate != null) {
                                    pinMarker = GeoPoint(coordinate.first, coordinate.second)
                                } else {
                                    messaggioEsito = "Indirizzo non trovato. Tocca la mappa per posizionare il pin manualmente."
                                }
                            }
                        }
                    }) { Icon(Icons.Default.Search, contentDescription = "Cerca", tint = Tema.colorePrincipale) }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Tocca la mappa per mettere il pin esattamente dove vuoi!", color = Tema.colorePrincipale, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // MAPPA ANDROIDVIEW
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, Tema.colorePrincipale, RoundedCornerShape(12.dp))) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(6.0)
                            controller.setCenter(GeoPoint(42.5, 12.5)) // Centro Italia

                            val trueDarkModeMatrix = android.graphics.ColorMatrix(floatArrayOf(
                                0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                                0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                                -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                            ))
                            overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(trueDarkModeMatrix))

                            val receiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    if (p != null) pinMarker = p
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
                            val defaultBmp = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo_muretto)
                            marker.icon = android.graphics.drawable.BitmapDrawable(context.resources, android.graphics.Bitmap.createScaledBitmap(defaultBmp, 100, 70, true))
                            map.overlays.add(marker)
                            map.controller.animateTo(pt)
                        }
                        map.invalidate()
                    }
                )
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

                        // INVIA EVENTO CON SCALA FISSA A 1.0f
                        val successo = DatabaseMcs.inserisciNuovoEvento(
                            titolo = titolo.trim(), locationNome = indirizzoTesto.trim(), lat = pinMarker!!.latitude, lng = pinMarker!!.longitude,
                            dataOra = "$dataSelezionata, $oraSelezionata", tipo = tipo, prezzo = prezzo.trim(), scalaPin = 1.0f, imageBytes = bytesImmagine
                        )

                        staCaricando = false
                        if (successo) {
                            messaggioEsito = "Evento inviato in attesa di approvazione Admin!"
                            titolo = ""; indirizzoTesto = ""; dataSelezionata = ""; oraSelezionata = ""; imageUri = null; pinMarker = null
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