package com.nodexati.xdownload.data

/** Uma versão do mesmo vídeo (resolução/bitrate) servida pelo X. */
data class VideoVariant(
    val url: String,
    val bitrate: Long,
    val width: Int,
    val height: Int,
) {
    /** "720p", "1080p"… ou "GIF" quando o X não informa resolução. */
    val label: String
        get() = when {
            width > 0 && height > 0 -> "${minOf(width, height)}p"
            height > 0 -> "${height}p"
            else -> "Original"
        }
}

enum class MediaKind { Video, Gif }

data class VideoMedia(
    val kind: MediaKind,
    val thumbnail: String?,
    val durationMs: Long?,
    /** Ordenadas da maior para a menor qualidade. */
    val variants: List<VideoVariant>,
)

data class PostInfo(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatar: String?,
    val text: String,
    val videos: List<VideoMedia>,
)

class ExtractException(message: String) : Exception(message)
