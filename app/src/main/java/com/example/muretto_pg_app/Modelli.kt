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