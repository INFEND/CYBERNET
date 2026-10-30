package com.v2ray.ang.ui.checkupdate

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val OTA_MANIFEST =
    "https://github.com/ANTIFERMENT/CYBERNET/releases/latest/download/update.json"

private data class CybernetUpdate(
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val sha256: String,
    val notes: String
)

class CheckUpdateActivity : BaseComponentActivity() {
    private var checking by mutableStateOf(false)
    private var downloading by mutableStateOf(false)
    private var downloadPercent by mutableIntStateOf(0)
    private var status by mutableStateOf("Проверка релизов CYBERNET")
    private var latest by mutableStateOf<CybernetUpdate?>(null)
    private var downloadedApk: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) check()
    }

    private fun installedVersionCode(): Long {
        @Suppress("DEPRECATION")
        val info = packageManager.getPackageInfo(packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    }

    private fun openConnection(raw: String): HttpURLConnection {
        val connection = (URL(raw).openConnection() as HttpURLConnection)
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "CYBERNET Android OTA")
        return connection
    }

    private fun fetchManifest(): CybernetUpdate {
        val c = openConnection(OTA_MANIFEST)
        try {
            if (c.responseCode != HttpURLConnection.HTTP_OK)
                error("Манифест недоступен (HTTP ${c.responseCode}). Проверьте GitHub Releases.")
            val data = c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            require(data.length <= 65536) { "Слишком большой манифест обновления" }
            val json = JSONObject(data)
            val url = json.getString("apkUrl")
            require(url.startsWith("https://github.com/ANTIFERMENT/CYBERNET/releases/download/")) {
                "Недоверенный адрес APK"
            }
            val sha = json.getString("sha256").lowercase()
            require(sha.matches(Regex("[0-9a-f]{64}"))) { "Неверная контрольная сумма" }
            val code = json.getLong("versionCode")
            require(code > 0) { "Неверный номер версии" }
            return CybernetUpdate(
                versionName = json.getString("versionName"),
                versionCode = code,
                apkUrl = url,
                sha256 = sha,
                notes = json.optString("notes", "Исправления и улучшения.")
            )
        } finally {
            c.disconnect()
        }
    }

    private fun check() {
        if (checking || downloading) return
        checking = true
        status = "Проверяем обновления…"
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { fetchManifest() }
                latest = if (result.versionCode > installedVersionCode()) result else null
                status = if (latest != null)
                    "Доступна версия ${result.versionName}"
                    else "Установлена актуальная версия CYBERNET"
            } catch (e: Exception) {
                latest = null
                status = "Не удалось проверить: ${e.message ?: "Ошибка сети"}"
            } finally {
                checking = false
            }
        }
    }

    private fun downloadUpdate() {
        val release = latest ?: return
        if (checking || downloading) return
        if (downloadedApk?.exists() == true) {
            installApk(downloadedApk!!)
            return
        }
        downloading = true
        downloadPercent = 0
        status = "Скачиваем проверенный APK…"
        lifecycleScope.launch {
            try {
                val apk = withContext(Dispatchers.IO) {
                    val file = File(cacheDir, "CYBERNET-update.apk")
                    val temp = File(cacheDir, "CYBERNET-update.part")
                    temp.delete()
                    val connection = openConnection(release.apkUrl)
                    try {
                        check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                            "Ошибка загрузки HTTP ${connection.responseCode}"
                        }
                        val expectedSize = connection.contentLengthLong
                        require(expectedSize <= 250L * 1024 * 1024) { "APK превышает допустимый размер" }
                        val digest = MessageDigest.getInstance("SHA-256")
                        var total = 0L
                        connection.inputStream.use { input ->
                            temp.outputStream().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val n = input.read(buffer)
                                    if (n < 0) break
                                    total += n
                                    require(total <= 250L * 1024 * 1024) { "APK слишком большой" }
                                    digest.update(buffer, 0, n)
                                    output.write(buffer, 0, n)
                                    if (expectedSize > 0) {
                                        val current = ((total * 100) / expectedSize).toInt().coerceIn(0, 99)
                                        if (current != downloadPercent) withContext(Dispatchers.Main) { downloadPercent = current }
                                    }
                                }
                            }
                        }
                        val actual = digest.digest().joinToString("") { "%02x".format(it) }
                        require(actual.equals(release.sha256, true)) {
                            "SHA-256 не совпадает. Установка отменена."
                        }
                        file.delete()
                        check(temp.renameTo(file)) { "Не удалось подготовить APK" }
                        file
                    } finally {
                        connection.disconnect()
                        if (temp.exists()) temp.delete()
                    }
                }
                downloadPercent = 100
                downloadedApk = apk
                status = "Файл проверен. Подтвердите установку Android."
                installApk(apk)
            } catch (e: Exception) {
                status = "Скачивание не выполнено: ${e.message ?: "Ошибка"}"
            } finally {
                downloading = false
            }
        }
    }

    private fun installApk(apk: File) {
        try {
            if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                status = "Разрешите CYBERNET установку приложений и нажмите кнопку ещё раз."
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName"))
                startActivity(intent)
                return
            }
            val uri = FileProvider.getUriForFile(this, "$packageName.cache", apk)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            status = "Установщик недоступен: ${e.message ?: "Ошибка"}"
        }
    }

    @Composable
    override fun ScreenContent() {
        Scaffold(
            topBar = {
                AppTopBar(title = "Обновление CYBERNET", onBackClick = { finish() },
                    isLoading = checking || downloading)
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("CYBERNET", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text("Установлено: ${BuildConfig.VERSION_NAME}")
                Text(status, style = MaterialTheme.typography.bodyLarge)
                if (downloading) {
                    LinearProgressIndicator(
                        progress = { downloadPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Загружено: $downloadPercent%")
                }
                val release = latest
                if (release != null) {
                    Text("Новая версия: ${release.versionName}",
                        style = MaterialTheme.typography.titleMedium)
                    Text(release.notes)
                    Button(onClick = { downloadUpdate() },
                        enabled = !checking && !downloading,
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (downloadedApk != null) "Установить обновление"
                             else "Скачать и установить")
                    }
                }
                Spacer(Modifier.height(5.dp))
                OutlinedButton(onClick = { check() },
                    enabled = !checking && !downloading,
                    modifier = Modifier.fillMaxWidth()) { Text("Проверить повторно") }
                Text("Загрузка только из официальных релизов CYBERNET. Контроль SHA-256; установку подтверждает Android.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
