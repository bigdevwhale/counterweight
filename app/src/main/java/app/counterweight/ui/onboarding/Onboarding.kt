package app.counterweight.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.counterweight.AppContainer
import app.counterweight.container
import app.counterweight.data.Gender
import app.counterweight.data.Settings
import app.counterweight.domain.Kind
import app.counterweight.notify.Bell
import app.counterweight.ui.Words
import app.counterweight.ui.art.Art.scale
import app.counterweight.ui.art.StoneLook
import app.counterweight.ui.components.Btn
import app.counterweight.ui.components.Chip
import app.counterweight.ui.components.Flow
import app.counterweight.ui.components.Gap
import app.counterweight.ui.components.Kind as BtnKind
import app.counterweight.ui.components.LocalToast
import app.counterweight.ui.components.baize
import app.counterweight.ui.scale.WeightIcon
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T

/** Three pages: the idea, the metals, and how to be addressed and when to ring. */
@Composable
fun Onboarding(w: Words, settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val toast = LocalToast.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish(c, ctx) }
    val body = T.Body.copy(color = C.chalk)

    Column(Modifier.fillMaxSize().baize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)) {
        when (page) {
            0 -> {
                Canvas(Modifier.fillMaxWidth().height(300.dp)) {
                    scale(-6f, StoneLook(7, 0f, 5), 1f, listOf(Kind.FACT, Kind.AFTER, Kind.QUESTION, Kind.POWER, Kind.BRIGHT, Kind.FEATHER, Kind.VOICE), 1f, false)
                }
                Text(w.t("Камень на душе можно взвесить", "A stone on your heart can be weighed"), style = T.Title)
                Gap(8)
                Text(w.t("Что не даёт покоя — деньги, анализы, начальник, который не ответил, — кладёшь на левую чашу. На правую палата кладёт гирьки: на каждой один довод. Пока не перевесит.",
                    "Whatever won't let you rest (money, test results, the boss who didn't reply) goes on the left pan. On the right the chamber lays weights, one argument on each, until the beam tips."), style = body)
            }
            1 -> {
                Text(w.t("Из чего отлиты гирьки", "What the weights are cast from"), style = T.Title)
                Gap(12)
                for ((k, line) in listOf(
                    Kind.FACT to w.t("Чугун — проверенные факты и цифры, с источником.", "Cast iron: checked facts and figures, with a source."),
                    Kind.AFTER to w.t("Латунь — худший случай по шагам: «и что тогда?»", "Brass: the worst case, step by step. “And then what?”"),
                    Kind.POWER to w.t("Медь — маленькое дело, которое в твоих руках сегодня.", "Copper: one small thing in your hands today."),
                    Kind.QUESTION to w.t("Стекло — вопрос, который стоит себе задать.", "Glass: a question worth asking yourself."),
                    Kind.BRIGHT to w.t("Серебро — другая сторона того же камня.", "Silver: the other side of the same stone."),
                    Kind.VOICE to w.t("Бронза — голоса тех, кто думал об этом до нас.", "Bronze: voices of those who thought about it before us."),
                    Kind.TRUTH to w.t("Сталь — честно, когда страх настоящий.", "Steel: honesty, when the fear is real."),
                    Kind.FEATHER to w.t("И пёрышко. Иногда перевешивает именно оно.", "And a feather. Sometimes it's what tips the scale."),
                )) {
                    Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        WeightIcon(k, 40.dp)
                        Spacer(Modifier.size(12.dp))
                        Text(line, style = body)
                    }
                }
                Text(w.t("Больше тысячи тревог и почти шестнадцать тысяч гирек — без интернета, без рекламы, без аккаунта.", "Over a thousand worries and nearly sixteen thousand weights. No internet, no ads, no account."), style = T.Small.copy(color = C.chalkMuted))
            }
            else -> {
                Text(w.t("Почти всё", "Almost there"), style = T.Title)
                Gap(12)
                if (w.ru) {
                    Text("Как к тебе обращаться?", style = T.BodyB.copy(color = C.chalk))
                    Flow(Modifier.padding(top = 8.dp)) {
                        Chip("справился", settings.gender == Gender.HE, dark = true) { c.write { this.settings.update { it.copy(gender = Gender.HE) } } }
                        Chip("справилась", settings.gender == Gender.SHE, dark = true) { c.write { this.settings.update { it.copy(gender = Gender.SHE) } } }
                    }
                    Gap(16)
                }
                Text(w.t("Час тревог", "The hour for worries"), style = T.BodyB.copy(color = C.chalk))
                Text(w.t("Камень можно отложить на полку до этого часа — и не носить его весь день. Если на полке что-то ждёт, палата тихо напомнит.",
                    "You can put a stone on the shelf until this hour and not carry it all day. If something waits on the shelf, the chamber rings once, quietly."), style = T.Small.copy(color = C.chalkMuted))
                Flow(Modifier.padding(top = 8.dp)) {
                    for (hr in listOf(17, 18, 19, 20)) Chip("$hr:00", settings.worryHour == hr, dark = true) { c.write { this.settings.update { it.copy(worryHour = hr) } } }
                }
                Gap(16)
                Text(w.t("Утро", "Morning"), style = T.BodyB.copy(color = C.chalk))
                Text(w.t("Гирька дня — или вопрос «ну что, сбылось?», если настал день узнать.", "The weight of the day, or “so, did it happen?” when the day to find out has come."), style = T.Small.copy(color = C.chalkMuted))
                Flow(Modifier.padding(top = 8.dp)) {
                    for (hr in listOf(8, 9, 10)) Chip("$hr:00", settings.morning == hr, dark = true) { c.write { this.settings.update { it.copy(morning = hr) } } }
                    Chip(w.t("не звонить", "no bell"), settings.morning < 0, dark = true) { c.write { this.settings.update { it.copy(morning = -1) } } }
                }
            }
        }
        Gap(24)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (page > 0) Btn(w.t("Назад", "Back"), Modifier.weight(1f), kind = BtnKind.CHALK) { page-- }
            Btn(if (page < 2) w.t("Дальше", "Next") else w.t("Открыть палату", "Open the chamber"), Modifier.weight(1.6f)) {
                when {
                    page < 2 -> page++
                    w.ru && settings.gender == null -> toast.show("Выбери, как к тебе обращаться")
                    Build.VERSION.SDK_INT >= 33 -> ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else -> finish(c, ctx)
                }
            }
        }
    }
}

private fun finish(c: AppContainer, ctx: android.content.Context) = c.write {
    settings.update { it.copy(onboarded = true) }
    Bell.plan(ctx)
}
