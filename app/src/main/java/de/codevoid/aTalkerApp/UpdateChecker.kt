package de.codevoid.aTalkerApp

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.URL

data class Release(val tagName: String, val downloadUrl: String)

object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/c0dev0id/aTalkerApp/releases"

    suspend fun latestPreRelease(currentVersion: String): Release? =
        withContext(Dispatchers.IO) {
            try {
                val releases = JSONArray(URL(RELEASES_URL).readText())
                for (i in 0 until releases.length()) {
                    val r = releases.getJSONObject(i)
                    if (!r.getBoolean("prerelease")) continue
                    val tag = r.getString("tag_name").trimStart('v')
                    if (!isNewer(tag, currentVersion)) continue
                    val assets = r.getJSONArray("assets")
                    for (j in 0 until assets.length()) {
                        val a = assets.getJSONObject(j)
                        if (a.getString("name").endsWith(".apk")) {
                            return@withContext Release(tag, a.getString("browser_download_url"))
                        }
                    }
                }
                null
            } catch (_: Exception) { null }
        }

    suspend fun download(context: Context, url: String): File =
        withContext(Dispatchers.IO) {
            val dest = File(context.cacheDir, "update.apk")
            URL(url).openStream().use { it.copyTo(dest.outputStream()) }
            dest
        }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apk,
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }
}
