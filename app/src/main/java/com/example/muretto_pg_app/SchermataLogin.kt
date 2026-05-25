package com.example.muretto_pg_app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun SchermataLogin(
    onLoginSuccess: () -> Unit,
    onTornaIndietro: () -> Unit,
    onVaiARegistrazione: () -> Unit
) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val MioFontKomtit = FontFamily(Font(R.font.komtit__))
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = Tema.coloreSfondo) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- SFONDO DINAMICO ---
            Image(
                painter = painterResource(id = if (Tema.isBarreFaul) R.drawable.sfondo_barre_faul else if(Tema.isAteneo) R.drawable.sfondo_ateneo else R.drawable.sfondo_muretto_classico),
                contentDescription = "Sfondo Login",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

            // Tasto Indietro in alto a sinistra
            IconButton(
                onClick = onTornaIndietro,
                modifier = Modifier
                    .padding(top = 44.dp, start = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFontKomtit)
            }

            // Se è già loggato mostra il pannello logout
            if (databaseViewModel.isAdmin || databaseViewModel.ruoloAttuale != RuoloUtente.NESSUNO) {
                val etichettaRuolo = when (databaseViewModel.ruoloAttuale) {
                    RuoloUtente.ADMIN -> "ADMIN"
                    RuoloUtente.ORGANIZZATORE_MURETTO -> "ORG. MURETTO"
                    RuoloUtente.ORGANIZZATORE_EVENTI -> "ORG. EVENTI"
                    RuoloUtente.RAPPER -> "RAPPER"
                    else -> "LOGGATO"
                }
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Tema.coloreSfondoCard)
                            .border(3.dp, Tema.colorePrincipale, RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PROFILO ATTIVO", color = Tema.coloreTesto, fontSize = 24.sp, fontFamily = MioFontKomtit, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(etichettaRuolo, color = Tema.colorePrincipale, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MioFontKomtit)
                            databaseViewModel.profiloAttuale?.let {
                                Text(it.nome_arte.uppercase(), color = Tema.coloreTesto, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                databaseViewModel.supabase.auth.clearSession()
                                databaseViewModel.isAdmin = false
                                databaseViewModel.ruoloAttuale = RuoloUtente.NESSUNO
                                databaseViewModel.profiloAttuale = null
                                databaseViewModel.eventiPreferiti.clear()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("LOGOUT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Il tasto indietro testuale non serve più perché ora c'è la freccia in alto
                }
            } else {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var passwordVisibile by remember { mutableStateOf(false) }
                var errore by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ACCESSO",
                        fontFamily = MioFontKomtit,
                        fontSize = 48.sp,
                        color = Tema.coloreTesto,
                        modifier = Modifier.padding(bottom = 40.dp),
                        fontWeight = FontWeight.Bold
                    )

                    val inputColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Tema.coloreTesto,
                        unfocusedTextColor = Tema.coloreTesto,
                        focusedBorderColor = Tema.colorePrincipale,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Tema.colorePrincipale,
                        unfocusedLabelColor = Color.Gray
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = inputColors,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisibile) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisibile = !passwordVisibile }) {
                                Text(
                                    if (passwordVisibile) "HIDE" else "SHOW",
                                    color = Tema.colorePrincipale,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = inputColors,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errore.isNotEmpty()) {
                        Text(
                            errore,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 10.dp),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errore = "Inserisci email e password"
                                return@Button
                            }
                            isLoading = true
                            errore = ""

                            scope.launch {
                                try {
                                    databaseViewModel.supabase.auth.signInWith(Email) {
                                        this.email = email.trim()
                                        this.password = password
                                    }
                                    databaseViewModel.controllaRuolo()
                                    isLoading = false
                                    onLoginSuccess()
                                } catch (e: Exception) {
                                    isLoading = false
                                    errore = "Credenziali errate o utente non trovato."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("ENTRA", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = MioFontKomtit)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Link registrazione
                    TextButton(onClick = { onVaiARegistrazione() }) {
                        Text("NON HAI UN ACCOUNT? REGISTRATI", color = Tema.coloreTesto, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
