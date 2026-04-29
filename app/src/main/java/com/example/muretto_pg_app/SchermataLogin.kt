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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Gli import puntano a gotrue, ma usano "auth"
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun SchermataLogin(onLoginSuccess: () -> Unit, onTornaIndietro: () -> Unit) {
    val MioFontKomtit = FontFamily(Font(R.font.komtit__))

    // Creiamo lo scope per le coroutine QUI, così possiamo usarlo sia per il login che per il logout
    val scope = rememberCoroutineScope()

    // Controlla se è già loggato
    if (DatabaseMcs.isAdmin) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text("SEI GIÀ LOGGATO COME ADMIN!", color = Color.Green, fontSize = 20.sp, fontFamily = MioFontKomtit)
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    // Lanciamo la coroutine per la funzione suspend
                    scope.launch {
                        DatabaseMcs.supabase.auth.clearSession()
                        DatabaseMcs.isAdmin = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("LOGOUT", color = Color.White) }
            TextButton(onClick = onTornaIndietro) { Text("INDIETRO", color = Color.Gray) }
        }
        return
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errore by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // Sfondo grigio oscuro
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LOGIN",
            fontFamily = MioFontKomtit,
            fontSize = 50.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        if (errore.isNotEmpty()) {
            Text(errore, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errore = "Inserisci email e password"
                    return@Button
                }

                isLoading = true
                scope.launch {
                    try {
                        DatabaseMcs.supabase.auth.signInWith(Email) {
                            this.email = email.trim()
                            this.password = password
                        }
                        DatabaseMcs.controllaAdmin() // Aggiorna lo stato Admin globale
                        isLoading = false
                        onLoginSuccess() // Torna alla mappa
                    } catch (e: Exception) {
                        isLoading = false
                        errore = "Accesso negato: credenziali errate"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), // Tasto verde
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("ACCEDI", fontSize = 20.sp, color = Color.White)
            }
        }

        TextButton(onClick = { onTornaIndietro() }, modifier = Modifier.padding(top = 20.dp)) {
            Text("ANNULLA", color = Color.Gray)
        }
    }
}