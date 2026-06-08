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
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.gotrue.providers.builtin.Email as EmailProvider
import io.ktor.util.InternalAPI
import kotlinx.serialization.SerialName
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

// ─── TEMA E RUOLI ─────────────────────────────────────────────────────────────

enum class MurettoAttivo { PG, BARRE_FAUL, ATENEO, GROSSETO, FORTITUDO}

object Tema {
    var murettoSelezionato by mutableStateOf(MurettoAttivo.PG)
    // Muretto caricato dal DB: se valorizzato, i suoi dati VINCONO sui fallback storici
    var murettoCorrente by mutableStateOf<Muretto?>(null)

    val isBarreFaul get() = murettoSelezionato == MurettoAttivo.BARRE_FAUL
    val isAteneo get() = murettoSelezionato == MurettoAttivo.ATENEO
    val isGrosseto get() = murettoSelezionato == MurettoAttivo.GROSSETO
    val isFortitudo get() = murettoSelezionato == MurettoAttivo.FORTITUDO
    private fun hex(s: String?): Color? = try {
        if (s.isNullOrBlank()) null else Color(android.graphics.Color.parseColor(s))
    } catch (e: Exception) { null }

    // --- FALLBACK dei 4 muretti storici (drawable/colori dentro l'APK) ---
    private val colorePrincipaleFallback: Color get() = when (murettoSelezionato) {
        MurettoAttivo.BARRE_FAUL -> Color(0xFF1E88E5)
        MurettoAttivo.ATENEO -> Color(0xFFFF9800)
        MurettoAttivo.GROSSETO -> Color(0xFFB71C1C)
        MurettoAttivo.PG -> Color(0xFFD32F2F)
        MurettoAttivo.FORTITUDO -> Color(0xFFFFD600) // Giallo Fortitudo
    }

    // --- COLORI (dal DB se c'è il muretto, altrimenti fallback) ---
    val colorePrincipale: Color get() = hex(murettoCorrente?.colore_cornici) ?: colorePrincipaleFallback
    val coloreBottoni: Color get() = hex(murettoCorrente?.colore_bottoni) ?: colorePrincipale

    val coloreSfondo: Color get() = when (murettoSelezionato) {
        MurettoAttivo.BARRE_FAUL -> Color(0xFFDCDCDC)
        else -> Color.Black
    }
    val coloreSfondoCard: Color get() = when (murettoSelezionato) {
        MurettoAttivo.BARRE_FAUL -> Color(0xFFF5F5F5)
        MurettoAttivo.ATENEO -> Color(0xFF1E1E1E)
        MurettoAttivo.GROSSETO -> Color(0xFF111111)
        MurettoAttivo.PG -> Color(0xFF111111)
        MurettoAttivo.FORTITUDO -> Color(0xFF111111)
    }
    val coloreTesto: Color get() = if (murettoSelezionato == MurettoAttivo.BARRE_FAUL) Color.Black else Color.White
    val coloreTestoSecondario: Color get() = if (murettoSelezionato == MurettoAttivo.BARRE_FAUL) Color.DarkGray else Color.Gray

    val gradienteCard: Brush
        get() = when (murettoSelezionato) {
            MurettoAttivo.BARRE_FAUL -> Brush.horizontalGradient(colors = listOf(Color(0xFF002244), Color(0xFF004488)))
            MurettoAttivo.ATENEO -> Brush.horizontalGradient(colors = listOf(Color(0xFF4A2B00), Color(0xFF804D00)))
            MurettoAttivo.GROSSETO -> Brush.horizontalGradient(colors = listOf(Color(0xFF4B0000), Color(0xFF8B0000)))
            MurettoAttivo.PG -> Brush.horizontalGradient(colors = listOf(Color(0xFF3A0000), Color(0xFF00003A)))
            MurettoAttivo.FORTITUDO -> Brush.horizontalGradient(colors = listOf(Color(0xFF3A3A00), Color(0xFF5C4D00)))
        }

