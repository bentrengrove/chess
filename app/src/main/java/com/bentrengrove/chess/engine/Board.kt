package com.bentrengrove.chess.engine

import androidx.annotation.DrawableRes
import com.bentrengrove.chess.R
import kotlin.math.abs

sealed class PieceType(
    val value: Int,
) {
    object Pawn : PieceType(1)

    object Knight : PieceType(3)

    object Bishop : PieceType(3)

    object Rook : PieceType(5)

    object Queen : PieceType(8)

    object King : PieceType(0)
}

sealed class PieceColor {
    object White : PieceColor()

    object Black : PieceColor()

    fun other(): PieceColor = if (this == White) Black else White
}

private fun pieceTypeFromId(id: String): Pair<PieceType, PieceColor> {
    val chars = id.toCharArray()
    if (chars.size != 3) throw IllegalStateException("Piece id should be 3 characters")
    val pieceColor =
        when (chars[0]) {
            'W' -> PieceColor.White
            'B' -> PieceColor.Black
            else -> throw IllegalStateException("First character should be W or B")
        }
    val pieceType =
        when (chars[1]) {
            'P' -> PieceType.Pawn
            'N' -> PieceType.Knight
            'B' -> PieceType.Bishop
            'R' -> PieceType.Rook
            'Q' -> PieceType.Queen
            'K' -> PieceType.King
            else -> throw IllegalStateException("Second character should be a piece type")
        }
    return pieceType to pieceColor
}

data class Piece(
    val id: String,
    val type: PieceType,
    val color: PieceColor,
) {
    companion object {
        fun pieceOrNullFromString(id: String?): Piece? {
            val id = id ?: return null
            val types = pieceTypeFromId(id)
            return Piece(id, types.first, types.second)
        }

        fun pieceFromString(id: String): Piece {
            val types = pieceTypeFromId(id)
            return Piece(id, types.first, types.second)
        }
    }

    @DrawableRes
    fun imageResource(): Int =
        when (type) {
            PieceType.Pawn -> if (color is PieceColor.White) R.drawable.w_pawn_2x_ns else R.drawable.b_pawn_2x_ns
            PieceType.Knight -> if (color is PieceColor.White) R.drawable.w_knight_2x_ns else R.drawable.b_knight_2x_ns
            PieceType.Bishop -> if (color is PieceColor.White) R.drawable.w_bishop_2x_ns else R.drawable.b_bishop_2x_ns
            PieceType.Rook -> if (color is PieceColor.White) R.drawable.w_rook_2x_ns else R.drawable.b_rook_2x_ns
            PieceType.Queen -> if (color is PieceColor.White) R.drawable.w_queen_2x_ns else R.drawable.b_queen_2x_ns
            PieceType.King -> if (color is PieceColor.White) R.drawable.w_king_2x_ns else R.drawable.b_king_2x_ns
        }
}

data class Delta(
    val x: Int,
    val y: Int,
)

data class Position(
    val x: Int,
    val y: Int,
) {
    operator fun plus(other: Position): Delta = Delta(this.x + other.x, this.y + other.y)

    operator fun minus(other: Position): Delta = Delta(this.x - other.x, this.y - other.y)

    operator fun plus(other: Delta): Position = Position(this.x + other.x, this.y + other.y)

    fun toAlgebraic(): String = "${'a' + x}${8 - y}"

    companion object {
        /** Parses a FEN/SAN algebraic square ("e4") into engine [Position] coordinates. */
        fun fromAlgebraic(square: String): Position {
            require(square.length == 2) { "'$square' is not a valid algebraic square" }
            val file = square[0] - 'a'
            val rank = square[1] - '1'
            require(file in 0..7 && rank in 0..7) { "'$square' is not a valid algebraic square" }
            return Position(file, 7 - rank)
        }
    }
}

private fun pieceTypeFromFenChar(c: Char): PieceType =
    when (c.uppercaseChar()) {
        'P' -> PieceType.Pawn
        'N' -> PieceType.Knight
        'B' -> PieceType.Bishop
        'R' -> PieceType.Rook
        'Q' -> PieceType.Queen
        'K' -> PieceType.King
        else -> throw IllegalArgumentException("'$c' is not a valid FEN piece character")
    }

private fun pieceToFenChar(piece: Piece): Char {
    val letter =
        when (piece.type) {
            PieceType.Pawn -> 'P'
            PieceType.Knight -> 'N'
            PieceType.Bishop -> 'B'
            PieceType.Rook -> 'R'
            PieceType.Queen -> 'Q'
            PieceType.King -> 'K'
        }
    return if (piece.color == PieceColor.White) letter else letter.lowercaseChar()
}

