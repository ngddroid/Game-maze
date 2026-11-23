package com.ngstudio.maze.render

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

    /**
     * The surface (wood, marble, ...) comes from the pre-rendered world image res/drawable-nodpi/surface_N.jpg
     * (tools/MaterialArt.kt), mapped to the screen with "centre crop". Menus show the same image, so game and
     * menus always match, and there is no GPU noise precision to worry about.
     */
    private const val SURFACE = """
uniform sampler2D uSurface;
uniform vec2 uSurfOffset;   // pixels cropped from the image's left/top, in screen pixels
uniform vec2 uSurfSize;     // displayed image size in screen pixels
uniform vec2 uRealScreen;
vec3 surfaceAt(vec2 sp) { return texture(uSurface, (sp + uSurfOffset) / uSurfSize).rgb; }
"""

    // ------------------------------------------------------------------------------------------ background (baked once)
    const val BG_FS = """#version 300 es
precision highp float;
in vec2 vPix;
out vec4 o;
""" + SURFACE + """
void main() { o = vec4(surfaceAt(vPix), 1.0); }
"""

    // ------------------------------------------------------------------------------------------ carved board bake
    const val BAKE_FS = """#version 300 es
precision highp float;
in vec2 vUv;
in vec2 vPix;
uniform highp sampler2D uGrid;      // R8: 1 = floor
uniform ivec2 uGridSize;
uniform float uCell;
uniform vec2 uOrigin;               // grid (0,0) in bake pixels
uniform vec2 uBakeOrigin;           // bake (0,0) in screen pixels
uniform int uMode;                  // 0 colour, 1 floor mask / AO / shadow
uniform vec3 uFloorA;
uniform vec3 uFloorB;
// The board sits on a wood slab inside a cream card with a gold rim (same look as the Home level card).
uniform vec4 uCard;                 // card rect in bake pixels (x, y, w, h)
uniform vec4 uSlab;                 // wood slab rect in bake pixels
uniform float uDp;                  // pixels per dp
uniform sampler2D uWood;            // polished board wood (world 1) ...
uniform int uWoodOn;                // ... or 0: the world's own surface
uniform vec2 uWoodOffset;           // slab-pixel offset into the cover-cropped wood image
uniform vec2 uWoodSize;             // displayed wood image size in pixels
out vec4 o;
""" + COMMON + SURFACE + """
vec3 woodAt(vec2 bp, vec2 sp) {
    if (uWoodOn == 0) return surfaceAt(sp);
    return texture(uWood, (bp - uSlab.xy + uWoodOffset) / uWoodSize).rgb;
}
const float HF = 0.13;  // inner wall face visible inside the channel, below each wall (cells)
float isFloor(ivec2 c) {
    if (c.x < 0 || c.y < 0 || c.x >= uGridSize.x || c.y >= uGridSize.y) return 0.0;
    return texelFetch(uGrid, c, 0).r;
}
// Signed distance to the floor channels (negative inside floor). Exact inside walls: used for the
// carved edge and the bevel. Walls sit exactly on the grid (no offset), so every wall is 1 cell thick.
float F(vec2 p) {
    ivec2 c = ivec2(floor(p));
    float d = 1.5;
    for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) {
        ivec2 cc = c + ivec2(i, j);
        if (isFloor(cc) > 0.5) d = smin(d, sdBox(p - (vec2(cc) + 0.5), vec2(0.5), 0.12), 0.18);
    }
    return d;
}
// Depth inside the floor = distance to the nearest wall cell (exact inside the floor, so neighbouring
// floor tiles blend without creases).
float depth(vec2 p) {
    ivec2 c = ivec2(floor(p));
    float d = 1.5;
    for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) {
        ivec2 cc = c + ivec2(i, j);
        if (isFloor(cc) < 0.5) d = min(d, sdBox(p - (vec2(cc) + 0.5), vec2(0.5), 0.12));
    }
    return d;
}
void main() {
    vec2 p = (vPix - uOrigin) / uCell;
    vec2 sp = vPix + uBakeOrigin;
    float aa = 1.0 / uCell;
    vec3 L = normalize(vec3(-0.45, -0.7, 0.75));
    float f0 = F(p);
    float wall = smoothstep(-aa, aa, f0);
    float dep = depth(p);
    // inner face: floor that lies within HF below a wall edge (the wall's side seen through the opening)
    float fAbove = F(p - vec2(0.0, HF));
    float face = (1.0 - wall) * smoothstep(-aa, aa, fAbove);
    float faceT = clamp(-f0 / HF, 0.0, 1.0);
    float inner = 1.0 - smoothstep(-0.02, 0.26, F(p + vec2(-0.22, -0.30)));
    float ao = smoothstep(0.0, 0.32, dep);
    if (uMode == 1) {
        o = vec4((1.0 - wall) * (1.0 - face), ao, inner, 1.0);
        return;
    }
    // ---- card + slab shapes
    vec2 cc = uCard.xy + uCard.zw * 0.5;
    float dCard = sdBox(vPix - cc, uCard.zw * 0.5, 24.0 * uDp);
    vec2 sc = uSlab.xy + uSlab.zw * 0.5;
    float slabR = 0.6 * uCell;
    float dSlab = sdBox(vPix - sc, uSlab.zw * 0.5, slabR);
    if (dSlab > 0.0) {
        float a1 = 1.0 / max(uDp, 1.0);
        // soft brown drop shadow under the card
        float sh = 1.0 - smoothstep(-6.0 * uDp, 12.0 * uDp, sdBox(vPix - cc - vec2(0.0, 5.0 * uDp), uCard.zw * 0.5, 24.0 * uDp));
        vec4 col = vec4(vec3(0.29, 0.14, 0.05) * 0.34 * sh, 0.34 * sh);
        float inCard = 1.0 - smoothstep(-0.8, 0.8, dCard);
        if (inCard > 0.0) {
            float t = clamp((vPix.y - uCard.y) / uCard.w, 0.0, 1.0);
            vec3 rimL = vec3(1.0, 0.851, 0.541), rimD = vec3(0.878, 0.573, 0.180);
            vec3 rim = mix(rimL, rimD, 1.0 - abs(t * 2.0 - 1.0));
            rim *= 1.0 + 0.18 * (1.0 - smoothstep(0.0, 1.5 * uDp, -dCard));   // bright outer lip
            vec3 cream = vec3(1.0, 0.973, 0.937), border = vec3(0.929, 0.710, 0.376);
            vec3 body = t < 0.5 ? mix(vec3(1.0), cream, t * 2.0) : mix(cream, mix(cream, border, 0.22), t * 2.0 - 1.0);
            // the slab sits in the card with a soft contact shadow
            body *= 1.0 - 0.22 * (1.0 - smoothstep(0.0, 7.0 * uDp, dSlab - 2.0 * uDp));
            float inRim = smoothstep(-4.0 * uDp - 0.8, -4.0 * uDp + 0.8, dCard);
            vec3 c = mix(body, rim, inRim);
            float line = (1.0 - smoothstep(0.0, 0.9, abs(dCard + 4.8 * uDp) - 0.75 * uDp)) * (1.0 - inRim);
            c = mix(c, vec3(1.0), 0.75 * line);
            col = vec4(c * inCard, inCard) + col * (1.0 - inCard);
        }
        // dark outline hugging the slab
        float ol = 1.0 - smoothstep(0.0, 1.0, abs(dSlab - 0.035 * uCell) - 0.035 * uCell);
        col.rgb = mix(col.rgb, vec3(0.26, 0.12, 0.04) * col.a, 0.55 * ol);
        o = col;
        return;
    }
    vec3 surf = woodAt(vPix, sp);
    // wall top with a small rounded bevel on the carved edge
    vec2 e = vec2(0.012, 0.0);
    vec2 g = vec2(F(p + e.xy) - F(p - e.xy), F(p + e.yx) - F(p - e.yx));
    g = g / max(length(g), 1e-4);
    float bev = 1.0 - smoothstep(0.0, 0.07, f0);
    vec3 n = normalize(vec3(-g * bev * 1.1, 1.0));
    float dif = dot(n, L);
    vec3 top = surf * (1.0 + 0.50 * (dif - L.z));
    top += vec3(0.05) * bev * max(dif - L.z, 0.0) * 4.0;
    // the face: lit near the top edge, darker where it meets the floor
    vec3 faceCol = woodAt(vPix + vec2(0.0, 41.0), sp + vec2(0.0, 41.0)) * mix(0.92, 0.60, faceT);
    faceCol *= 1.0 - 0.25 * smoothstep(0.75, 1.0, faceT);
    // channel floor: tile grid, ambient occlusion, inner shadow
    vec3 fl = mix(uFloorB, uFloorA, ao);
    fl *= 0.94 + 0.10 * fbm(sp / (uRealScreen.x / 9.0));
    vec2 q = fract(p);
    float edge = min(min(q.x, 1.0 - q.x), min(q.y, 1.0 - q.y));
    fl *= 1.0 - 0.24 * (1.0 - smoothstep(0.0, 0.018, edge));
    fl *= 1.0 - 0.45 * inner;
    vec3 c = mix(fl, faceCol, face);
    c = mix(c, top, wall);
    // slab rim: a slight bevel just inside the edge, then the dark outline
    c *= 1.0 - 0.18 * (1.0 - smoothstep(0.0, 0.18 * uCell, -dSlab));
    c = mix(c, vec3(0.26, 0.12, 0.04), 0.55 * (1.0 - smoothstep(0.0, 1.0, abs(dSlab - 0.035 * uCell) - 0.035 * uCell)));
    o = vec4(c, 1.0);
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
void main() {
    vec4 m = texture(uMask, vec2(vUv.x, 1.0 - vUv.y));
    if (m.r < 0.004) discard;
    vec2 p = (vPix - uOrigin) / uCell;
    ivec2 c = ivec2(floor(p));
    float cov = 0.0;
    vec4 best = vec4(0.0);
    vec2 bestCell = vec2(c);
    // A pixel inside a floor tile takes only that tile's paint (no bleeding into the unpainted tile next
    // to it). A pixel in a wall tile (the rounded fillets of the channel) takes the nearest painted tile.
    bool ownFloor = c.x >= 0 && c.y >= 0 && c.x < uGridSize.x && c.y < uGridSize.y && texelFetch(uPaint, c, 0).y > -0.5;
    for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) {
        ivec2 k = c + ivec2(i, j);
        if (k.x < 0 || k.y < 0 || k.x >= uGridSize.x || k.y >= uGridSize.y) continue;
        if (ownFloor && (i != 0 || j != 0)) continue;
        vec4 d = texelFetch(uPaint, k, 0);
        if (d.y < -0.5 || d.x <= 0.0) continue;
        vec2 q = p - vec2(k);
        float inBox = ownFloor ? 1.0 : 1.0 - smoothstep(0.08, 0.16, sdBox(q - 0.5, vec2(0.5), 0.0));
        if (inBox <= 0.0) continue;
        float s;
        if (d.y < 0.5) s = 1.0 - q.y;
        else if (d.y < 1.5) s = q.x;
        else if (d.y < 2.5) s = q.y;
        else if (d.y < 3.5) s = 1.0 - q.x;
        else s = length(q - 0.5) * 1.3;
        float wob = (noise(p * 4.3 + vec2(k) * 1.7) - 0.5) * 0.22 * (1.0 - d.x);
        float front = d.x * 1.3 - 0.15;
        float fill = 1.0 - smoothstep(front - 0.05, front + 0.05, s + wob);
        float v = fill * inBox;
        if (v > cov) { cov = v; best = d; bestCell = vec2(k); }
    }
    if (cov <= 0.002) discard;
    float age = max(uTime - best.z, 0.0);
    float wet = exp(-age * 1.8);
    vec3 base = uPaintA;
    if (uFinish > 0.5 && uFinish < 1.5) base = mix(base, vec3(1.0), 0.28);
    else if (uFinish > 3.5 && uFinish < 4.5) base = hsv2rgb(vec3(fract(best.w * 0.045 + 0.02 * p.y), 0.62, 0.98));
    else if (uFinish > 4.5) base = mix(uPaintA, uPaintB, clamp(p.y / float(uGridSize.y), 0.0, 1.0));
    base *= 0.97 + 0.05 * fbm(p * 2.0 + 3.1);
    base *= mix(0.90, 1.0, m.g);
    base *= 1.0 - 0.16 * m.b;
    vec2 q = fract(p);
    float edge = min(min(q.x, 1.0 - q.x), min(q.y, 1.0 - q.y));
    base *= 1.0 - 0.12 * (1.0 - smoothstep(0.0, 0.016, edge));
    float hgt = smoothstep(0.0, 1.0, m.g) * cov;
    vec3 n = normalize(vec3(-dFdx(hgt), -dFdy(hgt), 0.12));
    vec3 L = normalize(vec3(-0.45, -0.7, 0.75));
    vec3 Hh = normalize(L + vec3(0.0, 0.0, 1.0));
    bool metallic = uFinish > 2.5 && uFinish < 3.5;
    float spec = pow(max(dot(n, Hh), 0.0), metallic ? 20.0 : 46.0) * (metallic ? 0.9 : (0.05 + 0.25 * wet));
    vec3 col = base * (1.0 + 0.25 * exp(-age * 9.0));
    col += vec3(spec);
    col += vec3(0.08) * wet * smoothstep(0.45, 1.0, noise(p * 1.6 + age * 0.4));
    vec2 lc = p - bestCell - 0.5;
    col += vec3(0.18) * (1.0 - smoothstep(0.0, 0.07, abs(length(lc) - age * 1.6))) * exp(-age * 4.0);
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
uniform vec3 uColorA;
uniform vec3 uColorB;
uniform vec3 uColorC;
uniform float uPattern;     // see BallSkin
uniform float uGloss;
uniform float uMetal;
uniform vec3 uEnv;          // surface colour reflected near the bottom
uniform float uPx;          // ball diameter in pixels
uniform float uAlpha;
uniform float uTime;
out vec4 o;
""" + COMMON + """
float hash3(vec3 p) { p = fract(p * 0.3183099 + 0.1); p *= 17.0; return fract(p.x * p.y * p.z * (p.x + p.y + p.z)); }
vec3 pattern(vec3 m) {
    int pt = int(uPattern + 0.5);
    if (pt == 1) return abs(m.y) < 0.28 ? uColorB : uColorA;
    if (pt == 2) { float d = max(max(abs(m.x), abs(m.y)), abs(m.z)); return d > 0.86 ? uColorB : uColorA; }
    if (pt == 3) return m.x > 0.0 ? uColorA : uColorB;
    if (pt == 4) { float s = sin(atan(m.y, m.x) * 3.0 + m.z * 5.0); return mix(uColorA, uColorB, smoothstep(-0.2, 0.2, s)); }
    if (pt == 5) {
        float seg = floor((atan(m.z, m.x) + 3.14159) / 6.28318 * 6.0);
        vec3 c = mod(seg, 3.0) < 0.5 ? uColorA : (mod(seg, 3.0) < 1.5 ? uColorB : uColorC);
        return abs(m.y) > 0.88 ? vec3(1.0) : c;
    }
    if (pt == 6) { float n = fbm(m.xy * 3.0 + m.z * 2.0); return mix(uColorA, uColorB, smoothstep(0.35, 0.75, n)); }
    if (pt == 7) { float n = fbm(m.xz * 2.5 + m.y); vec3 c = mix(uColorA, uColorB, n); return c + vec3(step(0.985, hash3(floor(m * 40.0)))); }
    if (pt == 8) { float band = sin(m.y * 18.0 + fbm(m.xz * 4.0) * 3.0); return mix(uColorA, uColorB, smoothstep(-0.3, 0.3, band)); }
    if (pt == 9) { float a = atan(m.z, m.x); float r = length(m.xz); float star = r * (0.7 + 0.3 * cos(5.0 * a)); return (m.y > 0.4 && star < 0.42) ? uColorB : uColorA; }
    return uColorA;
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
    vec3 base = pattern(m);
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
    o = vec4(col * edge * uAlpha, edge * uAlpha);
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
void main() {
    vec2 p = (vUv - 0.5) * 2.0;
    float a;
    vec3 tint = vec3(1.0);
    if (vShape == 0) {
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
    o = vec4(vColor.rgb * tint, vColor.a) * a;
}
"""
}
