package com.example.muretto_pg_app

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Freestyler(val id: String, val nome: String, val immagineId: Int)

data class Round(
    val id: String,
    val numero: Int,
    var partecipanti: List<Freestyler>,
    var completato: Boolean = false,
    var vincitoreId: String? = null
)

// Definiamo le fasi
enum class FaseTorneo { OTTAVI, QUARTI, SEMIFINALE, FINALE }

object GestoreBattle {
    var mcsSelezionati = mutableStateListOf<Freestyler>()
    var roundsAttuali = mutableStateListOf<Round>()
    var faseAttuale = FaseTorneo.OTTAVI

    fun resetSelezione() {
        mcsSelezionati.clear()
        roundsAttuali.clear()
        faseAttuale = FaseTorneo.OTTAVI
    }

    // --- LOGICA INTELLIGENTE PER LA PARTENZA ---
    // Capisce da dove partire per evitare round con 1 sola persona
    fun iniziaTorneo(partecipanti: List<Freestyler>) {
        when {
            partecipanti.size < 4 -> generaFase(FaseTorneo.FINALE, partecipanti) // Se sono 2 o 3, finale diretta!
            partecipanti.size < 8 -> generaFase(FaseTorneo.SEMIFINALE, partecipanti) // Se sono da 4 a 7, partono in Semifinale
            partecipanti.size < 16 -> generaFase(FaseTorneo.QUARTI, partecipanti) // Se sono da 8 a 15, partono dai Quarti
            else -> generaFase(FaseTorneo.OTTAVI, partecipanti) // Dai 16 in su, Ottavi completi!
        }
    }

    // Genera i round in base alla fase
    fun generaFase(fase: FaseTorneo, partecipanti: List<Freestyler>) {
        faseAttuale = fase
        roundsAttuali.clear()

        // Matematica del tabellone ufficiale
        val numRound = when(fase) {
            FaseTorneo.OTTAVI -> 8      // Producono 8 vincitori
            FaseTorneo.QUARTI -> 4      // Producono 4 vincitori
            FaseTorneo.SEMIFINALE -> 2  // Producono 2 finalisti
            FaseTorneo.FINALE -> 1      // 1 vincitore
        }

        val liste = List(numRound) { mutableListOf<Freestyler>() }

        // Distribuisce gli MC come carte da gioco.
        // Così i dispari si uniscono automaticamente ai round creando Rumble!
        partecipanti.shuffled().forEachIndexed { i, mc ->
            liste[i % numRound].add(mc)
        }

        roundsAttuali.addAll(liste.mapIndexed { i, p ->
            Round("r_${fase.name}_$i", i + 1, p)
        })
    }

    // --- LOGICA DI SALVATAGGIO ---
    fun salvaProgresso(context: Context) {
        val sharedPref = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        sharedPref.edit().apply {
            putString("fase", faseAttuale.name)
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
        faseAttuale = FaseTorneo.valueOf(faseStr ?: "OTTAVI")

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

object DatiAllenamento {
    val argomenti = listOf(
        "Intelligenza Artificiale",
        "criptovalute",
        "viaggi nel tempo",
        "italia",
        "videogiochi",
        "anime",
        "lavoro",
        "famiglia",
        "cucina",
        "sport",
        "casa",
        "tecnologia",
        "musica",
        "cartoni",
        "sagre",
        "scuola",
        "trasporti pubblici",
        "figa",
        "politica",
        "amicizia",
        "viaggi",
        "schiavismo",
        "guerra",
        "razzismo",
        "religione",
        "cinema",
        "calcio",
        "basket",
        "alcol",
        "beatmaking",
        "poeti",
        "attori",
        "black humor",
        "imitazioni",
        "geografia",
        "storia",
        "serie tv",
        "fortuna",
        "crimini",
        "serial killer",
        "harry potter",
        "libri",
        "forme",
        "esami",
        "palestra",
        "vestiti",
        "emozioni",
        "giardinaggio",
        "animali",
        "ludopatia",
        "modi di dire",
        "alfabeto",
        "numeri",
        "arte",
        "ricorrenze/feste",
        "insetti",
        "meme",
        "laghi/fiumi",
        "fenomeni naturali",
        "attentati",
        "diritti delle donne",
        "malattie",
        "violenza di genere",
        "dittatura",
        "moda",
        "bevande",
        "porno",
        "spazio",
        "robot",
        "droga",
        "tempo libero",
        "supereroi",
        "soldi",
        "pokemon",
        "legge",
        "forze dell'ordine",
        "motori",
        "complimenti all'avversario",
        "social",
        "dialetti",
        "circo",
        "cosa non dire al primo appuntamento",
        "strumenti musicali",
        "catene fast food",
        "animali che puoi cavalcare",
        "giocattoli",
        "hip hop italiano",
        "conscious",
        "yu-gi-oh"
    )

    val modalita = listOf(
        "4/4", "4/4 Argomento", "3/4", "3/4 Argomento", "8/4", "8/4 Argomento", "Kickback Minuti", "Kickback 4/4",
        "Minuto Libero", "Minuto Beat Scelta", "Minuto Beat Argomento", "Linker", "Taboo", "Modalità Personaggi", "Modalità Situazioni", "Universi Paralleli",
        "Sono il tuo più grande fan", "Cover Battle"

    )
}