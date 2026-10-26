package com.nodexati.xdownload.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XLinkTest {
    @Test
    fun reconheceLinksDePosts() {
        val id = "1585341984679469056"
        listOf(
            "https://x.com/elonmusk/status/$id",
            "https://twitter.com/elonmusk/status/$id?s=46&t=abc",
            "https://mobile.twitter.com/elonmusk/status/$id",
            "https://fxtwitter.com/elonmusk/status/$id",
            "https://vxtwitter.com/elonmusk/status/$id/video/1",
            "https://x.com/i/status/$id",
            "https://x.com/i/web/status/$id",
            "Olha esse vídeo https://x.com/elonmusk/status/$id",
        ).forEach { assertEquals(it, id, XLink.statusId(it)) }
    }

    @Test
    fun ignoraLinksQueNaoSaoPosts() {
        assertNull(XLink.statusId("https://x.com/elonmusk"))
        assertNull(XLink.statusId("https://youtube.com/watch?v=123456789"))
        assertNull(XLink.statusId("https://notx.com/a/status/1234567890"))
    }

    @Test
    fun encontraLinkCurto() {
        assertEquals("https://t.co/D68z4K2wq7", XLink.shortLink("veja https://t.co/D68z4K2wq7"))
    }

    @Test
    fun tokenIgualAoDoEmbedOficial() {
        // Valores de referência gerados com o JavaScript do embed: ((id / 1e15) * Math.PI).toString(36)
        assertEquals("6dq1a2xwd93", XClient.syndicationToken("20"))
        assertEquals("3uchycv2wqc", XClient.syndicationToken("1585341984679469056"))
        assertEquals("4hfy2jnxph", XClient.syndicationToken("1850000000000000000"))
        assertEquals("2zqic77uqyk", XClient.syndicationToken("1234567890123456789"))
    }
}
