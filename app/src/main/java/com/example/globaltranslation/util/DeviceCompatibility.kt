package com.example.globaltranslation.util

import android.os.Build
import android.util.Log

/**
 * Utility class for monitoring device compatibility, particularly for 16KB page size support.
 * This helps with debugging and monitoring on newer ARM64 devices.
 */
object DeviceCompatibility {
    private const val TAG = "DeviceCompat"
    
    /**
     * Logs information about the device's page size and architecture.
     * This is useful for debugging 16KB page size compatibility issues.
     */
    fun logPageSizeInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val pageSize = android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
                Log.i(TAG, "System page size: $pageSize bytes (${pageSize / 1024}KB)")
                
                // Log architecture information (using SUPPORTED_ABIS instead of deprecated CPU_ABI)
                val primaryAbi = if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0] else "unknown"
                Log.i(TAG, "Device architecture: $primaryAbi")
                Log.i(TAG, "Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
                
                // Check if this is a 16KB page size device
                if (pageSize == 16384L) {
                    Log.i(TAG, "✅ Device supports 16KB page size - compatible with future requirements")
                } else {
                    Log.i(TAG, "ℹ️ Device uses ${pageSize / 1024}KB page size - standard configuration")
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Could not determine page size", e)
            }
        } else {
            Log.i(TAG, "Page size detection requires Android 12+ (current: ${Build.VERSION.SDK_INT})")
        }
    }
    
    /**
     * Checks if the device architecture supports 16KB page size.
     * @return true if the device architecture supports 16KB pages
     */
    fun supports16KBPageSize(): Boolean {
        return Build.SUPPORTED_ABIS.contains("arm64-v8a") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    
    /**
     * Gets a human-readable description of the device's page size compatibility.
     * @return String description of page size status
     */
    fun getPageSizeStatus(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val pageSize = android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
                when {
                    pageSize == 16384L -> "16KB page size (Future-ready)"
                    pageSize == 4096L -> "4KB page size (Standard)"
                    else -> "${pageSize / 1024}KB page size (Custom)"
                }
            } catch (e: Exception) {
                "Unknown page size"
            }
        } else {
            "Page size detection requires Android 12+"
        }
    }
}
