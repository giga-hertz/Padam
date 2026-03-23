package com.nuvio.tv.core.util

import kotlin.math.max
import kotlin.math.min

object FuzzyMatcher {
    private const val DEFAULT_THRESHOLD = 0.75

    /**
     * Matches movie/show with local file using fuzzy matching
     * Considers: title, year, season, episode
     */
    fun matchContent(
        contentTitle: String,
        contentYear: Int?,
        contentSeason: Int?,
        contentEpisode: Int?,
        fileName: String,
        threshold: Double = DEFAULT_THRESHOLD
    ): Boolean {
        // Remove file extensions
        val cleanFileName = fileName.substringBeforeLast(".")

        // Extract potential metadata from filename
        val fileMetadata = extractMetadataFromFilename(cleanFileName)

        // Calculate title similarity
        val titleSimilarity = calculateSimilarity(
            contentTitle.lowercase(),
            fileMetadata.title.lowercase()
        )

        if (titleSimilarity < threshold) return false

        // Check year match (if available)
        if (contentYear != null && fileMetadata.year != null) {
            if (contentYear != fileMetadata.year) return false
        }

        // Check season/episode match (if available)
        if (contentSeason != null && contentEpisode != null) {
            if (fileMetadata.season != contentSeason) return false
            if (fileMetadata.episode != contentEpisode) return false
        }

        return true
    }

    private fun extractMetadataFromFilename(filename: String): FileMetadata {
        var title = filename
        var year: Int? = null
        var season: Int? = null
        var episode: Int? = null

        // Extract year pattern: (2020), [2020], or year at end
        val yearRegex = Regex("""(?:\(|\\[)?(19|20)\d{2}(?:\)|\\])?""")
        yearRegex.find(filename)?.let {
            year = it.value.replace(Regex("""[()\\[\\]]"""), "").toIntOrNull()
            title = filename.replace(it.value, "").trim()
        }

        // Extract season/episode: S01E05, 1x05, etc
        val seasonEpisodeRegex = Regex("""[Ss](\d+)[Ee](\d+)""")
        seasonEpisodeRegex.find(filename)?.let {
            val (seasonStr, episodeStr) = it.destructured
            season = seasonStr.toIntOrNull()
            episode = episodeStr.toIntOrNull()
            title = title.replace(it.value, "").trim()
        }

        return FileMetadata(title, year, season, episode)
    }

    private fun calculateSimilarity(str1: String, str2: String): Double {
        val distance = levenshteinDistance(str1, str2)
        val maxLength = max(str1.length, str2.length)
        return if (maxLength == 0) 1.0 else 1.0 - (distance.toDouble() / maxLength)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }

        return dp[s1.length][s2.length]
    }

    data class FileMetadata(
        val title: String,
        val year: Int?,
        val season: Int?,
        val episode: Int?
    )
}
