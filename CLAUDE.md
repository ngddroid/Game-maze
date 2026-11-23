# Splashy Glide — Developer & AI Guide
_Last updated: 2026-11-18 (after the move-limit pass)_

Read this first. It explains what the game is, how it is built, and how to change it safely.
It is written for both human developers and AI coding assistants.

## 1. What this is
**Splashy Glide** is an original Android puzzle game in the "slide-and-paint" genre. You swipe; the ball slides until it hits a wall and paints every tile it rolls over. You win when every tile is painted.
- **Package / applicationId:** `com.ngstudio.maze` (project folder `Maze`). This id is permanent once the app is on Google Play.
- **Content:** 5000 levels, plus a 366-level Daily Challenge pool. Levels 1–10 are hand-made tutorials with unlimited moves. From level 11 every level has a **move limit** (par + a shrinking allowance), which is what makes them properly hard. Every level is machine-verified: it is always solvable, you can never get trapped, and its verified solution fits the move limit.
- **Look:** the whole screen is one real surface material, and the maze is carved into it. World 1 is "Golden Teak": a pale golden maple with wide, soft wavy grain (`MaterialArt.grain` with `LIGHT_MAPLE`), dark-brown ink, an espresso channel and a pearl ball. The UI chrome is warm polished wood (brown buttons, pills and bars) with cream text; content cards are cream with a gold rim (`ui/theme/Theme.kt` → `Pro`). The level previews on the Home and Daily cards use a richer polished-teak tile (`board_wood.jpg`, rendered by `:tools:run --args="surfaces"`, `MiniBoard(cardWood = true)`, world 1 only). The in-game board matches it: the maze is carved into a rounded wood slab (`board_wood.jpg` in world 1, the world's own surface elsewhere) inside a cream card with a gold rim, drawn by `BAKE_FS`. The game HUD uses the same pieces as Home: `WorldBadge(small = true)`, leaves, and `QuickTile` wood tiles for Undo / Restart / Hint. Home also has code-drawn leaf clusters in all four corners (`ui/components/Decor.kt`: `TopLeaves`, and `BottomLeaves` growing up from the bottom screen corners, behind the nav bar). There are 10 worlds: Golden Teak, Marble Hall, Pastel Clay, Walnut Study, Frost Ice, Zen Sand, Jade Stone, Slate Night, Cherry Wood and Sandstone. Each world covers 250 levels, and the list repeats after level 2500.
- **Inspiration and limits:** the gameplay genre and the quality bar follow AMAZE! by CrazyLabs. **Do not copy its look**: no orange wood combined with a gold ball, no glitter tubes, no purple wave banner, and no copying of its HUD, icons, levels or name. The art, levels, sounds and icon here are all original. The fonts are OFL-licensed.
- **Monetisation:** none yet. Ads come later; see §8.

## 2. Tech stack
- **Language and build:** Kotlin 2.2.10, AGP 9.4.1, Gradle 9.6.0. minSdk 24, compile/target SDK 37, Java 11 bytecode. Requires OpenGL ES 3.0.
- **UI:** Jetpack Compose (BOM 2026.02.01) and `core-splashscreen`. The app has a single activity; `LegalActivity` shows Terms and Privacy.
- **Playfield:** a custom **OpenGL ES 3.0** renderer (`GLSurfaceView`), with the HUD drawn on top in Compose.
- **No external art or audio:** sounds and music are synthesized by `tools`; surfaces, icons and store graphics are rendered by `tools`.
- **No DI, no database.** Settings live in SharedPreferences; stars are stored in an `AtomicFile`.
- **Firebase (project `game-maze-647dc`, `app/google-services.json`):** Analytics and Crashlytics, BoM 33.7.0, google-services plugin 4.4.2, Crashlytics plugin 3.0.2 (the same versions as Ridge Riders). Firebase Cloud Messaging and Play in-app updates are covered below; these are the only network use. The manifest removes the advertising-ID permissions and turns off ad personalisation; delete those lines when real ads arrive. Release builds upload the R8 mapping file to Crashlytics (`uploadCrashlyticsMappingFileRelease`). `keepRules/rules.keep` keeps every Firebase `ComponentRegistrar` constructor: without it, R8 full mode crashed Ridge Riders' release build on launch with "FirebaseCrashlytics component is not present".
- **Gradle JDK:** use Android Studio's JBR:
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`

## 3. Modules
| Module | Kind | What it does |
|---|---|---|
| `core` | Kotlin JVM | Rules engine shared by the app and the generator: `Level`, `Physics` (the only movement code), `Analyzer` (validator), `Solver`, `Generator`, `Difficulty`, `LevelCodec`, `HandmadeLevels`. Unit tests are here. |
| `levelgen` | JVM app | Generates and verifies all levels, writes `app/src/main/assets/levels/{levels,daily}.bin` and `docs/levels_report.html`. |
| `tools` | JVM app | `audio` synthesizes `res/raw/*.wav`. `surfaces` renders `res/drawable-nodpi/surface_N.jpg`. `store` renders `docs/store/*.png` and the legacy mipmaps. |
| `app` | Android | The game itself. |

Other folders:
- **`scripts/device_qa.sh`:** automatic on-device QA (see §4).
- **`docs/IMPROVEMENT_PLAN.md`:** the self-review, what was implemented, and the backlog.
- **`docs/levels_report.html`:** the difficulty curve chart and level previews (regenerated by `levelgen`).
- **`docs/store/`:** the Play icon (512) and the feature graphic.
- **`docs/qa/`:** QA run output. It is git-ignored.

### Inside `app/src/main/java/com/ngstudio/maze/`
```
MazeApp.kt            singletons: store, levels, sound, haptics, telemetry
telemetry/            Telemetry: the only code that touches Firebase Analytics/Crashlytics (never throws)
notifications/        Notifications (channels, permission, show), DailyReminders (9 AM / 7 PM alarms + messages), ReminderReceivers
push/                 Push (FCM init, topic "all"), PushService (FirebaseMessagingService)
update/               AppUpdater (Google Play in-app updates); UI in ui/screens/UpdateOverlay.kt
MainActivity.kt       Compose back stack: Splash → Onboarding → Home(tabs) ⇄ Game / Settings
LegalActivity.kt      WebView: file:///android_asset/legal/{terms,privacy}.html (JS off, no network)
render/               GameRenderer (all GL), Shaders (all GLSL), Particles, GlUtil, GameGLView, Visuals (worlds, skins, trails, finishes)
game/                 GameController (rules-side state of a level), Hints, DebugFlags
data/                 GameStore (all saved progress + Compose state), LevelRepository (reads the .bin packs)
audio/                SoundManager (SoundPool + music crossfade), Haptics
ui/components/        design system: CandyButton, RoundButton (rounded square), Panel, GameText, EngravedText, GameIcon (code-drawn icons), MiniBoard, SurfaceBackground
ui/screens/           StudioIntroScreen (NG Studio ident), SplashScreen, OnboardingScreen (no Skip), HomeScreen + LevelsTab, SkinsTab, DailyTab, TrophiesTab, GameScreen, SettingsScreen, AdDialogs
```

## 4. Build, run, test
```bash
./gradlew :core:test                      # rules + replay of ALL 5366 stored solutions (must pass)
./gradlew :levelgen:run                   # regenerate levels (deterministic; ~7 s)
./gradlew :levelgen:run --args="sample 250"           # quick curve check, writes nothing
./gradlew :levelgen:run --args="sample 4 11 80"       # every 4th level from 11 to 80
./gradlew :levelgen:run --args="explore check"        # print analysis of the hand-made levels
./gradlew :tools:run --args="audio"       # re-synthesize all sounds/music
./gradlew :tools:run --args="surfaces"    # re-render the 10 world surface images
./gradlew :tools:run --args="store"       # Play icon 512, feature graphic, legacy mipmaps
./gradlew :app:assembleDebug              # debug APK
./gradlew :app:assembleRelease            # R8 release APK (signed with the DEBUG key for now)
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```
- **Test device:** a Realme RMX5313 (720×1600, Mali-G57, Android 16) is the reference phone. `adb shell input` works on it. adb is not on PATH, so use `~/Library/Android/sdk/platform-tools/adb`.
- **FPS:** debug builds log `MazeGL fps=…` every 3 s (`adb logcat | grep MazeGL`). The target is 60, which the Realme reaches.
- **Automatic device QA:** `scripts/device_qa.sh` (or `LEVELS="11 59 260" scripts/device_qa.sh`). It runs the core tests, builds, installs, backs up progress, autoplays each level (`--ez autoplay true`) until the `MazeQA WIN` log line, screenshots the start and the win, collects fps, fails on any crash or GL error, and restores progress. Output goes to `docs/qa/<time>/report.md` (git-ignored).
- **Open any level for QA (debug only):** opening it changes nothing, but a **win still saves** (stars, coins, next level unlocked), so back up progress before autoplaying the tester's current level. `adb shell am start -S -n com.ngstudio.maze/.MainActivity --ei debugLevel 251`. Add `--ez autoplay true` to have it play the stored solution.
- **Firebase events (`telemetry/Telemetry.kt`):**
  - Every route logs `screen_view`: studio_intro, splash, onboarding, home_<tab>, game, game_daily, settings.
  - Levels log `level_start` (level, mode, world, par, move_limit, attempt) and `level_end`. A win is `success=true` with moves, stars, coins, first_clear and hints_used; a restart, retry or Home is `success=false` with a reason and painted_pct. Also `out_of_moves` and `hint_used`.
  - The economy logs `spend_virtual_currency` (hint, moves_5, <tab>_<id> skins) and `earn_virtual_currency` (level, daily, calendar, achievement, ad_x2), plus `rewarded_ad` (placement), `unlock_achievement`, and `tutorial_begin` / `tutorial_complete`.
  - Crashlytics custom keys: version, build_type, last_screen, level, mode, with breadcrumbs for screens and level starts.
  - Collection is on in debug builds too (`build_type` tells them apart).
  - To watch events live, run `adb shell setprop debug.firebase.analytics.app com.ngstudio.maze` and open Firebase → DebugView. Turn it off with `.none.`.
  - Crashlytics was verified with a test crash on 2026-11-23 (the report arrived in the console). The test-crash tool was then removed.
- **Debug log markers:** `MazeQA START level=… par=…` and `MazeQA WIN level=… moves=… stars=…` (used by the QA script).
- **Keep the tester's progress safe:** back it up before `pm clear` with `adb exec-out run-as com.ngstudio.maze tar cf - shared_prefs files > progress.tar`, and restore it with `cat progress.tar | adb shell run-as com.ngstudio.maze tar xf -`.
- **Debug tools (debug build only, in Settings):**
  - Autoplay: plays each level's stored solution automatically.
  - Unlock all 5000.
  - +1000 coins.
  - Reset progress.

## 5. Levels — rules, verification, difficulty
- **Rules (`core/Physics.kt`):**
  - The ball moves until the next cell is a wall, the grid edge, or a one-way gate pointing another way.
  - A **stopper** stops the ball on that cell.
  - **Deflectors** (`/` and `\`) turn it 90°.
  - **Portals** move it to the partner portal, keeping its direction.
  - Slides that loop forever are rejected.
- **The validator (`Analyzer`):** builds the graph of resting positions reachable from the start. A level is valid when there is **no loop**, the graph is **strongly connected** (you can never be trapped), and **all floor cells are covered** by some swipe. Together these prove the level can be finished from any position the player can reach.
- **The solver:** exact BFS over (node, painted groups) for small levels. For large levels, 24 randomized greedy runs plus a beam search. The solution found is stored in the pack and gives **par**.
- **Move limit (`Difficulty.moveLimit` / `slack`):**

  | Levels | Moves allowed |
  |---|---|
  | 1–10 | unlimited |
  | 11–30 | par + 5 |
  | 31–100 | par + 4 |
  | 101–300 | par + 3 |
  | 301+ | par + 2 |
  | Daily (`dailyMoveLimit`) | par + 3 |

  Tune `slack()` after playtests; this needs no regeneration.
- **Stars (`GameController.starsFor`):** 3★ for moves ≤ par; 2★ up to halfway between par and the starting limit; otherwise 1★.
- **Hint (`game/Hints.kt`):**
  - While your moves match the stored solution, the hint is its next move, so following hints always finishes at par.
  - Off that route, the hint is a greedy finish.
  - If that greedy finish no longer fits the moves left, the hint says **undo** (no hint is spent).
- **The generator:** "reverse carving" (it carves the slides of a virtual ball and locks the wall that stops each one). It then adds special tiles by mutation, applies a quality gate (`looksGood`: floor density ≥ 38%, no 5×5 solid wall block, portrait-friendly shape), validates, solves and scores.
- **Difficulty (`Difficulty.kt`):** the score D (0–1) combines par, floor size, confusion (how many moves a random swiper needs, divided by par), irreversible moves, branching and special tiles.
  - Weights: par 0.26, floor 0.14, **confusion 0.30**, **irreversible 0.18**, branching 0.04, specials 0.08.
  - The target curve is `0.52 + 0.33·base(n)`. `base` climbs fast until level 2000, then rises slowly to 5000 (never flat). A small sawtooth sits within each block of 10, where every 10th level is a "boss".
  - **Current averages (made much harder at the user's request):**

    | Levels | Average par | Average tiles |
    |---|---|---|
    | 1–250 | ≈19 | ≈56 |
    | 251–500 | ≈24 | ≈71 |
    | 751–1000 | ≈32 | ≈90 |
    | 2000+ | ≈37–39 | ≈100 (12×17) |

    Example: level 59 is 10×17, 67 tiles, par 19, 23 moves allowed.
  - New tiles arrive at L21 (stopper), L41 (one-way), L71 (deflector) and L111 (portal), each with a coach-mark tip and 3 gentler levels.
- **Never edit the .bin files by hand.** Change the code, run `:levelgen:run`, then `:core:test`. `ShippedLevelsTest` replays every stored solution and checks that:
  - every move limit fits its solution;
  - every 250-level bucket is at least as hard as the one before;
  - no 50-level stretch dips by more than 0.02.
- **To change a tutorial level:** edit the ASCII in `core/HandmadeLevels.kt` (`#` wall, `.` floor, `S` start, `o` stopper, `^>v<` one-way gates, `/` `\` deflectors, `P`/`Q` portal pairs). Then regenerate and test.

## 6. Rendering (render/)
- **One frame, in draw order:**
  1. The surface image (the world's `surface_N.jpg`, centre-cropped).
  2. The baked board (`BAKE_FS`, rendered once per level into an FBO): a soft shadow, a cream card with a gold rim (`uCard`), and a rounded wood slab (`uSlab`, 0.55 cell around the grid; `board_wood` + warmer channel browns in world 1, `uWoodOn`); carved channels with a lit bevel; walls sit **exactly on the grid** (every wall is 1 cell thick); the wall's inner face shows as one slim band (`HF = 0.13` cell) inside the channel just below each wall; a dark floor with a thin tile grid, AO (from `depth()`, the exact distance to the nearest wall, so tiles never look "quilted") and an inner shadow from the light at the top-left.
  3. Paint (`PAINT_FS`): a per-cell RGBA32F texture holding (progress, direction, start time, order). The wet front follows the ball, then the paint dries; the shader adds the tile grid and the win shine. A pixel in a floor tile only takes that tile's paint (no bleeding into the unpainted neighbour).
  4. Special tiles (`SPECIAL_FS`).
  5. The hint.
  6. The ball shadow, then the ball (`BALL_FS`: sphere impostor, rolling-pattern rotation matrix, studio reflection, gloss and metal).
  7. Board particles, then screen confetti.
- **Slide animation (`GameRenderer.updateMove`):** each slide is timed as a whole, `0.09 + 0.12·√cells` s (×0.6 for undo), so long slides stay snappy. The ball launches over the first 22% (`LAUNCH`), then hits the wall at full speed. It stretches with speed, leaves 3 fading motion-blur ghosts, then squashes and recoils off the wall (`impactDx/Dy`). Portal jumps pause the slide clock.
- **Board animation:** the board is part of the surface, so it never moves. Intro = the channels fade in ("carve"), then the ball drops in. Outro = fade out. A win sends a ring of light rippling out from the ball across the paint (`uShine` = ring radius, `uWaveOrigin`), with a sparkle popping on each tile it passes. The ball does two happy hops, confetti fires at 0.3 s, and the result panel opens at `WIN_PANEL_AT` (1.25 s).
- **The surface is an image, not GLSL noise.** Procedural noise on the GPU showed precision seams on Mali. The images are rendered on the CPU by `tools/MaterialArt.kt` (materials: wood, marble, clay, ice, sand, stone, `grain()` with `LIGHT_MAPLE` for world 1 (`HONEY_OAK` kept, unused), `burl()`, and `veneer()`/`planks()`, which are kept but currently unused). `:tools:run --args="surface-preview out.png"` renders a side-by-side sheet of candidate woods with a mock maze, and the menus and the MiniBoard previews use the same images, so everything matches.
- **To add or change a world:**
  1. Edit `WorldTheme.ALL` in `render/Visuals.kt` (material, colours, floor, paints, accent, ink).
  2. Make the same change in the `worlds` list in `tools/MaterialArt.kt`.
  3. Run `:tools:run --args="surfaces"`.
  4. If you add a world (rather than change one), also add its `surface_N` to `SURFACE_RES` (`GameRenderer.kt`) and `SURFACES` (`Components.kt`).
- **GLSL gotchas:**
  - `cast` (and other reserved words) do not compile on Mali; check logcat `MazeGL` for compile logs.
  - Load a texture *before* binding the grid texture, because `GlUtil.linear()` binds to the active unit.
  - All shaders output premultiplied alpha.

## 7. Screens and flow
- **Launch:** the system splash is plain white with a blank icon (`ic_splash_blank`, `Theme.Maze.Splash`). It flows straight into the **NG Studio intro** (`ui/screens/StudioIntroScreen.kt`, route `StudioIntro`, every launch, about 3.9 s, skippable by a tap after 1.2 s, no music). In the intro, the N and G fly in and collide, the orbit swoosh sweeps round with a comet head, the two stars pop, a shine passes, and STUDIO and "PLAY • CREATE • INSPIRE" settle in. It is copied exactly from the studio's Ridge Riders project (`Game-BikeRace`), including the layer images `res/drawable-nodpi/ngi_*.png`, cut from the logo by `tools/studio_intro/{seg,split}.py` (the source logo is not in this repo). It cross-fades over 600 ms into the **Splash**: an animated logo while the level packs load. `--ei debugLevel` skips both.
- **Onboarding:** first launch only. 3 pages, Next only (**no Skip**). It goes straight into Level 1, which shows a tutorial hand; Home sits under it in the back stack.
- **Home tabs:** Skins | Levels | **Play** (raised centre) | Daily | Trophies, plus a gear button that opens Settings.
- **Game:**
  - **Move limit** from level 11 (`Difficulty.moveLimit`): par + 5 for L11–30, par + 4 to 100, par + 3 to 300, par + 2 after that, and par + 3 for the Daily. Tutorials are unlimited.
  - **HUD:** the level, a paint bar, a MOVES-left pill (red at 3 or fewer), star targets and par.
  - **Buttons:** Undo (free; gives the move back), Restart (free) and Hint (count; buy for 60 coins, or a free one via Watch Ad in debug).
  - **Out of moves** panel: Undo, Retry, +5 moves for 80 coins, or +5 via Watch Ad (debug).
  - **Hints** follow the stored best route; when you're off it and the moves left are too few, the hint says to undo.
  - **Stuck nudge:** after 15 s without a move (from level 11 on), the Hint button pulses with a "Stuck? Try a hint" tip.
  - **Pause (`PauseOverlay`):** deliberately unlike the win screen; it uses the Home look. Over a light warm dim, a cream `Panel` with a gold rim drops in from the top with a small swing, and a wood "PAUSED" plaque pops onto its top edge. Inside: "LEVEL N • WORLD", the paint bar with painted % and tiles left, cream `InfoChip`s for MOVES LEFT (pink at 3 or fewer) and PAR, `WoodToggle` tiles for Sound / Music / Vibrate (coloured disc when on, grey slashed disc when off), RESUME (green), and `WoodButton`s for Restart / Home. Tapping outside the card resumes. To screenshot it on a phone that blocks injected input, send the app Home and bring it back (going to the background pauses the game).
  - **Also:** coach marks for new tiles.
- **Notifications (ported from Ridge Riders):**
  - **Daily reminders** at 9:00 and 19:00 device time (`DailyReminders`). There are 7 morning and 7 evening game-themed messages that rotate by day of year. Alarms are one-shot and re-arm when they fire, on every launch, and after a reboot, an app update or a clock change (`ReminderBootReceiver`). Android 12+ uses a 10-minute window (no exact-alarm permission). They are skipped while the game is on screen. There is no in-game toggle (removed at the user's request): reminders are always armed, and players mute them in Android's notification settings (channel "Daily reminders"). Covered by `app/src/test/.../DailyRemindersTest`.
  - **Push (FCM):** every install subscribes to the topic `all`. Debug builds log `MazePush: FCM token: …` for Firebase console > Messaging > "Send test message". Background notification messages use the manifest's default icon (`ic_stat_notify`), colour (`notify_accent`) and channel (`news_events`). Foreground and data-only messages (keys `title` and `body`) are posted by `PushService`.
  - **Permission:** on Android 13+, the app asks once per launch on Home until it is granted.
  - **Events:** `reminder_shown`, `notification_tap` (source), and `notif_permission_granted/denied`.
- **In-app updates (`AppUpdater` + `UpdateOverlay`):**
  - About 2.5 s after reaching Home, Play is asked for an update. A normal release shows our cream "UPDATE AVAILABLE" dialog (UPDATE / LATER; LATER snoozes that version for 3 days). The flexible download then shows a top banner with progress, then "Update ready → RESTART".
  - Play update priority ≥ 4 (set per release through the Play Developer API `inAppUpdatePriority`) forces Play's full-screen immediate update. If the player backs out of it, it opens again on the next resume until installed.
  - **Testing it:** only a build installed from Google Play (for example internal testing) can see an update. Upload a higher `versionCode` to the same track.
  - The UI shows only on Home and Settings. Only installs from Google Play (including internal testing) see updates; sideloaded and debug builds just see none.
- **Settings:** sound/music/vibration toggles, Rate, Share, **Terms of Use**, **Privacy Policy** (themed HTML in `assets/legal`), and debug tools.
- **Win screen (`LevelComplete`):** no card. Over a dark warm backdrop that also fades the game HUD out, a staged entrance plays. First the `BouncyTitle` letters drop in one by one, with a glint sweeping across every 2.8 s. The colour follows the result (`TitleColors.forStars`): cream with a teak outline for PERFECT (3★), lime green for GREAT (2★), ice blue for NICE (1★). PAUSED keeps the gold. Then the big glossy `GloryStar`s stamp in one by one in an arc, each with a sparkle burst, a halo and its own glint, over two counter-rotating ray layers and blinking twinkles; the middle star breathes, and empty stars are dark slots. Then a dark results card with a gold rim rises in, with "LEVEL N COMPLETE" between gold rules and `StatCell`s for MOVES | PAR | COINS (the coins count up with a spinning coin and pop at the end); streak, chest and daily `BonusLine`s go in its footer. Floating buttons rise from the bottom: x2 COINS (orange, debug ad), NEXT (green) and `GlassRound` Replay / Home. It has stars, coins that fly into the HUD counter, a perfect-streak bonus (+5 per consecutive 3★ after the first, max +25), the chest, and ×2 coins via Watch Ad (debug).
- **Skins:** tap any item to preview it live. The action bar offers Equip, Buy (coins) or Watch Ad (debug; a free unlock).
- **Economy (`GameStore`):**
  - Level: +10 coins, plus 5 for each star above one (first clear).
  - Chest every 10 levels: 50–100 coins and +1 hint.
  - Daily: 50 coins plus a streak bonus.
  - 7-day calendar: 20, 30, 40, 50, 75, 100, then 200 + a hint.
  - 23 achievements.
  - Shop: 16 balls, 8 trails, 6 paint finishes.

## 8. Lock rules and ads
- **Both builds:** a fresh install has only level 1 open, and winning level N opens N+1 (`GameStore.completeLevel`).
- **Debug:** tapping a locked level opens the **Watch Ad** dialog; a fake 3-second ad (`FakeAdOverlay`, a full-screen dialog) unlocks **that** level (`unlockSingle`). The same fake ad is used for:
  - shop unlocks;
  - ×2 coins;
  - a free hint;
  - +5 moves.
  All are hidden in release while `Ads.rewarded.available` is false.
- **Release:** a locked tap shakes the bubble and shows "Finish level N first".
- **Adding real ads:** implement `RewardedAds` in `ui/screens/AdDialogs.kt` with the ad SDK (e.g. Apero/AdMob, as in the team's other apps), and set `Ads.rewarded` for release. The dialog UI stays the same. Cap interstitials: the reference's top review complaint is "an ad after every level". If the Play audience includes children, Families policy applies.

## 9. Conventions
- **Movement:** all of it goes through `core/Physics.slide`. Never re-implement it in the app.
- **New levels or tiles:** extend `Tile`, `Physics`, `Analyzer`/`Generator`, the codec (4-bit tiles; 10–15 are free), the renderer `SPECIAL_FS` and the `CoachMark` text. Then regenerate and test.
- **UI:** Compose only. Use the components in `ui/components`. Titles on the surface use `EngravedText(theme.ink)`; buttons use `CandyButton`/`RoundButton`.
- **Strings:** English only for now (hard-coded in Compose). Move them to `strings.xml` before localising.
- **Sounds:** looked up by name (`SoundManager`); `res/raw/keep.xml` keeps them through R8.
- **Generated outputs:** do not hand-edit them (`levels/*.bin`, `res/raw/*.wav`, `surface_*.jpg`, `board_wood.jpg`, `ngi_*.png`, mipmap PNGs, `docs/store`). Regenerate them with the tools instead.
- **R8:** use plain `optimization { enable = true }`. The template's `packageScope` crashed release at start.

## 10. Before publishing (open items)
1. **Legal pages are complete.** The support email is `store.ngdtechnolab@gmail.com`. Host the privacy policy at a public URL for the Play listing.
2. **Signing:** create an upload keystore and set it in `app/build.gradle.kts`; release currently uses the debug key.
3. **Data safety form:** declare what Firebase collects: app interactions, crash logs and diagnostics, and device or other IDs (the app-instance ID and the FCM token; not the advertising ID). It is not shared, it is processed by Google, it is encrypted in transit, and it can't be deleted on request in-app. The privacy policy (`assets/legal/privacy.html`) has an "Analytics and crash reports", "Notifications" and "App updates" sections, and the terms have Updates, Notifications and Third-party services (all updated 23 Nov 2026). Both end with a **"Contact us"** card: a question line with the email, then the developer details (NGENDEV TECHNOLABS LLP (NGD Technolab), the Rajkot address, the phone as a `tel:` link, and `store.ngdtechnolab@gmail.com` as a `mailto:` link; `LegalActivity` opens these in the dialer and email app). Below it, outside the cards and on the page background, is a centred **"PROVIDED BY"** footer (`.provided`): the spaced-out label over the NG Studio logo (`assets/legal/ng_studio_logo.png`, a cut-out of `docs/store/ng_studio_logo.png` made by `tools/studio_intro/CutLogo.java`, which removes the image's baked-in checkerboard). (The governing-law line was removed from the terms at the user's request.) Also complete the content rating questionnaire.
4. **Real ads** (see §8), then update the privacy policy with the ad partners.
5. **Name:** re-check "Splashy Glide" on Play and for trademarks just before publishing. It had no exact match on 2026-11-18.
6. **Store assets** are in `docs/store` (icon_512, feature_graphic). Screenshots are still needed.

## 11. Progress log (all on 2026-11-18)
1. **Built from the template:**
   - the core engine, verified levels, the generator and tests;
   - the OpenGL renderer, game screen, all menus, synthesized audio, icon and legal pages.
2. **Art direction changed** at the user's request: the candy floating island became a **carved full-screen surface** (10 materials), with cleaner menus on the same surfaces.
3. **Polish pass:**
   - The user found the light oak "milky" and walls looked 1.5 cells thick. Walls are now exactly 1 cell, with one slim inner face.
   - No paint bleeding, no quilted tiles, brighter paint.
   - Professional espresso/cream/gold chrome.
   - Lighter Slate Night; the tile tip moved off the board.
4. **Second pass:**
   - World 1 became a **teak/walnut veneer**, procedural, matching a reference photo the user sent; the photo itself is not used.
   - Levels 11+ were made much harder: new curve and weights, earlier tiles.
5. **Third pass:**
   - Skins: live preview of any item, plus Watch Ad unlock (debug).
   - Wrote `docs/IMPROVEMENT_PLAN.md` and built it: ×2 coins, free hint by ad, the stuck nudge, perfect streak, coin flight, accessibility labels, and `scripts/device_qa.sh`.
   - **Move limits** with the Out of moves panel, and a steeper curve.
   - The teak launch screen, icon and store art (no more pink).
6. **Verification on the Realme (debug and release):**
   - splash, onboarding, home and all tabs, Settings and Terms/Privacy;
   - the debug Watch Ad unlocks (level, shop, +5 moves) and the release "Finish level N first";
   - every world;
   - the QA script autoplayed and won 16 levels across all worlds, then 5 with move limits, at 58–60 fps.
   - All 13 core tests pass.
7. **Fixes along the way:**
   - The start cell was not painted (the ball-landing animation overshot).
   - Seams in GPU wood: switched to surface images.
   - The R8 release crash.
   - The surface texture replaced the grid texture on unit 0.
   - A level could time out in QA when someone tapped the phone mid-run.

8. **2026-11-23:** the user disliked the teak veneer. Five candidates were rendered (walnut, honey oak, mahogany, smoked ash, burl) and the user picked Honey Oak, then sent a mockup and the home screen was redesigned to match it: pale wavy maple (`Material.MAPLE = 9`, name back to "Golden Teak", dark-brown ink), warm wood chrome, gold-rimmed cards, lime-green main action, corner leaves (`Decor.kt`; a wooden shelf with striped balls under the nav bar was tried and removed at the user's request). A second pass matched the user's detail crops: raised cream `WorldBadge`, glossy pill `CandyButton` with a rim, gold double-rim `Panel`, the rich-teak card board, and `PlayOrb` (green dome in a wood ring) for the centre nav button.

## 12. Next session
- **Ask the user how the move limits feel:** if too hard or too easy, adjust `Difficulty.slack()` only.
- **Before publishing:** the §10 items, which are the upload keystore, the hosted privacy URL, the Data safety form and store screenshots.
- **Then:**
  - real ads via `Ads.rewarded` (keep interstitials capped);
  - localisation;
  - cloud save.

  See the backlog in `docs/IMPROVEMENT_PLAN.md`.
- **Testing rule:** the user often plays on the Realme while you test. Prefer `--ei debugLevel` (no progress change), and back up and restore progress around anything that writes saves (`scripts/device_qa.sh` does this).
