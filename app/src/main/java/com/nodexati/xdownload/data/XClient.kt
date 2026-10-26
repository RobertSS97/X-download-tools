package com.nodexati.xdownload.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.PI

/**
 * Busca os vídeos de um post do X sem login.
 *
 * 1. Endpoint público de incorporação (cdn.syndication.twimg.com), o mesmo usado pelos
 *    embeds de posts em sites.
 * 2. Se falhar (post sensível, limite de acesso), tenta a API pública do FxTwitter.
 */
object XClient {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val resolutionInUrl = Regex("""/(\d{2,5})x(\d{2,5})/""")

    suspend fun fetch(input: String): PostInfo = withContext(Dispatchers.IO) {
        val id = XLink.statusId(input)
            ?: XLink.shortLink(input)?.let { resolveShort(it) }?.let { XLink.statusId(it) }
            ?: throw ExtractException("Esse link não parece ser de um post do X.")

        val errors = mutableListOf<String>()
        for (source in listOf(::fromSyndication, ::fromFxTwitter)) {
            try {
                val post = source(id)
                if (post.videos.isNotEmpty()) return@withContext post
                errors += "sem vídeo"
            } catch (e: ExtractException) {
                errors += e.message.orEmpty()
            } catch (e: IOException) {
                errors += "rede"
            } catch (e: Exception) {
                errors += "resposta inesperada"
            }
        }
        throw when {
            errors.all { it == "rede" } -> ExtractException("Sem conexão. Verifique a internet e tente de novo.")
            errors.any { it == "sem vídeo" } -> ExtractException("Esse post não tem vídeo nem GIF.")
            else -> ExtractException("Não foi possível abrir esse post. Ele pode ser privado, ter sido apagado ou ter conteúdo restrito.")
        }
    }

