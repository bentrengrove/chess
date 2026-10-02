package com.bentrengrove.chess.engine

import kotlin.math.abs

data class Move(
    val from: Position,
    val to: Position,
    val promotion: PieceType? = null,
) {
    fun contains(position: Position): Boolean = from == position || to == position
}

enum class GameState {
    IDLE,
    CHECK,
    CHECKMATE,
    STALEMATE,
}

data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true,
) {
    companion object {
        val ALL = CastlingRights()
        val NONE = CastlingRights(false, false, false, false)
    }
}

sealed class MoveResult {
    data class Success(
        val game: Game,
    ) : MoveResult()

    data class Promotion(
        val color: PieceColor,
        val game: Game,
        val onPieceSelection: (PieceType) -> MoveResult,
    ) : MoveResult()
}

data class Game(
    val board: Board = Board(),
    val history: List<Move> = listOf(),
    // The following three describe the position this game *started* from - they only matter
    // when history is empty (a game loaded mid-position from FEN) or, for castling rights,
    // as the ceiling that history-derived pieceHasMoved checks can further restrict. A game
    // built the normal way (Game() or replaying moves) keeps the all-defaults values below,
    // which reproduce the old, always-standard-start behavior exactly.
    val startingTurn: PieceColor = PieceColor.White,
    val startingCastlingRights: CastlingRights = CastlingRights.ALL,
    val startingEnPassantTarget: Position? = null,
    val halfmoveClock: Int = 0,
    val fullmoveNumber: Int = 1,
    // The full FEN this game was loaded from, or null for the standard start. Lets
    // startingPosition rebuild the exact starting board, which history alone can't recover.
    val startingFen: String? = null,
) {
    companion object {
        /** Parses a full FEN string ("piece-placement active-color castling ep half full"). */
        fun fromFen(fen: String): Game {
            val fields = fen.trim().split(Regex("\\s+"))
            require(fields.size >= 4) { "FEN '$fen' needs at least 4 fields, got ${fields.size}" }

            val board = Board.fromFen(fields[0])
            val turn =
                when (fields[1]) {
                    "w" -> PieceColor.White
                    "b" -> PieceColor.Black
                    else -> throw IllegalArgumentException("'${fields[1]}' is not a valid FEN active color")
                }
            val castlingField = fields[2]
            val castlingRights =
                if (castlingField == "-") {
                    CastlingRights.NONE
                } else {
                    CastlingRights(
                        whiteKingSide = castlingField.contains('K'),
                        whiteQueenSide = castlingField.contains('Q'),
                        blackKingSide = castlingField.contains('k'),
                        blackQueenSide = castlingField.contains('q'),
                    )
                }
            val enPassantTarget = if (fields[3] == "-") null else Position.fromAlgebraic(fields[3])
            val halfmoveClock = fields.getOrNull(4)?.toIntOrNull() ?: 0
            val fullmoveNumber = fields.getOrNull(5)?.toIntOrNull() ?: 1

            return Game(
                board = board,
                history = emptyList(),
                startingTurn = turn,
                startingCastlingRights = castlingRights,
                startingEnPassantTarget = enPassantTarget,
                halfmoveClock = halfmoveClock,
                fullmoveNumber = fullmoveNumber,
                startingFen = fen.trim(),
            )
        }

        /**
         * Loads user-supplied text as a FEN string when its first field looks like a FEN piece
         * placement (eight ranks separated by "/"), and as PGN otherwise.
         */
        fun fromFenOrPgn(text: String): Game {
            val firstField = text.trim().split(Regex("\\s+")).first()
            return if (firstField.count { it == '/' } == 7) fromFen(text) else fromPgn(text)
        }

        /**
         * Replays a PGN movetext (optionally preceded by "[Tag \"value\"]" tag pairs) from the
         * standard starting position, or from a "[FEN \"...\"]" tag's position when present.
         * Move numbers, comments in {}, variations in (), NAGs ($1), and a trailing result token
         * are all ignored - only the main line's SAN moves are applied.
         */
        fun fromPgn(pgn: String): Game {
            val tags = Regex("\\[(\\w+)\\s+\"([^\"]*)\"]").findAll(pgn).associate { it.groupValues[1] to it.groupValues[2] }
            var movetext = Regex("\\[(\\w+)\\s+\"([^\"]*)\"]").replace(pgn, "")
            movetext = stripPgnCommentsAndVariations(movetext)
            movetext = movetext.replace(Regex("\\$\\d+"), " ")
            movetext = movetext.replace(Regex("\\d+\\.+"), " ")

            var game = tags["FEN"]?.let { fromFen(it) } ?: Game()
            movetext
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() && it !in PGN_RESULT_TOKENS }
                .forEach { san -> game = game.applySanMove(san) }

            return game
        }
    }

    val gameState: GameState
        get() {
            val color = turn
            val canMove =
                allMovesFor(color).find {
                    val newBoard = doMove(it.from, it.to)
                    // doMove already refuses to make a move that leaves the mover's own king
                    // in check by returning Success(oldGame) - the board unchanged. Checking
                    // newBoard.game != this is what tells an actually-applied move apart from
                    // one that was silently reverted; kingIsInCheck(color) on newBoard.game
                    // would just re-grade the current position for every reverted move.
                    (newBoard is MoveResult.Success) && newBoard.game != this
                } != null
            if (kingIsInCheck(color)) {
                return if (canMove) GameState.CHECK else GameState.CHECKMATE
            }
            return if (canMove) GameState.IDLE else GameState.STALEMATE
        }

    val displayGameState: String
        get() {
            val turnString = if (turn == PieceColor.White) "White's Turn" else "Black's Turn"
            return when (gameState) {
                GameState.IDLE -> turnString
                GameState.CHECK -> "Check - $turnString"
                GameState.CHECKMATE -> "Checkmate - " + if (turn == PieceColor.White) "Black Wins" else "White Wins"
                GameState.STALEMATE -> "Draw - Stalemate"
            }
        }

    val turn: PieceColor
        get() = history.lastOrNull()?.let { board.pieceAt(it.to)?.color?.other() } ?: startingTurn

    /** The en passant target square a FEN export of the current position would report. */
    val currentEnPassantTarget: Position?
        get() {
            val lastMove = history.lastOrNull() ?: return startingEnPassantTarget
            val piece = board.pieceAt(lastMove.to) ?: return null
            if (piece.type != PieceType.Pawn) return null
            if (abs(lastMove.to.y - lastMove.from.y) != 2) return null
            return Position(lastMove.from.x, (lastMove.from.y + lastMove.to.y) / 2)
        }

    /** Which castles are still available, combining the starting rights with moves made since. */
    val currentCastlingRights: CastlingRights
        get() {
            fun stillAvailable(
                kingSquare: Position,
                rookSquare: Position,
                color: PieceColor,
            ): Boolean {
                if (pieceHasMoved(kingSquare)) return false
                if (pieceHasMoved(rookSquare)) return false
                val rook = board.pieceAt(rookSquare)
                return rook != null && rook.type == PieceType.Rook && rook.color == color
            }
            return CastlingRights(
                whiteKingSide = startingCastlingRights.whiteKingSide && stillAvailable(Position(4, 7), Position(7, 7), PieceColor.White),
                whiteQueenSide = startingCastlingRights.whiteQueenSide && stillAvailable(Position(4, 7), Position(0, 7), PieceColor.White),
                blackKingSide = startingCastlingRights.blackKingSide && stillAvailable(Position(4, 0), Position(7, 0), PieceColor.Black),
                blackQueenSide = startingCastlingRights.blackQueenSide && stillAvailable(Position(4, 0), Position(0, 0), PieceColor.Black),
            )
        }

    /** This position's full FEN string. */
    val fen: String
        get() {
            val activeColor = if (turn == PieceColor.White) "w" else "b"
            val rights = currentCastlingRights
            val castling =
                buildString {
                    if (rights.whiteKingSide) append('K')
                    if (rights.whiteQueenSide) append('Q')
                    if (rights.blackKingSide) append('k')
                    if (rights.blackQueenSide) append('q')
                }.ifEmpty { "-" }
            val enPassant = currentEnPassantTarget?.toAlgebraic() ?: "-"
            return "${board.fen} $activeColor $castling $enPassant $halfmoveClock $fullmoveNumber"
        }

    fun allMovesFor(position: Position): Sequence<Move> =
        board.allPositions
            .asSequence()
            .map { Move(position, it) }
            .filter { canMove(it.from, it.to) }

    fun allMovesFor(color: PieceColor): Sequence<Move> =
        board.allPieces
            .asSequence()
            .mapNotNull { (position, piece) ->
                if (piece.color == color) position else null
            }.flatMap { allMovesFor(it) }

    fun pieceIsThreatenedAt(position: Position): Boolean = board.allPositions.find { canMove(from = it, to = position) } != null

    fun kingPosition(color: PieceColor): Position? = board.firstPosition { it.type is PieceType.King && it.color == color }

    fun kingIsInCheck(color: PieceColor): Boolean {
        val kingPosition = kingPosition(color) ?: return false
        return pieceIsThreatenedAt(kingPosition)
    }

    fun canSelect(position: Position): Boolean = board.pieceAt(position)?.color == turn

    fun canMove(
        from: Position,
        to: Position,
    ): Boolean {
        val piece = board.pieceAt(from) ?: return false

        val delta = to - from
        val other = board.pieceAt(to)
        if (other != null) {
            if (other.color == piece.color) {
                return false
            }
            if (piece.type is PieceType.Pawn) {
                return pawnCanTake(from, delta)
            }
        }

        when (piece.type) {
            is PieceType.Pawn -> {
                if (enPassantTakePermitted(from, to)) {
                    return true
                }
                if (delta.x != 0) {
                    return false
                }
                return when (piece.color) {
                    is PieceColor.White -> {
                        if (from.y == 6) {
                            listOf(-1, -2).contains(delta.y) &&
                                !board.piecesExist(from, to)
                        } else {
                            delta.y == -1
                        }
                    }

                    is PieceColor.Black -> {
                        if (from.y == 1) {
                            listOf(1, 2).contains(delta.y) &&
                                !board.piecesExist(from, to)
                        } else {
                            delta.y == 1
                        }
                    }
                }
            }

            is PieceType.Rook -> {
                return (delta.x == 0 || delta.y == 0) && !board.piecesExist(from, to)
            }

            is PieceType.Bishop -> {
                return abs(delta.x) == abs(delta.y) && !board.piecesExist(from, to)
            }

            is PieceType.Queen -> {
                return (delta.x == 0 || delta.y == 0 || abs(delta.x) == abs(delta.y)) && !board.piecesExist(from, to)
            }

            is PieceType.King -> {
                if (abs(delta.x) <= 1 && abs(delta.y) <= 1) return true
                return castlingPermitted(from, to)
            }

            is PieceType.Knight -> {
                return listOf(
                    Delta(x = 1, y = 2),
                    Delta(x = -1, y = 2),
                    Delta(x = 2, y = 1),
                    Delta(x = -2, y = 1),
                    Delta(x = 1, y = -2),
                    Delta(x = -1, y = -2),
                    Delta(x = 2, y = -1),
                    Delta(x = -2, y = -1),
                ).contains(delta)
            }
        }
    }

    fun doMove(
        from: Position,
        to: Position,
    ): MoveResult {
        val oldGame = this.copy()
        val newGame = move(from, to)
        val wasInCheck = newGame.kingIsInCheck(oldGame.turn)

        if (wasInCheck) {
            return MoveResult.Success(oldGame)
        }

        val wasPromoted = newGame.canPromotePieceAt(to)

        if (wasPromoted) {
            return MoveResult.Promotion(oldGame.turn, newGame) { promoteTo ->
                MoveResult.Success(newGame.promotePieceAt(to, promoteTo))
            }
        }

        return MoveResult.Success(newGame)
    }

    private fun move(
        from: Position,
        to: Position,
    ): Game {
        val movingPiece = board.pieceAt(from)
        val isPawnMove = movingPiece?.type == PieceType.Pawn
        val isCapture = board.pieceAt(to) != null || (isPawnMove && enPassantTakePermitted(from, to))

        val intermediateBoard =
            if (movingPiece?.type == PieceType.King && abs(to.x - from.x) > 1) {
                val kingSide = (to.x == 6)
                val rookPosition = Position(if (kingSide) 7 else 0, to.y)
                val rookDestination = Position(if (kingSide) 5 else 3, to.y)
                board.movePiece(rookPosition, rookDestination)
            } else if (isPawnMove && enPassantTakePermitted(from, to)) {
                board.removePiece(Position(to.x, to.y - (to.y - from.y)))
            } else {
                board
            }
        return Game(
            board = intermediateBoard.movePiece(from, to),
            history = history + listOf(Move(from, to)),
            startingTurn = startingTurn,
            startingCastlingRights = startingCastlingRights,
            startingEnPassantTarget = startingEnPassantTarget,
            halfmoveClock = if (isPawnMove || isCapture) 0 else halfmoveClock + 1,
            fullmoveNumber = if (turn == PieceColor.Black) fullmoveNumber + 1 else fullmoveNumber,
            startingFen = startingFen,
        )
    }

    /** This game's position before any of [history] was played. */
    val startingPosition: Game
        get() = startingFen?.let { fromFen(it) } ?: Game()

    /** The fullmove number of the first move in [history], for numbering a move list. */
    val startingFullmoveNumber: Int
        get() {
            val blackMoves = if (startingTurn == PieceColor.White) history.size / 2 else (history.size + 1) / 2
            return fullmoveNumber - blackMoves
        }

    /** Plays [move], resolving a promotion to the move's recorded piece (a queen if none is recorded). */
    fun play(move: Move): Game =
        when (val result = doMove(move.from, move.to)) {
            is MoveResult.Success -> result.game
            is MoveResult.Promotion -> (result.onPieceSelection(move.promotion ?: PieceType.Queen) as MoveResult.Success).game
        }

    /** Plays each of [moves] in order from this position. */
    fun replay(moves: List<Move>): Game = moves.fold(this) { game, move -> game.play(move) }

    /** This game with its last move taken back, rebuilt from [startingPosition]. */
    fun undoLastMove(): Game = startingPosition.replay(history.dropLast(1))

    fun movesForPieceAt(position: Position?): List<Position> {
        if (position == null) return emptyList()
        return board.allPositions.filter { canMove(position, it) }
    }

    fun pawnCanTake(
        from: Position,
        withDelta: Delta,
    ): Boolean {
        val pawn = board.pieceAt(from) ?: return false
        if (abs(withDelta.x) != 1 || pawn.type != PieceType.Pawn) {
            return false
        }

        return if (pawn.color is PieceColor.White) {
            withDelta.y == -1
        } else {
            withDelta.y == 1
        }
    }

    fun canPromotePieceAt(position: Position): Boolean {
        val pawn = board.pieceAt(position)
        if (pawn?.type !is PieceType.Pawn) return false
        return (pawn.color == PieceColor.White && position.y == 0) || (pawn.color == PieceColor.Black && position.y == 7)
    }

    fun promotePieceAt(
        position: Position,
        to: PieceType,
    ): Game {
        // Record the promotion choice on the move that produced it, not just the board -
        // Board.fromHistory needs it to replay promotion; the plain from/to pair alone is
        // ambiguous about what the pawn became.
        val updatedHistory =
            history.lastOrNull()?.let { lastMove ->
                history.dropLast(1) + lastMove.copy(promotion = to)
            } ?: history
        return copy(board = board.promotePiece(position, to), history = updatedHistory)
    }

    fun pieceHasMoved(at: Position): Boolean = history.find { it.from == at } != null

    fun positionIsThreatened(
        position: Position,
        by: PieceColor,
    ): Boolean {
        return board.allPieces.find { (from, piece) ->
            if (piece.color != by) return@find false
            if (piece.type == PieceType.Pawn) return@find pawnCanTake(from, position - from)
            return@find canMove(from, position)
        } != null
    }

    fun castlingPermitted(
        from: Position,
        to: Position,
    ): Boolean {
        val piece = board.pieceAt(from) ?: return false
        if (piece.type != PieceType.King) return false
        val kingsRow = if (piece.color == PieceColor.Black) 0 else 7
        if (!(from.y == kingsRow && to.y == kingsRow && from.x == 4 && listOf(2, 6).contains(to.x))) return false

        val isKingSide = to.x == 6
        val rights = currentCastlingRights
        val allowedByRights =
            when {
                piece.color == PieceColor.White && isKingSide -> rights.whiteKingSide
                piece.color == PieceColor.White && !isKingSide -> rights.whiteQueenSide
                piece.color == PieceColor.Black && isKingSide -> rights.blackKingSide
                else -> rights.blackQueenSide
            }
        if (!allowedByRights) return false

        return ((if (isKingSide) 5..6 else 1..3).map { board.pieceAt(Position(it, kingsRow)) }.find { it != null } == null) &&
            (
                (if (isKingSide) 4..6 else 2..4)
                    .map {
                        positionIsThreatened(
                            Position(it, kingsRow),
                            by = piece.color.other(),
                        )
                    }.find { it == true } ==
                    null
            )
    }

    fun enPassantTakePermitted(
        from: Position,
        to: Position,
    ): Boolean {
        board.pieceAt(from) ?: return false
        if (!pawnCanTake(from, to - from)) return false

        val lastMove =
            history.lastOrNull() ?: run {
                // No move has been played in this Game yet - e.g. it was just loaded from a FEN
                // whose en passant target square records the same fact history normally would.
                return startingEnPassantTarget == to
            }
        if (lastMove.to.x != to.x) return false

        val lastPiece = board.pieceAt(lastMove.to) ?: return false
        if (lastPiece.type != PieceType.Pawn || lastPiece.color == turn) return false

        return when (lastPiece.color) {
            is PieceColor.White -> {
                lastMove.from.y == to.y + 1 && lastMove.to.y == to.y - 1
            }

            is PieceColor.Black -> {
                lastMove.from.y == to.y - 1 && lastMove.to.y == to.y + 1
            }
        }
    }

    fun valueFor(color: PieceColor): Int =
        board.allPieces
            .filter { it.second.color == color }
            .map { it.second.type.value }
            .sum()

    fun capturedPiecesFor(color: PieceColor): List<Piece> {
        val startingPieces = STARTING_PIECES.filter { it.color == color }.map { it.id }.toSet()
        val currentPieces =
            board.allPieces
                .map { it.second }
                .filter { it.color == color }
                .map { it.id }
                .toSet()
        val capturedPieces = startingPieces - currentPieces
        return capturedPieces.map { Piece.pieceFromString(it) }
    }
}

