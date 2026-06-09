package com.example.muretto_pg_app

import android.content.Context
import coil.Coil
import coil.request.ImageRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Cache offline (sola lettura): salva in locale i dati del DB usati da Muretti e Allenamento
 * (muretti, mcs, common_words, topics, modes) come JSON in filesDir/offline/, e precarica
 * le immagini nella cache disco di Coil così restano visibili senza rete.
 * Il generatore immagini NON è disponibile offline (richiede l'AI online).
 */
object GestoreOffline {
    private const val DIR = "offline"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun dir(ctx: Context): File = File(ctx.filesDir, DIR).apply { mkdirs() }
    private fun file(ctx: Context, nome: String): File = File(dir(ctx), "$nome.json")
    private fun leggi(ctx: Context, nome: String): String? {
        val f = file(ctx, nome); return if (f.exists()) f.readText() else null
    }

    /** Vero se è già stata scaricata almeno una volta la versione offline. */
    fun datiDisponibili(ctx: Context): Boolean = file(ctx, "muretti").exists()

    // ─── SALVATAGGIO ───
    fun salvaMuretti(ctx: Context, l: List<Muretto>) = file(ctx, "muretti").writeText(json.encodeToString(l))
    fun salvaMcs(ctx: Context, l: List<Freestyler>) = file(ctx, "mcs").writeText(json.encodeToString(l))
    fun salvaWords(ctx: Context, l: List<Word>) = file(ctx, "common_words").writeText(json.encodeToString(l))
    fun salvaTopics(ctx: Context, l: List<Topic>) = file(ctx, "topics").writeText(json.encodeToString(l))
    fun salvaModes(ctx: Context, l: List<Mode>) = file(ctx, "modes").writeText(json.encodeToString(l))

    // ─── LETTURA ───
    fun caricaMuretti(ctx: Context): List<Muretto> = leggi(ctx, "muretti")?.let { runCatching { json.decodeFromString<List<Muretto>>(it) }.getOrNull() } ?: emptyList()
    fun caricaMcs(ctx: Context): List<Freestyler> = leggi(ctx, "mcs")?.let { runCatching { json.decodeFromString<List<Freestyler>>(it) }.getOrNull() } ?: emptyList()
    fun caricaWords(ctx: Context): List<Word> = leggi(ctx, "common_words")?.let { runCatching { json.decodeFromString<List<Word>>(it) }.getOrNull() } ?: emptyList()
    fun caricaTopics(ctx: Context): List<Topic> = leggi(ctx, "topics")?.let { runCatching { json.decodeFromString<List<Topic>>(it) }.getOrNull() } ?: emptyList()
    fun caricaModes(ctx: Context): List<Mode> = leggi(ctx, "modes")?.let { runCatching { json.decodeFromString<List<Mode>>(it) }.getOrNull() } ?: emptyList()

    // ─── DOWNLOAD COMPLETO ───
    suspend fun scaricaTutto(ctx: Context, supabase: SupabaseClient, onProgress: (String, Float) -> Unit) {
        onProgress("Scarico i muretti…", 0.05f)
        val muretti = supabase.postgrest["muretti"].select().decodeList<Muretto>()
        salvaMuretti(ctx, muretti)

        onProgress("Scarico gli MC…", 0.18f)
        val mcs = supabase.postgrest["mcs"].select().decodeList<Freestyler>()
        salvaMcs(ctx, mcs)

        onProgress("Scarico parole e generatori…", 0.32f)
        salvaWords(ctx, supabase.postgrest["common_words"].select().decodeList<Word>())
        salvaTopics(ctx, supabase.postgrest["topics"].select().decodeList<Topic>())
        salvaModes(ctx, supabase.postgrest["modes"].select().decodeList<Mode>())

        // Immagini: le facciamo passare da Coil così finiscono nella cache disco (offline-ready)
        val urls = buildList {
            muretti.forEach { m ->
                listOf(
                    m.immagineURL, m.pin_url, m.sfondo_iniziale_url, m.sfondo_url,
                    m.sfondo_card_muretto_url, m.sfondo_card_2v2_url, m.sfondo_card_contest_url
                ).forEach { if (!it.isNullOrBlank()) add(it) }
            }
            mcs.forEach { if (!it.immagineUrl.isNullOrBlank()) add(it.immagineUrl!!) }
        }.distinct()

        val loader = Coil.imageLoader(ctx)
        val tot = urls.size.coerceAtLeast(1)
        urls.forEachIndexed { i, url ->
            runCatching {
                val req = ImageRequest.Builder(ctx).data(url).memoryCacheKey(url).diskCacheKey(url).build()
                loader.execute(req)
            }
            onProgress("Scarico immagini ${i + 1}/${urls.size}…", 0.4f + 0.6f * ((i + 1f) / tot))
        }
        onProgress("Fatto! Versione offline pronta.", 1f)
    }
}