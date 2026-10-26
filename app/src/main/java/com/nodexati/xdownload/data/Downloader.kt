package com.nodexati.xdownload.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Estado de um download acompanhado pela tela. */
sealed interface DownloadState {
    data class Running(val downloaded: Long, val total: Long) : DownloadState {
        val fraction: Float? get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null
    }
    data class Done(val uri: Uri?, val size: Long) : DownloadState
    data class Failed(val reason: String) : DownloadState
}

/**
 * Baixa pelo DownloadManager do sistema: continua mesmo com o app fechado, mostra a
 * notificação de progresso e salva em Filmes/XDownload (aparece na galeria).
 */
class Downloader(private val context: Context) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueue(url: String, fileName: String, title: String): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title)
            .setDescription("XDownload")
            .setMimeType("video/mp4")
            .addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) XDownload")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "$FOLDER/$fileName")
        return manager.enqueue(request)
    }

    /** Acompanha o download até terminar (consulta o sistema a cada 400 ms). */
    fun observe(id: Long): Flow<DownloadState> = flow {
        while (true) {
            val state = query(id)
            emit(state)
            if (state !is DownloadState.Running) break
            delay(400)
        }
    }.flowOn(Dispatchers.IO)

    fun query(id: Long): DownloadState {
        manager.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (c == null || !c.moveToFirst()) return DownloadState.Failed("Download cancelado.")
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> DownloadState.Done(manager.getUriForDownloadedFile(id), total)
                DownloadManager.STATUS_FAILED -> {
                    val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    DownloadState.Failed(failureText(reason))
                }
                else -> DownloadState.Running(done, total)
            }
        }
    }

    fun uriFor(id: Long): Uri? = runCatching { manager.getUriForDownloadedFile(id) }.getOrNull()

    fun remove(id: Long) {
        runCatching { manager.remove(id) }
    }

    private fun failureText(reason: Int): String = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Sem espaço no celular."
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Armazenamento indisponível."
        DownloadManager.ERROR_HTTP_DATA_ERROR, DownloadManager.ERROR_TOO_MANY_REDIRECTS,
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "O X recusou o arquivo. Tente outra qualidade."
        else -> "Não foi possível baixar. Tente de novo."
    }

    companion object {
        const val FOLDER = "XDownload"

        fun fileName(post: PostInfo, index: Int, variant: VideoVariant): String {
            val handle = post.authorHandle.ifBlank { "x" }.replace(Regex("[^A-Za-z0-9_]"), "")
            val part = if (index > 0) "_${index + 1}" else ""
            val quality = variant.label.lowercase()
            return "${handle}_${post.id}$part" + "_$quality.mp4"
        }
    }
}
