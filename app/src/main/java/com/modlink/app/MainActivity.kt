package com.modlink.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private lateinit var urlInput: EditText
    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var resolveButton: Button
    private var resolvedUrl: String = ""

    private val bg = Color.rgb(10, 10, 12)
    private val panel = Color.rgb(24, 24, 28)
    private val red = Color.rgb(255, 48, 65)
    private val white = Color.WHITE
    private val muted = Color.rgb(170, 170, 178)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(34), dp(22), dp(24))
        }
        scroll.addView(column)
        setContentView(scroll)

        val brand = TextView(this).apply {
            text = "MODLINK"
            textSize = 30f
            setTextColor(red)
            gravity = Gravity.CENTER
            letterSpacing = 0.16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        column.addView(brand, matchWrap())
        val subtitle = TextView(this).apply {
            text = "SAFE LINK RESOLVER"
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
            letterSpacing = 0.18f
        }
        column.addView(subtitle, matchWrap())
        addSpace(column, 28)

        val label = TextView(this).apply {
            text = "MASUKKAN LINK"
            textSize = 12f
            setTextColor(white)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        column.addView(label)
        urlInput = EditText(this).apply {
            hint = "https://contoh.com/download"
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 14f
            singleLine = true
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(panel)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        val inputParams = LinearLayout.LayoutParams(-1, dp(54))
        inputParams.topMargin = dp(9)
        column.addView(urlInput, inputParams)

        resolveButton = Button(this).apply {
            text = "RESOLVE LINK"
            setTextColor(white)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(red))
            setOnClickListener { resolveLink() }
        }
        val buttonParams = LinearLayout.LayoutParams(-1, dp(52))
        buttonParams.topMargin = dp(14)
        column.addView(resolveButton, buttonParams)

        addSpace(column, 22)
        status = TextView(this).apply {
            text = "STATUS: SIAP"
            textSize = 12f
            setTextColor(red)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        column.addView(status)
        result = TextView(this).apply {
            text = "Hasil resolusi akan muncul di sini."
            textSize = 13f
            setTextColor(white)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(panel)
        }
        val resultParams = LinearLayout.LayoutParams(-1, -2)
        resultParams.topMargin = dp(10)
        column.addView(result, resultParams)

        val copyButton = Button(this).apply {
            text = "COPY HASIL"
            setTextColor(white)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(65, 65, 72)))
            setOnClickListener {
                if (resolvedUrl.isNotBlank()) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Resolved URL", resolvedUrl))
                    Toast.makeText(this@MainActivity, "Link disalin", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(this@MainActivity, "Belum ada hasil", Toast.LENGTH_SHORT).show()
            }
        }
        val copyParams = LinearLayout.LayoutParams(-1, dp(50))
        copyParams.topMargin = dp(10)
        column.addView(copyButton, copyParams)

        addSpace(column, 24)
        val note = TextView(this).apply {
            text = "INFO KEAMANAN\\nMODLINK hanya mengikuti redirect HTTP/HTTPS biasa. Aplikasi tidak membobol CAPTCHA, login, paywall, atau proteksi akses. Link APK/mod dari sumber tidak dikenal tetap berisiko malware. Periksa izin aplikasi dan sumber unduhan sebelum memasang."
            textSize = 12f
            setTextColor(muted)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(panel)
        }
        column.addView(note)
    }

    private fun resolveLink() {
        val raw = urlInput.text.toString().trim()
        val uri = try { URI(raw) } catch (_: Exception) { null }
        if (uri == null || (uri.scheme != "https" && uri.scheme != "http") || uri.host.isNullOrBlank()) {
            status.text = "STATUS: LINK TIDAK VALID"
            result.text = "Masukkan URL lengkap yang diawali http:// atau https://."
            return
        }
        resolveButton.isEnabled = false
        status.text = "STATUS: MEMERIKSA..."
        result.text = "Mengikuti redirect standar. Tunggu sebentar..."
        worker.execute {
            try {
                val request = Request.Builder().url(raw).head().header("User-Agent", "MODLINK-Android/1.0").build()
                client.newCall(request).execute().use { response ->
                    val finalUrl = response.request.url.toString()
                    val type = response.header("Content-Type") ?: "Tidak diketahui"
                    val length = response.header("Content-Length") ?: "Tidak diketahui"
                    val isFile = !type.contains("text/html", true) &&
                        (type.contains("application", true) || type.contains("octet-stream", true))
                    runOnUiThread {
                        resolvedUrl = finalUrl
                        status.text = if (response.isSuccessful) "STATUS: SELESAI" else "STATUS: HTTP ${response.code}"
                        result.text = "URL AKHIR:\\n$finalUrl\\n\\nHTTP: ${response.code}\\nTIPE: $type\\nUKURAN: $length\\n\\n" +
                            if (isFile) "Respons tampak seperti file. Tetap verifikasi sumbernya sebelum mengunduh."
                            else "URL akhir ditemukan, tetapi server belum mengonfirmasi bahwa ini file unduhan. Bisa jadi masih halaman web."
                        resolveButton.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "STATUS: GAGAL"
                    result.text = "Tidak bisa memeriksa link: ${e.localizedMessage ?: "koneksi bermasalah"}\\n\\nBeberapa situs menolak pemeriksaan otomatis atau membutuhkan JavaScript/login."
                    resolveButton.isEnabled = true
                }
            }
        }
    }

    private fun addSpace(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }
}
