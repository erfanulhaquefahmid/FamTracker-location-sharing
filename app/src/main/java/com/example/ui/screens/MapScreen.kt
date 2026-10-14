package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.FamilyMember
import com.example.model.FreshnessLevel
import com.example.model.LocationEvent
import com.example.model.LocationFreshness
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DelayedAmber
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.StaleRed
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapScreen(
    currentUserId: String,
    members: List<FamilyMember>,
    selfLatestLocation: LocationEvent?
) {
    val context = LocalContext.current
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

    // Configure OSM User Agent and lifecycle
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        Configuration.getInstance().userAgentValue = context.packageName
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapViewInstance?.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapViewInstance?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewInstance?.onPause()
            mapViewInstance?.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_map_view"),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)

                    // Default center: current user's location or world default
                    val initialLat = selfLatestLocation?.latitude ?: 37.7749
                    val initialLng = selfLatestLocation?.longitude ?: -122.4194
                    controller.setCenter(GeoPoint(initialLat, initialLng))
                    mapViewInstance = this
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                // Add markers for all members who have a location
                for (member in members) {
                    val lat = if (member.uid == currentUserId && selfLatestLocation != null) {
                        selfLatestLocation.latitude
                    } else member.latestLatitude

                    val lng = if (member.uid == currentUserId && selfLatestLocation != null) {
                        selfLatestLocation.longitude
                    } else member.latestLongitude

                    if (lat != null && lng != null) {
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(lat, lng)
                            title = member.displayName
                            snippet = if (member.uid == currentUserId) "You" else "Family Member"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                selectedMember = member
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }
                mapView.invalidate()
            }
        )

        // Center on Me Button
        FloatingActionButton(
            onClick = {
                val lat = selfLatestLocation?.latitude
                val lng = selfLatestLocation?.longitude
                if (lat != null && lng != null && mapViewInstance != null) {
                    mapViewInstance?.controller?.animateTo(GeoPoint(lat, lng))
                    mapViewInstance?.controller?.setZoom(16.0)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .testTag("center_on_me_fab"),
            containerColor = CyanAccent,
            contentColor = Color(0xFF00363D)
        ) {
            Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Center on me")
        }

        // Bottom Detail Sheet for Selected Member
        selectedMember?.let { member ->
            val isSelf = member.uid == currentUserId
            val lat = if (isSelf && selfLatestLocation != null) selfLatestLocation.latitude else member.latestLatitude
            val lng = if (isSelf && selfLatestLocation != null) selfLatestLocation.longitude else member.latestLongitude
            val acc = if (isSelf && selfLatestLocation != null) selfLatestLocation.accuracyMeters else member.latestAccuracyMeters
            val capturedAt = if (isSelf && selfLatestLocation != null) selfLatestLocation.capturedAt else member.latestCapturedAt

            val freshness = LocationFreshness.calculate(capturedAt)
            val freshnessColor = when (freshness.level) {
                FreshnessLevel.FRESH -> FreshGreen
                FreshnessLevel.DELAYED -> DelayedAmber
                FreshnessLevel.STALE -> StaleRed
                FreshnessLevel.UNAVAILABLE -> Color(0xFF64748B)
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isSelf) CyanAccent else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.displayName + if (isSelf) " (You)" else "",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (lat != null && lng != null) {
                                Text(
                                    text = "${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}" +
                                            if (acc != null) " (±${acc.toInt()}m)" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = freshnessColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = freshness.label,
                                color = freshnessColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (capturedAt != null && capturedAt > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Measured at: ${SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault()).format(Date(capturedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
