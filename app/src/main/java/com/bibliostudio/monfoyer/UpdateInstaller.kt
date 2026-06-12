package com.bibliostudio.monfoyer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Telechargement en arriere-plan + installation des mises a jour APK.
 *
 * L'APK est telecharge dans le cache de l'app puis l'installateur systeme est
 * lance via FileProvider. Android exige toujours la confirmation finale de
 * l'utilisateur (ecran systeme « Installer ») : c'est une securite incontournable.
 */
object UpdateInstaller {
    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 30000

    private fun updateFile(context: Context, versionName: String): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        return File(dir, "mon-foyer-$versionName.apk")
    }

    /**
     * Telecharge l'APK de [update] dans le cache (reprend le fichier deja
     * present si un telechargement precedent a abouti).
     * [onProgress] est appele sur le dispatcher courant avec une valeur 0..1.
     */
    suspend fun download(
        context: Context,
        update: UpdateInfo,
        onProgress: suspend (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val target = updateFile(context, update.versionName)
        if (target.exists() && target.length() > 0) return@withContext target
        // nettoie les anciens APK pour ne pas accumuler dans le cache
        target.parentFile?.listFiles()?.forEach { it.delete() }
        val tmp = File(target.parentFile, target.name + ".part")
        val connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "MonFoyer-Android")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong
            var lastPercent = -1
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(done.toFloat() / total)
                            }
                        }
                    }
                }
            }
            if (total > 0 && tmp.length() != total) throw IOException("Telechargement incomplet")
            if (!tmp.renameTo(target)) throw IOException("Impossible de finaliser le fichier")
            target
        } finally {
            connection.disconnect()
            tmp.delete()
        }
    }

    /**
     * Lance l'installateur systeme sur [apk].
     * @return false si l'utilisateur doit d'abord autoriser les « sources
     * inconnues » pour Mon Foyer (l'ecran de reglages est alors ouvert).
     */
    fun install(context: Context, apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        return true
    }
}
