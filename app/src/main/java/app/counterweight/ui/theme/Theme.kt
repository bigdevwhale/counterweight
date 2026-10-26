package app.counterweight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.counterweight.R

/** The chamber of weights and measures: green baize, brass, a dark wood counter, ledger paper and sealing wax. */
@Immutable
object C {
    val baize = Color(0xFF1E3A2C)
    val baizeDeep = Color(0xFF13271D)
    val baizeLight = Color(0xFF2B4D3B)
    val night = Color(0xFF0B1611)
    val paper = Color(0xFFF3EAD7)
    val paperDark = Color(0xFFE3D4B4)
    val line = Color(0xFFD3C29E)
    val ink = Color(0xFF2A2420)
    val muted = Color(0xFF7D7062)
    val chalk = Color(0xFFEDE4CF)
    val chalkMuted = Color(0xFFA8B3A4)
    val brass = Color(0xFFC9A04A)
    val brassHi = Color(0xFFF2DA92)
    val brassDeep = Color(0xFF85631F)
    val wood = Color(0xFF5A3A22)
    val woodDeep = Color(0xFF331F11)
    val wax = Color(0xFF9E2B25)
    val waxDeep = Color(0xFF6E1915)
    val iron = Color(0xFF37383B)
    val ironHi = Color(0xFF6B6E73)
    val copper = Color(0xFFB8693A)
    val copperHi = Color(0xFFE8A37A)
    val silver = Color(0xFFBFC4CA)
    val silverHi = Color(0xFFF4F6F8)
    val glass = Color(0xFF8FC2D3)
    val bronze = Color(0xFF94652C)
    val bronzeHi = Color(0xFFD7A766)
    val steel = Color(0xFF3F4852)
    val steelHi = Color(0xFF7D8A96)
    val gold = Color(0xFFE2BC55)
    val stone = Color(0xFF6F6A64)
    val stoneHi = Color(0xFF9C968E)
    val stoneDeep = Color(0xFF45413C)
    val water = Color(0xFF2F5D6B)
    val scrim = Color(0xD90B1611)
}

val Display = FontFamily(Font(R.font.prata, FontWeight.Normal))
val Serif = FontFamily(
    Font(R.font.pt_serif_regular, FontWeight.Normal),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
    Font(R.font.pt_serif_italic, FontWeight.Normal, FontStyle.Italic),
)
/** The chamber's stamps and labels. */
val Mono = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

object T {
    val Title = TextStyle(fontFamily = Display, fontSize = 27.sp, lineHeight = 33.sp, color = C.chalk)
    val Heading = TextStyle(fontFamily = Display, fontSize = 20.sp, lineHeight = 26.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Serif, fontSize = 16.sp, lineHeight = 23.sp, color = C.ink)
    val BodyB = Body.copy(fontWeight = FontWeight.Bold)
    val Italic = Body.copy(fontStyle = FontStyle.Italic)
    val Small = TextStyle(fontFamily = Serif, fontSize = 13.5.sp, lineHeight = 19.sp, color = C.muted)
    val Label = TextStyle(fontFamily = Mono, fontSize = 11.5.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp, color = C.muted)
    val Button = TextStyle(fontFamily = Mono, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 1.sp)
    /** The argument on a weight, as it is read on the scale. */
    val Engraving = TextStyle(fontFamily = Serif, fontSize = 19.sp, lineHeight = 27.sp, color = C.ink)
}

@Composable
fun ChamberTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.brass, onPrimary = C.woodDeep, background = C.baize, surface = C.paper, onSurface = C.ink,
            onBackground = C.chalk, outline = C.line, error = C.wax,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
