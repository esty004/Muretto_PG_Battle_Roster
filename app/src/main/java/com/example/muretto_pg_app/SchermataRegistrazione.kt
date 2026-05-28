package com.example.muretto_pg_app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataRegistrazione(onTornaIndietro: () -> Unit) {
    val databaseViewModel = LocalDatabaseViewModel.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val MioFont = FontFamily(Font(R.font.komtit__))

    var nome by remember { mutableStateOf("") }
    var cognome by remember { mutableStateOf("") }
    var nomeArte by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    val tipiAccount = listOf(
        "Rapper (Solo Battle ed Eventi)",
        "Organizzatore Muretto (Gestione Freestyler/Eventi)",
        "Organizzatore Eventi (Gestione Eventi)"
    )
    var tipoAccountSelezionato by remember { mutableStateOf(tipiAccount[0]) }
    var menuTipoAperto by remember { mutableStateOf(false) }

    val muretti = listOf("Muretto PG", "Barre Faul", "Ateneo", "Fortitudo")
    var murettoSelezionato by remember { mutableStateOf(muretti[0]) }
    var menuMurettoAperto by remember { mutableStateOf(false) }

    var staCaricando by remember { mutableStateOf(false) }
    var registrazioneOk by remember { mutableStateOf(false) }
    var messaggio by remember { mutableStateOf("") }

    // Stile esplicito per far vedere bene il testo bianco su sfondo scuro
    val coloriInput = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Tema.colorePrincipale,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = Tema.colorePrincipale,
        unfocusedLabelColor = Color.LightGray,
        cursorColor = Tema.colorePrincipale,
        focusedTrailingIconColor = Color.White,
        unfocusedTrailingIconColor = Color.LightGray
    )

    if (registrazioneOk) {
        Box(modifier = Modifier.fillMaxSize().background(Tema.coloreSfondo), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp).background(Tema.coloreSfondoCard, RoundedCornerShape(20.dp)).padding(32.dp)) {
                Text("✅", fontSize = 60.sp)
                Spacer(modifier = Modifier.height(16.dp))
                if (tipoAccountSelezionato.contains("Rapper")) {
                    Text("Sei dentro!", color = Tema.coloreTesto, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Account attivato. Torna al login per accedere.", color = Tema.coloreTestoSecondario, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Text("Richiesta inviata!", color = Tema.coloreTesto, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Un Admin la revisionerà a breve.", color = Tema.coloreTestoSecondario, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onTornaIndietro, colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale)) {
                    Text("TORNA AL LOGIN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // <--- IL FIX MAGICO DELLA TASTIERA!
        color = Tema.coloreSfondo
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
                IconButton(onClick = onTornaIndietro, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("<", color = Tema.coloreTesto, fontSize = 45.sp, fontFamily = MioFont)
                }
                Text("REGISTRAZIONE", color = Tema.coloreTesto, fontSize = 32.sp, fontFamily = MioFont, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            Column(modifier = Modifier.fillMaxWidth(0.9f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, colors = coloriInput, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cognome, onValueChange = { cognome = it }, label = { Text("Cognome") }, colors = coloriInput, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nomeArte, onValueChange = { nomeArte = it }, label = { Text("Nome d'arte (A.K.A)") }, colors = coloriInput, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Numero di Telefono") }, colors = coloriInput, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, colors = coloriInput, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), colors = coloriInput, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = menuTipoAperto, onExpandedChange = { menuTipoAperto = !menuTipoAperto }) {
                    OutlinedTextField(
                        value = tipoAccountSelezionato, onValueChange = {}, readOnly = true, label = { Text("Tipo Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuTipoAperto) }, colors = coloriInput, modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = menuTipoAperto, onDismissRequest = { menuTipoAperto = false }, modifier = Modifier.background(Tema.coloreSfondoCard)) {
                        tipiAccount.forEach { opzione ->
                            DropdownMenuItem(
                                text = { Text(opzione, color = Tema.coloreTesto) },
                                onClick = { tipoAccountSelezionato = opzione; menuTipoAperto = false }
                            )
                        }
                    }
                }

                if (tipoAccountSelezionato.contains("Organizzatore Muretto")) {
                    ExposedDropdownMenuBox(expanded = menuMurettoAperto, onExpandedChange = { menuMurettoAperto = !menuMurettoAperto }) {
                        OutlinedTextField(
                            value = murettoSelezionato, onValueChange = {}, readOnly = true, label = { Text("Muretto gestito") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuMurettoAperto) }, colors = coloriInput, modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = menuMurettoAperto, onDismissRequest = { menuMurettoAperto = false }, modifier = Modifier.background(Tema.coloreSfondoCard)) {
                            muretti.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m, color = Tema.coloreTesto) },
                                    onClick = { murettoSelezionato = m; menuMurettoAperto = false }
                                )
                            }
                        }
                    }
                }

                if (messaggio.isNotEmpty()) {
                    Text(messaggio, color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (nome.isBlank() || cognome.isBlank() || nomeArte.isBlank() || email.isBlank() || password.isBlank() || telefono.isBlank()) {
                            messaggio = "Compila tutti i campi!"
                            return@Button
                        }
                        if (password.length < 6) {
                            messaggio = "La password deve avere almeno 6 caratteri."
                            return@Button
                        }
                        staCaricando = true
                        messaggio = ""

                        scope.launch {
                            val tipoDatabase = when {
                                tipoAccountSelezionato.startsWith("Rapper") -> "rapper"
                                tipoAccountSelezionato.startsWith("Organizzatore Eventi") -> "organizzatore_eventi"
                                else -> "organizzatore_muretto"
                            }
                            val murettoDatabase = if (tipoDatabase == "organizzatore_muretto") {
                                when (murettoSelezionato) {
                                    "Barre Faul" -> "2d0f412c-4e9d-4eab-b886-f7a2226d7b9e"
                                    "Fortitudo" -> "22ea8a2f-d45d-40b2-a6ee-841058f12f99"
                                    "Ateneo" -> "d20af410-652c-4d91-ab62-3aae3b2a8db2" // Inserisci qui l'UUID di Ateneo se ce l'hai nel DB!
                                    else -> "09fbe1d3-0022-41b8-ba4b-edc887c145a2" // Muretto PG
                                }
                            } else null

                            val successo = if (tipoDatabase == "rapper") {
                                databaseViewModel.registraRapperDiretto(nome.trim(), cognome.trim(), nomeArte.trim(), email.trim(), password.trim(), telefono.trim())
                            } else {
                                databaseViewModel.inviaRichiestaAccount(nome.trim(), cognome.trim(), nomeArte.trim(), email.trim(), password.trim(), telefono.trim(), tipoDatabase, murettoDatabase)
                            }

                            staCaricando = false
                            if (successo) {
                                registrazioneOk = true
                            } else {
                                messaggio = "Errore durante la registrazione. Riprova."
                            }
                        }
                    },
                    enabled = !staCaricando,
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.colorePrincipale),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    if (staCaricando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("INVIA", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}