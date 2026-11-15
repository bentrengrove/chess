package com.bentrengrove.chess.openingscreen

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.engine.Opening
import com.bentrengrove.chess.engine.Piece
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.PieceType
import com.bentrengrove.chess.engine.lines
import com.bentrengrove.chess.ui.ChessTheme

/** The bundled openings, each showing its name and the side the learner plays. */
@Composable
fun OpeningListView(
    openings: List<BundledOpening>,
    onOpeningSelected: (BundledOpening) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Openings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(openings, key = { it.id }) { bundled ->
                OpeningRow(opening = bundled.opening, onClick = { onOpeningSelected(bundled) })
            }
        }
    }
}

@Composable
private fun OpeningRow(
    opening: Opening,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Piece(id = "side", type = PieceType.King, color = opening.side).imageResource()),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = opening.name, style = MaterialTheme.typography.titleMedium)
                val lineCount = opening.lines().size
                Text(
                    text = "You play ${sideName(opening.side)} · $lineCount ${if (lineCount == 1) "line" else "lines"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun sideName(color: PieceColor): String = if (color == PieceColor.White) "White" else "Black"

/** Small stand-ins for the bundled openings, for previews and screenshot tests. */
internal val sampleOpenings =
    listOf(
        BundledOpening(
            id = "french",
            opening =
                Opening.fromPgn(
                    "{The French.} 1. e4 e6 {Prepares d5.} 2. d4 {White takes the centre.} d5 3. e5 (3. Nc3 Bb4) c5",
                    name = "French Defence",
                    side = PieceColor.Black,
                ),
        ),
        BundledOpening(
            id = "vienna-gambit",
            opening =
                Opening.fromPgn(
                    "1. e4 e5 2. Nc3 Nf6 3. f4 d5 (3... exf4 4. e5) 4. fxe5",
                    name = "Vienna Gambit",
                    side = PieceColor.White,
                ),
        ),
    )

@Composable
private fun OpeningListPreviewContent(darkTheme: Boolean) {
    ChessTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            OpeningListView(openings = sampleOpenings, onOpeningSelected = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OpeningListPreview() {
    OpeningListPreviewContent(darkTheme = false)
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OpeningListDarkPreview() {
    OpeningListPreviewContent(darkTheme = true)
}
