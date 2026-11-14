package com.bentrengrove.chess.engine

/**
 * One position in an [Opening] tree, reached by playing [san] from its parent.
 *
 * @property san the SAN of the move that reached this node, with annotation glyphs (`!`, `?`)
 * removed but check/mate markers kept (e.g. "Bb5+"). Null for the root (the starting position).
 * @property comment the PGN `{...}` comment attached to this move, if any. On the root it holds
 * any comment that comes before the first move.
 * @property children the moves that can follow, in PGN order: main line first, then variations.
 */
data class OpeningNode(
    val san: String?,
    val comment: String? = null,
    val children: List<OpeningNode> = emptyList(),
)

/**
 * An opening repertoire: a tree of moves from the standard starting position in which the
 * learner, playing [side], always has exactly one move to make while the opponent may branch.
 */
data class Opening(
    val name: String,
    val side: PieceColor,
    val root: OpeningNode,
) {
    companion object {
        /**
         * Parses PGN text containing variations and comments into an [Opening].
         *
         * The opening's name and side come from the [name] and [side] arguments when given,
         * otherwise from the PGN's `[Event "..."]` and `[Side "White"|"Black"]` headers. A
         * missing name or side throws.
         *
         * Every move is replayed from the standard starting position, so an illegal or
         * unparseable move throws with the move and the line leading to it. A PGN that offers
         * more than one move where it is [side]'s turn is rejected. `[FEN]` start positions are
         * not supported.
         *
         * Move numbers (`3.`, `3...`), NAGs (`$1`), annotation glyphs (`!`, `?!`), `;` line
         * comments and result tokens (`*`, `1-0`, ...) are ignored. A comment straight after
         * `(` is attached to the first move of that variation. Repeating a move that already
         * exists at the same point merges into the existing node.
         */
        fun fromPgn(
            pgn: String,
            name: String? = null,
            side: PieceColor? = null,
        ): Opening {
            val tags = PGN_TAG_REGEX.findAll(pgn).associate { it.groupValues[1] to it.groupValues[2] }
            require("FEN" !in tags) { "Openings must start from the standard position; [FEN] is not supported" }

            val openingName =
                name ?: tags["Event"]?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("Opening has no name: pass one or add an [Event] header")
            val openingSide =
                side ?: tags["Side"]?.let { parseSide(it) }
                    ?: throw IllegalArgumentException("Opening has no side: pass one or add a [Side \"White\"] header")

            val movetext = PGN_TAG_REGEX.replace(pgn, " ")
            val root = OpeningTreeBuilder(openingSide).build(tokenize(movetext))
            return Opening(openingName, openingSide, root)
        }
    }
}

/**
 * Every path from the root to a leaf, depth first in PGN order (main line first). Each line
 * lists the move nodes from the first move to the leaf; the root itself is not included.
 * An opening with no moves has no lines.
 */
fun Opening.lines(): List<List<OpeningNode>> {
    val result = mutableListOf<List<OpeningNode>>()

    fun visit(
        node: OpeningNode,
        path: List<OpeningNode>,
    ) {
        if (node.children.isEmpty()) {
            if (path.isNotEmpty()) result += path
            return
        }
        node.children.forEach { visit(it, path + it) }
    }
    visit(root, emptyList())
    return result
}

private val PGN_TAG_REGEX = Regex("\\[(\\w+)\\s+\"([^\"]*)\"]")
private val MOVE_NUMBER_REGEX = Regex("^\\d+\\.+")
private val OPENING_RESULT_TOKENS = setOf("1-0", "0-1", "1/2-1/2", "*")

private fun parseSide(value: String): PieceColor =
    when (value.trim().lowercase()) {
        "white" -> PieceColor.White
        "black" -> PieceColor.Black
        else -> throw IllegalArgumentException("[Side \"$value\"] must be \"White\" or \"Black\"")
    }

private sealed class PgnToken {
    data class SanMove(
        val san: String,
    ) : PgnToken()

    data class Comment(
        val text: String,
    ) : PgnToken()

    object VariationStart : PgnToken()

    object VariationEnd : PgnToken()
}

