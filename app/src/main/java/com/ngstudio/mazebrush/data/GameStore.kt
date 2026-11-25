package com.ngstudio.mazebrush.data

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ngstudio.mazebrush.render.BallSkin
import com.ngstudio.mazebrush.render.BoardTheme
import com.ngstudio.mazebrush.render.PaintFinish
import com.ngstudio.mazebrush.render.TrailStyle
import java.io.File
import java.util.TimeZone

/**
 * All saved progress and settings. Values are Compose state, so screens update automatically.
 * Scalars live in SharedPreferences ("maze"); the 5000 star values in files/stars.bin (AtomicFile).
 *
 * Lock rule (debug AND release): only level 1 is open on a fresh install; winning level N opens N+1.
 * Levels unlocked one by one (debug Watch Ad dialog; later a real rewarded ad) are kept in [extraUnlocked].
 */
/** First-time wins needed to fill the New Item bar. */
const val NEW_ITEM_EVERY = 5
/** Debug test builds start fully unlocked (set false for a locked debug build). Never affects release. */
const val DEBUG_UNLOCK_ALL = false

class GameStore(context: Context) {
    private val prefs = context.getSharedPreferences("maze", Context.MODE_PRIVATE)
    private val starsFile = AtomicFile(File(context.filesDir, "stars.bin"))
    private val stars = ByteArray(LevelRepository.TOTAL)

    var onboardingDone by mutableStateOf(prefs.getBoolean("onboarding_done", false)); private set
    var sound by mutableStateOf(prefs.getBoolean("sound", true)); private set
    var music by mutableStateOf(prefs.getBoolean("music", true)); private set
    var vibration by mutableStateOf(prefs.getBoolean("vibration", true)); private set

