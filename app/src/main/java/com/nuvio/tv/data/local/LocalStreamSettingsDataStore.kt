package com.nuvio.tv.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalStreamSettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val LOCAL_SERVER_IP = preferencesKey<String>("local_server_ip")
        val LOCAL_SERVER_PORT = preferencesKey<Int>("local_server_port")
        val IS_LOCAL_STREAMING_ENABLED = preferencesKey<Boolean>("is_local_streaming_enabled")
    }

    val localServerIp: Flow<String?> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LOCAL_SERVER_IP] }

    val localServerPort: Flow<Int?> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LOCAL_SERVER_PORT] }

    val isLocalStreamingEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.IS_LOCAL_STREAMING_ENABLED] ?: false }

    suspend fun updateLocalServerIp(ip: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCAL_SERVER_IP] = ip
        }
    }

    suspend fun updateLocalServerPort(port: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCAL_SERVER_PORT] = port
        }
    }

    suspend fun setLocalStreamingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_LOCAL_STREAMING_ENABLED] = enabled
        }
    }
}
