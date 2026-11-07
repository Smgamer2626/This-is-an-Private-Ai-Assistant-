package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.YuriBorderBright
import com.example.ui.theme.YuriCyanGlow
import com.example.ui.theme.YuriCyanPrimary
import com.example.ui.theme.YuriStatusListening
import com.example.ui.theme.YuriStatusOffline
import com.example.ui.theme.YuriStatusOnline
import com.example.ui.theme.YuriStatusThinking
import com.example.ui.viewmodel.YuriStatus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun YuriAiCore(
    status: YuriStatus,
    audioRms: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "YuriCoreTransition")

    // Bobbing vertical motion
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bobbing"
    )

    // Gentle breathing scale
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (status) {
                    YuriStatus.THINKING -> 800
                    YuriStatus.LISTENING -> 600
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathing"
    )

    // Outer ring clockwise rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (status) {
                    YuriStatus.THINKING -> 4000
                    YuriStatus.LISTENING -> 6000
                    else -> 16000
                },
                easing = LinearEasing
            )
        ),
        label = "OuterRingRotation"
    )

    // Inner ring counter-clockwise rotation
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (status) {
                    YuriStatus.THINKING -> 3000
                    YuriStatus.LISTENING -> 4500
                    else -> 12000
                },
                easing = LinearEasing
            )
        ),
        label = "InnerRingRotation"
    )

    // Reactive scale when listening based on microphone RMS level
    val reactiveScale = if (status == YuriStatus.LISTENING) {
        breathingScale + (audioRms * 0.035f)
    } else {
        breathingScale
    }

    val (statusColor, statusText) = when (status) {
        YuriStatus.ONLINE -> Pair(YuriStatusOnline, "ONLINE")
        YuriStatus.LISTENING -> Pair(YuriStatusListening, "LISTENING")
        YuriStatus.THINKING -> Pair(YuriStatusThinking, "THINKING...")
        YuriStatus.SPEAKING -> Pair(YuriCyanGlow, "SPEAKING")
        YuriStatus.OFFLINE -> Pair(YuriStatusOffline, "OFFLINE")
    }

    Column(
        modifier = modifier
            .offset(y = bobbingOffset.dp)
            .clickable(onClick = onClick)
            .testTag("yuri_ai_core"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Holographic HUD Canvas Rings
            Canvas(modifier = Modifier.size(240.dp)) {
                val center = this.center
                val radiusOuter = size.minDimension / 2.05f
                val radiusInner = size.minDimension / 2.35f

                // Outer ambient cyan glow circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.22f),
                            statusColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 2f
                    )
                )

                // Rotating Outer Segmented Arcs
                val arcCount = 4
                val sweep = 60f
                for (i in 0 until arcCount) {
                    val startAngle = outerRotation + (i * 90f) + 15f
                    drawArc(
                        color = statusColor.copy(alpha = 0.65f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Rotating Inner Fine Arc with Ticks
                for (j in 0 until 3) {
                    val startAngle = innerRotation + (j * 120f)
                    drawArc(
                        color = YuriCyanPrimary.copy(alpha = 0.4f),
                        startAngle = startAngle,
                        sweepAngle = 40f,
                        useCenter = false,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Sci-fi tick marks along the ring
                for (k in 0 until 16) {
                    val angle = (k * 22.5f) * (PI / 180.0)
                    val r1 = radiusOuter - 2.dp.toPx()
                    val r2 = radiusOuter + 3.dp.toPx()
                    val p1X = center.x + (r1 * cos(angle)).toFloat()
                    val p1Y = center.y + (r1 * sin(angle)).toFloat()
                    val p2X = center.x + (r2 * cos(angle)).toFloat()
                    val p2Y = center.y + (r2 * sin(angle)).toFloat()

                    drawLine(
                        color = statusColor.copy(alpha = if (k % 4 == 0) 0.8f else 0.35f),
                        start = androidx.compose.ui.geometry.Offset(p1X, p1Y),
                        end = androidx.compose.ui.geometry.Offset(p2X, p2Y),
                        strokeWidth = if (k % 4 == 0) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            // Central Bobbing Glowing AI Bubble Asset
            Box(
                modifier = Modifier
                    .size(174.dp)
                    .scale(reactiveScale)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.yuri_core_bubble),
                    contentDescription = "YURI AI Core Bubble",
                    modifier = Modifier.size(174.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status Badge Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF081426))
                .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
        }
    }
}
