package com.bentrengrove.chess

import kotlin.math.abs

data class Move(val from: Position, val to: Position)

enum class GameState {
    IDLE, CHECK, CHECKMATE, STALEMATE
}

data class Game(val board: Board = Board(), val history: List<Move> = listOf()) {
    val gameState: GameState = GameState.IDLE

    val turn: PieceColor
        get() = history.lastOrNull()?.let { board.pieceAt(it.to)?.color?.other() } ?: PieceColor.White

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
        }

        when (piece.type) {
            is PieceType.Pawn -> {
                if (delta.x != 0) {
                    return false
                }
                return when (piece.color) {
                    is PieceColor.White -> {
                        delta.y == -1
                    }
                    is PieceColor.Black -> {
                        delta.y == 1
                    }
                }
            }
            is PieceType.Rook -> {
                return (delta.x == 0 || delta.y == 0)
            }
            is PieceType.Bishop -> {
                return abs(delta.x) == abs(delta.y)
            }
            is PieceType.Queen -> {
                return (delta.x == 0 || delta.y == 0 || abs(delta.x) == abs(delta.y))
            }
            is PieceType.King -> {
                return abs(delta.x) <= 1 && abs(delta.y) <= 1
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

    fun doMove(from: Position, to: Position): Game {
        return Game(board = board.movePiece(from, to), history = history + listOf(Move(from, to)))
    }
}