package com.cufica.dailyayah.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.cufica.dailyayah.widget.PrayerWidgetPreferences
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DeviceCityResolver {
    private const val PreferencesName = "device_city"
    private const val LastCheckKey = "last_check_millis"
    private val WidgetCheckInterval = TimeUnit.MINUTES.toMillis(15)

    suspend fun resolve(context: Context): String? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null
        val locationManager = context.getSystemService(LocationManager::class.java) ?: return@withContext null
        val location = sequenceOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: return@withContext null
        runCatching {
            @Suppress("DEPRECATION")
            val address = Geocoder(context, Locale("tr", "TR"))
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
            address?.let { it.subAdminArea ?: it.locality ?: it.adminArea }
        }.getOrNull()
    }

    // Background location may be denied by the OS; the saved city is kept in that case.
    suspend fun widgetCity(context: Context): String {
        val saved = PrayerWidgetPreferences.selectedCity(context)
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - preferences.getLong(LastCheckKey, 0L) < WidgetCheckInterval) return saved
        preferences.edit().putLong(LastCheckKey, now).apply()

        val city = resolve(context) ?: return saved
        if (city != saved) PrayerWidgetPreferences.setSelectedCity(context, city)
        return city
    }

    private fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
