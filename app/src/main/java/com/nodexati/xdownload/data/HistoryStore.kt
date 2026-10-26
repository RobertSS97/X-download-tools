package com.nodexati.xdownload.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.json.JSONArray
import org.json.JSONObject

/** Um vídeo baixado, guardado no histórico. */
data class HistoryEntry(
    val downloadId: Long,
    val postId: String,
    val authorName: String,
    val authorHandle: String,
    val text: String,
    val thumbnail: String?,
    val quality: String,
    val durationMs: Long?,
    val fileName: String,
    val createdAt: Long,
) {
    val postUrl: String get() = "https://x.com/${authorHandle.ifBlank { "i" }}/status/$postId"
}

/** Histórico local de downloads (SharedPreferences em JSON; poucas dezenas de itens). */
class HistoryStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("xdownload_history", Context.MODE_PRIVATE)
    val items: SnapshotStateList<HistoryEntry> = mutableStateListOf<HistoryEntry>().apply { addAll(load()) }

    fun add(entry: HistoryEntry) {
        items.removeAll { it.downloadId == entry.downloadId }
        items.add(0, entry)
        while (items.size > MAX) items.removeAt(items.lastIndex)
        save()
    }

    fun remove(entry: HistoryEntry) {
        items.remove(entry)
        save()
    }

    fun clear() {
        items.clear()
        save()
    }

    private fun load(): List<HistoryEntry> = runCatching {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            HistoryEntry(
                downloadId = o.getLong("downloadId"),
                postId = o.getString("postId"),
                authorName = o.optString("authorName"),
                authorHandle = o.optString("authorHandle"),
                text = o.optString("text"),
                thumbnail = o.optString("thumbnail").ifBlank { null },
                quality = o.optString("quality"),
                durationMs = o.optLong("durationMs", 0).takeIf { it > 0 },
                fileName = o.optString("fileName"),
                createdAt = o.optLong("createdAt"),
            )
        }
    }.getOrDefault(emptyList())

    private fun save() {
        val array = JSONArray()
        items.forEach { e ->
            array.put(
                JSONObject()
                    .put("downloadId", e.downloadId)
                    .put("postId", e.postId)
                    .put("authorName", e.authorName)
                    .put("authorHandle", e.authorHandle)
                    .put("text", e.text)
                    .put("thumbnail", e.thumbnail ?: "")
                    .put("quality", e.quality)
                    .put("durationMs", e.durationMs ?: 0)
                    .put("fileName", e.fileName)
                    .put("createdAt", e.createdAt)
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val KEY = "items"
        private const val MAX = 200

        @Volatile private var instance: HistoryStore? = null
        fun get(context: Context): HistoryStore =
            instance ?: synchronized(this) { instance ?: HistoryStore(context.applicationContext).also { instance = it } }
    }
}
