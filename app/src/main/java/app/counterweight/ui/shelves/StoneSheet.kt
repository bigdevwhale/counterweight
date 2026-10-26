package app.counterweight.ui.shelves

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.counterweight.container
import app.counterweight.domain.Engine
import app.counterweight.domain.Rules
import app.counterweight.domain.date
import app.counterweight.sound.Snd
import app.counterweight.ui.Home
import app.counterweight.ui.Nav
import app.counterweight.ui.Overlay
import app.counterweight.ui.art.Art.stone
import app.counterweight.ui.components.Btn
import app.counterweight.ui.components.Gap
import app.counterweight.ui.components.Kind
import app.counterweight.ui.components.Label
import app.counterweight.ui.components.LocalToast
import app.counterweight.ui.components.Sheet
import app.counterweight.ui.scale.Session
import app.counterweight.ui.scale.WeightIcon
import app.counterweight.ui.scale.shown
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One stone up close: its story, the weights laid for it, and what can be done with it now. */
@Composable
fun StoneSheet(h: Home, id: Long, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val st = h.s.stone(id) ?: run { nav.pop(); return }
    val fall = remember { Animatable(0f) }
    val ripple = remember { Animatable(0f) }
    var forget by remember { mutableStateOf(false) }
    val polish = h.s.polish(st.worry)

    Sheet(w.sphere(st.sphere), { nav.pop() }, w.t("Закрыть", "Close")) {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val look = h.look(st)
            val r = size.height * (.16f + look.heft * .016f)
            if (fall.value > 0f) river(0, ripple.value)
            clipRect(bottom = if (fall.value > 0f) size.height * .45f + r * (1 - fall.value) else size.height) {
                stone(look, Offset(size.width / 2, size.height * .5f + fall.value * size.height * .4f), r * (1 - fall.value * .3f))
            }
        }
        Text(h.title(st), style = T.Heading)
        Text(w.t("Положен{|а} ", "Put down on ") + w.dateY(st.at.date(h.zone)) + " · " + w.t("тяжесть", "heft") + " ${st.heft}" + (st.after?.let { " → $it" } ?: ""), style = T.Small)
        st.worry?.let { _ ->
            Text(
                if (polish >= Rules.PEBBLE) w.t("Эта тревога перевешена ${w.n(polish, "раз", "раза", "раз", "", "")} — камень стал галькой.", "This worry has been outweighed $polish times. The stone is a pebble now.")
                else w.t("Перевешена ${w.n(polish, "раз", "раза", "раз", "", "")} из ${Rules.PEBBLE}, чтобы стать галькой.", "Outweighed $polish of ${Rules.PEBBLE} times on the way to a pebble."),
                style = T.Small,
            )
        }
        st.due?.let { d ->
            Text(
                st.outcome?.let { w.outcome(it) + " · " + w.date(LocalDate.ofEpochDay(d)) }
                    ?: w.t("Поверка ${w.rel(d, h.today)}", "Checking ${w.rel(d, h.today)}"),
                style = T.Small.copy(color = C.wax),
            )
        }
        Gap(12)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(if (st.lifted) w.t("Взвесить снова", "Weigh again") else w.t("Взвесить", "Weigh it"), Modifier.weight(1f)) {
                nav.pop()
                nav.weigh(if (st.lifted) Session(st.worry, st.words, st.sphere, st.after?.coerceAtLeast(1) ?: st.heft) else Session(st.worry, st.words, st.sphere, st.heft, st.id))
            }
            Btn(w.t("Колодец", "The well"), Modifier.weight(1f), kind = Kind.GHOST) { nav.push(Overlay.Well(st.worry, st.sphere)) }
        }
        if (Engine.canToss(st, h.s)) {
            Gap(8)
            Btn(w.t("Бросить в реку", "Throw it in the river"), Modifier.fillMaxWidth(), kind = Kind.WAX) {
                scope.launch {
                    fall.animateTo(1f, tween(700, easing = FastOutLinearInEasing))
                    c.sfx.play(Snd.SPLASH)
                    c.sfx.feel(30, 60)
                    ripple.animateTo(1f, tween(900, easing = LinearEasing))
                    c.repo.toss(st.id)
                    toast.show(w.t("Буль. Одним камнем меньше.", "Plop. One stone fewer."))
                    nav.pop()
                }
            }
        }
        val laid = h.s.laidOn(st.id).mapNotNull { h.shown(it.weight) }.distinctBy { it.key }
        if (laid.isNotEmpty()) {
            Label(w.t("На чаше лежали", "On the pan were"))
            for (wd in laid) {
                Row(Modifier.fillMaxWidth().clickable { nav.push(Overlay.WeightDetail(wd.key)) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    WeightIcon(wd.kind, 30.dp)
                    Text(wd.text, style = T.Body, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 8.dp))
                }
                HorizontalDivider(color = C.line)
            }
        }
        Gap(16)
        Text(if (forget) w.t("Точно забыть этот камень? Нажми ещё раз.", "Forget this stone for good? Tap again.") else w.t("Забыть этот камень", "Forget this stone"),
            style = T.Small.copy(color = if (forget) C.wax else C.muted),
            modifier = Modifier.clickable { if (forget) { c.write { repo.forget(st.id) }; nav.pop() } else forget = true }.padding(vertical = 8.dp))
        Gap(10)
    }
}

