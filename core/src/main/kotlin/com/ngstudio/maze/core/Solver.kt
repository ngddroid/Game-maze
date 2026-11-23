package com.ngstudio.maze.core

import kotlin.random.Random

class Solution(val moves: List<Dir>, val optimal: Boolean)

/**
 * Finds a short move list that paints every floor cell.
 *
 * Cells always painted by exactly the same set of swipes are merged into one "group", so the search
 * state is (resting node, painted groups). Small levels get an exact BFS (true minimum). Larger levels
 * use randomized greedy restarts plus a beam search; the best result is used as par.
 */
object Solver {

    private class Problem(
        val a: Analysis,
        val groupCount: Int,
        val words: Int,
        /** edgeMask[node * 4 + dir] -> group bitset words (null if no edge). */
        val edgeMask: Array<LongArray?>,
        val full: LongArray,
    )

    private fun build(a: Analysis): Problem {
        val level = a.level
        // Index every edge, then give each required cell a signature = the edges that paint it.
        val edgeIds = HashMap<Edge, Int>()
        val coverers = HashMap<Int, MutableList<Int>>()
        var id = 0
        for (n in a.nodes.indices) for (e in a.edges[n]) {
            if (e == null) continue
            val eid = id++
            edgeIds[e] = eid
            for (c in e.cells) if (c != level.start) coverers.getOrPut(c) { ArrayList(2) }.add(eid)
        }
        val groupOf = HashMap<List<Int>, Int>()
        val cellGroup = HashMap<Int, Int>()
        for ((cell, list) in coverers) {
            val sig = list.distinct().sorted()
            cellGroup[cell] = groupOf.getOrPut(sig) { groupOf.size }
        }
        val g = groupOf.size
        val words = maxOf(1, (g + 63) / 64)
        val edgeMask = arrayOfNulls<LongArray>(a.nodes.size * 4)
        for (n in a.nodes.indices) for (e in a.edges[n]) {
            if (e == null) continue
            val m = LongArray(words)
            for (c in e.cells) {
                val gi = cellGroup[c] ?: continue
                m[gi ushr 6] = m[gi ushr 6] or (1L shl (gi and 63))
            }
            edgeMask[n * 4 + e.dir.ordinal] = m
        }
        val full = LongArray(words)
        for (gi in 0 until g) full[gi ushr 6] = full[gi ushr 6] or (1L shl (gi and 63))
        return Problem(a, g, words, edgeMask, full)
    }

    /**
     * @param exactMaxGroups exact BFS is only tried when the level has at most this many cell groups
     *        (keeps bulk generation fast; tests use a larger value).
     */
    fun solve(
        level: Level,
        a: Analysis = Analyzer.analyze(level),
        seed: Long = 1L,
        exactStateLimit: Int = 400_000,
        exactMaxGroups: Int = 40,
    ): Solution? {
        if (!a.valid) return null
        val p = build(a)
        if (p.groupCount == 0) return Solution(emptyList(), true)
        val nodeBits = 32 - Integer.numberOfLeadingZeros(a.nodes.size)
        if (p.groupCount <= exactMaxGroups && p.groupCount + nodeBits <= 62) {
            exact(p, nodeBits, exactStateLimit)?.let { return Solution(it, true) }
        }
        val rng = Random(seed)
        var best: List<Dir>? = null
        repeat(24) { r ->
            val s = greedy(p, rng, randomize = r > 0)
            if (s != null && (best == null || s.size < best!!.size)) best = s
        }
        val beamResult = beam(p, width = 260, maxDepth = (best?.size ?: 200))
        if (beamResult != null && (best == null || beamResult.size < best!!.size)) best = beamResult
        return best?.let { Solution(it, false) }
    }

    // ---------------------------------------------------------------- exact BFS (single-long keys)

    private fun exact(p: Problem, nodeBits: Int, limit: Int): List<Dir>? {
        val g = p.groupCount
        val fullMask = p.full[0]
        val maskOf = LongArray(p.edgeMask.size) { p.edgeMask[it]?.get(0) ?: -1L }
        val map = LongIntMap(1 shl 16)
        var keys = LongArray(1 shl 14)
        var parent = IntArray(1 shl 14)
        var move = ByteArray(1 shl 14)
        var count = 0
        fun add(key: Long, par: Int, mv: Int): Int {
            if (count == keys.size) {
                keys = keys.copyOf(count * 2); parent = parent.copyOf(count * 2); move = move.copyOf(count * 2)
            }
            keys[count] = key; parent[count] = par; move[count] = mv.toByte()
            map.put(key, count)
            return count++
        }
        add(0L, -1, 0) // node 0, empty mask
        var head = 0
        while (head < count) {
            if (count > limit) return null
            val key = keys[head]
            val node = (key ushr g).toInt()
            val mask = key and ((1L shl g) - 1)
            for (d in 0 until 4) {
                val em = maskOf[node * 4 + d]
                if (em == -1L) continue
                val to = p.a.edges[node][d]!!.to
                val nm = mask or em
                val nk = (to.toLong() shl g) or nm
                if (map.get(nk) >= 0) continue
                val idx = add(nk, head, d)
                if (nm == fullMask) {
                    val out = ArrayList<Dir>()
                    var i = idx
                    while (parent[i] >= 0) { out.add(Dir.of(move[i].toInt())); i = parent[i] }
                    out.reverse()
                    return out
                }
            }
            head++
        }
        return null
    }

    // ---------------------------------------------------------------- greedy restarts

