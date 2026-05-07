package com.AbuAlaa.data.repo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private val prefs = context.getSharedPreferences("location_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_CITY = "city"
        private const val KEY_MANUAL = "is_manual"
    }

    /**
     * احفظ موقع اختاره المستخدم يدوياً (من الإعدادات).
     * هيتم استخدامه في كل مرة طلبنا الموقع.
     */
    fun saveManualLocation(lat: Double, lng: Double, city: String) {
        prefs.edit()
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LNG, lng.toFloat())
            .putString(KEY_CITY, city)
            .putBoolean(KEY_MANUAL, true)
            .apply()
    }

    /**
     * امسح الموقع المحفوظ يدوياً (للرجوع للتلقائي).
     */
    fun clearManualLocation() {
        prefs.edit()
            .remove(KEY_LAT)
            .remove(KEY_LNG)
            .remove(KEY_CITY)
            .putBoolean(KEY_MANUAL, false)
            .apply()
    }

    fun getSavedCity(): String? = prefs.getString(KEY_CITY, null)

    /**
     * هل في موقع محفوظ (سواء تلقائي أو يدوي)؟
     */
    fun hasCachedLocation(): Boolean {
        val lat = prefs.getFloat(KEY_LAT, 0f)
        val lng = prefs.getFloat(KEY_LNG, 0f)
        return lat != 0f && lng != 0f
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): android.location.Location? {
        // لو في موقع محفوظ (تلقائي أو يدوي) استخدمه مباشرة
        val cachedLat = prefs.getFloat(KEY_LAT, 0f)
        val cachedLng = prefs.getFloat(KEY_LNG, 0f)

        if (cachedLat != 0f && cachedLng != 0f) {
            val cached = android.location.Location("cache")
            cached.latitude = cachedLat.toDouble()
            cached.longitude = cachedLng.toDouble()
            return cached
        }

        // مفيش في الـ cache - اجيب من GPS وحفظ
        return fetchAndCache()
    }

    @SuppressLint("MissingPermission")
    suspend fun getFreshLocation(): android.location.Location? {
        return fetchAndCache()
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchAndCache(): android.location.Location? =
        suspendCancellableCoroutine { cont ->
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000L
            )
            .setMaxUpdates(1)
            .setWaitForAccurateLocation(true)
            .setMinUpdateDistanceMeters(0f)
            .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fused.removeLocationUpdates(this)
                    result.lastLocation?.let { save(it) }
                    cont.resume(result.lastLocation)
                }
            }
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
            cont.invokeOnCancellation { fused.removeLocationUpdates(callback) }
        }

    private fun save(loc: android.location.Location) {
        // لا تتجاوز الموقع اليدوي بموقع GPS تلقائي
        val isManual = prefs.getBoolean(KEY_MANUAL, false)
        if (!isManual) {
            prefs.edit()
                .putFloat(KEY_LAT, loc.latitude.toFloat())
                .putFloat(KEY_LNG, loc.longitude.toFloat())
                .apply()
        }
    }
}