/** «And then what?» all the way down: the worst case step by step, and at the bottom what holds you up. */
@Composable
fun WellSheet(h: Home, worry: String?, sphere: app.counterweight.domain.Sphere, onClose: () -> Unit) {
    val w = h.w
    val chains = remember(worry, sphere) { Engine.well(h.catalog.worry(worry), sphere, h.catalog, h.ru) }
    var which by remember { mutableStateOf(0) }
    var depth by remember { mutableStateOf(1) }
    Sheet(w.t("Колодец «и что тогда?»", "The well: “and then what?”"), onClose, w.t("Закрыть", "Close")) {
        Text(w.t("Спустимся по ступеням худшего случая. На каждой — один вопрос: и что тогда?", "Let's walk down the steps of the worst case. On each one, a single question: and then what?"), style = T.Small)
        Gap(12)
        val chain = chains.getOrNull(which)
        if (chain == null) {
            Text(w.t("Для этого камня в колодце пока нет ступеней.", "There are no steps in the well for this stone yet."), style = T.Italic)
        } else {
            for ((i, step) in chain.take(depth).withIndex()) {
                Column(Modifier.padding(start = (i * 14).dp, bottom = 8.dp)) {
                    if (i > 0) Text(w.t("и что тогда?", "and then what?"), style = T.Label.copy(color = C.brassDeep))
                    Text(step, style = if (i == chain.lastIndex) T.BodyB else T.Body)
                }
            }
            Gap(8)
            if (depth < chain.size) Btn(w.t("И что тогда?", "And then what?"), Modifier.fillMaxWidth()) { depth++ }
            else {
                HorizontalDivider(color = C.brassDeep)
                Gap(10)
                Text(w.t("Дно колодца", "The bottom of the well"), style = T.Label.copy(color = C.brassDeep))
                Text(w.t("Даже здесь ты есть. И у тебя есть:", "Even here, you are still here. And you have:"), style = T.BodyB)
                val props = h.settings.props.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (props.isEmpty()) Text(w.t("…то, что тебя держит. Запиши это в «Разновесе» — и оно будет ждать на дне каждого колодца.", "…whatever holds you up. Write it down in the Case, and it will wait at the bottom of every well."), style = T.Italic)
                else for (p in props) Text("· $p", style = T.Body)
                Gap(12)
                if (chains.size > 1) Btn(w.t("Другой сценарий", "Another scenario"), Modifier.fillMaxWidth(), kind = Kind.GHOST) { which = (which + 1) % chains.size; depth = 1 }
            }
        }
        Gap(20)
    }
}
