package app.counterweight.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.counterweight.ui.theme.C
import app.counterweight.ui.theme.T
import kotlinx.coroutines.delay
import kotlin.math.sin

/** Green baize: the cloth of the counter, with a faint weave and a darker edge. */
fun Modifier.baize(night: Boolean = false) = this.background(if (night) C.night else C.baize).drawBehind {
    val step = 3.dp.toPx()
    var y = 0f
    var i = 0
    while (y < size.height) {
        drawLine(Color.Black.copy(alpha = if (i % 2 == 0) .05f else .025f), Offset(0f, y), Offset(size.width, y), 1f)
        y += step; i++
    }
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .35f)), Offset(size.width / 2, size.height * .4f), size.maxDimension * .75f))
}

/** Ledger paper with faint ruling. */
fun Modifier.paper() = this.background(C.paper).drawBehind {
    val step = 26.dp.toPx()
    var y = step
    while (y < size.height) { drawLine(C.paperDark.copy(alpha = .6f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
}

enum class Kind { BRASS, GHOST, SOFT, WAX, CHALK }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: Kind = Kind.BRASS, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val (bg, fg) = when (kind) {
        Kind.BRASS -> C.brass to C.woodDeep
        Kind.WAX -> C.wax to C.paper
        Kind.GHOST -> Color.Transparent to C.ink
        Kind.SOFT -> C.paper to C.ink
        Kind.CHALK -> Color.Transparent to C.chalk
    }
    val under = if (kind == Kind.WAX) C.waxDeep else C.brassDeep
    Box(
        modifier
            .then(if ((kind == Kind.BRASS || kind == Kind.WAX) && enabled) Modifier.drawBehind { drawRoundRect(under, Offset(0f, 3.dp.toPx()), size, CornerRadius(8.dp.toPx())) } else Modifier)
            .clip(shape)
            .then(if (kind == Kind.BRASS && enabled) Modifier.background(Brush.verticalGradient(listOf(C.brassHi, C.brass, C.brass))) else Modifier.background(if (enabled) bg else C.line))
            .then(
                when (kind) {
                    Kind.GHOST, Kind.SOFT -> Modifier.border(1.5.dp, if (enabled) C.ink.copy(alpha = .6f) else C.muted, shape)
                    Kind.CHALK -> Modifier.border(1.5.dp, C.chalk.copy(alpha = .55f), shape)
                    else -> Modifier
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text.uppercase(), style = T.Button.copy(color = if (enabled) fg else C.muted), textAlign = TextAlign.Center) }
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, dark: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val (bg, fg, edge) = when {
        on -> Triple(if (dark) C.brass else C.ink, if (dark) C.woodDeep else C.paper, if (dark) C.brass else C.ink)
        dark -> Triple(Color.Transparent, C.chalk, C.chalk.copy(alpha = .35f))
        else -> Triple(C.paper, C.ink, C.line)
    }
    Box(modifier.clip(shape).background(bg).border(1.5.dp, edge, shape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp)) {
        Text(text, style = T.Body.copy(color = fg, fontSize = T.Small.fontSize * 1.05f))
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, bg: Color = C.paper, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(bg).border(1.dp, C.line, RoundedCornerShape(6.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = C.muted) =
    Text(text.uppercase(), style = T.Label.copy(color = color), modifier = modifier.padding(top = 18.dp, bottom = 6.dp))

@Composable
fun Gap(h: Int = 10) = Spacer(Modifier.height(h.dp))

@Composable
fun Flow(modifier: Modifier = Modifier, gap: Dp = 6.dp, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) { content() }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, style: TextStyle = T.Body, minLines: Int = 1, max: Int = 200) {
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.fillMaxWidth().clip(shape).background(Color.White.copy(alpha = .55f)).border(1.5.dp, C.line, shape).padding(horizontal = 12.dp, vertical = 10.dp)) {
        if (value.isEmpty()) Text(hint, style = style.copy(color = C.muted.copy(alpha = .75f)))
        BasicTextField(
            value, { onChange(it.take(max)) }, Modifier.fillMaxWidth(), textStyle = style, minLines = minLines, cursorBrush = SolidColor(C.wax),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}

/** The title of a tab, lettered on the baize. */
@Composable
fun TopBar(title: String, sub: String? = null, end: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title)
            if (sub != null) Text(sub, style = T.Small.copy(color = C.chalkMuted))
        }
        end()
    }
}

/** A sheet of ledger paper over the counter, with a title and a way back. */
@Composable
fun Sheet(title: String, onClose: () -> Unit, closeLabel: String, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(C.scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(10.dp).clip(RoundedCornerShape(8.dp)).paper()) {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = T.Heading, modifier = Modifier.weight(1f))
                Text(closeLabel.uppercase(), style = T.Button.copy(color = C.muted), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClose).padding(10.dp))
            }
            val inner = Modifier.fillMaxWidth().weight(1f).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 16.dp, vertical = 8.dp)
            Column(inner, content = content)
        }
    }
}

/** Short messages at the bottom of the screen. */
class Toaster {
    var text by mutableStateOf<String?>(null)
    var seq by mutableIntStateOf(0)
    fun show(s: String) { text = s; seq++ }
}

val LocalToast = compositionLocalOf { Toaster() }

@Composable
fun BoxScope.ToastHost(t: Toaster) {
    LaunchedEffect(t.seq) { if (t.text != null) { delay(3200); t.text = null } }
    val s = t.text ?: return
    Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 84.dp)) {
        Text(s, style = T.Body.copy(color = C.paper, textAlign = TextAlign.Center), modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(C.woodDeep).clickable { t.text = null }.padding(horizontal = 16.dp, vertical = 11.dp))
    }
}

/** A slow sway for things hanging on chains: [t] in seconds. */
fun sway(t: Float, amp: Float = 1f) = sin(t * 1.3f) * amp
