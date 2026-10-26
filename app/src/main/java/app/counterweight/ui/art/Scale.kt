package app.counterweight.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import app.counterweight.domain.Kind
import app.counterweight.ui.theme.C
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A stone as drawn: its size from how heavy it felt, its edges from how often it has been outweighed. */
data class StoneLook(val heft: Int, val smooth: Float, val seed: Int)

/** Where things are on the scale, for a canvas of a given size and a beam tilt. */
class ScaleGeometry(val w: Float, val h: Float, tiltDeg: Float) {
    val pivot = Offset(w / 2, h * .2f)
    val floor = h * .94f
    val arm = w * .35f
    val drop = h * .43f
    val panW = w * .3f
    /** A drawing unit: lines and small parts scale with the canvas. */
    val u = w / 400f
    private val a = (tiltDeg * PI / 180).toFloat()
    val left = Offset(pivot.x - arm * cos(a), pivot.y + arm * sin(a))
    val right = Offset(pivot.x + arm * cos(a), pivot.y - arm * sin(a))
    /** The middle of each pan's rim. */
    val leftPan = left + Offset(0f, drop)
    val rightPan = right + Offset(0f, drop)

    fun stoneR(heft: Int) = w * (.04f + heft.coerceIn(1, 10) * .0068f)
    val weightS get() = w * .062f

    /** Where the [i]-th weight of [n] stands on the right pan: centre of its base. */
    fun slot(i: Int): Offset {
        // Rows of four and three in turn, each sitting in the gaps of the one below, like a stacked pile.
        var row = 0
        var left = i
        while (left >= (if (row % 2 == 0) 4 else 3)) { left -= if (row % 2 == 0) 4 else 3; row++ }
        val inRow = if (row % 2 == 0) 4 else 3
        val s = weightS
        return Offset(rightPan.x + (left - (inRow - 1) / 2f) * s * 1.02f, rightPan.y - row * s * .74f)
    }
}

object Art {
    private val BRASS = listOf(C.brassDeep, C.brassHi, C.brass, C.brassDeep)

    /**
     * The whole balance on its counter. [tilt] in degrees, positive with the stone's side down; [stoneIn]
     * 0 while the stone is still above and 1 when it rests; [lastIn] the same for the newest weight.
     */
    fun DrawScope.scale(tilt: Float, stone: StoneLook?, stoneIn: Float, weights: List<Kind>, lastIn: Float, night: Boolean) {
        val g = ScaleGeometry(size.width, size.height, tilt)
        lamp(g, night)
        counter(g)
        column(g)
        pointer(g, tilt)
        beam(g, tilt)
        pan(g, g.left, g.leftPan) {
            if (stone != null) {
                val r = g.stoneR(stone.heft)
                val y = g.leftPan.y - r * .62f - (1 - stoneIn) * size.height * .45f
                if (stoneIn > 0f) stone(stone, Offset(g.leftPan.x, y), r)
            }
        }
        pan(g, g.right, g.rightPan) {
            for ((i, k) in weights.withIndex()) {
                val p = g.slot(i)
                val fall = if (i == weights.lastIndex) (1 - lastIn) * size.height * .35f else 0f
                weight(k, Offset(p.x, p.y - fall), g.weightS)
            }
        }
        pivotCap(g)
    }

    /** The light over the counter: a lamp's warm pool by day, a candle's small one at night. */
    private fun DrawScope.lamp(g: ScaleGeometry, night: Boolean) {
        val c = Offset(g.w / 2, g.pivot.y + g.h * .25f)
        drawCircle(Brush.radialGradient(listOf(C.brassHi.copy(alpha = if (night) .10f else .16f), Color.Transparent), c, g.w * (if (night) .55f else .8f)), g.w * .9f, c)
    }

