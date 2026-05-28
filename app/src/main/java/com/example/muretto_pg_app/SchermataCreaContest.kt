package com.example.muretto_pg_app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ArrowBack
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
import org.json.JSONArray
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataCreaContest(onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val MioFont = FontFamily(Font(R.font.komtit__))

    // GESTIONE FASI
    var stepAttuale by remember { mutableIntStateOf(1) }
    var staSalvandoTutto by remember { mutableStateOf(false) }

    // --- VARIABILI STEP 1 (Dati Mappa e Trasferta) ---
    var titolo by remember { mutableStateOf("") }
    var locationTesto by remember { mutableStateOf("") }
    var dataSelezionata by remember { mutableStateOf("") }
    var oraSelezionata by remember { mutableStateOf("") }
    var prezzo by remember { mutableStateOf("Gratis") } // NUOVO CAMPO
    var insta by remember { mutableStateOf("") } // NUOVO CAMPO
    var maps by remember { mutableStateOf("") } // NUOVO CAMPO
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var pinMarker by remember { mutableStateOf<GeoPoint?>(null) }
    var mostraMappaIntera by remember { mutableStateOf(false) }
    var errorMessageStep1 by remember { mutableStateOf("") }

    val selettoreImmagine = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> imageUri = uri }
    val calendar = Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(context, { _, y, m, d -> dataSelezionata = "$d/${m + 1}/$y" }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    val timePickerDialog = android.app.TimePickerDialog(context, { _, h, m -> oraSelezionata = String.format("%02d:%02d", h, m) }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)

    // --- VARIABILI STEP 2 (Modalità e Stile Manuale) ---
    var modalitaSelezionata by remember { mutableStateOf("1 VS 1") }
    val opzioniModalita = listOf("1 VS 1", "2 VS 2", "SQUADRE")

    var stileSelezionato by remember { mutableStateOf("DEFAULT") }
    val opzioniStile = listOf("DEFAULT", "CUSTOM", "DELEGA")

    // Variabili per lo Stile Custom
    var sfondoCustomUri by remember { mutableStateOf<Uri?>(null) }
    var sfondoCardCustomUri by remember { mutableStateOf<Uri?>(null) }
    var coloreCornici by remember { mutableStateOf(Color.White) }
    var coloreBottoni by remember { mutableStateOf(Tema.colorePrincipale) }

    val selettoreSfondoCustom = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> sfondoCustomUri = uri }
    val selettoreSfondoCardCustom = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> sfondoCardCustomUri = uri }

    val paletteColori = listOf(
        Color.White, Color.Black, Color(0xFFD32F2F), // Rosso Muretto
        Color(0xFF1E88E5), // Blu Barre Faul
        Color(0xFFFF9800), // Arancione Ateneo
        Color(0xFF4CAF50), // Verde
        Color(0xFF9C27B0), // Viola
        Color(0xFFFFEB3B), // Giallo
        Color(0xFF00BCD4)  // Ciano
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // HEADER E BARRA DI PROGRESSO
            Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 10.dp)) {
                IconButton(onClick = { if (stepAttuale == 2) stepAttuale = 1 else onTornaIndietro() }, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("<", color = Tema.coloreTesto, fontSize = 40.sp, fontFamily = MioFont)
                }
                Text("CREA CONTEST", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, modifier = Modifier.align(Alignment.Center))
            }

            LinearProgressIndicator(
                progress = if (stepAttuale == 1) 0.5f else 1.0f, color = Tema.colorePrincipale, trackColor = Color.DarkGray,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(targetState = stepAttuale, label = "StepAnimation") { targetStep ->
                when (targetStep) {
                    // ==========================================
                    // STEP 1: DATI BASE E MAPPA FULLSCREEN
                    // ==========================================
                    1 -> {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PASSO 1: DATI PUBBLICI (TRASFERTE)", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(modifier = Modifier.size(150.dp).clip(CircleShape).background(Tema.coloreSfondoCard).border(4.dp, Tema.colorePrincipale, CircleShape).clickable { selettoreImmagine.launch("image/*") }, contentAlignment = Alignment.Center) {
                                if (imageUri != null) AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Text("Immagine Locandina", color = Tema.coloreTestoSecondario, textAlign = TextAlign.Center, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // PULSANTE PER APRIRE LA MAPPA A SCHERMO INTERO
                            Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).border(2.dp, Tema.colorePrincipale, RoundedCornerShape(12.dp)).clickable { mostraMappaIntera = true }, contentAlignment = Alignment.Center) {
                                if (pinMarker != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Tema.colorePrincipale)
                                        Text("📍 Posizione salvata!", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray)
                                        Text("Tocca per mettere il Pin sulla Mappa", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(value = titolo, onValueChange = { titolo = it }, label = { Text("Nome Contest", color = Tema.coloreTestoSecondario) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = locationTesto, onValueChange = { locationTesto = it }, label = { Text("Location (Es. Gallery Caffè)", color = Tema.coloreTestoSecondario) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto))
                            Spacer(modifier = Modifier.height(12.dp))

                            // NUOVO CAMPO: PREZZO
                            OutlinedTextField(value = prezzo, onValueChange = { prezzo = it }, label = { Text("Prezzo", color = Tema.coloreTestoSecondario) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(value = dataSelezionata, onValueChange = {}, readOnly = true, label = { Text("Data", color = Tema.coloreTestoSecondario) }, modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { datePickerDialog.show() }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Tema.coloreTesto), enabled = false)
                                OutlinedTextField(value = oraSelezionata, onValueChange = {}, readOnly = true, label = { Text("Ora", color = Tema.coloreTestoSecondario) }, modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { timePickerDialog.show() }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Tema.coloreTesto), enabled = false)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // NUOVI CAMPI: INSTA E MAPS
                            OutlinedTextField(value = insta, onValueChange = { insta = it }, label = { Text("Link Instagram (opzionale)", color = Tema.coloreTestoSecondario) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = maps, onValueChange = { maps = it }, label = { Text("Link Google Maps (opzionale)", color = Tema.coloreTestoSecondario) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto))


                            if (errorMessageStep1.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(errorMessageStep1, color = Color.Red, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                            Button(
                                onClick = {
                                    if (titolo.isBlank() || locationTesto.isBlank() || dataSelezionata.isBlank() || pinMarker == null) {
                                        errorMessageStep1 = "Compila i campi principali e posiziona il Pin!"
                                    } else {
                                        errorMessageStep1 = ""
                                        stepAttuale = 2
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                                modifier = Modifier.fillMaxWidth(0.9f).height(60.dp), shape = RoundedCornerShape(12.dp)
                            ) { Text("AVANTI: MODALITÀ E STILE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    // ==========================================
                    // STEP 2: MODALITÀ E STILE MANUALE
                    // ==========================================
                    2 -> {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("PASSO 2: REGOLE E DESIGN", color = Tema.colorePrincipale, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(24.dp))

                            // 1. Scelta Modalità
                            Text("Modalità Contest:", color = Tema.coloreTesto, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                opzioniModalita.forEach { modalita ->
                                    val isSelezionata = modalitaSelezionata == modalita
                                    Box(
                                        modifier = Modifier.weight(1f).padding(4.dp).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelezionata) Tema.colorePrincipale else Tema.coloreSfondoCard).border(2.dp, if (isSelezionata) Color.White else Color.Transparent, RoundedCornerShape(12.dp)).clickable { modalitaSelezionata = modalita },
                                        contentAlignment = Alignment.Center
                                    ) { Text(modalita, color = if (isSelezionata) Color.White else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // 2. Scelta Stile Generale
                            Text("Stile Estetico:", color = Tema.coloreTesto, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                opzioniStile.forEach { stile ->
                                    val isSelezionato = stileSelezionato == stile
                                    Box(
                                        modifier = Modifier.weight(1f).padding(4.dp).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelezionato) Tema.colorePrincipale else Tema.coloreSfondoCard).border(2.dp, if (isSelezionato) Color.White else Color.Transparent, RoundedCornerShape(12.dp)).clickable { stileSelezionato = stile },
                                        contentAlignment = Alignment.Center
                                    ) { Text(stile, color = if (isSelezionato) Color.White else Tema.coloreTestoSecondario, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                                }
                            }

                            // 3. PANNELLO STILE CUSTOM
                            AnimatedVisibility(visible = stileSelezionato == "CUSTOM", enter = expandVertically(), exit = shrinkVertically()) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray.copy(alpha = 0.3f)).border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp)).padding(16.dp)) {

                                    Text("IMMAGINI DI SFONDO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Sfondo Generale
                                        Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).clickable { selettoreSfondoCustom.launch("image/*") }, contentAlignment = Alignment.Center) {
                                            if (sfondoCustomUri != null) AsyncImage(model = sfondoCustomUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            else Text("Sfondo App\n(Tocca)", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 12.sp)
                                        }
                                        // Sfondo Card
                                        Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(12.dp)).background(Tema.coloreSfondoCard).clickable { selettoreSfondoCardCustom.launch("image/*") }, contentAlignment = Alignment.Center) {
                                            if (sfondoCardCustomUri != null) AsyncImage(model = sfondoCardCustomUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            else Text("Sfondo Card\n(Tocca)", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Palette Cornici
                                    Text("COLORE CORNICI (Round e MCs)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        paletteColori.forEach { colore ->
                                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(colore).border(3.dp, if (coloreCornici == colore) Color.White else Color.Transparent, CircleShape).clickable { coloreCornici = colore }, contentAlignment = Alignment.Center) {
                                                if (coloreCornici == colore) Icon(Icons.Default.Check, contentDescription = null, tint = if (colore == Color.White) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Palette Bottoni
                                    Text("COLORE BOTTONI E PLAYER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        paletteColori.forEach { colore ->
                                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(colore).border(3.dp, if (coloreBottoni == colore) Color.White else Color.Transparent, CircleShape).clickable { coloreBottoni = colore }, contentAlignment = Alignment.Center) {
                                                if (coloreBottoni == colore) Icon(Icons.Default.Check, contentDescription = null, tint = if (colore == Color.White) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // --- AREA ANTEPRIMA VISIVA LIVE ---
                            Text("ANTEPRIMA DELLO STILE:", color = Tema.coloreTesto, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (stileSelezionato == "CUSTOM" && sfondoCustomUri != null) Color.Transparent else Tema.coloreSfondo)
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Se c'è uno sfondo d'app custom caricato, mostralo sotto nell'anteprima
                                if (stileSelezionato == "CUSTOM" && sfondoCustomUri != null) {
                                    AsyncImage(model = sfondoCustomUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else if (stileSelezionato == "DEFAULT") {
                                    Image(painter = painterResource(id = Tema.sfondoGenerale), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }

                                // Finestra scura di simulazione
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

                                // Card del Round simulata
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(75.dp)
                                        .border(
                                            width = 3.dp,
                                            color = if (stileSelezionato == "CUSTOM") coloreCornici else Tema.colorePrincipale,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (stileSelezionato == "CUSTOM") Tema.coloreSfondoCard else Tema.coloreSfondoCard
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (stileSelezionato == "CUSTOM" && sfondoCardCustomUri != null) {
                                            AsyncImage(model = sfondoCardCustomUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(modalitaSelezionata, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = MioFont, fontSize = 18.sp)
                                            // Bottone simulato
                                            Box(
                                                modifier = Modifier
                                                    .size(70.dp, 30.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (stileSelezionato == "CUSTOM") coloreBottoni else Tema.colorePrincipale),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("VOTA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(30.dp))

                            // SALVATAGGIO FINALE DI TUTTO NEL DATABASE
                            Button(
                                onClick = {
                                    staSalvandoTutto = true
                                    scope.launch {
                                        val bytesImmagine = imageUri?.let { uri -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }

                                        // QUI SALVIAMO L'EVENTO NEL DB
                                        val successo = databaseViewModel.inserisciNuovoEvento(
                                            titolo = titolo, locationNome = locationTesto, lat = pinMarker!!.latitude, lng = pinMarker!!.longitude,
                                            dataOra = "$dataSelezionata, $oraSelezionata", tipo = modalitaSelezionata, prezzo = prezzo, scalaPin = 1.0f, imageBytes = bytesImmagine,
                                            insta = insta, maps = maps, descrizione = "Contest ufficiale"
                                        )

                                        staSalvandoTutto = false
                                        if(successo) onTornaIndietro()
                                    }
                                },
                                enabled = !staSalvandoTutto,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(12.dp)
                            ) {
                                if(staSalvandoTutto) CircularProgressIndicator(color = Color.White)
                                else Text("PUBBLICA CONTEST", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = MioFont)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG MAPPA FULLSCREEN (Per lo Step 1) ---
    if (mostraMappaIntera) {
        var searchQueryLocale by remember { mutableStateOf("") }
        var staCercandoIndirizzo by remember { mutableStateOf(false) }
        var mapError by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { mostraMappaIntera = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(6.0)
                            if (pinMarker != null) { controller.setCenter(pinMarker); controller.setZoom(12.0) }
                            else { controller.setCenter(GeoPoint(42.5, 12.5)) }

                            val trueDarkModeMatrix = android.graphics.ColorMatrix(floatArrayOf(
                                0.0f, 0.0f, -1.0f, 0.0f, 255.0f, 0.0f, -1.0f, 0.0f, 0.0f, 255.0f, -1.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                            ))
                            overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(trueDarkModeMatrix))

                            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean { if (p != null) { pinMarker = p; mapError = "" }; return true }
                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }))
                        }
                    },
                    update = { map ->
                        map.overlays.removeAll { it is Marker }
                        pinMarker?.let { pt ->
                            val marker = Marker(map)
                            marker.position = pt; marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.icon = android.graphics.drawable.BitmapDrawable(context.resources, android.graphics.Bitmap.createScaledBitmap(android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo_muretto), 100, 70, true))
                            map.overlays.add(marker)
                            map.controller.animateTo(pt)
                        }
                        map.invalidate()
                    }
                )

                Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { mostraMappaIntera = false }, modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)) { Icon(Icons.Default.ArrowBack, contentDescription = "Chiudi", tint = Color.White) }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQueryLocale, onValueChange = { searchQueryLocale = it }, placeholder = { Text("Cerca città o via...", color = Color.Gray) }, singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (searchQueryLocale.isNotBlank()) {
                                        staCercandoIndirizzo = true; mapError = ""
                                        scope.launch {
                                            val coordinate = ottieniCoordinate(searchQueryLocale.trim())
                                            staCercandoIndirizzo = false
                                            if (coordinate != null) pinMarker = GeoPoint(coordinate.first, coordinate.second) else mapError = "Posizione non trovata."
                                        }
                                    }
                                }) { if (staCercandoIndirizzo) CircularProgressIndicator(color = Tema.colorePrincipale, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Search, contentDescription = "Cerca", tint = Tema.colorePrincipale) }
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Black.copy(alpha = 0.8f), unfocusedContainerColor = Color.Black.copy(alpha = 0.8f), focusedBorderColor = Tema.colorePrincipale, unfocusedBorderColor = Color.Transparent), modifier = Modifier.weight(1f)
                        )
                    }
                    if (mapError.isNotEmpty()) Text(mapError, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, start = 56.dp).background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(4.dp))
                }

                Button(
                    onClick = { mostraMappaIntera = false }, colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale), shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).fillMaxWidth(0.85f).height(55.dp)
                ) { Text(if (pinMarker != null) "CONFERMA POSIZIONE" else "CHIUDI MAPPA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}