    var maxUnlocked by mutableIntStateOf(prefs.getInt("max_unlocked", 1)); private set
    var extraUnlocked by mutableStateOf(prefs.getStringSet("extra_unlocked", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()); private set
    var coins by mutableIntStateOf(prefs.getInt("coins", 50)); private set
    var hints by mutableIntStateOf(prefs.getInt("hints", 3)); private set
    /** Bumped whenever stars change, so level grids recompose. */
    var starsVersion by mutableIntStateOf(0); private set

    var ballSkin by mutableIntStateOf(prefs.getInt("ball", 0)); private set
    var trail by mutableIntStateOf(prefs.getInt("trail", 1)); private set
    var finish by mutableIntStateOf(prefs.getInt("finish", 0)); private set
    var ownedBalls by mutableStateOf(intSet("owned_balls", setOf(0))); private set
    var ownedTrails by mutableStateOf(intSet("owned_trails", setOf(0, 1))); private set
    var ownedFinishes by mutableStateOf(intSet("owned_finishes", setOf(0))); private set
    /** Board colour theme (shop "Themes" tab). */
    var boardTheme by mutableIntStateOf(prefs.getInt("board", 0)); private set
    var ownedBoards by mutableStateOf(intSet("owned_boards", setOf(0))); private set
    /** New Item progress: first-time wins towards the next free item (0 until [NEW_ITEM_EVERY]). */
    var itemProgress by mutableIntStateOf(prefs.getInt("item_progress", 0)); private set
    /** Paint colour (shop Paint tab, [com.ngstudio.mazebrush.render.PaintColor]). A saved 0 (the old "Auto") counts as Coral. */
    var paintColor by mutableIntStateOf(prefs.getInt("paint_color", com.ngstudio.mazebrush.render.PaintColor.DEFAULT).let { if (it == 0) com.ngstudio.mazebrush.render.PaintColor.DEFAULT else it }); private set

    // stats
    var tilesPainted by mutableLongStateOf(prefs.getLong("tiles_painted", 0)); private set
    var swipes by mutableLongStateOf(prefs.getLong("swipes", 0)); private set
    var perfects by mutableIntStateOf(prefs.getInt("perfects", 0)); private set
    var hintsUsed by mutableIntStateOf(prefs.getInt("hints_used", 0)); private set

    // daily
    var dailyDoneDay by mutableLongStateOf(prefs.getLong("daily_done_day", -1)); private set
    var dailyStreak by mutableIntStateOf(prefs.getInt("daily_streak", 0)); private set
    var bestDailyStreak by mutableIntStateOf(prefs.getInt("best_daily_streak", 0)); private set
    var dailiesDone by mutableIntStateOf(prefs.getInt("dailies_done", 0)); private set
    var calendarClaimDay by mutableLongStateOf(prefs.getLong("calendar_claim_day", -1)); private set
    var calendarIndex by mutableIntStateOf(prefs.getInt("calendar_index", 0)); private set
    var claimedAchievements by mutableStateOf(prefs.getStringSet("achievements", emptySet())!!.toSet()); private set
    var chestsOpened by mutableIntStateOf(prefs.getInt("chests", 0)); private set
    /** Consecutive 3-star campaign wins (bonus coins). */
    var perfectStreak by mutableIntStateOf(prefs.getInt("perfect_streak", 0)); private set

    init {
        if (starsFile.baseFile.exists()) runCatching {
            val b = starsFile.readFully()
            System.arraycopy(b, 0, stars, 0, minOf(b.size, stars.size))
        }
        // Test builds (debug only): unlock everything once on first launch so testers see all levels and skins.
        // Release is never affected (BuildConfig.DEBUG is false). To ship a locked debug build, flip DEBUG_UNLOCK_ALL to false.
        if (com.ngstudio.mazebrush.BuildConfig.DEBUG && DEBUG_UNLOCK_ALL && !prefs.getBoolean("debug_unlocked_all", false)) {
            debugUnlockAll()
            prefs.edit().putBoolean("debug_unlocked_all", true).apply()
        }
    }

    private fun intSet(key: String, def: Set<Int>): Set<Int> =
        prefs.getStringSet(key, null)?.mapNotNull { it.toIntOrNull() }?.toSet() ?: def

    private fun putIntSet(key: String, v: Set<Int>) = prefs.edit().putStringSet(key, v.map { it.toString() }.toSet()).apply()

    // ---------------------------------------------------------------- settings
    fun setOnboardingDone() { onboardingDone = true; prefs.edit().putBoolean("onboarding_done", true).apply() }
    fun toggleSound() { sound = !sound; prefs.edit().putBoolean("sound", sound).apply() }
    fun toggleMusic() { music = !music; prefs.edit().putBoolean("music", music).apply() }
    fun toggleVibration() { vibration = !vibration; prefs.edit().putBoolean("vibration", vibration).apply() }

    // ---------------------------------------------------------------- levels
    /**
     * Rewarded-ad unlocks (single levels, free skins) are only honoured while rewarded ads exist: today that is
     * the debug build's fake ad. In release there are no ads yet, so levels open strictly one by one by winning
     * the previous level, and any ad unlocks saved by a debug build are ignored.
     */
    private val adUnlocks: Boolean get() = com.ngstudio.mazebrush.ui.screens.Ads.rewarded.available

    fun isUnlocked(n: Int) = n <= maxUnlocked || (adUnlocks && n in extraUnlocked)
    fun starsOf(n: Int): Int = stars.getOrElse(n - 1) { 0 }.toInt()
    fun isCompleted(n: Int) = starsOf(n) > 0
    val completedCount: Int get() { starsVersion; return stars.count { it > 0 } }
    val totalStars: Int get() { starsVersion; return stars.sumOf { it.toInt() } }

    /** The level the big PLAY button opens: the first unlocked level not finished yet (the frontier). */
    val currentLevel: Int get() = maxUnlocked.coerceAtMost(LevelRepository.TOTAL)

    /** The newest level the player can open: won up to, or unlocked one-off by an ad. Picks the menu background. */
    val latestUnlocked: Int get() = maxOf(maxUnlocked, if (adUnlocks) extraUnlocked.maxOrNull() ?: 0 else 0).coerceAtMost(LevelRepository.TOTAL)

    /** Unlock one specific level (debug Watch Ad / rewarded ad). */
    fun unlockSingle(n: Int) {
        if (!adUnlocks || isUnlocked(n)) return
        extraUnlocked = extraUnlocked + n
        prefs.edit().putStringSet("extra_unlocked", extraUnlocked.map { it.toString() }.toSet()).apply()
    }

    class Reward(
        val coins: Int, val newStars: Int, val firstClear: Boolean, val chest: Int, val streak: Int = 0, val streakBonus: Int = 0,
        /** The item the New Item bar just unlocked, if it filled up on this win. */
        val newItem: ShopItem? = null,
    )

    /** A ball skin or board theme, as offered by the New Item bar. */
    data class ShopItem(val kind: Kind, val id: Int) {
        enum class Kind { BALL, BOARD }
        val name: String get() = if (kind == Kind.BALL) BallSkin.byId(id).name else BoardTheme.byId(id).name
        val price: Int get() = if (kind == Kind.BALL) BallSkin.byId(id).price else BoardTheme.byId(id).price
    }

    /** The next item the New Item bar will unlock: the cheapest ball not owned yet (null when all are owned). Board themes follow the level, so they are never prizes. */
    val nextNewItem: ShopItem?
        get() = BallSkin.ALL.filter { it.id !in ownedBalls }.minByOrNull { it.price }?.let { ShopItem(ShopItem.Kind.BALL, it.id) }

    /** Records a win and returns the coins earned. */
    fun completeLevel(n: Int, earnedStars: Int, tiles: Int, moves: Int): Reward {
        val old = starsOf(n)
        val first = old == 0
        var coinsEarned = when {
            first -> 10 + 5 * (earnedStars - 1)
            earnedStars > old -> 5 * (earnedStars - old)
            else -> 0
        }
        var chest = 0
        if (first && n % 10 == 0) { chest = (50 + (n / 100) * 5).coerceAtMost(100); chestsOpened++ }
        coinsEarned += chest
        if (earnedStars > old) {
            stars[n - 1] = earnedStars.toByte()
            starsFile.startWrite().let { out ->
                try { out.write(stars); starsFile.finishWrite(out) } catch (e: Exception) { starsFile.failWrite(out) }
            }
            starsVersion++
        }
        if (earnedStars == 3 && old < 3) perfects++
        // perfect streak: +5 coins per consecutive 3-star win after the first, max +25
        perfectStreak = if (earnedStars == 3) perfectStreak + 1 else 0
        val streakBonus = if (perfectStreak >= 2) (5 * (perfectStreak - 1)).coerceAtMost(25) else 0
        coinsEarned += streakBonus
        // unlock the next level
        if (n < LevelRepository.TOTAL) {
            if (n >= maxUnlocked) {
                // Frontier moves on; single unlocks at or below it are no longer needed.
                maxUnlocked = n + 1
                extraUnlocked = extraUnlocked.filter { it > maxUnlocked }.toSet()
            } else if (adUnlocks && !isUnlocked(n + 1)) {
                // Won a level that was unlocked on its own: open the one after it too.
                extraUnlocked = extraUnlocked + (n + 1)
            }
        }
        if (first && n % 10 == 0) hints += 1 // +1 hint with every chapter chest
        // New Item bar: every NEW_ITEM_EVERY first-time wins unlock the next item (or 100 coins once all are owned)
        var newItem: ShopItem? = null
        if (first) {
            itemProgress++
            if (itemProgress >= NEW_ITEM_EVERY) {
                itemProgress = 0
                newItem = nextNewItem
                when (newItem?.kind) {
                    ShopItem.Kind.BALL -> { ownedBalls = ownedBalls + newItem.id; putIntSet("owned_balls", ownedBalls) }
                    ShopItem.Kind.BOARD -> { ownedBoards = ownedBoards + newItem.id; putIntSet("owned_boards", ownedBoards) }
                    null -> coinsEarned += 100
                }
            }
        }
        tilesPainted += tiles
        swipes += moves
        coins += coinsEarned
        prefs.edit()
            .putInt("max_unlocked", maxUnlocked)
            .putStringSet("extra_unlocked", extraUnlocked.map { it.toString() }.toSet())
            .putInt("coins", coins).putInt("hints", hints).putInt("perfects", perfects).putInt("chests", chestsOpened).putInt("perfect_streak", perfectStreak)
            .putInt("item_progress", itemProgress)
            .putLong("tiles_painted", tilesPainted).putLong("swipes", swipes)
            .apply()
        return Reward(coinsEarned, earnedStars, first, chest, perfectStreak, streakBonus, newItem)
    }

    // ---------------------------------------------------------------- economy
    fun addCoins(n: Int) { coins += n; prefs.edit().putInt("coins", coins).apply() }
    fun spend(n: Int): Boolean {
        if (coins < n) return false
        coins -= n; prefs.edit().putInt("coins", coins).apply(); return true
    }
    fun useHint(): Boolean {
        if (hints <= 0) return false
        hints--; hintsUsed++
        prefs.edit().putInt("hints", hints).putInt("hints_used", hintsUsed).apply(); return true
    }
    /** Free hint (rewarded ad). */
    fun addHint(n: Int = 1) { hints += n; prefs.edit().putInt("hints", hints).apply() }
    fun buyHint(price: Int): Boolean {
        if (!spend(price)) return false
        hints++; prefs.edit().putInt("hints", hints).apply(); return true
    }

    fun buyBall(id: Int) = buy(id, BallSkin.byId(id).price, ownedBalls) { ownedBalls = it; putIntSet("owned_balls", it) }
    fun buyTrail(id: Int) = buy(id, TrailStyle.byId(id).price, ownedTrails) { ownedTrails = it; putIntSet("owned_trails", it) }
    fun buyBoard(id: Int) = buy(id, BoardTheme.byId(id).price, ownedBoards) { ownedBoards = it; putIntSet("owned_boards", it) }
    fun buyFinish(id: Int) = buy(id, PaintFinish.byId(id).price, ownedFinishes) { ownedFinishes = it; putIntSet("owned_finishes", it) }
    private fun buy(id: Int, price: Int, owned: Set<Int>, save: (Set<Int>) -> Unit): Boolean {
        if (id in owned) return true
        if (!spend(price)) return false
        save(owned + id); return true
    }
    /** Free unlocks (rewarded ad; debug fake ad for now). */
    fun grantBall(id: Int) { if (!adUnlocks) return; ownedBalls = ownedBalls + id; putIntSet("owned_balls", ownedBalls) }
    fun grantTrail(id: Int) { if (!adUnlocks) return; ownedTrails = ownedTrails + id; putIntSet("owned_trails", ownedTrails) }
    fun grantFinish(id: Int) { if (!adUnlocks) return; ownedFinishes = ownedFinishes + id; putIntSet("owned_finishes", ownedFinishes) }
    fun selectBall(id: Int) { if (id in ownedBalls) { ballSkin = id; prefs.edit().putInt("ball", id).apply() } }
    fun selectTrail(id: Int) { if (id in ownedTrails) { trail = id; prefs.edit().putInt("trail", id).apply() } }
    fun grantBoard(id: Int) { if (!adUnlocks) return; ownedBoards = ownedBoards + id; putIntSet("owned_boards", ownedBoards) }
    fun selectBoard(id: Int) { if (id in ownedBoards) { boardTheme = id; prefs.edit().putInt("board", id).apply() } }
    /** Campaign levels finished so far (paint colours unlock by this count). */
    val levelsDone: Int get() = maxUnlocked - 1
    fun ownsPaint(id: Int) = levelsDone >= com.ngstudio.mazebrush.render.PaintColor.byId(id).levels
    fun selectPaint(id: Int) { if (ownsPaint(id)) { paintColor = id; prefs.edit().putInt("paint_color", id).apply() } }
    fun selectFinish(id: Int) { if (id in ownedFinishes) { finish = id; prefs.edit().putInt("finish", id).apply() } }

    // ---------------------------------------------------------------- daily
    fun today(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    val dailyDoneToday: Boolean get() = dailyDoneDay == today()

    /** Daily challenge finished: +50 coins + 10 per streak day (max +100). */
    fun completeDaily(): Int {
        val t = today()
        if (dailyDoneDay == t) return 0
        dailyStreak = if (dailyDoneDay == t - 1) dailyStreak + 1 else 1
        bestDailyStreak = maxOf(bestDailyStreak, dailyStreak)
        dailyDoneDay = t
        dailiesDone++
        val reward = 50 + (10 * (dailyStreak - 1)).coerceAtMost(100)
        coins += reward
        prefs.edit().putLong("daily_done_day", t).putInt("daily_streak", dailyStreak).putInt("best_daily_streak", bestDailyStreak)
            .putInt("dailies_done", dailiesDone).putInt("coins", coins).apply()
        return reward
    }

    /** Streak shown on the Daily tab (0 if a day was missed). */
    val liveDailyStreak: Int get() = if (dailyDoneDay >= today() - 1) dailyStreak else 0

    val calendarReady: Boolean get() = calendarClaimDay != today()

    /** Day index (0..6) the next claim gives; resets to day 1 after a missed day or after day 7. */
    val calendarNextIndex: Int get() = if (calendarClaimDay == today() - 1 || calendarClaimDay == today()) calendarIndex % 7 else 0

    fun claimCalendar(): Int {
        if (!calendarReady) return 0
        val idx = calendarNextIndex
        val reward = CALENDAR[idx]
        calendarIndex = (idx + 1) % 7
        calendarClaimDay = today()
        coins += reward
        if (idx == 6) hints += 1
        prefs.edit().putLong("calendar_claim_day", calendarClaimDay).putInt("calendar_index", calendarIndex)
            .putInt("coins", coins).putInt("hints", hints).apply()
        return reward
    }

    // ---------------------------------------------------------------- achievements
    fun claimAchievement(id: String, reward: Int) {
        if (id in claimedAchievements) return
        claimedAchievements = claimedAchievements + id
        coins += reward
        prefs.edit().putStringSet("achievements", claimedAchievements).putInt("coins", coins).apply()
    }

    // ---------------------------------------------------------------- debug tools
    /** Debug only: open every level, own every ball/trail/finish (paint colours unlock by level, so all become free too), and stock coins/hints. */
    fun debugUnlockAll() {
        if (!com.ngstudio.mazebrush.BuildConfig.DEBUG) return
        maxUnlocked = LevelRepository.TOTAL
        ownedBalls = BallSkin.ALL.map { it.id }.toSet()
        ownedTrails = TrailStyle.ALL.map { it.id }.toSet()
        ownedFinishes = PaintFinish.ALL.map { it.id }.toSet()
        ownedBoards = BoardTheme.ALL.map { it.id }.toSet()
        coins = maxOf(coins, 999_999)
        hints = maxOf(hints, 999)
        putIntSet("owned_balls", ownedBalls); putIntSet("owned_trails", ownedTrails)
        putIntSet("owned_finishes", ownedFinishes); putIntSet("owned_boards", ownedBoards)
        prefs.edit().putInt("max_unlocked", maxUnlocked).putInt("coins", coins).putInt("hints", hints).apply()
        starsVersion++
    }

    fun debugReset() {
        if (!com.ngstudio.mazebrush.BuildConfig.DEBUG) return
        prefs.edit().clear().apply()
        stars.fill(0)
        starsFile.delete()
        onboardingDone = false; maxUnlocked = 1; extraUnlocked = emptySet(); coins = 50; hints = 3; starsVersion++
        ballSkin = 0; trail = 1; finish = 0; ownedBalls = setOf(0); ownedTrails = setOf(0, 1); ownedFinishes = setOf(0); boardTheme = 0; ownedBoards = setOf(0); itemProgress = 0; paintColor = com.ngstudio.mazebrush.render.PaintColor.DEFAULT
        perfectStreak = 0; tilesPainted = 0; swipes = 0; perfects = 0; hintsUsed = 0; dailyDoneDay = -1; dailyStreak = 0; bestDailyStreak = 0
        dailiesDone = 0; calendarClaimDay = -1; calendarIndex = 0; claimedAchievements = emptySet(); chestsOpened = 0
    }

    companion object {
        val CALENDAR = intArrayOf(20, 30, 40, 50, 75, 100, 200)

    }
}
