package com.bentrengrove.chess.gamescreen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.engine.Piece

private val CAPTURED_PIECE_SIZE = 24.dp

@Composable
fun CapturedView(
    pieces: List<Piece>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            Modifier
                .height(CAPTURED_PIECE_SIZE)
                .horizontalScroll(rememberScrollState())
                .then(modifier),
    ) {
        pieces.forEach {
            PieceView(piece = it, modifier = Modifier.width(CAPTURED_PIECE_SIZE).height(CAPTURED_PIECE_SIZE))
        }
    }
}
