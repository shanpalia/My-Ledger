package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val PREFS = "my_ledger_update_settings"
private const val KEY_URL = "update_info_url"

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
    val forceUpdate: Boolean
)

sealed class UpdateCheckResult {
    object UpToDate : UpdateCheckResult()
    data class Available(val info: UpdateInfo) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object AppUpdateManager {
    fun getUpdateInfoUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URL, "").orEmpty()

    fun saveUpdateInfoUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_URL, url.trim()).apply()
    }

    suspend fun check(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val urlText = getUpdateInfoUrl(context)
        if (urlText.isBlank()) return@withContext UpdateCheckResult.Error("Update server URL is not configured yet.")
        try {
            val conn = (URL(urlText).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
                useCaches = false
            }
            val code = conn.responseCode
            if (code !in 200..299) return@withContext UpdateCheckResult.Error("Update server returned HTTP $code")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val info = UpdateInfo(
                versionCode = json.optInt("versionCode", BuildConfig.VERSION_CODE),
                versionName = json.optString("versionName", BuildConfig.VERSION_NAME),
                apkUrl = json.optString("apkUrl", ""),
                notes = json.optString("notes", ""),
                forceUpdate = json.optBoolean("forceUpdate", false)
            )
            if (info.versionCode > BuildConfig.VERSION_CODE) UpdateCheckResult.Available(info) else UpdateCheckResult.UpToDate
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Could not check for updates")
        }
    }

    fun openUpdate(context: Context, apkUrl: String) {
        if (apkUrl.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
