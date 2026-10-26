package app.counterweight.ui.scale

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.counterweight.container
import app.counterweight.domain.Engine
import app.counterweight.domain.Kind
import app.counterweight.domain.Rules
import app.counterweight.domain.Sphere
import app.counterweight.notify.Bell
import app.counterweight.sound.Snd
import app.counterweight.ui.Home
import app.counterweight.ui.Nav
import app.counterweight.ui.Overlay
import app.counterweight.ui.Tab
import app.counterweight.ui.art.Art.scale
import app.counterweight.ui.art.StoneLook
import app.counterweight.ui.components.Btn
import app.counterweight.ui.components.Card
import app.counterweight.ui.components.Chip
import app.counterweight.ui.components.Flow
import app.counterweight.ui.components.Gap
import app.counterweight.ui.components.Kind as BtnKind
import app.counterweight.ui.components.Label
import app.counterweight.ui.components.LocalToast
import app.counterweight.ui.components.TopBar
import app.counterweight.ui.components.paper
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

enum class Phase { HEFT, WEIGH, DONE }

/**
 * A weighing under way. [stone] is 0 until the stone is put on the pan (or the id of one taken off the
 * shelf); [queue] is every weight in the order it would come, [laid] what is on the pan now.
 */
data class Session(
    val worry: String?,
    val words: String,
    val sphere: Sphere,
    val heft: Int = 5,
    val stone: Long = 0,
    val phase: Phase = Phase.HEFT,
    val queue: List<String> = emptyList(),
    val next: Int = 0,
    val laid: List<String> = emptyList(),
    val seed: Int = Random.nextInt(),
)

@Composable
fun ScaleScreen(h: Home, nav: Nav, session: Session?, onSession: (Session?) -> Unit) {
    if (session == null) Idle(h, nav) else Weighing(h, nav, session, onSession)
}

