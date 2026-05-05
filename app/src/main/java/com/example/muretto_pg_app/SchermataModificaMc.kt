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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun SchermataModificaMc(mcId: String, onTornaIndietro: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val MioFont = FontFamily(Font(R.font.komtit__))

    val mcOriginale = DatabaseMcs.tuttiMcsCloud.find { it.id == mcId }
    if (mcOriginale == null) {
        onTornaIndietro()
        return
    }

    var nome by remember { mutableStateOf(mcOriginale.nome) }
    var modalitaModificaNome by remember { mutableStateOf(false) }

    var nuovaImageUri by remember { mutableStateOf<Uri?>(null) }
    var staCaricando by remember { mutableStateOf(false) }
    var mostraDialogElimina by remember { mutableStateOf(false) }

    val selettoreImmagine = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        nuovaImageUri = uri
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 40.dp)) {
                    Text("PROFILO MC", color = Tema.coloreTesto, fontSize = 28.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                }

                // GESTIONE FOTO
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(3.dp, Tema.colorePrincipale, RoundedCornerShape(16.dp))
                        .background(Tema.coloreSfondoCard)
                        .clickable { selettoreImmagine.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = nuovaImageUri ?: (if (mcOriginale.immagineUrl.isNotBlank()) mcOriginale.immagineUrl else R.drawable.no_pic),
                        contentDescription = "Foto MC",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.7f), CircleShape).padding(6.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Cambia Foto", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // GESTIONE NOME
                if (modalitaModificaNome) {
                    OutlinedTextField(
                        value = nome, onValueChange = { nome = it },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Tema.coloreTesto, unfocusedTextColor = Tema.coloreTesto, focusedBorderColor = Tema.colorePrincipale),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { modalitaModificaNome = false }) { Icon(Icons.Default.Check, contentDescription = "Ok", tint = Color.Green) }
                        }
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nome.uppercase(), color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = { modalitaModificaNome = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifica Nome", tint = Tema.coloreTestoSecondario)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))

                // TASTO ELIMINA
                Button(
                    onClick = { mostraDialogElimina = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ELIMINA MC", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // TASTO INDIETRO
            FloatingActionButton(onClick = onTornaIndietro, containerColor = Tema.colorePrincipale, contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 32.dp)) {
                Text("<", fontSize = 30.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-2).dp))
            }

            // TASTO SALVA (SPUNTA VERDE)
            FloatingActionButton(
                onClick = {
                    staCaricando = true
                    scope.launch {
                        val bytesImmagine = nuovaImageUri?.let { uri -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                        DatabaseMcs.aggiornaMc(mcId, nome.trim(), bytesImmagine)
                        DatabaseMcs.fetchMcsDalCloud(mcOriginale.muretto) // Ricarica la lista per aggiornarla
                        staCaricando = false
                        onTornaIndietro()
                    }
                },
                containerColor = Color.Green, contentColor = Color.Black, shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 32.dp)
            ) {
                if (staCaricando) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else Icon(Icons.Default.Check, contentDescription = "Salva", modifier = Modifier.size(34.dp))
            }

            if (mostraDialogElimina) {
                AlertDialog(
                    onDismissRequest = { mostraDialogElimina = false },
                    containerColor = Tema.coloreSfondoCard,
                    title = { Text("ELIMINA FREESTYLER", color = Color.Red, fontWeight = FontWeight.Bold) },
                    text = { Text("Sei sicuro di voler eliminare definitivamente $nome?", color = Tema.coloreTesto) },
                    confirmButton = {
                        Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Red), onClick = {
                            mostraDialogElimina = false
                            staCaricando = true
                            scope.launch {
                                DatabaseMcs.eliminaMc(mcId)
                                DatabaseMcs.fetchMcsDalCloud(mcOriginale.muretto)
                                staCaricando = false
                                onTornaIndietro()
                            }
                        }) { Text("ELIMINA", color = Color.White) }
                    },
                    dismissButton = { TextButton(onClick = { mostraDialogElimina = false }) { Text("ANNULLA", color = Tema.coloreTestoSecondario) } }
                )
            }
        }
    }
}