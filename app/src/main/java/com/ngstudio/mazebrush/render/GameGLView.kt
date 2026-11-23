package com.ngstudio.mazebrush.render

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView

/** OpenGL ES 3.0 surface hosting [GameRenderer]. Touch input is handled by the Compose layer above it. */
@SuppressLint("ViewConstructor")
class GameGLView(context: Context, val renderer: GameRenderer) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
