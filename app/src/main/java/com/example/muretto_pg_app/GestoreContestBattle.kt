package com.example.muretto_pg_app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.UUID

// ─── RIGA TABELLA contest_battle ──────────────────────────────────────────────
// NB: richiede che `Round` in Modelli.kt sia annotato @Serializable (vedi patch).
@Serializable
data class ContestBattleRow(
    val evento_id: String,
    val tipo_battle: String = "1v1",            // '1v1' | '2v2' | 'squadra'
    val accoppiamenti: String = "predefiniti",  // 'predefiniti' | 'casuali'
    val fase_partenza: String = "OTTAVI",
    val roster: List<Freestyler> = emptyList(),
    val tabellone: List<Round> = emptyList(),
    val stato: String = "bozza"                 // 'bozza' | 'configurato' | 'iniziato'
)

// ─── STILE DEL CONTEST (sostituisce Tema nelle schermate del builder) ──────────
data class StileContest(
    val coloreCornici: Color,
    val coloreBottoni: Color,
    val coloreSfondoCard: Color,
    val sfondoUrl: String?,   // immagine custom (se CUSTOM/DELEGA compilato), altrimenti null
    val sfondoRes: Int        // fallback drawable (Tema.sfondoGenerale)
) {
    companion object {
        private fun parseHex(hex: String?, fallback: Color): Color = try {
            if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) { fallback }

        // In creazione: colore_sfondo = colore CORNICI, colore_primario = colore BOTTONI.
        fun risolvi(design: ContestDesign?): StileContest {
            val custom = design?.tipo_stile == "CUSTOM" || design?.tipo_stile == "DELEGA"
            return StileContest(
                coloreCornici = parseHex(design?.colore_sfondo, Tema.colorePrincipale),
                coloreBottoni = parseHex(design?.colore_primario, Tema.colorePrincipale),
                coloreSfondoCard = Tema.coloreSfondoCard,
                sfondoUrl = if (custom) design?.sfondo_custom_url else null,
                sfondoRes = Tema.sfondoGenerale
            )
        }
    }
}

// ─── GESTORE SEPARATO (NON tocca GestoreBattle) ────────────────────────────────
object GestoreContestBattle {
    var eventoIdCorrente by mutableStateOf<String?>(null)
    var tipoBattle by mutableStateOf("1v1")
    var accoppiamenti by mutableStateOf("predefiniti")
    var fasePartenza by mutableStateOf(FaseTorneo.OTTAVI)
    var stato by mutableStateOf("bozza")

    val rosterSelezionato = mutableStateListOf<Freestyler>()
    val roundsAttuali = mutableStateListOf<Round>()

    fun reset() {
        eventoIdCorrente = null
        tipoBattle = "1v1"; accoppiamenti = "predefiniti"
        fasePartenza = FaseTorneo.OTTAVI; stato = "bozza"
        rosterSelezionato.clear(); roundsAttuali.clear()
    }

    // ── PERSISTENZA ──────────────────────────────────────────────────────────
    suspend fun caricaDalCloud(supabase: SupabaseClient, eventoId: String) {
        reset()
        eventoIdCorrente = eventoId
        try {
            val righe = supabase.postgrest["contest_battle"]
                .select { filter { eq("evento_id", eventoId) } }
                .decodeList<ContestBattleRow>()
            val r = righe.firstOrNull() ?: return
            withContext(Dispatchers.Main) {
                tipoBattle = r.tipo_battle
                accoppiamenti = r.accoppiamenti
                fasePartenza = runCatching { FaseTorneo.valueOf(r.fase_partenza) }.getOrDefault(FaseTorneo.OTTAVI)
                stato = r.stato
                rosterSelezionato.clear(); rosterSelezionato.addAll(r.roster)
                roundsAttuali.clear(); roundsAttuali.addAll(r.tabellone)
            }
        } catch (e: Exception) {
            android.util.Log.e("CONTEST_BATTLE", "Errore carica: ${e.message}", e)
        }
    }

    suspend fun salvaSulCloud(supabase: SupabaseClient, nuovoStato: String? = null): Boolean {
        val id = eventoIdCorrente ?: return false
        nuovoStato?.let { stato = it }
        return try {
            val row = ContestBattleRow(
                evento_id = id,
                tipo_battle = tipoBattle,
                accoppiamenti = accoppiamenti,
                fase_partenza = fasePartenza.name,
                roster = rosterSelezionato.toList(),
                tabellone = roundsAttuali.toList(),
                stato = stato
            )
            supabase.postgrest["contest_battle"].upsert(row)
            true
        } catch (e: Exception) {
            android.util.Log.e("CONTEST_BATTLE", "Errore salva: ${e.message}", e)
            false
        }
    }

