package com.bentrengrove.chess.gamescreen

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionLayoutScope
import kotlinx.coroutines.channels.Channel

@Composable
fun AnimatingConstraintLayout(
    constraintSet: ConstraintSet,
    modifier: Modifier = Modifier,
    content: @Composable MotionLayoutScope.() -> Unit
    ) {
    var currentConstraints by remember { mutableStateOf(constraintSet) }
    val progress = remember { Animatable(0.0f) }
    val channel = remember { Channel<ConstraintSet>(Channel.CONFLATED) }

    SideEffect {
        channel.trySend(constraintSet)
    }

    LaunchedEffect(channel) {
        for (constraints in channel) {
            val newConstraints = channel.tryReceive().getOrNull() ?: constraints
            if (newConstraints != currentConstraints) {
                progress.snapTo(0f)
                progress.animateTo(1f)

                currentConstraints = newConstraints
            }
        }

    }

    MotionLayout(
        start = currentConstraints,
        end = constraintSet,
        progress = progress.value,
        modifier = modifier,
        content = content)
}