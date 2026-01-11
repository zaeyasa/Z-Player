package com.example.zplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zplayer.data.model.AudioFile
import androidx.compose.ui.graphics.drawscope.rotate // Ensure this is imported

@Composable
fun CassetteTape(
    isPlaying: Boolean,
    currentSong: AudioFile?,
    modifier: Modifier = Modifier
) {
    // Animation for spool rotation
    val infiniteTransition = rememberInfiniteTransition(label = "tape")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF333333)) // Cassette plastic body color
            .border(2.dp, Color(0xFF555555), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Label Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f) // Top part is the label
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE0E0E0)) // Paper label color
                .padding(8.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "A SIDE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Song Title on Cassette Label
                Text(
                    text = currentSong?.title ?: "Z-PLAYER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    ),
                    maxLines = 1
                )
                 Text(
                    text = currentSong?.artist ?: "Retro Mix", 
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    ),
                    maxLines = 1
                )
            }
            
            // Central Window
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.Center)
                    .offset(y = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Spool
                Spool(rotation = currentRotation)
                
                Spacer(modifier = Modifier.width(40.dp))
                
                // Right Spool
                Spool(rotation = currentRotation)
            }
        }
        
        // Screws
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screwRadius = 4.dp.toPx()
            val screwColor = Color(0xFF888888)
            drawCircle(screwColor, screwRadius, center = Offset(16.dp.toPx(), 16.dp.toPx()))
            drawCircle(screwColor, screwRadius, center = Offset(size.width - 16.dp.toPx(), 16.dp.toPx()))
            drawCircle(screwColor, screwRadius, center = Offset(16.dp.toPx(), size.height - 16.dp.toPx()))
            drawCircle(screwColor, screwRadius, center = Offset(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()))
            
            // Bottom trapezoid shape detail (magnetic tape area)
             val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.2f, size.height)
                lineTo(size.width * 0.25f, size.height * 0.85f)
                lineTo(size.width * 0.75f, size.height * 0.85f)
                lineTo(size.width * 0.8f, size.height)
                close()
            }
            drawPath(path, Color(0xFF222222))
        }
    }
}



@Composable
fun Spool(rotation: Float) {
    Canvas(
        modifier = Modifier
            .size(40.dp)
            .rotate(rotation) // This rotates the entire Canvas
    ) {
        // Outer rim
        drawCircle(
            color = Color.White,
            style = Stroke(width = 2.dp.toPx())
        )

        // Teeth
        for (i in 0 until 6) {
            val angle = i * 60f
            // Use the rotate function provided by DrawScope
            // It takes a block where the rotation is applied to drawing operations
            this.rotate(degrees = angle) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(center.x - 2.dp.toPx(), center.y - 20.dp.toPx()),
                    size = Size(4.dp.toPx(), 10.dp.toPx())
                )
            }
        }
    }
}


