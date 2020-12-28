package com.bentrengrove.chess

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val CAPTURED_PIECE_SIZE = 32.dp
@Composable
fun CapturedView(pieces: List<Piece>, modifier: Modifier = Modifier) {
    Row(modifier = Modifier.height(CAPTURED_PIECE_SIZE).then(modifier)) {
        pieces.forEach {
            PieceView(piece = it, modifier = Modifier.width(CAPTURED_PIECE_SIZE).height(CAPTURED_PIECE_SIZE))
        }
    }
}