private fun Board.piecesExist(
    between: Position,
    and: Position,
): Boolean {
    val step =
        Delta(
            x =
                if (between.x > and.x) {
                    -1
                } else if (between.x < and.x) {
                    1
                } else {
                    0
                },
            y =
                if (between.y > and.y) {
                    -1
                } else if (between.y < and.y) {
                    1
                } else {
                    0
                },
        )
    var position = between
    position += step
    while (position != and) {
        if (pieceAt(position) != null) {
            return true
        }
        position += step
    }
    return false
}

private val PGN_RESULT_TOKENS = setOf("1-0", "0-1", "1/2-1/2", "*")

/** Strips `{comments}` and `(variations)`, tracking nesting depth for both bracket kinds together. */
private fun stripPgnCommentsAndVariations(input: String): String {
    val sb = StringBuilder()
    var depth = 0
    for (c in input) {
        when (c) {
            '{', '(' -> depth++
            '}', ')' -> if (depth > 0) depth--
            else -> if (depth == 0) sb.append(c)
        }
    }
    return sb.toString()
}

private fun pieceTypeFromSanChar(c: Char): PieceType =
    when (c) {
        'K' -> PieceType.King
        'Q' -> PieceType.Queen
        'R' -> PieceType.Rook
        'B' -> PieceType.Bishop
        'N' -> PieceType.Knight
        else -> throw IllegalArgumentException("'$c' is not a valid SAN piece letter")
    }

