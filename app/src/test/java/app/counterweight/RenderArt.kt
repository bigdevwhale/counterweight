package app.counterweight

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import app.counterweight.domain.Kind
import app.counterweight.ui.art.Art.scale
import app.counterweight.ui.art.Art.stone
import app.counterweight.ui.art.Art.weight
import app.counterweight.ui.art.StoneLook
import app.counterweight.ui.theme.C
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Draws the scale, the stones and the weights to PNGs (run with `-Prender`): a look at the art without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArt {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }

    private fun render(name: String, w: Int, h: Int, draw: DrawScope.() -> Unit) {
        val image = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat()), draw)
        File(out, "$name.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun scales() {
        val weights = listOf(Kind.FACT, Kind.AFTER, Kind.QUESTION, Kind.POWER, Kind.BRIGHT, Kind.VOICE, Kind.FEATHER, Kind.TRUTH, Kind.OWN, Kind.LEDGER)
        for ((name, tilt, n) in listOf(Triple("scale-empty", 0f, 0), Triple("scale-heavy", 12f, 2), Triple("scale-tipped", -9f, 10))) {
            render(name, 1080, 1000) {
                drawRect(C.baize)
                scale(tilt, if (name == "scale-empty") null else StoneLook(8, 0f, 3), 1f, weights.take(n), 1f, false)
            }
        }
    }

    @Test fun stonesAndWeights() {
        render("stones", 1200, 260) {
            drawRect(C.baize)
            for (i in 0..6) stone(StoneLook(4 + i, i / 6f, 7), Offset(90f + i * 170f, 130f), 50f + i * 5f)
        }
        render("weights", 1100, 160) {
            drawRect(C.paper)
            for ((i, k) in Kind.entries.withIndex()) weight(k, Offset(60f + i * 108f, 130f), 90f)
        }
    }
}

/** The launcher icon drawn from its own vectors into docs/logo.png (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArtIcon {
    @Test fun logo() {
        val c = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        for (id in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            ContextCompat.getDrawable(c, id)!!.apply { setBounds(0, 0, 512, 512); draw(canvas) }
        }
        File("../docs/logo.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
