package com.example.muretto_pg_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val MioFont = FontFamily(Font(R.font.komtit__))
    val scope = rememberCoroutineScope()

    val coloriInput = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Tema.coloreTesto,
        unfocusedTextColor = Tema.coloreTesto,
        focusedBorderColor = Tema.colorePrincipale,
        unfocusedBorderColor = Tema.coloreTestoSecondario,
        focusedLabelColor = Tema.colorePrincipale,
        unfocusedLabelColor = Tema.coloreTestoSecondario,
        cursorColor = Tema.colorePrincipale
    )

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
            modifier = Modifier.fillMaxSize().background(Tema.coloreSfondo).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "SEI GIÀ LOGGATO",
                color = Tema.colorePrincipale,
                fontSize = 24.sp,
                fontFamily = MioFont,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                etichettaRuolo,
                color = Tema.coloreTesto,
                fontSize = 18.sp,
                fontFamily = MioFont
            )
            databaseViewModel.profiloAttuale?.let {
                Text(
                    it.nome_arte,
                    color = Tema.coloreTestoSecondario,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("LOGOUT", color = Color.White, fontWeight = FontWeight.Bold) }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onTornaIndietro) {
                Text("INDIETRO", color = Tema.coloreTestoSecondario, fontFamily = MioFont)
            }
        }
        return
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisibile by remember { mutableStateOf(false) }
    var errore by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Tema.coloreSfondo)) {
        // Tasto Indietro in alto a sinistra
        IconButton(
            onClick = onTornaIndietro,
            modifier = Modifier.padding(top = 40.dp, start = 16.dp)
        ) {
            Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFont)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "LOGIN",
                fontFamily = MioFont,
                fontSize = 50.sp,
                color = Tema.coloreTesto,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = coloriInput,
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
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
                            if (passwordVisibile) "NASCONDI" else "MOSTRA",
                            color = Tema.coloreTestoSecondario,
                            fontSize = 11.sp
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = coloriInput,
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            if (errore.isNotEmpty()) {
                Text(
                    errore,
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
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
                            errore = e.message ?: "Accesso negato: credenziali errate"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                shape = RoundedCornerShape(24.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("ACCEDI", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = MioFont)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Link registrazione
            TextButton(onClick = { onVaiARegistrazione() }) {
                Text("Non hai un account? REGISTRATI", color = Tema.colorePrincipale, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}