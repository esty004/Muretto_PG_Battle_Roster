package com.example.muretto_pg_app

import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import io.ktor.http.contentType
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.plus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import kotlinx.serialization.Serializable
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import io.github.jan.supabase.functions.functions
import io.ktor.util.InternalAPI
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

// ─── TEMA E RUOLI ─────────────────────────────────────────────────────────────

// Inseriscilo nel file Modelli.kt
enum class MurettoAttivo { PG, BARRE_FAUL, ATENEO }

object Tema {
    var murettoSelezionato by mutableStateOf(MurettoAttivo.PG)

    // Helper per non rompere i controlli isBarreFaul esistenti negli altri file
    val isBarreFaul get() = murettoSelezionato == MurettoAttivo.BARRE_FAUL
    val isAteneo get() = murettoSelezionato == MurettoAttivo.ATENEO

    val colorePrincipale: Color get() = when (murettoSelezionato) {
        MurettoAttivo.BARRE_FAUL -> Color(0xFF1E88E5) // Blu
        MurettoAttivo.ATENEO -> Color(0xFFFF9800)     // Arancione (Ateneo)
        MurettoAttivo.PG -> Color(0xFFD32F2F)         // Rosso
    }

    val coloreSfondo: Color get() = when (murettoSelezionato) {
        MurettoAttivo.BARRE_FAUL -> Color(0xFFDCDCDC) // Chiaro
        else -> Color.Black                           // Nero (PG e Ateneo)
    }

    val coloreSfondoCard: Color get() = when (murettoSelezionato) {
        MurettoAttivo.BARRE_FAUL -> Color(0xFFF5F5F5)
        MurettoAttivo.ATENEO -> Color(0xFF1E1E1E)
        MurettoAttivo.PG -> Color(0xFF111111)
    }

    val coloreTesto: Color get() = if (murettoSelezionato == MurettoAttivo.BARRE_FAUL) Color.Black else Color.White
    val coloreTestoSecondario: Color get() = if (murettoSelezionato == MurettoAttivo.BARRE_FAUL) Color.DarkGray else Color.Gray

    val gradienteCard: Brush
        get() = when (murettoSelezionato) {
            MurettoAttivo.BARRE_FAUL -> Brush.horizontalGradient(colors = listOf(Color(0xFF002244), Color(0xFF004488)))
            MurettoAttivo.ATENEO -> Brush.horizontalGradient(colors = listOf(Color(0xFF4A2B00), Color(0xFF804D00)))
            MurettoAttivo.PG -> Brush.horizontalGradient(colors = listOf(Color(0xFF3A0000), Color(0xFF00003A)))
        }

    // Sfondo per le schermate interne
    val sfondoGenerale: Int get() = when (murettoSelezionato) {
        MurettoAttivo.BARRE_FAUL -> R.drawable.sfondo_barre_faul
        MurettoAttivo.ATENEO -> R.drawable.sfondo_ateneo
        MurettoAttivo.PG -> R.drawable.sfondo_muretto_classico
    }
}

enum class RuoloUtente { NESSUNO, ADMIN, ORGANIZZATORE_MURETTO, ORGANIZZATORE_EVENTI, RAPPER }

// ─── MODELLI DATI ─────────────────────────────────────────────────────────────

@Serializable
data class Freestyler(
    val id: String = "",
    val nome: String = "",
    val immagineUrl: String? = null, // Protetto per i null
    val muretto: String? = null,
    val muretto_id: String? = null // Ora è una String per accettare gli UUID
)

@Serializable
data class ProfiloUtente(
    val id: String = "",
    val nome: String = "",
    val cognome: String = "",
    val nome_arte: String = "",
    val telefono: String = "",
    val tipo_account: String = "",
    val muretto_id: String? = null
)

@Serializable
data class RichiestaAccount(
    val id: String = "",
    val nome: String = "",
    val cognome: String = "",
    val nome_arte: String = "",
    val email: String = "",
    val password_temp: String = "",
    val telefono: String = "",
    val tipo_account: String = "",
    val muretto_id: String? = null,
    val stato: String = "in_attesa",
    val created_at: String = ""
)

@Serializable
data class Evento(
    val id: String = java.util.UUID.randomUUID().toString(),
    val titolo: String,
    val location_nome: String,
    val lat: Double,
    val lng: Double,
    val data_ora: String,
    val tipo: String,
    val prezzo: String,
    val immagine_url: String?,
    val organizzatore_id: String,
    val stato: String = "in_attesa",
    val scala_pin: Float = 1.0f,
    // --- CAMPI RESI SICURI CON IL PUNTO INTERROGATIVO (?) ---
    val insta: String? = null,
    val maps: String? = null,
    val descrizione: String? = null
)

