package com.choomhub.app.feature.snaps

import android.content.Context
import android.graphics.Paint

/**
 * Every emoji, by category, from assets/emoji.tsv (built from the MIT-licensed unicode-emoji-json;
 * see assets/emoji-LICENSE.txt). Only emojis this phone can draw are kept, so none show as boxes.
 */
object EmojiData {
    @Volatile private var cached: List<EmojiGroup>? = null

    fun load(context: Context, canDraw: (String) -> Boolean = Paint()::hasGlyph): List<EmojiGroup> = cached ?: synchronized(this) {
        cached ?: parse(context.assets.open("emoji.tsv").bufferedReader().readLines(), canDraw).also { cached = it }
    }

    fun parse(lines: List<String>, canDraw: (String) -> Boolean): List<EmojiGroup> {
        val groups = mutableListOf<EmojiGroup>()
        var name = ""
        var icon = ""
        var emojis = mutableListOf<EmojiEntry>()
        fun flush() {
            if (name.isNotEmpty() && emojis.isNotEmpty()) groups += EmojiGroup(name, icon, emojis)
        }
        for (line in lines) {
            val parts = line.split('\t')
            if (parts.size < 2) continue
            if (parts[0] == "#") {
                flush()
                name = parts[1]
                icon = parts.getOrElse(2) { "" }
                emojis = mutableListOf()
            } else if (canDraw(parts[0])) {
                emojis += EmojiEntry(parts[0], parts[1])
            }
        }
        flush()
        return groups.map { g -> if (g.icon.isEmpty() || !canDraw(g.icon)) g.copy(icon = g.emojis.first().char) else g }
    }
}
