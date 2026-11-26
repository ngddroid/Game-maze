package com.ngstudio.mazebrush.render

/**
 * All GLSL ES 3.0 shaders. Every fragment shader outputs PREMULTIPLIED alpha
 * (blend: ONE, ONE_MINUS_SRC_ALPHA). Coordinates are screen pixels, y down.
 *
 * Look: the whole screen is one real surface (wood, marble, clay, ice, sand, stone; see [SURFACE]) and the
 * maze is carved into it.
 *  - BG: the surface image across the screen.
 *  - BAKE: once per level. The same surface around the maze plus the carved channels: lit bevel on the
 *    carved edge, the visible inner wall faces (camera tilted slightly from the bottom), a dark floor with a
 *    tile grid, ambient occlusion and an inner shadow from the light (top-left). Mode 1 bakes the floor
 *    mask (R), AO (G) and inner shadow (B) used by the paint shader.
 *  - PAINT: per frame. Bright paint with the tile grid; the wet front follows the ball, then dries.
 */
object Shaders {

    const val QUAD_VS = """#version 300 es
layout(location = 0) in vec2 aPos;
uniform vec4 uRect;    // x, y, w, h in pixels
uniform vec2 uCenter;  // scale pivot
uniform vec3 uXform;   // scale, offsetX, offsetY; identity = (1, 0, 0)
uniform vec2 uScreen;
out vec2 vUv;
out vec2 vPix;
void main() {
    vUv = aPos;
    vec2 px = uRect.xy + aPos * uRect.zw;
    vPix = px;
    px = uCenter + (px - uCenter) * uXform.x + uXform.yz;
    gl_Position = vec4(px.x / uScreen.x * 2.0 - 1.0, 1.0 - px.y / uScreen.y * 2.0, 0.0, 1.0);
}
"""

    private const val COMMON = """
float hash(vec2 p) { p = fract(p * vec2(123.34, 456.21)); p += dot(p, p + 45.32); return fract(p.x * p.y); }
float noise(vec2 p) {
    vec2 i = floor(p); vec2 f = fract(p); vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}
float fbm(vec2 p) { float s = 0.0; float a = 0.5; for (int i = 0; i < 4; i++) { s += a * noise(p); p *= 2.03; a *= 0.5; } return s; }
float sdBox(vec2 p, vec2 b, float r) { vec2 q = abs(p) - b + r; return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r; }
float smin(float a, float b, float k) { float h = max(k - abs(a - b), 0.0) / k; return min(a, b) - h * h * k * 0.25; }
vec4 over(vec4 dst, vec3 c, float a) { return vec4(c * a + dst.rgb * (1.0 - a), a + dst.a * (1.0 - a)); }
"""

    // ------------------------------------------------------------------------------------------ background
    /** The clean light background: the same vertical gradient the menus draw (Backdrop.CLEAN), with a soft light. */
    const val BG_FS = """#version 300 es
precision highp float;
in vec2 vPix;
uniform vec3 uTop;
uniform vec3 uBottom;
uniform vec2 uScreen;
out vec4 o;
void main() {
    vec2 uv = vPix / uScreen;
    vec3 c = mix(uTop, uBottom, uv.y);
    o = vec4(c, 1.0);
}
"""

