package ru.adonixis.yandexlivepictures.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

object UiExtensions {

    fun Path.drawSmoothLine(points: List<Offset>) {
        if (points.size > 1) {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = if (i > 0) points[i - 1] else points[i]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = if (i < points.size - 2) points[i + 2] else p2

                val controlPoint1X = p1.x + (p2.x - p0.x) / 6
                val controlPoint1Y = p1.y + (p2.y - p0.y) / 6
                val controlPoint2X = p2.x - (p3.x - p1.x) / 6
                val controlPoint2Y = p2.y - (p3.y - p1.y) / 6

                cubicTo(
                    controlPoint1X, controlPoint1Y,
                    controlPoint2X, controlPoint2Y,
                    p2.x, p2.y
                )
            }
        }
    }

    fun DrawScope.drawSquare(center: Offset, size: Float, color: Color, scale: Float = 1f, rotation: Float = 0f) {
        val scaledSize = size * scale
        rotate(rotation, center) {
            val path = Path().apply {
                moveTo(center.x - scaledSize/2, center.y - scaledSize/2)
                lineTo(center.x + scaledSize/2, center.y - scaledSize/2)
                lineTo(center.x + scaledSize/2, center.y + scaledSize/2)
                lineTo(center.x - scaledSize/2, center.y + scaledSize/2)
                close()
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    fun DrawScope.drawCircle(center: Offset, size: Float, color: Color, scale: Float = 1f, rotation: Float = 0f) {
        val scaledSize = size * scale
        rotate(rotation, center) {
            drawCircle(
                color = color,
                radius = scaledSize/2,
                center = center,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    fun DrawScope.drawTriangle(center: Offset, size: Float, color: Color, scale: Float = 1f, rotation: Float = 0f) {
        val scaledSize = size * scale
        rotate(rotation, center) {
            val path = Path().apply {
                moveTo(center.x, center.y - scaledSize/2)
                lineTo(center.x + scaledSize/2, center.y + scaledSize/2)
                lineTo(center.x - scaledSize/2, center.y + scaledSize/2)
                close()
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    fun DrawScope.drawArrow(center: Offset, size: Float, color: Color, scale: Float = 1f, rotation: Float = 0f) {
        val scaledSize = size * scale
        rotate(rotation, center) {
            val path = Path().apply {
                moveTo(center.x, center.y - scaledSize/2)
                lineTo(center.x - scaledSize/3, center.y - scaledSize/6)
                moveTo(center.x, center.y - scaledSize/2)
                lineTo(center.x + scaledSize/3, center.y - scaledSize/6)
                moveTo(center.x, center.y - scaledSize/2)
                lineTo(center.x, center.y + scaledSize/2)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    fun Offset.getDistanceTo(other: Offset): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
