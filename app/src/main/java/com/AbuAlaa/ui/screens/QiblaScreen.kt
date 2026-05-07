package com.AbuAlaa.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.AbuAlaa.R
import com.google.android.gms.location.LocationServices
import kotlin.math.*

// Kaaba coordinates
private const val KAABA_LAT = 21.4225
private const val KAABA_LNG = 39.8262

@Composable
fun QiblaScreen(
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var qiblaDirection by remember { mutableFloatStateOf(0f) }
    var hasPermission by remember {
    mutableStateOf(
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    )
  }   
    var sensorAccuracy by remember { mutableStateOf("جيدة") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Get location
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        userLocation = it
                        qiblaDirection = calculateQiblaDirection(it.latitude, it.longitude)
                    }
                }
            } catch (e: SecurityException) {
                // Handle permission error
            }
        }
    }

    // Compass sensor
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                    }
                }

                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (azimuth < 0) azimuth += 360
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                sensorAccuracy = when (accuracy) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "ممتازة"
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "جيدة"
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "ضعيفة - قم بمعايرة البوصلة"
                    else -> "غير محددة"
                }
            }
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val animatedRotation by animateFloatAsState(
        targetValue = -azimuth,
        animationSpec = tween(durationMillis = 100),
        label = "compass_rotation"
    )

    val animatedQiblaRotation by animateFloatAsState(
        targetValue = qiblaDirection - azimuth,
        animationSpec = tween(durationMillis = 100),
        label = "qibla_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                "اتجاه القبلة",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Compass
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                // Compass rose
                Canvas(
                    modifier = Modifier
                        .size(280.dp)
                        .rotate(animatedRotation)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 - 20f

                    // Outer circle
                    drawCircle(
                        color = Color(0xFF10171A),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4f)
                    )

                    // Inner circle
                    drawCircle(
                        color = Color(0xFF10171A).copy(alpha = 0.3f),
                        radius = radius - 30f,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Direction markers
                    val directions = listOf("ش", "شش", "ش", "جش", "ج", "جغ", "غ", "شغ")
                    val mainDirections = setOf(0, 2, 4, 6)

                    for (i in 0 until 8) {
                        val angle = i * 45f - 90f
                        val rad = Math.toRadians(angle.toDouble())
                        val isMain = i in mainDirections
                        val lineLength = if (isMain) 25f else 15f
                        val startRadius = radius - lineLength

                        drawLine(
                            color = if (isMain) Color(0xFF10171A) else Color(0xFF10171A).copy(alpha = 0.5f),
                            start = Offset(
                                center.x + (startRadius * cos(rad)).toFloat(),
                                center.y + (startRadius * sin(rad)).toFloat()
                            ),
                            end = Offset(
                                center.x + (radius * cos(rad)).toFloat(),
                                center.y + (radius * sin(rad)).toFloat()
                            ),
                            strokeWidth = if (isMain) 4f else 2f
                        )
                    }

                    // North arrow (red)
                    val northArrowPath = Path().apply {
                        moveTo(center.x, center.y - radius + 40f)
                        lineTo(center.x - 12f, center.y - radius + 70f)
                        lineTo(center.x + 12f, center.y - radius + 70f)
                        close()
                    }
                    drawPath(northArrowPath, Color(0xFFE53935), style = Fill)
                }

                // Qibla arrow (always points to Kaaba)
                if (userLocation != null) {
                    Canvas(
                        modifier = Modifier
                            .size(280.dp)
                            .rotate(animatedQiblaRotation)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)

                        // Qibla arrow (golden)
                        val qiblaArrowPath = Path().apply {
                            moveTo(center.x, 50f)
                            lineTo(center.x - 20f, 100f)
                            lineTo(center.x, 80f)
                            lineTo(center.x + 20f, 100f)
                            close()
                        }
                        drawPath(qiblaArrowPath, Color(0xFF10171A), style = Fill)

                        // Visual indicator for Kaaba direction
                        drawCircle(
                            color = Color(0xFF10171A),
                            radius = 6f,
                            center = Offset(center.x, 40f) // Just above the arrow tip
                        )
                    }
                }

                // Center Image (Kaaba) - Replaces the old Box/Icon
                Image(
                    painter = painterResource(id = R.drawable.alkaba),
                    contentDescription = "الكعبة",
                    modifier = Modifier.size(60.dp) // Adjusted size for better fit
                )
            }

            // Qibla degree info
            if (userLocation != null) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 16.dp, horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${qiblaDirection.toInt()}°",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "اتجاه القبلة من موقعك",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                ElevatedCard(
                  onClick = { launcher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                 )) },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "يرجى تفعيل الموقع لتحديد اتجاه القبلة",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Sensor accuracy
            Text(
                "دقة الحساس: $sensorAccuracy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Calibration hint
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "💡 لمعايرة البوصلة، حرّك هاتفك على شكل رقم 8",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun calculateQiblaDirection(lat: Double, lng: Double): Float {
    val latRad = Math.toRadians(lat)
    val lngRad = Math.toRadians(lng)
    val kaabaLatRad = Math.toRadians(KAABA_LAT)
    val kaabaLngRad = Math.toRadians(KAABA_LNG)

    val dLng = kaabaLngRad - lngRad

    val x = sin(dLng) * cos(kaabaLatRad)
    val y = cos(latRad) * sin(kaabaLatRad) - sin(latRad) * cos(kaabaLatRad) * cos(dLng)

    var bearing = Math.toDegrees(atan2(x, y))
    if (bearing < 0) bearing += 360

    return bearing.toFloat()
}
