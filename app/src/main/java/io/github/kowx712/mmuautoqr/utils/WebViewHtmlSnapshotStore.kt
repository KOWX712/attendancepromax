package io.github.kowx712.mmuautoqr.utils

import org.json.JSONTokener
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

val SNAPSHOT_TIMESTAMP_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.US).withZone(ZoneId.systemDefault())

class WebViewHtmlSnapshotStore(
    private val snapshotDir: File,
    private val exportDir: File = File(snapshotDir.parentFile ?: snapshotDir, "webview-snapshot-exports"),
    private val timestampProvider: () -> String = {
        SNAPSHOT_TIMESTAMP_FORMATTER.format(Instant.now())
    }
) {

    fun saveSnapshot(url: String?, html: String): File {
        require(html.isNotBlank()) { "HTML snapshot must not be blank" }

        snapshotDir.mkdirs()

        val normalizedUrl = sanitizeFilenameSegment(url.orEmpty())
            .ifBlank { "page" }
        val snapshotFile = snapshotDir.resolve("${timestampProvider()}-$normalizedUrl.html")
        snapshotFile.writeText(html, Charsets.UTF_8)
        return snapshotFile
    }

    fun createExportZip(): File? {
        val snapshotFiles = snapshotDir
            .listFiles { file -> file.isFile && file.extension.equals("html", ignoreCase = true) }
            ?.sortedBy { it.name }
            .orEmpty()

        if (snapshotFiles.isEmpty()) {
            return null
        }

        exportDir.mkdirs()
        val exportZip = exportDir.resolve("${timestampProvider()}-rendered-html.zip")
        ZipOutputStream(exportZip.outputStream().buffered()).use { zipOutputStream ->
            snapshotFiles.forEach { snapshotFile ->
                zipOutputStream.putNextEntry(ZipEntry(snapshotFile.name))
                FileInputStream(snapshotFile).use { input ->
                    input.copyTo(zipOutputStream)
                }
                zipOutputStream.closeEntry()
            }
        }

        return exportZip
    }

    fun copyZipTo(outputStream: OutputStream, zipFile: File) {
        zipFile.inputStream().buffered().use { input ->
            input.copyTo(outputStream)
        }
    }

    fun deleteTempZip(zipFile: File) {
        if (zipFile.exists()) {
            zipFile.delete()
        }
    }
}

fun decodeEvaluateJavascriptResult(serializedResult: String?): String {
    if (serializedResult.isNullOrBlank() || serializedResult == "null") {
        return ""
    }

    val parsedValue = JSONTokener(serializedResult).nextValue()
    return (parsedValue as? String).orEmpty()
}

private fun sanitizeFilenameSegment(value: String): String {
    return value
        .lowercase(Locale.US)
        .replace(Regex("^https?://"), "")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
}
