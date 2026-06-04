package com.example.muretto_pg_app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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

// COLORI DEI GRUPPI (coppie/squadre) - identici a quelli di CardFreestylerTorneo
val ColoriGruppiContest = listOf(
    Color(0xFFFF3D00), // 1 Rosso/Arancio
    Color(0xFF00B0FF), // 2 Azzurro
    Color(0xFF00E676), // 3 Verde
    Color(0xFFFFEA00), // 4 Giallo
    Color(0xFFD500F9), // 5 Viola
    Color(0xFF1DE9B6)  // 6 Teal
)

// RIGA TABELLA contest_battle  (richiede Round @Serializable in Modelli.kt)
@Serializable
data class ContestBattleRow(
    val evento_id: String,
    val tipo_battle: String = "1v1",            // '1v1' | '2v2' | 'squadra'
    val accoppiamenti: String = "predefiniti",  // 'predefiniti' | 'casuali'
    val fase_partenza: String = "OTTAVI",
    val dimensione_squadra: Int = 2,            // 1 = 1v1, 2 = coppia, >2 = squadra
    val roster: List<Freestyler> = emptyList(),
    val tabellone: List<Round> = emptyList(),
    val stato: String = "bozza"                 // 'bozza' | 'configurato' | 'iniziato'
)

// STILE DEL CONTEST (sostituisce Tema nelle schermate del builder)
data class StileContest(
    val coloreCornici: Color,
    val coloreBottoni: Color,
    val coloreSfondoCard: Color,
    val sfondoUrl: String?,      // sfondo generale custom
    val sfondoCardUrl: String?,  // sfondo della card del round custom
    val sfondoRes: Int           // fallback drawable (Tema.sfondoGenerale)
) {
    companion object {
        private fun parseHex(hex: String?, fallback: Color): Color = try {
            if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) { fallback }

        fun risolvi(design: ContestDesign?): StileContest {
            val custom = design?.tipo_stile == "CUSTOM" || design?.tipo_stile == "DELEGA"
            return StileContest(
                coloreCornici = parseHex(design?.colore_sfondo, Tema.colorePrincipale),
                coloreBottoni = parseHex(design?.colore_primario, Tema.colorePrincipale),
                coloreSfondoCard = Tema.coloreSfondoCard,
                sfondoUrl = if (custom) design?.sfondo_custom_url else null,
                sfondoCardUrl = if (custom) design?.sfondo_card_url else null,
                sfondoRes = Tema.sfondoGenerale
            )
        }
    }
}

// GESTORE SEPARATO (NON tocca GestoreBattle)
object GestoreContestBattle {
    var eventoIdCorrente by mutableStateOf<String?>(null)
    var tipoBattle by mutableStateOf("1v1")
    var accoppiamenti by mutableStateOf("predefiniti")
    var fasePartenza by mutableStateOf(FaseTorneo.OTTAVI)
    var dimensioneSquadra by mutableIntStateOf(2)
    var stato by mutableStateOf("bozza")

    val rosterSelezionato = mutableStateListOf<Freestyler>()
    val roundsAttuali = mutableStateListOf<Round>()

    fun reset() {
        eventoIdCorrente = null
        tipoBattle = "1v1"; accoppiamenti = "predefiniti"
        fasePartenza = FaseTorneo.OTTAVI; dimensioneSquadra = 2; stato = "bozza"
        rosterSelezionato.clear(); roundsAttuali.clear()
    }

    /** Quanti freestyler per "lato" del match (1 per 1v1, 2 per coppia, N per squadra). */
    fun membriPerLato(): Int = when (tipoBattle) {
        "1v1" -> 1
        "2v2" -> 2
        else -> dimensioneSquadra.coerceAtLeast(2)
    }

    /** Vero quando i lati sono "gruppi" (coppie/squadre) con accoppiamenti predefiniti. */
    fun isAGruppi(): Boolean = tipoBattle != "1v1" && accoppiamenti == "predefiniti"

    // PERSISTENZA
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
                dimensioneSquadra = r.dimensione_squadra
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
                dimensione_squadra = membriPerLato(),
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

    // GRUPPI / LOGICA BUILDER

    /** Unisce piu' freestyler in un unico "lato" (id e url separati, compatibile col rendering). */
    fun creaGruppo(membri: List<Freestyler>): Freestyler {
        if (membri.size == 1) return membri.first()
        val urls = membri.joinToString(",") { if (it.immagineUrl.isNullOrBlank()) "no_pic" else it.immagineUrl!! }
        return Freestyler(
            id = membri.joinToString("_") { it.id },
            nome = membri.joinToString(" & ") { it.nome },
            immagineUrl = urls
        )
    }

    /** I gruppi predefiniti, formati dall'ordine di selezione nel roster (chunked per dimensione). */
    fun gruppiPredefiniti(): List<List<Freestyler>> =
        if (isAGruppi()) rosterSelezionato.chunked(membriPerLato())
        else rosterSelezionato.map { listOf(it) }

    /** Colore del gruppo a cui appartiene un dato freestyler (per cornici coerenti). */
    fun coloreGruppoDi(idMembro: String, fallback: Color): Color {
        if (!isAGruppi()) return fallback
        val idx = gruppiPredefiniti().indexOfFirst { gruppo -> gruppo.any { it.id == idMembro } }
        return if (idx >= 0) ColoriGruppiContest[idx % ColoriGruppiContest.size] else fallback
    }