    // ------------------------------------------------------------------------------------------ carved board bake
    /**
     * The board is a light slab shaped like the maze (its outline follows the paths, with rounded corners, a
     * thin light rim, a front lip and a soft shadow); the paths are dark channels carved into it with big
     * rounded corners. Just below every wall edge the wall's face shows as a darker band inside the channel.
     * Walls that open to the outside are background; walls enclosed by paths are slab.
     * uGrid is RGBA8: r = floor, g = unused, b = corner bits, a = floor neighbour bits (CPU, GameRenderer.bake).
     * Mode 1 writes the paint mask: r = the whole channel (paint covers the wall-face band), g = distance to the nearest wall.
     */
    const val BAKE_FS = """#version 300 es
precision highp float;
in vec2 vUv;
in vec2 vPix;
uniform highp sampler2D uGrid;
uniform ivec2 uGridSize;
uniform float uCell;
uniform vec2 uOrigin;               // grid (0,0) in bake pixels
uniform int uMode;                  // 0 colour, 1 mask
uniform vec3 uFloor;                // carved channel floor
uniform vec3 uSideTop;              // slab top
uniform vec3 uSideBottom;           // slab front lip and wall faces' tint
uniform float uDepth;               // slab lip depth in cells
uniform int uPattern;               // board surface: 0 plain, 1 wood grain, 2 terrazzo, 3 marble
out vec4 o;
""" + COMMON + """
const float RIM = 0.30;
// surface patterns for the board themes (p in cells, so values stay small: no precision seams on Mali)
float bh(vec2 q) { return fract(sin(dot(q, vec2(127.1, 311.7))) * 43758.5453); }
float bn(vec2 q) {
    vec2 i = floor(q), f = fract(q); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(bh(i), bh(i + vec2(1.0, 0.0)), f.x), mix(bh(i + vec2(0.0, 1.0)), bh(i + vec2(1.0, 1.0)), f.x), f.y);
}
vec3 surface(vec3 base, vec2 p, float strength) {
    if (uPattern == 1) {            // wood: long wavy grain running across the board
        float n = bn(p * vec2(0.45, 1.6)) * 1.8 + bn(p * vec2(1.1, 4.0)) * 0.5;
        float g = 0.5 + 0.5 * sin((p.y * 2.6 + n * 2.2) * 3.14159);
        float fine = bn(p * vec2(0.8, 14.0));
        return base * (1.0 + strength * (0.16 * g - 0.08 - 0.08 * fine));
    }
    if (uPattern == 2) {            // terrazzo: little coloured chips
        vec2 q = p * 3.2;
        vec2 i = floor(q);
        float h = bh(i);
        vec2 c = vec2(bh(i + 3.1), bh(i + 7.7)) * 0.6 + 0.2;
        float r = 0.07 + 0.09 * bh(i + 1.7);
        float d = length(fract(q) - c) - r;
        float chip = (h > 0.35 ? 1.0 : 0.0) * (1.0 - smoothstep(-0.03, 0.03, d)) * strength;
        float k = bh(i + 5.3);
        vec3 cc = k < 0.25 ? vec3(0.95, 0.45, 0.38) : k < 0.5 ? vec3(0.25, 0.70, 0.66) : k < 0.75 ? vec3(0.96, 0.76, 0.30) : vec3(0.55, 0.52, 0.50);
        return mix(base, cc, chip);
    }
    if (uPattern == 3) {            // marble: soft grey veins
        float n = bn(p * 0.7) * 3.0 + bn(p * 1.9) * 1.2;
        float v = abs(sin(p.x * 0.8 + p.y * 0.55 + n));
        float vein = (1.0 - smoothstep(0.0, 0.12, v)) * 0.55 + (1.0 - smoothstep(0.0, 0.35, v)) * 0.12;
        return mix(base, base * 0.72, vein * strength);
    }
    return base;
}   // slab margin around the outer paths (cells)
const float HF = 0.26;    // visible wall face inside the channel, below each wall edge (cells)
vec4 cellAt(ivec2 c) {
    if (c.x < 0 || c.y < 0 || c.x >= uGridSize.x || c.y >= uGridSize.y) return vec4(0.0);
    return texelFetch(uGrid, c, 0);
}
const vec2 CORNER[4] = vec2[4](vec2(1.0, 1.0), vec2(1.0, -1.0), vec2(-1.0, 1.0), vec2(-1.0, -1.0));
// Box with its own radius per corner: r.x (+x,+y), r.y (+x,-y), r.z (-x,+y), r.w (-x,-y). (y points down.)
float sdRBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}
// Signed distance to the floor channels (negative inside). Per cell the CPU packs (Kotlin cornerBits):
// b = corner bits: low 4 = round this floor cell's corner off; high 4 = on a wall cell, fillet that corner
//     (the three cells around it are floor), on a floor cell, all four cells around that corner are floor;
// a = floor neighbour bits: 1 east, 2 west, 4 south, 8 north.
// Each floor cell's box reaches halfway into its floor neighbours, so the inside of a path has no seams
// (no grid lines); boxes are joined with a plain min, so straight runs stay perfectly straight.
float F(vec2 p) {
    const float R = 0.5;     // outer corners and dead ends: fully round
    const float RF = 0.32;   // fillets on wall corners (rounded islands)
    ivec2 c = ivec2(floor(p));
    float d = 2.5;
    for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) {
        ivec2 cc = c + ivec2(i, j);
        vec4 t = cellAt(cc);
        int bits = int(t.b * 255.0 + 0.5);
        vec2 ctr = vec2(cc) + 0.5;
        if (t.r > 0.5) {
            int nb = int(t.a * 255.0 + 0.5);
            vec4 r = vec4((bits & 1) != 0 ? R : 0.0, (bits & 2) != 0 ? R : 0.0, (bits & 4) != 0 ? R : 0.0, (bits & 8) != 0 ? R : 0.0);
            // stretched along x into east/west floor neighbours, and along y into north/south ones
            vec2 loH = vec2((nb & 2) != 0 ? -1.0 : -0.5, -0.5), hiH = vec2((nb & 1) != 0 ? 1.0 : 0.5, 0.5);
            vec2 loV = vec2(-0.5, (nb & 8) != 0 ? -1.0 : -0.5), hiV = vec2(0.5, (nb & 4) != 0 ? 1.0 : 0.5);
            d = min(d, sdRBox(p - ctr - (loH + hiH) * 0.5, (hiH - loH) * 0.5, r));
            d = min(d, sdRBox(p - ctr - (loV + hiV) * 0.5, (hiV - loV) * 0.5, r));
            // a grid corner with floor on all four cells: cover it, so open areas have no seam dots
            for (int k = 0; k < 4; k++) if ((bits & (16 << k)) != 0) d = min(d, sdBox(p - ctr - 0.5 * CORNER[k], vec2(0.5), 0.0));
        } else if (bits > 15) {
            for (int k = 0; k < 4; k++) {
                if ((bits & (16 << k)) == 0) continue;
                // local frame: the grid corner at the origin, +x/+y pointing into this wall cell; the fillet
                // fills the square [0,RF]^2 outside the circle of radius RF around (RF,RF)
                vec2 v = ctr + 0.5 * CORNER[k];
                vec2 lp = (p - v) * (-CORNER[k]);
                vec2 q = lp - vec2(RF);
                float dc = (q.x <= 0.0 && q.y <= 0.0) ? RF - length(q) : min(length(lp - vec2(RF, 0.0)), length(lp - vec2(0.0, RF)));
                // the square reaches 0.06 into the neighbouring floor cells so the join has no hairline
                d = min(d, max(sdBox(lp - vec2((RF - 0.06) * 0.5), vec2((RF + 0.06) * 0.5), 0.0), dc));
            }
        }
    }
    return d;
}
// the board slab: one rounded rectangle around the whole grid, grown by RIM
float S(vec2 p) { return sdBox(p - vec2(uGridSize) * 0.5, vec2(uGridSize) * 0.5, 0.5) - RIM; }
// inside the floor: distance to the nearest wall cell (true distance; F's inside values are per cell)
float W(vec2 p) {
    ivec2 c = ivec2(floor(p));
    float d = 1.5;
    for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) {
        ivec2 cc = c + ivec2(i, j);
        if (cellAt(cc).r < 0.5) d = min(d, sdBox(p - (vec2(cc) + 0.5), vec2(0.5), 0.0));
    }
    return max(d, 0.0);
}
void main() {
    vec2 p = (vPix - uOrigin) / uCell;
    float aa = 1.0 / uCell;
    float f0 = F(p);
    float fl = 1.0 - smoothstep(-aa, aa, f0);
    // the wall face: floor whose point HF above is not floor
    float faceOn = smoothstep(-aa, aa, F(p - vec2(0.0, HF)));
    float face = fl * faceOn;
    float dw = W(p);
    if (uMode == 1) {
        // paint covers the whole channel, the wall-face band too (the user wants no dark strip left over)
        o = vec4(fl, clamp(dw / 0.5, 0.0, 1.0), 0.0, 1.0);
        return;
    }
    float s0 = S(p);
    float slab = 1.0 - smoothstep(-aa, aa, s0);
    // soft shadow under the slab and its front lip
    float sh = 2.5;
    for (int k = 0; k <= 3; k++) sh = min(sh, S(p - vec2(0.06, uDepth + 0.10 + 0.14 * float(k))));
    float shadow = (1.0 - smoothstep(-0.25, 0.6, sh)) * 0.16;
    vec4 col = vec4(vec3(0.42, 0.28, 0.18) * shadow, shadow);
    float lipD = 2.5;
    for (int k = 1; k <= 4; k++) lipD = min(lipD, S(p - vec2(0.0, uDepth * float(k) / 4.0)));
    float lip = (1.0 - smoothstep(-aa, aa, lipD)) * (1.0 - slab);
    col = vec4(surface(uSideBottom, p, 0.6) * lip, lip) + col * (1.0 - lip);
    // slab top with a light rim along its outline
    vec3 top = surface(uSideTop, p, 1.0) * (0.985 + 0.03 * smoothstep(0.0, 1.5, -s0));
    top = mix(top, vec3(1.0), 0.4 * (1.0 - smoothstep(0.02, 0.07, -s0)));
    col = vec4(top * slab, slab) + col * (1.0 - slab);
    // carved channel: floor with a thin tile grid and soft shading near the walls
    vec3 fc = (uPattern == 1 ? surface(uFloor, p + vec2(3.7, 1.3), 1.0) : uFloor) * (0.90 + 0.10 * smoothstep(0.0, 0.45, dw));
    // the wall face: darker than the floor, a little lighter right at the top edge
    float t = clamp(-f0 / HF, 0.0, 1.0);
    vec3 faceC = mix(uFloor * 0.62, mix(uFloor, uSideBottom, 0.25) * 0.78, smoothstep(0.75, 1.0, 1.0 - t));
    fc = mix(fc, faceC, face);
    // a faint cell grid on the channel floor (reference "graph-paper" look), aligned to the maze cells
    vec2 gf = abs(fract(p) - 0.5);
    float gline = smoothstep(0.455, 0.5, max(gf.x, gf.y));
    fc = mix(fc, fc * 1.5 + 0.02, gline * 0.5);
    col = vec4(fc * fl, fl) + col * (1.0 - fl);
    o = col;
}
"""