/** Applies one SAN token (e.g. "Nbd7", "exd8=Q+", "O-O") to this game, resolving disambiguation. */
private fun Game.applySanMove(san: String): Game {
    val token = san.trim().trimEnd('+', '#', '!', '?')
    val kingsRow = if (turn == PieceColor.Black) 0 else 7

    if (token == "O-O" || token == "0-0") {
        val from = Position(4, kingsRow)
        val to = Position(6, kingsRow)
        require(canMove(from, to) && isLegalMove(from, to)) { "'$san' is not a legal move for $turn" }
        return applySanResult(doMove(from, to), null)
    }
    if (token == "O-O-O" || token == "0-0-0") {
        val from = Position(4, kingsRow)
        val to = Position(2, kingsRow)
        require(canMove(from, to) && isLegalMove(from, to)) { "'$san' is not a legal move for $turn" }
        return applySanResult(doMove(from, to), null)
    }

    var rest = token
    var promotion: PieceType? = null
    val equalsIndex = rest.indexOf('=')
    if (equalsIndex >= 0) {
        promotion = pieceTypeFromSanChar(rest[equalsIndex + 1])
        rest = rest.substring(0, equalsIndex)
    }

    val pieceType = if (rest[0] in "KQRBN") pieceTypeFromSanChar(rest[0]) else PieceType.Pawn
    val body = (if (pieceType == PieceType.Pawn) rest else rest.substring(1)).replace("x", "")
    require(body.length >= 2) { "'$san' is not a recognizable SAN move" }
    val destination = Position.fromAlgebraic(body.takeLast(2))
    val disambiguation = body.dropLast(2)

    var fromFile: Int? = null
    var fromRank: Int? = null
    disambiguation.forEach { c ->
        when (c) {
            in 'a'..'h' -> fromFile = c - 'a'
            in '1'..'8' -> fromRank = 7 - (c - '1')
        }
    }

    val candidates =
        board.allPieces.filter { (position, piece) ->
            piece.color == turn &&
                piece.type == pieceType &&
                (fromFile == null || position.x == fromFile) &&
                (fromRank == null || position.y == fromRank) &&
                canMove(position, destination) &&
                isLegalMove(position, destination)
        }
    require(candidates.size == 1) {
        "'$san' matched ${candidates.size} legal moves for $turn, expected exactly 1"
    }

    return applySanResult(doMove(candidates.first().first, destination), promotion)
}

/** True if moving from->to doesn't leave the mover's own king in check (mirrors gameState's check). */
private fun Game.isLegalMove(
    from: Position,
    to: Position,
): Boolean {
    val result = doMove(from, to)
    return (result is MoveResult.Success && result.game != this) || result is MoveResult.Promotion
}

private fun Game.applySanResult(
    result: MoveResult,
    promotion: PieceType?,
): Game =
    when (result) {
        is MoveResult.Success -> result.game
        is MoveResult.Promotion -> (result.onPieceSelection(promotion ?: PieceType.Queen) as MoveResult.Success).game
    }
