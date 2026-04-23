package com.example.muretto_pg_app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Freestyler(val id: String, val nome: String, val immagineId: Int)

// Nuovo modello: Il partecipante di un round è ora un "Team" (può essere singolo o coppia)
data class Team(
    val id: String,
    val nome: String,
    val membri: List<Freestyler>
)

data class Round(
    val id: String,
    val numero: Int,
    var partecipanti: List<Team>,
    var completato: Boolean = false,
    var vincitoreId: String? = null
)

enum class FaseTorneo { OTTAVI, QUARTI, SEMIFINALE, FINALE }
enum class TipoTorneo { SINGOLO, COPPIE_CASUALI, COPPIE_PREDEFINITE }

object GestoreBattle {
    var mcsSelezionati = mutableStateListOf<Freestyler>() // Usato temporaneamente per la selezione
    var roundsAttuali = mutableStateListOf<Round>()
    var faseAttuale = FaseTorneo.OTTAVI
    var tipoTorneoAttuale = TipoTorneo.SINGOLO

    fun resetSelezione() {
        mcsSelezionati.clear()
        roundsAttuali.clear()
        faseAttuale = FaseTorneo.OTTAVI
        tipoTorneoAttuale = TipoTorneo.SINGOLO
    }

    // Trasforma la lista di Freestyler selezionati in Team e avvia
    fun preparaEIniziaTorneo(tipo: TipoTorneo, mcs: List<Freestyler>) {
        tipoTorneoAttuale = tipo
        val teams = when (tipo) {
            TipoTorneo.SINGOLO -> mcs.map { Team(it.id, it.nome, listOf(it)) }
            TipoTorneo.COPPIE_CASUALI -> {
                // Scarta l'ultimo MC se il numero è dispari
                val mcsPari = if (mcs.size % 2 != 0) mcs.dropLast(1) else mcs
                mcsPari.shuffled().chunked(2).mapIndexed { index, chunk ->
                    val nome = chunk.joinToString(" & ") { it.nome }
                    Team("team_cas_$index", nome, chunk)
                }
            }
            TipoTorneo.COPPIE_PREDEFINITE -> {
                // Scarta l'ultimo MC se il numero è dispari
                val mcsPari = if (mcs.size % 2 != 0) mcs.dropLast(1) else mcs
                mcsPari.chunked(2).mapIndexed { index, chunk ->
                    val nome = chunk.joinToString(" & ") { it.nome }
                    Team("team_pre_$index", nome, chunk)
                }
            }
        }
        iniziaTorneo(teams)
    }

    // --- LA MATEMATICA CORRETTA DEL MURETTO ---
    fun iniziaTorneo(teams: List<Team>) {
        when {
            teams.size < 4 -> generaFase(FaseTorneo.FINALE, teams)      // 2 o 3 Team = Finale diretta
            teams.size < 8 -> generaFase(FaseTorneo.SEMIFINALE, teams)  // 4-7 Team = Semifinale
            teams.size < 16 -> generaFase(FaseTorneo.QUARTI, teams)     // 8-15 Team = Quarti
            else -> generaFase(FaseTorneo.OTTAVI, teams)                // 16+ Team = Ottavi completi!
        }
    }

    fun generaFase(fase: FaseTorneo, teams: List<Team>) {
        faseAttuale = fase
        roundsAttuali.clear()

        val numRound = when(fase) {
            FaseTorneo.OTTAVI -> 8
            FaseTorneo.QUARTI -> 4
            FaseTorneo.SEMIFINALE -> 2
            FaseTorneo.FINALE -> 1
        }

        val liste = List(numRound) { mutableListOf<Team>() }

        teams.shuffled().forEachIndexed { i, team ->
            liste[i % numRound].add(team)
        }

        roundsAttuali.addAll(liste.mapIndexed { i, p ->
            Round("r_${fase.name}_$i", i + 1, p)
        })
    }

    fun salvaProgresso(context: Context) {
        val sharedPref = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        sharedPref.edit().apply {
            putString("fase", faseAttuale.name)
            putString("tipoTorneo", tipoTorneoAttuale.name)
            putString("rounds", gson.toJson(roundsAttuali.toList()))
            apply()
        }
    }

    fun haProgresso(context: Context): Boolean {
        return context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE).contains("rounds")
    }

    fun caricaProgresso(context: Context) {
        val sharedPref = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        val faseStr = sharedPref.getString("fase", "OTTAVI")
        val tipoStr = sharedPref.getString("tipoTorneo", "SINGOLO")
        faseAttuale = FaseTorneo.valueOf(faseStr ?: "OTTAVI")
        tipoTorneoAttuale = TipoTorneo.valueOf(tipoStr ?: "SINGOLO")

        val roundsJson = sharedPref.getString("rounds", null)
        if (roundsJson != null) {
            val type = object : TypeToken<List<Round>>() {}.type
            val list: List<Round> = gson.fromJson(roundsJson, type)
            roundsAttuali.clear()
            roundsAttuali.addAll(list)
        }
    }

    fun pulisciSalvataggio(context: Context) {
        val sharedPref = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
    }
}

