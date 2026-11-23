package com.ngstudio.maze.core

/**
 * Levels 1-10: hand-picked tutorial levels. Each one teaches one idea, every move can be undone by the
 * opposite swipe (no one-way moves), and difficulty rises gently (D 0.00 -> 0.25).
 * They go through the same validator and solver as the generated levels (see HandmadeLevelsTest).
 * Levels 6-10 were picked from validated generator candidates (`:levelgen:run --args="explore ..."`).
 */
object HandmadeLevels {

    val ASCII: List<String> = listOf(
        // 1 - One swipe. Par 1 (R).
        """
        S....
        """,
        // 2 - Turning a corner. Par 2 (R D).
        """
        S..
        ##.
        ##.
        """,
        // 3 - Ring: any order works. Par 4.
        """
        S.....
        .####.
        ......
        """,
        // 4 - Snake: every swipe is forced. Par 5.
        """
        S#...
        .#.#.
        ...#.
        """,
        // 5 - Spiral inward. Par 5.
        """
        S....
        ####.
        ...#.
        .###.
        .....
        """,
        // 6 - Dead end: go up, come back down. Par 6.
        """
        .#####
        .#####
        S.....
        #####.
        .####.
        .####.
        ......
        """,
        // 7 - First real choice between two routes. Par 7.
        """
        ....#S
        .##.#.
        .##...
        .#.###
        ...###
        """,
        // 8 - Big ring with a tail. Par 7.
        """
        ......
        .####.
        .####.
        .####.
        .S#...
        ###.##
        ###...
        """,
        // 9 - Zig-zag with a branch. Par 8.
        """
        #.....
        S.###.
        ##....
        ##.###
        ##...#
        ####.#
        ####.#
        ####.#
        """,
        // 10 - Milestone: long corridors and a loop. Par 9.
        """
        ...####
        ##.####
        ##S....
        ######.
        ....##.
        .#####.
        .#####.
        .......
        """,
    )

    fun all(): List<Level> = ASCII.map { Level.parse(it) }
}