private val INITIAL_BOARD =
    listOf(
        listOf("BR0", "BN1", "BB2", "BQ3", "BK4", "BB5", "BN6", "BR7").map { Piece.pieceOrNullFromString(it) },
        listOf("BP0", "BP1", "BP2", "BP3", "BP4", "BP5", "BP6", "BP7").map { Piece.pieceOrNullFromString(it) },
        listOf(null, null, null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
        listOf(null, null, null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
        listOf(null, null, null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
        listOf(null, null, null, null, null, null, null, null).map { Piece.pieceOrNullFromString(it) },
        listOf("WP0", "WP1", "WP2", "WP3", "WP4", "WP5", "WP6", "WP7").map { Piece.pieceOrNullFromString(it) },
        listOf("WR0", "WN1", "WB2", "WQ3", "WK4", "WB5", "WN6", "WR7").map { Piece.pieceOrNullFromString(it) },
    )
val STARTING_PIECES = INITIAL_BOARD.flatten().filterNotNull()

data class Board(
    val pieces: List<List<Piece?>> = INITIAL_BOARD,
) {
    companion object {
        private val ALL_POSITIONS =
            (0 until 8).flatMap { y ->
                (0 until 8).map { x -> Position(x, y) }
            }

        /**
         * Only valid for a game that started from the standard position - this always replays
         * from `Board()`, so it reconstructs the wrong board for a game loaded via
         * [Game.fromFen] with a non-standard starting placement. [Game.fromPgn] loading a
         * `[FEN]` tag hits this too, since `Game.history` alone can't recover an arbitrary
         * starting board.
         */
        fun fromHistory(history: List<Move>): Board {
            var board = Board()
            history.forEach { move ->
                val piece = board.pieceAt(move.from)
                val delta = move.to - move.from
                board =
                    when {
                        // Castling: the recorded move is only the king's two-square hop: the
                        // rook has to jump alongside it, mirroring Game.move()'s special-case.
                        piece?.type == PieceType.King && abs(delta.x) > 1 -> {
                            val kingSide = delta.x > 0
                            val rookFrom = Position(if (kingSide) 7 else 0, move.to.y)
                            val rookTo = Position(if (kingSide) 5 else 3, move.to.y)
                            board.movePiece(rookFrom, rookTo).movePiece(move.from, move.to)
                        }

                        // En passant: a pawn moving diagonally onto an empty square can only
                        // be an en passant capture (a normal diagonal pawn move always lands
                        // on an occupied square) - the captured pawn sits beside the
                        // destination, on the mover's starting rank.
                        piece?.type == PieceType.Pawn && delta.x != 0 && board.pieceAt(move.to) == null -> {
                            board.removePiece(Position(move.to.x, move.from.y)).movePiece(move.from, move.to)
                        }

                        else -> {
                            board.movePiece(move.from, move.to)
                        }
                    }
                if (move.promotion != null) {
                    board = board.promotePiece(move.to, move.promotion)
                }
            }

            return board
        }

        /** Parses the piece-placement field of a FEN string (ranks 8-1, separated by "/"). */
        fun fromFen(placement: String): Board {
            val ranks = placement.split("/")
            require(ranks.size == 8) { "FEN piece placement '$placement' must have 8 ranks" }

            val grid =
                ranks.map { rank ->
                    val squares = mutableListOf<Piece?>()
                    var file = 0
                    rank.forEach { c ->
                        if (c.isDigit()) {
                            repeat(c.digitToInt()) {
                                squares.add(null)
                                file++
                            }
                        } else {
                            val color = if (c.isUpperCase()) PieceColor.White else PieceColor.Black
                            val type = pieceTypeFromFenChar(c)
                            val colorChar = if (color == PieceColor.White) "W" else "B"
                            squares.add(Piece("$colorChar${c.uppercaseChar()}$file", type, color))
                            file++
                        }
                    }
                    require(squares.size == 8) { "FEN rank '$rank' does not describe 8 squares" }
                    squares.toList()
                }
            return Board(grid)
        }
    }

    /** The piece-placement field of this board's FEN representation (ranks 8-1). */
    val fen: String
        get() =
            pieces.joinToString("/") { rank ->
                val sb = StringBuilder()
                var emptyRun = 0
                rank.forEach { piece ->
                    if (piece == null) {
                        emptyRun++
                    } else {
                        if (emptyRun > 0) {
                            sb.append(emptyRun)
                            emptyRun = 0
                        }
                        sb.append(pieceToFenChar(piece))
                    }
                }
                if (emptyRun > 0) sb.append(emptyRun)
                sb.toString()
            }

    val allPositions = ALL_POSITIONS
    val allPieces: List<Pair<Position, Piece>> =
        allPositions.mapNotNull { position ->
            pieces[position.y][position.x]?.let { position to it }
        }

    fun pieceAt(position: Position): Piece? = pieces.getOrNull(position.y)?.getOrNull(position.x)

    fun movePiece(
        from: Position,
        to: Position,
    ): Board {
        val piece = pieceAt(from)
        val newPieces = pieces.map { it.toMutableList() }.toMutableList()

        newPieces[to.y][to.x] = piece
        newPieces[from.y][from.x] = null

        return Board(newPieces.map { it.toList() }.toList())
    }

    fun firstPosition(where: (Piece) -> Boolean): Position? = allPieces.firstOrNull { where(it.second) }?.first

    fun promotePiece(
        at: Position,
        to: PieceType,
    ): Board {
        val oldPiece = pieceAt(at) ?: return this
        val newPieces = pieces.map { it.toMutableList() }.toMutableList()
        newPieces[at.y][at.x] = oldPiece.copy(type = to)

        return Board(newPieces.map { it.toList() }.toList())
    }

    fun removePiece(at: Position): Board {
        val oldPiece = pieceAt(at) ?: return this
        val newPieces = pieces.map { it.toMutableList() }.toMutableList()
        newPieces[at.y][at.x] = null

        return Board(newPieces.map { it.toList() }.toList())
    }
}