    private fun DrawScope.counter(g: ScaleGeometry) {
        val top = g.floor
        drawRect(Brush.verticalGradient(listOf(C.wood, C.woodDeep), top, size.height), Offset(0f, top), Size(size.width, size.height - top))
        drawLine(C.brassDeep.copy(alpha = .8f), Offset(0f, top), Offset(size.width, top), 1.2f * g.u)
        // The plinth: a stepped wooden base with a brass foot plate.
        val bw = g.w * .3f
        val bh = g.h * .05f
        val b = Path().apply {
            moveTo(g.pivot.x - bw / 2, top); lineTo(g.pivot.x + bw / 2, top)
            lineTo(g.pivot.x + bw * .38f, top - bh); lineTo(g.pivot.x - bw * .38f, top - bh); close()
        }
        drawPath(b, Brush.verticalGradient(listOf(C.wood, C.woodDeep), top - bh, top))
        drawPath(b, C.woodDeep, style = Stroke(.8f * g.u))
        drawRect(Brush.horizontalGradient(BRASS, g.pivot.x - bw * .3f, g.pivot.x + bw * .3f), Offset(g.pivot.x - bw * .3f, top - bh - g.h * .012f), Size(bw * .6f, g.h * .012f))
        drawOval(Color.Black.copy(alpha = .3f), Offset(g.pivot.x - bw * .62f, top - 1.5f * g.u), Size(bw * 1.24f, 4f * g.u))
    }

    private fun DrawScope.column(g: ScaleGeometry) {
        val cw = g.w * .034f
        val top = g.pivot.y
        val bottom = g.floor - g.h * .062f
        drawRect(Brush.horizontalGradient(BRASS, g.pivot.x - cw / 2, g.pivot.x + cw / 2), Offset(g.pivot.x - cw / 2, top), Size(cw, bottom - top))
        // Rings turned on the column.
        for (f in listOf(.18f, .55f, .92f)) {
            val y = top + (bottom - top) * f
            drawRoundRect(Brush.horizontalGradient(BRASS, g.pivot.x - cw, g.pivot.x + cw), Offset(g.pivot.x - cw * .9f, y - 2f * g.u), Size(cw * 1.8f, 4f * g.u), androidx.compose.ui.geometry.CornerRadius(2f * g.u))
        }
    }

    /** The needle above the pivot over a small engraved arc; it leans with the beam. */
    private fun DrawScope.pointer(g: ScaleGeometry, tilt: Float) {
        val r = g.h * .085f
        val c = g.pivot
        drawArc(C.brassDeep, 245f, 50f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(1.4f * g.u))
        for (i in -4..4) {
            val a = ((-90 + i * 6) * PI / 180).toFloat()
            val l = (if (i == 0) 6f else 3f) * g.u
            drawLine(C.brassHi.copy(alpha = .8f), c + Offset(cos(a) * r, sin(a) * r), c + Offset(cos(a) * (r - l), sin(a) * (r - l)), (if (i == 0) 1.2f else .7f) * g.u)
        }
        rotate(-tilt, c) {
            drawLine(C.ink, c, c + Offset(0f, -r * .96f), 1.4f * g.u, cap = StrokeCap.Round)
            drawCircle(C.wax, 1.8f * g.u, c + Offset(0f, -r * .96f))
        }
    }

    private fun DrawScope.beam(g: ScaleGeometry, tilt: Float) {
        val c = g.pivot
        rotate(-tilt, c) {
            val l = g.arm
            val u = g.u
            val p = Path().apply {
                moveTo(c.x - l, c.y - 2f * u); quadraticTo(c.x - l * .5f, c.y - 3.5f * u, c.x, c.y - 5.5f * u); quadraticTo(c.x + l * .5f, c.y - 3.5f * u, c.x + l, c.y - 2f * u)
                lineTo(c.x + l, c.y + 2f * u); quadraticTo(c.x + l * .5f, c.y + 3f * u, c.x, c.y + 4.5f * u); quadraticTo(c.x - l * .5f, c.y + 3f * u, c.x - l, c.y + 2f * u); close()
            }
            drawPath(p, Brush.verticalGradient(listOf(C.brassHi, C.brass, C.brassDeep), c.y - 5.5f * g.u, c.y + 4.5f * g.u))
            drawPath(p, C.brassDeep, style = Stroke(.6f * g.u))
            // Scrolls at the ends, where the hangers hook on.
            for (s in listOf(-1f, 1f)) {
                drawCircle(Brush.radialGradient(listOf(C.brassHi, C.brassDeep), Offset(c.x + s * l - g.u, c.y - g.u), 4.5f * g.u), 3.5f * g.u, Offset(c.x + s * l, c.y))
                drawCircle(C.brassDeep, 3.5f * g.u, Offset(c.x + s * l, c.y), style = Stroke(.6f * g.u))
            }
        }
    }

