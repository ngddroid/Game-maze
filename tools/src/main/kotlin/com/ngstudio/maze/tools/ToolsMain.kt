package com.ngstudio.maze.tools

import java.io.File

/**
 *   ./gradlew :tools:run --args="audio"  -> app/src/main/res/raw/sfx_*.wav + music_*.wav
 *   ./gradlew :tools:run --args="store"  -> docs/store/icon_512.png + feature_graphic.png (+ legacy mipmaps)
 *   ./gradlew :tools:run --args="surfaces" -> res/drawable-nodpi/surface_N.jpg (menu backgrounds, one per world)
 */
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "audio" -> { println("Synthesizing audio..."); AudioGen.run(File("app/src/main/res/raw")) }
        "store" -> { println("Rendering store graphics..."); StoreArt.run(File("docs/store")) }
        "surfaces" -> { println("Rendering menu surfaces..."); MaterialArt.run(File("app/src/main/res/drawable-nodpi")) }
        "surface-preview" -> MaterialArt.preview(File(args.getOrElse(1) { "docs/qa/surface_preview.png" }))
        else -> println("usage: audio | store | surfaces | surface-preview [out.png]")
    }
}
