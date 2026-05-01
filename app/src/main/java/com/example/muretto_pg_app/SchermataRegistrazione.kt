package com.example.muretto_pg_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataRegistrazione(onTornaIndietro: () -> Unit) {
    val MioFont = FontFamily(Font(R.font.komtit__))
    val scope = rememberCoroutineScope()

    var nome by remember { mutableStateOf("") }
    var cognome by remember { mutableStateOf("") }
    var nomeArte by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confermaPassword by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var passwordVisibile by remember { mutableStateOf(false) }
    var confermaPasswordVisibile by remember { mutableStateOf(false) }

    // Tipo account
    var tipoAccount by remember { mutableStateOf("") } // 'organizzatore_muretto' o 'organizzatore_eventi'
    var menuTipoAperto by remember { mutableStateOf(false) }

    // Muretto (solo per organizzatore_muretto)
    var murettoSelezionato by remember { mutableStateOf("") }
    var menuMurettoAperto by remember { mutableStateOf(false) }

    var errore by remember { mutableStateOf("") }
    var staInviando by remember { mutableStateOf(false) }
    var mostraPopupAttesa by remember { mutableStateOf(false) }

    val sfondoScuro = Color(0xFF1E1E1E)
    val verdePrimario = Color(0xFF4CAF50)
    val coloreTextField = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = verdePrimario,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = verdePrimario,
        unfocusedLabelColor = Color.Gray
    )

    // Popup attesa approvazione
    if (mostraPopupAttesa) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(20.dp))
                    .border(2.dp, verdePrimario, RoundedCornerShape(20.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏳", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "RICHIESTA INVIATA",
                        color = verdePrimario,
                        fontSize = 22.sp,
                        fontFamily = MioFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Attendi la conferma su WhatsApp da parte di un amministratore per procedere.",
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            mostraPopupAttesa = false
                            onTornaIndietro()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = verdePrimario),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(sfondoScuro)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 24.dp)) {
            IconButton(
                onClick = onTornaIndietro,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("<", color = Color.White, fontSize = 45.sp, fontFamily = MioFont)
            }
            Text(
                "REGISTRATI",
                color = Color.White,
                fontSize = 28.sp,
                fontFamily = MioFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Nome
        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome") },
            colors = coloreTextField,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Cognome
        OutlinedTextField(
            value = cognome,
            onValueChange = { cognome = it },
            label = { Text("Cognome") },
            colors = coloreTextField,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Nome d'arte
        OutlinedTextField(
            value = nomeArte,
            onValueChange = { nomeArte = it },
            label = { Text("Nome d'arte") },
            colors = coloreTextField,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            colors = coloreTextField,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Telefono
        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Numero di telefono (con prefisso, es. +39...)") },
            colors = coloreTextField,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisibile) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisibile = !passwordVisibile }) {
                    Text(if (passwordVisibile) "NASCONDI" else "MOSTRA", color = Color.Gray, fontSize = 11.sp)
                }
            },
            colors = coloreTextField,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Conferma Password
        OutlinedTextField(
            value = confermaPassword,
            onValueChange = { confermaPassword = it },
            label = { Text("Conferma password") },
            visualTransformation = if (confermaPasswordVisibile) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { confermaPasswordVisibile = !confermaPasswordVisibile }) {
                    Text(if (confermaPasswordVisibile) "NASCONDI" else "MOSTRA", color = Color.Gray, fontSize = 11.sp)
                }
            },
            colors = coloreTextField,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Tipo account — menu a tendina
        Text(
            "Tipo di account",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
        )
        ExposedDropdownMenuBox(
            expanded = menuTipoAperto,
            onExpandedChange = { menuTipoAperto = !menuTipoAperto }
        ) {
            OutlinedTextField(
                value = when (tipoAccount) {
                    "organizzatore_muretto" -> "Organizzatore Muretto"
                    "organizzatore_eventi" -> "Organizzatore Eventi"
                    else -> ""
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Seleziona ruolo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuTipoAperto) },
                colors = coloreTextField,
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = menuTipoAperto,
                onDismissRequest = { menuTipoAperto = false },
                modifier = Modifier.background(Color(0xFF2A2A2A))
            ) {
                DropdownMenuItem(
                    text = { Text("Organizzatore Muretto", color = Color.White) },
                    onClick = { tipoAccount = "organizzatore_muretto"; menuTipoAperto = false; murettoSelezionato = "" }
                )
                DropdownMenuItem(
                    text = { Text("Organizzatore Eventi", color = Color.White) },
                    onClick = { tipoAccount = "organizzatore_eventi"; menuTipoAperto = false; murettoSelezionato = "" }
                )
            }
        }

        // Selezione muretto — solo se organizzatore_muretto
        if (tipoAccount == "organizzatore_muretto") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Seleziona il tuo Muretto",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { murettoSelezionato = "muretto_pg" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (murettoSelezionato == "muretto_pg") Color(0xFFD32F2F) else Color(0xFF333333)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("Muretto PG", color = Color.White, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { murettoSelezionato = "barre_faul" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (murettoSelezionato == "barre_faul") Color(0xFF1E88E5) else Color(0xFF333333)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("Barre Faul", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Errore
        if (errore.isNotEmpty()) {
            Text(
                errore,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Bottone Crea Account
        Button(
            onClick = {
                // Validazioni
                errore = when {
                    nome.isBlank() || cognome.isBlank() || nomeArte.isBlank() -> "Compila tutti i campi anagrafici"
                    email.isBlank() || !email.contains("@") -> "Email non valida"
                    telefono.isBlank() -> "Inserisci il numero di telefono"
                    password.length < 6 -> "La password deve avere almeno 6 caratteri"
                    password != confermaPassword -> "Le password non coincidono"
                    tipoAccount.isBlank() -> "Seleziona un tipo di account"
                    tipoAccount == "organizzatore_muretto" && murettoSelezionato.isBlank() -> "Seleziona il tuo muretto"
                    else -> ""
                }
                if (errore.isNotEmpty()) return@Button

                staInviando = true
                scope.launch {
                    val successo = DatabaseMcs.inviaRichiestaAccount(
                        nome = nome.trim(),
                        cognome = cognome.trim(),
                        nomeArte = nomeArte.trim(),
                        email = email.trim(),
                        passwordTemp = password,
                        telefono = telefono.trim(),
                        tipoAccount = tipoAccount,
                        muretto = if (tipoAccount == "organizzatore_muretto") murettoSelezionato else null
                    )
                    staInviando = false
                    if (successo) {
                        mostraPopupAttesa = true
                    } else {
                        errore = "Errore nell'invio della richiesta. Riprova."
                    }
                }
            },
            enabled = !staInviando,
            colors = ButtonDefaults.buttonColors(containerColor = verdePrimario),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            if (staInviando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("CREA ACCOUNT", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}