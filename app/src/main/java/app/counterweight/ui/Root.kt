package app.counterweight.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.counterweight.R
import app.counterweight.container
import app.counterweight.data.Settings
import app.counterweight.domain.Catalog
import app.counterweight.domain.Engine
import app.counterweight.domain.Sphere
import app.counterweight.domain.State
import app.counterweight.domain.Stone
import app.counterweight.domain.Worry
import app.counterweight.domain.day
import app.counterweight.notify.Bell
import app.counterweight.ru
import app.counterweight.ui.case.CaseScreen
import app.counterweight.ui.case.EngraveSheet
import app.counterweight.ui.case.WeightSheet
import app.counterweight.ui.components.LocalToast
import app.counterweight.ui.components.ToastHost
import app.counterweight.ui.components.Toaster
import app.counterweight.ui.components.baize
import app.counterweight.ui.ledger.LedgerScreen
import app.counterweight.ui.onboarding.Onboarding
import app.counterweight.ui.scale.HelpSheet
import app.counterweight.ui.scale.PickSheet
import app.counterweight.ui.scale.ScaleScreen
import app.counterweight.ui.scale.Session
import app.counterweight.ui.settings.SettingsSheet
import app.counterweight.ui.shelves.ShelvesScreen
import app.counterweight.ui.shelves.StoneSheet
import app.counterweight.ui.shelves.WellSheet
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T
import kotlinx.coroutines.delay
import java.time.ZoneId

/** Everything a screen reads, at one moment. */
class Home(val s: State, val settings: Settings, val w: Words, val now: Long, val catalog: Catalog) {
    val ru get() = w.ru
    val zone: ZoneId = ZoneId.systemDefault()
    val today: Long get() = now.day(zone)
    val night get() = Engine.isNight(now, zone)

    /** What a stone is called: the person's own words, or the catalogue's. */
    fun title(st: Stone): String = st.words.ifBlank { catalog.worry(st.worry)?.let { w.t(it.text) } ?: w.sphere(st.sphere) }
    fun worry(st: Stone): Worry? = catalog.worry(st.worry)
}

enum class Tab { SCALE, SHELVES, CASE, LEDGER }

sealed interface Overlay {
    data object Pick : Overlay
    data class Help(val words: String) : Overlay
    data class StoneDetail(val id: Long) : Overlay
    data class Well(val worry: String?, val sphere: Sphere) : Overlay
    data class WeightDetail(val key: String) : Overlay
    data class Engrave(val id: Long, val worry: String? = null, val sphere: Sphere? = null) : Overlay
    data object Settings : Overlay
}

/** How screens open sheets, switch tabs and start a weighing. */
class Nav(val overlays: SnapshotStateList<Overlay>, val tab: (Tab) -> Unit, val weigh: (Session) -> Unit) {
    fun push(o: Overlay) = overlays.add(o)
    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }
}

@Composable
fun ChamberRoot(settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val state by c.repo.state.collectAsStateWithLifecycle()
    val words = remember(ctx.ru, settings.gender) { Words(ctx.ru, settings.gender) }
    val toaster = remember { Toaster() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.sound) { c.sfx.on = settings.sound }
    if (!settings.onboarded) {
        CompositionLocalProvider(LocalToast provides toaster) { Box(Modifier.fillMaxSize()) { Onboarding(words, settings); ToastHost(toaster) } }
        return
    }
    val life = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        life.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            c.repo.refresh()
            Bell.plan(ctx)
            while (true) { now = System.currentTimeMillis(); delay(30_000) }
        }
    }
    val s = state ?: run { Box(Modifier.fillMaxSize().baize()); return }
    val home = Home(s, settings, words, now, c.catalog)

    // A bell opens straight onto what it rang about.
    val opened = remember { (ctx as? Activity)?.intent?.getStringExtra(Bell.OPEN) }
    var tab by remember { mutableStateOf(when (opened) { "ledger" -> Tab.LEDGER; "shelf" -> Tab.SHELVES; else -> Tab.SCALE }) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    /** The weighing under way on the scale, kept while sheets open over it. */
    var session by remember { mutableStateOf<Session?>(null) }
    val nav = remember { Nav(overlays, { tab = it }, { session = it; tab = Tab.SCALE }) }
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.SCALE) { tab = Tab.SCALE }

    CompositionLocalProvider(LocalToast provides toaster) {
        Box(Modifier.fillMaxSize().baize(home.night)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.SCALE -> ScaleScreen(home, nav, session) { session = it }
                        Tab.SHELVES -> ShelvesScreen(home, nav)
                        Tab.CASE -> CaseScreen(home, nav)
                        Tab.LEDGER -> LedgerScreen(home, nav)
                    }
                }
                if (session == null || tab != Tab.SCALE) TabBar(tab) { tab = it }
            }
            when (val o = overlays.lastOrNull()) {
                Overlay.Pick -> PickSheet(home, nav)
                is Overlay.Help -> HelpSheet(home, o.words) { nav.pop() }
                is Overlay.StoneDetail -> StoneSheet(home, o.id, nav)
                is Overlay.Well -> WellSheet(home, o.worry, o.sphere) { nav.pop() }
                is Overlay.WeightDetail -> WeightSheet(home, o.key, nav)
                is Overlay.Engrave -> EngraveSheet(home, o, nav)
                Overlay.Settings -> SettingsSheet(home) { nav.pop() }
                null -> Unit
            }
            ToastHost(toaster)
        }
    }
}

@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    val items = listOf(Tab.SCALE to R.string.tab_scale, Tab.SHELVES to R.string.tab_shelves, Tab.CASE to R.string.tab_case, Tab.LEDGER to R.string.tab_ledger)
    Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(C.wood, C.woodDeep)))) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(C.brassDeep))
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            for ((t, label) in items) {
                val sel = t == tab
                Box(Modifier.weight(1f).clickable { onTab(t) }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(label).uppercase(), style = T.Button.copy(color = if (sel) C.brassHi else C.chalkMuted, fontSize = T.Label.fontSize * 1.05f))
                }
            }
        }
    }
}
