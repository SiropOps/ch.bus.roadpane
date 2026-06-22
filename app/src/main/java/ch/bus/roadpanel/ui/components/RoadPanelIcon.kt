package ch.bus.roadpanel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

enum class RoadPanelIconKind {
    Dashboard,
    Map,
    Power,
    Sensors,
    Settings,
    Refresh,
    Layers,
    Locate,
    Battery,
    Gps,
    Speed,
    Altitude,
    Solar,
    Connection,
    SensorCooler,
    SensorHead,
    SensorRoof,
    SensorOutside,
    SensorHeater,
}

@Composable
fun RoadPanelIcon(
    kind: RoadPanelIconKind,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.075f
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val w = size.width
        val h = size.height
        val s = size.minDimension
        val cx = w / 2f
        val cy = h / 2f

        when (kind) {
            RoadPanelIconKind.Dashboard -> {
                drawRoundRect(color, Offset(w * .18f, h * .18f), Size(w * .26f, h * .26f), CornerRadius(s * .06f), style = stroke)
                drawRoundRect(color, Offset(w * .56f, h * .18f), Size(w * .26f, h * .42f), CornerRadius(s * .06f), style = stroke)
                drawRoundRect(color, Offset(w * .18f, h * .56f), Size(w * .26f, h * .26f), CornerRadius(s * .06f), style = stroke)
                drawRoundRect(color, Offset(w * .56f, h * .72f), Size(w * .26f, h * .10f), CornerRadius(s * .05f), style = stroke)
            }
            RoadPanelIconKind.Map, RoadPanelIconKind.Gps -> {
                drawCircle(color, s * .22f, Offset(cx, h * .37f), style = stroke)
                val pin = Path().apply {
                    moveTo(cx, h * .83f)
                    cubicTo(w * .33f, h * .62f, w * .24f, h * .48f, w * .24f, h * .34f)
                    cubicTo(w * .24f, h * .16f, w * .38f, h * .10f, cx, h * .10f)
                    cubicTo(w * .62f, h * .10f, w * .76f, h * .16f, w * .76f, h * .34f)
                    cubicTo(w * .76f, h * .48f, w * .67f, h * .62f, cx, h * .83f)
                }
                drawPath(pin, color, style = stroke)
            }
            RoadPanelIconKind.Power, RoadPanelIconKind.Battery -> {
                drawRoundRect(color, Offset(w * .14f, h * .32f), Size(w * .66f, h * .36f), CornerRadius(s * .08f), style = stroke)
                drawRoundRect(color, Offset(w * .82f, h * .43f), Size(w * .07f, h * .14f), CornerRadius(s * .03f), style = stroke)
                drawLine(color, Offset(w * .28f, h * .50f), Offset(w * .60f, h * .50f), strokeWidth, StrokeCap.Round)
            }
            RoadPanelIconKind.Sensors -> {
                drawCircle(color, s * .13f, Offset(w * .28f, h * .32f), style = stroke)
                drawCircle(color, s * .13f, Offset(w * .70f, h * .48f), style = stroke)
                drawCircle(color, s * .13f, Offset(w * .38f, h * .74f), style = stroke)
                drawLine(color, Offset(w * .40f, h * .37f), Offset(w * .58f, h * .44f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .60f, h * .57f), Offset(w * .48f, h * .66f), strokeWidth, StrokeCap.Round)
            }
            RoadPanelIconKind.Settings -> {
                drawCircle(color, s * .19f, Offset(cx, cy), style = stroke)
                for (angle in 0 until 360 step 45) {
                    val radians = Math.toRadians(angle.toDouble())
                    val start = Offset(
                        cx + kotlin.math.cos(radians).toFloat() * s * .31f,
                        cy + kotlin.math.sin(radians).toFloat() * s * .31f,
                    )
                    val end = Offset(
                        cx + kotlin.math.cos(radians).toFloat() * s * .40f,
                        cy + kotlin.math.sin(radians).toFloat() * s * .40f,
                    )
                    drawLine(color, start, end, strokeWidth, StrokeCap.Round)
                }
            }
            RoadPanelIconKind.Refresh -> {
                drawArc(color, -40f, 300f, false, Offset(w * .22f, h * .22f), Size(w * .56f, h * .56f), style = stroke)
                val arrow = Path().apply {
                    moveTo(w * .76f, h * .20f)
                    lineTo(w * .76f, h * .42f)
                    lineTo(w * .58f, h * .34f)
                }
                drawPath(arrow, color, style = stroke)
            }
            RoadPanelIconKind.Layers -> {
                drawRoundRect(color, Offset(w * .24f, h * .18f), Size(w * .52f, h * .30f), CornerRadius(s * .06f), style = stroke)
                drawLine(color, Offset(w * .22f, h * .56f), Offset(w * .78f, h * .56f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .28f, h * .70f), Offset(w * .72f, h * .70f), strokeWidth, StrokeCap.Round)
            }
            RoadPanelIconKind.Locate -> {
                drawCircle(color, s * .25f, Offset(cx, cy), style = stroke)
                drawCircle(color, s * .06f, Offset(cx, cy))
                drawLine(color, Offset(cx, h * .10f), Offset(cx, h * .24f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(cx, h * .76f), Offset(cx, h * .90f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .10f, cy), Offset(w * .24f, cy), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .76f, cy), Offset(w * .90f, cy), strokeWidth, StrokeCap.Round)
            }
            RoadPanelIconKind.Speed -> {
                drawArc(color, 180f, 180f, false, Offset(w * .18f, h * .24f), Size(w * .64f, h * .64f), style = stroke)
                drawLine(color, Offset(cx, h * .62f), Offset(w * .66f, h * .42f), strokeWidth, StrokeCap.Round)
                drawCircle(color, s * .04f, Offset(cx, h * .62f))
            }
            RoadPanelIconKind.Altitude -> {
                val mountain = Path().apply {
                    moveTo(w * .14f, h * .78f)
                    lineTo(w * .40f, h * .28f)
                    lineTo(w * .54f, h * .54f)
                    lineTo(w * .66f, h * .34f)
                    lineTo(w * .88f, h * .78f)
                }
                drawPath(mountain, color, style = stroke)
            }
            RoadPanelIconKind.Solar -> {
                drawCircle(color, s * .16f, Offset(cx, cy), style = stroke)
                for (angle in 0 until 360 step 45) {
                    val radians = Math.toRadians(angle.toDouble())
                    drawLine(
                        color,
                        Offset(cx + kotlin.math.cos(radians).toFloat() * s * .28f, cy + kotlin.math.sin(radians).toFloat() * s * .28f),
                        Offset(cx + kotlin.math.cos(radians).toFloat() * s * .40f, cy + kotlin.math.sin(radians).toFloat() * s * .40f),
                        strokeWidth,
                        StrokeCap.Round,
                    )
                }
            }
            RoadPanelIconKind.Connection -> {
                drawArc(color, 205f, 130f, false, Offset(w * .20f, h * .20f), Size(w * .60f, h * .60f), style = stroke)
                drawArc(color, 218f, 104f, false, Offset(w * .32f, h * .34f), Size(w * .36f, h * .36f), style = stroke)
                drawCircle(color, s * .045f, Offset(cx, h * .72f))
            }
            RoadPanelIconKind.SensorCooler -> {
                drawRoundRect(color, Offset(w * .16f, h * .28f), Size(w * .68f, h * .56f), CornerRadius(s * .09f), style = stroke)
                drawLine(color, Offset(w * .16f, h * .45f), Offset(w * .84f, h * .45f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .38f, h * .18f), Offset(w * .62f, h * .18f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .38f, h * .18f), Offset(w * .34f, h * .28f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .62f, h * .18f), Offset(w * .66f, h * .28f), strokeWidth, StrokeCap.Round)
            }
            RoadPanelIconKind.SensorHead -> {
                drawCircle(color, s * .18f, Offset(cx, h * .31f), style = stroke)
                drawArc(color, 200f, 140f, false, Offset(w * .22f, h * .45f), Size(w * .56f, h * .40f), style = stroke)
            }
            RoadPanelIconKind.SensorRoof -> {
                val roof = Path().apply {
                    moveTo(w * .12f, h * .67f)
                    lineTo(w * .28f, h * .39f)
                    lineTo(w * .72f, h * .39f)
                    lineTo(w * .88f, h * .67f)
                    close()
                }
                drawPath(roof, color, style = stroke)
                drawLine(color, Offset(w * .20f, h * .76f), Offset(w * .80f, h * .76f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .29f, h * .39f), Offset(w * .37f, h * .20f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .71f, h * .39f), Offset(w * .63f, h * .20f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * .37f, h * .20f), Offset(w * .63f, h * .20f), strokeWidth, StrokeCap.Round)
            }
            RoadPanelIconKind.SensorOutside -> {
                drawCircle(color, s * .17f, Offset(cx, cy), style = stroke)
                for (angle in 0 until 360 step 45) {
                    val radians = Math.toRadians(angle.toDouble())
                    drawLine(
                        color,
                        Offset(cx + kotlin.math.cos(radians).toFloat() * s * .29f, cy + kotlin.math.sin(radians).toFloat() * s * .29f),
                        Offset(cx + kotlin.math.cos(radians).toFloat() * s * .40f, cy + kotlin.math.sin(radians).toFloat() * s * .40f),
                        strokeWidth,
                        StrokeCap.Round,
                    )
                }
            }
            RoadPanelIconKind.SensorHeater -> {
                val flame = Path().apply {
                    moveTo(cx, h * .12f)
                    cubicTo(w * .57f, h * .30f, w * .76f, h * .38f, w * .76f, h * .61f)
                    cubicTo(w * .76f, h * .79f, w * .64f, h * .88f, cx, h * .88f)
                    cubicTo(w * .35f, h * .88f, w * .23f, h * .77f, w * .23f, h * .61f)
                    cubicTo(w * .23f, h * .43f, w * .35f, h * .36f, w * .42f, h * .25f)
                    cubicTo(w * .46f, h * .20f, w * .48f, h * .16f, cx, h * .12f)
                }
                drawPath(flame, color, style = stroke)
                drawArc(
                    color,
                    185f,
                    170f,
                    false,
                    Offset(w * .37f, h * .52f),
                    Size(w * .26f, h * .25f),
                    style = stroke,
                )
            }
        }
    }
}
