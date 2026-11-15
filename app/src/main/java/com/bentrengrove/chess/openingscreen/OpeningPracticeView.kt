package com.bentrengrove.chess.openingscreen

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.engine.HintLevel
import com.bentrengrove.chess.engine.OpeningNode
import com.bentrengrove.chess.engine.OpeningTrainer
import com.bentrengrove.chess.engine.Position
import com.bentrengrove.chess.engine.TrainerMode
import com.bentrengrove.chess.gamescreen.GameView
import com.bentrengrove.chess.ui.ChessTheme
import kotlin.random.Random

/**
 * Practising one opening: the board from the learner's side, a Learn/Drill switch, and the
 * controls for the current mode. Stateless apart from the piece the learner has selected.
 */
@Composable
fun OpeningPracticeView(
    trainer: OpeningTrainer,
    onMove: (from: Position, to: Position) -> Unit,
    onModeChange: (TrainerMode) -> Unit,
    onSelectLine: (Int) -> Unit,
    onHint: () -> Unit,
    onUndo: () -> Unit,
    onNextLine: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = trainer.game
    // Any change of position, or a rejected move, clears the selection.
    var selection: Position? by remember(trainer.lineIndex, trainer.ply, trainer.wrongMoveCount) { mutableStateOf(null) }

    val onTap: (Position) -> Unit = onTap@{ position ->
        if (!trainer.isLearnerTurn) return@onTap
        val selected = selection
        selection =
            when {
                game.canSelect(position) -> {
                    position
                }

                selected != null && game.canMove(selected, position) -> {
                    onMove(selected, position)
                    null
                }

                else -> {
                    null
                }
            }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        PracticeLayout(trainer, selection, onTap, onModeChange, onSelectLine, onHint, onUndo, onNextLine, onBack)
    }
}

@Composable
private fun PracticeLayout(
    trainer: OpeningTrainer,
    selection: Position?,
    onTap: (Position) -> Unit,
    onModeChange: (TrainerMode) -> Unit,
    onSelectLine: (Int) -> Unit,
    onHint: () -> Unit,
    onUndo: () -> Unit,
    onNextLine: () -> Unit,
    onBack: () -> Unit,
) {
    val game = trainer.game
    Column(Modifier.fillMaxSize()) {
        PracticeHeader(trainer = trainer, onModeChange = onModeChange, onSelectLine = onSelectLine)

        WrongMoveFeedback(wrongMoveCount = trainer.wrongMoveCount) { feedbackModifier ->
            GameView(
                modifier = feedbackModifier,
                game = game,
                selection = selection,
                moves = game.movesForPieceAt(selection),
                didTap = onTap,
                orientation = trainer.opening.side,
                hintSquares = listOfNotNull(trainer.highlightedFrom, trainer.highlightedTo),
            )
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (trainer.mode == TrainerMode.Drill && trainer.isLineComplete) {
                DrillResultCard(trainer = trainer, onNextLine = onNextLine, onBack = onBack)
            } else {
                Text(text = statusText(trainer), style = MaterialTheme.typography.titleMedium)
                when (trainer.mode) {
                    TrainerMode.Learn -> LearnControls(trainer = trainer, onUndo = onUndo, onNextLine = onNextLine)
                    TrainerMode.Drill -> DrillControls(trainer = trainer, onHint = onHint)
                }
            }
        }
    }
}

