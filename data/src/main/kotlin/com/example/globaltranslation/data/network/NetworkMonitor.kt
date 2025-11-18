package com.example.globaltranslation.data.network

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity changes.
 * Provides Flow of network state and WiFi availability.
 * Open for testing - allows fake implementations to override behavior.
 */
@Singleton
open class NetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Checks if ACCESS_NETWORK_STATE permission is granted.
     * For Android 6.0+, this permission is granted at install time, but we check for safety.
     */
    private fun hasNetworkStatePermission(): Boolean {
        // Since minSdk is 29, we're always on Android 6.0+
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Flow that emits network connectivity state.
     */
    open val networkState: Flow<NetworkState> = callbackFlow {
        // Check permission first
        if (!hasNetworkStatePermission()) {
            trySend(NetworkState.Disconnected)
            awaitClose { }
            return@callbackFlow
        }
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentNetworkState())
            }
            
            override fun onLost(network: Network) {
                trySend(getCurrentNetworkState())
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getCurrentNetworkState())
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Emit initial state
        trySend(getCurrentNetworkState())
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Gets the current network state.
     */
    open fun getCurrentNetworkState(): NetworkState {
        // Check permission first
        if (!hasNetworkStatePermission()) {
            return NetworkState.Disconnected
        }
        
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        return when {
            capabilities == null -> NetworkState.Disconnected
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.WiFi
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.Cellular
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkState.WiFi // Treat ethernet as WiFi
            else -> NetworkState.Connected // Some other connection type
        }
    }
    
    /**
     * Checks if device is connected to WiFi.
     */
    open fun isOnWiFi(): Boolean {
        return getCurrentNetworkState() == NetworkState.WiFi
    }
    
    /**
     * Checks if device has any network connection.
     */
    open fun isConnected(): Boolean {
        return getCurrentNetworkState() != NetworkState.Disconnected
    }
}

/**
 * Represents different network connectivity states.
 */
sealed class NetworkState {
    data object WiFi : NetworkState()
    data object Cellular : NetworkState()
    data object Connected : NetworkState() // Other connection type
    data object Disconnected : NetworkState()
    
    fun isConnected(): Boolean = this != Disconnected
    fun isWiFi(): Boolean = this == WiFi
    fun isCellular(): Boolean = this == Cellular
}

