package com.nuvio.tv.domain.model

data class LocalStreamFile(
    val id: String,
    val name: String,
    val path: String,
    val mimeType: String,
    val fileSize: Long,
    val resolution: String? = null,
    val format: String? = null,
    val bitrate: String? = null,
    val webdavUrl: String,
    val isSelected: Boolean = false
)