    /** Id dei singoli freestyler gia' piazzati nei round (scompone i lati combinati "a_b_c"). */
    fun idsUsati(): Set<String> {
        val out = mutableSetOf<String>()
        roundsAttuali.forEach { round ->
            round.partecipanti.forEach { p ->
                if (p.id.contains("_")) out.addAll(p.id.split("_")) else out.add(p.id)
            }
        }
        return out
    }

    /** I gruppi ancora da accoppiare (per costruire i round). */
    fun gruppiDisponibili(): List<List<Freestyler>> {
        val usati = idsUsati()
        return gruppiPredefiniti().filter { gruppo -> gruppo.none { it.id in usati } }
    }

    fun nuovoRound(): Round {
        val r = Round(id = "cr_${UUID.randomUUID()}", numero = roundsAttuali.size + 1, partecipanti = emptyList())
        roundsAttuali.add(r)
        return r
    }

    /** Salva i due lati del round. `lati` = lista di 2 gruppi (ognuno con 1..N membri). */
    fun salvaLatiRound(roundId: String, lati: List<List<Freestyler>>) {
        val idx = roundsAttuali.indexOfFirst { it.id == roundId }
        if (idx == -1) return
        val partecipanti = lati.map { creaGruppo(it) }
        roundsAttuali[idx] = roundsAttuali[idx].copy(partecipanti = partecipanti)
    }

    fun rimuoviRound(roundId: String) {
        roundsAttuali.removeIf { it.id == roundId }
        roundsAttuali.forEachIndexed { i, r -> roundsAttuali[i] = r.copy(numero = i + 1) }
    }

    /** Vero se tutti i gruppi sono stati accoppiati. */
    fun tuttiAccoppiati(): Boolean =
        rosterSelezionato.isNotEmpty() && gruppiDisponibili().isEmpty() &&
                roundsAttuali.all { it.partecipanti.isNotEmpty() }

    /** Per accoppiamenti CASUALI: genera automaticamente i round. */
    fun generaCasuale() {
        roundsAttuali.clear()
        val membri = membriPerLato()
        val base = rosterSelezionato.shuffled()
        val lati = base.chunked(membri).map { creaGruppo(it) }
        val total = lati.size
        if (total == 0) return
        if (total % 2 == 0) {
            var i = 0
            while (i < total) { roundsAttuali.add(Round("cr_${fasePartenza.name}_${i / 2}", (i / 2) + 1, listOf(lati[i], lati[i + 1]))); i += 2 }
        } else if (total >= 3) {
            val num1v1 = (total - 3) / 2
            for (i in 0 until num1v1) roundsAttuali.add(Round("cr_${fasePartenza.name}_$i", i + 1, listOf(lati[i * 2], lati[i * 2 + 1])))
            val start = num1v1 * 2
            roundsAttuali.add(Round("cr_${fasePartenza.name}_$num1v1", num1v1 + 1, listOf(lati[start], lati[start + 1], lati[start + 2])))
        } else {
            roundsAttuali.add(Round("cr_${fasePartenza.name}_0", 1, listOf(lati[0])))
        }
    }
}

// METODI DB AGGIUNTIVI (extension su DatabaseViewModel)

object RosterGlobaleContest { val lista = mutableStateListOf<Freestyler>() }
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

/** Aggiorna l'estetica di un contest (sfondo generale + sfondo card + colori). */
suspend fun DatabaseViewModel.aggiornaDesignContest(
    eventoId: String,
    tipoStile: String,
    colorePrimario: String?,
    coloreSfondo: String?,
    sfondoBytes: ByteArray?,
    sfondoCardBytes: ByteArray? = null,
    descrizioneDelega: String? = null,
    delegaCompletata: Boolean? = null
): Boolean {
    return try {
        val bucket = supabase.storage["locandine_eventi"]
        var sfondoUrl: String? = null
        if (sfondoBytes != null) {
            val name = "sfondo_contest_${UUID.randomUUID()}.jpg"
            bucket.upload(name, sfondoBytes, upsert = true)
            sfondoUrl = bucket.publicUrl(name)
        }
        var sfondoCardUrl: String? = null
        if (sfondoCardBytes != null) {
            val name = "sfondo_card_${UUID.randomUUID()}.jpg"
            bucket.upload(name, sfondoCardBytes, upsert = true)
            sfondoCardUrl = bucket.publicUrl(name)
        }
        supabase.postgrest["contest_design"].update({
            set("tipo_stile", tipoStile)
            set("colore_primario", colorePrimario)
            set("colore_sfondo", coloreSfondo)
            if (sfondoUrl != null) set("sfondo_custom_url", sfondoUrl)
            if (sfondoCardUrl != null) set("sfondo_card_url", sfondoCardUrl)
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

suspend fun DatabaseViewModel.salvaDescrizioneDelega(eventoId: String, descrizione: String): Boolean {
    return try {
        supabase.postgrest["contest_design"].update({
            set("descrizione_delega", descrizione)
            set("delega_completata", false)
        }) { filter { eq("evento_id", eventoId) } }
        true
    } catch (e: Exception) { false }
}

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