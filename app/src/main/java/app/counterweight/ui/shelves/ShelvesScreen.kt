package app.counterweight.ui.shelves

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.counterweight.domain.Engine
import app.counterweight.domain.Stone
import app.counterweight.ui.Home
import app.counterweight.ui.Nav
import app.counterweight.ui.Overlay
import app.counterweight.ui.art.Art.stone
import app.counterweight.ui.art.StoneLook
import app.counterweight.ui.components.Card
import app.counterweight.ui.components.Gap
import app.counterweight.ui.components.Label
import app.counterweight.ui.components.TopBar
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T
import kotlin.math.sin
import kotlin.random.Random

/** How a stone looks now: its size from how heavy it was last felt, its edges from how often it was outweighed. */
fun Home.look(st: Stone) = StoneLook((st.after ?: st.heft).coerceAtLeast(1), Engine.smooth(s.polish(st.worry)), (st.worry ?: st.words).hashCode() xor st.id.toInt())

/** The stones on wooden shelves by sphere, those waiting for the hour of worries, and the river. */
@Composable
fun ShelvesScreen(h: Home, nav: Nav) {
    val w = h.w
    val lying = h.s.shelved
    val tossed = h.s.stones.count { it.tossed }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Полки", "Shelves"), if (w.ru) "${w.n(lying.size, "камень", "камня", "камней", "", "")} на полках · ${w.n(tossed, "камешек", "камешка", "камешков", "", "")} в реке"
            else "${lying.size} on the shelves · $tossed in the river")
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (lying.isEmpty() && tossed == 0) {
                Gap(8)
                Card {
                    Text(w.t("Полки пока пусты", "The shelves are empty for now"), style = T.Heading)
                    Text(w.t("Каждый взвешенный камень ложится сюда, на полку своей сферы. Чем чаще одна и та же тревога перевешена, тем глаже её камень, — пока не станет галькой, которую можно бросить в реку.",
                        "Every stone you weigh lands here, on its sphere's shelf. The more often the same worry is outweighed, the smoother its stone, until it's a pebble you can throw into the river."), style = T.Body)
                }
            }
            val waiting = h.s.waiting
            if (waiting.isNotEmpty()) {
                Label(w.t("Ждут часа тревог · ${h.settings.worryHour.coerceAtLeast(0)}:00", "Waiting for the hour · ${h.settings.worryHour.coerceAtLeast(0)}:00"), color = C.brassHi)
                Shelf(h, waiting, nav)
            }
            for ((sp, list) in lying.filter { it !in waiting }.groupBy { it.sphere }.entries.sortedByDescending { it.value.size }) {
                Label("${w.sphere(sp)} · ${list.size}", color = C.chalkMuted)
                Shelf(h, list.sortedByDescending { it.at }, nav)
            }
            Label(w.t("Река", "The river"), color = C.chalkMuted)
            River(h, tossed)
            Gap(24)
        }
    }
}

@Composable
private fun Shelf(h: Home, list: List<Stone>, nav: Nav) {
    Column {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            for (st in list) {
                Column(Modifier.width(78.dp).clickable { nav.push(Overlay.StoneDetail(st.id)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(70.dp)) {
                        val look = h.look(st)
                        val r = size.minDimension * (.18f + look.heft * .022f)
                        stone(look, Offset(size.width / 2, size.height - r * .8f), r)
                        // A wax dot: this stone has a day of checking ahead.
                        if (st.due != null && st.outcome == null) drawCircle(C.wax, 3.5f * density, Offset(size.width / 2 + r * .85f, size.height - r * 1.55f))
                    }
                    Text(h.title(st), style = T.Small.copy(color = C.chalk, textAlign = TextAlign.Center, fontSize = T.Small.fontSize * .85f, lineHeight = T.Small.lineHeight * .82f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(34.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(12.dp).background(Brush.verticalGradient(listOf(C.wood, C.woodDeep))))
        Box(Modifier.fillMaxWidth().height(6.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent))))
    }
}

/** The river where pebbles go: water with the ones thrown lying on the bottom. */
@Composable
private fun River(h: Home, tossed: Int) {
    val w = h.w
    Box(Modifier.fillMaxWidth().height(120.dp)) {
        Canvas(Modifier.fillMaxSize()) { river(tossed) }
        Text(
            if (tossed == 0) w.t("Сюда уходят камни, которые стали галькой или не сбылись.", "Stones go here once they've worn to pebbles or didn't come true.")
            else w.t("Здесь лежит ${w.n(tossed, "камешек", "камешка", "камешков", "", "")}. Их больше не нужно носить.", "$tossed ${if (tossed == 1) "pebble lies" else "pebbles lie"} here. No need to carry them any more."),
            style = T.Small.copy(color = C.chalk), modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
    }
}

fun DrawScope.river(pebbles: Int, splash: Float = 0f) {
    drawRoundRect(Brush.verticalGradient(listOf(C.water, Color(0xFF173640))), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f))
    val rnd = Random(17)
    for (i in 0 until pebbles.coerceAtMost(60)) {
        val x = rnd.nextFloat() * size.width
        val y = size.height * (.6f + rnd.nextFloat() * .32f)
        val r = 5f + rnd.nextFloat() * 6f
        drawOval(Color(0xFF8C8A80).copy(alpha = .75f), Offset(x - r, y - r * .6f), Size(r * 2, r * 1.2f))
        drawOval(Color.White.copy(alpha = .12f), Offset(x - r * .6f, y - r * .5f), Size(r * .8f, r * .4f))
    }
    for (i in 0 until 5) {
        val y = size.height * (.25f + i * .14f)
        val x0 = (i * 53f) % size.width
        drawLine(Color.White.copy(alpha = .08f), Offset(x0, y), Offset(x0 + size.width * .25f, y + sin(i.toFloat()) * 2f), 2f)
    }
    if (splash > 0f) for (k in 0 until 3) {
        val r = (splash * 70f - k * 14f).coerceAtLeast(0f)
        drawOval(Color.White.copy(alpha = (1f - splash) * .6f), Offset(size.width / 2 - r, size.height * .45f - r * .3f), Size(r * 2, r * .6f), style = Stroke(2f))
    }
}
