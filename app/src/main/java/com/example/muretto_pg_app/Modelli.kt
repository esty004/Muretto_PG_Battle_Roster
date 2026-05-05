package com.example.muretto_pg_app

import android.content.Context
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
import kotlinx.serialization.Serializable
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

// ─── TEMA E RUOLI ─────────────────────────────────────────────────────────────

object Tema {
    var isBarreFaul by mutableStateOf(false)

    val colorePrincipale: Color get() = if (isBarreFaul) Color(0xFF1E88E5) else Color(0xFFD32F2F)
    val coloreSfondo: Color get() = if (isBarreFaul) Color(0xFFDCDCDC) else Color.Black
    val coloreSfondoCard: Color get() = if (isBarreFaul) Color(0xFFF5F5F5) else Color(0xFF111111)
    val coloreTesto: Color get() = if (isBarreFaul) Color.Black else Color.White
    val coloreTestoSecondario: Color get() = if (isBarreFaul) Color.DarkGray else Color.Gray

    val gradienteCard: Brush
        get() = if (isBarreFaul) {
            Brush.horizontalGradient(colors = listOf(Color(0xFF002244), Color(0xFF004488)))
        } else {
            Brush.horizontalGradient(colors = listOf(Color(0xFF3A0000), Color(0xFF00003A)))
        }
}

enum class RuoloUtente { NESSUNO, ADMIN, ORGANIZZATORE_MURETTO, ORGANIZZATORE_EVENTI, RAPPER }

// ─── MODELLI DATI ─────────────────────────────────────────────────────────────

@Serializable
data class Freestyler(
    val id: String = "",
    val nome: String = "",
    val immagineUrl: String = "",
    val muretto: String = ""
)

@Serializable
data class ProfiloUtente(
    val id: String = "",
    val nome: String = "",
    val cognome: String = "",
    val nome_arte: String = "",
    val telefono: String = "",
    val tipo_account: String = "",
    val muretto: String? = null
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
    val muretto: String? = null,
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
    val scala_pin: Float = 1.0f // <--- NUOVO CAMPO
)

// ─── DATABASE ─────────────────────────────────────────────────────────────────

object DatabaseMcs {
    private val supabaseUrl = "https://bvzwnuxljhbxeplhbjze.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2endudXhsamhieGVwbGhianplIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzczNzUwNjQsImV4cCI6MjA5Mjk1MTA2NH0.4d9ZS_87F7LcelnJMBAdbDkbXOeE2xQ7rGSehFHtMs8"

    val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
        install(Postgrest)
        install(Auth)
        install(Storage)
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

