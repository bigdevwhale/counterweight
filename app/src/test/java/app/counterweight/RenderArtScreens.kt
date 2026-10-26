package app.counterweight

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.counterweight.data.DemoSeed
import app.counterweight.data.Gender
import app.counterweight.domain.Engine
import app.counterweight.ui.Home
import app.counterweight.ui.Nav
import app.counterweight.ui.Words
import app.counterweight.ui.case.CaseScreen
import app.counterweight.ui.case.WeightSheet
import app.counterweight.ui.components.LocalToast
import app.counterweight.ui.components.Toaster
import app.counterweight.ui.components.baize
import app.counterweight.ui.ledger.LedgerScreen
import app.counterweight.ui.onboarding.Onboarding
import app.counterweight.ui.scale.Phase
import app.counterweight.ui.scale.PickSheet
import app.counterweight.ui.scale.ScaleScreen
import app.counterweight.ui.scale.Session
import app.counterweight.ui.settings.SettingsSheet
import app.counterweight.ui.shelves.ShelvesScreen
import app.counterweight.ui.shelves.StoneSheet
import app.counterweight.ui.shelves.WellSheet
import app.counterweight.ui.theme.ChamberTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo history, drawn to PNGs (run with `-Prender`), English and Russian. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<CounterweightApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c)
        val s = c.repo.refresh()
        val st = c.settings.now()
        // Midday, so the screens show the day's look rather than the night's.
        val noon = java.time.LocalDate.now().atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        Home(s, st, Words(lang == "ru", Gender.HE), noon, c.catalog)
    }

    private fun nav() = Nav(mutableStateListOf(), {}, {})

    private fun shot(name: String, content: @Composable (Home) -> Unit) {
        val h = home()
        rule.mainClock.autoAdvance = false
        rule.setContent { ChamberTheme { CompositionLocalProvider(LocalToast provides Toaster()) { Box(Modifier.fillMaxSize().baize()) { content(h) } } } }
        rule.mainClock.advanceTimeBy(4_000)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /** A weighing frozen mid-way: the demo's most weighed worry with six weights on the pan. */
    private fun session(h: Home, phase: Phase): Session {
        val st = h.s.stones.filter { it.lifted && it.worry != null }.groupBy { it.worry }.maxBy { it.value.size }.value.last()
        val w = h.catalog.worry(st.worry)!!
        val q = Engine.queue(w, w.sphere, h.s, h.catalog, false, kotlin.random.Random(4))
        return Session(w.key, "", w.sphere, 7, st.id, phase, q, 6, q.take(6), seed = 11)
    }

    private val stoneId get() = { h: Home -> h.s.stones.filter { it.lifted && !it.tossed }.maxBy { h.s.laidOn(it.id).size }.id }

    @Test fun welcome() = shot("0-welcome") { Onboarding(it.w, it.settings.copy(onboarded = false)) }
    @Test fun scale() = shot("1-scale") { ScaleScreen(it, nav(), null) {} }
    @Test fun pick() = shot("2-pick") { PickSheet(it, nav()) }
    @Test fun weighing() = shot("3-weighing") { h -> ScaleScreen(h, nav(), session(h, Phase.WEIGH)) {} }
    @Test fun lifted() = shot("4-lifted") { h -> ScaleScreen(h, nav(), session(h, Phase.DONE).let { it.copy(laid = it.queue.take(9), next = 9) }) {} }
    @Test fun shelves() = shot("5-shelves") { ShelvesScreen(it, nav()) }
    @Test fun stone() = shot("6-stone") { h -> StoneSheet(h, stoneId(h), nav()) }
    @Test fun ledger() = shot("7-ledger") { LedgerScreen(it, nav()) }
    @Test fun case() = shot("8-case") { CaseScreen(it, nav()) }
    @Test fun weight() = shot("9-weight") { h -> WeightSheet(h, h.s.kept.first(), nav()) }
    @Test fun well() = shot("a-well") { h -> val st = h.s.stone(stoneId(h))!!; WellSheet(h, st.worry, st.sphere) {} }
    @Test fun settings() = shot("x-settings") { SettingsSheet(it) {} }

}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
