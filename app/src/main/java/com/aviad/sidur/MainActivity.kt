package com.aviad.sidur

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val startUrl = "https://appassets.androidplatform.net/assets/index.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.setBackgroundColor(0xFF12161C.toInt())
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest) = loader.shouldInterceptRequest(r.url)
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val u = r.url
                if (u.host == "appassets.androidplatform.net") return false
                return try { startActivity(Intent(Intent.ACTION_VIEW, u)); true } catch (e: Exception) { false }
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, cb: GeolocationPermissions.Callback) {
                cb.invoke(origin, true, false)
            }
        }
        tts = TextToSpeech(this) { st -> if (st == TextToSpeech.SUCCESS) { tts?.language = Locale("he", "IL"); ttsReady = true } }
        web.addJavascriptInterface(Bridge(), "Android")
        askPermissions()
        if (savedInstanceState == null) web.loadUrl(startUrl) else web.restoreState(savedInstanceState)
    }

    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); web.saveState(outState) }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }

    private fun askPermissions() {
        val need = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE)
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) ActivityCompat.requestPermissions(this, need.toTypedArray(), 1)
    }

    private fun has(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    inner class Bridge {
        @JavascriptInterface fun openUrl(url: String) { runOnUiThread {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { toast("לא נמצאה אפליקציה לפתיחה") }
        } }

        @JavascriptInterface fun dial(number: String) { runOnUiThread {
            val uri = Uri.parse("tel:" + Uri.encode(number))
            try {
                if (has(Manifest.permission.CALL_PHONE)) startActivity(Intent(Intent.ACTION_CALL, uri))
                else startActivity(Intent(Intent.ACTION_DIAL, uri))
            } catch (e: Exception) { startActivity(Intent(Intent.ACTION_DIAL, uri)) }
        } }

        @JavascriptInterface fun sendSms(number: String, msg: String) { runOnUiThread {
            if (!has(Manifest.permission.SEND_SMS)) { askPermissions(); toast("אין הרשאת SMS – אשר ונסה שוב"); return@runOnUiThread }
            try {
                val sm = getSystemService(SmsManager::class.java)
                val parts = sm.divideMessage(msg)
                sm.sendMultipartTextMessage(number, null, parts, null, null)
            } catch (e: Exception) { toast("שליחת SMS נכשלה: ${e.message}") }
        } }

        @JavascriptInterface fun share(text: String) { runOnUiThread {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "שתף סיכום"))
        } }

        @JavascriptInterface fun saveCsv(name: String, csv: String) { runOnUiThread {
            try {
                val dir = File(cacheDir, "export").apply { mkdirs() }
                val f = File(dir, name); f.writeText(csv)
                val uri = FileProvider.getUriForFile(this@MainActivity, "com.aviad.sidur.files", f)
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "שמור / שתף קובץ"))
            } catch (e: Exception) { toast("ייצוא נכשל: ${e.message}") }
        } }

        @JavascriptInterface fun speak(text: String) { runOnUiThread {
            if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "safe")
        } }

        @JavascriptInterface fun toast(s: String) { runOnUiThread { Toast.makeText(this@MainActivity, s, Toast.LENGTH_SHORT).show() } }
    }
}