@Serializable
data class EventoPreferito(
    val user_id: String,
    val evento_id: String
)

@Serializable
data class Word(val valore: String)

@Serializable
data class Topic(val valore: String, val parole_vietate: String? = null)

@Serializable
data class Mode(val valore: String)

// ─── DATABASE E SUPABASE SICURO ───────────────────────────────────────────────

data class DatabaseUiState(
    val error: String? = null
)

val LocalDatabaseViewModel = staticCompositionLocalOf<DatabaseViewModel> {
    error("No DatabaseViewModel provided")
}

class DatabaseViewModel : ViewModel() {
    private val supabaseUrl = "https://bvzwnuxljhbxeplhbjze.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2endudXhsamhieGVwbGhianplIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzczNzUwNjQsImV4cCI6MjA5Mjk1MTA2NH0.4d9ZS_87F7LcelnJMBAdbDkbXOeE2xQ7rGSehFHtMs8"
    // DA ORA IN POI SUPABASE SI INIZIALIZZA TRAMITE LA CASSAFORTE
    lateinit var supabase: io.github.jan.supabase.SupabaseClient

    fun inizializzaSupabase(context: Context) {
        // 1. Creiamo la Master Key AES256
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // 2. Creiamo il file blindato SharedPreferences
        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "supabase_session_sicura",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // 3. Avviamo Supabase
        supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
            install(Postgrest)
            install(Storage)
            install(Auth)
            install(io.github.jan.supabase.functions.Functions) // <--- AGGIUNGI QUESTA RIGA!
        }
    }

    private val _uiState = MutableStateFlow(DatabaseUiState())
    val uiState: StateFlow<DatabaseUiState> = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("DatabaseViewModel", "Errore critico DB: ${throwable.message}", throwable)
        _uiState.update { it.copy(error = "Si è verificato un errore di connessione. Riprova più tardi.") }
        staCaricando.value = false
    }

    private val safeScope = viewModelScope + exceptionHandler

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    var listaMcsCloud = mutableStateListOf<Freestyler>()
    var tuttiMcsCloud = mutableListOf<Freestyler>()
    var staCaricando = mutableStateOf(false)

    var isAdmin by mutableStateOf(false)
    var ruoloAttuale by mutableStateOf(RuoloUtente.NESSUNO)
    var profiloAttuale by mutableStateOf<ProfiloUtente?>(null)

    var richiesteInAttesa = mutableStateListOf<RichiestaAccount>()
    var eventiInAttesa = mutableStateListOf<Evento>()
    var eventiApprovati = mutableStateListOf<Evento>()

    var eventiPreferiti = mutableStateListOf<String>() // Conterrà gli ID degli eventi preferiti dell'utente


    suspend fun eseguiRegistrazioneSicura(richiesta: RichiestaAccount): Boolean {
        return try {
            android.util.Log.d("TEST_FUNZIONE", "1. Costruisco il JSON per il server...")

            // Creiamo un payload JSON manuale e formattato perfettamente
            // Gestiamo il muretto_id in sicurezza per evitare stringhe "null" letterali
            val murettoJson = if (richiesta.muretto_id != null) "\"${richiesta.muretto_id}\"" else "null"

            val payloadJSON = """
                {
                    "email": "${richiesta.email}",
                    "password_temp": "${richiesta.password_temp}",
                    "nome": "${richiesta.nome}",
                    "cognome": "${richiesta.cognome}",
                    "nome_arte": "${richiesta.nome_arte}",
                    "telefono": "${richiesta.telefono}",
                    "tipo_account": "${richiesta.tipo_account}",
                    "muretto_id": $murettoJson
                }
            """.trimIndent()

            android.util.Log.d("TEST_FUNZIONE", "2. Payload pronto, invio al server...")

            val response = supabase.functions.invoke("gestore-registrazioni") {
                // IL SEGRETO: Passiamo il JSON come TESTO GREZZO (String), non come Oggetto
                setBody(payloadJSON)
                contentType(io.ktor.http.ContentType.Application.Json)
            }

            android.util.Log.d("TEST_FUNZIONE", "3. Successo! Risposta: ${response.bodyAsText()}")
            true
        } catch (e: Exception) {
            android.util.Log.e("TEST_FUNZIONE", "ERRORE CRITICO: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    fun inizializzaSessione() {
        safeScope.launch(Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(500)
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    controllaRuolo()
                    fetchEventiPreferiti()
                }
            } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore sessione", e) }
        }
    }

    suspend fun controllaRuolo() {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            withContext(Dispatchers.Main) {
                isAdmin = false
                ruoloAttuale = RuoloUtente.NESSUNO
                profiloAttuale = null
            }
            return
        }
        try {
            val adminList = supabase.postgrest["amministratori"].select { filter { eq("id", user.id) } }.data
            if (adminList != "[]") {
                withContext(Dispatchers.Main) {
                    isAdmin = true
                    ruoloAttuale = RuoloUtente.ADMIN
                }
                fetchRichiesteInAttesa()
                fetchEventiInAttesa()
                return
            }

            val profiliJson = supabase.postgrest["profili"].select { filter { eq("id", user.id) } }.decodeList<ProfiloUtente>()
            withContext(Dispatchers.Main) {
                isAdmin = false
                if (profiliJson.isNotEmpty()) {
                    val profilo = profiliJson[0]
                    profiloAttuale = profilo
                    ruoloAttuale = when (profilo.tipo_account) {
                        "organizzatore_muretto" -> RuoloUtente.ORGANIZZATORE_MURETTO
                        "organizzatore_eventi" -> RuoloUtente.ORGANIZZATORE_EVENTI
                        "rapper" -> RuoloUtente.RAPPER
                        else -> RuoloUtente.NESSUNO
                    }
                } else {
                    ruoloAttuale = RuoloUtente.NESSUNO
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                isAdmin = false
                ruoloAttuale = RuoloUtente.NESSUNO
            }
        }
    }

    fun controllaAdmin() { safeScope.launch(Dispatchers.IO) { controllaRuolo() } }

    fun fetchMcsDalCloud(idDelMuretto: String) {
        staCaricando.value = true
        safeScope.launch(Dispatchers.IO) {
            try {
                val scaricati = supabase.postgrest["mcs"].select().decodeList<Freestyler>()
                withContext(Dispatchers.Main) {
                    tuttiMcsCloud.clear()
                    tuttiMcsCloud.addAll(scaricati)

                    // Filtra usando il nuovo UUID
                    val filtrati = scaricati.filter { it.muretto_id == idDelMuretto }

                    listaMcsCloud.clear()
                    listaMcsCloud.addAll(filtrati)
                    staCaricando.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { staCaricando.value = false }
            }
        }
    }

    fun cercaMcGlobale(nomeDaCercare: String): Freestyler? {
        return tuttiMcsCloud.find { it.nome.equals(nomeDaCercare, ignoreCase = true) }
    }

    suspend fun inserisciNuovoMc(nome: String, murettoId: String, imageBytes: ByteArray?): Boolean {
        return try {
            var imageUrl = ""
            if (imageBytes != null) {
                val fileName = "mc_${UUID.randomUUID()}.jpg"
                val bucket = supabase.storage["immagini"]
                bucket.upload(fileName, imageBytes, upsert = true)
                imageUrl = bucket.publicUrl(fileName)
            }

            val esistenti = supabase.postgrest["mcs"].select { filter { eq("muretto_id", murettoId) } }.decodeList<Freestyler>()

            val nuovoId = if (murettoId == "2d0f412c-4e9d-4eab-b886-f7a2226d7b9e") { // Barre Faul
                val maxNum = esistenti.mapNotNull { it.id.replace("bf", "").toIntOrNull() }.maxOrNull() ?: 0
                "bf${maxNum + 1}"
            } else { // Muretto PG
                val maxNum = esistenti.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0
                (maxNum + 1).toString()
            }

            val nuovoMc = Freestyler(id = nuovoId, nome = nome, immagineUrl = imageUrl, muretto = null, muretto_id = murettoId)
            supabase.postgrest["mcs"].insert(nuovoMc)
            true
        } catch (e: Exception) { false }
    }

    suspend fun aggiornaMc(mcId: String, nuovoNome: String, imageBytes: ByteArray?): Boolean {
        return try {
            var nuovaUrl: String? = null
            if (imageBytes != null) {
                val fileName = "mc_${UUID.randomUUID()}.jpg"
                val bucket = supabase.storage["immagini"]
                bucket.upload(fileName, imageBytes, upsert = true)
                nuovaUrl = bucket.publicUrl(fileName)
            }
            supabase.postgrest["mcs"].update({
                set("nome", nuovoNome)
                if (nuovaUrl != null) set("immagineUrl", nuovaUrl)
            }) { filter { eq("id", mcId) } }
            true
        } catch (e: Exception) { false }
    }

    suspend fun eliminaMc(mcId: String): Boolean {
        return try {
            supabase.postgrest["mcs"].delete { filter { eq("id", mcId) } }
            withContext(Dispatchers.Main) {
                listaMcsCloud.removeIf { it.id == mcId }
                tuttiMcsCloud.removeIf { it.id == mcId }
            }
            true
        } catch (e: Exception) { false }
    }

    suspend fun inserisciNuovoEvento(
        titolo: String, locationNome: String, lat: Double, lng: Double,
        dataOra: String, tipo: String, prezzo: String, scalaPin: Float, imageBytes: ByteArray?,
        insta: String, maps: String, descrizione: String // Nuovi parametri
    ): Boolean {
        return try {
            val user = supabase.auth.currentUserOrNull() ?: return false
            var imageUrl: String? = null
            if (imageBytes != null) {
                val fileName = "evento_${UUID.randomUUID()}.jpg"
                val bucket = supabase.storage["locandine_eventi"]
                bucket.upload(fileName, imageBytes, upsert = true)
                imageUrl = bucket.publicUrl(fileName)
            }
            val nuovoEvento = Evento(
                titolo = titolo, location_nome = locationNome, lat = lat, lng = lng,
                data_ora = dataOra, tipo = tipo, prezzo = prezzo, immagine_url = imageUrl,
                organizzatore_id = user.id, stato = "in_attesa", scala_pin = scalaPin,
                insta = insta, maps = maps, descrizione = descrizione // Nuovi campi
            )
            supabase.postgrest["eventi"].insert(nuovoEvento)
            true
        } catch (e: Exception) {
            android.util.Log.e("ERRORE_SUPABASE", "Il database dice: ${e.message}", e)
            false
        }
    }

    suspend fun inviaRichiestaAccount(
        nome: String, cognome: String, nomeArte: String, email: String, passwordTemp: String, telefono: String, tipoAccount: String, muretto: String?
    ): Boolean {
        return try {
            val passwordCriptata = CryptoHelper.encrypt(passwordTemp)
            // Creiamo la richiesta SENZA il campo created_at (lasciamo fare al DB)
            val richiesta = RichiestaAccount(
                id = UUID.randomUUID().toString(),
                nome = nome,
                cognome = cognome,
                nome_arte = nomeArte,
                email = email,
                password_temp = passwordCriptata,
                telefono = telefono,
                tipo_account = tipoAccount,
                muretto_id = muretto,
                stato = "in_attesa"
            )

            // Proviamo l'inserimento
            supabase.postgrest["richieste_account"].insert(richiesta)

            android.util.Log.d("DEBUG_REGISTRAZIONE", "Richiesta inviata con successo!")
            true
        } catch (e: Exception) {
            // QUESTO LOG TI DIRÀ PERCHÉ FALLISCE (es: permessi mancanti o colonne errate)
            android.util.Log.e("DEBUG_REGISTRAZIONE", "FALLIMENTO: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun registraRapperDiretto(
        nome: String, cognome: String, nomeArte: String, email: String, passwordTemp: String, telefono: String
    ): Boolean {
        return try {
            val serviceRoleKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2endudXhsamhieGVwbGhianplIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NzM3NTA2NCwiZXhwIjoyMDkyOTUxMDY0fQ.HdIa06E9UVn30zyO9cL6sR_kODfoJJ5NHlZN4VHgoe8"

            val nuovoUserId = withContext(Dispatchers.IO) {
                val url = URL("https://bvzwnuxljhbxeplhbjze.supabase.co/auth/v1/admin/users")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", serviceRoleKey)
                conn.setRequestProperty("Authorization", "Bearer $serviceRoleKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val body = """{"email":"$email","password":"$passwordTemp","email_confirm":true}"""
                conn.outputStream.use { it.write(body.toByteArray()) }

                val codice = conn.responseCode
                if (codice in 200..299) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseStr)
                    jsonResponse.getString("id")
                } else null
            }

            if (nuovoUserId != null) {
                val nuovoProfilo = ProfiloUtente(
                    id = nuovoUserId,
                    nome = nome,
                    cognome = cognome,
                    nome_arte = nomeArte,
                    telefono = telefono,
                    tipo_account = "rapper"
                )
                supabase.postgrest["profili"].insert(nuovoProfilo)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun fetchRichiesteInAttesa() {
        safeScope.launch(Dispatchers.IO) {
            try {
                val richieste = supabase.postgrest["richieste_account"].select { filter { eq("stato", "in_attesa") } }.decodeList<RichiestaAccount>()
                withContext(Dispatchers.Main) {
                    richiesteInAttesa.clear()
                    richiesteInAttesa.addAll(richieste)
                }
            } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore richieste", e) }
        }
    }

    fun fetchEventiInAttesa() {
        safeScope.launch(Dispatchers.IO) {
            try {
                val eventi = supabase.postgrest["eventi"].select { filter { eq("stato", "in_attesa") } }.decodeList<Evento>()
                withContext(Dispatchers.Main) {
                    eventiInAttesa.clear()
                    eventiInAttesa.addAll(eventi)
                }
            } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore eventi in attesa", e) }
        }
    }

    fun fetchEventiApprovati() {
        safeScope.launch(Dispatchers.IO) {
            try {
                val eventi = supabase.postgrest["eventi"].select { filter { eq("stato", "approvato") } }.decodeList<Evento>()
                withContext(Dispatchers.Main) {
                    eventiApprovati.clear()
                    eventiApprovati.addAll(eventi)
                }
            } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore eventi approvati", e) }
        }
    }

    // NUOVE FUNZIONI PER I PREFERITI
    fun fetchEventiPreferiti() {
        safeScope.launch(Dispatchers.IO) {
            try {
                val user = supabase.auth.currentUserOrNull() ?: return@launch
                val preferiti = supabase.postgrest["eventi_preferiti"].select { filter { eq("user_id", user.id) } }.decodeList<EventoPreferito>()
                withContext(Dispatchers.Main) {
                    eventiPreferiti.clear()
                    eventiPreferiti.addAll(preferiti.map { it.evento_id })
                }
            } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore preferiti", e) }
        }
    }

    fun togglePreferito(eventoId: String) {
        safeScope.launch(Dispatchers.IO) {
            try {
                val user = supabase.auth.currentUserOrNull() ?: return@launch
                if (eventiPreferiti.contains(eventoId)) {
                    supabase.postgrest["eventi_preferiti"].delete { filter { eq("user_id", user.id); eq("evento_id", eventoId) } }
                    withContext(Dispatchers.Main) { eventiPreferiti.remove(eventoId) }
                } else {
                    supabase.postgrest["eventi_preferiti"].insert(EventoPreferito(user.id, eventoId))
                    withContext(Dispatchers.Main) { eventiPreferiti.add(eventoId) }
                }
            } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore toggle preferito", e) }
        }
    }

    suspend fun accettaRichiesta(richiesta: RichiestaAccount): Boolean {
        android.util.Log.d("DEBUG_ACCETTA", "Inizio per: ${richiesta.email}")

        // Chiamiamo la funzione sicura
        val successoAuth = eseguiRegistrazioneSicura(richiesta)

        // Se fallisce ma NON è per colpa dell'email già registrata, allora ci fermiamo
        if (!successoAuth) {
            // Qui potresti aggiungere un controllo più fine, ma per ora
            // facciamo in modo che l'admin possa comunque provare a forzare
            // l'aggiornamento dello stato se vede che il profilo esiste già.
            android.util.Log.e("DEBUG_ACCETTA", "Il server ha dato errore. Controllo se devo comunque chiudere la notifica...")
        }

        return try {
            // FORZIAMO l'aggiornamento dello stato sul DB
            // Se l'admin preme "Accetta", vogliamo che la notifica sparisca comunque
            supabase.postgrest["richieste_account"].update(
                { set("stato", "accettata") }
            ) {
                filter { eq("id", richiesta.id) }
            }

            // Rimuoviamo dalla lista locale per far sparire il Dialog
            withContext(Dispatchers.Main) {
                richiesteInAttesa.removeIf { it.id == richiesta.id }
            }

            android.util.Log.d("DEBUG_ACCETTA", "Notifica aggiornata con successo.")
            true
        } catch (e: Exception) {
            android.util.Log.e("DEBUG_ACCETTA", "Errore finale DB: ${e.message}")
            false
        }
    }

    suspend fun rifiutaRichiesta(richiestaId: String): Boolean {
        return try {
            supabase.postgrest["richieste_account"].update({ set("stato", "rifiutata") }) { filter { eq("id", richiestaId) } }
            withContext(Dispatchers.Main) { richiesteInAttesa.removeIf { it.id == richiestaId } }
            true
        } catch (e: Exception) { false }
    }

    suspend fun accettaEvento(eventoId: String): Boolean {
        return try {
            supabase.postgrest["eventi"].update({ set("stato", "approvato") }) { filter { eq("id", eventoId) } }
            withContext(Dispatchers.Main) { eventiInAttesa.removeIf { it.id == eventoId } }
            true
        } catch (e: Exception) { false }
    }

    suspend fun rifiutaEvento(eventoId: String): Boolean {
        return try {
            supabase.postgrest["eventi"].delete { filter { eq("id", eventoId) } }
            withContext(Dispatchers.Main) { eventiInAttesa.removeIf { it.id == eventoId } }
            true
        } catch (e: Exception) { false }
    }

    // --- NUOVE FUNZIONI GENERATORI (DB REALTIME) ---

    suspend fun fetchRandomWords(quantita: Int): List<String> {
        return try {
            // Prendiamo un pool di 200 parole e ne scegliamo X a caso
            val result = supabase.postgrest["common_words"].select().decodeList<Word>()
            result.shuffled().take(quantita).map { it.valore }
        } catch (e: Exception) {
            Log.e("Supabase", "Errore fetch parole: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchRandomTopic(): String {
        return try {
            val result = supabase.postgrest["topics"].select().decodeList<Topic>()
            result.randomOrNull()?.valore ?: "ERRORE CARICAMENTO"
        } catch (e: Exception) {
            "ERRORE CONNESSIONE"
        }
    }

    suspend fun fetchRandomMode(): String {
        return try {
            val result = supabase.postgrest["modes"].select().decodeList<Mode>()
            result.randomOrNull()?.valore ?: "ERRORE CARICAMENTO"
        } catch (e: Exception) {
            "ERRORE CONNESSIONE"
        }
    }

    suspend fun fetchRandomTaboo(): Topic? {
        return try {
            val result = supabase.postgrest["topics"].select().decodeList<Topic>()
            result.filter { !it.parole_vietate.isNullOrBlank() }.randomOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchLinkerData(): Pair<String, List<String>> = coroutineScope {
        try {
            val topicTask = async { supabase.postgrest["topics"].select().decodeList<Topic>().random().valore }
            val wordsTask = async { supabase.postgrest["common_words"].select().decodeList<Word>().shuffled().take(3).map { it.valore } }
            
            Pair(topicTask.await(), wordsTask.await())
        } catch (e: Exception) {
            Pair("ERRORE", listOf("ERRORE", "ERRORE", "ERRORE"))
        }
    }
}

// ─── TORNEO ───────────────────────────────────────────────────────────────────
data class Round(val id: String, val numero: Int, var partecipanti: List<Freestyler>, var completato: Boolean = false, var vincitoreId: String? = null)
enum class FaseTorneo { OTTAVI, QUARTI, SEMIFINALE, FINALE }
enum class TipoTorneo { SINGOLO, COPPIE_CASUALI, COPPIE_PREDEFINITE }

object GestoreBattle {
    var mcsSelezionati = mutableStateListOf<Freestyler>()
    var roundsAttuali = mutableStateListOf<Round>()
    var faseAttuale = FaseTorneo.OTTAVI
    var is2v2 = false
    var tipoTorneoAttuale = TipoTorneo.SINGOLO

    fun resetSelezione() { mcsSelezionati.clear(); roundsAttuali.clear(); faseAttuale = FaseTorneo.OTTAVI; is2v2 = false; tipoTorneoAttuale = TipoTorneo.SINGOLO }
    fun determinaFase(numeroPartecipanti: Int): FaseTorneo = when { numeroPartecipanti > 12 -> FaseTorneo.OTTAVI; numeroPartecipanti > 6 -> FaseTorneo.QUARTI; numeroPartecipanti > 3 -> FaseTorneo.SEMIFINALE; else -> FaseTorneo.FINALE }
    fun iniziaTorneo(partecipanti: List<Freestyler>) { is2v2 = false; tipoTorneoAttuale = TipoTorneo.SINGOLO; generaFase(determinaFase(partecipanti.size), partecipanti) }
    fun iniziaTorneo2v2(partecipanti: List<Freestyler>, tipo: TipoTorneo) {
        is2v2 = true; tipoTorneoAttuale = tipo
        val lista = if (tipo == TipoTorneo.COPPIE_CASUALI) partecipanti.shuffled() else partecipanti.toList()
        val coppie = mutableListOf<Freestyler>()

        for (i in 0 until (lista.size / 2) * 2 step 2) {
            val mc1 = lista[i]
            val mc2 = lista[i + 1]

            // CONCATENIAMO GLI URL SEPARANDOLI CON UNA VIRGOLA!
            // Se un mc non ha foto, mettiamo "no_pic" per gestire il fallback dopo
            val url1 = if (mc1.immagineUrl.isNullOrBlank()) "no_pic" else mc1.immagineUrl
            val url2 = if (mc2.immagineUrl.isNullOrBlank()) "no_pic" else mc2.immagineUrl
            val urlCoppia = "$url1,$url2"

            coppie.add(
                Freestyler(
                    id = "${mc1.id}_${mc2.id}",
                    nome = "${mc1.nome} & ${mc2.nome}",
                    immagineUrl = urlCoppia, // Salviamo la stringa concatenata!
                    muretto_id = if (Tema.isBarreFaul) "barre_faul" else "muretto_pg"
                )
            )
        }
        if (lista.size % 2 != 0) coppie.add(lista.last()) // Il povero dispari da solo
        generaFase(determinaFase(coppie.size), coppie)
    }
    fun generaFase(fase: FaseTorneo, partecipanti: List<Freestyler>) {
        faseAttuale = fase; roundsAttuali.clear()
        if (partecipanti.isEmpty()) return
        val shuffled = partecipanti.shuffled(); val total = shuffled.size
        if (total % 2 == 0) {
            for (i in 0 until total step 2) roundsAttuali.add(Round("r_${fase.name}_${i / 2}", (i / 2) + 1, listOf(shuffled[i], shuffled[i + 1])))
        } else {
            if (total >= 3) {
                val num1v1 = (total - 3) / 2
                for (i in 0 until num1v1) roundsAttuali.add(Round("r_${fase.name}_$i", i + 1, listOf(shuffled[i * 2], shuffled[i * 2 + 1])))
                val startRumble = num1v1 * 2
                roundsAttuali.add(Round("r_${fase.name}_$num1v1", num1v1 + 1, listOf(shuffled[startRumble], shuffled[startRumble + 1], shuffled[startRumble + 2])))
            } else roundsAttuali.add(Round("r_${fase.name}_0", 1, listOf(shuffled[0])))
        }
    }
    fun salvaProgresso(context: Context) {
        context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE).edit().putString("fase", faseAttuale.name).putString("tipoTorneo", tipoTorneoAttuale.name).putString("rounds", Gson().toJson(roundsAttuali.toList())).putBoolean("is2v2", is2v2).apply()
    }
    fun haProgresso(context: Context): Boolean = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE).contains("rounds")
    fun caricaProgresso(context: Context) {
        val sharedPref = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE)
        faseAttuale = FaseTorneo.valueOf(sharedPref.getString("fase", "OTTAVI") ?: "OTTAVI")
        tipoTorneoAttuale = TipoTorneo.valueOf(sharedPref.getString("tipoTorneo", "SINGOLO") ?: "SINGOLO")
        is2v2 = sharedPref.getBoolean("is2v2", false)
        sharedPref.getString("rounds", null)?.let { roundsAttuali.clear(); roundsAttuali.addAll(Gson().fromJson(it, object : TypeToken<List<Round>>() {}.type)) }
    }
    fun pulisciSalvataggio(context: Context) { context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE).edit().clear().apply() }
}

enum class ModoMatchmaking(val etichetta: String) {
    UNO_VS_UNO("1 VS 1"),
    DUE_VS_DUE("2 VS 2"),
    RUMBLE_3("1 VS 1 VS 1")
}

object GestoreAllenamento {
    var mcsSelezionatiIds by mutableStateOf(setOf<String>())
    var battleGenerate by mutableStateOf(listOf<List<Freestyler>>()) // Lista di gruppi (ogni gruppo è una battle)
    var modoAttuale by mutableStateOf(ModoMatchmaking.UNO_VS_UNO)
    var staMostrandoRisultati by mutableStateOf(false)

    fun generaMatchmaking(tuttiMcs: List<Freestyler>) {
        // Filtriamo gli MC selezionati dal database globale
        val selezionati = tuttiMcs.filter { mcsSelezionatiIds.contains(it.id) }.shuffled()
        if (selezionati.isEmpty()) return

        val nuoveBattle = mutableListOf<List<Freestyler>>()

        when (modoAttuale) {
            ModoMatchmaking.UNO_VS_UNO -> {
                for (i in 0 until selezionati.size step 2) {
                    val fine = if (i + 2 <= selezionati.size) i + 2 else selezionati.size
                    nuoveBattle.add(selezionati.subList(i, fine))
                }
            }
            ModoMatchmaking.DUE_VS_DUE -> {
                // Gruppi da 4 (Coppia vs Coppia)
                for (i in 0 until selezionati.size step 4) {
                    val fine = if (i + 4 <= selezionati.size) i + 4 else selezionati.size
                    nuoveBattle.add(selezionati.subList(i, fine))
                }
            }
            ModoMatchmaking.RUMBLE_3 -> {
                // Gruppi da 3
                for (i in 0 until selezionati.size step 3) {
                    val fine = if (i + 3 <= selezionati.size) i + 3 else selezionati.size
                    nuoveBattle.add(selezionati.subList(i, fine))
                }
            }
        }
        battleGenerate = nuoveBattle
    }
}

// ─── COMPONENTE BoxMC (Risolti Bug Foto 2v2 e Croce Rossa) ─────────────────────
@Composable
fun BoxMC(
    mc: Freestyler,
    isVincitore: Boolean = false,
    isSconfitto: Boolean = false,
    width: Dp = 100.dp,
    height: Dp = 140.dp, // AUMENTATA L'ALTEZZA QUI!
    coloreBordoCustom: Color? = null
) {
    val colorMatrix = if (isSconfitto) ColorMatrix().apply { setToSaturation(0f) } else null
    val borderColor = coloreBordoCustom ?: when {
        isVincitore -> Color.Green
        isSconfitto -> Color.DarkGray
        else -> Tema.colorePrincipale
    }

    val safeImageUrl = mc.immagineUrl ?: ""

    // Controlliamo se è un URL combinato (2VS2)
    val urlParti = safeImageUrl.split(",")
    val is2v2 = urlParti.size == 2

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .border(4.dp, borderColor, RoundedCornerShape(12.dp))
            .background(Tema.coloreSfondoCard),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (is2v2) {
            // DISEGNAMO LO SPLIT SCREEN!
            Row(modifier = Modifier.fillMaxSize()) {
                val url1 = if (urlParti[0] == "no_pic") R.drawable.no_pic else urlParti[0]
                val url2 = if (urlParti[1] == "no_pic") R.drawable.no_pic else urlParti[1]

                AsyncImage(
                    model = url1,
                    contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    placeholder = painterResource(R.drawable.no_pic),
                    error = painterResource(R.drawable.no_pic),
                    colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null
                )
                // Divisore nero al centro
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                AsyncImage(
                    model = url2,
                    contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    placeholder = painterResource(R.drawable.no_pic),
                    error = painterResource(R.drawable.no_pic),
                    colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null
                )
            }
        } else {
            // MC SINGOLO NORMALE
            val imageModel: Any = if (safeImageUrl.isBlank() || safeImageUrl == "no_pic") R.drawable.no_pic else safeImageUrl
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                placeholder = painterResource(R.drawable.no_pic),
                error = painterResource(R.drawable.no_pic),
                colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.8f)).padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = mc.nome.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        if (isSconfitto) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(color = Color.Red.copy(alpha = 0.8f), start = Offset(0f, 0f), end = Offset(size.width, size.height), strokeWidth = 10f)
                drawLine(color = Color.Red.copy(alpha = 0.8f), start = Offset(size.width, 0f), end = Offset(0f, size.height), strokeWidth = 10f)
            }
        }
    }
}

object CryptoHelper {
    private const val CHIAVE_SEGRETA = "MurettoPG_Secret" // Deve essere di 16 caratteri

    fun encrypt(password: String): String {
        val keySpec = SecretKeySpec(CHIAVE_SEGRETA.toByteArray(), "AES")

        // Creiamo l'array di 16 byte (nasce già riempito di zeri in Kotlin)
        val iv = javax.crypto.spec.IvParameterSpec(ByteArray(16))

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv)

        val encryptedBytes = cipher.doFinal(password.toByteArray())
        return android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
    }
}