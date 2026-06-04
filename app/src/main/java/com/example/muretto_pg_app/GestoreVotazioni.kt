package com.example.muretto_pg_app

import androidx.compose.runtime.mutableStateListOf
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.util.UUID

// ─── MODELLI TABELLE ──────────────────────────────────────────────────────────
@Serializable
data class OpzioneVoto(val id: String, val nome: String)

@Serializable
data class SessioneVoto(
    val id: String,
    val evento_id: String,
    val round_id: String,
    val etichetta: String = "Match",
    val opzioni: List<OpzioneVoto> = emptyList(),
    val stato: String = "attesa"   // 'attesa' | 'in_corso' | 'chiusa'
)

@Serializable
data class PresenzaRow(
    val evento_id: String,
    val partecipante_id: String,
    val ruolo: String,             // 'giudice' | 'spettatore'
    val nome: String? = null,
    val codice: String? = null,
    val last_seen: String? = null  // ISO timestamptz
)

@Serializable
data class VotoRow(
    val sessione_id: String,
    val partecipante_id: String,
    val ruolo: String,
    val scelta: String             // id lato | 'spareggio'
)

// ─── RISULTATO DEI VERDETTI (motore "impeccabile") ────────────────────────────
data class RisultatoVerdetti(
    val verdettiGiudici: List<Pair<String, String>>, // (nomeGiudice, sceltaId | 'spareggio')
    val verdettoSpettatori: String?,                 // lato vincente tra gli spettatori (o null)
    val conteggioPerLato: Map<String, Int>,          // lato -> n. verdetti a favore
    val vincitoreId: String?,                        // null = pareggio → serve spareggio
    val spareggio: Boolean
)

/**
 * Sessione di voto live, sincronizzata via polling.
 * Tiene in memoria lo stato locale; le schermate chiamano i metodi suspend in loop (LaunchedEffect).
 */
object GestoreVotazioni {

    const val SOGLIA_PRESENZA_SEC = 20L  // oltre questo silenzio, il partecipante è "uscito"

    val presenti = mutableStateListOf<PresenzaRow>()
    val voti = mutableStateListOf<VotoRow>()
    var sessioneCorrente: SessioneVoto? = null

    // identità di QUESTO dispositivo nella sessione (rigenerata a ogni ingresso = anti doppio-voto)
    var mioId: String = UUID.randomUUID().toString()
    var mioRuolo: String = "spettatore"

    fun nuovaIdentita(ruolo: String) { mioId = UUID.randomUUID().toString(); mioRuolo = ruolo }

    // ── PRESENZA (heartbeat) ──────────────────────────────────────────────────
    suspend fun battitoPresenza(supabase: SupabaseClient, eventoId: String, nome: String?, codice: String?) {
        try {
            supabase.postgrest["contest_presenza"].upsert(
                PresenzaRow(eventoId, mioId, mioRuolo, nome, codice, OffsetDateTime.now().toString())
            )
        } catch (e: Exception) { android.util.Log.e("VOTO", "battito: ${e.message}") }
    }

    suspend fun esci(supabase: SupabaseClient, eventoId: String) {
        try {
            supabase.postgrest["contest_presenza"].delete { filter { eq("evento_id", eventoId); eq("partecipante_id", mioId) } }
        } catch (e: Exception) { }
    }

    suspend fun aggiornaPresenti(supabase: SupabaseClient, eventoId: String) {
        try {
            val lista = supabase.postgrest["contest_presenza"].select { filter { eq("evento_id", eventoId) } }.decodeList<PresenzaRow>()
            withContext(Dispatchers.Main) { presenti.clear(); presenti.addAll(lista) }
        } catch (e: Exception) { }
    }

    private fun isRecente(iso: String?): Boolean {
        if (iso == null) return false
        return try {
            val t = OffsetDateTime.parse(iso).toInstant()
            java.time.Instant.now().epochSecond - t.epochSecond <= SOGLIA_PRESENZA_SEC
        } catch (e: Exception) { false }
    }

    fun giudiciPresenti(): List<PresenzaRow> = presenti.filter { it.ruolo == "giudice" && isRecente(it.last_seen) }
    fun spettatoriPresentiIds(): Set<String> = presenti.filter { it.ruolo == "spettatore" && isRecente(it.last_seen) }.map { it.partecipante_id }.toSet()

    // ── SESSIONE ────────────────────────────────────────────────────────────────
    suspend fun creaSessione(supabase: SupabaseClient, eventoId: String, roundId: String, etichetta: String, opzioni: List<OpzioneVoto>): SessioneVoto? {
        return try {
            val s = SessioneVoto(UUID.randomUUID().toString(), eventoId, roundId, etichetta, opzioni, "in_corso")
            supabase.postgrest["contest_sessione_voto"].insert(s)
            sessioneCorrente = s
            s
        } catch (e: Exception) { android.util.Log.e("VOTO", "creaSessione: ${e.message}"); null }
    }

