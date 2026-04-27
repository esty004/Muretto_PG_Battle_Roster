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

        // Se è casuale, mischia. Altrimenti mantiene l'ordine in cui sono stati selezionati (o scritti nel notepad)
        val lista = if (tipo == TipoTorneo.COPPIE_CASUALI) partecipanti.shuffled() else partecipanti.toList()
        val coppie = mutableListOf<Freestyler>()

        for (i in 0 until (lista.size / 2) * 2 step 2) {
            coppie.add(
                Freestyler(
                    id = "${lista[i].id}_${lista[i+1].id}",
                    nome = "${lista[i].nome} & ${lista[i+1].nome}",
                    immagineId = R.drawable.due_contro_due
                )
            )
        }

        if (lista.size % 2 != 0) {
            coppie.add(lista.last())
        }

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

    fun haProgresso(context: Context): Boolean = context.getSharedPreferences("battle_pref", Context.MODE_PRIVATE).contains("rounds")

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
    fun caricaArgomenti(context: Context): List<String> {
        return try {
            val json = context.resources.openRawResource(R.raw.topics).bufferedReader().use { it.readText() }
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) { listOf("Errore Caricamento") }
    }

    fun caricaModalita(context: Context): List<String> {
        return try {
            val json = context.resources.openRawResource(R.raw.modes).bufferedReader().use { it.readText() }
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) { listOf("Errore Caricamento") }
    }
}