    private fun resolveShort(url: String): String? {
        val request = Request.Builder().url(url).header("User-Agent", "curl/8.0").build()
        return http.newCall(request).execute().use { it.request.url.toString() }
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            if (response.code == 404) throw ExtractException("não encontrado")
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw ExtractException("resposta vazia")
            return JSONObject(body)
        }
    }

    // ---- cdn.syndication.twimg.com ----

    private fun fromSyndication(id: String): PostInfo {
        val json = getJson("https://cdn.syndication.twimg.com/tweet-result?id=$id&lang=pt&token=${syndicationToken(id)}")
        if (json.optString("__typename") == "TweetTombstone") throw ExtractException("restrito")
        val user = json.optJSONObject("user") ?: JSONObject()
        val media = json.optJSONArray("mediaDetails") ?: JSONArray()
        val videos = buildList {
            for (i in 0 until media.length()) {
                val item = media.getJSONObject(i)
                val type = item.optString("type")
                if (type != "video" && type != "animated_gif") continue
                val info = item.optJSONObject("video_info") ?: continue
                val variants = parseVariants(info.optJSONArray("variants"), urlKey = "url", typeKey = "content_type")
                if (variants.isEmpty()) continue
                add(
                    VideoMedia(
                        kind = if (type == "animated_gif") MediaKind.Gif else MediaKind.Video,
                        thumbnail = item.optString("media_url_https").ifBlank { null },
                        durationMs = info.optLong("duration_millis", 0).takeIf { it > 0 },
                        variants = variants,
                    )
                )
            }
        }
        return PostInfo(
            id = id,
            authorName = user.optString("name"),
            authorHandle = user.optString("screen_name"),
            authorAvatar = user.optString("profile_image_url_https").ifBlank { null },
            text = cleanText(json.optString("text")),
            videos = videos,
        )
    }

    /** Token exigido pelo endpoint de embed: (id / 1e15 * π) em base 36, sem zeros nem ponto. */
    internal fun syndicationToken(id: String): String {
        val value = id.toDouble() / 1e15 * PI
        return toBase36(value).replace(Regex("(0+|\\.)"), "")
    }

    /**
     * Number.prototype.toString(36) do JavaScript (algoritmo DoubleToRadixCString do V8):
     * gera dígitos da fração enquanto ainda carregam precisão do double, com arredondamento.
     */
    private fun toBase36(value: Double): String {
        val digits = "0123456789abcdefghijklmnopqrstuvwxyz"
        val radix = 36
        var integer = Math.floor(value)
        var fraction = value - integer
        var delta = maxOf(0.5 * Math.ulp(value), java.lang.Double.MIN_VALUE)
        val frac = StringBuilder()
        if (fraction >= delta) {
            do {
                fraction *= radix
                delta *= radix
                val digit = fraction.toInt()
                frac.append(digits[digit])
                fraction -= digit
                if (fraction > 0.5 || (fraction == 0.5 && (digit and 1) == 1)) {
                    if (fraction + delta > 1) {
                        // Arredonda para cima, propagando o "vai um".
                        while (true) {
                            if (frac.isEmpty()) {
                                integer += 1
                                break
                            }
                            val last = digits.indexOf(frac.last())
                            frac.setLength(frac.length - 1)
                            if (last + 1 < radix) {
                                frac.append(digits[last + 1])
                                break
                            }
                        }
                        break
                    }
                }
            } while (fraction >= delta)
        }
        var whole = integer.toLong()
        val intPart = if (whole == 0L) "0" else buildString {
            while (whole > 0) {
                insert(0, digits[(whole % radix).toInt()])
                whole /= radix
            }
        }
        return if (frac.isEmpty()) intPart else "$intPart.$frac"
    }

    // ---- api.fxtwitter.com ----

    private fun fromFxTwitter(id: String): PostInfo {
        val root = getJson("https://api.fxtwitter.com/status/$id")
        val tweet = root.optJSONObject("tweet") ?: throw ExtractException("não encontrado")
        val author = tweet.optJSONObject("author") ?: JSONObject()
        val list = tweet.optJSONObject("media")?.optJSONArray("videos") ?: JSONArray()
        val videos = buildList {
            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val fromList = parseVariants(item.optJSONArray("variants"), urlKey = "url", typeKey = "content_type")
                val variants = fromList.ifEmpty {
                    val url = item.optString("url")
                    if (url.isBlank()) emptyList()
                    else listOf(VideoVariant(url, 0, item.optInt("width"), item.optInt("height")))
                }
                if (variants.isEmpty()) continue
                add(
                    VideoMedia(
                        kind = if (item.optString("type") == "gif") MediaKind.Gif else MediaKind.Video,
                        thumbnail = item.optString("thumbnail_url").ifBlank { null },
                        durationMs = item.optDouble("duration", 0.0).takeIf { it > 0 }?.let { (it * 1000).toLong() },
                        variants = variants,
                    )
                )
            }
        }
        return PostInfo(
            id = id,
            authorName = author.optString("name"),
            authorHandle = author.optString("screen_name"),
            authorAvatar = author.optString("avatar_url").ifBlank { null },
            text = cleanText(tweet.optString("text")),
            videos = videos,
        )
    }

    // ---- comum ----

    private fun parseVariants(array: JSONArray?, urlKey: String, typeKey: String): List<VideoVariant> {
        if (array == null) return emptyList()
        val result = mutableListOf<VideoVariant>()
        for (i in 0 until array.length()) {
            val v = array.optJSONObject(i) ?: continue
            val url = v.optString(urlKey)
            val type = v.optString(typeKey)
            // HLS (m3u8) não é um arquivo baixável direto; ficam só os MP4.
            if (url.isBlank() || (type.isNotBlank() && type != "video/mp4") || url.contains(".m3u8")) continue
            val match = resolutionInUrl.find(url)
            result += VideoVariant(
                url = url,
                bitrate = v.optLong("bitrate", 0),
                width = match?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                height = match?.groupValues?.get(2)?.toIntOrNull() ?: 0,
            )
        }
        return result
            .distinctBy { it.url.substringBefore('?') }
            .sortedWith(compareByDescending<VideoVariant> { it.width * it.height }.thenByDescending { it.bitrate })
    }

    /** Tira o link t.co que o X coloca no fim do texto de posts com mídia. */
    private fun cleanText(text: String): String =
        text.replace(Regex("""\s*https://t\.co/\w+\s*$"""), "").trim()
}