    /** Polling: trova la sessione attiva (in_corso) per il round, se esiste (per giudici/spettatori). */
    suspend fun trovaSessioneAttiva(supabase: SupabaseClient, eventoId: String, roundId: String): SessioneVoto? {
        return try {
            val lista = supabase.postgrest["contest_sessione_voto"].select {
                filter { eq("evento_id", eventoId); eq("round_id", roundId); eq("stato", "in_corso") }
            }.decodeList<SessioneVoto>()
            sessioneCorrente = lista.firstOrNull()
            sessioneCorrente
        } catch (e: Exception) { null }
    }

    suspend fun chiudiSessione(supabase: SupabaseClient, sessioneId: String) {
        try {
            supabase.postgrest["contest_sessione_voto"].update({ set("stato", "chiusa") }) { filter { eq("id", sessioneId) } }
        } catch (e: Exception) { }
    }

    // ── VOTO ────────────────────────────────────────────────────────────────────
    suspend fun vota(supabase: SupabaseClient, sessioneId: String, scelta: String) {
        try {
            supabase.postgrest["contest_voto"].upsert(VotoRow(sessioneId, mioId, mioRuolo, scelta))
        } catch (e: Exception) { android.util.Log.e("VOTO", "vota: ${e.message}") }
    }

    /** L'organizzatore può votare al posto di un giudice assente (id sintetico). */
    suspend fun votaPerGiudiceAssente(supabase: SupabaseClient, sessioneId: String, idGiudice: String, scelta: String) {
        try {
            supabase.postgrest["contest_voto"].upsert(VotoRow(sessioneId, "ASSENTE_$idGiudice", "giudice", scelta))
        } catch (e: Exception) { }
    }

    suspend fun aggiornaVoti(supabase: SupabaseClient, sessioneId: String) {
        try {
            val lista = supabase.postgrest["contest_voto"].select { filter { eq("sessione_id", sessioneId) } }.decodeList<VotoRow>()
            withContext(Dispatchers.Main) { voti.clear(); voti.addAll(lista) }
        } catch (e: Exception) { }
    }

    // ── MOTORE DEI VERDETTI ──────────────────────────────────────────────────────
    /**
     * Calcola il risultato della sessione corrente.
     * Regole:
     *  - Ogni giudice presente (incluso l'organizzatore) = 1 verdetto per il lato scelto.
     *  - Gli spettatori PRESENTI (heartbeat recente) contano come 1 SOLO verdetto collettivo:
     *    il lato più votato tra loro. Chi è uscito (heartbeat scaduto) NON conta: deve rientrare e rivotare.
     *  - Vince il lato con più verdetti. Pareggio ⇒ spareggio.
     * @param nomiGiudici mappa partecipante_id → nome (per il dettaglio); l'organizzatore è già fra i voti giudice.
     */
    fun calcolaRisultato(votoPubblicoAttivo: Boolean, nomiGiudici: Map<String, String>): RisultatoVerdetti {
        val conteggio = linkedMapOf<String, Int>()
        val dettaglio = mutableListOf<Pair<String, String>>()

        // verdetti dei giudici (conta solo chi è presente, + i voti "ASSENTE_" inseriti dall'organizzatore)
        val idGiudiciPresenti = giudiciPresenti().map { it.partecipante_id }.toSet()
        voti.filter { it.ruolo == "giudice" && (it.partecipante_id in idGiudiciPresenti || it.partecipante_id.startsWith("ASSENTE_")) }
            .forEach { v ->
                val nome = nomiGiudici[v.partecipante_id] ?: if (v.partecipante_id.startsWith("ASSENTE_")) "Giudice (per assente)" else "Giudice"
                dettaglio.add(nome to v.scelta)
                if (v.scelta != "spareggio") conteggio[v.scelta] = (conteggio[v.scelta] ?: 0) + 1
            }

        // verdetto collettivo spettatori (solo presenti)
        var verdettoSpett: String? = null
        if (votoPubblicoAttivo) {
            val idSpettPresenti = spettatoriPresentiIds()
            val votiSpett = voti.filter { it.ruolo == "spettatore" && it.partecipante_id in idSpettPresenti && it.scelta != "spareggio" }
            if (votiSpett.isNotEmpty()) {
                verdettoSpett = votiSpett.groupingBy { it.scelta }.eachCount().maxByOrNull { it.value }?.key
                verdettoSpett?.let { conteggio[it] = (conteggio[it] ?: 0) + 1 }
            }
        }

        val max = conteggio.values.maxOrNull()
        val vincitori = conteggio.filter { it.value == max }.keys
        val vincitore = if (max != null && vincitori.size == 1) vincitori.first() else null

        return RisultatoVerdetti(
            verdettiGiudici = dettaglio,
            verdettoSpettatori = verdettoSpett,
            conteggioPerLato = conteggio,
            vincitoreId = vincitore,
            spareggio = vincitore == null
        )
    }

    fun reset() { presenti.clear(); voti.clear(); sessioneCorrente = null }
}