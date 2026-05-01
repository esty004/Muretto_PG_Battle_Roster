package com.example.muretto_pg_app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// ─── TEMA ────────────────────────────────────────────────────────────────────

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

// ─── RUOLI UTENTE ─────────────────────────────────────────────────────────────

enum class RuoloUtente {
    NESSUNO,
    ADMIN,
    ORGANIZZATORE_MURETTO,
    ORGANIZZATORE_EVENTI
}

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

// ─── DATABASE ─────────────────────────────────────────────────────────────────

object DatabaseMcs {
    private val supabaseUrl = "https://bvzwnuxljhbxeplhbjze.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2endudXhsamhieGVwbGhianplIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzczNzUwNjQsImV4cCI6MjA5Mjk1MTA2NH0.4d9ZS_87F7LcelnJMBAdbDkbXOeE2xQ7rGSehFHtMs8"

    val supabase = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey
    ) {
        install(Postgrest)
        install(Auth)
        install(Storage)
    }

    var listaMcsCloud = mutableStateListOf<Freestyler>()
    var tuttiMcsCloud = mutableListOf<Freestyler>()
    var staCaricando = mutableStateOf(false)

    // Stato ruolo utente corrente
    var isAdmin by mutableStateOf(false)
    var ruoloAttuale by mutableStateOf(RuoloUtente.NESSUNO)
    var profiloAttuale by mutableStateOf<ProfiloUtente?>(null)
    var richiesteInAttesa = mutableStateListOf<RichiestaAccount>()

    // Ripristina la sessione al riavvio dell'app
    fun inizializzaSessione() {
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            controllaRuolo()
        }
    }

    fun controllaRuolo() {
        val user = supabase.auth.currentUserOrNull() ?: run {
            isAdmin = false
            ruoloAttuale = RuoloUtente.NESSUNO
            profiloAttuale = null
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Controlla prima se è admin
                val adminList = supabase.postgrest["amministratori"].select {
                    filter { eq("id", user.id) }
                }.data

                if (adminList != "[]") {
                    withContext(Dispatchers.Main) {
                        isAdmin = true
                        ruoloAttuale = RuoloUtente.ADMIN
                    }
                    fetchRichiesteInAttesa()
                    return@launch
                }

                // Altrimenti controlla il profilo
                val profiliJson = supabase.postgrest["profili"].select {
                    filter { eq("id", user.id) }
                }.decodeList<ProfiloUtente>()

                withContext(Dispatchers.Main) {
                    isAdmin = false
                    if (profiliJson.isNotEmpty()) {
                        val profilo = profiliJson[0]
                        profiloAttuale = profilo
                        ruoloAttuale = when (profilo.tipo_account) {
                            "organizzatore_muretto" -> RuoloUtente.ORGANIZZATORE_MURETTO
                            "organizzatore_eventi" -> RuoloUtente.ORGANIZZATORE_EVENTI
                            else -> RuoloUtente.NESSUNO
                        }
                    } else {
                        ruoloAttuale = RuoloUtente.NESSUNO
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isAdmin = false
                    ruoloAttuale = RuoloUtente.NESSUNO
                }
            }
        }
    }

    // Alias per compatibilità con il codice esistente
    fun controllaAdmin() = controllaRuolo()

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
                e.printStackTrace()
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
            val esistenti = supabase.postgrest["mcs"].select {
                filter { eq("muretto", muretto) }
            }.decodeList<Freestyler>()
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
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Invia richiesta di registrazione (senza creare account Auth)
    suspend fun inviaRichiestaAccount(
        nome: String, cognome: String, nomeArte: String,
        email: String, passwordTemp: String, telefono: String,
        tipoAccount: String, muretto: String?
    ): Boolean {
        return try {
            val richiesta = RichiestaAccount(
                id = UUID.randomUUID().toString(),
                nome = nome,
                cognome = cognome,
                nome_arte = nomeArte,
                email = email,
                password_temp = passwordTemp,
                telefono = telefono,
                tipo_account = tipoAccount,
                muretto = muretto,
                stato = "in_attesa"
            )
            supabase.postgrest["richieste_account"].insert(richiesta)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Fetch richieste in attesa per admin
    fun fetchRichiesteInAttesa() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val richieste = supabase.postgrest["richieste_account"].select {
                    filter { eq("stato", "in_attesa") }
                }.decodeList<RichiestaAccount>()
                withContext(Dispatchers.Main) {
                    richiesteInAttesa.clear()
                    richiesteInAttesa.addAll(richieste)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Accetta richiesta: crea account Auth (il trigger SQL crea il profilo) + aggiorna stato
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

                // email_confirm: false → Supabase invia la mail di conferma
                val body = """{"email":"${richiesta.email}","password":"${richiesta.password_temp}","email_confirm":false}"""
                conn.outputStream.use { it.write(body.toByteArray()) }

                val codice = conn.responseCode
                conn.disconnect()
                codice in 200..299
            }

            if (!esito) return false

            supabase.postgrest["richieste_account"].update(
                { set("stato", "accettata") }
            ) { filter { eq("id", richiesta.id) } }

            withContext(Dispatchers.Main) {
                richiesteInAttesa.removeIf { it.id == richiesta.id }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Rifiuta richiesta
    suspend fun rifiutaRichiesta(richiestaId: String): Boolean {
        return try {
            supabase.postgrest["richieste_account"].update(
                { set("stato", "rifiutata") }
            ) { filter { eq("id", richiestaId) } }
            withContext(Dispatchers.Main) {
                richiesteInAttesa.removeIf { it.id == richiestaId }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

// ─── TORNEO ───────────────────────────────────────────────────────────────────

data class Round(
    val id: String,
    val numero: Int,
    var partecipanti: List<Freestyler>,
    var completato: Boolean = false,
    var vincitoreId: String? = null
)

enum class FaseTorneo { OTTAVI, QUARTI, SEMIFINALE, FINALE }
enum class TipoTorneo { SINGOLO, COPPIE_CASUALI, COPPIE_PREDEFINITE }

object GestoreBattle {
    var mcsSelezionati = mutableStateListOf<Freestyler>()
    var roundsAttuali = mutableStateListOf<Round>()
    var faseAttuale = FaseTorneo.OTTAVI
    var is2v2 = false
    var tipoTorneoAttuale = TipoTorneo.SINGOLO

    fun resetSelezione() {
        mcsSelezionati.clear()
        roundsAttuali.clear()
        faseAttuale = FaseTorneo.OTTAVI
        is2v2 = false
        tipoTorneoAttuale = TipoTorneo.SINGOLO
    }

    fun determinaFase(numeroPartecipanti: Int): FaseTorneo {
        return when {
            numeroPartecipanti > 12 -> FaseTorneo.OTTAVI
            numeroPartecipanti > 6 -> FaseTorneo.QUARTI
            numeroPartecipanti > 3 -> FaseTorneo.SEMIFINALE
            else -> FaseTorneo.FINALE
        }
    }

    fun iniziaTorneo(partecipanti: List<Freestyler>) {
        is2v2 = false
        tipoTorneoAttuale = TipoTorneo.SINGOLO
        val faseIniziale = determinaFase(partecipanti.size)
        generaFase(faseIniziale, partecipanti)
    }

    fun iniziaTorneo2v2(partecipanti: List<Freestyler>, tipo: TipoTorneo) {
        is2v2 = true
        tipoTorneoAttuale = tipo
        val lista = if (tipo == TipoTorneo.COPPIE_CASUALI) partecipanti.shuffled() else partecipanti.toList()
        val coppie = mutableListOf<Freestyler>()
        for (i in 0 until (lista.size / 2) * 2 step 2) {
            coppie.add(
                Freestyler(
                    id = "${lista[i].id}_${lista[i + 1].id}",
                    nome = "${lista[i].nome} & ${lista[i + 1].nome}",
                    immagineUrl = "local_2v2",
                    muretto = if (Tema.isBarreFaul) "barre_faul" else "muretto_pg"
                )
            )
        }
        if (lista.size % 2 != 0) coppie.add(lista.last())
        val faseIniziale = determinaFase(coppie.size)
        generaFase(faseIniziale, coppie)
    }

    fun generaFase(fase: FaseTorneo, partecipanti: List<Freestyler>) {
        faseAttuale = fase
        roundsAttuali.clear()
        if (partecipanti.isEmpty()) return
        val shuffled = partecipanti.shuffled()
        val total = shuffled.size
        if (total % 2 == 0) {
            for (i in 0 until total step 2) {
                roundsAttuali.add(Round("r_${fase.name}_${i / 2}", (i / 2) + 1, listOf(shuffled[i], shuffled[i + 1])))
            }
        } else {
            if (total >= 3) {
                val num1v1 = (total - 3) / 2
                for (i in 0 until num1v1) {
                    roundsAttuali.add(Round("r_${fase.name}_$i", i + 1, listOf(shuffled[i * 2], shuffled[i * 2 + 1])))
                }
                val startRumble = num1v1 * 2
                roundsAttuali.add(Round("r_${fase.name}_$num1v1", num1v1 + 1, listOf(shuffled[startRumble], shuffled[startRumble + 1], shuffled[startRumble + 2])))
            } else {
                roundsAttuali.add(Round("r_${fase.name}_0", 1, listOf(shuffled[0])))
            }
        }
    }

    fun salvaProgresso(context: Context) {
        val sharedPref = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        sharedPref.edit().apply {
            putString("fase", faseAttuale.name)
            putString("tipoTorneo", tipoTorneoAttuale.name)
            putString("rounds", gson.toJson(roundsAttuali.toList()))
            putBoolean("is2v2", is2v2)
            apply()
        }
    }

    fun haProgresso(context: Context): Boolean =
        context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE).contains("rounds")

    fun caricaProgresso(context: Context) {
        val sharedPref = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        faseAttuale = FaseTorneo.valueOf(sharedPref.getString("fase", "OTTAVI") ?: "OTTAVI")
        tipoTorneoAttuale = TipoTorneo.valueOf(sharedPref.getString("tipoTorneo", "SINGOLO") ?: "SINGOLO")
        is2v2 = sharedPref.getBoolean("is2v2", false)
        val roundsJson = sharedPref.getString("rounds", null)
        if (roundsJson != null) {
            val type = object : TypeToken<List<Round>>() {}.type
            val list: List<Round> = gson.fromJson(roundsJson, type)
            roundsAttuali.clear()
            roundsAttuali.addAll(list)
        }
    }

    fun pulisciSalvataggio(context: Context) {
        context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE).edit().clear().apply()
    }
}

// ─── ALLENAMENTO ──────────────────────────────────────────────────────────────

object GestoreAllenamento {
    var tabSelezionata by mutableIntStateOf(0)
    var mcsSelezionatiIds by mutableStateOf(setOf<String>())
    var mostraRisultatiMatchmaking by mutableStateOf(false)
    var battleGenerate by mutableStateOf(listOf<Pair<Freestyler, Freestyler>>())
    var mcSingolo by mutableStateOf<Freestyler?>(null)
    var testoRicerca by mutableStateOf("")
}

object DatiAllenamento {
    private fun fetchValori(context: Context, rawResId: Int): List<String> {
        return try {
            val json = context.resources.openRawResource(rawResId).bufferedReader().use { it.readText() }
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun caricaArgomenti(context: Context): List<String> = fetchValori(context, R.raw.topics)
    fun caricaModalita(context: Context): List<String> = fetchValori(context, R.raw.modes)
    fun caricaParole(context: Context): List<String> = fetchValori(context, R.raw.common_words)
}

// ─── COMPONENTE BoxMC ─────────────────────────────────────────────────────────

@Composable
fun BoxMC(
    mc: Freestyler,
    isVincitore: Boolean = false,
    isSconfitto: Boolean = false,
    width: Dp = 100.dp,
    height: Dp = 130.dp
) {
    val colorMatrix = if (isSconfitto) ColorMatrix().apply { setToSaturation(0f) } else null
    val borderColor = when {
        isVincitore -> Color.Green
        isSconfitto -> Color.DarkGray
        else -> Tema.colorePrincipale
    }
    val imageModel: Any = when {
        mc.immagineUrl == "local_2v2" -> if (Tema.isBarreFaul) R.drawable.due_contro_due_barre_faul else R.drawable.due_contro_due
        mc.immagineUrl.isBlank() -> R.drawable.no_pic
        else -> mc.immagineUrl
    }
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, borderColor, RoundedCornerShape(12.dp))
            .background(Tema.coloreSfondoCard),
        contentAlignment = Alignment.BottomCenter
    ) {
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mc.nome.uppercase(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}