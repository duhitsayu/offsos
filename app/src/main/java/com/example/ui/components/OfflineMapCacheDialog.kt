package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.map.OfflineCacheState
import java.util.Locale

@Composable
fun OfflineMapCacheDialog(
    state: OfflineCacheState,
    userLocation: Pair<Double, Double>,
    onDownloadCache: (lat: Double, lon: Double) -> Unit,
    onCancelDownload: () -> Unit,
    onClearCache: () -> Unit,
    onToggleOfflineSimulation: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val activeLat = if (userLocation.first != 0.0) userLocation.first else state.centerLat
    val activeLon = if (userLocation.second != 0.0) userLocation.second else state.centerLon

    val sizeMb = String.format(Locale.US, "%.2f", state.sizeBytes / (1024.0 * 1024.0))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(Color(0xFF000000), RoundedCornerShape(4.dp))
                .border(1.5.dp, Color(0xFFFFFFFF), RoundedCornerShape(4.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OffSosLogo(size = 32.dp, pulsing = state.isDownloading)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OFFLINE MAP CACHE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFFFFFFFF)
                        )
                        Text(
                            text = "LOCAL GEOGRAPHIC AREA STORAGE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sector & Coordinates Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A), RoundedCornerShape(2.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(2.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color(0xFFFFFFFF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TARGET SECTOR COORDINATES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFFFFFFFF)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.5f", activeLat)}° N, ${String.format(Locale.US, "%.5f", activeLon)}° E",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFFFFFFFF)
                        )
                        Text(
                            text = "Coverage: 3.5 km Radius • Zoom Levels 13 to 16",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = Color(0xFF888888)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cache Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cached Tiles
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF0A0A0A), RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(2.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "TILES CACHED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                color = Color(0xFF888888)
                            )
                            Text(
                                text = "${state.tileCount} Tiles",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = if (state.tileCount > 0) Color(0xFFFFFFFF) else Color(0xFF777777)
                            )
                        }
                    }

                    // Disk Footprint
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF0A0A0A), RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(2.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "STORAGE SIZE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                color = Color(0xFF888888)
                            )
                            Text(
                                text = "$sizeMb MB",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar if Downloading
                if (state.isDownloading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D0D0D), RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(2.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "DOWNLOADING LOCAL CACHE...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFFFFFFFF)
                                )
                                Text(
                                    text = "${state.downloadedTiles}/${state.totalTilesToDownload}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { state.downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = Color(0xFFFFFFFF),
                                trackColor = Color(0xFF333333),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tile: ${state.currentTileKey}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                color = Color(0xFFAAAAAA)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Airplane / Offline Simulation Toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A), RoundedCornerShape(2.dp))
                        .border(1.dp, if (state.isOfflineSimulated) Color(0xFFFFFFFF) else Color(0xFF333333), RoundedCornerShape(2.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Flight,
                                contentDescription = null,
                                tint = if (state.isOfflineSimulated) Color(0xFFFFFFFF) else Color(0xFF777777),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "SIMULATE OFFLINE MODE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFFFFFFFF)
                                )
                                Text(
                                    text = if (state.isOfflineSimulated) "Active: Relying 100% on local cache" else "Test map with zero network requests",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    ),
                                    color = Color(0xFF888888)
                                )
                            }
                        }

                        Switch(
                            checked = state.isOfflineSimulated,
                            onCheckedChange = { onToggleOfflineSimulation(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF000000),
                                checkedTrackColor = Color(0xFFFFFFFF),
                                uncheckedThumbColor = Color(0xFF888888),
                                uncheckedTrackColor = Color(0xFF222222)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Download, Clear, Close
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isDownloading) {
                        OutlinedButton(
                            onClick = onCancelDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = "CANCEL DOWNLOAD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    } else {
                        Button(
                            onClick = { onDownloadCache(activeLat, activeLon) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("btn_download_offline_cache"),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFFFFF),
                                contentColor = Color(0xFF000000)
                            )
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.tileCount > 0) "REFRESH LOCAL AREA CACHE" else "DOWNLOAD LOCAL AREA CACHE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.tileCount > 0 && !state.isDownloading) {
                            OutlinedButton(
                                onClick = onClearCache,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("btn_clear_offline_cache"),
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFFFFF))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PURGE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = "CLOSE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }
            }
        }
    }
}
