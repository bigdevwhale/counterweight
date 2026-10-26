package app.counterweight.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.counterweight.BuildConfig
import app.counterweight.container
import app.counterweight.data.Gender
import app.counterweight.data.Settings
import app.counterweight.notify.Bell
import app.counterweight.ui.Home
import app.counterweight.ui.Words
import app.counterweight.ui.components.Card
import app.counterweight.ui.components.Chip
import app.counterweight.ui.components.Flow
import app.counterweight.ui.components.Gap
import app.counterweight.ui.components.Sheet
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "counterweight"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

@Composable
fun SettingsSheet(h: Home, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val st = h.settings
    fun save(f: (Settings) -> Settings) = c.write { settings.update(f); Bell.plan(ctx) }

    Sheet(w.t("Настройки", "Settings"), onClose, w.t("Закрыть", "Close")) {
        Card {
            Text(w.t("Язык", "Language"), style = T.Heading)
            Flow(Modifier.padding(top = 8.dp)) {
                Chip("Русский", h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
                Chip("English", !h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
            }
            if (h.ru) {
                Text("Как к тебе обращаться", style = T.BodyB, modifier = Modifier.padding(top = 12.dp))
                Flow(Modifier.padding(top = 6.dp)) {
                    Chip("справился", st.gender == Gender.HE) { save { it.copy(gender = Gender.HE) } }
                    Chip("справилась", st.gender == Gender.SHE) { save { it.copy(gender = Gender.SHE) } }
                }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Час тревог", "The hour for worries"), style = T.Heading)
            Text(w.t("До этого часа камни ждут на полке. Если что-то ждёт — одно тихое напоминание.", "Shelved stones wait until this hour. If any wait, one quiet reminder."), style = T.Small)
            Flow(Modifier.padding(top = 8.dp)) {
                for (hr in listOf(16, 17, 18, 19, 20, 21)) Chip("$hr:00", st.worryHour == hr) { save { it.copy(worryHour = hr) } }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Утренний колокольчик", "The morning bell"), style = T.Heading)
            Text(w.t("Гирька дня, а в день поверки — вопрос, сбылось ли.", "The weight of the day, and on a checking day, whether it came true."), style = T.Small)
            Flow(Modifier.padding(top = 8.dp)) {
                for (hr in listOf(7, 8, 9, 10, 11)) Chip("$hr:00", st.morning == hr) { save { it.copy(morning = hr) } }
                Chip(w.t("не звонить", "no bell"), st.morning < 0) { save { it.copy(morning = -1) } }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Звук", "Sound"), style = T.Heading)
            Flow(Modifier.padding(top = 8.dp)) {
                Chip(w.t("латунь звенит", "brass rings"), st.sound) { save { it.copy(sound = true) } }
                Chip(w.t("тихо", "silent"), !st.sound) { save { it.copy(sound = false) } }
            }
        }
        Gap(10)
        Card(onClick = { c.write { settings.update { it.copy(onboarded = false) } }; onClose() }) {
            Text(w.t("Как это устроено", "How it works"), style = T.Heading)
            Text(w.t("Пройти знакомство с палатой ещё раз ›", "Go through the welcome pages again ›"), style = T.Small.copy(color = C.wax))
        }
        Gap(10)
        CommunityLinks(w)
        Gap(10)
        Card {
            Text(w.t("О «Противовесе»", "About Counterweight"), style = T.Heading)
            Text(w.t("Тревогу можно взвесить. На другую чашу ложатся факты, худшие случаи по шагам, маленькие дела, вопросы и голоса. Приложение не заменяет врача и психотерапевта; если тяжело долго — к ним стоит сходить. Без рекламы, без аккаунта, без интернета.",
                "A worry can be weighed. Facts, worst cases walked through, small deeds, questions and voices go on the other pan. The app doesn't replace a doctor or a therapist; if it's been hard for a long time, it's worth going to one. No ads, no account, no internet."), style = T.Small)
            Text(w.t("Версия ${BuildConfig.VERSION_NAME} · шрифты Prata, PT Serif и PT Mono (OFL) · исходники на GitHub", "Version ${BuildConfig.VERSION_NAME} · Prata, PT Serif and PT Mono fonts (OFL) · source on GitHub"),
                style = T.Small.copy(color = C.wax),
                modifier = Modifier.padding(top = 8.dp).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bigdevwhale/counterweight"))) } })
        }
        Gap(20)
    }
}

/** Report a bug, suggest an idea, follow updates, join the chat; each opens Telegram. */
@Composable
private fun CommunityLinks(w: Words) {
    val ctx = LocalContext.current
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Card {
        Text(w.t("Сообщество", "Community"), style = T.Heading)
        Text(w.t("Приложение делается вместе с теми, кто им пользуется. Всё идёт через бота StillCraft в Telegram.", "The app is made together with the people who use it. It all goes through the StillCraft bot on Telegram."),
            style = T.Small, modifier = Modifier.padding(bottom = 4.dp))
        for ((icon, label, sub, url) in listOf(
            Link("🐞", w.t("Сообщить о баге", "Report a bug"), w.t("что сломалось и как повторить", "what broke and how to repeat it"), Community.BUG),
            Link("💡", w.t("Предложить идею", "Suggest an idea"), w.t("тревога, гирька, функция", "a worry, a weight, a feature"), Community.IDEA),
            Link("🔔", w.t("Подписаться на обновления", "Follow updates"), w.t("бот напишет, когда выйдет версия", "the bot writes when a version is out"), Community.FOLLOW),
            Link("💬", w.t("Чат StillCraft", "StillCraft chat"), w.t("все приложения и люди", "all the apps and people"), Community.CHAT),
        )) {
            Row(Modifier.fillMaxWidth().clickable { open(url) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, style = T.Heading, modifier = Modifier.width(34.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = T.BodyB)
                    Text(sub, style = T.Small)
                }
                Text("›", style = T.Heading.copy(color = C.wax))
            }
        }
    }
}

private data class Link(val icon: String, val label: String, val sub: String, val url: String)