    private fun greedy(p: Problem, rng: Random, randomize: Boolean): List<Dir>? {
        val a = p.a
        val n = a.nodes.size
        val mask = LongArray(p.words)
        var node = 0
        val moves = ArrayList<Dir>()
        val dist = IntArray(n)
        val prevNode = IntArray(n)
        val prevDir = IntArray(n)
        val queue = IntArray(n)
        var guard = 0
        while (!covers(mask, p.full)) {
            if (guard++ > 4 * n + 400) return null
            // BFS over resting positions from the current one.
            dist.fill(-1)
            dist[node] = 0; prevNode[node] = -1
            var qh = 0; var qt = 0
            queue[qt++] = node
            var bestScore = -1.0; var bestNode = -1; var bestDir = -1
            while (qh < qt) {
                val u = queue[qh++]
                for (d in 0 until 4) {
                    val em = p.edgeMask[u * 4 + d] ?: continue
                    val gain = gain(mask, em)
                    if (gain > 0) {
                        var score = gain.toDouble() / (dist[u] + 1)
                        if (randomize) score *= 0.75 + rng.nextDouble() * 0.5
                        if (score > bestScore) { bestScore = score; bestNode = u; bestDir = d }
                    }
                    val v = a.edges[u][d]!!.to
                    if (dist[v] < 0) { dist[v] = dist[u] + 1; prevNode[v] = u; prevDir[v] = d; queue[qt++] = v }
                }
            }
            if (bestNode < 0) return null
            val path = ArrayList<Dir>()
            var v = bestNode
            while (v != node) { path.add(Dir.of(prevDir[v])); v = prevNode[v] }
            path.reverse()
            for (d in path) {
                or(mask, p.edgeMask[node * 4 + d.ordinal]!!)
                node = a.edges[node][d.ordinal]!!.to
                moves.add(d)
            }
            or(mask, p.edgeMask[node * 4 + bestDir]!!)
            node = a.edges[node][bestDir]!!.to
            moves.add(Dir.of(bestDir))
        }
        return moves
    }

    // ---------------------------------------------------------------- beam search

    private class BeamState(val node: Int, val mask: LongArray, val bits: Int, val parent: BeamState?, val dir: Int)

    private fun beam(p: Problem, width: Int, maxDepth: Int): List<Dir>? {
        var layer = listOf(BeamState(0, LongArray(p.words), 0, null, -1))
        val seen = HashSet<MaskKey>()
        seen.add(MaskKey(0, layer[0].mask))
        for (depth in 1..maxDepth) {
            val next = ArrayList<BeamState>(layer.size * 4)
            for (s in layer) for (d in 0 until 4) {
                val em = p.edgeMask[s.node * 4 + d] ?: continue
                val to = p.a.edges[s.node][d]!!.to
                val nm = s.mask.copyOf()
                or(nm, em)
                if (!seen.add(MaskKey(to, nm))) continue
                val ns = BeamState(to, nm, popcount(nm), s, d)
                if (covers(nm, p.full)) {
                    val out = ArrayList<Dir>()
                    var c: BeamState? = ns
                    while (c != null && c.dir >= 0) { out.add(Dir.of(c.dir)); c = c.parent }
                    out.reverse()
                    return out
                }
                next.add(ns)
            }
            if (next.isEmpty()) return null
            next.sortByDescending { it.bits }
            layer = if (next.size > width) next.subList(0, width) else next
        }
        return null
    }

    private class MaskKey(val node: Int, val mask: LongArray) {
        override fun hashCode(): Int = node * 31 + mask.contentHashCode()
        override fun equals(other: Any?): Boolean = other is MaskKey && other.node == node && other.mask.contentEquals(mask)
    }

    private fun covers(mask: LongArray, full: LongArray): Boolean {
        for (i in mask.indices) if (mask[i] != full[i]) return false
        return true
    }

    private fun gain(mask: LongArray, add: LongArray): Int {
        var g = 0
        for (i in mask.indices) g += java.lang.Long.bitCount(add[i] and mask[i].inv())
        return g
    }

    private fun or(mask: LongArray, add: LongArray) {
        for (i in mask.indices) mask[i] = mask[i] or add[i]
    }

    private fun popcount(m: LongArray): Int = m.sumOf { java.lang.Long.bitCount(it) }
}

/** Minimal open-addressing Long -> Int map (no boxing) for the exact search. */
internal class LongIntMap(initial: Int) {
    private var keys = LongArray(initial)
    private var vals = IntArray(initial)
    private var used = BooleanArray(initial)
    private var size = 0

    fun get(k: Long): Int {
        var i = slot(k, keys.size)
        while (used[i]) {
            if (keys[i] == k) return vals[i]
            i = (i + 1) and (keys.size - 1)
        }
        return -1
    }

    fun put(k: Long, v: Int) {
        if ((size + 1) * 2 > keys.size) grow()
        var i = slot(k, keys.size)
        while (used[i]) {
            if (keys[i] == k) { vals[i] = v; return }
            i = (i + 1) and (keys.size - 1)
        }
        used[i] = true; keys[i] = k; vals[i] = v; size++
    }

    private fun slot(k: Long, cap: Int): Int {
        var h = k * -0x61c8864680b583ebL
        h = h xor (h ushr 29)
        return (h.toInt()) and (cap - 1)
    }

    private fun grow() {
        val ok = keys; val ov = vals; val ou = used
        keys = LongArray(ok.size * 2); vals = IntArray(ok.size * 2); used = BooleanArray(ok.size * 2); size = 0
        for (i in ok.indices) if (ou[i]) put(ok[i], ov[i])
    }
}
