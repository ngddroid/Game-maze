package com.ngstudio.maze.levelgen

import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Mechanic
import com.ngstudio.maze.core.Tile

/** docs/levels_report.html: difficulty curve, per-bucket stats and drawn previews. Open it in a browser. */
object Report {

    fun html(levels: List<Level>, targets: List<Double>, attempts: List<Int>, daily: List<Level>): String = buildString {
        append("<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>")
        append("<title>Maze Brush levels</title><style>")
        append("body{font-family:system-ui,sans-serif;margin:24px;background:#fff7fb;color:#2d2140}h1{color:#c2187a}")
        append("table{border-collapse:collapse;font-size:13px}td,th{border:1px solid #e6d5ea;padding:4px 8px;text-align:right}")
        append(".grid{display:flex;flex-wrap:wrap;gap:16px}.card{background:#fff;border-radius:12px;padding:10px;box-shadow:0 2px 8px #0001}")
        append(".card p{margin:4px 0 0;font-size:12px}</style></head><body>")
        append("<h1>Maze Brush &mdash; ${levels.size} levels + ${daily.size} daily</h1>")
        append("<p>Every level passed the validator (no traps, every tile paintable, no endless loops) and its stored solution was replayed.</p>")

        // ---- difficulty chart
        val w = 1000.0; val h = 320.0
        append("<h2>Difficulty: target (grey) vs actual (pink), 10-level rolling average (purple)</h2>")
        append("<svg viewBox='0 0 $w $h' width='100%' style='background:#fff;border-radius:12px'>")
        fun px(i: Int) = 30 + (w - 40) * i / (levels.size - 1)
        fun py(v: Double) = h - 20 - (h - 40) * v
        for (g in 0..10) append("<line x1='30' x2='${w - 10}' y1='${py(g / 10.0)}' y2='${py(g / 10.0)}' stroke='#eee'/>")
        fun poly(values: List<Double>, color: String, width: Double) {
            append("<polyline fill='none' stroke='$color' stroke-width='$width' points='")
            values.forEachIndexed { i, v -> append("%.1f,%.1f ".format(px(i), py(v))) }
            append("'/>")
        }
        poly(levels.map { it.difficulty.toDouble() }, "#ff7ac0", 0.6)
        poly(targets, "#999", 0.8)
        val rolling = levels.indices.map { i -> levels.subList(maxOf(0, i - 9), i + 1).map { it.difficulty.toDouble() }.average() }
        poly(rolling, "#7a3cff", 1.6)
        append("</svg>")

        // ---- bucket table
        append("<h2>Per 250 levels</h2><table><tr><th>Levels</th><th>Target</th><th>Actual D</th><th>Par</th><th>Floor</th><th>Max size</th>")
        Mechanic.values().forEach { append("<th>${it.name.lowercase()}</th>") }
        append("<th>Avg attempts</th></tr>")
        for (b in levels.indices step 250) {
            val e = minOf(levels.size, b + 250)
            val sub = levels.subList(b, e)
            append("<tr><td>${b + 1}-$e</td><td>%.3f</td><td>%.3f</td><td>%.1f</td><td>%.1f</td><td>%dx%d</td>".format(
                targets.subList(b, e).average(), sub.map { it.difficulty.toDouble() }.average(), sub.map { it.par }.average(),
                sub.map { it.floorCount }.average(), sub.maxOf { it.width }, sub.maxOf { it.height }))
            Mechanic.values().forEach { m -> append("<td>${sub.count { m in it.mechanics() }}</td>") }
            append("<td>%.1f</td></tr>".format(attempts.subList(b, e).average()))
        }
        append("</table>")

        // ---- previews
        val picks = (1..10).toList() + listOf(11, 20, 31, 50, 81, 100, 151, 200, 251, 300, 500, 750, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 4990, 5000)
        append("<h2>Previews</h2><div class='grid'>")
        for (n in picks) if (n <= levels.size) append(card("Level $n", levels[n - 1]))
        append("</div><h2>Daily samples</h2><div class='grid'>")
        for (d in listOf(1, 2, 3, 4, 5, 6, 7)) append(card("Daily $d", daily[d - 1]))
        append("</div></body></html>")
    }

    private fun card(title: String, l: Level): String {
        val c = 14
        val sb = StringBuilder("<div class='card'><svg width='${l.width * c + 8}' height='${l.height * c + 8}'>")
        sb.append("<rect width='100%' height='100%' rx='6' fill='#f3d7c6'/>")
        for (i in 0 until l.size) {
            val x = 4 + l.x(i) * c; val y = 4 + l.y(i) * c
            val t = l.tile(i)
            if (t == Tile.WALL) continue
            val fill = when {
                i == l.start -> "#ff3d9a"
                t == Tile.STOPPER -> "#ffb300"
                Tile.isOneWay(t) -> "#4fc3f7"
                Tile.isDeflector(t) -> "#66bb6a"
                t == Tile.PORTAL -> "#7a3cff"
                else -> "#fff"
            }
            sb.append("<rect x='$x' y='$y' width='${c - 1}' height='${c - 1}' rx='2' fill='$fill'/>")
            if (Tile.isOneWay(t)) {
                val arrow = "^>v<"[t - Tile.ONEWAY_UP]
                sb.append("<text x='${x + 3}' y='${y + 11}' font-size='10'>$arrow</text>")
            }
            if (Tile.isDeflector(t)) sb.append("<text x='${x + 4}' y='${y + 11}' font-size='10'>${if (t == Tile.DEFLECT_SLASH) "/" else "\\"}</text>")
        }
        sb.append("</svg><p><b>$title</b> ${l.width}x${l.height} &middot; par ${l.par} &middot; D %.2f</p>".format(l.difficulty))
        sb.append("<p style='font-family:monospace'>${l.solutionDirs().joinToString("") { it.symbol.toString() }}</p></div>")
        return sb.toString()
    }
}
