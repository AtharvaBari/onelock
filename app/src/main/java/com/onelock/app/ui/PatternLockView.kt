package com.onelock.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onelock.app.ui.theme.OneUIAccent
import com.onelock.app.ui.theme.OneUIDanger
import com.onelock.app.ui.theme.OneUISuccess
import kotlin.math.sqrt

/**
 * 3x3 Interactive Samsung One UI Pattern Lock component.
 * Nodes are indexed 0..8 from top-left to bottom-right.
 */
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 290.dp,
    enabled: Boolean = true,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    hideTrail: Boolean = false,
    onPatternCompleted: (pattern: String) -> Unit,
    onPatternStarted: () -> Unit = {}
) {
    var selectedNodes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current

    val nodeHitRadiusPx = with(density) { 34.dp.toPx() }
    val normalDotRadiusPx = with(density) { 5.dp.toPx() }
    val selectedDotRadiusPx = with(density) { 8.dp.toPx() }
    val outerRingRadiusPx = with(density) { 26.dp.toPx() }
    val strokeWidthPx = with(density) { 4.5.dp.toPx() }

    val activeColor = when {
        isError -> OneUIDanger
        isSuccess -> OneUISuccess
        else -> OneUIAccent
    }
    val inactiveDotColor = Color(0xFF636366)
    val inactiveRingColor = Color(0xFF1E1E22)

    Box(modifier = modifier.size(sizeDp)) {
        Canvas(
            modifier = Modifier
                .size(sizeDp)
                .pointerInput(enabled, isError) {
                    if (!enabled) return@pointerInput

                    detectDragGestures(
                        onDragStart = { offset ->
                            onPatternStarted()
                            val nodePositions = calculateNodePositions(size.width.toFloat(), size.height.toFloat())
                            val startIndex = findClosestNode(offset, nodePositions, nodeHitRadiusPx)
                            if (startIndex != null) {
                                selectedNodes = listOf(startIndex)
                                currentTouchPos = offset
                            } else {
                                selectedNodes = emptyList()
                                currentTouchPos = null
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (selectedNodes.isNotEmpty()) {
                                currentTouchPos = change.position
                                val nodePositions = calculateNodePositions(size.width.toFloat(), size.height.toFloat())
                                val hoveredIndex = findClosestNode(change.position, nodePositions, nodeHitRadiusPx)
                                if (hoveredIndex != null && !selectedNodes.contains(hoveredIndex)) {
                                    // Check if there is an unselected intermediate node (e.g. 0 -> 2 jumps over 1)
                                    val last = selectedNodes.last()
                                    val middle = getIntermediateNode(last, hoveredIndex)
                                    val updated = selectedNodes.toMutableList()
                                    if (middle != null && !updated.contains(middle)) {
                                        updated.add(middle)
                                    }
                                    updated.add(hoveredIndex)
                                    selectedNodes = updated
                                }
                            }
                        },
                        onDragEnd = {
                            currentTouchPos = null
                            if (selectedNodes.size >= 2) {
                                val patternString = selectedNodes.joinToString("")
                                onPatternCompleted(patternString)
                            } else {
                                selectedNodes = emptyList()
                            }
                        },
                        onDragCancel = {
                            currentTouchPos = null
                            selectedNodes = emptyList()
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val nodePositions = calculateNodePositions(width, height)

            // Draw connecting lines between selected nodes
            if (!hideTrail && selectedNodes.isNotEmpty()) {
                val path = Path()
                val firstPos = nodePositions[selectedNodes.first()]
                path.moveTo(firstPos.x, firstPos.y)

                for (i in 1 until selectedNodes.size) {
                    val pos = nodePositions[selectedNodes[i]]
                    path.lineTo(pos.x, pos.y)
                }

                // Connect to current moving touch position
                val touch = currentTouchPos
                if (touch != null) {
                    path.lineTo(touch.x, touch.y)
                }

                drawPath(
                    path = path,
                    color = activeColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Draw all 9 nodes (outer rings and dots)
            for (i in 0 until 9) {
                val pos = nodePositions[i]
                val isSelected = selectedNodes.contains(i)

                if (isSelected) {
                    // Outer glow / translucent ring
                    drawCircle(
                        color = activeColor.copy(alpha = 0.18f),
                        radius = outerRingRadiusPx,
                        center = pos
                    )
                    // Outer stroke ring
                    drawCircle(
                        color = activeColor,
                        radius = outerRingRadiusPx,
                        center = pos,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Center active dot
                    drawCircle(
                        color = activeColor,
                        radius = selectedDotRadiusPx,
                        center = pos
                    )
                } else {
                    // Subtle inactive background circle
                    drawCircle(
                        color = inactiveRingColor,
                        radius = outerRingRadiusPx * 0.75f,
                        center = pos
                    )
                    // Inactive center dot
                    drawCircle(
                        color = inactiveDotColor,
                        radius = normalDotRadiusPx,
                        center = pos
                    )
                }
            }
        }
    }
}

private fun calculateNodePositions(width: Float, height: Float): List<Offset> {
    val paddingX = width * 0.18f
    val paddingY = height * 0.18f
    val stepX = (width - 2 * paddingX) / 2f
    val stepY = (height - 2 * paddingY) / 2f

    val positions = mutableListOf<Offset>()
    for (row in 0..2) {
        for (col in 0..2) {
            val x = paddingX + col * stepX
            val y = paddingY + row * stepY
            positions.add(Offset(x, y))
        }
    }
    return positions
}

private fun findClosestNode(touch: Offset, nodePositions: List<Offset>, hitRadius: Float): Int? {
    for (i in nodePositions.indices) {
        val pos = nodePositions[i]
        val dx = touch.x - pos.x
        val dy = touch.y - pos.y
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (dist <= hitRadius) {
            return i
        }
    }
    return null
}

/**
 * Returns the intermediate node if connecting from -> to skips directly over a center point.
 */
private fun getIntermediateNode(from: Int, to: Int): Int? {
    val min = minOf(from, to)
    val max = maxOf(from, to)

    return when {
        // Horizontal rows
        min == 0 && max == 2 -> 1
        min == 3 && max == 5 -> 4
        min == 6 && max == 8 -> 7
        // Vertical columns
        min == 0 && max == 6 -> 3
        min == 1 && max == 7 -> 4
        min == 2 && max == 8 -> 5
        // Diagonals
        min == 0 && max == 8 -> 4
        min == 2 && max == 6 -> 4
        else -> null
    }
}
