package com.ngstudio.maze.core

/**
 * Graph of every resting position reachable from the start, with one edge per swipe that moves the ball.
 *
 * A level is VALID when:
 *  1. no slide loops forever,
 *  2. the graph is strongly connected (from any reachable position you can get back to any other,
 *     so the player can never be trapped), and
 *  3. the cells covered by all edges plus the start cell are exactly all floor cells.
 * (2) + (3) prove the level can always be finished from any state the player can reach: any unpainted
 * cell lies on some edge, and that edge's source is always reachable.
 */
class Analysis(
    val level: Level,
    /** Resting cells; nodes[0] is the start. */
    val nodes: IntArray,
    /** edges[node][dir] (node = index into [nodes]) or null when that swipe does not move the ball. */
    val edges: Array<Array<Edge?>>,
    val loops: Boolean,
    val covered: BooleanArray,
    val uncoveredCount: Int,
    val stronglyConnected: Boolean,
) {
    val valid: Boolean get() = !loops && uncoveredCount == 0 && stronglyConnected
    val nodeCount get() = nodes.size
    val edgeCount get() = edges.sumOf { row -> row.count { it != null } }

    fun nodeOf(cell: Int): Int = nodes.indexOf(cell)

    /** Share of edges whose opposite swipe does not lead straight back (a "costly" move). */
    fun irreversibleRatio(): Double {
        var total = 0; var irrev = 0
        for (n in nodes.indices) for (e in edges[n]) {
            if (e == null) continue
            total++
            val back = edges[e.to][e.dir.opposite.ordinal]
            if (back == null || back.to != n) irrev++
        }
        return if (total == 0) 0.0 else irrev.toDouble() / total
    }

    /** Average number of distinct places a swipe can take you, over all resting positions. */
    fun branching(): Double {
        if (nodes.isEmpty()) return 0.0
        var sum = 0
        for (n in nodes.indices) sum += edges[n].filterNotNull().map { it.to }.distinct().size
        return sum.toDouble() / nodes.size
    }
}

/** [to] is a node index; [cells] are the cells the swipe paints. */
class Edge(val from: Int, val dir: Dir, val to: Int, val cells: IntArray)

object Analyzer {

    fun analyze(level: Level, maxNodes: Int = 4000): Analysis {
        val nodeIndex = HashMap<Int, Int>()
        val nodes = ArrayList<Int>()
        val edgeList = ArrayList<Array<Edge?>>()
        val covered = BooleanArray(level.size)
        covered[level.start] = true
        var loops = false
        nodeIndex[level.start] = 0
        nodes.add(level.start)
        edgeList.add(arrayOfNulls(4))
        var head = 0
        while (head < nodes.size && nodes.size <= maxNodes) {
            val cell = nodes[head]
            for (d in Dir.VALUES) {
                val s = Physics.slide(level, cell, d) ?: continue
                if (s.loops) { loops = true; continue }
                for (c in s.cells) covered[c] = true
                val to = nodeIndex.getOrPut(s.end) {
                    nodes.add(s.end); edgeList.add(arrayOfNulls(4)); nodes.size - 1
                }
                edgeList[head][d.ordinal] = Edge(head, d, to, s.cells)
            }
            head++
        }
        val edges = edgeList.toTypedArray()
        var uncovered = 0
        for (i in 0 until level.size) if (level.isFloor(i) && !covered[i]) uncovered++

        // Strongly connected <=> every node (all reachable from start by construction) can reach the start.
        val reverse = Array(nodes.size) { ArrayList<Int>(4) }
        for (n in nodes.indices) for (e in edges[n]) if (e != null) reverse[e.to].add(n)
        val seen = BooleanArray(nodes.size)
        val stack = ArrayDeque<Int>()
        seen[0] = true; stack.add(0)
        var count = 1
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            for (p in reverse[n]) if (!seen[p]) { seen[p] = true; count++; stack.add(p) }
        }
        val sc = count == nodes.size && nodes.size <= maxNodes
        return Analysis(level, nodes.toIntArray(), edges, loops, covered, uncovered, sc)
    }

    /** Human-readable reasons a level fails validation (empty when valid). */
    fun problems(level: Level): List<String> {
        val a = analyze(level)
        val out = ArrayList<String>()
        if (a.loops) out += "a slide loops forever"
        if (!a.stronglyConnected) out += "player can get trapped (graph not strongly connected)"
        if (a.uncoveredCount > 0) {
            val cells = (0 until level.size).filter { level.isFloor(it) && !a.covered[it] }
                .joinToString { "(${level.x(it)},${level.y(it)})" }
            out += "${a.uncoveredCount} floor cell(s) can never be painted: $cells"
        }
        return out
    }
}