    // ------------------------------------------------------------------------------------------ comet streak
    /** One segment of the ball's comet streak: a capsule from A to B whose radius and alpha grow towards the ball. */
    const val STREAK_FS = """#version 300 es
precision highp float;
in vec2 vPix;
uniform vec2 uA;
uniform vec2 uB;
uniform float uRA;
uniform float uRB;
uniform float uAA;
uniform float uAB;
uniform vec3 uColor;
out vec4 o;
void main() {
    vec2 pa = vPix - uA;
    vec2 ba = uB - uA;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-4), 0.0, 1.0);
    float r = mix(uRA, uRB, h);
    float dc = length(pa - ba * h);
    float a = (1.0 - smoothstep(r - 1.5, r + 1.0, dc)) * mix(uAA, uAB, h);
    if (a <= 0.002) discard;
    vec3 c = mix(uColor, vec3(1.0), 0.45 * (1.0 - smoothstep(0.0, r * 0.55, dc)));
    o = vec4(c * a, a);
}
"""

    // ------------------------------------------------------------------------------------------ composite (baked texture)
    const val TEX_FS = """#version 300 es
precision mediump float;
in vec2 vUv;
uniform sampler2D uTex;
uniform float uAlpha;
out vec4 o;
void main() { o = texture(uTex, vec2(vUv.x, 1.0 - vUv.y)) * uAlpha; }
"""

