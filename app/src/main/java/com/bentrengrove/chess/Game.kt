package com.bentrengrove.chess

import kotlin.math.abs

data class Move(val from: Position, val to: Position)

enum class GameState {
    IDLE, CHECK, CHECKMATE, STALEMATE
}

sealed class MoveResult {
    data class Success(val game: Game): MoveResult()
    data class Promotion(val onPieceSelection: (PieceType)->MoveResult): MoveResult()
}

data class Game(val board: Board = Board(), val history: List<Move> = listOf()) {
    val gameState: GameState
        get() {
            val color = turn
            val canMove = allMovesFor(color).find {
                val newBoard = doMove(it.from, it.to)
                (newBoard is MoveResult.Success) && !newBoard.game.kingIsInCheck(color)
            } != null
            if (kingIsInCheck(color)) {
                return if (canMove) GameState.CHECK else GameState.CHECKMATE
            }
            return if (canMove) GameState.IDLE else GameState.STALEMATE
        }

    val turn: PieceColor
        get() = history.lastOrNull()?.let { board.pieceAt(it.to)?.color?.other() } ?: PieceColor.White

    fun allMovesFor(position: Position): Sequence<Move> {
         return board.allPositions.asSequence()
             .map { Move(position, it) }
             .filter { canMove(it.from, it.to) }
    }

    fun allMovesFor(color: PieceColor): Sequence<Move> {
        return board.allPieces.asSequence().mapNotNull { (position, piece) ->
            if (piece.color == color) position else null
        }.flatMap { allMovesFor(it) }
    }

    fun pieceIsThreatenedAt(position: Position): Boolean {
        return board.allPositions.find { canMove(from = it, to = position) } != null
    }

    fun kingIsInCheck(color: PieceColor): Boolean {
        val kingPosition = board.firstPosition { it.type is PieceType.King && it.color == color } ?: return false
        return pieceIsThreatenedAt(kingPosition)
    }

    fun canSelect(position: Position): Boolean {
        return board.pieceAt(position)?.color == turn
    }

    fun canMove(from: Position, to: Position): Boolean {
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
                    Delta(x = -2, y = -1)
                ).contains(delta)
            }
        }
    }

    fun doMove(from: Position, to: Position): MoveResult {
        val oldGame = this.copy()
        val newGame = move(from, to)
        val wasInCheck = newGame.kingIsInCheck(oldGame.turn)

        if (wasInCheck) {
            return MoveResult.Success(oldGame)
        }

        val wasPromoted = newGame.canPromotePieceAt(to)

        if (wasPromoted) {
            return MoveResult.Promotion { promoteTo ->
                MoveResult.Success(newGame.promotePieceAt(to, promoteTo))
            }
        }

        return MoveResult.Success(newGame)
    }

    private fun move(from: Position, to: Position): Game {
        val intermediateBoard = if (board.pieceAt(from)?.type == PieceType.King && abs(to.x - from.x) > 1) {
            val kingSide = (to.x == 6)
            val rookPosition = Position(if (kingSide) 7 else 0, to.y)
            val rookDestination = Position(if (kingSide) 5 else 3, to.y)
            board.movePiece(rookPosition, rookDestination)
        } else if (board.pieceAt(from)?.type == PieceType.Pawn && enPassantTakePermitted(from, to)) {
            board.removePiece(Position(to.x, to.y - (to.y - from.y)))
        } else {
            board
        }
        return Game(board = intermediateBoard.movePiece(from, to), history = history + listOf(Move(from, to)))
    }

    fun movesForPieceAt(position: Position?): List<Position> {
        if (position == null) return emptyList()
        return board.allPositions.filter { canMove(position, it) }
    }

    fun pawnCanTake(from: Position, withDelta: Delta): Boolean {
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

    fun promotePieceAt(position: Position, to: PieceType): Game {
        return Game(board.promotePiece(position, to), this.history)
    }

    fun pieceHasMoved(at: Position): Boolean {
        return history.find { it.from == at } != null
    }

    fun positionIsThreatened(position: Position, by: PieceColor): Boolean {
        return board.allPieces.find { (from, piece) ->
            if (piece.color != by) return@find false
            if (piece.type == PieceType.Pawn) return@find pawnCanTake(from, position - from)
            return canMove(from, position)
        } != null
    }

    fun castlingPermitted(from: Position, to: Position): Boolean {
        val piece = board.pieceAt(from) ?: return false
        if (piece.type != PieceType.King) return false
        val kingsRow = if (piece.color == PieceColor.Black) 0 else 7
        if (!(from.y == kingsRow && to.y == kingsRow && from.x == 4 && listOf(2, 6).contains(to.x))) return false

        val kingPosition = Position(4, kingsRow)
        if (pieceHasMoved(kingPosition)) return false

        val isKingSide = to.x == 6
        val rookPosition = Position(if (isKingSide) 7 else 0, kingsRow)
        if (pieceHasMoved(rookPosition)) return false

        return ((if (isKingSide) 5..6 else 1..3).map { board.pieceAt(Position(it, kingsRow)) }.find { it != null } == null) &&
                ((if (isKingSide) 4..6 else 2..4).map { positionIsThreatened(Position(it, kingsRow ), by = this.turn.other()) }.find { it == true } == null)
    }

    fun enPassantTakePermitted(from: Position, to: Position): Boolean {
        board.pieceAt(from) ?: return false
        if (!pawnCanTake(from, to - from)) return false

        val lastMove = history.lastOrNull() ?: return false
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

    fun valueFor(color: PieceColor): Int {
        return board.allPieces.filter { it.second.color == color }.map { it.second.type.value }.sum()
    }
}

private fun Board.piecesExist(between: Position, and: Position): Boolean {
    val step = Delta(
        x = if(between.x > and.x) -1 else if (between.x < and.x) 1 else 0,
        y = if(between.y > and.y) -1 else if (between.y < and.y) 1 else 0
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