object GestoreAllenamento {
    var tabSelezionata by mutableIntStateOf(0)
    var mcsSelezionatiIds by mutableStateOf(setOf<String>())
    var mostraRisultatiMatchmaking by mutableStateOf(false)
    var battleGenerate by mutableStateOf(listOf<Pair<Freestyler, Freestyler>>())
    var mcSingolo by mutableStateOf<Freestyler?>(null)
    var testoRicerca by mutableStateOf("")

    var listaMcsAllenamento = mutableStateListOf<Freestyler>()

    fun inizializzaSeVuoto() {
        if (listaMcsAllenamento.isEmpty()) {
            listaMcsAllenamento.addAll(getListaBaseMcs())
        }
    }

    private fun getListaBaseMcs() = listOf(
        Freestyler("1", "Bahmes", R.drawable.bahmes),
        Freestyler("2", "Big", R.drawable.big),
        Freestyler("3", "Bisca", R.drawable.bisca),
        Freestyler("4", "Brage", R.drawable.brage),
        Freestyler("5", "Chapel", R.drawable.chapel),
        Freestyler("6", "Deku", R.drawable.deku),
        Freestyler("7", "Esty", R.drawable.esty),
        Freestyler("8", "Fist", R.drawable.fist),
        Freestyler("9", "Fto", R.drawable.fto),
        Freestyler("10", "Ganesh", R.drawable.ganesh),
        Freestyler("11", "Gross", R.drawable.gross),
        Freestyler("12", "Henker", R.drawable.henker),
        Freestyler("13", "Koko", R.drawable.koko),
        Freestyler("14", "Lil Dik", R.drawable.lil_dik),
        Freestyler("15", "Lordao", R.drawable.lordao),
        Freestyler("16", "Lyl Dark", R.drawable.lyl_dark),
        Freestyler("17", "Madra", R.drawable.madra),
        Freestyler("18", "Mogio", R.drawable.mogio),
        Freestyler("19", "Monkey", R.drawable.monkey),
        Freestyler("20", "Mt", R.drawable.mt),
        Freestyler("21", "Olegan", R.drawable.olegan),
        Freestyler("22", "Rein", R.drawable.rein),
        Freestyler("23", "Samyr", R.drawable.samyr),
        Freestyler("24", "Schiaccia", R.drawable.schiaccia),
        Freestyler("25", "Shock", R.drawable.shock),
        Freestyler("26", "Sockold", R.drawable.sockold),
        Freestyler("27", "Stiwi", R.drawable.stiwi),
        Freestyler("28", "Tchain", R.drawable.tchain),
        Freestyler("29", "Yama", R.drawable.yama)
    )
}

object DatiAllenamento {
    val argomenti = listOf(
        "Intelligenza Artificiale", "criptovalute", "viaggi nel tempo", "italia", "videogiochi",
        "anime", "lavoro", "famiglia", "cucina", "sport", "casa", "tecnologia", "musica",
        "cartoni", "sagre", "scuola", "trasporti pubblici", "figa", "politica", "amicizia",
        "viaggi", "schiavismo", "guerra", "razzismo", "religione", "cinema", "calcio",
        "basket", "alcol", "beatmaking", "poeti", "attori", "black humor", "imitazioni",
        "geografia", "storia", "serie tv", "fortuna", "crimini", "serial killer", "harry potter",
        "libri", "forme", "esami", "palestra", "vestiti", "emozioni", "giardinaggio", "animali",
        "ludopatia", "modi di dire", "alfabeto", "numeri", "arte", "ricorrenze/feste", "insetti",
        "meme", "laghi/fiumi", "fenomeni naturali", "attentati", "diritti delle donne",
        "malattie", "violenza di genere", "dittatura", "moda", "bevande", "porno", "spazio",
        "robot", "droga", "tempo libero", "supereroi", "soldi", "pokemon", "legge",
        "forze dell'ordine", "motori", "complimenti all'avversario", "social", "dialetti",
        "circo", "cosa non dire al primo appuntamento", "strumenti musicali", "catene fast food",
        "animali che puoi cavalcare", "giocattoli", "hip hop italiano", "conscious", "yu-gi-oh"
    )

    val modalita = listOf(
        "4/4", "4/4 Argomento", "3/4", "3/4 Argomento", "8/4", "8/4 Argomento", "Kickback Minuti", "Kickback 4/4",
        "Minuto Libero", "Minuto Beat Scelta", "Minuto Beat Argomento", "Linker", "Taboo", "Modalità Personaggi", "Modalità Situazioni", "Universi Paralleli",
        "Sono il tuo più grande fan", "Cover Battle", "Cypher Tecniche", "Oggetti", "Immagini", "4/4 Tecniche Perfette 1vs1", "Handicap Match", "1 VS 1"
    )
}