    // ------------------------------------------------------------------------------------------ paint
    const val PAINT_FS = """#version 300 es
precision highp float;
in vec2 vUv;
in vec2 vPix;
uniform sampler2D uMask;
uniform highp sampler2D uPaint;     // RGBA32F per cell: progress 0..1, dir (0..3, 4 radial, -1 wall), t0, order
uniform ivec2 uGridSize;
uniform float uCell;
uniform vec2 uOrigin;
uniform vec3 uPaintA;
uniform vec3 uPaintB;
uniform float uTime;
uniform float uShine;               // win wave front radius in cells (-1 = off)
uniform vec2 uWaveOrigin;           // win wave centre (the ball), in cells
uniform float uFinish;              // 0 classic, 1 pastel, 2 neon, 3 metallic, 4 rainbow, 5 gradient
uniform float uGlow;                // world with glowing paint
uniform float uAlpha;
out vec4 o;
""" + COMMON + """
vec3 hsv2rgb(vec3 c) { vec3 p = abs(fract(c.xxx + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0); return c.z * mix(vec3(1.0), clamp(p - 1.0, 0.0, 1.0), c.y); }
const vec2 DIRV[4] = vec2[4](vec2(0.0, -1.0), vec2(1.0, 0.0), vec2(0.0, 1.0), vec2(-1.0, 0.0));
float segD(vec2 p, vec2 a, vec2 b) { vec2 pa = p - a, ba = b - a; float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0); return length(pa - ba * h); }
// Signed distance to one cell's paint, drawn like a round brush stroke (q in cell units, the cell is [0,1]^2;
// see GameRenderer for the encoding). Bars and the stroke body reach a little past the cell's edges (W) so
// neighbouring painted cells join without seams; a pixel is only shaded by its own cell, so nothing leaks.
float paintSD(vec2 q, vec4 d) {
    if (d.y < -0.5) return 9.0;
    int code = int(d.y + 0.5);
    int arms = code & 31;
    int ent = code >> 5;
    const float W = 0.56;
    vec2 c = vec2(0.5);
    float sd = 9.0;
    if ((arms & 16) != 0) sd = length(q - c) - 0.5;
    if ((arms & 1) != 0) sd = min(sd, sdBox(q - vec2(0.5, 0.22), vec2(W, 0.28), 0.0));
    if ((arms & 2) != 0) sd = min(sd, sdBox(q - vec2(0.78, 0.5), vec2(0.28, W), 0.0));
    if ((arms & 4) != 0) sd = min(sd, sdBox(q - vec2(0.5, 0.78), vec2(W, 0.28), 0.0));
    if ((arms & 8) != 0) sd = min(sd, sdBox(q - vec2(0.22, 0.5), vec2(0.28, W), 0.0));
    float tip = d.x;
    if (ent >= 1 && ent <= 4 && tip > 0.0) {
        vec2 dv = DIRV[ent - 1];
        vec2 e0 = c - dv * 0.5;                  // the edge the ball came in through
        vec2 a = e0 - dv;                        // the stroke starts behind it (no cap there)
        vec2 b = e0 + dv * (tip - 0.5);          // its round cap; tip 1 = cap centred on the cell
        float len = max(dot(b - a, dv), 0.0);
        vec2 r = q - a;
        float body = sdBox(vec2(dot(r, dv) - len * 0.5, dot(r, vec2(-dv.y, dv.x))), vec2(len * 0.5, W), 0.0);
        sd = min(sd, min(segD(q, a, b) - 0.5, body));
    } else if (ent == 5) {
        sd = min(sd, length(q - c) - 0.5 * tip);
    }
    return sd;
}
// A cell's paint plus a bar towards each painted floor neighbour: a round end (where the ball stopped, or the
// start spot) must not leave an unpainted corner next to paint in the neighbouring cell. The bar grows in over
// ~0.2 s after both cells were painted, so the round end at the moving ball stays round.
const ivec2 NB[4] = ivec2[4](ivec2(0, -1), ivec2(1, 0), ivec2(0, 1), ivec2(-1, 0));
float cellSD(vec2 q, ivec2 k, vec4 d) {
    float sd = paintSD(q, d);
    if (d.y < 0.5) return sd;                    // an unpainted cell never reaches towards painted ones
    for (int i = 0; i < 4; i++) {
        ivec2 n = k + NB[i];
        if (n.x < 0 || n.y < 0 || n.x >= uGridSize.x || n.y >= uGridSize.y) continue;
        vec4 e = texelFetch(uPaint, n, 0);
        if (e.y < 0.5) continue;
        float grow = smoothstep(0.10, 0.30, uTime - max(d.z, e.z));
        if (grow <= 0.0) continue;
        vec2 dv = vec2(NB[i]);
        vec2 hs = dv.x != 0.0 ? vec2(0.28, 0.56) : vec2(0.56, 0.28);
        sd = min(sd, sdBox(q - vec2(0.5) - dv * 0.28, hs, 0.0) + 0.6 * (1.0 - grow));
    }
    return sd;
}
void main() {
    vec4 m = texture(uMask, vec2(vUv.x, 1.0 - vUv.y));
    if (m.r < 0.004) discard;
    vec2 p = (vPix - uOrigin) / uCell;
    ivec2 c = ivec2(floor(p));
    float aa = 1.0 / uCell;
    bool inGrid = c.x >= 0 && c.y >= 0 && c.x < uGridSize.x && c.y < uGridSize.y;
    vec4 best = inGrid ? texelFetch(uPaint, c, 0) : vec4(0.0, -1.0, 0.0, 0.0);
    float cov = 0.0;
    if (best.y > -0.5) {
        cov = 1.0 - smoothstep(-aa, aa, cellSD(p - vec2(c), c, best));
    } else {
        // a wall pixel inside a rounded fillet (the wall corner between two floor cells): it is painted only as
        // far as BOTH of those floor cells are, so a stroke never flares into the corner of an unpainted channel
        vec2 fq = p - vec2(c);
        ivec2 sx = ivec2(fq.x < 0.5 ? -1 : 1, 0), sy = ivec2(0, fq.y < 0.5 ? -1 : 1);
        float ca = 0.0, cb = 0.0;
        bool fa = false, fb = false;
        vec4 da = vec4(0.0, -1.0, 0.0, 0.0), db = da;
        ivec2 ka = c + sx, kb = c + sy;
        if (ka.x >= 0 && ka.y >= 0 && ka.x < uGridSize.x && ka.y < uGridSize.y) { da = texelFetch(uPaint, ka, 0); fa = da.y > -0.5; }
        if (kb.x >= 0 && kb.y >= 0 && kb.x < uGridSize.x && kb.y < uGridSize.y) { db = texelFetch(uPaint, kb, 0); fb = db.y > -0.5; }
        if (fa && da.y > 0.5) ca = 1.0 - smoothstep(-aa, aa, cellSD(clamp(p - vec2(ka), 0.0, 1.0), ka, da));
        if (fb && db.y > 0.5) cb = 1.0 - smoothstep(-aa, aa, cellSD(clamp(p - vec2(kb), 0.0, 1.0), kb, db));
        if (fa && fb) cov = min(ca, cb); else if (fa) cov = ca; else cov = cb;
        best = ca >= cb ? da : db;
    }
    if (cov <= 0.002) discard;
    float age = max(uTime - best.z, 0.0);
    float wet = exp(-age * 1.8);
    vec3 base = uPaintA;
    if (uFinish > 0.5 && uFinish < 1.5) base = mix(base, vec3(1.0), 0.28);
    else if (uFinish > 3.5 && uFinish < 4.5) base = hsv2rgb(vec3(fract(best.w * 0.045 + 0.02 * p.y), 0.62, 0.98));
    else if (uFinish > 4.5) base = mix(uPaintA, uPaintB, clamp(p.y / float(uGridSize.y), 0.0, 1.0));
    base *= mix(0.90, 1.0, m.g);
    base *= 1.0 - 0.16 * m.b;
    float hgt = smoothstep(0.0, 1.0, m.g) * cov;
    vec3 n = normalize(vec3(-dFdx(hgt), -dFdy(hgt), 0.12));
    vec3 L = normalize(vec3(-0.45, -0.7, 0.75));
    vec3 Hh = normalize(L + vec3(0.0, 0.0, 1.0));
    bool metallic = uFinish > 2.5 && uFinish < 3.5;
    float spec = pow(max(dot(n, Hh), 0.0), metallic ? 20.0 : 46.0) * (metallic ? 0.9 : (0.05 + 0.25 * wet));
    vec3 col = base * (1.0 + 0.25 * exp(-age * 9.0));
    // fresh paint has a soft wet sheen that dries within ~0.3 s (smooth: no grid, no speckles)
    col += vec3(0.10) * exp(-age * 7.0);
    col += vec3(spec);
    // the same faint cell grid shows through the paint (reference look), aligned to the maze cells
    vec2 gf = abs(fract(p) - 0.5);
    float gline = smoothstep(0.455, 0.5, max(gf.x, gf.y));
    col *= 1.0 - 0.12 * gline;

    if (uShine > -0.5) {
        // win: a bright ring of light ripples out from the ball, with a softer echo behind it,
        // and every tile it passes keeps a warm afterglow that fades back
        float d = length(p - uWaveOrigin);
        float ring = 1.0 - smoothstep(0.0, 0.9, abs(d - uShine));
        float echo = 1.0 - smoothstep(0.0, 1.4, abs(d - (uShine - 2.6)));
        float after = smoothstep(0.0, 1.0, uShine - d) * exp(-max(uShine - d, 0.0) * 0.22);
        col = mix(col, vec3(1.0), 0.55 * ring);
        col += base * 0.35 * echo + vec3(0.10) * after;
    }
    if ((uFinish > 1.5 && uFinish < 2.5) || uGlow > 0.5) col *= 1.12 + 0.08 * sin(uTime * 3.0 + p.x * 0.7);
    float a = cov * m.r * uAlpha;
    o = vec4(col * a, a);
}
"""