/** The empty scale, balanced, waiting; with what the day has for the person around it. */
@Composable
private fun Idle(h: Home, nav: Nav) {
    val w = h.w
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Противовес", "Counterweight"), if (h.night) w.t("Ночью всё кажется тяжелее, чем есть", "At night everything weighs more than it does") else w.t("Палата мер и весов", "The chamber of weights and measures")) {
            Icon(Icons.Outlined.Settings, w.t("Настройки", "Settings"), tint = C.chalkMuted, modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { nav.push(Overlay.Settings) }.padding(10.dp).size(24.dp))
        }
        Canvas(Modifier.fillMaxWidth().height(330.dp)) { scale(0f, null, 0f, emptyList(), 1f, h.night) }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Btn(w.t("Положить камень на весы", "Put a stone on the scale"), Modifier.fillMaxWidth()) { nav.push(Overlay.Pick) }
            Text(w.t("Что не даёт покоя? Назови — и посмотрим, что можно положить на другую чашу.", "What's weighing on you? Name it, and let's see what goes on the other pan."),
                style = T.Small.copy(color = C.chalkMuted, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            val due = h.s.due(h.today)
            if (due.isNotEmpty()) {
                Gap(14)
                Card(onClick = { nav.tab(Tab.LEDGER) }) {
                    Label(w.t("Поверка", "Checking"), Modifier.padding(top = 0.dp), color = C.wax)
                    Text(w.t("Настал день узнать: «${h.title(due.first())}». Ну что — сбылось?", "The day has come: “${h.title(due.first())}”. So, did it happen?"), style = T.Body)
                    if (due.size > 1) Text(w.t("И ещё ${due.size - 1} — в поверочной книге", "And ${due.size - 1} more in the ledger"), style = T.Small)
                }
            }
            val waiting = h.s.waiting
            if (waiting.isNotEmpty()) {
                Gap(10)
                Card(onClick = { nav.weigh(Session(waiting.first().worry, waiting.first().words, waiting.first().sphere, waiting.first().heft, waiting.first().id)) }) {
                    Label(w.t("На полке до часа тревог", "On the shelf till the hour for worries"), Modifier.padding(top = 0.dp))
                    Text(h.title(waiting.first()), style = T.Body)
                    Text(w.t("Взвесить сейчас ›", "Weigh it now ›"), style = T.Small.copy(color = C.wax))
                }
            }
            Engine.ofTheDay(h.s, h.catalog, h.today, h.settings.salt)?.let { key -> h.shown(key) }?.let { wd ->
                Gap(10)
                Card(onClick = { nav.push(Overlay.WeightDetail(wd.key)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeightIcon(wd.kind, 44.dp)
                        Column(Modifier.padding(start = 10.dp)) {
                            Label(w.t("Гирька дня", "Weight of the day"), Modifier.padding(top = 0.dp, bottom = 2.dp))
                            Text(w.metal(wd.kind), style = T.Small)
                        }
                    }
                    Text(wd.text, style = T.Body, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Gap(24)
        }
    }
}

@Composable
private fun Weighing(h: Home, nav: Nav, ses: Session, onSession: (Session?) -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val tilt = remember { Animatable(0f) }
    val stoneIn = remember { Animatable(if (ses.phase == Phase.HEFT) .55f else 1f) }
    val lastIn = remember { Animatable(1f) }
    val shown = ses.laid.mapNotNull { h.shown(it) }
    val stoneMass = Rules.mass(ses.heft)
    val mass = ses.laid.sumOf { Engine.mass(it, h.catalog) }
    val over = ses.phase != Phase.HEFT && mass > stoneMass
    val look = StoneLook(ses.heft, Engine.smooth(h.s.polish(ses.worry)), ses.seed)
    val title = ses.words.ifBlank { h.catalog.worry(ses.worry)?.let { w.t(it.text) } ?: w.sphere(ses.sphere) }

    // The beam follows the masses, swinging on a soft spring like real brass.
    LaunchedEffect(ses.phase, mass, stoneIn.value >= 1f) {
        val target = if (ses.phase == Phase.HEFT || stoneIn.value < 1f) 0f else Rules.tilt(stoneMass, mass)
        tilt.animateTo(target, spring(dampingRatio = .32f, stiffness = Spring.StiffnessVeryLow))
    }
    var tipped by remember { mutableStateOf(false) }
    LaunchedEffect(over) {
        if (over && !tipped) {
            tipped = true
            delay(500)
            c.sfx.play(Snd.TIP)
            c.sfx.feel(40, 120)
        }
    }

    fun lay(s0: Session): Session {
        val key = s0.queue.getOrNull(s0.next) ?: return s0
        val s1 = s0.copy(next = s0.next + 1, laid = s0.laid + key)
        c.write { repo.lay(s1.stone, key) }
        h.shown(key)?.let { c.sfx.weight(it.kind) }
        scope.launch { lastIn.snapTo(0f); lastIn.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        return s1
    }

    fun start() = scope.launch {
        val id = if (ses.stone != 0L) ses.stone else c.repo.put(ses.worry, ses.words, ses.sphere, ses.heft)
        val st = c.repo.refresh()
        val q = Engine.queue(h.catalog.worry(ses.worry), ses.sphere, st, h.catalog, h.night, Random(ses.seed))
        val s1 = ses.copy(stone = id, phase = Phase.WEIGH, queue = q)
        onSession(s1)
        stoneIn.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        c.sfx.play(Snd.STONE)
        c.sfx.feel(60, 180)
        delay(900)
        onSession(lay(s1))
    }

    fun takeOff() {
        // A stone taken off half-weighed waits on the shelf instead of vanishing.
        if (ses.stone != 0L) c.write { repo.shelve(ses.stone, Bell.next(h.settings.worryHour.coerceAtLeast(0), System.currentTimeMillis(), h.zone)) }
        onSession(null)
    }
    BackHandler {
        when (ses.phase) {
            Phase.HEFT, Phase.WEIGH -> takeOff()
            // Leaving after the beam tipped keeps the weighing, with nothing set to check.
            Phase.DONE -> { c.write { repo.lifted(ses.stone, (ses.heft / 2).coerceAtLeast(1), null) }; onSession(null) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(w.sphere(ses.sphere).uppercase(), style = T.Label.copy(color = C.chalkMuted))
                Text(title, style = T.Heading.copy(color = C.chalk), maxLines = 3)
            }
            if (ses.phase != Phase.DONE) Text(w.t("Снять", "Take off").uppercase(), style = T.Button.copy(color = C.chalkMuted), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { takeOff() }.padding(10.dp))
        }
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            scale(tilt.value, look, stoneIn.value, shown.map { it.kind }, lastIn.value, h.night)
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).paper().padding(16.dp)) {
            when (ses.phase) {
                Phase.HEFT -> Heft(h, ses, onSession, ::start)
                Phase.WEIGH -> Reading(h, ses, shown.lastOrNull(), over, onNext = {
                    if (ses.next < ses.queue.size) onSession(lay(ses)) else toast.show(w.t("Гирьки на этот камень кончились", "No more weights for this stone"))
                }, onNotMine = { key ->
                    c.write { repo.notMine(key) }
                    val left = ses.copy(laid = ses.laid.dropLast(1))
                    onSession(if (left.next < left.queue.size) lay(left) else left)
                }, onDone = { onSession(ses.copy(phase = Phase.DONE)) })
                Phase.DONE -> Done(h, nav, ses) { onSession(null) }
            }
        }
    }
}

/** Before the stone goes down: how heavy is it? */
@Composable
private fun Heft(h: Home, ses: Session, onSession: (Session?) -> Unit, start: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val toast = LocalToast.current
    Text(w.t("Насколько тяжело?", "How heavy is it?"), style = T.Heading)
    Text("${ses.heft} · ${w.heft(ses.heft)}", style = T.Small)
    Slider(ses.heft.toFloat(), { onSession(ses.copy(heft = it.toInt().coerceIn(1, 10))) }, valueRange = 1f..10f, steps = 8,
        colors = SliderDefaults.colors(thumbColor = C.stoneDeep, activeTrackColor = C.stone, inactiveTrackColor = C.line, activeTickColor = C.paper, inactiveTickColor = C.muted))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ses.stone == 0L) Btn(w.t("На полку до ${h.settings.worryHour.coerceAtLeast(0)}:00", "Shelve till ${h.settings.worryHour.coerceAtLeast(0)}:00"), Modifier.weight(1f), kind = BtnKind.GHOST) {
            val until = Bell.next(h.settings.worryHour.coerceAtLeast(0), System.currentTimeMillis(), h.zone)
            c.write { repo.put(ses.worry, ses.words, ses.sphere, ses.heft, shelf = until) }
            toast.show(w.t("Камень полежит на полке до часа тревог. До тех пор он не твоя забота.", "The stone will wait on the shelf till the hour for worries. Until then it's not your job."))
            onSession(null)
        }
        Btn(w.t("Положить на чашу", "Put it on the pan"), Modifier.weight(1.3f)) { start() }
    }
}

