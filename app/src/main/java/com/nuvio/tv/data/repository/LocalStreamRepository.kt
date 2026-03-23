package com.nuvio.tv.data.repository

import com.nuvio.tv.domain.model.LocalStreamFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalStreamRepository @Inject constructor() {

    suspend fun browsePath(baseUrl: String, path: String): List<LocalStreamFile> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/$path")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PROPFIND"
            connection.setRequestProperty("Depth", "1")
            connection.setRequestProperty("Content-Type", "application/xml")

            if (connection.responseCode == 207 || connection.responseCode == HttpURLConnection.HTTP_OK) {
                parseWebDAVResponse(connection.inputStream.bufferedReader().readText(), baseUrl, path)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseWebDAVResponse(xmlResponse: String, baseUrl: String, basePath: String): List<LocalStreamFile> {
        val files = mutableListOf<LocalStreamFile>()
        val videoExtensions = listOf("mkv", "mp4", "avi", "mov", "flv", "wmv", "webm")

        // Simple XML parsing for WebDAV PROPFIND response
        val filePattern = Regex("""<d:href>([^<]+)</d:href>""")
        val sizePattern = Regex("""<d:getcontentlength>(\d+)</d:getcontentlength>""")

        filePattern.findAll(xmlResponse).forEach { match ->
            val href = match.groupValues[1].trim()
            val fileName = href.substringAfterLast("/").urlDecode()

            if (fileName.isNotEmpty() && fileName != "/" && !fileName.endsWith("/")) {
                val extension = fileName.substringAfterLast(".", "").lowercase()
                if (extension in videoExtensions) {
                    val fileSize = sizePattern.find(xmlResponse)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                    val resolution = extractResolution(fileName)
                    val format = extension.uppercase()

                    files.add(
                        LocalStreamFile(
                            id = href.hashCode().toString(),
                            name = fileName,
                            path = href,
                            mimeType = "video/$extension",
                            fileSize = fileSize,
                            resolution = resolution,
                            format = format,
                            webdavUrl = "$baseUrl$href"
                        )
                    )
                }
            }
        }

        return files
    }

    private fun extractResolution(fileName: String): String? {
        val resolutionPattern = Regex("""(480p|720p|1080p|2160p|4K)""", RegexOption.IGNORE_CASE)
        return resolutionPattern.find(fileName)?.value?.uppercase()
    }

    private fun String.urlDecode(): String {
        return java.net.URLDecoder.decode(this, "UTF-8")
    }
}