    private fun DrawScope.pivotCap(g: ScaleGeometry) {
        val c = g.pivot
        drawCircle(Brush.radialGradient(listOf(C.brassHi, C.brass, C.brassDeep), c + Offset(-2f * g.u, -2f * g.u), 8f * g.u), 6f * g.u, c)
        drawCircle(C.brassDeep, 6f * g.u, c, style = Stroke(.7f * g.u))
        drawCircle(C.brassDeep, 1.5f * g.u, c)
    }

    /** A hanging pan: chains from the beam end, the back of the bowl, what's in it, then the front lip. */
    private fun DrawScope.pan(g: ScaleGeometry, hook: Offset, rim: Offset, inside: DrawScope.() -> Unit) {
        val half = g.panW / 2
        val depth = g.h * .05f
        val chain = PathEffect.dashPathEffect(floatArrayOf(3.5f * g.u, 2f * g.u))
        for (dx in listOf(-half * .92f, half * .92f)) {
            drawLine(C.brassDeep, hook, rim + Offset(dx, 0f), 1.6f * g.u, pathEffect = chain)
            drawLine(C.brassHi.copy(alpha = .7f), hook, rim + Offset(dx, 0f), .6f * g.u, pathEffect = chain)
        }
        drawCircle(C.brassDeep, 2.5f * g.u, hook)
        val rimRect = Rect(rim.x - half, rim.y - depth * .35f, rim.x + half, rim.y + depth * .35f)
        // The inside of the bowl, seen from slightly above.
        drawOval(Brush.verticalGradient(listOf(C.brassDeep, C.brass), rimRect.top, rimRect.bottom), rimRect.topLeft, rimRect.size)
        inside()
        val bowl = Path().apply {
            moveTo(rim.x - half, rim.y)
            cubicTo(rim.x - half * .8f, rim.y + depth * 1.3f, rim.x + half * .8f, rim.y + depth * 1.3f, rim.x + half, rim.y)
            arcTo(rimRect, 0f, 180f, false)
            close()
        }
        drawPath(bowl, Brush.horizontalGradient(BRASS, rim.x - half, rim.x + half))
        drawPath(bowl, C.brassDeep, style = Stroke(.6f * g.u))
        drawArc(C.brassHi, 10f, 160f, false, rimRect.topLeft, rimRect.size, style = Stroke(1f * g.u))
        drawOval(Color.Black.copy(alpha = .22f), Offset(rim.x - half * .7f, g.floor - 2.5f * g.u), Size(half * 1.4f, 5f * g.u))
    }

