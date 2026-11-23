# Maze Brush — Improvement Plan
_Written 2026-11-18 as a self-review of the game. "Now" items are implemented in this pass; "Later" items are the backlog._

## How I reviewed it
- Played the flow on the Realme: first launch, the tutorial levels, levels 11–55, and one level from each world.
- Checked against what top casual puzzle games do well:
  - fast feedback;
  - rewarding wins;
  - fair help when stuck;
  - cosmetics you can try before buying;
  - rewarded ads only where the player opts in.
- Read the reference game's Play reviews. The top complaints were an ad after every level, levels that trap you, and a level that would not advance. Maze Brush already avoids all three by design (no forced ads, verified levels).

## What is already strong (keep)
- **Verified levels:** every level is solvable and can never trap you, and its stored solution is replayed in tests.
- **Visuals:** a carved real-wood playfield, 10 material worlds and 60 fps on a mid-range Mali GPU.
- **Help and progress:** Undo and Restart are free, Hint uses a real path search, stars are measured against par, and there are chapter chests, a daily challenge with streaks, a reward calendar and 23 achievements.

## Gaps found
| # | Gap | Why it matters |
|---|---|---|
| G1 | Shop items can't be tried before buying, and the preview only shows the equipped ball | Players buy cosmetics they can see; preview drives purchases |
| G2 | No way to get a locked skin without coins (and no ad hook to test) | Rewarded ads are the main income for casual games |
| G3 | Win panel has no "double coins" option | Standard, opt-in, high-value rewarded placement |
| G4 | When out of hints, the only option is coins | Stuck players without coins quit |
| G5 | Nothing helps a player who is stuck and not moving | Silent frustration means churn |
| G6 | 3-star play has no extra reward | Skilled players need a reason to replay and aim for par |
| G7 | Coins jump up instantly when you win | It feels flat; coins should fly into the counter |
| G8 | Icon buttons have no accessibility labels | Play pre-launch report flags them; screen readers can't use the game |
| G9 | No repeatable device test | Every change needs the same manual checks |

## Now (implemented in this pass)
1. **Shop preview (G1).**
   - Tapping any ball, trail or paint previews it live in the 3D preview, owned or not.
   - An action bar shows **Equip**, **Buy (coins)** or, where rewarded ads are available, **Watch Ad**.
2. **Watch Ad unlock in the shop (G2).**
   - The debug build uses the fake 3-second ad; it unlocks the item and equips it.
   - The release build hides the button until real ads are added.
3. **Double coins (G3).**
   - The win panel shows **WATCH AD ×2** after the coins land (debug: fake ad).
   - It can be used once per win.
4. **Free hint by ad (G4).** The "Need a hint?" dialog gets **WATCH AD — FREE HINT** (debug).
5. **"Stuck?" nudge (G5).** From level 11 on, after 15 s without a move, the Hint button pulses and a small "Stuck? Try a hint" tip appears. It resets on any move.
6. **Perfect streak (G6).**
   - Each 3★ win in a row adds a bonus: +5 coins per step after the first, up to +25.
   - Any win below 3★ resets the streak.
   - The win panel shows "Perfect streak ×N".
7. **Coin flight (G7).** Coins fly from the win panel into the coin counter, and the counter rolls up as they land.
8. **Accessibility (G8).** Every icon button and candy button gets a screen-reader label and the Button role.
9. **Device QA script (G9):** `scripts/device_qa.sh` (see below).
10. **Real difficulty: move limits.** The user found level 59 "still easy": with unlimited moves you can wander until everything is painted. Rules now:
    - From level 11, each level allows **par + slack** moves: +5 (L11–30), +4 (to 100), +3 (to 300), +2 after that. The Daily allows par + 3. Tutorials 1–10 stay unlimited.
    - The HUD shows MOVES left, which turns red at 3 or fewer.
    - Running out opens **Out of moves**: Undo, Retry, +5 moves for 80 coins, or +5 moves via Watch Ad (debug).
    - Stars: 3★ at par or fewer moves; 2★ up to halfway between par and the limit; 1★ otherwise.
    - Hints follow the stored best route. When you're off it and the greedy finish no longer fits, the hint says **undo** (free).
    - The level curve was raised again (`CURVE_START 0.52`): levels 1–250 average par 19 on 56 tiles.
11. **Launch screen and icon:** the old candy-pink system splash is now deep teak, and the launcher icon and store art moved to the teak look.

**Result of this pass:** `scripts/device_qa.sh` autoplayed 16 levels from every world, then 5 levels with move limits. All won end to end at 58–60 fps.

## Later (backlog, in priority order)
0. **Tune the move slack with real players:** if too many fail, raise `Difficulty.slack()` for a band; if too few, lower it. No level regeneration is needed.
1. **Real ads:**
   - Rewarded ads for the shop, ×2 coins, free hint and level unlock (hooks exist: `Ads.rewarded`).
   - Interstitials capped at no more than one every 3 levels or 3 minutes, and never after level 1–10.
   - Update the privacy policy and the Data safety form.
2. **Localisation:** move the hard-coded Compose strings to `strings.xml`, then translate.
3. **Cloud save:** Play Games sign-in, or an export/import code, so progress survives a reinstall.
4. **Daily reminder notification:** needs the `POST_NOTIFICATIONS` permission and an opt-in screen.
5. **New mechanics for 3000+:** breakable tiles, paint-colour switches, timed gates. Each needs `Physics` + `Analyzer` + generator + shader + coach mark.
6. **Seasonal worlds** (e.g. winter, festival) as limited-time daily challenge themes.
7. **Remote config** for economy numbers (coin rewards, hint price, ad caps) once Firebase is added.
8. **Tablet / landscape layout** (the game is portrait-locked today).

## How to keep improving (the loop)
1. Change the code.
2. Run `scripts/device_qa.sh`. It:
   - runs the core tests;
   - builds and installs the debug app;
   - backs up the tester's progress;
   - **autoplays** sample levels from every world and screenshots the start and the win;
   - logs fps;
   - fails on any crash or GL error;
   - restores the progress.
   Screenshots and a report go to `docs/qa/<timestamp>/`.
3. Look at the screenshots and the fps lines, fix what's wrong, and repeat.
4. **Level difficulty:** run `./gradlew :levelgen:run --args="sample 4 11 80"` to inspect, change `Difficulty.kt`, regenerate with `:levelgen:run`, and run `:core:test`.
