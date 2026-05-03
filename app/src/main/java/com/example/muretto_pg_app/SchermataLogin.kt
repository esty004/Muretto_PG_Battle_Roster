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
    val scope = rememberCoroutineScope()

    // Se è già loggato mostra il pannello logout
    if (DatabaseMcs.isAdmin || DatabaseMcs.ruoloAttuale != RuoloUtente.NESSUNO) {
        val etichettaRuolo = when (DatabaseMcs.ruoloAttuale) {
            RuoloUtente.ADMIN -> "ADMIN"
            RuoloUtente.ORGANIZZATORE_MURETTO -> "ORG. MURETTO"
            RuoloUtente.ORGANIZZATORE_EVENTI -> "ORG. EVENTI"
            else -> "LOGGATO"
        }
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "SEI GIÀ LOGGATO",
                color = Color.Green,
                fontSize = 20.sp,
                fontFamily = Tema.fontKomtit
            )
            Text(
                etichettaRuolo,
                color = Color.Green,
                fontSize = 16.sp,
                fontFamily = Tema.fontKomtit,
                modifier = Modifier.padding(top = 4.dp)
            )
            DatabaseMcs.profiloAttuale?.let {
                Text(
                    "${it.nome_arte}",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    scope.launch {
                        DatabaseMcs.supabase.auth.clearSession()
                        DatabaseMcs.isAdmin = false
                        DatabaseMcs.ruoloAttuale = RuoloUtente.NESSUNO
                        DatabaseMcs.profiloAttuale = null
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("LOGOUT", color = Color.White) }
            TextButton(onClick = onTornaIndietro) {
                Text("INDIETRO", color = Color.Gray)
            }
        }
        return
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisibile by remember { mutableStateOf(false) }
    var errore by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LOGIN",
            fontFamily = Tema.fontKomtit,
            fontSize = 50.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = Color.Gray) },
            visualTransformation = if (passwordVisibile) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisibile = !passwordVisibile }) {
                    Text(
                        if (passwordVisibile) "NASCONDI" else "MOSTRA",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )

        if (errore.isNotEmpty()) {
            Text(
                errore,
                color = Color.Red,
                fontSize = 14.sp,
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
                        DatabaseMcs.supabase.auth.signInWith(Email) {
                            this.email = email.trim()
                            this.password = password
                        }
                        DatabaseMcs.controllaRuolo()
                        isLoading = false
                        onLoginSuccess()
                    } catch (e: Exception) {
                        isLoading = false
                        errore = "Accesso negato: credenziali errate"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("ACCEDI", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link registrazione
        TextButton(onClick = { onVaiARegistrazione() }) {
            Text("Non hai un account? REGISTRATI", color = Color(0xFF4CAF50), fontSize = 14.sp)
        }

        TextButton(onClick = { onTornaIndietro() }) {
            Text("ANNULLA", color = Color.Gray)
        }
    }
}