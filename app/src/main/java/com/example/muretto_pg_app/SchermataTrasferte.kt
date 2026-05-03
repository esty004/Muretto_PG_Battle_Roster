package com.example.muretto_pg_app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class FreestyleEvent(
    val id: String,
    val title: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val startTime: String,
    val eventType: String,
    val price: String,
    val immagineRes: Int
)

val mockEvents = listOf(
    FreestyleEvent("1", "Muretto PG - Battle 1v1", "Perugia, Loggette del Duomo", 43.112056, 12.388439, "Ogni Venerdì, 22:00", "Battle 1v1", "Gratis", R.drawable.sfondo_schermata_iniziale),
    FreestyleEvent("2", "Tritolo Battle", "Roma, Parco degli Acquedotti", 41.847, 12.561, "15 Maggio, 16:00", "Battle 1v1", "5€", R.drawable.muretto_classico),
    FreestyleEvent("3", "Mic Scrauso", "Milano, Colonne di San Lorenzo", 45.458, 9.182, "20 Giugno, 21:00", "Jam / Open Mic", "Gratis", R.drawable.evento),
    FreestyleEvent("4", "Barre Faul - Street Jam", "Viterbo, Prato Giardino", 42.416669, 12.100123, "2 Settembre, 17:00", "Jam", "Gratis", R.drawable.sfondo_schermata_iniziale_barre_faul)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataTrasferte(onTornaIndietro: () -> Unit) {
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState()
    var selectedEvent by remember { mutableStateOf<FreestyleEvent?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    Configuration.getInstance().userAgentValue = context.packageName

    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                Text("<", fontSize = 30.sp, fontFamily = Tema.fontKomtit, fontWeight = FontWeight.Bold)
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

                        val trueDarkModeMatrix = ColorMatrix(floatArrayOf(
                            0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                            0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                            -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                            0.0f,  0.0f,  0.0f, 1.0f, 0.0f
                        ))
                        overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(trueDarkModeMatrix))

                        // Creazione dei Pin Personalizzati "a nuvoletta" con immagine dell'evento
                        mockEvents.forEach { event ->
                            val markerIcon = creaPinConImmagine(context, event.immagineRes)
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(event.latitude, event.longitude)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = event.title

                            marker.icon = BitmapDrawable(context.resources, markerIcon)

                            marker.setOnMarkerClickListener { _, _ ->
                                controller.animateTo(marker.position)
                                selectedEvent = event
                                showBottomSheet = true
                                true
                            }
                            overlays.add(marker)
                        }
                    }
                }
            )

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
                            text = selectedEvent!!.title.uppercase(),
                            fontSize = 26.sp,
                            fontFamily = Tema.fontJackboa,
                            fontWeight = FontWeight.Bold,
                            color = Tema.colorePrincipale
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        AsyncImage(
                            model = selectedEvent!!.immagineRes,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        InfoRow(icon = Icons.Default.LocationOn, text = selectedEvent!!.locationName)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(icon = Icons.Default.LocationOn, text = selectedEvent!!.startTime)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(icon = Icons.Default.LocationOn, text = selectedEvent!!.eventType)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "PREZZO: ${selectedEvent!!.price}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Tema.coloreTestoSecondario
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Navigazione verso ${selectedEvent!!.locationName}...", Toast.LENGTH_SHORT).show()
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

/**
 * Funzione per creare programmaticamente un pin a "nuvoletta" con l'immagine dell'evento
 */
fun creaPinConImmagine(context: android.content.Context, immagineRes: Int): Bitmap {
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

    val eventBitmap = BitmapFactory.decodeResource(context.resources, immagineRes)
    if (eventBitmap != null) {
        val innerSize = (size - (borderSize * 2) - 8)
        val scaledBitmap = Bitmap.createScaledBitmap(eventBitmap, innerSize, innerSize, true)

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