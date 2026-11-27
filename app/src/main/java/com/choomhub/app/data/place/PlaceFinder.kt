package com.choomhub.app.data.place

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** A spot on the map, and its name if we know one. */
data class FoundPlace(val lat: Double, val lon: Double, val name: String?)

/**
 * Turns place names into map pins and back, with the phone's own Geocoder (no API key, no cost),
 * and finds where you are with the phone's location (only after you allow it).
 * Searches are kept to Delhi.
 */
@Singleton
class PlaceFinder @Inject constructor(@ApplicationContext private val context: Context) {

    val canSearch: Boolean get() = Geocoder.isPresent()

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Where [name] is in Delhi, or null if the phone can't find it. */
    @Suppress("DEPRECATION")
    suspend fun search(name: String): FoundPlace? = withContext(Dispatchers.IO) {
        if (!canSearch || name.isBlank()) return@withContext null
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val hit = geocoder.getFromLocationName("$name, Delhi", 1, SOUTH, WEST, NORTH, EAST)?.firstOrNull()
                ?: geocoder.getFromLocationName("$name, Delhi", 1)?.firstOrNull()
            hit?.let { FoundPlace(it.latitude, it.longitude, name) }
        }.getOrNull()
    }

    /** A short name for a spot ("Hauz Khas Village"), or null. */
    @Suppress("DEPRECATION")
    suspend fun nameOf(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        if (!canSearch) return@withContext null
        runCatching {
            val a = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull() ?: return@runCatching null
            listOfNotNull(a.featureName?.takeUnless { it.any(Char::isDigit) }, a.subLocality, a.locality).firstOrNull()
        }.getOrNull()
    }

    /** Where this phone is now (or was very recently), with its name; null without permission or a fix. */
    @SuppressLint("MissingPermission")
    suspend fun here(): FoundPlace? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val fix = current(manager) ?: listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        return fix?.let { FoundPlace(it.latitude, it.longitude, nameOf(it.latitude, it.longitude)) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun current(manager: LocationManager): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) } ?: return null
        return withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { cont ->
                val cancel = android.os.CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                runCatching {
                    manager.getCurrentLocation(provider, cancel, context.mainExecutor) { cont.resume(it) }
                }.onFailure { cont.resume(null) }
            }
        }
    }

    private companion object {
        // Delhi (with a margin), to keep name searches local.
        const val SOUTH = 28.35
        const val WEST = 76.75
        const val NORTH = 28.95
        const val EAST = 77.45
    }
}
