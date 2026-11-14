package com.bentrengrove.chess.titlescreen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bentrengrove.chess.ui.ChessTheme

/**
 * Asks the player to paste a FEN position or PGN game. [onLoad] returns false when the text
 * can't be loaded, which keeps the dialog open with an error so the player can fix it.
 */
@Composable
fun LoadGameDialog(
    onLoad: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        LoadGameDialogContent(
            text = text,
            isError = isError,
            onTextChange = {
                text = it
                isError = false
            },
            onLoad = { isError = !onLoad(text) },
            onDismiss = onDismiss,
        )
    }
}

@Composable
internal fun LoadGameDialogContent(
    text: String,
    isError: Boolean,
    onTextChange: (String) -> Unit,
    onLoad: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Load Game",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Paste a FEN position or a PGN game.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("FEN or PGN") },
                isError = isError,
                supportingText =
                    if (isError) {
                        { Text("This isn't a valid FEN or PGN game.") }
                    } else {
                        null
                    },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                TextButton(onClick = onLoad, enabled = text.isNotBlank()) {
                    Text("Load")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadGameDialogEmptyPreview() {
    ChessTheme {
        LoadGameDialogContent(text = "", isError = false, onTextChange = {}, onLoad = {}, onDismiss = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadGameDialogErrorPreview() {
    ChessTheme {
        LoadGameDialogContent(text = "1. e4 e5 2. Ke3", isError = true, onTextChange = {}, onLoad = {}, onDismiss = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadGameDialogDarkPreview() {
    ChessTheme(darkTheme = true) {
        LoadGameDialogContent(
            text = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            isError = false,
            onTextChange = {},
            onLoad = {},
            onDismiss = {},
        )
    }
}
