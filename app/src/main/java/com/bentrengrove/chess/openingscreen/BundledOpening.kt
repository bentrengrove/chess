package com.bentrengrove.chess.openingscreen

import android.content.res.AssetManager
import com.bentrengrove.chess.engine.Opening

/** An opening bundled in `assets/openings/`, identified by its file name without the extension. */
data class BundledOpening(
    val id: String,
    val opening: Opening,
)

private const val OPENINGS_DIR = "openings"

/** Loads and parses every `.pgn` in `assets/openings/`, sorted by opening name. */
fun loadBundledOpenings(assets: AssetManager): List<BundledOpening> =
    assets
        .list(OPENINGS_DIR)
        .orEmpty()
        .filter { it.endsWith(".pgn") }
        .map { fileName ->
            val pgn = assets.open("$OPENINGS_DIR/$fileName").bufferedReader().use { it.readText() }
            BundledOpening(id = fileName.removeSuffix(".pgn"), opening = Opening.fromPgn(pgn))
        }.sortedBy { it.opening.name }
