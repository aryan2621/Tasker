package com.tasker.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkConnectivityObserver(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connectionState = MutableStateFlow(getCurrentConnectivityState())
    val connectionState: StateFlow<ConnectionState> = _connectionState

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun observe(): Flow<ConnectionState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            // Network became available
            override fun onAvailable(network: Network) {
                val state = getCurrentConnectivityState()
                _connectionState.value = state
                trySend(state)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val state = getCurrentConnectivityState()
                _connectionState.value = state
                trySend(state)
            }

            override fun onLost(network: Network) {
                val state = ConnectionState.Unavailable
                _connectionState.value = state
                trySend(state)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        }

        trySend(getCurrentConnectivityState())
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    fun isConnected(): Boolean {
        return getCurrentConnectivityState() != ConnectionState.Unavailable
    }

    private fun getCurrentConnectivityState(): ConnectionState {
        val network = connectivityManager.activeNetwork ?: return ConnectionState.Unavailable
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionState.Unavailable

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionState.Available(ConnectionType.Wifi)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionState.Available(ConnectionType.Cellular)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionState.Available(ConnectionType.Ethernet)
            else -> ConnectionState.Unavailable
        }
    }
}

sealed class ConnectionState {
    data class Available(val type: ConnectionType) : ConnectionState()
    data object Unavailable : ConnectionState()
}

enum class ConnectionType {
    Wifi,
    Cellular,
    Ethernet
}