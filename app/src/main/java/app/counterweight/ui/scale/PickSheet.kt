package app.counterweight.ui.scale

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.counterweight.domain.Engine
import app.counterweight.domain.Sphere
import app.counterweight.domain.Worry
import app.counterweight.ui.Home
import app.counterweight.ui.Nav
import app.counterweight.ui.Overlay
import app.counterweight.ui.components.Btn
import app.counterweight.ui.components.Card
import app.counterweight.ui.components.Chip
import app.counterweight.ui.components.Field
import app.counterweight.ui.components.Flow
import app.counterweight.ui.components.Gap
import app.counterweight.ui.components.Kind
import app.counterweight.ui.components.Label
import app.counterweight.ui.components.Sheet
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T

/** Choosing the stone: typed in one's own words, found in the catalogue, or picked from a sphere's shelf. */
@Composable
fun PickSheet(h: Home, nav: Nav) {
    val w = h.w
    var query by rememberSaveable { mutableStateOf("") }
    var sphere by rememberSaveable { mutableStateOf<Sphere?>(null) }
    var own by remember { mutableStateOf<Sphere?>(null) }
    val found = remember(query) { h.catalog.search(query) }
    val guess = remember(query) { h.catalog.guess(query) }
    val crisis = Engine.crisis(query)
    // Long typed sentences are kept as the stone's name even when it matches a catalogue worry.
    val keepWords = query.trim().split(Regex("\\s+")).size >= 3

    fun choose(worry: Worry) {
        nav.pop()
        nav.weigh(Session(worry.key, if (keepWords) query.trim() else "", worry.sphere))
    }

    Sheet(w.t("Что за камень?", "What's the stone?"), { nav.pop() }, w.t("Закрыть", "Close")) {
        Field(query, { query = it }, w.t("Своими словами: «боюсь, что не потяну ипотеку»…", "In your own words: “I'm afraid I can't pay the mortgage”…"), minLines = 2)
        if (crisis) {
            Gap(10)
            Card(bg = C.wax.copy(alpha = .12f), onClick = { nav.push(Overlay.Help(query)) }) {
                Text(w.t("Похоже, тебе сейчас очень тяжело.", "It sounds like things are very hard right now."), style = T.BodyB)
                Text(w.t("Для этого весы не годятся — здесь нужен живой человек. Открыть, куда можно позвонить ›", "Scales aren't the right tool for this; a living person is. See who you can call ›"), style = T.Body.copy(color = C.wax))
            }
        }
        if (query.isNotBlank() && !crisis) {
            if (found.isNotEmpty()) {
                Label(w.t("Похоже на", "Sounds like"))
                for (wr in found) WorryRow(h, wr) { choose(wr) }
            }
            Label(w.t("Или взвесить как есть", "Or weigh it as it is"))
            Text(w.t("О чём это?", "What is it about?"), style = T.Small)
            Flow(Modifier.padding(top = 6.dp)) {
                for (sp in Sphere.entries) Chip(w.sphere(sp), (own ?: guess) == sp) { own = sp }
            }
            Gap(10)
            Btn(w.t("Взвесить своими словами", "Weigh it in my own words"), Modifier.fillMaxWidth(), enabled = (own ?: guess) != null && query.trim().length >= 3) {
                nav.pop()
                nav.weigh(Session(found.firstOrNull()?.takeIf { it.sphere == (own ?: guess) }?.key, query.trim(), own ?: guess!!))
            }
        }
        if (query.isBlank()) {
            Label(w.t("Или выбрать на полках", "Or pick from the shelves"))
            Flow {
                for (sp in Sphere.entries) Chip("${w.sphere(sp)} · ${h.catalog.worriesOf(sp).size}", sphere == sp) { sphere = if (sphere == sp) null else sp }
            }
            sphere?.let { sp ->
                Gap(10)
                for (wr in h.catalog.worriesOf(sp)) WorryRow(h, wr) { choose(wr) }
            }
        }
        Gap(20)
    }
}

@Composable
private fun WorryRow(h: Home, wr: Worry, onClick: () -> Unit) {
    val count = h.catalog.of(wr.key).size
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(h.w.t(wr.text), style = T.Body)
                Text("${h.w.sphere(wr.sphere)} · ${h.w.n(count, "гирька", "гирьки", "гирек", "weight", "weights")}", style = T.Small)
            }
            Text("›", style = T.Heading.copy(color = C.brassDeep))
        }
        HorizontalDivider(color = C.line)
    }
}

/** When the words say someone may be in danger: no scale, only people who can help, now. */
@Composable
fun HelpSheet(h: Home, words: String, onClose: () -> Unit) {
    val w = h.w
    val ctx = LocalContext.current
    fun open(uri: String) { runCatching { ctx.startActivity(Intent(if (uri.startsWith("tel:")) Intent.ACTION_DIAL else Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Sheet(w.t("Ты не {один|одна}", "You're not alone"), onClose, w.t("Закрыть", "Close")) {
        Text(w.t("Если ты думаешь о том, чтобы причинить себе вред, — это слишком тяжело, чтобы нести одному. Позвони или напиши тем, кто умеет помочь: это бесплатно и анонимно.",
            "If you're thinking about hurting yourself, that's too heavy to carry alone. Call or write to people who know how to help: it's free and anonymous."), style = T.Body)
        Gap(14)
        Card(onClick = { open("tel:112") }) {
            Text(w.t("Экстренная помощь — 112", "Emergency — 112 / 911 / 999"), style = T.BodyB)
            Text(w.t("Если опасность прямо сейчас", "If you're in danger right now"), style = T.Small)
        }
        Gap(8)
        if (w.ru) {
            Card(onClick = { open("tel:88002000122") }) {
                Text("8-800-2000-122", style = T.BodyB)
                Text("Телефон доверия для детей, подростков и родителей, бесплатно по России", style = T.Small)
            }
            Gap(8)
            Card(onClick = { open("tel:+74959895050") }) {
                Text("+7 495 989-50-50", style = T.BodyB)
                Text("Экстренная психологическая помощь МЧС, круглосуточно", style = T.Small)
            }
            Gap(8)
        }
        Card(onClick = { open("https://findahelpline.com") }) {
            Text("findahelpline.com", style = T.BodyB)
            Text(w.t("Бесплатные линии помощи в любой стране", "Free helplines in any country"), style = T.Small)
        }
        Gap(14)
        Text(w.t("И, если можешь, скажи кому-то рядом: «Мне сейчас очень плохо». Этого достаточно для начала.", "And if you can, tell someone near you: “I'm not okay right now.” That's enough to start."), style = T.Italic)
        Gap(14)
        Btn(w.t("Вернуться", "Go back"), Modifier.fillMaxWidth(), kind = Kind.GHOST, onClick = onClose)
    }
}