    // ── LOGICA BUILDER ────────────────────────────────────────────────────────

    /** Id dei singoli MC già piazzati nei round (scompone le coppie 2v2 "idA_idB"). */
    fun idsUsati(): Set<String> {
        val out = mutableSetOf<String>()
        roundsAttuali.forEach { round ->
            round.partecipanti.forEach { p ->
                if (tipoBattle == "2v2" && p.id.contains("_")) out.addAll(p.id.split("_"))
                else out.add(p.id)
            }
        }
        return out
    }

    fun rosterDisponibile(): List<Freestyler> {
        val usati = idsUsati()
        return rosterSelezionato.filter { it.id !in usati }
    }

    /** Quanti MC servono per riempire un round (1v1 → 2 singoli, 2v2 → 4 singoli). */
    fun mcPerRound(): Int = if (tipoBattle == "2v2") 4 else 2

    fun nuovoRound(): Round {
        val r = Round(id = "cr_${UUID.randomUUID()}", numero = roundsAttuali.size + 1, partecipanti = emptyList())
        roundsAttuali.add(r)
        return r
    }

    /** Combina due singoli in una "coppia" con lo stesso encoding di GestoreBattle (per BoxMC). */
    private fun creaCoppia(a: Freestyler, b: Freestyler): Freestyler {
        val u1 = if (a.immagineUrl.isNullOrBlank()) "no_pic" else a.immagineUrl
        val u2 = if (b.immagineUrl.isNullOrBlank()) "no_pic" else b.immagineUrl
        return Freestyler(id = "${a.id}_${b.id}", nome = "${a.nome} & ${b.nome}", immagineUrl = "$u1,$u2")
    }

    /** Salva i partecipanti del round. `singoli` = MC scelti in ordine (2 per 1v1, 4 per 2v2). */
    fun salvaPartecipantiRound(roundId: String, singoli: List<Freestyler>) {
        val idx = roundsAttuali.indexOfFirst { it.id == roundId }
        if (idx == -1) return
        val partecipanti = if (tipoBattle == "2v2" && singoli.size == 4) {
            listOf(creaCoppia(singoli[0], singoli[1]), creaCoppia(singoli[2], singoli[3]))
        } else {
            singoli
        }
        roundsAttuali[idx] = roundsAttuali[idx].copy(partecipanti = partecipanti)
    }

    fun rimuoviRound(roundId: String) {
        roundsAttuali.removeIf { it.id == roundId }
        // rinumera
        roundsAttuali.forEachIndexed { i, r -> roundsAttuali[i] = r.copy(numero = i + 1) }
    }

    /** Vero se tutti gli MC del roster sono stati accoppiati (→ mostra CONFERMA, nascondi NUOVO ROUND). */
    fun tuttiAccoppiati(): Boolean =
        rosterSelezionato.isNotEmpty() && rosterDisponibile().isEmpty() &&
                roundsAttuali.all { it.partecipanti.isNotEmpty() }

    /**
     * Per accoppiamenti CASUALI: genera automaticamente i round usando la STESSA logica
     * di GestoreBattle.generaFase (portata qui, senza toccare lo stato globale).
     */
    fun generaCasuale() {
        roundsAttuali.clear()
        val base = rosterSelezionato.toList()
        val partecipanti = if (tipoBattle == "2v2") {
            val mescolati = base.shuffled()
            val coppie = mutableListOf<Freestyler>()
            var i = 0
            while (i + 1 < mescolati.size) { coppie.add(creaCoppia(mescolati[i], mescolati[i + 1])); i += 2 }
            if (mescolati.size % 2 != 0) coppie.add(mescolati.last())
            coppie
        } else base.shuffled()

        val fase = fasePartenza
        val total = partecipanti.size
        if (total == 0) return
        if (total % 2 == 0) {
            var i = 0
            while (i < total) { roundsAttuali.add(Round("cr_${fase.name}_${i / 2}", (i / 2) + 1, listOf(partecipanti[i], partecipanti[i + 1]))); i += 2 }
        } else if (total >= 3) {
            val num1v1 = (total - 3) / 2
            for (i in 0 until num1v1) roundsAttuali.add(Round("cr_${fase.name}_$i", i + 1, listOf(partecipanti[i * 2], partecipanti[i * 2 + 1])))
            val start = num1v1 * 2
            roundsAttuali.add(Round("cr_${fase.name}_$num1v1", num1v1 + 1, listOf(partecipanti[start], partecipanti[start + 1], partecipanti[start + 2])))
        } else {
            roundsAttuali.add(Round("cr_${fase.name}_0", 1, listOf(partecipanti[0])))
        }
    }
}