    /** A stone: lumpy and faceted when new, rounder each time it's outweighed, a flat pebble at the end. */
    fun DrawScope.stone(look: StoneLook, c: Offset, r: Float) {
        val rnd = Random(look.seed)
        val n = 11
        val rough = 1f - look.smooth
        val squash = 1f - look.smooth * .3f
        val pts = (0 until n).map { i ->
            val a = i * 2 * PI.toFloat() / n + rnd.nextFloat() * .25f
            val rr = r * (1f + (rnd.nextFloat() - .5f) * .38f * rough)
            Offset(c.x + cos(a) * rr, c.y + sin(a) * rr * squash + r * (1 - squash) * .6f)
        }
        val p = Path()
        if (look.smooth < .45f) {
            p.moveTo(pts[0].x, pts[0].y); for (q in pts.drop(1)) p.lineTo(q.x, q.y); p.close()
        } else {
            val mid = { i: Int -> (pts[i % n] + pts[(i + 1) % n]) / 2f }
            p.moveTo(mid(0).x, mid(0).y)
            for (i in 1..n) { val q = pts[i % n]; val m = mid(i); p.quadraticTo(q.x, q.y, m.x, m.y) }
            p.close()
        }
        drawOval(Color.Black.copy(alpha = .28f), Offset(c.x - r * .9f, c.y + r * .55f), Size(r * 1.8f, r * .4f))
        drawPath(p, Brush.radialGradient(listOf(C.stoneHi, C.stone, C.stoneDeep), c + Offset(-r * .35f, -r * .4f), r * 1.5f))
        // Facets and a crack on a rough stone; a soft shine on a polished one.
        if (rough > .3f) {
            for (i in 0 until 3) {
                val q = pts[(i * 4) % n]
                drawLine(C.stoneDeep.copy(alpha = .55f * rough), c + (q - c) * .25f, q, r * .028f)
            }
            drawLine(C.stoneDeep.copy(alpha = .7f * rough), c + Offset(-r * .1f, -r * .5f), c + Offset(r * .12f, r * .05f), r * .032f)
        }
        repeat(9) {
            val sp = c + Offset((rnd.nextFloat() - .5f) * r * 1.2f, (rnd.nextFloat() - .5f) * r * 1.0f)
            drawCircle(if (rnd.nextBoolean()) C.stoneDeep.copy(alpha = .5f) else C.stoneHi.copy(alpha = .6f), r * .025f, sp)
        }
        drawOval(Color.White.copy(alpha = .08f + .18f * look.smooth), Offset(c.x - r * .55f, c.y - r * .62f), Size(r * .6f, r * .3f))
        drawPath(p, C.stoneDeep.copy(alpha = .6f), style = Stroke(r * .026f))
    }

    /** One weight standing on [base] (the middle of its foot), [s] tall, in its kind's metal and shape. */
    fun DrawScope.weight(kind: Kind, base: Offset, s: Float) {
        drawOval(Color.Black.copy(alpha = .25f), Offset(base.x - s * .38f, base.y - s * .06f), Size(s * .76f, s * .12f))
        when (kind) {
            Kind.AFTER -> knob(base, s, listOf(C.brassDeep, C.brassHi, C.brass, C.brassDeep), C.brassDeep, 1f)
            Kind.BRIGHT -> knob(base, s * .86f, listOf(Color(0xFF8B9198), C.silverHi, C.silver, Color(0xFF7E848B)), Color(0xFF6E747A), 1.15f)
            Kind.OWN -> {
                knob(base, s * 1.05f, listOf(C.brassDeep, Color(0xFFFFEFB0), C.gold, C.brassDeep), C.brassDeep, 1f)
                star(Offset(base.x, base.y - s * .3f), s * .1f, C.brassDeep)
            }
            Kind.LEDGER -> {
                knob(base, s, listOf(C.brassDeep, C.brassHi, C.brass, C.brassDeep), C.brassDeep, 1.1f)
                drawCircle(C.wax, s * .13f, Offset(base.x, base.y - s * .3f))
                drawCircle(C.waxDeep, s * .13f, Offset(base.x, base.y - s * .3f), style = Stroke(s * .014f))
            }
            Kind.FACT -> kettlebell(base, s * 1.05f)
            Kind.POWER -> discs(base, s)
            Kind.QUESTION -> glass(base, s * .9f)
            Kind.VOICE -> hexWeight(base, s)
            Kind.FEATHER -> feather(base, s)
            Kind.TRUTH -> bar(base, s)
        }
    }

    /** The classic cylinder weight with a knob: body, shoulder, neck, ball. */
    private fun DrawScope.knob(b: Offset, s: Float, metal: List<Color>, edge: Color, wide: Float) {
        val bw = s * .62f * wide
        val bh = s * .55f
        val top = b.y - bh
        drawRect(Brush.horizontalGradient(metal, b.x - bw / 2, b.x + bw / 2), Offset(b.x - bw / 2, top), Size(bw, bh))
        drawOval(Brush.horizontalGradient(metal, b.x - bw / 2, b.x + bw / 2), Offset(b.x - bw / 2, b.y - s * .07f), Size(bw, s * .12f))
        drawOval(Brush.verticalGradient(listOf(metal[1], metal[2]), top - s * .06f, top + s * .06f), Offset(b.x - bw / 2, top - s * .06f), Size(bw, s * .12f))
        drawOval(edge, Offset(b.x - bw / 2, top - s * .06f), Size(bw, s * .12f), style = Stroke(s * .014f))
        val nw = s * .16f
        drawRect(Brush.horizontalGradient(metal, b.x - nw / 2, b.x + nw / 2), Offset(b.x - nw / 2, top - s * .2f), Size(nw, s * .17f))
        drawCircle(Brush.radialGradient(listOf(metal[1], metal[2], metal[3]), Offset(b.x - s * .05f, top - s * .3f), s * .2f), s * .13f, Offset(b.x, top - s * .27f))
        drawLine(edge.copy(alpha = .5f), Offset(b.x - bw / 2, b.y), Offset(b.x - bw / 2, top), s * .012f)
        drawLine(edge.copy(alpha = .5f), Offset(b.x + bw / 2, b.y), Offset(b.x + bw / 2, top), s * .012f)
    }

