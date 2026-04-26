package com.example.muretto_pg_app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Modello dati per un Freestyler (MC).
 */
data class Freestyler(val id: String, val nome: String, val immagineId: Int)

/**
 * Modello dati per un Round del torneo.
 */
data class Round(
    val id: String,
    val numero: Int,
    var partecipanti: List<Freestyler>,
    var completato: Boolean = false,
    var vincitoreId: String? = null
)

/**
 * Fasi disponibili nel torneo.
 */
enum class FaseTorneo { OTTAVI, QUARTI, SEMIFINALE, FINALE }

/**
 * Singleton che gestisce lo stato del Torneo Classico.
 * Gestisce la logica di generazione dei tabelloni e il salvataggio persistente.
 */
object GestoreBattle {
    var mcsSelezionati = mutableStateListOf<Freestyler>()
    var roundsAttuali = mutableStateListOf<Round>()
    var faseAttuale = FaseTorneo.OTTAVI
    var is2v2 = false

    /**
     * Resetta completamente il torneo.
     */
    fun resetSelezione() {
        mcsSelezionati.clear()
        roundsAttuali.clear()
        faseAttuale = FaseTorneo.OTTAVI
        is2v2 = false
    }

    /**
     * Determina il nome della fase appropriato in base al numero di partecipanti.
     */
    fun determinaFase(numeroPartecipanti: Int): FaseTorneo {
        return when {
            numeroPartecipanti > 12 -> FaseTorneo.OTTAVI
            numeroPartecipanti > 6 -> FaseTorneo.QUARTI
            numeroPartecipanti > 3 -> FaseTorneo.SEMIFINALE
            else -> FaseTorneo.FINALE
        }
    }

    /**
     * Avvia il torneo decidendo la fase iniziale in base al numero di MC selezionati.
     */
    fun iniziaTorneo(partecipanti: List<Freestyler>) {
        is2v2 = false
        val faseIniziale = determinaFase(partecipanti.size)
        generaFase(faseIniziale, partecipanti)
    }

    /**
     * Avvia il torneo in modalità 2v2.
     * Crea coppie casuali e poi genera la fase.
     */
    fun iniziaTorneo2v2(partecipanti: List<Freestyler>) {
        is2v2 = true
        val shuffled = partecipanti.shuffled()
        val coppie = mutableListOf<Freestyler>()

        // Accoppia i partecipanti
        for (i in 0 until (shuffled.size / 2) * 2 step 2) {
            val p1 = shuffled[i]
            val p2 = shuffled[i + 1]
            coppie.add(
                Freestyler(
                    id = "${p1.id}_${p2.id}",
                    nome = "${p1.nome} & ${p2.nome}",
                    immagineId = R.drawable.due_contro_due
                )
            )
        }

        // Se c'è un MC dispari, lo aggiungiamo come "singolo"
        if (shuffled.size % 2 != 0) {
            coppie.add(shuffled.last())
        }

        val faseIniziale = determinaFase(coppie.size)
        generaFase(faseIniziale, coppie)
    }

    /**
     * Crea i round per una specifica fase distribuendo gli MC in modo casuale.
     */
    fun generaFase(fase: FaseTorneo, partecipanti: List<Freestyler>) {
        faseAttuale = fase
        roundsAttuali.clear()

        if (partecipanti.isEmpty()) return

        val shuffled = partecipanti.shuffled()
        val total = shuffled.size

        if (total % 2 == 0) {
            // Pari: tutti 1v1
            for (i in 0 until total step 2) {
                roundsAttuali.add(Round("r_${fase.name}_${i / 2}", (i / 2) + 1, listOf(shuffled[i], shuffled[i + 1])))
            }
        } else {
            // Dispari: massimizza gli 1v1 e crea esattamente un 3-person rumble
            if (total >= 3) {
                val num1v1 = (total - 3) / 2
                for (i in 0 until num1v1) {
                    roundsAttuali.add(Round("r_${fase.name}_$i", i + 1, listOf(shuffled[i * 2], shuffled[i * 2 + 1])))
                }
                // Aggiunge la rumble da 3 alla fine
                val startIndexForRumble = num1v1 * 2
                roundsAttuali.add(
                    Round(
                        "r_${fase.name}_$num1v1",
                        num1v1 + 1,
                        listOf(shuffled[startIndexForRumble], shuffled[startIndexForRumble + 1], shuffled[startIndexForRumble + 2])
                    )
                )
            } else {
                // Caso limite con 1 solo partecipante
                roundsAttuali.add(Round("r_${fase.name}_0", 1, listOf(shuffled[0])))
            }
        }
    }

    // --- LOGICA DI PERSISTENZA (SharedPreferences + GSON) ---

    fun salvaProgresso(context: Context) {
        val sharedPref = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        sharedPref.edit().apply {
            putString("fase", faseAttuale.name)
            putString("rounds", gson.toJson(roundsAttuali.toList()))
            putBoolean("is2v2", is2v2)
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
        faseAttuale = FaseTorneo.valueOf(faseStr ?: "OTTAVI")
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
        val sharedPref = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
    }
}

/**
 * Singleton che mantiene lo stato della sezione Allenamento.
 * Essendo globale, i dati (MC selezionati, battle generate, tab attiva) 
 * non vengono persi navigando tra le schermate.
 */
object GestoreAllenamento {
    var tabSelezionata by mutableIntStateOf(0) // 0: Matchmaking, 1: Generatori
    var mcsSelezionatiIds by mutableStateOf(setOf<String>())
    var mostraRisultatiMatchmaking by mutableStateOf(false)
    var battleGenerate by mutableStateOf(listOf<Pair<Freestyler, Freestyler>>())
    var mcSingolo by mutableStateOf<Freestyler?>(null)
    var testoRicerca by mutableStateOf("")
    
    // Lista caricata una sola volta per tutto il ciclo di vita dell'app
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

/**
 * Dataset statici per i generatori.
 */
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
        "Sono il tuo più grande fan", "Cover Battle"
    )

    fun caricaArgomenti(context: Context): List<String> {
        return try {
            val json = context.resources.openRawResource(R.raw.topics).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            argomenti
        }
    }

    fun caricaModalita(context: Context): List<String> {
        return try {
            val json = context.resources.openRawResource(R.raw.modes).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            modalita
        }
    }
}
