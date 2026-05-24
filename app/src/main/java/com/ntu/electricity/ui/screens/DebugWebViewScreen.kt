package com.ntu.electricity.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ntu.electricity.EcoCheckApplication
import com.ntu.electricity.data.local.entity.QueryHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DebugWV"

private const val STEP_INIT = 0
private const val STEP_LOGIN_PAGE = 1
private const val STEP_AUTH_FILL = 2
private const val STEP_AUTH_SUBMIT = 3
private const val STEP_MAIN_PAGE = 4
private const val STEP_ELEC_CAMPUS = 5
private const val STEP_ELEC_BUILDING = 6
private const val STEP_ELEC_ROOM = 7
private const val STEP_DONE = 8

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DebugWebViewScreen(
    url: String,
    studentId: String,
    password: String,
    campusId: String,
    buildingId: String,
    roomId: String,
    onDismiss: () -> Unit
) {
    var stepLog by remember { mutableStateOf("正在连接...") }
    var currentStep by remember { mutableStateOf(STEP_INIT) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Holds the query result when received from JS
    var queryResultData by remember { mutableStateOf<QueryResultData?>(null) }
    val context = LocalContext.current
    val app = remember { context.applicationContext as EcoCheckApplication }
    val dao = remember { app.database.queryHistoryDao() }

    // When JS sends back the result, save it to history
    LaunchedEffect(queryResultData) {
        queryResultData?.let { data ->
            withContext(Dispatchers.IO) {
                dao.insert(
                    QueryHistoryEntity(
                        date = data.date,
                        time = data.time,
                        electricity = data.electricity,
                        campus = data.campus,
                        building = data.building,
                        room = data.room
                    )
                )
            }
        }
    }

    // Show snackbar when message is set — removed, status now shown in dialog

    // 🤖 Auto-advance: when page state changes, automatically trigger next action
    LaunchedEffect(currentStep) {
        val wv = webViewRef ?: return@LaunchedEffect
        // Small delay to ensure page/DOM is ready
        kotlinx.coroutines.delay(300)
        when (currentStep) {
            STEP_LOGIN_PAGE -> {
                stepLog = "🤖 自动点击统一身份认证…"
                wv.evaluateJavascript(clickSSOScript(), null)
            }
            STEP_AUTH_FILL -> {
                stepLog = "🤖 自动填写账号密码…"
                wv.evaluateJavascript(fillOnlyScript(studentId, password)) { result ->
                    val r = result?.removeSurrounding("\"") ?: ""
                    stepLog = "🤖 填写完成，自动提交登录"
                    currentStep = STEP_AUTH_SUBMIT
                }
            }
            STEP_AUTH_SUBMIT -> {
                stepLog = "🤖 自动提交登录…"
                wv.evaluateJavascript(submitLoginScript(), null)
            }
            STEP_MAIN_PAGE -> {
                stepLog = "🤖 自动点击学生宿舍电费…"
                wv.evaluateJavascript(clickElecScript(), null)
            }
            STEP_ELEC_CAMPUS -> {
                stepLog = "🤖 自动选校区…"
                wv.evaluateJavascript(selectCampusScript(campusId), null)
            }
            STEP_ELEC_BUILDING -> {
                stepLog = "🤖 自动选楼栋…"
                wv.evaluateJavascript(selectBuildingScript(buildingId), null)
            }
            STEP_ELEC_ROOM -> {
                stepLog = "🤖 自动选房间，等待电量结果…"
                wv.evaluateJavascript(selectRoomOnlyScript(roomId), null)
            }
            STEP_DONE -> {
                // All done, show completion
                queryResultData?.let {
                    stepLog = "✅ 查询完成！${it.campus} ${it.building} ${it.room}: ${it.electricity} 度"
                }
            }
            else -> { /* STEP_INIT: wait for first page load */ }
        }
    }

    // JavaScript interface: bridges JS calls (Android.onStep / Android.onPageReady) back to Compose state
    val jsInterface = remember {
        object {
            @JavascriptInterface
            fun onStep(message: String) {
                Handler(Looper.getMainLooper()).post {
                    stepLog = message
                }
            }

            @JavascriptInterface
            fun onPageReady(pageType: String) {
                Handler(Looper.getMainLooper()).post {
                    when (pageType) {
                        "elec_building" -> {
                            currentStep = STEP_ELEC_BUILDING
                            stepLog = "楼栋加载完成，点右下角按钮选楼栋 →"
                        }
                        "elec_room" -> {
                            currentStep = STEP_ELEC_ROOM
                            stepLog = "房间加载完成，点右下角按钮选房间 →"
                        }
                        else -> {
                            stepLog = "页面就绪: $pageType"
                        }
                    }
                }
            }

            @JavascriptInterface
            fun onResult(campusName: String, buildingName: String, roomName: String, electricity: String, date: String, time: String) {
                Handler(Looper.getMainLooper()).post {
                    currentStep = STEP_DONE
                    stepLog = "✅ 查询完成！$campusName $buildingName $roomName: $electricity 度"
                    queryResultData = QueryResultData(
                        campus = campusName,
                        building = buildingName,
                        room = roomName,
                        electricity = electricity,
                        date = date,
                        time = time
                    )
                }
            }
        }
    }

    // Simplified progress steps for the dialog
    val progressSteps = listOf("正在登录", "进入电费查询", "选择校区", "选择楼栋", "查询电量")
    fun progressState(index: Int): StepProgress {
        val doneAt = listOf(STEP_MAIN_PAGE, STEP_ELEC_CAMPUS, STEP_ELEC_BUILDING, STEP_ELEC_ROOM, STEP_DONE)
        return when {
            currentStep >= STEP_DONE -> StepProgress.DONE
            currentStep >= doneAt[index] -> StepProgress.DONE
            index == 0 || currentStep >= doneAt.getOrElse(index - 1) { STEP_INIT } -> StepProgress.LOADING
            else -> StepProgress.PENDING
        }
    }

    // Hidden WebView — does all the work, truly invisible
    AndroidView(
        modifier = Modifier
            .size(1.dp)
            .alpha(0f),
        factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    webViewRef = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString =
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

                    val cookieMgr = CookieManager.getInstance()
                    cookieMgr.setAcceptCookie(true)
                    cookieMgr.setAcceptThirdPartyCookies(this, true)

                    addJavascriptInterface(jsInterface, "Android")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d(TAG, "Page finished: $url")

                            view?.evaluateJavascript(detectPageScript()) { result ->
                                val pageType = result?.removeSurrounding("\"") ?: "unknown"
                                Log.d(TAG, "Detected: $pageType")
                                when (pageType) {
                                    "login_page" -> currentStep = STEP_LOGIN_PAGE
                                    "auth_page" -> currentStep = STEP_AUTH_FILL
                                    "main_page" -> currentStep = STEP_MAIN_PAGE
                                    "elec_campus" -> currentStep = STEP_ELEC_CAMPUS
                                    "elec_building" -> currentStep = STEP_ELEC_BUILDING
                                    "elec_room" -> currentStep = STEP_ELEC_ROOM
                                    "result_page" -> currentStep = STEP_DONE
                                }
                            }
                        }
                    }

                    loadUrl(url)
                }
            }
        )

        // Dialog card
        Card(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 12.dp,
                pressedElevation = 12.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (currentStep == STEP_DONE && queryResultData != null) {
                    // ── Result view ──
                    val data = queryResultData!!
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "查询完成",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${data.electricity} 度",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${data.campus} ${data.building} ${data.room}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${data.date}  ${data.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("确定")
                    }
                } else {
                    // ── Progress view ──
                    Text(
                        text = "NTU 电费查询",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    progressSteps.forEachIndexed { index, label ->
                        StepProgressRow(
                            label = label,
                            state = progressState(index)
                        )
                    }
                    // Current detail log
                    if (stepLog.isNotEmpty() && currentStep > STEP_INIT) {
                        Text(
                            text = stepLog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            }
        }
    }


private enum class StepProgress { PENDING, LOADING, DONE }

@Composable
private fun StepProgressRow(label: String, state: StepProgress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            StepProgress.LOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            StepProgress.DONE -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            StepProgress.PENDING -> {
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = when (state) {
                StepProgress.LOADING -> MaterialTheme.colorScheme.onSurface
                StepProgress.DONE -> MaterialTheme.colorScheme.onSurface
                StepProgress.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
        )
    }
}

/** Detect which page we're on */
private fun detectPageScript(): String {
    return """
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
}

/** Step 1: Click SSO link */
private fun clickSSOScript(): String {
    return """
    (function() {
        var img = document.querySelector('img[src="/images/sso_img.png"]');
        if (img) {
            var link = img.closest('a');
            if (link) { link.click(); return 'clicked'; }
        }
        return 'not_found';
    })();
    """.trimIndent()
}

/** Step 2: Fill credentials only */
private fun fillOnlyScript(studentId: String, password: String): String {
    return """
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
}

/** Step 3: Submit login form */
private fun submitLoginScript(): String {
    return """
    (function() {
        var btn = document.getElementById('login_submit')
            || document.querySelector('input[type="submit"]')
            || document.querySelector('button[type="submit"]');
        if (btn) { btn.click(); return 'clicked: ' + btn.tagName + '#' + btn.id; }
        return 'no_submit_found';
    })();
    """.trimIndent()
}

/** Step 3: Click electricity link */
private fun clickElecScript(): String {
    return """
    (function() {
        var link = document.querySelector('a[onclick*="elecdetails5118E017"]');
        if (link) { link.click(); return 'clicked'; }
        var all = document.querySelectorAll('a');
        for (var i = 0; i < all.length; i++) {
            if (all[i].textContent.indexOf('电费') >= 0) { all[i].click(); return 'clicked_text'; }
        }
        return 'not_found';
    })();
    """.trimIndent()
}

/** Select campus — dispatch change event to trigger onchange handler naturally */
private fun selectCampusScript(campusId: String): String {
    return """
    (function() {
        try {
            var s = document.getElementById('schoolid');
            if (!s) { Android.onStep('ERROR: 找不到schoolid, 页面上所有select: ' + Array.from(document.querySelectorAll('select')).map(function(el){return el.id||el.name||'unnamed'}).join(',')); return; }
            Android.onStep('找到schoolid, 当前值=' + s.value + ', 选项数=' + s.options.length + ', 设为campusId=$campusId');
            s.value = '$campusId';
            // Fire native change event (more reliable than calling changeSchool directly)
            var evt = document.createEvent('HTMLEvents');
            evt.initEvent('change', true, true);
            s.dispatchEvent(evt);
            Android.onStep('已触发change事件, 等待楼栋加载...');
        } catch(e) {
            Android.onStep('JS异常: ' + e.message);
            return;
        }

        // Poll building options
        var count = 0;
        var timer = setInterval(function() {
            count++;
            try {
                var b = document.getElementById('building');
                if (!b) { clearInterval(timer); Android.onStep('ERROR: 找不到building'); return; }
                var n = b.options.length;
                Android.onStep('轮询#' + count + ': building.options.length=' + n);
                if (n > 1) {
                    clearInterval(timer);
                    Android.onStep('楼栋加载完成(' + n + '项), 点按钮选楼栋');
                    Android.onPageReady('elec_building');
                } else if (count > 15) {
                    clearInterval(timer);
                    Android.onStep('楼栋超时(15s), options=' + n + ', building.innerHTML前200字符=' + b.innerHTML.substring(0,200));
                }
            } catch(e) {
                clearInterval(timer);
                Android.onStep('轮询异常: ' + e.message);
            }
        }, 1000);
    })();
    """.trimIndent()
}

/** Select building — dispatch change event to trigger onchange handler naturally */
private fun selectBuildingScript(buildingId: String): String {
    return """
    (function() {
        try {
            var b = document.getElementById('building');
            if (!b) { Android.onStep('ERROR: 找不到building'); return; }
            Android.onStep('找到building, 当前值=' + b.value + ', 选项数=' + b.options.length + ', 设为buildingId=$buildingId');
            b.value = '$buildingId';
            // Fire native change event
            var evt = document.createEvent('HTMLEvents');
            evt.initEvent('change', true, true);
            b.dispatchEvent(evt);
            Android.onStep('已触发change事件, 等待房间加载...');
        } catch(e) {
            Android.onStep('JS异常: ' + e.message);
            return;
        }

        var count = 0;
        var timer = setInterval(function() {
            count++;
            try {
                var r = document.getElementById('room');
                if (!r) { clearInterval(timer); Android.onStep('ERROR: 找不到room'); return; }
                var n = r.options.length;
                Android.onStep('轮询#' + count + ': room.options.length=' + n);
                if (n > 1) {
                    clearInterval(timer);
                    Android.onStep('房间加载完成(' + n + '项), 点按钮选房间');
                    Android.onPageReady('elec_room');
                } else if (count > 15) {
                    clearInterval(timer);
                    Android.onStep('房间超时(15s), options=' + n + ', room.innerHTML前200字符=' + r.innerHTML.substring(0,200));
                }
            } catch(e) {
                clearInterval(timer);
                Android.onStep('轮询异常: ' + e.message);
            }
        }, 1000);
    })();
    """.trimIndent()
}

/** Select room and trigger query, then poll for electricity result */
private fun selectRoomOnlyScript(roomId: String): String {
    return """
    (function() {
        try {
            var r = document.getElementById('room');
            if (!r) { Android.onStep('ERROR: 找不到room'); return 'no room'; }
            Android.onStep('找到room, 当前值=' + r.value + ', 选项数=' + r.options.length + ', 设为roomId=$roomId');
            r.value = '$roomId';
            var evt = document.createEvent('HTMLEvents');
            evt.initEvent('change', true, true);
            r.dispatchEvent(evt);
            Android.onStep('已触发change事件, 等待电量结果...');
        } catch(e) {
            Android.onStep('JS异常: ' + e.message);
            return 'error';
        }

        // Poll for electricity result (td#zhye gets populated via AJAX, no page navigation)
        var count = 0;
        var timer = setInterval(function() {
            count++;
            try {
                var zhye = document.querySelector('td#zhye');
                if (zhye && zhye.textContent.trim().length > 0) {
                    clearInterval(timer);
                    var raw = zhye.textContent.trim();
                    var match = raw.match(/\d+\.?\d*/);
                    var elec = match ? match[0] : raw;
                    
                    var campusSel = document.getElementById('schoolid');
                    var buildingSel = document.getElementById('building');
                    var roomSel = document.getElementById('room');
                    var campusName = (campusSel && campusSel.selectedIndex >= 0) ? campusSel.options[campusSel.selectedIndex].text : '?';
                    var buildingName = (buildingSel && buildingSel.selectedIndex >= 0) ? buildingSel.options[buildingSel.selectedIndex].text : '?';
                    var roomName = (roomSel && roomSel.selectedIndex >= 0) ? roomSel.options[roomSel.selectedIndex].text : '?';
                    
                    var now = new Date();
                    var date = now.getFullYear() + '-' + 
                        String(now.getMonth()+1).padStart(2,'0') + '-' + 
                        String(now.getDate()).padStart(2,'0');
                    var time = String(now.getHours()).padStart(2,'0') + ':' + 
                        String(now.getMinutes()).padStart(2,'0') + ':' + 
                        String(now.getSeconds()).padStart(2,'0');
                    
                    Android.onResult(campusName, buildingName, roomName, elec, date, time);
                } else if (count > 15) {
                    clearInterval(timer);
                    Android.onStep('电量结果加载超时(15s), 请点击按钮重试');
                } else {
                    Android.onStep('等电量结果... ' + count + 's');
                }
            } catch(e) {
                clearInterval(timer);
                Android.onStep('结果轮询异常: ' + e.message);
            }
        }, 1000);

        return 'ok';
    })();
    """.trimIndent()
}

private data class QueryResultData(
    val campus: String,
    val building: String,
    val room: String,
    val electricity: String,
    val date: String,
    val time: String
)
