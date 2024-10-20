package com.bentrengrove.chess.engine

import androidx.annotation.DrawableRes
import com.bentrengrove.chess.R

sealed class PieceType(val value: Int) {
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

    fun other(): PieceColor {
        return if (this == White) Black else White
    }
}

private fun pieceTypeFromId(id: String): Pair<PieceType, PieceColor> {
    val chars = id.toCharArray()
    if (chars.size != 3) throw IllegalStateException("Piece id should be 3 characters")
    val pieceColor = when (chars[0]) {
        'W' -> PieceColor.White
        'B' -> PieceColor.Black
        else -> throw IllegalStateException("First character should be W or B")
    }
    val pieceType = when (chars[1]) {
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
data class Piece(val id: String, val type: PieceType, val color: PieceColor) {
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
    fun imageResource(): Int {
        return when (type) {
            PieceType.Pawn -> if (color is PieceColor.White) R.drawable.w_pawn_2x_ns else R.drawable.b_pawn_2x_ns
            PieceType.Knight -> if (color is PieceColor.White) R.drawable.w_knight_2x_ns else R.drawable.b_knight_2x_ns
            PieceType.Bishop -> if (color is PieceColor.White) R.drawable.w_bishop_2x_ns else R.drawable.b_bishop_2x_ns
            PieceType.Rook -> if (color is PieceColor.White) R.drawable.w_rook_2x_ns else R.drawable.b_rook_2x_ns
            PieceType.Queen -> if (color is PieceColor.White) R.drawable.w_queen_2x_ns else R.drawable.b_queen_2x_ns
            PieceType.King -> if (color is PieceColor.White) R.drawable.w_king_2x_ns else R.drawable.b_king_2x_ns
        }
    }
}

data class Delta(val x: Int, val y: Int)
data class Position(val x: Int, val y: Int) {
    operator fun plus(other: Position): Delta {
        return Delta(this.x + other.x, this.y + other.y)
    }

    operator fun minus(other: Position): Delta {
        return Delta(this.x - other.x, this.y - other.y)
    }

    operator fun plus(other: Delta): Position {
        return Position(this.x + other.x, this.y + other.y)
    }
}

private val INITIAL_BOARD = listOf(
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

data class Board(val pieces: List<List<Piece?>> = INITIAL_BOARD) {
    companion object {
        private val ALL_POSITIONS = (0 until 8).flatMap { y ->
            (0 until 8).map { x -> Position(x, y) }
        }

        fun fromHistory(history: List<Move>): Board {
            var board = Board()
            history.forEach {
                board = board.movePiece(it.from, it.to)
            }

            return board
        }
    }

    val allPositions = ALL_POSITIONS
    val allPieces: List<Pair<Position, Piece>> = allPositions.mapNotNull { position -> pieces[position.y][position.x]?.let { position to it } }

    fun pieceAt(position: Position): Piece? {
        return pieces.getOrNull(position.y)?.getOrNull(position.x)
    }

    fun movePiece(from: Position, to: Position): Board {
        val piece = pieceAt(from)
        val newPieces = pieces.map { it.toMutableList() }.toMutableList()

        newPieces[to.y][to.x] = piece
        newPieces[from.y][from.x] = null

        return Board(newPieces.map { it.toList() }.toList())
    }

    fun firstPosition(where: (Piece) -> Boolean): Position? {
        return allPieces.firstOrNull { where(it.second) }?.first
    }

    fun promotePiece(at: Position, to: PieceType): Board {
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