// ─── METODI DB AGGIUNTIVI (extension su DatabaseViewModel: usa supabase pubblico) ──

/** Roster osservabile con TUTTI gli MC di TUTTI i muretti (per la selezione del contest). */
object RosterGlobaleContest { val lista = mutableStateListOf<Freestyler>() }

/** Contest in DELEGA non ancora completati (per la tab notifiche admin). */
object StatoDelegaContest { val lista = mutableStateListOf<Evento>() }

suspend fun DatabaseViewModel.caricaRosterGlobaleContest() {
    try {
        val all = supabase.postgrest["mcs"].select().decodeList<Freestyler>()
        withContext(Dispatchers.Main) {
            RosterGlobaleContest.lista.clear()
            RosterGlobaleContest.lista.addAll(all)
        }
    } catch (e: Exception) {
        android.util.Log.e("CONTEST_BATTLE", "Errore roster globale: ${e.message}", e)
    }
}

/** Aggiorna l'estetica di un contest esistente (tab Estetica). Carica anche un nuovo sfondo se presente. */
suspend fun DatabaseViewModel.aggiornaDesignContest(
    eventoId: String,
    tipoStile: String,
    colorePrimario: String?,   // colore BOTTONI (hex #AARRGGBB)
    coloreSfondo: String?,     // colore CORNICI (hex #AARRGGBB)
    sfondoBytes: ByteArray?,
    descrizioneDelega: String? = null,
    delegaCompletata: Boolean? = null
): Boolean {
    return try {
        var sfondoUrl: String? = null
        if (sfondoBytes != null) {
            val name = "sfondo_contest_${UUID.randomUUID()}.jpg"
            val bucket = supabase.storage["locandine_eventi"]
            bucket.upload(name, sfondoBytes, upsert = true)
            sfondoUrl = bucket.publicUrl(name)
        }
        supabase.postgrest["contest_design"].update({
            set("tipo_stile", tipoStile)
            set("colore_primario", colorePrimario)
            set("colore_sfondo", coloreSfondo)
            if (sfondoUrl != null) set("sfondo_custom_url", sfondoUrl)
            if (descrizioneDelega != null) set("descrizione_delega", descrizioneDelega)
            if (delegaCompletata != null) set("delega_completata", delegaCompletata)
        }) { filter { eq("evento_id", eventoId) } }
        fetchEventiApprovati()
        true
    } catch (e: Exception) {
        android.util.Log.e("CONTEST_BATTLE", "Errore aggiorna design: ${e.message}", e)
        false
    }
}

/** Salva la descrizione della delega estetica (chiamata dalla creazione contest). */
suspend fun DatabaseViewModel.salvaDescrizioneDelega(eventoId: String, descrizione: String): Boolean {
    return try {
        supabase.postgrest["contest_design"].update({
            set("descrizione_delega", descrizione)
            set("delega_completata", false)
        }) { filter { eq("evento_id", eventoId) } }
        true
    } catch (e: Exception) { false }
}

/** Carica i contest in delega (tipo_stile = 'DELEGA' e non ancora completati) per gli admin. */
suspend fun DatabaseViewModel.fetchContestInDelega() {
    try {
        val eventi = supabase.postgrest["eventi"]
            .select(columns = Columns.raw("*, contest_design(*)")) { filter { eq("stato", "approvato") } }
            .decodeList<Evento>()
        val delegati = eventi.filter {
            it.contest_design?.tipo_stile == "DELEGA" && it.contest_design?.delega_completata != true
        }
        withContext(Dispatchers.Main) {
            StatoDelegaContest.lista.clear()
            StatoDelegaContest.lista.addAll(delegati)
        }
    } catch (e: Exception) {
        android.util.Log.e("CONTEST_BATTLE", "Errore fetch delega: ${e.message}", e)
    }
}