    fun inizializzaSessione() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                kotlinx.coroutines.delay(500)
                val user = supabase.auth.currentUserOrNull()
                if (user != null) controllaRuolo()
            } catch (e: Exception) { e.printStackTrace() }
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

    fun controllaAdmin() { CoroutineScope(Dispatchers.IO).launch { controllaRuolo() } }

    fun fetchMcsDalCloud(nomeMuretto: String) {
        staCaricando.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scaricati = supabase.postgrest["mcs"].select().decodeList<Freestyler>()
                withContext(Dispatchers.Main) {
                    tuttiMcsCloud.clear()
                    tuttiMcsCloud.addAll(scaricati)
                    val filtrati = scaricati.filter { it.muretto == nomeMuretto }
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

    suspend fun inserisciNuovoMc(nome: String, muretto: String, imageBytes: ByteArray?): Boolean {
        return try {
            var imageUrl = ""
            if (imageBytes != null) {
                val fileName = "mc_${UUID.randomUUID()}.jpg"
                val bucket = supabase.storage["immagini"]
                bucket.upload(fileName, imageBytes, upsert = true)
                imageUrl = bucket.publicUrl(fileName)
            }
            val esistenti = supabase.postgrest["mcs"].select { filter { eq("muretto", muretto) } }.decodeList<Freestyler>()
            val nuovoId = if (muretto == "barre_faul") {
                val maxNum = esistenti.mapNotNull { it.id.replace("bf", "").toIntOrNull() }.maxOrNull() ?: 0
                "bf${maxNum + 1}"
            } else {
                val maxNum = esistenti.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0
                (maxNum + 1).toString()
            }
            val nuovoMc = Freestyler(id = nuovoId, nome = nome, immagineUrl = imageUrl, muretto = muretto)
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
        dataOra: String, tipo: String, prezzo: String, scalaPin: Float, imageBytes: ByteArray?
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
                titolo = titolo,
                location_nome = locationNome,
                lat = lat,
                lng = lng,
                data_ora = dataOra,
                tipo = tipo,
                prezzo = prezzo,
                immagine_url = imageUrl,
                organizzatore_id = user.id,
                stato = "in_attesa",
                scala_pin = scalaPin
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
            val richiesta = RichiestaAccount(
                id = UUID.randomUUID().toString(), nome = nome, cognome = cognome, nome_arte = nomeArte, email = email, password_temp = passwordTemp, telefono = telefono, tipo_account = tipoAccount, muretto = muretto, stato = "in_attesa"
            )
            supabase.postgrest["richieste_account"].insert(richiesta)
            true
        } catch (e: Exception) { false }
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val richieste = supabase.postgrest["richieste_account"].select { filter { eq("stato", "in_attesa") } }.decodeList<RichiestaAccount>()
                withContext(Dispatchers.Main) {
                    richiesteInAttesa.clear()
                    richiesteInAttesa.addAll(richieste)
                }
            } catch (e: Exception) { }
        }
    }

    fun fetchEventiInAttesa() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val eventi = supabase.postgrest["eventi"].select { filter { eq("stato", "in_attesa") } }.decodeList<Evento>()
                withContext(Dispatchers.Main) {
                    eventiInAttesa.clear()
                    eventiInAttesa.addAll(eventi)
                }
            } catch (e: Exception) { }
        }
    }

    fun fetchEventiApprovati() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val eventi = supabase.postgrest["eventi"].select { filter { eq("stato", "approvato") } }.decodeList<Evento>()
                withContext(Dispatchers.Main) {
                    eventiApprovati.clear()
                    eventiApprovati.addAll(eventi)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun accettaRichiesta(richiesta: RichiestaAccount): Boolean {
        return try {
            val serviceRoleKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2endudXhsamhieGVwbGhianplIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NzM3NTA2NCwiZXhwIjoyMDkyOTUxMDY0fQ.HdIa06E9UVn30zyO9cL6sR_kODfoJJ5NHlZN4VHgoe8"
            val esito = withContext(Dispatchers.IO) {
                val url = URL("https://bvzwnuxljhbxeplhbjze.supabase.co/auth/v1/admin/users")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", serviceRoleKey)
                conn.setRequestProperty("Authorization", "Bearer $serviceRoleKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val body = """{"email":"${richiesta.email}","password":"${richiesta.password_temp}","email_confirm":true}"""
                conn.outputStream.use { it.write(body.toByteArray()) }

                val codice = conn.responseCode
                conn.disconnect()
                codice in 200..299
            }
            if (!esito) return false

            supabase.postgrest["richieste_account"].update({ set("stato", "accettata") }) { filter { eq("id", richiesta.id) } }
            withContext(Dispatchers.Main) { richiesteInAttesa.removeIf { it.id == richiesta.id } }
            true
        } catch (e: Exception) { false }
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
        for (i in 0 until (lista.size / 2) * 2 step 2) coppie.add(Freestyler("${lista[i].id}_${lista[i + 1].id}", "${lista[i].nome} & ${lista[i + 1].nome}", "local_2v2", if (Tema.isBarreFaul) "barre_faul" else "muretto_pg"))
        if (lista.size % 2 != 0) coppie.add(lista.last())
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

object GestoreAllenamento {
    var tabSelezionata by mutableIntStateOf(0)
    var mcsSelezionatiIds by mutableStateOf(setOf<String>())
    var mostraRisultatiMatchmaking by mutableStateOf(false)
    var battleGenerate by mutableStateOf(listOf<Pair<Freestyler, Freestyler>>())
    var mcSingolo by mutableStateOf<Freestyler?>(null)
    var testoRicerca by mutableStateOf("")
}

object DatiAllenamento {
    private fun fetchValori(context: Context, rawResId: Int): List<String> = try { Gson().fromJson(context.resources.openRawResource(rawResId).bufferedReader().use { it.readText() }, object : TypeToken<List<String>>() {}.type) } catch (e: Exception) { emptyList() }
    fun caricaArgomenti(context: Context): List<String> = fetchValori(context, R.raw.topics)
    fun caricaModalita(context: Context): List<String> = fetchValori(context, R.raw.modes)
    fun caricaParole(context: Context): List<String> = fetchValori(context, R.raw.common_words)
}

// ─── COMPONENTE BoxMC (Risolti Bug Foto 2v2 e Croce Rossa) ─────────────────────
@Composable
fun BoxMC(
    mc: Freestyler,
    isVincitore: Boolean = false,
    isSconfitto: Boolean = false,
    width: Dp = 100.dp,
    height: Dp = 130.dp,
    coloreBordoCustom: Color? = null
) {
    val colorMatrix = if (isSconfitto) ColorMatrix().apply { setToSaturation(0f) } else null
    val borderColor = coloreBordoCustom ?: when {
        isVincitore -> Color.Green
        isSconfitto -> Color.DarkGray
        else -> Tema.colorePrincipale
    }

    val is2v2 = mc.immagineUrl == "local_2v2"
    val imageModel: Any = when {
        is2v2 -> if (Tema.isBarreFaul) R.drawable.due_contro_due_barre_faul else R.drawable.due_contro_due
        mc.immagineUrl.isBlank() -> R.drawable.no_pic
        else -> mc.immagineUrl
    }

    val scaleMode = if (is2v2) ContentScale.Fit else ContentScale.Crop

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .border(4.dp, borderColor, RoundedCornerShape(12.dp))
            .background(Tema.coloreSfondoCard),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = scaleMode,
            alignment = Alignment.TopCenter,
            placeholder = painterResource(R.drawable.no_pic),
            error = painterResource(R.drawable.no_pic),
            colorFilter = if (colorMatrix != null) ColorFilter.colorMatrix(colorMatrix) else null
        )

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