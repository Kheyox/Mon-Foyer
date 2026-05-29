package com.bibliostudio.monfoyer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Couche reseau centralisee : timeouts stricts + retry avec backoff exponentiel.
 *
 * Avant, tous les appels utilisaient `URL(url).readText()` sans aucun timeout, ce qui
 * pouvait bloquer une coroutine (et l'UI en chargement) indefiniment sur un reseau lent.
 */
object Net {
    private const val CONNECT_TIMEOUT_MS = 8000
    private const val READ_TIMEOUT_MS = 8000
    private const val MAX_ATTEMPTS = 3

    /**
     * Telecharge le contenu texte d'une URL avec timeouts et retry.
     * @throws Exception si tous les essais echouent (le caller utilise runCatching).
     */
    suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "MonFoyer-Android")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = connection.responseCode
                    if (code in 200..299) {
                        return@withContext connection.inputStream.bufferedReader().use { it.readText() }
                    }
                    // 4xx : inutile de reessayer, l'erreur ne disparaitra pas.
                    if (code in 400..499) {
                        throw java.io.IOException("HTTP $code")
                    }
                    lastError = java.io.IOException("HTTP $code")
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                lastError = e
                if (e is java.io.IOException && e.message?.startsWith("HTTP 4") == true) {
                    throw e
                }
            }
            // backoff exponentiel : 500ms, 1000ms (pas apres le dernier essai)
            if (attempt < MAX_ATTEMPTS - 1) {
                delay(500L * (attempt + 1))
            }
        }
        throw lastError ?: java.io.IOException("Echec reseau apres $MAX_ATTEMPTS essais")
    }

    suspend fun fetchJson(url: String): JSONObject = JSONObject(fetchText(url))
}