    // ------------------------------------------------------------------------------------------ special tiles
    const val SPECIAL_FS = """#version 300 es
precision highp float;
in vec2 vUv;
uniform int uType;        // 1 stopper, 2..5 one-way (up,right,down,left), 6 '/', 7 '\', 8 portal
uniform vec3 uColor;
uniform float uTime;
uniform float uCellPx;
uniform float uAlpha;
out vec4 o;
""" + COMMON + """
float sdSeg(vec2 p, vec2 a, vec2 b) { vec2 pa = p - a; vec2 ba = b - a; float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0); return length(pa - ba * h); }
float chevron(vec2 p) { p.x = abs(p.x); return sdSeg(p, vec2(0.0, -0.10), vec2(0.16, 0.06)) - 0.045; }
void main() {
    vec2 p = vUv - 0.5;
    float aa = 1.4 / uCellPx;
    vec4 col = vec4(0.0);
    vec3 L = normalize(vec3(-0.5, -0.65, 0.75));
    if (uType == 1) {
        float r = length(p);
        col = over(col, vec3(0.0), 0.28 * (1.0 - smoothstep(0.26, 0.40, length(p - vec2(0.03, 0.05)))));
        float body = 1.0 - smoothstep(0.31 - aa, 0.31 + aa, r);
        float z = sqrt(max(0.0, 1.0 - (r / 0.31) * (r / 0.31)));
        vec3 n = normalize(vec3(p / 0.31 * 0.6, z + 0.4));
        vec3 c = uColor * (0.70 + 0.40 * max(dot(n, L), 0.0));
        float ring = 1.0 - smoothstep(0.0, aa * 1.5, abs(r - 0.19) - 0.025);
        c = mix(c, vec3(1.0), 0.65 * ring);
        c += vec3(0.5) * pow(max(dot(reflect(-L, n), vec3(0.0, 0.0, 1.0)), 0.0), 24.0);
        col = over(col, c, body);
    } else if (uType >= 2 && uType <= 5) {
        vec2 q = p;
        if (uType == 3) q = vec2(p.y, -p.x);
        else if (uType == 4) q = vec2(-p.x, -p.y);
        else if (uType == 5) q = vec2(-p.y, p.x);
        float plate = sdBox(p, vec2(0.40), 0.12);
        col = over(col, uColor * 0.85, 0.32 * (1.0 - smoothstep(-aa, aa, plate)));
        float t = fract(uTime * 0.9);
        float d1 = chevron(q - vec2(0.0, 0.16 - t * 0.30));
        float fade1 = smoothstep(0.0, 0.3, t) * (1.0 - smoothstep(0.7, 1.0, t));
        col = over(col, vec3(0.0), 0.2 * (1.0 - smoothstep(-aa, aa * 3.0, chevron(q - vec2(0.02, 0.08)))));
        col = over(col, vec3(1.0), 1.0 - smoothstep(-aa, aa, chevron(q - vec2(0.0, 0.05))));
        col = over(col, uColor * 1.15 + 0.1, (1.0 - smoothstep(-aa, aa, d1)) * fade1 * 0.85);
    } else if (uType == 6 || uType == 7) {
        vec2 a = uType == 6 ? vec2(-0.36, 0.36) : vec2(-0.36, -0.36);
        vec2 b = -a;
        float d = sdSeg(p, a, b) - 0.085;
        col = over(col, vec3(0.0), 0.25 * (1.0 - smoothstep(-0.02, 0.08, sdSeg(p - vec2(0.03, 0.05), a, b) - 0.085)));
        float body = 1.0 - smoothstep(-aa, aa, d);
        float across = clamp(-d / 0.085, 0.0, 1.0);
        vec3 c = uColor * (0.75 + 0.35 * across);
        c += vec3(0.35) * (1.0 - smoothstep(0.0, 0.25, abs(across - 0.55)));
        col = over(col, c, body);
        for (int k = 0; k < 2; k++) {
            vec2 e = k == 0 ? a : b;
            col = over(col, vec3(0.95), 1.0 - smoothstep(-aa, aa, length(p - e) - 0.11));
            col = over(col, uColor * 0.7, 1.0 - smoothstep(-aa, aa, length(p - e) - 0.05));
        }
    } else if (uType == 8) {
        float r = length(p);
        float ang = atan(p.y, p.x);
        col = over(col, uColor, 0.35 * (1.0 - smoothstep(0.20, 0.50, r)));
        float disc = 1.0 - smoothstep(0.36 - aa, 0.36 + aa, r);
        float sw = 0.5 + 0.5 * sin(ang * 3.0 + r * 22.0 - uTime * 6.0);
        vec3 c = mix(uColor * 0.35, uColor * 1.25 + 0.15, sw * smoothstep(0.02, 0.36, r));
        c = mix(vec3(0.05, 0.02, 0.10), c, smoothstep(0.0, 0.16, r));
        c = mix(c, vec3(1.0), (1.0 - smoothstep(0.0, aa * 2.0, abs(r - 0.35) - 0.02)) * 0.8);
        col = over(col, c, disc);
    }
    o = col * uAlpha;
}
"""

