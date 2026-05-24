package com.ntu.electricity.network

import org.jsoup.Jsoup

object HtmlParser {

    data class LoginPageInfo(
        val formAction: String,
        val ssoLink: String
    )

    fun parseLoginPage(html: String): LoginPageInfo? {
        val doc = Jsoup.parse(html)
        val ssoImg = doc.selectFirst("img[src=/images/sso_img.png]") ?: return null
        val ssoLink = ssoImg.closest("a")?.attr("href") ?: return null
        val form = doc.selectFirst("form") ?: return null
        val formAction = form.attr("action")
        return LoginPageInfo(
            formAction = formAction,
            ssoLink = ssoLink
        )
    }

    fun parseSsoPage(html: String): String? {
        // Try form action first
        val doc = Jsoup.parse(html)
        val form = doc.selectFirst("form")
        if (form != null) return form.attr("action")

        // Try JavaScript redirect: window.location.href='...'
        val jsRedirect = Regex("""window\.location\.href\s*=\s*['"]([^'"]+)['"]""")
            .find(html)
        if (jsRedirect != null) return jsRedirect.groupValues[1]

        // Try window.location='...'
        val jsRedirect2 = Regex("""window\.location\s*=\s*['"]([^'"]+)['"]""")
            .find(html)
        if (jsRedirect2 != null) return jsRedirect2.groupValues[1]

        return null
    }

    fun parseElecDetailsLink(html: String): String? {
        val doc = Jsoup.parse(html)
        val link = doc.selectFirst("a[onclick*=elecdetails5118E017]") ?: return null
        val onclick = link.attr("onclick")
        val urlPattern = Regex("""window\.location\s*=\s*['"]([^'"]+)['"]""")
        val match = urlPattern.find(onclick)
        return match?.groupValues?.get(1)
    }

    fun parseElectricityValue(html: String): String? {
        val doc = Jsoup.parse(html)
        val td = doc.selectFirst("td.table-2#zhye") ?: return null
        val rawText = td.text()
        val cleaned = rawText.replace(Regex("[^\\d.]"), "")
        val numberMatch = Regex("""\d+\.?\d*""").find(cleaned)
        return numberMatch?.value
    }
}
