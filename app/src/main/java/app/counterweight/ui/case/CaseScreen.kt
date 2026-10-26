package app.counterweight.ui.case

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.counterweight.container
import app.counterweight.domain.Engraved
import app.counterweight.domain.Kind
import app.counterweight.domain.Sphere
import app.counterweight.ui.Home
import app.counterweight.ui.Nav
import app.counterweight.ui.Overlay
import app.counterweight.ui.components.Btn
import app.counterweight.ui.components.Card
import app.counterweight.ui.components.Chip
import app.counterweight.ui.components.Field
import app.counterweight.ui.components.Flow
import app.counterweight.ui.components.Gap
import app.counterweight.ui.components.Kind as BtnKind
import app.counterweight.ui.components.Label
import app.counterweight.ui.components.LocalToast
import app.counterweight.ui.components.Sheet
import app.counterweight.ui.components.TopBar
import app.counterweight.ui.scale.WeightIcon
import app.counterweight.ui.scale.shown
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T

private val VELVET = Color(0xFF5A1620)
private val VELVET_HI = Color(0xFF7E2430)

/** The case of weights: the ones kept, the ones engraved, and what lies at the bottom of every well. */
@Composable
fun CaseScreen(h: Home, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val kept = h.s.kept.mapNotNull { h.shown(it) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Разновес", "The case"), w.t("Набор гирек в бархатном футляре", "A set of weights in a velvet case"))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Gap(6)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Brush.verticalGradient(listOf(VELVET_HI, VELVET)))
                    .border(4.dp, Brush.verticalGradient(listOf(C.wood, C.woodDeep)), RoundedCornerShape(8.dp)).padding(14.dp),
            ) {
                if (kept.isEmpty()) Text(w.t("Футляр пуст. На весах нажми ♡ у гирьки, которая попала в точку, — и она ляжет сюда, в своё гнездо.",
                    "The case is empty. On the scale, tap ♡ on a weight that hits home, and it will rest here in its own slot."), style = T.Body.copy(color = C.chalk))
                for ((i, wd) in kept.withIndex()) {
                    if (i > 0) HorizontalDivider(color = Color.Black.copy(alpha = .25f))
                    Row(Modifier.fillMaxWidth().clickable { nav.push(Overlay.WeightDetail(wd.key)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        WeightIcon(wd.kind, 38.dp)
                        Text(wd.text, style = T.Body.copy(color = C.chalk), maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp))
                    }
                }
            }
            Label(w.t("Гравировка", "Engraving"), color = C.chalkMuted)
            Card {
                Text(w.t("Свой довод весит больше чужого. Выгравируй то, что помогает именно тебе, — и он будет ложиться на чашу вместе с остальными.",
                    "Your own argument weighs more than anyone else's. Engrave what helps you in particular, and it will go on the pan with the rest."), style = T.Small)
                for (e in h.s.engraved) {
                    Row(Modifier.fillMaxWidth().clickable { nav.push(Overlay.WeightDetail(e.key)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        WeightIcon(Kind.OWN, 34.dp)
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(e.text, style = T.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(h.catalog.worry(e.worry)?.let { w.t(it.text) } ?: e.sphere?.let { w.sphere(it) } ?: w.t("к любому камню", "for any stone"), style = T.Small)
                        }
                    }
                }
                Gap(6)
                Btn(w.t("Выгравировать гирьку", "Engrave a weight"), Modifier.fillMaxWidth()) { nav.push(Overlay.Engrave(0)) }
            }
            Label(w.t("На дне колодца", "At the bottom of the well"), color = C.chalkMuted)
            Card {
                Text(w.t("Что тебя держит, что бы ни случилось? Люди, руки, умения, места. По одному в строке.", "What holds you up whatever happens? People, skills, places. One per line."), style = T.Small)
                Gap(6)
                var props by remember { mutableStateOf(h.settings.props) }
                Field(props, { props = it; c.write { settings.update { s -> s.copy(props = it) } } },
                    w.t("Сестра, которая всегда возьмёт трубку\nЯ умею учиться\nДрузья в Казани", "My sister, who always picks up\nI know how to learn\nFriends in Leeds"), minLines = 4, max = 600)
            }
            val set = h.s.notMine.size
            if (set > 0) {
                Gap(10)
                Text(w.t("Отложено как «не про меня»: $set. Вернуть их на стойку ›", "Set aside as “not for me”: $set. Put them back on the rack ›"),
                    style = T.Small.copy(color = C.chalkMuted), modifier = Modifier.clickable { c.write { for (k in h.s.notMine) repo.notMine(k, false) } }.padding(vertical = 6.dp))
            }
            Gap(24)
        }
    }
}

/** One weight up close: its words and source, to keep, share, or set aside. */
@Composable
fun WeightSheet(h: Home, key: String, nav: Nav) {
    val w = h.w
    val ctx = LocalContext.current
    val c = ctx.container
    val wd = h.shown(key) ?: run { nav.pop(); return }
    val kept = h.s.isKept(key)
    val own = key.startsWith("own:")
    Sheet(w.metal(wd.kind).replaceFirstChar { it.uppercase() }, { nav.pop() }, w.t("Закрыть", "Close")) {
        WeightIcon(wd.kind, 90.dp, Modifier.align(Alignment.CenterHorizontally))
        Gap(8)
        Text(wd.text, style = T.Engraving)
        wd.source?.let { Text("— $it", style = T.Small, modifier = Modifier.padding(top = 6.dp)) }
        h.catalog.weight(key)?.let { cw -> h.catalog.worry(cw.worry)?.let { Text(w.t("Из гирек к камню «${w.t(it.text)}»", "From the weights for “${w.t(it.text)}”"), style = T.Small, modifier = Modifier.padding(top = 10.dp)) } }
        Gap(16)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(if (kept) w.t("Вынуть из футляра", "Take out of the case") else w.t("В футляр ♡", "Into the case ♡"), Modifier.weight(1f), kind = if (kept) BtnKind.GHOST else BtnKind.BRASS) { c.write { repo.keep(key, !kept) } }
            Btn(w.t("Поделиться", "Share"), Modifier.weight(1f), kind = BtnKind.GHOST) {
                val text = wd.text + (wd.source?.let { "\n— $it" } ?: "") + "\n\n" + w.t("«Противовес»", "Counterweight")
                runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        Gap(10)
        if (own) {
            val id = key.removePrefix("own:").toLong()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(w.t("Переписать", "Re-engrave"), Modifier.weight(1f), kind = BtnKind.GHOST) { nav.push(Overlay.Engrave(id)) }
                Btn(w.t("Переплавить", "Melt down"), Modifier.weight(1f), kind = BtnKind.GHOST) { c.write { repo.unengrave(id) }; nav.pop() }
            }
        } else if (wd.kind != Kind.LEDGER) {
            val set = key in h.s.notMine
            Text(if (set) w.t("Вернуть на стойку", "Put back on the rack") else w.t("Не про меня — больше не класть", "Not for me: don't lay it again"),
                style = T.Small.copy(color = C.muted), modifier = Modifier.clickable { c.write { repo.notMine(key, !set) } }.padding(vertical = 8.dp))
        }
        Gap(20)
    }
}

/** Engraving a weight of one's own: words, and which stones it goes on. */
@Composable
fun EngraveSheet(h: Home, o: Overlay.Engrave, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val toast = LocalToast.current
    val old = h.s.engraved.firstOrNull { it.id == o.id }
    var text by remember { mutableStateOf(old?.text ?: "") }
    var sphere by remember { mutableStateOf(old?.sphere ?: o.sphere) }
    val worry = old?.worry ?: o.worry
    var forWorry by remember { mutableStateOf(worry != null) }
    Sheet(w.t("Гравировка", "Engraving"), { nav.pop() }, w.t("Закрыть", "Close")) {
        Text(w.t("Что ты сам{|а} себе скажешь, когда этот камень снова станет тяжёлым?", "What will you tell yourself when this stone gets heavy again?"), style = T.Body)
        Gap(8)
        Field(text, { text = it }, w.t("Я уже переживал{|а} такое в 2019-м — и вышло лучше, чем думал{|а}", "I went through this in 2019, and it turned out better than I thought"), minLines = 4, max = 240)
        Label(w.t("Класть на чашу", "Lay it on the pan"))
        Flow {
            if (worry != null) Chip(h.catalog.worry(worry)?.let { w.t(it.text) } ?: w.t("с этим камнем", "with this stone"), forWorry) { forWorry = true }
            Chip(w.t("с любым камнем", "with any stone"), !forWorry && sphere == null) { forWorry = false; sphere = null }
            for (sp in Sphere.entries) Chip(w.sphere(sp), !forWorry && sphere == sp) { forWorry = false; sphere = sp }
        }
        Gap(16)
        Btn(w.t("Выгравировать", "Engrave it"), Modifier.fillMaxWidth(), enabled = text.trim().length >= 3) {
            val e = Engraved(id = old?.id ?: 0, text = text.trim(), worry = if (forWorry) worry else null, sphere = if (forWorry) h.catalog.worry(worry)?.sphere else sphere, at = old?.at ?: 0)
            c.write { repo.engrave(e) }
            toast.show(w.t("Готово: золотая гирька в футляре.", "Done: a gold weight in your case."))
            nav.pop()
        }
        Gap(20)
    }
}
