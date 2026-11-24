package com.ngstudio.maze.tools

import java.io.File

/**
 *   ./gradlew :tools:run --args="audio"  -> app/src/main/res/raw/sfx_*.wav + music_*.wav
 *   ./gradlew :tools:run --args="store"  -> docs/store/icon_512.png + feature_graphic.png (+ legacy mipmaps)
 *   ./gradlew :tools:run --args="surfaces" -> res/drawable-nodpi/surface_N.jpg (menu backgrounds, one per world)
 *   ./gradlew :tools:run --args="scenes [from to]" -> res/drawable-nodpi/backdrop_10..99.jpg (chapters 11..100, SceneArt)
 *   ./gradlew :tools:run --args="backdrops" -> res/drawable-nodpi/backdrop_N.jpg (chapter 2..10 backgrounds, ThemeArt)
 */
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "audio" -> { println("Synthesizing audio..."); AudioGen.run(File("app/src/main/res/raw")) }
        "store" -> { println("Rendering store graphics..."); StoreArt.run(File("docs/store")) }
        "surfaces" -> { println("Rendering menu surfaces..."); MaterialArt.run(File("app/src/main/res/drawable-nodpi")) }
        "backdrops" -> { println("Rendering chapter backdrops..."); ThemeArt.run(File("app/src/main/res/drawable-nodpi")) }
        "scenes" -> { println("Rendering chapter 11..100 scenes..."); SceneArt.run(File("app/src/main/res/drawable-nodpi"), args.getOrNull(1)?.toInt() ?: 11, args.getOrNull(2)?.toInt() ?: 100) }
        "surface-preview" -> MaterialArt.preview(File(args.getOrElse(1) { "docs/qa/surface_preview.png" }))
        else -> println("usage: audio | store | surfaces | backdrops | scenes [from to] | surface-preview [out.png]")
    }
}
