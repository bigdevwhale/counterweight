package app.counterweight.domain

import java.time.ZoneId
import kotlin.random.Random

/** The chamber's rules, apart from storage so they can be tested. */
object Engine {
    /** The order metals come off the rack: a heavy fact first, then the worst case walked through, then a question… */
    private val ROUND = listOf(Kind.FACT, Kind.AFTER, Kind.QUESTION, Kind.POWER, Kind.BRIGHT, Kind.VOICE, Kind.FEATHER, Kind.TRUTH)

    /** Words that mean someone may be in danger: they get a help screen instead of the scale. */
    private val CRISIS = Regex(
        "суицид|самоубий|покончить с собой|убить себя|убью себя|не хочу жить|не хочется жить|жить не хочу|лучше бы меня не было|выпилиться|вскрыть вены|" +
            "suicid|kill myself|end my life|want to die|don't want to live|dont want to live|better off dead|self[- ]harm|hurt myself",
        RegexOption.IGNORE_CASE,
    )

    fun crisis(text: String) = CRISIS.containsMatchIn(text)

    fun isNight(now: Long, zone: ZoneId) = now.hour(zone) in 0..4

    /**
     * The weights for a stone, in the order they are laid: an honest one first for a heavy fear, then the
     * worry's own weights taking turns by metal (unseen ones first), the person's own record and engraved
     * weights early on, then the sphere's, then the night's or everyone's. Weights set aside never come.
     */
    fun queue(worry: Worry?, sphere: Sphere, s: State, cat: Catalog, night: Boolean, rnd: Random): List<String> {
        val skip = s.notMine
        val mine = worry?.let { cat.of(it.key) }.orEmpty().filter { it.key !in skip }
        val out = mutableListOf<String>()
        if (worry?.heavy == true) mine.filter { it.kind == Kind.TRUTH }.shuffled(rnd).firstOrNull()?.let { out += it.key }
        out += interleave(mine.filter { it.key !in out }, s.seen, rnd).map { it.key }

        val own = s.engraved.filter { e -> (worry != null && e.worry == worry.key) || (e.worry == null && (e.sphere == sphere || e.sphere == null)) }
            .shuffled(rnd).map { it.key }
        if (own.isNotEmpty()) out.add(minOf(2, out.size), own.first())
        if (ledger(s) != null) out.add(minOf(1, out.size), Rules.LEDGER_KEY)
        if (night) cat.night.filter { it.key !in skip }.shuffled(rnd).firstOrNull()?.let { out.add(minOf(1, out.size), it.key) }
        out += own.drop(1)

        out += freshFirst(cat.of(sphere).filter { it.key !in skip }, s.seen, rnd).map { it.key }
        if (night) out += freshFirst(cat.night.filter { it.key !in skip && it.key !in out }, s.seen, rnd).map { it.key }
        out += freshFirst(cat.universal.filter { it.key !in skip }, s.seen, rnd).map { it.key }
        // The same quote can sit on two racks; it is laid only once.
        return out.distinctBy { cat.weight(it)?.text?.ru ?: it }
    }

    private fun freshFirst(ws: List<Weight>, seen: Set<String>, rnd: Random) = ws.shuffled(rnd).sortedBy { if (it.key in seen) 1 else 0 }

    /** One of each metal in turn, so a pan never fills with only facts or only questions. */
    fun interleave(ws: List<Weight>, seen: Set<String>, rnd: Random): List<Weight> {
        val groups = ROUND.associateWith { k -> freshFirst(ws.filter { it.kind == k }, seen, rnd).toMutableList() }
        val out = mutableListOf<Weight>()
        while (groups.values.any { it.isNotEmpty() }) for (k in ROUND) groups[k]!!.removeFirstOrNull()?.let(out::add)
        return out
    }

    /** What a weight weighs: its metal's mass. */
    fun mass(key: String, cat: Catalog): Int = when {
        key == Rules.LEDGER_KEY -> Kind.LEDGER.mass
        key.startsWith("own:") -> Kind.OWN.mass
        else -> cat.weight(key)?.kind?.mass ?: 1
    }

    /** The person's own record of worries checked against what happened. */
    data class Record(val checked: Int, val not: Int, val partly: Int, val coped: Int, val hard: Int) {
        val came get() = partly + coped + hard
        /** Of those that came true even in part, how many were got through. */
        val survived get() = partly + coped
    }

    fun ledger(s: State): Record? {
        val c = s.checked
        if (c.size < Rules.LEDGER_MIN) return null
        return Record(c.size, c.count { it.outcome == Outcome.NOT }, c.count { it.outcome == Outcome.PARTLY }, c.count { it.outcome == Outcome.YES_COPED }, c.count { it.outcome == Outcome.YES_HARD })
    }

    /**
     * The weight of the day, the same all day: from the spheres the person has weighed most if any,
     * kept weights now and then, otherwise from everyone's rack.
     */
    fun ofTheDay(s: State, cat: Catalog, day: Long, salt: Long): String? {
        val rnd = Random(salt xor (day * -7046029254386353131L))
        val spheres = s.stones.groupingBy { it.sphere }.eachCount().entries.sortedByDescending { it.value }.take(3).map { it.key }
        val pool = mutableListOf<String>()
        for (sp in spheres) pool += cat.of(sp).filter { it.key !in s.notMine }.map { it.key }
        pool += cat.universal.filter { it.key !in s.notMine && it.kind != Kind.FEATHER }.map { it.key }
        if (s.kept.isNotEmpty() && rnd.nextFloat() < .25f) return s.kept[rnd.nextInt(s.kept.size)]
        val fresh = pool.filter { it !in s.seen }.ifEmpty { pool }
        return fresh.takeIf { it.isNotEmpty() }?.let { it[rnd.nextInt(it.size)] }
    }

    /** How smooth a worry's stone is drawn: 0 rough, 1 a pebble. */
    fun smooth(polish: Int) = (polish / Rules.PEBBLE.toFloat()).coerceIn(0f, 1f)

    /** A stone can go to the river once it has been worn to a pebble, or once its day showed it didn't come true. */
    fun canToss(st: Stone, s: State) = !st.tossed && (s.polish(st.worry) >= Rules.PEBBLE || st.outcome == Outcome.NOT || (st.after != null && st.after <= 2))

    /** The worst cases of a worry for the well, each as its steps down: the AFTER weights split on their arrows. */
    fun well(worry: Worry?, sphere: Sphere, cat: Catalog, ru: Boolean): List<List<String>> {
        val ws = (worry?.let { cat.of(it.key) }.orEmpty() + cat.of(sphere)).filter { it.kind == Kind.AFTER }
        return ws.map { w -> (if (ru) w.text.ru else w.text.en).split('→').map { it.trim() }.filter { it.isNotEmpty() } }.filter { it.size >= 2 }
    }
}
