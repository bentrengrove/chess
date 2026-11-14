package com.bentrengrove.chess.screenshots

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.R
import com.bentrengrove.chess.ui.ChessTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Renders the adaptive launcher icon layers the way launchers do: each 108dp layer is masked to
 * the central 72dp by a launcher-chosen shape. Covers the common masks, the full layer with the
 * mask and 66dp safe zone drawn on top, and the themed (monochrome) icon.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric's SDK 35+ sandboxes need Java 21, and this project builds with JDK 17.
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppIconScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appIcon_light() = captureAppIcon(darkTheme = false)

    @Test
    fun appIcon_dark() = captureAppIcon(darkTheme = true)

    private fun captureAppIcon(darkTheme: Boolean) {
        composeRule.setContent {
            ChessTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppIconSheet(darkTheme = darkTheme)
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}

// Approximations of the Android 13+ themed icon colours for a blue wallpaper.
private val themedLightBackground = Color(0xFFD8E2FF)
private val themedLightForeground = Color(0xFF001A41)
private val themedDarkBackground = Color(0xFF1F2A3D)
private val themedDarkForeground = Color(0xFFADC6FF)

private val SquircleShape =
    GenericShape { size, _ ->
        // Superellipse |x|^4 + |y|^4 = 1, close to the squircle used by many launchers.
        val steps = 120
        for (i in 0..steps) {
            val t = 2 * Math.PI * i / steps
            val c = cos(t)
            val s = sin(t)
            val x = sign(c) * abs(c).pow(0.5)
            val y = sign(s) * abs(s).pow(0.5)
            val px = (size.width / 2 * (1 + x)).toFloat()
            val py = (size.height / 2 * (1 + y)).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

private val TeardropShape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 50, bottomEndPercent = 0)

@Composable
private fun AppIconSheet(darkTheme: Boolean) {
    val masks =
        listOf(
            "Circle" to CircleShape,
            "Squircle" to SquircleShape,
            "Rounded square" to RoundedCornerShape(percent = 20),
            "Teardrop" to TeardropShape,
        )
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Masks", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            masks.forEach { (label, shape) -> LabelledIcon(label) { AdaptiveIcon(shape) } }
        }
        Text("Themed", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val background = if (darkTheme) themedDarkBackground else themedLightBackground
            val foreground = if (darkTheme) themedDarkForeground else themedLightForeground
            masks.take(2).forEach { (label, shape) ->
                LabelledIcon(label) { AdaptiveIcon(shape, themed = background to foreground) }
            }
        }
        Text("Full layer, 72dp mask and 66dp safe zone", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LayerWithGuides(themed = null)
            LayerWithGuides(themed = Color.Transparent to MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun LabelledIcon(
    label: String,
    icon: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        icon()
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** The icon as a launcher shows it: [size] is the visible (masked) size, 72dp of the 108dp layer. */
@Composable
private fun AdaptiveIcon(
    shape: Shape,
    size: Dp = 80.dp,
    themed: Pair<Color, Color>? = null,
) {
    Box(
        modifier = Modifier.size(size).clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        IconLayers(layerSize = size * 108f / 72f, themed = themed)
    }
}

@Composable
private fun IconLayers(
    layerSize: Dp,
    themed: Pair<Color, Color>?,
) {
    Box(Modifier.requiredSize(layerSize)) {
        val layerModifier = Modifier.requiredSize(layerSize)
        if (themed == null) {
            Image(painterResource(R.drawable.ic_launcher_background), null, layerModifier)
            Image(painterResource(R.drawable.ic_launcher_foreground), null, layerModifier)
        } else {
            Box(layerModifier.background(themed.first))
            Image(
                painterResource(R.drawable.ic_launcher_monochrome),
                null,
                layerModifier,
                colorFilter = ColorFilter.tint(themed.second),
            )
        }
    }
}

@Composable
private fun LayerWithGuides(themed: Pair<Color, Color>?) {
    val guide = Color(0xFFE53935)
    Box(
        Modifier.size(162.dp).drawWithContent {
            drawContent()
            val unit = size.width / 108f
            val stroke = Stroke(width = 1.dp.toPx())
            // Launchers only ever show the central 72dp; the safe zone is a 66dp circle.
            drawRect(guide, topLeft = Offset(18 * unit, 18 * unit), size = Size(72 * unit, 72 * unit), style = stroke)
            drawCircle(guide, radius = 33 * unit, center = center, style = stroke)
        },
    ) {
        IconLayers(layerSize = 162.dp, themed = themed)
    }
}
