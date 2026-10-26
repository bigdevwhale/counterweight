package app.counterweight.ui.ledger

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.counterweight.container
import app.counterweight.domain.Engine
import app.counterweight.domain.Outcome
import app.counterweight.domain.Stone
import app.counterweight.domain.date
import app.counterweight.ui.Home
import app.counterweight.ui.Nav
import app.counterweight.ui.Overlay
import app.counterweight.ui.components.Card
import app.counterweight.ui.components.Chip
import app.counterweight.ui.components.Flow
import app.counterweight.ui.components.Gap
import app.counterweight.ui.components.Label
import app.counterweight.ui.components.LocalToast
import app.counterweight.ui.components.TopBar
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T

/** The ledger of checking: every worry with a day to find out, and the person's own numbers. */
@Composable
fun LedgerScreen(h: Home, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val toast = LocalToast.current
    val s = h.s
    val due = s.due(h.today)
    val ahead = s.stones.filter { it.due != null && it.due > h.today && it.outcome == null }.sortedBy { it.due }
    val checked = s.checked.sortedByDescending { it.checkedAt }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Поверка", "Checking"), w.t("Поверочная книга палаты", "The chamber's ledger"))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Gap(6)
            Record(h)
            if (due.isNotEmpty()) {
                Label(w.t("Настал день узнать", "The day has come"), color = C.brassHi)
                for (st in due) Card(Modifier.padding(bottom = 8.dp)) {
                    Text(h.title(st), style = T.BodyB)
                    Text(w.t("Положен{|а} ", "Put down ") + w.date(st.at.date(h.zone)) + " · " + w.t("тяжесть", "heft") + " ${st.heft}", style = T.Small)
                    Gap(4)
                    Text(w.t("Ну что — сбылось?", "So, did it happen?"), style = T.Body)
                    Flow(Modifier.padding(top = 6.dp)) {
                        for (o in Outcome.entries) Chip(w.outcome(o), false) {
                            c.write { repo.check(st.id, o) }
                            toast.show(when (o) {
                                Outcome.NOT -> w.t("Записано. Ещё одна гирька в твою статистику — и камень можно бросить в реку.", "Noted. One more weight for your own record, and the stone can go in the river.")
                                Outcome.PARTLY, Outcome.YES_COPED -> w.t("Записано. Ты прош{ёл|ла} через это — это тоже статистика.", "Noted. You got through it, and that counts too.")
                                Outcome.YES_HARD -> w.t("Записано. Сейчас тяжело — и это не навсегда. Береги себя.", "Noted. It's hard right now, and it won't always be. Be gentle with yourself.")
                            })
                        }
                    }
                }
                Text(w.t("Не знаешь пока? Перенести на неделю ›", "Don't know yet? Move them a week on ›"), style = T.Small.copy(color = C.chalkMuted),
                    modifier = Modifier.clickable { c.write { for (st in due) repo.due(st.id, h.today + 7) } }.padding(vertical = 6.dp))
            }
            if (ahead.isNotEmpty()) {
                Label(w.t("Впереди", "Ahead"), color = C.chalkMuted)
                Card { for ((i, st) in ahead.withIndex()) { if (i > 0) HorizontalDivider(color = C.line); Line(h, st, w.rel(st.due!!, h.today)) { nav.push(Overlay.StoneDetail(st.id)) } } }
            }
            if (checked.isNotEmpty()) {
                Label(w.t("Проверено", "Checked"), color = C.chalkMuted)
                Card { for ((i, st) in checked.withIndex()) { if (i > 0) HorizontalDivider(color = C.line); Line(h, st, w.outcome(st.outcome!!)) { nav.push(Overlay.StoneDetail(st.id)) } } }
            }
            if (due.isEmpty() && ahead.isEmpty() && checked.isEmpty()) {
                Gap(10)
                Card {
                    Text(w.t("Книга пока пуста", "The ledger is empty for now"), style = T.Heading)
                    Text(w.t("Когда взвешиваешь камень, скажи, когда станет ясно, сбылось ли. В тот день палата спросит — и твой собственный опыт ляжет на весы самой тяжёлой гирькой.",
                        "When you weigh a stone, say when it will be clear whether it came true. On that day the chamber will ask, and your own experience will go on the scale as the heaviest weight of all."), style = T.Body)
                }
            }
            Gap(24)
        }
    }
}

@Composable
private fun Line(h: Home, st: Stone, right: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(h.title(st), style = T.Body, modifier = Modifier.weight(1f))
        Text(right, style = T.Small.copy(color = if (st.outcome == Outcome.NOT) C.baizeLight else C.wax), modifier = Modifier.padding(start = 8.dp))
    }
}

/** The person's numbers: how many checked worries came true, drawn as one ruled bar. */
@Composable
private fun Record(h: Home) {
    val w = h.w
    val s = h.s
    val c = s.checked
    val lifted = s.stones.filter { it.lifted }
    Card {
        Text(w.t("Твоя статистика", "Your own numbers"), style = T.Heading)
        if (c.isEmpty()) Text(w.t("Пока ничего не проверено. Первые числа появятся после первой поверки.", "Nothing checked yet. The first numbers come after the first check."), style = T.Small)
        else {
            val parts = listOf(
                Outcome.NOT to Color(0xFF4F7A5E), Outcome.PARTLY to C.brass, Outcome.YES_COPED to C.copper, Outcome.YES_HARD to C.wax,
            ).map { (o, col) -> Triple(o, col, c.count { it.outcome == o }) }
            Canvas(Modifier.fillMaxWidth().height(22.dp).padding(vertical = 4.dp)) {
                var x = 0f
                for ((_, col, n) in parts) {
                    val wd = size.width * n / c.size
                    if (wd > 0) drawRoundRect(col, Offset(x, 0f), Size(wd - 2f, size.height), CornerRadius(3f))
                    x += wd
                }
            }
            for ((o, col, n) in parts) if (n > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.padding(end = 8.dp).height(10.dp).fillMaxWidth(.03f)) { drawRect(col) }
                Text("${w.outcome(o)} — $n", style = T.Small.copy(color = C.ink))
            }
            Engine.ledger(s)?.let { Text(w.ledger(it), style = T.Italic, modifier = Modifier.padding(top = 8.dp)) }
        }
        if (lifted.isNotEmpty()) {
            val avg = lifted.sumOf { it.heft - (it.after ?: it.heft) }.toFloat() / lifted.size
            Text(w.t("Взвешено ${w.n(lifted.size, "камень", "камня", "камней", "", "")}; в среднем каждый стал легче на ${"%.1f".format(avg)}.",
                "${lifted.size} stones weighed; on average each got ${"%.1f".format(avg)} lighter."), style = T.Small, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