    // --- SFONDO GENERALE: drawable di fallback (usato finché non c'è l'URL) ---
    val sfondoGenerale: Int get() = when (murettoSelezionato) {
        MurettoAttivo.BARRE_FAUL -> R.drawable.sfondo_barre_faul
        MurettoAttivo.ATENEO -> R.drawable.sfondo_ateneo
        MurettoAttivo.GROSSETO -> R.drawable.muretto_classico_grosseto
        MurettoAttivo.PG -> R.drawable.sfondo_muretto_classico
        MurettoAttivo.FORTITUDO -> R.drawable.sfondo_fortitudo
    }

    // --- URL dal DB (li useremo per gli sfondi nello stadio 2c) ---
    val sfondoGeneraleUrl: String? get() = murettoCorrente?.sfondo_url
    val sfondoInizialeUrl: String? get() = murettoCorrente?.sfondo_iniziale_url
    val sfondoCardMurettoUrl: String? get() = murettoCorrente?.sfondo_card_muretto_url
    val sfondoCard2v2Url: String? get() = murettoCorrente?.sfondo_card_2v2_url
    val sfondoCardContestUrl: String? get() = murettoCorrente?.sfondo_card_contest_url

    // ID del muretto attivo: preferisci quello DB, altrimenti i 4 storici
    fun ottieniIdMurettoAttivo(): String = murettoCorrente?.id ?: when (murettoSelezionato) {
        MurettoAttivo.BARRE_FAUL -> "2d0f412c-4e9d-4eab-b886-f7a2226d7b9e"
        MurettoAttivo.ATENEO -> "d20af410-652c-4d91-ab62-3aae3b2a8db2"
        MurettoAttivo.GROSSETO -> "34f6c62b-6d81-46ed-9d64-ec0a5ee02e82"
        MurettoAttivo.FORTITUDO -> "22ea8a2f-d45d-40b2-a6ee-841058f12f99"
        MurettoAttivo.PG -> "09fbe1d3-0022-41b8-ba4b-edc887c145a2"
    }

    // Seleziona un muretto del DB: imposta il corrente e mappa l'enum (per i fallback drawable)
    fun selezionaMuretto(m: Muretto?) {
        murettoCorrente = m
        if (m != null) {
            murettoSelezionato = when {
                m.name.contains("Barre", true) -> MurettoAttivo.BARRE_FAUL
                m.name.contains("Ateneo", true) -> MurettoAttivo.ATENEO
                m.name.contains("Grosseto", true) -> MurettoAttivo.GROSSETO
                m.name.contains("Fortitudo", true) -> MurettoAttivo.FORTITUDO
                else -> MurettoAttivo.PG
            }
        }
    }
}

enum class RuoloUtente { NESSUNO, ADMIN, ORGANIZZATORE_MURETTO, ORGANIZZATORE_EVENTI, RAPPER }

// ─── MODELLI DATI ─────────────────────────────────────────────────────────────
@Serializable
data class Amministratore(
    val id: String = "",
    val created_at: String? = null
)

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
    val muretto_id: String? = null,
    val email: String = "",
    val statoRichiesta: Boolean = false   // true = approvato, false = in attesa
)

@Serializable
data class ContestDesign(
    val evento_id: String,
    val tipo_stile: String = "DEFAULT",
    val colore_primario: String? = null,
    val colore_sfondo: String? = null,
    val colore_testo: String? = null,
    val sfondo_custom_url: String? = null,
    val sfondo_card_url: String? = null,
    // PREPARAZIONE PER I VOTI
    val sistema_voti: String? = null,
    val voto_pubblico: Boolean = false,
    val numero_giudici: Int = 0,
    val codici_accesso_giudici: String? = null,
    val codice_accesso_pubblico: String? = null,
    val qr_code_pubblico: String? = null,
    val link_pubblico: String? = null,
    val descrizione_delega: String? = null,
    val delega_completata: Boolean = false
)


