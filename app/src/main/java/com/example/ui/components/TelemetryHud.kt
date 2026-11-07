package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.ServerHealthResult
import com.example.data.system.BatteryTelemetry
import com.example.data.system.SystemStatusReport
import com.example.ui.theme.YuriBackgroundCard
import com.example.ui.theme.YuriBorderBright
import com.example.ui.theme.YuriBorderCyan
import com.example.ui.theme.YuriCyanGlow
import com.example.ui.theme.YuriCyanPrimary
import com.example.ui.theme.YuriStatusOffline
import com.example.ui.theme.YuriStatusOnline
import com.example.ui.theme.YuriStatusWarning
import com.example.ui.theme.YuriTextPrimary
import com.example.ui.theme.YuriTextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopHudClockBar(
    serverHealth: ServerHealthResult?,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App title & time
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "YURI",
                    color = YuriCyanPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI",
                    color = YuriTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "${timeFormat.format(Date(currentTime))} • ${dateFormat.format(Date(currentTime)).uppercase(Locale.ROOT)}",
                color = YuriTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        // Local Server Connection Pill
        val isConnected = serverHealth?.isSuccess == true
        val statusPillColor = if (isConnected) YuriStatusOnline else YuriStatusOffline
        val statusPillText = if (isConnected) {
            "TERMUX: ONLINE"
        } else {
            "TERMUX: DISCONNECTED"
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(YuriBackgroundCard)
                .border(1.dp, statusPillColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable(onClick = onStatusClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("termux_status_pill"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusPillColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusPillText,
                color = statusPillColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun HardwareTelemetryDashboard(
    battery: BatteryTelemetry,
    report: SystemStatusReport,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(YuriBackgroundCard.copy(alpha = 0.85f))
            .border(1.dp, YuriBorderCyan, RoundedCornerShape(16.dp))
            .clickable(onClick = onExpandClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Battery Metric
        val batteryColor = when {
            battery.percentage > 40 -> YuriStatusOnline
            battery.percentage > 15 -> YuriStatusWarning
            else -> YuriStatusOffline
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (battery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                    tint = batteryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${battery.percentage}%",
                    color = YuriTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = if (battery.isCharging) "CHARGING" else "${battery.temperatureCelsius}°C",
                color = YuriTextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(YuriBorderCyan)
        )

        // RAM Metric
        val ramUsedGb = report.ram.usedBytes / (1024f * 1024f * 1024f)
        val ramTotalGb = report.ram.totalBytes / (1024f * 1024f * 1024f)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "RAM",
                    tint = YuriCyanGlow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${report.ram.percentageUsed}%",
                    color = YuriTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "RAM %.1f/%.1fGB".format(Locale.US, ramUsedGb, ramTotalGb),
                color = YuriTextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(YuriBorderCyan)
        )

        // CPU Metric
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "CPU",
                    tint = YuriCyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${report.cpu.estimatedUsagePercent}%",
                    color = YuriTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "${report.cpu.coreCount} CORES",
                color = YuriTextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
