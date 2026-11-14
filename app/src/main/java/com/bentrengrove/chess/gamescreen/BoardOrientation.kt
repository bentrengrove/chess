package com.bentrengrove.chess.gamescreen

import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.Position

/**
 * Maps between the cell drawn at ([column], [row]) on screen, counted from the top-left, and the
 * board [Position] it shows when [orientation] is the side at the bottom of the board.
 *
 * With White at the bottom the screen matches engine coordinates (a8 top-left). With Black at the
 * bottom the board is rotated 180 degrees, so h1 is top-left and a8 bottom-right.
 */
fun positionForCell(
    column: Int,
    row: Int,
    orientation: PieceColor,
): Position =
    when (orientation) {
        PieceColor.White -> Position(column, row)
        PieceColor.Black -> Position(7 - column, 7 - row)
    }

/**
 * The on-screen (column, row) cell, counted from the top-left, where [position] is drawn when
 * [orientation] is the side at the bottom. The inverse of [positionForCell].
 */
fun cellForPosition(
    position: Position,
    orientation: PieceColor,
): Pair<Int, Int> =
    when (orientation) {
        PieceColor.White -> position.x to position.y
        PieceColor.Black -> (7 - position.x) to (7 - position.y)
    }
