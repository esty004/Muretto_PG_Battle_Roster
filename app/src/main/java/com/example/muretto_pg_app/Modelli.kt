package com.example.muretto_pg_app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// --- IL NUOVO MOTORE DEI TEMI ---
object Tema {
    var isBarreFaul by mutableStateOf(false)

    val colorePrincipale: Color
        get() = if (isBarreFaul) Color(0xFF1E88E5) else Color(0xFFD32F2F)

    val coloreSfondo: Color
        get() = if (isBarreFaul) Color(0xFFDCDCDC) else Color.Black

    val coloreSfondoCard: Color
        get() = if (isBarreFaul) Color(0xFFF5F5F5) else Color(0xFF111111)

    val coloreTesto: Color
        get() = if (isBarreFaul) Color.Black else Color.White

    val coloreTestoSecondario: Color
        get() = if (isBarreFaul) Color.DarkGray else Color.Gray

    val gradienteCard: Brush
        get() = if (isBarreFaul) {
            Brush.horizontalGradient(colors = listOf(Color(0xFF002244), Color(0xFF004488)))
        } else {
            Brush.horizontalGradient(colors = listOf(Color(0xFF3A0000), Color(0xFF00003A)))
        }
}

// --- DATABASE DEGLI UNIVERSI ---
object DatabaseMcs {
    fun getListaMuretto() = listOf(
        Freestyler("1", "Bahmes", R.drawable.bahmes), Freestyler("2", "Big", R.drawable.big), Freestyler("3", "Bisca", R.drawable.bisca),
        Freestyler("4", "Brage", R.drawable.brage), Freestyler("5", "Chapel", R.drawable.chapel), Freestyler("6", "Deku", R.drawable.deku),
        Freestyler("7", "Esty", R.drawable.esty), Freestyler("8", "Fist", R.drawable.fist), Freestyler("9", "Fto", R.drawable.fto),
        Freestyler("10", "Ganesh", R.drawable.ganesh), Freestyler("11", "Gross", R.drawable.gross), Freestyler("12", "Henker", R.drawable.henker),
        Freestyler("13", "Koko", R.drawable.koko), Freestyler("14", "Lil Dik", R.drawable.lil_dik), Freestyler("15", "Lordao", R.drawable.lordao),
        Freestyler("16", "Lyl Dark", R.drawable.lyl_dark), Freestyler("17", "Madra", R.drawable.madra), Freestyler("18", "Mogio", R.drawable.mogio),
        Freestyler("19", "Monkey", R.drawable.monkey), Freestyler("20", "Mt", R.drawable.mt), Freestyler("21", "Olegan", R.drawable.olegan),
        Freestyler("22", "Rein", R.drawable.rein), Freestyler("23", "Samyr", R.drawable.samyr), Freestyler("24", "Schiaccia", R.drawable.schiaccia),
        Freestyler("25", "Shock", R.drawable.shock), Freestyler("26", "Sockold", R.drawable.sockold), Freestyler("27", "Stiwi", R.drawable.stiwi),
        Freestyler("28", "Tchain", R.drawable.tchain), Freestyler("29", "Yama", R.drawable.yama)
    )

    fun getListaBarreFaul() = listOf(
        Freestyler("bf1", "Darry", R.drawable.darry),
        Freestyler("bf2", "Corra", R.drawable.corra),
        Freestyler("bf3", "Lofo", R.drawable.lofo),
        Freestyler("bf4", "Harion", R.drawable.harion),
        Freestyler("bf5", "Centos", R.drawable.centos),
        Freestyler("bf6", "Secch", R.drawable.secch),
        Freestyler("bf7", "Purino", R.drawable.purino),
        Freestyler("bf8", "Haki One", R.drawable.haki_one),
        Freestyler("bf9", "Tarvos", R.drawable.tarvos),
        Freestyler("bf10", "Mad Riot", R.drawable.mad_riot),
        Freestyler("bf11", "Clover", R.drawable.clover),
        Freestyler("bf12", "Ask-1", R.drawable.ask1),
        Freestyler("bf13", "Mantra", R.drawable.mantra),
        Freestyler("bf14", "Chef Kein", R.drawable.chef_kein),
        Freestyler("24", "Schiaccia", R.drawable.schiaccia) // Condiviso con Muretto PG! Stesso ID.
    )

    fun getListaAttuale() = if (Tema.isBarreFaul) getListaBarreFaul() else getListaMuretto()

    // Meccanica "Cerca MC Globale": Ricerca globale in entrambi gli universi
    fun cercaMcGlobale(nomeDaCercare: String): Freestyler? {
        val tuttiMcs = getListaMuretto() + getListaBarreFaul()
        // Ritorna il primo MC trovato che corrisponde al nome (ignorando maiuscole/minuscole)
        return tuttiMcs.find { it.nome.equals(nomeDaCercare, ignoreCase = true) }
    }
}

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
        listaMcsAllenamento.clear()
        listaMcsAllenamento.addAll(DatabaseMcs.getListaAttuale())
    }
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

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, borderColor, RoundedCornerShape(12.dp))
            .background(Tema.coloreSfondoCard),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = mc.immagineId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
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