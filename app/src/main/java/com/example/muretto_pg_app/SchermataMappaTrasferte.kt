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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataMappaTrasferte(onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val italyBounds = BoundingBox(47.1, 18.3, 35.5, 6.6)
    val centroMappa = GeoPoint(42.764, 12.244)
    val MioFont = FontFamily(Font(R.font.komtit__))

    val eventi = databaseViewModel.eventiApprovati
    var eventoSelezionato by remember { mutableStateOf<Evento?>(null) }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val mapView = MapView(ctx).apply {
                        minZoomLevel = 6.5
                        controller.setZoom(8.0)
                        controller.setCenter(centroMappa)
                        setScrollableAreaLimitDouble(italyBounds)
                        setMultiTouchControls(true)

                        val trueDarkModeMatrix = android.graphics.ColorMatrix(floatArrayOf(
                            0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                            0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                            -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                        ))
                        overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(trueDarkModeMatrix))
                    }

                    eventi.forEach { evento ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(evento.lat, evento.lng)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                            val defaultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.evento)
                            icon = BitmapDrawable(context.resources, creaPinConBitmap(defaultBitmap, evento.scala_pin))

                            setOnMarkerClickListener { _, _ ->
                                mapView.controller.animateTo(position)
                                eventoSelezionato = evento
                                showBottomSheet = true
                                true
                            }
                        }
                        mapView.overlays.add(marker)

                        if (!evento.immagine_url.isNullOrBlank()) {
                            scope.launch {
                                try {
                                    val request = ImageRequest.Builder(context)
                                        .data(evento.immagine_url)
                                        .allowHardware(false)
                                        .build()

                                    val result = context.imageLoader.execute(request)
                                    val loadedBitmap = (result.drawable as? BitmapDrawable)?.bitmap

                                    if (loadedBitmap != null) {
                                        withContext(Dispatchers.Main) {
                                            marker.icon = BitmapDrawable(context.resources, creaPinConBitmap(loadedBitmap, evento.scala_pin))
                                            mapView.invalidate()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    mapView
                }
            )

            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                Text("MAPPA TRASFERTE", color = Color.White, fontSize = 28.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            FloatingActionButton(
                onClick = onTornaIndietro, containerColor = Tema.colorePrincipale, contentColor = Color.White, shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 32.dp)
            ) {
                Text("<", fontSize = 30.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp))
            }

            if (showBottomSheet && eventoSelezionato != null) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false }, sheetState = sheetState, containerColor = Tema.coloreSfondoCard, contentColor = Tema.coloreTesto,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Tema.colorePrincipale) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = eventoSelezionato!!.titolo.uppercase(), fontSize = 26.sp, fontFamily = FontFamily(Font(R.font.jackboa)), fontWeight = FontWeight.Bold, color = Tema.colorePrincipale, textAlign = TextAlign.Center)

                        Spacer(modifier = Modifier.height(16.dp))

                        if (eventoSelezionato!!.immagine_url != null) {
                            AsyncImage(model = eventoSelezionato!!.immagine_url, contentDescription = "Locandina Evento", modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).border(2.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray), contentAlignment = Alignment.Center) { Text("Nessuna Locandina", color = Color.Gray) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        InfoRow(icon = Icons.Default.LocationOn, text = eventoSelezionato!!.location_nome)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(icon = Icons.Default.Info, text = eventoSelezionato!!.data_ora.replace("T", " "))
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(icon = Icons.Default.Info, text = eventoSelezionato!!.tipo)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "PREZZO: ${eventoSelezionato!!.prezzo}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Tema.coloreTestoSecondario)

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { Toast.makeText(context, "Navigazione verso ${eventoSelezionato!!.location_nome}...", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale)
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

fun creaPinConBitmap(eventBitmap: Bitmap?, scaleFactor: Float = 1f): Bitmap {
    val baseSize = 140
    val size = (baseSize * scaleFactor).toInt().coerceAtLeast(50)
    val bottomPoint = size + (20 * scaleFactor).toInt()
    val fullHeight = size + (25 * scaleFactor).toInt()

    val bitmap = Bitmap.createBitmap(size, fullHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    val path = android.graphics.Path()
    path.moveTo(size * 0.25f, size * 0.75f)
    path.lineTo(size / 2f, bottomPoint.toFloat())
    path.lineTo(size * 0.75f, size * 0.75f)
    path.close()
    canvas.drawPath(path, paint)

    paint.color = android.graphics.Color.RED
    val borderSize = (6 * scaleFactor).toInt()
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - borderSize, paint)

    if (eventBitmap != null) {
        val innerSize = (size - (borderSize * 2) - (8 * scaleFactor).toInt()).coerceAtLeast(10)

        val dimension = min(eventBitmap.width, eventBitmap.height)
        val cropped = Bitmap.createBitmap(eventBitmap, (eventBitmap.width - dimension) / 2, (eventBitmap.height - dimension) / 2, dimension, dimension)
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = Tema.coloreTestoSecondario, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Tema.coloreTesto, fontSize = 16.sp)
    }
}