    // ------------------------------------------------------------------------------------------ ball
    const val BALL_FS = """#version 300 es
precision highp float;
in vec2 vUv;
uniform mat3 uRot;          // rolling rotation (applied to the surface pattern)
uniform sampler2D uBallTex; // equirectangular albedo; alpha = 1 - glow (tools/BallArt.kt)
uniform float uGloss;
uniform float uMetal;
uniform float uGlass;       // 0 solid .. 1 see-through in the middle (bubble)
uniform vec3 uEnv;          // surface colour reflected near the bottom
uniform float uPx;          // ball diameter in pixels
uniform float uAlpha;
uniform float uTime;
out vec4 o;
""" + COMMON + """
// the ball's texture at direction m (seam-safe gradients so mipmaps don't break along u = 0/1)
vec4 ballTex(vec3 m) {
    vec2 uv = vec2(atan(m.z, m.x) * 0.15915494 + 0.5, acos(clamp(m.y, -1.0, 1.0)) * 0.31830989);
    vec2 dx = dFdx(uv), dy = dFdy(uv);
    float u2 = fract(uv.x + 0.5);
    vec2 dx2 = vec2(dFdx(u2), dx.y), dy2 = vec2(dFdy(u2), dy.y);
    if (abs(dx2.x) + abs(dy2.x) < abs(dx.x) + abs(dy.x)) { dx = dx2; dy = dy2; }
    return textureGrad(uBallTex, uv, dx, dy);
}
vec3 env(vec3 r) {
    vec3 sky = mix(vec3(0.95, 0.97, 1.0), vec3(0.78, 0.84, 0.95), clamp(1.0 - r.y, 0.0, 1.0));
    vec3 c = mix(uEnv * 0.75, sky, smoothstep(-0.35, 0.35, r.y));
    float win = pow(max(dot(r, normalize(vec3(-0.45, 0.65, 0.62))), 0.0), 48.0);
    return c + vec3(2.2) * win;
}
void main() {
    vec2 l = (vUv - 0.5) * 2.0;
    float r2 = dot(l, l);
    float edge = 1.0 - smoothstep(1.0 - 2.5 / uPx, 1.0, sqrt(r2));
    if (edge <= 0.0) discard;
    vec3 n = vec3(l.x, -l.y, sqrt(max(0.0, 1.0 - r2)));
    vec3 L = normalize(vec3(-0.45, 0.65, 0.62));
    vec3 m = uRot * n;
    vec4 tx = ballTex(m);
    vec3 base = tx.rgb;
    float glowAmt = 1.0 - tx.a;
    float wrap = clamp((dot(n, L) + 0.4) / 1.4, 0.0, 1.0);
    vec3 diffuse = base * (0.38 + 0.72 * wrap);
    diffuse += base * 0.18 * pow(1.0 - n.z, 2.0);
    vec3 R = vec3(2.0 * n.z * n.x, 2.0 * n.z * n.y, 2.0 * n.z * n.z - 1.0);
    vec3 e = env(R);
    float fres = 0.05 + 0.95 * pow(1.0 - n.z, 4.0);
    vec3 col = mix(diffuse, e * mix(vec3(1.0), base, 0.85), uMetal);
    col = mix(col, e, fres * 0.55 * uGloss * (1.0 - uMetal * 0.5));
    float spec = pow(max(dot(reflect(-L, n), vec3(0.0, 0.0, 1.0)), 0.0), 90.0);
    col += vec3(1.6 * spec * uGloss);
    col *= 1.0 - 0.22 * smoothstep(0.1, 1.0, -n.y);
    col = mix(col, base * 1.3, glowAmt);
    float a = edge * uAlpha * mix(1.0, 0.45 + 0.55 * fres + 0.4 * spec, uGlass);
    o = vec4(col * a, a);
}
"""