@Serializable
data class Evento(
    val id: String = java.util.UUID.randomUUID().toString(),
    val titolo: String = "",
    val location_nome: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val data_ora: String = "",
    val tipo: String = "",
    val prezzo: String = "",
    val immagine_url: String? = null,
    val organizzatore_id: String? = null,
    val stato: String = "in_attesa",
    val scala_pin: Float? = 1.0f,
    val insta: String? = null,
    val maps: String? = null,
    val descrizione: String? = null,
    val deleted_at: String? = null,
    val muretto_id: String? = null, // <- AGGIUNTO PER CAPIRE DI CHI È IL CONTEST
    val contest_design: ContestDesign? = null
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

@Serializable
data class Muretto(
    val id: String = "",
    val name: String = "",
    val meeting_schedule: String? = null,
    val description: String? = null,
    val instagram: String? = null,
    val location: String? = null,
    @SerialName("immagineURL") val immagineURL: String? = null, // logo
    val sfondo_iniziale_url: String? = null,
    val sfondo_url: String? = null,
    val sfondo_card_muretto_url: String? = null,
    val sfondo_card_2v2_url: String? = null,
    val sfondo_card_contest_url: String? = null,
    val colore_cornici: String? = null,
    val colore_bottoni: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val pin_url: String? = null,
    val scala_pin: Float? = 1.0f
)

@Serializable
data class MurettoInsert(
    val name: String,
    val meeting_schedule: String? = null,
    val description: String? = null,
    val instagram: String? = null,
    val location: String? = null,
    @SerialName("immagineURL") val immagineURL: String? = null,
    val sfondo_iniziale_url: String? = null,
    val sfondo_url: String? = null,
    val sfondo_card_muretto_url: String? = null,
    val sfondo_card_2v2_url: String? = null,
    val sfondo_card_contest_url: String? = null,
    val colore_cornici: String? = null,
    val colore_bottoni: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val pin_url: String? = null,
    val scala_pin: Float? = 1.0f
)

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

    // ─── SESSIONE E PROFILO ───────────────────────────────────────────────────

    suspend fun inizializzaSessione() {
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            controllaRuolo()
            fetchEventiPreferiti(user.id)
        }
    }

    suspend fun controllaRuolo() {
        try {
            val user = supabase.auth.currentUserOrNull()
            if (user == null) {
                withContext(Dispatchers.Main) {
                    isAdmin = false
                    ruoloAttuale = RuoloUtente.NESSUNO
                    profiloAttuale = null
                    accountInAttesa = false
                }
                return
            }

            // 1) Sei admin? -> riga in 'amministratori' con il tuo id
            val isAdminDb = supabase.postgrest["amministratori"].select {
                filter { eq("id", user.id) }
            }.decodeList<Amministratore>().isNotEmpty()

            if (isAdminDb) {
                withContext(Dispatchers.Main) {
                    isAdmin = true
                    ruoloAttuale = RuoloUtente.ADMIN
                    accountInAttesa = false
                }
                fetchRichiesteInAttesa()
                fetchEventiInAttesa()
                return
            }

            // 2) Altrimenti leggi il profilo per gli altri ruoli
            val profilo = supabase.postgrest["profili"].select {
                filter { eq("id", user.id) }
            }.decodeList<ProfiloUtente>().firstOrNull()

            withContext(Dispatchers.Main) {
                isAdmin = false
                if (profilo == null) {
                    profiloAttuale = null
                    ruoloAttuale = RuoloUtente.NESSUNO
                    accountInAttesa = false
                } else {
                    profiloAttuale = profilo
                    if (!profilo.statoRichiesta) {
                        accountInAttesa = true
                        ruoloAttuale = RuoloUtente.NESSUNO
                    } else {
                        accountInAttesa = false
                        ruoloAttuale = when (profilo.tipo_account) {
                            "organizzatore_muretto" -> RuoloUtente.ORGANIZZATORE_MURETTO
                            "organizzatore_eventi" -> RuoloUtente.ORGANIZZATORE_EVENTI
                            "rapper" -> RuoloUtente.RAPPER
                            else -> RuoloUtente.NESSUNO
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DatabaseViewModel", "Errore controllaRuolo: ${e.message}")
            withContext(Dispatchers.Main) {
                isAdmin = false
                ruoloAttuale = RuoloUtente.NESSUNO
            }
        }
    }

    // ─── MCS / ROSTER ─────────────────────────────────────────────────────────

    var listaMcsCloud = mutableStateListOf<Freestyler>()
    var tuttiMcsCloud = mutableStateListOf<Freestyler>()
    var staCaricando = mutableStateOf(false)

    suspend fun fetchMcsDalCloud(murettoId: String) {
        try {
            val mcs = supabase.postgrest["mcs"].select {
                filter { eq("muretto_id", murettoId) }
            }.decodeList<Freestyler>()
            withContext(Dispatchers.Main) {
                listaMcsCloud.clear()
                listaMcsCloud.addAll(mcs)
                tuttiMcsCloud.clear()
                tuttiMcsCloud.addAll(mcs)
            }
        } catch (e: Exception) {
            Log.e("DatabaseViewModel", "Errore fetch mcs: ${e.message}")
        }
    }

    suspend fun cercaMcGlobale(nome: String): Freestyler? {
        return try {
            supabase.postgrest["mcs"].select {
                filter { eq("nome", nome) }
            }.decodeList<Freestyler>().firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun inserisciNuovoMc(nome: String, murettoId: String, immagineBytes: ByteArray?): Boolean {
        return try {
            val bucket = supabase.storage["avatar_mcs"]
            var imageUrl: String? = null
            if (immagineBytes != null) {
                val name = "avatar_${UUID.randomUUID()}.jpg"
                bucket.upload(name, immagineBytes, upsert = true)
                imageUrl = bucket.publicUrl(name)
            }
            val mc = Freestyler(id = UUID.randomUUID().toString(), nome = nome, immagineUrl = imageUrl, muretto_id = murettoId)
            supabase.postgrest["mcs"].insert(mc)
            true
        } catch (e: Exception) { false }
    }

    fun fetchMuretti() {
        safeScope.launch(Dispatchers.IO) {
            try {
                val lista = supabase.postgrest["muretti"].select().decodeList<Muretto>()
                withContext(Dispatchers.Main) { murettiCloud.clear(); murettiCloud.addAll(lista) }
            } catch (e: Exception) { android.util.Log.e("MURETTI", "Errore fetch: ${e.message}", e) }
        }
    }

    /** Crea un muretto caricando tutte le immagini sul bucket pubblico 'immagini'.
     *  `immagini` ha chiavi: logo, pin, sfondo_iniziale, sfondo, card_muretto, card_2v2, card_contest. */
    suspend fun creaMuretto(
        nome: String, descrizione: String?, instagram: String?, orari: String?, location: String?,
        lat: Double?, lng: Double?, scalaPin: Float,
        coloreCornici: String?, coloreBottoni: String?,
        immagini: Map<String, ByteArray>
    ): Boolean {
        return try {
            val bucket = supabase.storage["Muretti"]   // <-- bucket dedicato, M maiuscola
            // mappa: chiave logica -> sottocartella nel bucket
            val cartelle = mapOf(
                "logo" to "loghi",
                "pin" to "pin_muretti",
                "sfondo_iniziale" to "schermate_muretti",
                "sfondo" to "sfondi_muretti",
                "card_muretto" to "sfondi_card/muretto_classico",
                "card_2v2" to "sfondi_card/2 VS 2",
                "card_contest" to "sfondi_card/contest"
            )
            suspend fun up(key: String): String? {
                val b = immagini[key] ?: return null
                val cartella = cartelle[key] ?: return null
                val path = "$cartella/${key}_${UUID.randomUUID()}.png"
                bucket.upload(path, b, upsert = true)
                return bucket.publicUrl(path)
            }
            val nuovo = MurettoInsert(
                name = nome.trim(),
                description = descrizione?.ifBlank { null },
                instagram = instagram?.ifBlank { null },
                meeting_schedule = orari?.ifBlank { null },
                location = location?.ifBlank { null },
                lat = lat, lng = lng, scala_pin = scalaPin,
                colore_cornici = coloreCornici, colore_bottoni = coloreBottoni,
                immagineURL = up("logo"),
                pin_url = up("pin"),
                sfondo_iniziale_url = up("sfondo_iniziale"),
                sfondo_url = up("sfondo"),
                sfondo_card_muretto_url = up("card_muretto"),
                sfondo_card_2v2_url = up("card_2v2"),
                sfondo_card_contest_url = up("card_contest")
            )
            supabase.postgrest["muretti"].insert(nuovo)
            fetchMuretti()
            true
        } catch (e: Exception) {
            android.util.Log.e("MURETTI", "Errore crea: ${e.message}", e); false
        }
    }

    suspend fun aggiornaMc(mcId: String, nuovoNome: String, immagineBytes: ByteArray?): Boolean {
        return try {
            val bucket = supabase.storage["avatar_mcs"]
            var imageUrl: String? = null
            if (immagineBytes != null) {
                val name = "avatar_${UUID.randomUUID()}.jpg"
                bucket.upload(name, immagineBytes, upsert = true)
                imageUrl = bucket.publicUrl(name)
            }
            supabase.postgrest["mcs"].update({
                set("nome", nuovoNome)
                if (imageUrl != null) set("immagineUrl", imageUrl)
            }) { filter { eq("id", mcId) } }
            true
        } catch (e: Exception) { false }
    }

    suspend fun eliminaMc(mcId: String): Boolean {
        return try {
            supabase.postgrest["mcs"].delete { filter { eq("id", mcId) } }
            true
        } catch (e: Exception) { false }
    }

    // ─── EVENTI ───────────────────────────────────────────────────────────────

    var eventiInAttesa = mutableStateListOf<Evento>()
    var eventiApprovati = mutableStateListOf<Evento>()
    var murettiCloud = mutableStateListOf<Muretto>()

    suspend fun fetchEventiApprovati() {
        try {
            val eventi = supabase.postgrest["eventi"]
                .select(columns = Columns.raw("*, contest_design(*)")) { filter { eq("stato", "approvato") } }
                .decodeList<Evento>()
            withContext(Dispatchers.Main) {
                eventiApprovati.clear()
                eventiApprovati.addAll(eventi)
            }
        } catch (e: Exception) {
            Log.e("DatabaseViewModel", "Errore fetch eventi approvati: ${e.message}")
        }
    }

    suspend fun fetchEventiInAttesa() {
        try {
            val eventi = supabase.postgrest["eventi"]
                .select(columns = Columns.raw("*, contest_design(*)")) { filter { eq("stato", "in_attesa") } }
                .decodeList<Evento>()
            withContext(Dispatchers.Main) {
                eventiInAttesa.clear()
                eventiInAttesa.addAll(eventi)
            }
        } catch (e: Exception) {
            Log.e("DatabaseViewModel", "Errore fetch eventi in attesa: ${e.message}")
        }
    }

    suspend fun inserisciNuovoEvento(
        titolo: String, locationNome: String, lat: Double, lng: Double, dataOra: String, tipo: String, prezzo: String,
        scalaPin: Float = 1.0f, imageBytes: ByteArray? = null, insta: String? = null, maps: String? = null, descrizione: String? = null,
        isContest: Boolean = false, tipoStile: String = "DEFAULT", colorePrimario: String? = null, coloreSfondo: String? = null, coloreTesto: String? = null,
        sfondoCustomBytes: ByteArray? = null, descrizioneDelega: String? = null
    ): Boolean {
        return try {
            val bucket = supabase.storage["locandine_eventi"]
            var imageUrl: String? = null
            if (imageBytes != null) {
                val name = "locandina_${UUID.randomUUID()}.jpg"; bucket.upload(name, imageBytes, upsert = true); imageUrl = bucket.publicUrl(name)
            }
            var sfondoCustomUrl: String? = null
            if (sfondoCustomBytes != null) {
                val name = "sfondo_contest_${UUID.randomUUID()}.jpg"; bucket.upload(name, sfondoCustomBytes, upsert = true); sfondoCustomUrl = bucket.publicUrl(name)
            }
            val evento = Evento(
                id = UUID.randomUUID().toString(), titolo = titolo, location_nome = locationNome, lat = lat, lng = lng, data_ora = dataOra, tipo = tipo, prezzo = prezzo,
                immagine_url = imageUrl, organizzatore_id = supabase.auth.currentUserOrNull()?.id, stato = "in_attesa", scala_pin = scalaPin, insta = insta, maps = maps, descrizione = descrizione,
                muretto_id = Tema.ottieniIdMurettoAttivo()
            )
            supabase.postgrest["eventi"].insert(evento)
            if (isContest || tipo == "Contest") {
                val design = ContestDesign(
                    evento_id = evento.id, tipo_stile = tipoStile, colore_primario = colorePrimario,
                    colore_sfondo = coloreSfondo, colore_testo = coloreTesto, sfondo_custom_url = sfondoCustomUrl,
                    descrizione_delega = descrizioneDelega, delega_completata = false
                )
                supabase.postgrest["contest_design"].insert(design)
            }
            true
        } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore inserimento evento", e); false }
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

    suspend fun archiviaEvento(eventoId: String): Boolean {
        return try {
            supabase.postgrest["eventi"].update({ set("stato", "concluso") }) { filter { eq("id", eventoId) } }
            withContext(Dispatchers.Main) { eventiApprovati.removeIf { it.id == eventoId } }
            true
        } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore archiviazione evento", e); false }
    }

    suspend fun eliminaEventoDefinitivamente(eventoId: String): Boolean {
        return try {
            supabase.postgrest["eventi"].delete { filter { eq("id", eventoId) } }
            withContext(Dispatchers.Main) { eventiApprovati.removeIf { it.id == eventoId } }
            true
        } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore eliminazione definitiva evento", e); false }
    }

    // ─── PREFERITI ────────────────────────────────────────────────────────────

    var eventiPreferiti = mutableStateListOf<String>()

    suspend fun fetchEventiPreferiti(userId: String) {
        try {
            val prefs = supabase.postgrest["eventi_preferiti"].select {
                filter { eq("user_id", userId) }
            }.decodeList<EventoPreferito>()
            withContext(Dispatchers.Main) {
                eventiPreferiti.clear()
                eventiPreferiti.addAll(prefs.map { it.evento_id })
            }
        } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore fetch preferiti: ${e.message}") }
    }

    fun togglePreferito(eventoId: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        safeScope.launch(Dispatchers.IO) {
            try {
                if (eventiPreferiti.contains(eventoId)) {
                    supabase.postgrest["eventi_preferiti"].delete {
                        filter { eq("user_id", userId); eq("evento_id", eventoId) }
                    }
                    withContext(Dispatchers.Main) { eventiPreferiti.remove(eventoId) }
                } else {
                    val pref = EventoPreferito(user_id = userId, evento_id = eventoId)
                    supabase.postgrest["eventi_preferiti"].insert(pref)
                    withContext(Dispatchers.Main) { eventiPreferiti.add(eventoId) }
                }
            } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore togglePreferito: ${e.message}") }
        }
    }

    // ─── ACCOUNT E RICHIESTE ──────────────────────────────────────────────────

    var isAdmin by mutableStateOf(false)
    var ruoloAttuale by mutableStateOf(RuoloUtente.NESSUNO)
    var profiloAttuale by mutableStateOf<ProfiloUtente?>(null)
    var accountInAttesa by mutableStateOf(false)
    var richiesteInAttesa = mutableStateListOf<ProfiloUtente>()

    suspend fun eseguiRegistrazioneDiretta(
        nome: String, cognome: String, nomeArte: String,
        email: String, passwordTemp: String, telefono: String,
        tipoAccount: String, murettoId: String?
    ): Boolean {
        return try {
            val authResponse = supabase.auth.signUpWith(EmailProvider) {
                this.email = email.trim(); this.password = passwordTemp
            }
            val userId = authResponse?.id ?: throw Exception("Errore creazione Auth")
            val autoApprovato = tipoAccount == "rapper"
            val nuovoProfilo = ProfiloUtente(id = userId, nome = nome.trim(), cognome = cognome.trim(), nome_arte = nomeArte.trim(), telefono = telefono.trim(), tipo_account = tipoAccount, muretto_id = murettoId, email = email.trim(), statoRichiesta = autoApprovato)
            supabase.postgrest["profili"].insert(nuovoProfilo)
            true
        } catch (e: Exception) { Log.e("REGISTRAZIONE", "Errore: ${e.message}", e); false }
    }

    suspend fun accettaRichiesta(profilo: ProfiloUtente): Boolean {
        return try {
            supabase.postgrest["profili"].update({ set("statoRichiesta", true) }) { filter { eq("id", profilo.id) } }
            withContext(Dispatchers.Main) { richiesteInAttesa.removeIf { it.id == profilo.id } }
            true
        } catch (e: Exception) { Log.e("DEBUG_ACCETTA", "Errore: ${e.message}"); false }
    }

    suspend fun rifiutaRichiesta(profiloId: String): Boolean {
        return try {
            supabase.postgrest["profili"].delete { filter { eq("id", profiloId) } }
            withContext(Dispatchers.Main) { richiesteInAttesa.removeIf { it.id == profiloId } }
            true
        } catch (e: Exception) { Log.e("DEBUG_RIFIUTA", "Errore: ${e.message}"); false }
    }

    fun fetchRichiesteInAttesa() {
        safeScope.launch(Dispatchers.IO) {
            try {
                val richieste = supabase.postgrest["profili"].select { filter { eq("statoRichiesta", false) } }.decodeList<ProfiloUtente>()
                withContext(Dispatchers.Main) {
                    richiesteInAttesa.clear(); richiesteInAttesa.addAll(richieste)
                }
            } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore richieste", e) }
        }
    }

    // ─── CONTEST DESIGN / VERDETTI ────────────────────────────────────────────

    suspend fun salvaImpostazioniVerdetti(eventoId: String, sistemaVoti: String, votoPubblico: Boolean, numeroGiudici: Int, codGiudici: String?, codPubblico: String?, qrPubblico: String?, linkPubblico: String?) {
        try {
            supabase.postgrest["contest_design"].update({
                set("sistema_voti", sistemaVoti); set("voto_pubblico", votoPubblico); set("numero_giudici", numeroGiudici)
                set("codici_accesso_giudici", codGiudici); set("codice_accesso_pubblico", codPubblico); set("qr_code_pubblico", qrPubblico); set("link_pubblico", linkPubblico)
            }) { filter { eq("evento_id", eventoId) } }
        } catch (e: Exception) { Log.e("DatabaseViewModel", "Errore salvataggio verdetti", e) }
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
@Serializable
data class Round(val id: String, val numero: Int, var partecipanti: List<Freestyler>, var completato: Boolean = false, var vincitoreId: String? = null)enum class FaseTorneo { OTTAVI, QUARTI, SEMIFINALE, FINALE }
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