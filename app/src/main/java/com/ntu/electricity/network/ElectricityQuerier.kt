package com.ntu.electricity.network

import android.util.Log
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "EcoQuery"

class ElectricityQuerier(private val client: NtuHttpClient) {

    data class QueryResult(
        val date: String,
        val time: String,
        val electricity: String
    )

    fun query(
        studentId: String,
        password: String,
        campusId: String,
        buildingId: String,
        roomId: String
    ): QueryResult {
        val baseUrl = "https://pay.ntu.edu.cn"

        // Step 1: GET login page, follow redirects if any
        Log.d(TAG, "Step 1: GET $baseUrl/innerUserLogin")
        val loginPageResp = client.get("$baseUrl/innerUserLogin")
        Log.d(TAG, "Step 1: status=${loginPageResp.code}, redirect=${loginPageResp.header("Location")}")
        val loginPageRedirect = client.getRedirectLocation(loginPageResp)
        val loginPageHtml = if (loginPageRedirect != null) {
            loginPageResp.close()
            val redirectUrl = resolveUrl(baseUrl, loginPageRedirect)
            Log.d(TAG, "Step 1: following redirect to $redirectUrl")
            val r = client.get(redirectUrl)
            val html = r.body?.string() ?: throw Exception("Empty login page")
            r.close()
            html
        } else {
            val html = loginPageResp.body?.string() ?: throw Exception("Empty login page")
            loginPageResp.close()
            html
        }
        Log.d(TAG, "Step 1: page length=${loginPageHtml.length}, snippet=${loginPageHtml.take(300)}")

        val loginPageInfo = HtmlParser.parseLoginPage(loginPageHtml)
            ?: throw Exception("Cannot parse login page")
        Log.d(TAG, "Step 1: ssoLink=${loginPageInfo.ssoLink}, formAction=${loginPageInfo.formAction}")

        // Step 2: Follow SSO link
        val ssoUrl = if (loginPageInfo.ssoLink.startsWith("http")) {
            loginPageInfo.ssoLink
        } else {
            "$baseUrl${loginPageInfo.ssoLink}"
        }
        Log.d(TAG, "Step 2: GET ssoUrl=$ssoUrl")

        val ssoResp = client.get(ssoUrl, mapOf("Referer" to "$baseUrl/innerUserLogin"))
        Log.d(TAG, "Step 2: status=${ssoResp.code}, redirect=${ssoResp.header("Location")}")
        val ssoRedirect = client.getRedirectLocation(ssoResp)
        val ssoHtml = if (ssoRedirect != null) {
            ssoResp.close()
            val redirectUrl = resolveUrl("https://cas.ntu.edu.cn", ssoRedirect)
            val ssoLoginPageResp = client.get(
                redirectUrl,
                mapOf("Referer" to ssoUrl)
            )
            val ssoRedirect2 = client.getRedirectLocation(ssoLoginPageResp)
            if (ssoRedirect2 != null) {
                ssoLoginPageResp.close()
                val url2 = resolveUrl(redirectUrl, ssoRedirect2)
                val r2 = client.get(url2, mapOf("Referer" to redirectUrl))
                val html = r2.body?.string() ?: throw Exception("Empty SSO page")
                r2.close()
                html
            } else {
                val html = ssoLoginPageResp.body?.string() ?: throw Exception("Empty SSO page")
                ssoLoginPageResp.close()
                html
            }
        } else {
            val html = ssoResp.body?.string() ?: throw Exception("Empty SSO page")
            ssoResp.close()
            html
        }

        Log.d(TAG, "Step 2: ssoHtml length=${ssoHtml.length}, snippet=${ssoHtml.take(300)}")

        // Step 3: Get actual login form URL
        val ssoParsed = HtmlParser.parseSsoPage(ssoHtml)
            ?: throw Exception("Cannot parse SSO page")
        Log.d(TAG, "Step 3: ssoParsed=$ssoParsed")

        // If result looks like a URL (JS redirect), GET it to find the real login form
        val loginFormHtml: String
        val loginFormAction: String
        val hiddenFields: Map<String, String>
        if (ssoParsed.startsWith("http://") || ssoParsed.startsWith("https://")) {
            Log.d(TAG, "Step 3: JS redirect detected, GET $ssoParsed")
            val loginPageResp = client.get(ssoParsed)
            Log.d(TAG, "Step 3: loginPage status=${loginPageResp.code}, redirect=${loginPageResp.header("Location")}")
            val loginPageRedirect = client.getRedirectLocation(loginPageResp)
            if (loginPageRedirect != null) {
                loginPageResp.close()
                val redirectUrl = resolveUrl(ssoParsed, loginPageRedirect)
                Log.d(TAG, "Step 3: following redirect to $redirectUrl")
                val r = client.get(redirectUrl)
                loginFormHtml = r.body?.string() ?: throw Exception("Empty login form page")
                Log.d(TAG, "Step 3: loginFormHtml length=${loginFormHtml.length}")
                // Log relevant JS sections for password encryption detection
                val jsSections = Regex("""<script[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(loginFormHtml)
                    .map { it.groupValues[1].trim() }
                    .filter { it.contains("encrypt") || it.contains("password") || it.contains("AES") || it.contains("salt") || it.contains("pwdDefault") || it.contains("cllt") || it.contains("submitLogin") }
                    .toList()
                for ((i, js) in jsSections.withIndex()) {
                    Log.d(TAG, "Step 3: relevant JS[$i]: ${js.take(800)}")
                }
                r.close()
                val doc = Jsoup.parse(loginFormHtml)
                val form = doc.selectFirst("form")
                    ?: throw Exception("No form found on login page")
                val rawAction = form.attr("action")
                loginFormAction = if (rawAction.startsWith("http")) rawAction
                    else resolveUrl(redirectUrl, rawAction)
                hiddenFields = extractHiddenFields(doc)
            } else {
                loginFormHtml = loginPageResp.body?.string() ?: throw Exception("Empty login form page")
                loginPageResp.close()
                Log.d(TAG, "Step 3: loginFormHtml snippet=${loginFormHtml.take(500)}")
                val doc = Jsoup.parse(loginFormHtml)
                val form = doc.selectFirst("form")
                    ?: throw Exception("No form found on login page")
                val rawAction = form.attr("action")
                loginFormAction = if (rawAction.startsWith("http")) rawAction
                    else resolveUrl(ssoParsed, rawAction)
                hiddenFields = extractHiddenFields(doc)
            }
        } else {
            loginFormHtml = ssoHtml
            loginFormAction = if (ssoParsed.startsWith("http")) ssoParsed
                else "https://authserver.ntu.edu.cn$ssoParsed"
            hiddenFields = extractHiddenFields(Jsoup.parse(ssoHtml))
        }
        Log.d(TAG, "Step 3: loginFormAction=$loginFormAction, hiddenFields=$hiddenFields")

        // Step 4: POST login credentials (merge hidden fields + credentials)
        // Detect password field name from form
        val loginDoc = Jsoup.parse(loginFormHtml)
        val passwordFieldName = loginDoc.select("form input[type=password]").firstOrNull()?.attr("name") ?: "password"
        Log.d(TAG, "Step 4: detected password field name=$passwordFieldName")

        val formFields = hiddenFields.toMutableMap()
        formFields["username"] = studentId
        formFields[passwordFieldName] = password
        // Remove empty lt/uuid if present (some CAS versions require them removed)
        if (formFields["lt"].isNullOrEmpty()) formFields.remove("lt")
        if (formFields["uuid"].isNullOrEmpty()) formFields.remove("uuid")

        Log.d(TAG, "Step 4: POST $loginFormAction, fields=${formFields.keys}")
        val loginResp = client.post(
            loginFormAction,
            formFields,
            mapOf(
                "Referer" to loginFormAction,
                "Origin" to java.net.URI(loginFormAction).let { "${it.scheme}://${it.host}" },
                "Content-Type" to "application/x-www-form-urlencoded"
            )
        )
        Log.d(TAG, "Step 4: status=${loginResp.code}, redirect=${loginResp.header("Location")}")

        val loginRedirect = client.getRedirectLocation(loginResp)
        val loginRespBody = if (loginRedirect == null) {
            loginResp.body?.string() ?: ""
        } else ""
        loginResp.close()

        if (loginRedirect == null) {
            // Check if there's an error message in the response body
            val errorSnippet = loginRespBody.take(500)
            Log.e(TAG, "Step 4: no redirect, body snippet=$errorSnippet")
            throw Exception("Login failed - no redirect")
        }
        Log.d(TAG, "Step 4: loginRedirect=$loginRedirect")

        // Step 5: Follow login redirect chain back to pay system
        var currentUrl = resolveUrl(loginFormAction, loginRedirect)
        Log.d(TAG, "Step 5: following redirect chain from $currentUrl")

        var mainPageHtml: String? = null
        var redirectCount = 0
        while (redirectCount < 10) {
            val resp = client.get(currentUrl, mapOf("Referer" to loginFormAction))
            val location = client.getRedirectLocation(resp)
            if (location != null) {
                resp.close()
                currentUrl = resolveUrl(currentUrl, location)
                redirectCount++
            } else {
                mainPageHtml = resp.body?.string()
                resp.close()
                break
            }
        }

        if (mainPageHtml == null) {
            throw Exception("Failed to reach main page after login")
        }
        Log.d(TAG, "Step 5: mainPageHtml length=${mainPageHtml.length}, snippet=${mainPageHtml.take(300)}")

        // Step 6: Find and follow electricity details link
        val elecLink = HtmlParser.parseElecDetailsLink(mainPageHtml)
            ?: throw Exception("Cannot find electricity link")
        Log.d(TAG, "Step 6: elecLink=$elecLink")

        val elecUrl = resolveUrl(baseUrl, elecLink)
        Log.d(TAG, "Step 6: GET elecUrl=$elecUrl")

        val elecPageResp = client.get(elecUrl, mapOf("Referer" to currentUrl))
        Log.d(TAG, "Step 6: status=${elecPageResp.code}")
        val elecPageHtml = elecPageResp.body?.string()
            ?: throw Exception("Empty electricity page")
        Log.d(TAG, "Step 6: elecPageHtml length=${elecPageHtml.length}, snippet=${elecPageHtml.take(300)}")
        elecPageResp.close()

        // Step 7: POST select campus/building/room and query
        Log.d(TAG, "Step 7: POST campus=$campusId, building=$buildingId, room=$roomId")
        val queryParams = mutableMapOf(
            "schoolid" to campusId,
            "building" to buildingId,
            "room" to roomId
        )

        val queryResp = client.post(
            elecUrl,
            queryParams,
            mapOf(
                "Referer" to elecUrl,
                "Origin" to baseUrl,
                "Content-Type" to "application/x-www-form-urlencoded"
            )
        )

        var queryHtml: String? = null
        val queryRedirect = client.getRedirectLocation(queryResp)
        Log.d(TAG, "Step 7: queryResp status=${queryResp.code}, redirect=$queryRedirect")
        if (queryRedirect != null) {
            queryResp.close()
            val redirectUrl = resolveUrl(baseUrl, queryRedirect)
            val redirectResp = client.get(redirectUrl, mapOf("Referer" to elecUrl))
            queryHtml = redirectResp.body?.string()
            redirectResp.close()
        } else {
            queryHtml = queryResp.body?.string()
            queryResp.close()
        }

        if (queryHtml == null) {
            throw Exception("Empty query result page")
        }
        Log.d(TAG, "Step 7: queryHtml length=${queryHtml.length}, snippet=${queryHtml.take(500)}")

        // Step 8: Parse result
        val electricityValue = HtmlParser.parseElectricityValue(queryHtml)
            ?: throw Exception("Cannot parse electricity value")

        val now = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        return QueryResult(
            date = dateFormat.format(now),
            time = timeFormat.format(now),
            electricity = electricityValue
        )
    }

    private fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
        if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
            return relativeOrAbsolute
        }
        val uri = java.net.URI(baseUrl)
        if (relativeOrAbsolute.startsWith("/")) {
            return "${uri.scheme}://${uri.host}$relativeOrAbsolute"
        }
        val basePath = baseUrl.substringBeforeLast("/")
        return "$basePath/$relativeOrAbsolute"
    }

    private fun extractHiddenFields(doc: org.jsoup.nodes.Document): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        val form = doc.selectFirst("form") ?: return fields
        // Log ALL input fields for debugging
        for (input in form.select("input")) {
            val name = input.attr("name")
            val type = input.attr("type")
            val value = input.attr("value")
            if (name.isNotBlank()) {
                Log.d(TAG, "  form field: name=$name, type=$type, value=${value.take(50)}")
                if (type.equals("hidden", ignoreCase = true)) {
                    fields[name] = value
                }
            }
        }
        return fields
    }
}