/** Splits movetext into moves, comments and variation brackets, dropping everything else. */
private fun tokenize(movetext: String): List<PgnToken> {
    val tokens = mutableListOf<PgnToken>()
    var i = 0
    while (i < movetext.length) {
        val c = movetext[i]
        when {
            c.isWhitespace() -> {
                i++
            }

            c == '{' -> {
                val end = movetext.indexOf('}', i + 1)
                require(end >= 0) { "Unterminated comment starting '${movetext.substring(i).take(20)}'" }
                val text = movetext.substring(i + 1, end).trim().replace(Regex("\\s+"), " ")
                if (text.isNotEmpty()) tokens += PgnToken.Comment(text)
                i = end + 1
            }

            c == '}' -> {
                throw IllegalArgumentException("Unmatched '}' at '${movetext.substring(i).take(20)}'")
            }

            c == ';' -> {
                val end = movetext.indexOf('\n', i).let { if (it < 0) movetext.length else it }
                i = end
            }

            c == '(' -> {
                tokens += PgnToken.VariationStart
                i++
            }

            c == ')' -> {
                tokens += PgnToken.VariationEnd
                i++
            }

            else -> {
                var end = i
                while (end < movetext.length && !movetext[end].isWhitespace() && movetext[end] !in "{}();") end++
                sanFromWord(movetext.substring(i, end))?.let { tokens += PgnToken.SanMove(it) }
                i = end
            }
        }
    }
    return tokens
}

/** Turns one whitespace-delimited word into a SAN move, or null if it isn't one. */
private fun sanFromWord(word: String): String? {
    if (word in OPENING_RESULT_TOKENS || word.startsWith("$")) return null
    val san = word.replace(MOVE_NUMBER_REGEX, "").trimEnd('!', '?').trimStart('.')
    return san.ifEmpty { null }
}

/** Builds the immutable tree; the mutable builders never escape [build]. */
private class OpeningTreeBuilder(
    private val side: PieceColor,
) {
    private class Node(
        val san: String?,
        val game: Game,
        val parent: Node?,
    ) {
        var comment: String? = null
        val children = mutableListOf<Node>()

        val path: List<String>
            get() = generateSequence(this) { it.parent }.mapNotNull { it.san }.toList().reversed()

        fun addComment(text: String) {
            comment = comment?.let { "$it $text" } ?: text
        }

        fun toOpeningNode(): OpeningNode = OpeningNode(san, comment, children.map { it.toOpeningNode() })
    }

    fun build(tokens: List<PgnToken>): OpeningNode {
        val root = Node(san = null, game = Game(), parent = null)
        var current = root
        val variationStack = ArrayDeque<Node>()
        // Comments seen before the first move of a variation, waiting for that move.
        val pendingComments = mutableListOf<String>()
        var atVariationStart = false

        for (token in tokens) {
            when (token) {
                is PgnToken.Comment -> {
                    if (atVariationStart) pendingComments += token.text else current.addComment(token.text)
                }

                PgnToken.VariationStart -> {
                    val parent =
                        current.parent
                            ?: throw IllegalArgumentException("Variation '(' must follow a move")
                    variationStack.addLast(current)
                    current = parent
                    atVariationStart = true
                }

                PgnToken.VariationEnd -> {
                    require(variationStack.isNotEmpty()) { "Unmatched ')' after ${formatLine(current.path)}" }
                    require(!atVariationStart) { "Empty variation after ${formatLine(current.path)}" }
                    current = variationStack.removeLast()
                }

                is PgnToken.SanMove -> {
                    current = addMove(current, token.san)
                    pendingComments.forEach { current.addComment(it) }
                    pendingComments.clear()
                    atVariationStart = false
                }
            }
        }
        require(variationStack.isEmpty()) { "Unclosed '(' after ${formatLine(current.path)}" }
        return root.toOpeningNode()
    }

    private fun addMove(
        parent: Node,
        san: String,
    ): Node {
        val game =
            try {
                parent.game.applySanMove(san)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Illegal or unparseable move '$san' after ${formatLine(parent.path)}: ${e.message}",
                    e,
                )
            }
        parent.children.firstOrNull { it.game == game }?.let { return it }

        require(parent.game.turn != side || parent.children.isEmpty()) {
            val existing = parent.children.joinToString { "'${it.san}'" }
            "Repertoire branches on the learner's (${sideName(side)}) move after ${formatLine(parent.path)}: " +
                "'$san' conflicts with $existing"
        }
        return Node(san, game, parent).also { parent.children += it }
    }
}

private fun sideName(color: PieceColor): String = if (color == PieceColor.White) "White" else "Black"

/** Formats a line of SAN moves from the start as "1.e4 e5 2.Nf3", or "the start" if empty. */
private fun formatLine(sans: List<String>): String {
    if (sans.isEmpty()) return "the start"
    return sans
        .mapIndexed { ply, san -> if (ply % 2 == 0) "${ply / 2 + 1}.$san" else san }
        .joinToString(" ")
}
