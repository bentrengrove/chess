package com.bentrengrove.chess.titlescreen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.engine.Board
import com.bentrengrove.chess.engine.Piece
import kotlin.math.roundToInt

/**
 * A cheap integer mix (Murmur3-style finalizer) so nearby inputs produce
 * well-decorrelated outputs. Needed because piece ids like "WP0".."WP7" differ
 * by one character, so their raw String.hashCode() values are near-sequential -
 * slicing bit ranges directly out of those would give a whole rank of pieces
 * almost identical derived values (they'd fall in a single rigid line).
 */
private fun mix(x: Int): Int {
    var h = x
    h = h xor (h ushr 16)
    h *= -0x7ee3623b
    h = h xor (h ushr 13)
    h *= -0x3b314601
    h = h xor (h ushr 16)
    return h
}

/**
 * A stable, deterministic "randomness" derived from a piece's id, so each piece
 * always falls in the same lane/size/speed across recompositions without needing
 * mutable random state.
 */
private class FallingPieceStyle(
    piece: Piece,
    index: Int,
    total: Int,
) {
    private val seed = mix(piece.id.hashCode())

    // Spread pieces evenly across the width by lane (index), then jitter within the
    // lane by hash so it doesn't read as a rigid grid.
    private val laneWidth = 1f / total
    private val jitter = ((seed ushr 4) and 0xFF).toFloat() / 0xFF - 0.5f
    val xFraction = (((index + 0.5f) * laneWidth) + jitter * laneWidth * 0.7f).coerceIn(0.02f, 0.96f)

    val phase = ((seed ushr 8) and 0xFF).toFloat() / 0xFF
    val sizeDp = 30 + ((seed ushr 16) and 0x0F) // 30..45dp
    val alpha = 0.32f + (((seed ushr 20) and 0x0F).toFloat() / 0x0F) * 0.28f // 0.32..0.60
    val speed = 0.75f + ((seed ushr 24) and 0x0F).toFloat() / 0x0F * 0.6f // 0.75..1.35x
}

/**
 * A decorative, non-interactive backdrop of the 32 starting pieces continuously
 * falling and looping. Each piece carries a shared-element key matching the real
 * board's piece of the same id, so wherever a piece happens to be mid-fall becomes
 * the starting point for its flight into the real board when navigation begins.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FallingPiecesBackground(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val pieces = remember { Board().allPieces.map { it.second } }
    var containerSizePx by remember { mutableStateOf(IntOffset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "falling-pieces")
    val globalT by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 9000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "fall-progress",
    )

    val density = LocalDensity.current

    with(sharedTransitionScope) {
        Box(
            modifier =
                modifier.onSizeChanged {
                    containerSizePx = IntOffset(it.width, it.height)
                },
        ) {
            pieces.forEachIndexed { index, piece ->
                val style = remember(piece.id) { FallingPieceStyle(piece, index, pieces.size) }
                val sizePx = with(density) { style.sizeDp.dp.roundToPx() }

                Image(
                    painter = painterResource(id = piece.imageResource()),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(style.sizeDp.dp)
                            .offset {
                                val t = (globalT * style.speed + style.phase) % 1f
                                val travel = containerSizePx.y + sizePx * 2
                                IntOffset(
                                    x = (style.xFraction * containerSizePx.x).roundToInt(),
                                    y = (t * travel - sizePx).roundToInt(),
                                )
                            }.sharedElement(
                                sharedContentState = rememberSharedContentState(key = piece.id),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ).alpha(style.alpha),
                )
            }
        }
    }
}
