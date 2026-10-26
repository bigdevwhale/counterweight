package app.counterweight.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import app.counterweight.domain.Kind
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The chamber's sounds, synthesised once into small WAV files: a weight set down on a brass pan (its own
 * ring for each metal), a stone dropped in, the beam tipping over, a pebble into the river. The heavy
 * moments are also felt through the vibration motor.
 */
class Sfx(private val context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(8)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = IntArray(Snd.entries.size)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    @Volatile var on = true

    fun load() {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        for (s in Snd.entries) {
            val f = File(dir, "${s.name.lowercase()}1.wav")
            if (!f.exists()) Wav.write(f, s.make())
            ids[s.ordinal] = pool.load(f.absolutePath, 1)
        }
    }

    fun play(s: Snd, vol: Float = 1f, rate: Float = 1f) {
        if (on && ids[s.ordinal] != 0) pool.play(ids[s.ordinal], vol, vol, 1, 0, rate)
    }

    /** The sound of a weight of [k] set down on the pan. */
    fun weight(k: Kind) = when (k) {
        Kind.FACT, Kind.TRUTH -> { play(Snd.IRON); feel(30, 140) }
        Kind.QUESTION -> play(Snd.GLASS)
        Kind.FEATHER -> play(Snd.FEATHER, .8f)
        Kind.POWER -> play(Snd.CLACK, .9f, .82f)
        Kind.BRIGHT -> play(Snd.CLACK, .9f, 1.25f)
        Kind.VOICE -> play(Snd.CLACK, 1f, .7f)
        else -> { play(Snd.CLACK); feel(18, 90) }
    }

    /** A short buzz of [ms] at [amp] (1–255). */
    fun feel(ms: Long, amp: Int = 80) {
        runCatching { if (vibrator?.hasVibrator() == true) vibrator.vibrate(VibrationEffect.createOneShot(ms, amp.coerceIn(1, 255))) }
    }
}

enum class Snd(val make: () -> ShortArray) {
    CLACK(Synth::clack), IRON(Synth::iron), GLASS(Synth::glass), FEATHER(Synth::feather), STONE(Synth::stone),
    TIP(Synth::tip), SPLASH(Synth::splash), TICK(Synth::tick)
}

object Synth {
    const val RATE = 22050

    private fun pcm(seconds: Double, f: (t: Double, i: Int) -> Double): ShortArray {
        val n = (RATE * seconds).toInt()
        return ShortArray(n) { i -> (f(i / RATE.toDouble(), i) * 32000).toInt().coerceIn(-32767, 32767).toShort() }
    }

    /** A struck metal plate: inharmonic partials that die at different speeds. */
    private fun ring(t: Double, f: Double, decay: Double) =
        (sin(2 * PI * f * t) + .6 * sin(2 * PI * f * 2.41 * t) * exp(-t * 3) + .35 * sin(2 * PI * f * 3.93 * t) * exp(-t * 6)) * exp(-t * decay)

    /** Brass on brass: a click, then the pan's ring. */
    fun clack(): ShortArray {
        val r = Random(3)
        return pcm(.7) { t, _ -> (r.nextDouble() * 2 - 1) * exp(-t * 300) * .5 + ring(t, 1240.0, 7.0) * .32 }
    }

    /** Cast iron set down hard: a dull knock and a low ring of the pan. */
    fun iron(): ShortArray {
        val r = Random(4)
        var lp = 0.0
        return pcm(.8) { t, _ ->
            lp += ((r.nextDouble() * 2 - 1) - lp) * .15
            lp * exp(-t * 60) * 1.4 + sin(2 * PI * (90 - 30 * t) * t) * exp(-t * 25) * .6 + ring(t, 640.0, 6.0) * .25
        }
    }

    /** Glass touching brass: high and clear. */
    fun glass(): ShortArray = pcm(1.0) { t, _ -> (sin(2 * PI * 2637.0 * t) + .5 * sin(2 * PI * 3951.0 * t) * exp(-t * 4)) * exp(-t * 5) * .28 + ring(t, 1240.0, 14.0) * .1 }

    /** A feather landing: a breath of air. */
    fun feather(): ShortArray {
        val r = Random(6)
        var lp = 0.0
        return pcm(.5) { t, _ ->
            lp += ((r.nextDouble() * 2 - 1) - lp) * .05
            val env = sin(PI * (t / .5).coerceIn(0.0, 1.0))
            lp * env * .9
        }
    }

    /** A stone dropped into the pan: a heavy thump, grit, and the whole pan humming. */
    fun stone(): ShortArray {
        val r = Random(8)
        var lp = 0.0
        return pcm(1.2) { t, _ ->
            lp += ((r.nextDouble() * 2 - 1) - lp) * .1
            sin(2 * PI * (70 - 25 * t) * t) * exp(-t * 14) * .9 + lp * exp(-t * 35) * 1.3 + ring(t, 520.0, 4.0) * .22
        }
    }

    /** The beam tipping over: a soft low bell and a higher one, as in an old shop. */
    fun tip(): ShortArray = pcm(2.2) { t, _ ->
        fun b(f: Double, at: Double) = if (t < at) 0.0 else (t - at).let { d -> (sin(2 * PI * f * d) + .4 * sin(2 * PI * f * 2.76 * d) + .15 * sin(2 * PI * f * 5.4 * d)) * exp(-d * 2.2) }
        (b(659.25, 0.0) + .8 * b(987.77, .22)) * .2
    }

    /** A pebble into the river: the plop, then the water closing. */
    fun splash(): ShortArray {
        val r = Random(11)
        var lp = 0.0
        return pcm(1.1) { t, _ ->
            lp += ((r.nextDouble() * 2 - 1) - lp) * .3
            val plop = if (t < .12) sin(2 * PI * (300 + 1400 * t) * t) * exp(-t * 30) * .6 else 0.0
            plop + lp * exp(-(t - .05).coerceAtLeast(0.0) * 7) * .5 * (if (t < .03) t / .03 else 1.0)
        }
    }

    fun tick(): ShortArray = pcm(.04) { t, _ -> sin(2 * PI * 2200 * t) * exp(-t * 140) * .45 }
}

object Wav {
    fun write(f: File, pcm: ShortArray) {
        val data = ByteArrayOutputStream()
        for (s in pcm) { data.write(s.toInt() and 0xFF); data.write((s.toInt() shr 8) and 0xFF) }
        val bytes = data.toByteArray()
        DataOutputStream(f.outputStream().buffered()).use { o ->
            fun le32(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF); o.write((v shr 16) and 0xFF); o.write((v shr 24) and 0xFF) }
            fun le16(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF) }
            o.writeBytes("RIFF"); le32(36 + bytes.size); o.writeBytes("WAVE")
            o.writeBytes("fmt "); le32(16); le16(1); le16(1); le32(Synth.RATE); le32(Synth.RATE * 2); le16(2); le16(16)
            o.writeBytes("data"); le32(bytes.size); o.write(bytes)
        }
    }
}