    const val SHADOW_FS = """#version 300 es
precision mediump float;
in vec2 vUv;
uniform float uAlpha;
out vec4 o;
void main() {
    float d = length((vUv - 0.5) * 2.0);
    float a = (1.0 - smoothstep(0.15, 1.0, d)) * uAlpha;
    o = vec4(0.0, 0.0, 0.0, a);
}
"""

    // ------------------------------------------------------------------------------------------ hint (arrow + dotted path)
    const val HINT_FS = """#version 300 es
precision highp float;
in vec2 vUv;
uniform float uTime;
uniform float uAlpha;
uniform int uKind;   // 0 dot, 1 arrow
uniform int uDir;    // 0 up, 1 right, 2 down, 3 left
out vec4 o;
""" + COMMON + """
void main() {
    vec2 p = vUv - 0.5;
    if (uDir == 0) p = vec2(-p.y, p.x);
    else if (uDir == 2) p = vec2(p.y, -p.x);
    else if (uDir == 3) p = vec2(-p.x, -p.y);
    float a;
    if (uKind == 0) {
        a = 1.0 - smoothstep(0.30, 0.42, length(p));
    } else {
        float shaft = sdBox(p - vec2(-0.12, 0.0), vec2(0.20, 0.07), 0.05);
        vec2 q = p - vec2(0.12, 0.0);
        float head = max(q.x * 0.9 + abs(q.y) - 0.18, -q.x - 0.10);
        a = 1.0 - smoothstep(-0.02, 0.02, min(shaft, head));
    }
    float pulse = 0.65 + 0.35 * sin(uTime * 6.0);
    o = vec4(vec3(1.0) * a, a) * uAlpha * pulse;
}
"""

    // ------------------------------------------------------------------------------------------ particles (instanced)
    const val PARTICLE_VS = """#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec4 aA;            // x, y, size, rotation
layout(location = 2) in vec4 aColor;        // premultiplied rgba
layout(location = 3) in vec2 aB;            // shape, flip (confetti 3D tumble, -1..1)
uniform vec2 uScreen;
uniform vec2 uCenter;
uniform vec3 uXform;
out vec2 vUv;
out vec4 vColor;
flat out int vShape;
out float vFlip;
void main() {
    vec2 c = aPos - 0.5;
    c.x *= (aB.x > 0.5 && aB.x < 1.5) ? max(abs(aB.y), 0.08) : 1.0;
    float s = sin(aA.w); float co = cos(aA.w);
    vec2 r = vec2(c.x * co - c.y * s, c.x * s + c.y * co) * aA.z;
    vec2 px = aA.xy + r;
    px = uCenter + (px - uCenter) * uXform.x + uXform.yz;
    gl_Position = vec4(px.x / uScreen.x * 2.0 - 1.0, 1.0 - px.y / uScreen.y * 2.0, 0.0, 1.0);
    vUv = aPos;
    vColor = aColor;
    vShape = int(aB.x + 0.5);
    vFlip = aB.y;
}
"""