@Composable
private fun PracticeHeader(
    trainer: OpeningTrainer,
    onModeChange: (TrainerMode) -> Unit,
    onSelectLine: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = trainer.opening.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "You play ${sideName(trainer.opening.side)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ModeSwitch(mode = trainer.mode, onModeChange = onModeChange)
        }
        when (trainer.mode) {
            TrainerMode.Learn -> {
                LinePicker(trainer = trainer, onSelectLine = onSelectLine)
            }

            TrainerMode.Drill -> {
                Text(
                    text = "${trainer.coveredLines.size}/${trainer.lines.size} lines covered",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ModeSwitch(
    mode: TrainerMode,
    onModeChange: (TrainerMode) -> Unit,
) {
    val modes = TrainerMode.entries
    SingleChoiceSegmentedButtonRow {
        modes.forEachIndexed { index, option ->
            SegmentedButton(
                selected = mode == option,
                onClick = { onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
            ) {
                Text(option.name)
            }
        }
    }
}

@Composable
private fun LinePicker(
    trainer: OpeningTrainer,
    onSelectLine: (Int) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    TextButton(onClick = { showSheet = true }) {
        Text("Line ${trainer.lineIndex + 1}/${trainer.lines.size}: ${lineName(trainer.lines, trainer.lineIndex)}")
        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose line")
    }
    if (showSheet) {
        LineSheet(
            lines = trainer.lines,
            selectedIndex = trainer.lineIndex,
            onSelectLine = {
                showSheet = false
                onSelectLine(it)
            },
            onDismiss = { showSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineSheet(
    lines: List<List<OpeningNode>>,
    selectedIndex: Int,
    onSelectLine: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LineList(lines = lines, selectedIndex = selectedIndex, onSelectLine = onSelectLine)
    }
}

/**
 * Every line, named by the move where it branches off, with its full moves underneath: the
 * moves it shares with other lines dimmed and its own moves in full contrast.
 */
@Composable
internal fun LineList(
    lines: List<List<OpeningNode>>,
    selectedIndex: Int,
    onSelectLine: (Int) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        Text(
            text = "Choose a line",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        lines.indices.forEach { index ->
            val branch = branchPly(lines, index)
            val shared = formatMoves(lines[index].take(branch), startPly = 0)
            val own = formatMoves(lines[index].drop(branch), startPly = branch)
            val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            ListItem(
                headlineContent = { Text("Line ${index + 1}: ${lineName(lines, index)}") },
                supportingContent = {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = dim)) { append(shared) }
                            if (shared.isNotEmpty()) append(" ")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) { append(own) }
                        },
                    )
                },
                trailingContent =
                    if (index == selectedIndex) {
                        { Icon(Icons.Filled.Check, contentDescription = "Current line") }
                    } else {
                        null
                    },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onSelectLine(index) },
            )
        }
    }
}

@Composable
private fun LearnControls(
    trainer: OpeningTrainer,
    onUndo: () -> Unit,
    onNextLine: () -> Unit,
) {
    val comments = trainer.currentComments
    if (comments.isNotEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                comments.forEach { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onUndo, enabled = trainer.canUndo) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Back a move")
        }
        if (trainer.isLineComplete) {
            Button(onClick = onNextLine) { Text("Next line") }
        }
    }
}

@Composable
private fun DrillControls(
    trainer: OpeningTrainer,
    onHint: () -> Unit,
) {
    OutlinedButton(onClick = onHint, enabled = trainer.isLearnerTurn && trainer.hint != HintLevel.Move) {
        Text(if (trainer.hint == HintLevel.None) "Hint" else "Show move")
    }
}

@Composable
private fun DrillResultCard(
    trainer: OpeningTrainer,
    onNextLine: () -> Unit,
    onBack: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Line complete", style = MaterialTheme.typography.titleLarge)
            val mistakes = trainer.lineMistakes
            Text(
                text =
                    when (mistakes) {
                        0 -> "No mistakes"
                        1 -> "1 mistake"
                        else -> "$mistakes mistakes"
                    },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${trainer.coveredLines.size}/${trainer.lines.size} lines covered",
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                ) {
                    Text("Back")
                }
                Button(onClick = onNextLine) { Text("Next line") }
            }
        }
    }
}

/** Shakes and briefly flashes red whenever [wrongMoveCount] goes up. */
@Composable
private fun WrongMoveFeedback(
    wrongMoveCount: Int,
    content: @Composable (Modifier) -> Unit,
) {
    val shake = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(wrongMoveCount) {
        if (wrongMoveCount == 0) return@LaunchedEffect
        flash.snapTo(0.35f)
        for (offset in listOf(-12f, 12f, -8f, 8f, -4f, 0f)) shake.animateTo(offset, tween(durationMillis = 40))
        flash.animateTo(0f, tween(durationMillis = 200))
    }
    content(
        Modifier
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
            .drawWithContent {
                drawContent()
                if (flash.value > 0f) drawRect(Color.Red.copy(alpha = flash.value))
            },
    )
}

private fun statusText(trainer: OpeningTrainer): String {
    val opponent = sideName(trainer.opening.side.other())
    return when {
        trainer.isLineComplete -> "Line complete"
        trainer.isOpponentTurn -> "$opponent to move…"
        trainer.mode == TrainerMode.Learn -> "Your move: play the highlighted move"
        trainer.hint == HintLevel.Piece -> "Hint: move the highlighted piece"
        trainer.hint == HintLevel.Move -> "Hint: play the highlighted move"
        trainer.currentMoveMissed -> "Not quite, try again"
        else -> "Your move"
    }
}