    /** A cast-iron kettlebell (гиря): the heaviest metal, for facts. */
    private fun DrawScope.kettlebell(b: Offset, s: Float) {
        val r = s * .34f
        val c = Offset(b.x, b.y - r * .95f)
        drawArc(C.iron, 200f, 140f, false, Offset(c.x - r * .72f, c.y - r * 1.75f), Size(r * 1.44f, r * 1.7f), style = Stroke(s * .1f, cap = StrokeCap.Round))
        drawArc(C.ironHi.copy(alpha = .6f), 215f, 60f, false, Offset(c.x - r * .72f, c.y - r * 1.75f), Size(r * 1.44f, r * 1.7f), style = Stroke(s * .03f, cap = StrokeCap.Round))
        drawCircle(Brush.radialGradient(listOf(C.ironHi, C.iron, Color(0xFF1C1D1F)), c + Offset(-r * .35f, -r * .4f), r * 1.4f), r, c)
        drawRect(C.iron, Offset(b.x - r * .7f, b.y - r * .25f), Size(r * 1.4f, r * .25f))
    }

    /** Copper slotted discs, stacked: small deeds add up. */
    private fun DrawScope.discs(b: Offset, s: Float) {
        val w = s * .74f
        val h = s * .16f
        val metal = listOf(Color(0xFF7A3F1E), C.copperHi, C.copper, Color(0xFF7A3F1E))
        for (i in 0 until 3) {
            val y = b.y - i * h * 1.05f
            drawRect(Brush.horizontalGradient(metal, b.x - w / 2, b.x + w / 2), Offset(b.x - w / 2, y - h), Size(w, h))
            drawOval(Brush.horizontalGradient(metal, b.x - w / 2, b.x + w / 2), Offset(b.x - w / 2, y - h * .3f), Size(w, h * .6f))
            drawOval(Brush.verticalGradient(listOf(C.copperHi, C.copper), y - h * 1.3f, y - h * .7f), Offset(b.x - w / 2, y - h * 1.3f), Size(w, h * .6f))
        }
        drawLine(Color(0xFF5A2C12), Offset(b.x, b.y - h * 3.3f), Offset(b.x + w * .1f, b.y - h * 3.1f), s * .025f)
    }

    /** A glass drop, see-through: a question holds no metal, only light. */
    private fun DrawScope.glass(b: Offset, s: Float) {
        val r = s * .3f
        val c = Offset(b.x, b.y - r)
        val p = Path().apply {
            moveTo(c.x, c.y - r * 2.1f)
            cubicTo(c.x + r * .5f, c.y - r * 1.2f, c.x + r * 1.05f, c.y - r * .4f, c.x + r, c.y + r * .1f)
            cubicTo(c.x + r * .95f, c.y + r * 1.05f, c.x - r * .95f, c.y + r * 1.05f, c.x - r, c.y + r * .1f)
            cubicTo(c.x - r * 1.05f, c.y - r * .4f, c.x - r * .5f, c.y - r * 1.2f, c.x, c.y - r * 2.1f)
            close()
        }
        drawPath(p, Brush.radialGradient(listOf(C.glass.copy(alpha = .35f), C.glass.copy(alpha = .75f)), c, r * 1.4f))
        drawPath(p, Color(0xFF4E8496), style = Stroke(s * .014f))
        drawOval(Color.White.copy(alpha = .85f), Offset(c.x - r * .6f, c.y - r * .6f), Size(r * .32f, r * .6f))
        drawCircle(Color.White.copy(alpha = .6f), r * .08f, Offset(c.x + r * .45f, c.y + r * .35f))
    }

