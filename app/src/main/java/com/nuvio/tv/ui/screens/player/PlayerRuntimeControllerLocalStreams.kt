package com.nuvio.tv.ui.screens.player

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.util.FuzzyMatcher
import com.nuvio.tv.domain.model.LocalStreamFile

internal fun PlayerRuntimeController.loadLocalStreams() {
    val type: String = contentType ?: return
    val title: String = this.title.ifBlank { return }
    val year: Int? = releaseYear?.toIntOrNull()
    val season: Int? = currentSeason
    val episode: Int? = currentEpisode

    localStreamsJob?.cancel()
    localStreamsJob = scope.launch {
        _uiState.update {
            it.copy(
                isLoadingLocalStreams = true,
                localStreamsError = null,
                localAllStreams = emptyList()
            )
        }

        try {
            val settings = localStreamSettingsDataStore.isLocalStreamingEnabled.first()
            if (!settings) {
                _uiState.update { it.copy(isLoadingLocalStreams = false) }
                return@launch
            }

            val ip = localStreamSettingsDataStore.localServerIp.first()
            val port = localStreamSettingsDataStore.localServerPort.first()

            if (ip.isNullOrBlank() || port == null) {
                _uiState.update {
                    it.copy(
                        isLoadingLocalStreams = false,
                        localStreamsError = "Local server settings not configured"
                    )
                }
                return@launch
            }

            val webdavUrl = "http://$ip:$port"
            val files = localStreamRepository.browsePath(webdavUrl, "")

            val matchedFiles = files.filter { file ->
                FuzzyMatcher.matchContent(
                    contentTitle = title,
                    contentYear = year,
                    contentSeason = season,
                    contentEpisode = episode,
                    fileName = file.name,
                    threshold = 0.7
                )
            }.map { file ->
                file.copy(webdavUrl = "$webdavUrl/${file.path}")
            }

            _uiState.update {
                it.copy(
                    isLoadingLocalStreams = false,
                    localAllStreams = matchedFiles,
                    localFilteredStreams = matchedFiles,
                    localStreamsError = null
                )
            }

            updateLocalStreamChips(matchedFiles)

        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingLocalStreams = false,
                    localStreamsError = e.message ?: "Failed to load local streams"
                )
            }
        }
    }
}

internal fun PlayerRuntimeController.switchToLocalStream(file: LocalStreamFile) {
    val url = file.webdavUrl
    if (url.isBlank()) {
        _uiState.update { it.copy(localStreamsError = "Invalid file URL") }
        return
    }

    nextEpisodeAutoPlayJob?.cancel()
    nextEpisodeAutoPlayJob = null

    flushPlaybackSnapshotForSwitchOrExit()

    resetLoadingOverlayForNewStream()
    releasePlayer(flushPlaybackState = false)

    applySelectedStreamState(
        stream = null,
        url = url,
        headers = emptyMap()
    )

    _uiState.update {
        it.copy(
            isBuffering = true,
            error = null,
            currentStreamName = file.name,
            currentStreamUrl = url,
            showSourcesPanel = false,
            isLoadingSourceStreams = false,
            sourceStreamsError = null
        )
    }

    showStreamSourceIndicator(null, "Local")
    resetNextEpisodeCardState(clearEpisode = false)

    preparePlaybackBeforeStart(
        url = url,
        headers = emptyMap(),
        loadSavedProgress = true
    )
}

private fun PlayerRuntimeController.updateLocalStreamChips(files: List<LocalStreamFile>) {
    val uniqueFormats = files.mapNotNull { it.format }.distinct()
    _uiState.update {
        it.copy(
            sourceChips = it.sourceChips + uniqueFormats.map { format ->
                SourceChipItem(format, SourceChipStatus.SUCCESS)
            }
        )
    }
}

internal fun PlayerRuntimeController.showLocalStreamsPanel() {
    _uiState.update {
        it.copy(
            showLocalStreamsPanel = true,
            showControls = true,
            showAudioOverlay = false,
            showSubtitleOverlay = false
        )
    }
    loadLocalStreams()
}

internal fun PlayerRuntimeController.dismissLocalStreamsPanel() {
    _uiState.update {
        it.copy(
            showLocalStreamsPanel = false,
            isLoadingLocalStreams = false
        )
    }
    scheduleHideControls()
}
