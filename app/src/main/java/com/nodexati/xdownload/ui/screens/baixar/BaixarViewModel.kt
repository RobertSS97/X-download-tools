package com.nodexati.xdownload.ui.screens.baixar

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nodexati.xdownload.data.DownloadState
import com.nodexati.xdownload.data.Downloader
import com.nodexati.xdownload.data.ExtractException
import com.nodexati.xdownload.data.HistoryEntry
import com.nodexati.xdownload.data.HistoryStore
import com.nodexati.xdownload.data.PostInfo
import com.nodexati.xdownload.data.VideoVariant
import com.nodexati.xdownload.data.XClient
import com.nodexati.xdownload.data.XLink
import com.nodexati.xdownload.settings.AppSettings
import com.nodexati.xdownload.settings.DefaultQuality
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Etapas da tela Baixar. */
sealed interface BaixarStep {
    data object Idle : BaixarStep
    data object Loading : BaixarStep
    data class Ready(val post: PostInfo) : BaixarStep
}

class BaixarViewModel(app: Application) : AndroidViewModel(app) {
    private val downloader = Downloader(app)
    private val history = HistoryStore.get(app)
    private val settings = AppSettings.get(app)

    var link by mutableStateOf("")
        private set
    var step by mutableStateOf<BaixarStep>(BaixarStep.Idle)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Vídeo do post escolhido (posts podem ter até 4) e a qualidade escolhida. */
    var selectedVideo by mutableStateOf(0)
        private set
    var selectedVariant by mutableStateOf(0)
        private set

    /** Downloads deste post, por vídeo+qualidade, para mostrar progresso no botão. */
    var downloads by mutableStateOf<Map<String, DownloadState>>(emptyMap())
        private set

    private var fetchJob: Job? = null

    fun onLinkChange(value: String) {
        link = value
        error = null
        if (value.isBlank()) reset()
    }

    /** Link recebido pelo "Compartilhar" do app do X: preenche e já busca. */
    fun onShared(text: String) {
        link = XLink.firstUrl(text) ?: text
        error = null
        fetch()
    }

    fun fetch() {
        val input = link.trim()
        if (input.isEmpty()) return
        if (!XLink.looksLikeX(input)) {
            error = "Cole o link de um post do X (x.com/…/status/…)."
            return
        }
        fetchJob?.cancel()
        error = null
        step = BaixarStep.Loading
        fetchJob = viewModelScope.launch {
            step = try {
                val post = XClient.fetch(input)
                selectedVideo = 0
                selectedVariant = defaultVariant(post.videos[0].variants)
                downloads = emptyMap()
                BaixarStep.Ready(post)
            } catch (e: ExtractException) {
                error = e.message
                BaixarStep.Idle
            } catch (e: Exception) {
                error = "Algo deu errado. Tente de novo."
                BaixarStep.Idle
            }
        }
    }

    fun selectVideo(index: Int) {
        val post = (step as? BaixarStep.Ready)?.post ?: return
        selectedVideo = index
        selectedVariant = defaultVariant(post.videos[index].variants)
    }

    fun selectVariant(index: Int) {
        selectedVariant = index
    }

    fun reset() {
        fetchJob?.cancel()
        step = BaixarStep.Idle
        downloads = emptyMap()
    }

    fun key(video: Int, variant: VideoVariant) = "$video|${variant.url}"

    fun download() {
        val post = (step as? BaixarStep.Ready)?.post ?: return
        val media = post.videos[selectedVideo]
        val variant = media.variants[selectedVariant]
        val key = key(selectedVideo, variant)
        if (downloads[key] is DownloadState.Running) return

        val fileName = Downloader.fileName(post, selectedVideo, variant)
        val title = "@${post.authorHandle} · ${variant.label}"
        val id = try {
            downloader.enqueue(variant.url, fileName, title)
        } catch (e: Exception) {
            downloads = downloads + (key to DownloadState.Failed("Não foi possível iniciar o download."))
            return
        }
        history.add(
            HistoryEntry(
                downloadId = id,
                postId = post.id,
                authorName = post.authorName,
                authorHandle = post.authorHandle,
                text = post.text,
                thumbnail = media.thumbnail,
                quality = variant.label,
                durationMs = media.durationMs,
                fileName = fileName,
                createdAt = System.currentTimeMillis(),
            )
        )
        downloads = downloads + (key to DownloadState.Running(0, 0))
        viewModelScope.launch {
            downloader.observe(id).collect { state -> downloads = downloads + (key to state) }
        }
    }

    private fun defaultVariant(variants: List<VideoVariant>): Int = when (settings.defaultQuality) {
        DefaultQuality.Best -> 0
        DefaultQuality.Smallest -> variants.lastIndex
        DefaultQuality.Medium -> variants.lastIndex / 2
    }
}
