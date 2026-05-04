package com.example.muretto_pg_app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataTrasferte(onTornaIndietro: () -> Unit) {
    val context = LocalContext.current
    val MioFont = FontFamily(Font(R.font.komtit__))

    val sheetState = rememberModalBottomSheetState()
    var selectedEvent by remember { mutableStateOf<Evento?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Configuration.getInstance().userAgentValue = context.packageName
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    // Quando si apre la schermata, scarica gli eventi veri dal Cloud
    LaunchedEffect(Unit) {
        DatabaseMcs.fetchEventiApprovati()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onTornaIndietro() },
                containerColor = Tema.colorePrincipale,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text("<", fontSize = 30.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold)
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        minZoomLevel = 6.0
                        maxZoomLevel = 19.0
                        controller.setZoom(7.5)
                        controller.setCenter(GeoPoint(42.5, 12.5))
                        @Suppress("DEPRECATION")
                        setBuiltInZoomControls(false)

                        val trueDarkModeMatrix = android.graphics.ColorMatrix(floatArrayOf(
                            0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                            0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                            -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                            0.0f,  0.0f,  0.0f, 1.0f, 0.0f
                        ))
                        overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(trueDarkModeMatrix))
                    }
                },
                update = { map ->
                    // Aggiorna i pin sulla mappa
                    map.overlays.removeAll { it is Marker }

                    DatabaseMcs.eventiApprovati.forEach { event ->
                        val marker = Marker(map)
                        marker.position = GeoPoint(event.lat, event.lng)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = event.titolo

                        // 1. Imposta subito l'icona generica come "segnaposto" in attesa del download
                        val defaultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.evento)
                        marker.icon = BitmapDrawable(context.resources, creaPinConBitmap(defaultBitmap))

                        marker.setOnMarkerClickListener { _, _ ->
                            map.controller.animateTo(marker.position)
                            selectedEvent = event
                            showBottomSheet = true
                            true
                        }
                        map.overlays.add(marker)

                        // 2. Scarica la locandina reale da Supabase usando Coil
                        if (!event.immagine_url.isNullOrBlank()) {
                            scope.launch {
                                try {
                                    val request = ImageRequest.Builder(context)
                                        .data(event.immagine_url)
                                        .allowHardware(false) // FONDAMENTALE: Osmdroid non supporta bitmap hardware sul suo Canvas
                                        .build()

                                    val result = context.imageLoader.execute(request)
                                    val loadedBitmap = (result.drawable as? BitmapDrawable)?.bitmap

                                    if (loadedBitmap != null) {
                                        withContext(Dispatchers.Main) {
                                            // Sostituisci il segnaposto con la locandina vera
                                            marker.icon = BitmapDrawable(context.resources, creaPinConBitmap(loadedBitmap))
                                            map.invalidate() // Ridisegna la mappa
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    map.invalidate()
                }
            )

            // BOTTOM SHEET (Dettagli Evento)
            if (showBottomSheet && selectedEvent != null) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = Tema.coloreSfondoCard,
                    contentColor = Tema.coloreTesto,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Tema.colorePrincipale) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = selectedEvent!!.titolo.uppercase(),
                            fontSize = 26.sp,
                            fontFamily = FontFamily(Font(R.font.jackboa)),
                            fontWeight = FontWeight.Bold,
                            color = Tema.colorePrincipale,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Locandina nel dettaglio in basso
                        if (selectedEvent!!.immagine_url != null) {
                            AsyncImage(
                                model = selectedEvent!!.immagine_url,
                                contentDescription = "Locandina Evento",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Nessuna Locandina", color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        InfoRow(icon = Icons.Default.LocationOn, text = selectedEvent!!.location_nome)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(icon = Icons.Default.Info, text = selectedEvent!!.data_ora)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(icon = Icons.Default.Info, text = selectedEvent!!.tipo)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "PREZZO: ${selectedEvent!!.prezzo}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Tema.coloreTestoSecondario
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Navigazione verso ${selectedEvent!!.location_nome}...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale)
                        ) {
                            Icon(painterResource(id = R.drawable.ic_music_note), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("NAVIGA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Nuova funzione per prendere direttamente un Bitmap e ritagliarlo a cerchio
fun creaPinConBitmap(eventBitmap: Bitmap?): Bitmap {
    val size = 140
    val bitmap = Bitmap.createBitmap(size, size + 25, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    val path = android.graphics.Path()
    path.moveTo(size * 0.25f, size * 0.75f)
    path.lineTo(size / 2f, size + 20f)
    path.lineTo(size * 0.75f, size * 0.75f)
    path.close()
    canvas.drawPath(path, paint)

    paint.color = android.graphics.Color.RED
    val borderSize = 6
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - borderSize, paint)

    if (eventBitmap != null) {
        val innerSize = (size - (borderSize * 2) - 8)

        // Ritaglia l'immagine al centro per renderla quadrata ed evitare che si deformi
        val dimension = min(eventBitmap.width, eventBitmap.height)
        val cropped = Bitmap.createBitmap(
            eventBitmap,
            (eventBitmap.width - dimension) / 2,
            (eventBitmap.height - dimension) / 2,
            dimension,
            dimension
        )
        val scaledBitmap = Bitmap.createScaledBitmap(cropped, innerSize, innerSize, true)

        val output = Bitmap.createBitmap(innerSize, innerSize, Bitmap.Config.ARGB_8888)
        val circleCanvas = Canvas(output)
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, innerSize, innerSize)

        circleCanvas.drawCircle(innerSize / 2f, innerSize / 2f, innerSize / 2f, circlePaint)
        circlePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        circleCanvas.drawBitmap(scaledBitmap, rect, rect, circlePaint)

        canvas.drawBitmap(output, (size - innerSize) / 2f, (size - innerSize) / 2f, null)
    }
    return bitmap
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = Tema.coloreTestoSecondario, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Tema.coloreTesto, fontSize = 16.sp)
    }
}