/** The weight just laid, its words, and what to do next. */
@Composable
private fun Reading(h: Home, ses: Session, cur: Shown?, over: Boolean, onNext: () -> Unit, onNotMine: (String) -> Unit, onDone: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    if (cur == null) { Text(w.t("Кладу первую гирьку…", "Laying the first weight…"), style = T.Italic); Gap(60); return }
    val kept = h.s.isKept(cur.key)
    Row(verticalAlignment = Alignment.CenterVertically) {
        WeightIcon(cur.kind, 34.dp)
        Text(w.metal(cur.kind).uppercase(), style = T.Label, modifier = Modifier.weight(1f).padding(start = 8.dp))
        Text("${ses.laid.size}", style = T.Label)
        Icon(if (kept) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, w.t("В разновес", "Keep in the case"), tint = if (kept) C.wax else C.muted,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { c.write { repo.keep(cur.key, !kept) } }.padding(8.dp).size(22.dp))
    }
    Column(Modifier.fillMaxWidth().height(170.dp).verticalScroll(rememberScrollState())) {
        if (cur.kind == Kind.AFTER && cur.text.contains('→')) {
            for ((i, step) in cur.text.split('→').map { it.trim() }.withIndex()) {
                Row(Modifier.padding(start = (i * 10).dp, top = 2.dp)) {
                    Text(if (i == 0) "▸ " else "↳ ", style = T.Body.copy(color = C.brassDeep))
                    Text(step, style = T.Body)
                }
            }
        } else Text(cur.text, style = T.Engraving)
        cur.source?.let { Text("— $it", style = T.Small.copy(textAlign = TextAlign.End), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) }
    }
    Gap(8)
    if (over) Text(w.t("Перевесило. Можно читать дальше — или посмотреть, каким стал камень.", "It's tipped. Read on if you like, or see what the stone is like now."),
        style = T.Small.copy(color = C.wax), modifier = Modifier.padding(bottom = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (cur.kind != Kind.LEDGER && !cur.key.startsWith("own:")) Btn(w.t("Не про меня", "Not for me"), Modifier.weight(1f), kind = BtnKind.GHOST) { onNotMine(cur.key) }
        if (over) Btn(w.t("Ещё", "More"), Modifier.weight(.8f), kind = BtnKind.GHOST) { onNext() }
        Btn(if (over) w.t("Снять камень", "Lift the stone") else w.t("Ещё гирьку", "Another weight"), Modifier.weight(1.3f), kind = if (over) BtnKind.WAX else BtnKind.BRASS) { if (over) onDone() else onNext() }
    }
}

/** After the beam tipped: how heavy now, when it'll be known, and where the stone goes. */
@Composable
private fun Done(h: Home, nav: Nav, ses: Session, close: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val toast = LocalToast.current
    var after by remember { mutableIntStateOf((ses.heft / 2).coerceAtLeast(1)) }
    var due by remember { mutableStateOf<Int?>(7) }
    Text(w.t("А теперь — насколько тяжело?", "And now, how heavy is it?"), style = T.Heading)
    Text("${ses.heft} → $after · ${if (after == 0) w.t("пух", "a feather") else w.heft(after)}", style = T.Small)
    Slider(after.toFloat(), { after = it.toInt() }, valueRange = 0f..10f, steps = 9,
        colors = SliderDefaults.colors(thumbColor = C.stoneDeep, activeTrackColor = C.stone, inactiveTrackColor = C.line, activeTickColor = C.paper, inactiveTickColor = C.muted))
    Text(w.t("Когда станет ясно, сбылось ли?", "When will you know if it came true?"), style = T.BodyB)
    Flow(Modifier.padding(top = 6.dp, bottom = 12.dp)) {
        for ((d, label) in listOf(1 to w.t("завтра", "tomorrow"), 3 to w.t("через 3 дня", "in 3 days"), 7 to w.t("через неделю", "in a week"), 30 to w.t("через месяц", "in a month"), null to w.t("не узнать", "can't tell")))
            Chip(label, due == d) { due = d }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn(w.t("Колодец", "The well"), Modifier.weight(1f), kind = BtnKind.GHOST) { nav.push(Overlay.Well(ses.worry, ses.sphere)) }
        Btn(w.t("На полку", "To the shelf"), Modifier.weight(1.4f)) {
            val day = due?.let { LocalDate.ofEpochDay(h.today).plusDays(it.toLong()).toEpochDay() }
            c.write { repo.lifted(ses.stone, after, day) }
            toast.show(
                if (day != null) w.t("Поверка — ${w.date(LocalDate.ofEpochDay(day))}. Тогда и посмотрим.", "We'll check on ${w.date(LocalDate.ofEpochDay(day))}.")
                else w.t("Камень лёг на полку «${w.sphere(ses.sphere)}».", "The stone is on the “${w.sphere(ses.sphere)}” shelf."),
            )
            close()
        }
    }
}