    /** A bronze hexagonal weight: the voices of old. */
    private fun DrawScope.hexWeight(b: Offset, s: Float) {
        val w = s * .7f
        val h = s * .58f
        val metal = listOf(Color(0xFF5E3C14), C.bronzeHi, C.bronze, Color(0xFF5E3C14))
        val body = Path().apply {
            moveTo(b.x - w / 2, b.y); lineTo(b.x + w / 2, b.y); lineTo(b.x + w * .38f, b.y - h); lineTo(b.x - w * .38f, b.y - h); close()
        }
        drawPath(body, Brush.horizontalGradient(metal, b.x - w / 2, b.x + w / 2))
        drawLine(Color(0xFF5E3C14).copy(alpha = .6f), Offset(b.x - w * .14f, b.y), Offset(b.x - w * .12f, b.y - h), s * .012f)
        drawLine(Color(0xFF5E3C14).copy(alpha = .6f), Offset(b.x + w * .14f, b.y), Offset(b.x + w * .12f, b.y - h), s * .012f)
        drawOval(Brush.verticalGradient(listOf(C.bronzeHi, C.bronze), b.y - h - s * .05f, b.y - h + s * .05f), Offset(b.x - w * .38f, b.y - h - s * .05f), Size(w * .76f, s * .1f))
        drawCircle(Brush.radialGradient(listOf(C.bronzeHi, Color(0xFF5E3C14)), Offset(b.x - 2f, b.y - h - s * .14f), s * .14f), s * .09f, Offset(b.x, b.y - h - s * .1f))
    }

    /** A feather lying across the pan: weighs almost nothing, and sometimes that's enough. */
    private fun DrawScope.feather(b: Offset, s: Float) {
        withTransform({ rotate(-18f, b) }) {
            val l = s * .9f
            val p = Path().apply {
                moveTo(b.x - l / 2, b.y - s * .05f)
                cubicTo(b.x - l * .2f, b.y - s * .32f, b.x + l * .3f, b.y - s * .3f, b.x + l / 2, b.y - s * .12f)
                cubicTo(b.x + l * .2f, b.y + s * .02f, b.x - l * .2f, b.y + s * .04f, b.x - l / 2, b.y - s * .05f)
                close()
            }
            drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFFBF0), Color(0xFFD9CDB5)), b.y - s * .3f, b.y))
            drawLine(Color(0xFF9C8A6C), Offset(b.x - l * .62f, b.y - s * .02f), Offset(b.x + l / 2, b.y - s * .12f), s * .016f)
            for (i in 1..5) {
                val x = b.x - l / 2 + i * l / 6
                drawLine(Color(0xFFBFAF92), Offset(x, b.y - s * .07f), Offset(x + l * .07f, b.y - s * .2f), s * .01f)
            }
        }
    }

    /** A dark steel bar: the honest weight, plain and heavy. */
    private fun DrawScope.bar(b: Offset, s: Float) {
        val w = s * .9f
        val h = s * .3f
        drawRect(Brush.verticalGradient(listOf(C.steelHi, C.steel, Color(0xFF242A30)), b.y - h, b.y), Offset(b.x - w / 2, b.y - h), Size(w, h))
        drawRect(C.steelHi.copy(alpha = .7f), Offset(b.x - w / 2, b.y - h), Size(w, 2f))
        drawRect(Color(0xFF1A1E22), Offset(b.x - w / 2, b.y - h), Size(w, h), style = Stroke(s * .014f))
    }

    private fun DrawScope.star(c: Offset, r: Float, color: Color) {
        val p = Path()
        for (i in 0 until 10) {
            val a = (-PI / 2 + i * PI / 5).toFloat()
            val rr = if (i % 2 == 0) r else r * .45f
            val q = Offset(c.x + cos(a) * rr, c.y + sin(a) * rr)
            if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y)
        }
        p.close()
        drawPath(p, color)
    }
}