    const val PARTICLE_FS = """#version 300 es
precision mediump float;
in vec2 vUv;
in vec4 vColor;
flat in int vShape;
in float vFlip;
out vec4 o;
float dot2(vec2 v) { return dot(v, v); }
float sdSegP(vec2 p, vec2 a, vec2 b) { vec2 pa = p - a, ba = b - a; float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0); return length(pa - ba * h); }
float sdHeart(vec2 p) {   // y up, from (0,0) at the tip to ~1 at the top
    p.x = abs(p.x);
    if (p.y + p.x > 1.0) return sqrt(dot2(p - vec2(0.25, 0.75))) - 0.35355339;
    return sqrt(min(dot2(p - vec2(0.0, 1.0)), dot2(p - 0.5 * max(p.x + p.y, 0.0)))) * sign(p.x - p.y);
}
float sdStar5(vec2 p, float r, float rf) {
    const vec2 k1 = vec2(0.809016994, -0.587785252);
    const vec2 k2 = vec2(-0.809016994, -0.587785252);
    p.x = abs(p.x);
    p -= 2.0 * max(dot(k1, p), 0.0) * k1;
    p -= 2.0 * max(dot(k2, p), 0.0) * k2;
    p.x = abs(p.x);
    p.y -= r;
    vec2 ba = rf * vec2(-k1.y, k1.x) - vec2(0.0, 1.0);
    float h = clamp(dot(p, ba) / dot(ba, ba), 0.0, r);
    return length(p - ba * h) * sign(p.y * ba.x - p.x * ba.y);
}
void main() {
    vec2 p = (vUv - 0.5) * 2.0;
    float a;
    vec3 tint = vec3(1.0);
    vec4 over = vec4(0.0);   // rgb mixed over the particle colour by .a (flower centres, white cores)
    if (vShape == 6) {                                   // heart
        float d = sdHeart(vec2(p.x, -p.y) * 0.62 + vec2(0.0, 0.5));
        a = 1.0 - smoothstep(-0.015, 0.025, d);
        tint = vec3(1.0 + 0.45 * (1.0 - smoothstep(0.0, 0.3, length(p - vec2(-0.35, -0.35)))));
    } else if (vShape == 7) {                            // five-point star with a soft glow
        float d = sdStar5(vec2(p.x, -p.y), 0.92, 0.45);
        a = clamp((1.0 - smoothstep(-0.02, 0.04, d)) + 0.35 * (1.0 - smoothstep(0.0, 0.3, d)), 0.0, 1.0);
        tint = vec3(1.0 + 0.35 * (1.0 - smoothstep(0.0, 0.45, length(p))));
    } else if (vShape == 8) {                            // leaf with a midrib
        float d = max(length(p - vec2(0.0, 0.62)), length(p + vec2(0.0, 0.62))) - 1.0;
        a = 1.0 - smoothstep(-0.03, 0.03, d);
        tint = vec3(p.y < 0.0 ? 1.12 : 0.88) * (1.0 - 0.3 * (1.0 - smoothstep(0.02, 0.06, abs(p.y))) * step(abs(p.x), 0.75));
    } else if (vShape == 9) {                            // flower: five petals and a yellow centre
        float r = length(p);
        float ang = atan(p.y, p.x);
        float pr = 0.62 + 0.3 * cos(5.0 * ang);
        a = 1.0 - smoothstep(pr - 0.06, pr, r);
        tint = vec3(0.85 + 0.25 * smoothstep(0.2, 0.8, r));
        over = vec4(1.0, 0.86, 0.35, 1.0 - smoothstep(0.2, 0.28, r));
    } else if (vShape == 10) {                           // music note
        vec2 hp = (p - vec2(-0.28, 0.52)) * mat2(0.8, 0.6, -0.6, 0.8);
        float d = length(hp * vec2(1.0, 1.5)) - 0.3;
        d = min(d, max(abs(p.x - 0.0) - 0.07, abs(p.y + 0.1) - 0.62));
        d = min(d, sdSegP(p, vec2(0.0, -0.72), vec2(0.42, -0.4)) - 0.08);
        a = 1.0 - smoothstep(-0.02, 0.04, d);
    } else if (vShape == 11) {                           // snowflake: six arms with little branches
        float r = length(p);
        float ang = mod(atan(p.y, p.x), 1.0471976) - 0.5235988;
        vec2 q = vec2(cos(ang), abs(sin(ang))) * r;
        float d = sdSegP(q, vec2(0.0), vec2(0.88, 0.0));
        d = min(d, sdSegP(q, vec2(0.5, 0.0), vec2(0.74, 0.2)));
        a = 1.0 - smoothstep(0.06, 0.13, d);
    } else if (vShape == 12) {                           // petal
        float d = max(length(p - vec2(0.0, 0.85)), length(p + vec2(0.0, 0.85))) - 1.18;
        d = max(d, p.x - 0.75);
        a = 1.0 - smoothstep(-0.04, 0.03, d);
        tint = vec3(0.9 + 0.3 * smoothstep(-0.8, 0.6, p.x));
    } else if (vShape == 13) {                           // crystal shard (long rhombus, two facets)
        float d = abs(p.x) * 0.5 + abs(p.y) * 1.0;
        a = 1.0 - smoothstep(0.42, 0.5, d);
        tint = vec3(p.y < 0.0 ? 1.3 : 0.82);
        over = vec4(1.0, 1.0, 1.0, 0.6 * (1.0 - smoothstep(0.0, 0.05, abs(p.y))) * (1.0 - smoothstep(0.3, 0.9, abs(p.x))));
    } else if (vShape == 14) {                           // lightning bolt with a white core
        float d = sdSegP(p, vec2(-0.95, 0.1), vec2(-0.35, -0.3));
        d = min(d, sdSegP(p, vec2(-0.35, -0.3), vec2(0.1, 0.25)));
        d = min(d, sdSegP(p, vec2(0.1, 0.25), vec2(0.95, -0.15)));
        a = clamp((1.0 - smoothstep(0.05, 0.12, d)) + 0.45 * (1.0 - smoothstep(0.1, 0.35, d)), 0.0, 1.0);
        over = vec4(1.0, 1.0, 1.0, 1.0 - smoothstep(0.02, 0.06, d));
    } else if (vShape == 0) {
        a = 1.0 - smoothstep(0.2, 1.0, length(p));
    } else if (vShape == 1) {
        vec2 q = abs(p) - vec2(0.75, 0.42);
        a = 1.0 - smoothstep(-0.05, 0.08, max(q.x, q.y));
        tint = vec3(0.70 + 0.45 * abs(vFlip));
    } else if (vShape == 2) {
        float d = min(abs(p.x) * 6.0 + abs(p.y), abs(p.y) * 6.0 + abs(p.x));
        a = (1.0 - smoothstep(0.3, 1.0, d)) + 0.5 * (1.0 - smoothstep(0.0, 0.35, length(p)));
        a = clamp(a, 0.0, 1.0);
    } else if (vShape == 3) {
        float r = length(p);
        a = 1.0 - smoothstep(0.80, 0.95, r);
        tint = vec3(1.0 + 0.6 * (1.0 - smoothstep(0.0, 0.3, length(p - vec2(-0.3, -0.35)))));
    } else if (vShape == 4) {
        a = 1.0 - smoothstep(0.0, 0.12, abs(length(p) - 0.8));
    } else {
        float r = length(p);
        a = (1.0 - smoothstep(0.88, 1.0, r)) * (0.25 + 0.75 * smoothstep(0.55, 0.95, r));
        a += 0.8 * (1.0 - smoothstep(0.0, 0.22, length(p - vec2(-0.35, -0.38))));
        a = clamp(a, 0.0, 1.0);
    }
    vec3 rgb = mix(vColor.rgb * tint, over.rgb * vColor.a, over.a);
    o = vec4(rgb, vColor.a) * a;
}
"""
}