/** How many moves line [index] shares with the line it has most in common with. */
internal fun branchPly(
    lines: List<List<OpeningNode>>,
    index: Int,
): Int {
    val line = lines[index]
    return lines.indices.filter { it != index }.maxOfOrNull { other ->
        line.zip(lines[other]).takeWhile { (a, b) -> a.san == b.san }.size
    } ?: 0
}

/**
 * A short name for line [index]: the move where it branches off the other lines, e.g. "3. Nd2"
 * or "3... exf4", or "Main line" when it is the only line. Since the learner never branches,
 * this is always an opponent move.
 */
internal fun lineName(
    lines: List<List<OpeningNode>>,
    index: Int,
): String {
    if (lines.size == 1) return "Main line"
    val branch = branchPly(lines, index)
    return formatMoves(lines[index].drop(branch).take(1), startPly = branch)
}

/**
 * Formats [nodes], the first of which is ply [startPly] from the start, as "3. e5 c5 4. c3", with
 * a no-break space after each move number.
 */
internal fun formatMoves(
    nodes: List<OpeningNode>,
    startPly: Int,
): String =
    nodes
        .mapIndexed { offset, node ->
            val ply = startPly + offset
            when {
                // A no-break space keeps a move number on the same row as its move when text wraps.
                ply % 2 == 0 -> "${ply / 2 + 1}.\u00A0${node.san}"

                offset == 0 -> "${ply / 2 + 1}...\u00A0${node.san}"

                else -> node.san
            }
        }.joinToString(" ")

/** French sample, Learn mode, after 1. e4 e6 2. d4 so the comments on e6 and d4 show. */
internal fun sampleLearnTrainer(): OpeningTrainer {
    var trainer = OpeningTrainer.start(sampleOpenings[0].opening)
    trainer = trainer.playOpponentMove()
    val move = trainer.expectedMove!!
    return trainer.playMove(move.from, move.to).playOpponentMove()
}

/** Vienna sample, Drill mode, on the learner's second move with the first hint used. */
internal fun sampleDrillTrainer(): OpeningTrainer {
    var trainer = OpeningTrainer.start(sampleOpenings[1].opening, TrainerMode.Drill, Random(0)).selectLine(0)
    val move = trainer.expectedMove!!
    trainer = trainer.playMove(move.from, move.to).playOpponentMove()
    return trainer.requestHint()
}

/** Vienna sample, Drill mode, at the end of a line with one mistake. */
internal fun sampleDrillResultTrainer(): OpeningTrainer {
    var trainer = OpeningTrainer.start(sampleOpenings[1].opening, TrainerMode.Drill, Random(0)).selectLine(0)
    trainer = trainer.requestHint()
    while (!trainer.isLineComplete) {
        trainer =
            if (trainer.isOpponentTurn) {
                trainer.playOpponentMove()
            } else {
                val move = trainer.expectedMove!!
                trainer.playMove(move.from, move.to)
            }
    }
    return trainer
}

@Composable
private fun PracticePreviewContent(
    trainer: OpeningTrainer,
    darkTheme: Boolean,
) {
    ChessTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            OpeningPracticeView(
                trainer = trainer,
                onMove = { _, _ -> },
                onModeChange = {},
                onSelectLine = {},
                onHint = {},
                onUndo = {},
                onNextLine = {},
                onBack = {},
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun LearnPreview() {
    PracticePreviewContent(sampleLearnTrainer(), darkTheme = false)
}

@Preview(showBackground = true, heightDp = 860, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LearnDarkPreview() {
    PracticePreviewContent(sampleLearnTrainer(), darkTheme = true)
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun DrillPreview() {
    PracticePreviewContent(sampleDrillTrainer(), darkTheme = false)
}

@Preview(showBackground = true, heightDp = 860, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DrillDarkPreview() {
    PracticePreviewContent(sampleDrillTrainer(), darkTheme = true)
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun DrillResultPreview() {
    PracticePreviewContent(sampleDrillResultTrainer(), darkTheme = false)
}

@Preview(showBackground = true, heightDp = 860, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DrillResultDarkPreview() {
    PracticePreviewContent(sampleDrillResultTrainer(), darkTheme = true)
}

@Composable
private fun LineListPreviewContent(darkTheme: Boolean) {
    ChessTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            LineList(lines = sampleLearnTrainer().lines, selectedIndex = 0, onSelectLine = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LineListPreview() {
    LineListPreviewContent(darkTheme = false)
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LineListDarkPreview() {
    LineListPreviewContent(darkTheme = true)
}
