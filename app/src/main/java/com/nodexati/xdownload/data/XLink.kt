package com.nodexati.xdownload.data

/** Reconhece links de posts do X/Twitter (inclusive espelhos como fxtwitter e vxtwitter). */
object XLink {
    private val statusPattern = Regex(
        """(?:^|[/.])(?:twitter|x|fxtwitter|vxtwitter|fixupx|fixvx|twittpr|nitter)\.(?:com|net)/(?:#!/)?(?:[A-Za-z0-9_]+/|i/web/|i/)?status(?:es)?/(\d{5,25})""",
        RegexOption.IGNORE_CASE,
    )
    private val shortPattern = Regex("""https?://t\.co/[A-Za-z0-9]+""", RegexOption.IGNORE_CASE)
    private val anyUrl = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    /** Id do post contido no texto, ou null. */
    fun statusId(text: String): String? = statusPattern.find(text)?.groupValues?.get(1)

    /** Link curto t.co contido no texto (precisa ser resolvido pela rede). */
    fun shortLink(text: String): String? = shortPattern.find(text)?.value

    fun looksLikeX(text: String): Boolean = statusId(text) != null || shortLink(text) != null

    /** Primeiro link do texto compartilhado (o app do X manda "texto + link"). */
    fun firstUrl(text: String): String? = anyUrl.find(text)?.value?.trimEnd('.', ',', ')', '"')
}
