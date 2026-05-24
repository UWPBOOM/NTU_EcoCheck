package com.ntu.electricity.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ntu.electricity.EcoCheckApplication
import com.ntu.electricity.data.local.entity.QueryHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "QueryVM"

sealed class QueryState {
    data object Idle : QueryState()
    data class Loading(val message: String) : QueryState()
    data class Success(val electricity: String, val date: String, val time: String) : QueryState()
    data class Error(val message: String) : QueryState()
}

@SuppressLint("SetJavaScriptEnabled")
class QueryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EcoCheckApplication
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null

    private val _state = MutableStateFlow<QueryState>(QueryState.Idle)
    val state: StateFlow<QueryState> = _state.asStateFlow()

    private var pendingQuery: PendingQuery? = null

    data class PendingQuery(
        val studentId: String,
        val password: String,
        val campusId: String,
        val buildingId: String,
        val roomId: String,
        val campusName: String,
        val buildingName: String,
        val roomName: String
    )

    fun startQuery(
        studentId: String,
        password: String,
        campusId: String,
        buildingId: String,
        roomId: String,
        campusName: String,
        buildingName: String,
        roomName: String
    ) {
        if (studentId.isBlank() || password.isBlank() || campusId.isBlank() || buildingId.isBlank() || roomId.isBlank()) {
            _state.value = QueryState.Error("请填写完整信息")
            return
        }

        pendingQuery = PendingQuery(studentId, password, campusId, buildingId, roomId, campusName, buildingName, roomName)
        _state.value = QueryState.Loading("正在初始化…")

        if (webView == null) {
            initWebView()
        } else {
            webView?.loadUrl("https://pay.ntu.edu.cn/innerUserLogin")
        }
    }

    private fun initWebView() {
        val context = getApplication<EcoCheckApplication>()
        handler.post {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString =
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

                val cookieMgr = CookieManager.getInstance()
                cookieMgr.setAcceptCookie(true)
                cookieMgr.setAcceptThirdPartyCookies(this, true)

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onStep(message: String) {
                        handler.post { _state.value = QueryState.Loading(message) }
                    }

                    @JavascriptInterface
                    fun onPageReady(pageType: String) {
                        handler.post { handlePageReady(pageType) }
                    }

                    @JavascriptInterface
                    fun onResult(
                        campusName: String, buildingName: String, roomName: String,
                        electricity: String, date: String, time: String
                    ) {
                        handler.post {
                            _state.value = QueryState.Success(electricity, date, time)
                            saveHistory(campusName, buildingName, roomName, electricity, date, time)
                        }
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.d(TAG, "Page started: $url")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "Page finished: $url")
                        view?.evaluateJavascript(detectPageScript()) { result ->
                            val pageType = result?.removeSurrounding("\"") ?: "unknown"
                            Log.d(TAG, "Detected: $pageType")
                            handlePageReady(pageType)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {}

                loadUrl("https://pay.ntu.edu.cn/innerUserLogin")
            }
        }
    }

    private fun handlePageReady(pageType: String) {
        val wv = webView ?: return
        val q = pendingQuery ?: return

        when (pageType) {
            "login_page" -> {
                _state.value = QueryState.Loading("正在打开统一身份认证…")
                wv.evaluateJavascript(clickSSOScript(), null)
            }
            "auth_page" -> {
                _state.value = QueryState.Loading("正在填写账号密码…")
                wv.evaluateJavascript(fillOnlyScript(q.studentId, q.password)) {
                    _state.value = QueryState.Loading("正在提交登录…")
                    wv.evaluateJavascript(submitLoginScript(), null)
                }
            }
            "main_page" -> {
                _state.value = QueryState.Loading("正在进入电费查询…")
                wv.evaluateJavascript(clickElecScript(), null)
            }
            "elec_campus" -> {
                _state.value = QueryState.Loading("正在选择校区…")
                wv.evaluateJavascript(selectCampusScript(q.campusId), null)
            }
            "elec_building" -> {
                _state.value = QueryState.Loading("正在选择楼栋…")
                wv.evaluateJavascript(selectBuildingScript(q.buildingId), null)
            }
            "elec_room" -> {
                _state.value = QueryState.Loading("正在选择房间并查询…")
                wv.evaluateJavascript(selectRoomOnlyScript(q.roomId), null)
            }
            "result_page" -> {
                _state.value = QueryState.Loading("正在读取结果…")
                wv.evaluateJavascript(readResultScript()) { result ->
                    val elec = result?.removeSurrounding("\"")?.replace(Regex("[^\\d.]"), "") ?: ""
                    if (elec.isNotEmpty()) {
                        val now = java.util.Date()
                        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(now)
                        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(now)
                        _state.value = QueryState.Success(elec, date, time)
                        saveHistory(q.campusName, q.buildingName, q.roomName, elec, date, time)
                    }
                }
            }
            "error_page" -> {
                _state.value = QueryState.Error("服务器繁忙，请稍后再试")
            }
        }
    }

    private fun saveHistory(campus: String, building: String, room: String, electricity: String, date: String, time: String) {
        viewModelScope.launch(Dispatchers.IO) {
            app.database.queryHistoryDao().insert(
                QueryHistoryEntity(
                    date = date, time = time, electricity = electricity,
                    campus = campus, building = building, room = room
                )
            )
        }
    }

    fun dismiss() {
        _state.value = QueryState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        webView?.destroy()
        webView = null
    }

    // ─── JS Scripts ───

    private fun detectPageScript() = """
    (function() {
        if (document.querySelector('img[src="/images/sso_img.png"]')) return 'login_page';
        if (document.querySelector('input[type="password"]') && location.host.indexOf('authserver') >= 0) return 'auth_page';
        var zhye = document.querySelector('td#zhye');
        if (zhye && zhye.textContent.trim().length > 0) return 'result_page';
        var s = document.getElementById('schoolid');
        var b = document.getElementById('building');
        var r = document.getElementById('room');
        if (s && b && r) {
            if (r.options.length > 1) return 'elec_room';
            if (b.options.length > 1) return 'elec_building';
            return 'elec_campus';
        }
        if (document.querySelector('a[onclick*="elecdetails5118E017"]')) return 'main_page';
        return 'unknown';
    })();
    """.trimIndent()

    private fun clickSSOScript() = """
    (function() {
        var img = document.querySelector('img[src="/images/sso_img.png"]');
        if (img) { var link = img.closest('a'); if (link) { link.click(); return 'ok'; } }
        return 'fail';
    })();
    """.trimIndent()

    private fun fillOnlyScript(studentId: String, password: String) = """
    (function() {
        var u = document.querySelector('input[name="username"]') || document.querySelector('input#username');
        var p = document.querySelector('input[type="password"]');
        if (u && p) {
            u.value = '$studentId';
            p.value = '$password';
            u.dispatchEvent(new Event('input', {bubbles:true}));
            p.dispatchEvent(new Event('input', {bubbles:true}));
            u.dispatchEvent(new Event('change', {bubbles:true}));
            p.dispatchEvent(new Event('change', {bubbles:true}));
            return 'filled';
        }
        return 'no_fields';
    })();
    """.trimIndent()

    private fun submitLoginScript() = """
    (function() {
        var btn = document.getElementById('login_submit')
            || document.querySelector('input[type="submit"]')
            || document.querySelector('button[type="submit"]');
        if (btn) { btn.click(); return 'ok'; }
        return 'no_btn';
    })();
    """.trimIndent()

    private fun clickElecScript() = """
    (function() {
        var link = document.querySelector('a[onclick*="elecdetails5118E017"]');
        if (link) { link.click(); return 'ok'; }
        return 'fail';
    })();
    """.trimIndent()

    private fun selectCampusScript(campusId: String) = """
    (function() {
        var s = document.getElementById('schoolid');
        if (!s) return;
        s.value = '$campusId';
        var evt = document.createEvent('HTMLEvents');
        evt.initEvent('change', true, true);
        s.dispatchEvent(evt);
        var count = 0;
        var timer = setInterval(function() {
            count++;
            var b = document.getElementById('building');
            if (b && b.options.length > 1) {
                clearInterval(timer);
                Android.onPageReady('elec_building');
            } else if (count > 15) {
                clearInterval(timer);
                Android.onStep('楼栋加载超时');
            } else {
                Android.onStep('正在加载楼栋… ' + count + 's');
            }
        }, 1000);
    })();
    """.trimIndent()

    private fun selectBuildingScript(buildingId: String) = """
    (function() {
        var b = document.getElementById('building');
        if (!b) return;
        b.value = '$buildingId';
        var evt = document.createEvent('HTMLEvents');
        evt.initEvent('change', true, true);
        b.dispatchEvent(evt);
        var count = 0;
        var timer = setInterval(function() {
            count++;
            var r = document.getElementById('room');
            if (r && r.options.length > 1) {
                clearInterval(timer);
                Android.onPageReady('elec_room');
            } else if (count > 15) {
                clearInterval(timer);
                Android.onStep('房间加载超时');
            } else {
                Android.onStep('正在加载房间… ' + count + 's');
            }
        }, 1000);
    })();
    """.trimIndent()

    private fun selectRoomOnlyScript(roomId: String) = """
    (function() {
        var r = document.getElementById('room');
        if (!r) return;
        r.value = '$roomId';
        var evt = document.createEvent('HTMLEvents');
        evt.initEvent('change', true, true);
        r.dispatchEvent(evt);
        var count = 0;
        var timer = setInterval(function() {
            count++;
            var zhye = document.querySelector('td#zhye');
            if (zhye && zhye.textContent.trim().length > 0) {
                clearInterval(timer);
                var raw = zhye.textContent.trim();
                var match = raw.match(/\d+\.?\d*/);
                var elec = match ? match[0] : raw;
                Android.onResult('', '', '', elec, '', '');
            } else if (count > 15) {
                clearInterval(timer);
                Android.onStep('电量结果加载超时');
            } else {
                Android.onStep('正在查询电量… ' + count + 's');
            }
        }, 1000);
    })();
    """.trimIndent()

    private fun readResultScript() = """
    (function() {
        var zhye = document.querySelector('td#zhye');
        if (zhye) return zhye.textContent.trim();
        return '';
    })();
    """.trimIndent()
}
