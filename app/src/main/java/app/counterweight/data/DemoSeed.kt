package app.counterweight.data

import app.counterweight.AppContainer
import app.counterweight.domain.Engine
import app.counterweight.domain.Engraved
import app.counterweight.domain.Outcome
import app.counterweight.domain.Rules
import app.counterweight.localized
import app.counterweight.ru
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * The demo build's past: about two months of weighings played through the real engine with a wound-back
 * clock, so the shelves, the case and the ledger look lived in. One stone waits on the shelf, one is due today.
 */
object DemoSeed {
    /** What this person worries about most, so a few stones get worn to pebbles. */
    private val SPHERES = listOf("MONEY", "WORK", "HEALTH", "LOVE", "FAMILY", "MIND")

    suspend fun seedIfEmpty(c: AppContainer) {
        val repo = c.repo
        if (repo.refresh().stones.isNotEmpty()) return
        val cat = c.catalog
        if (cat.worries.isEmpty()) return
        val ru = c.app.localized().ru
        c.settings.update {
            it.copy(
                onboarded = true, gender = Gender.HE, salt = 7,
                props = if (ru) "Аня, которая всегда возьмёт трубку\nЯ умею начинать заново\nРуки и голова\nДрузья в Казани" else "Anna, who always picks up\nI know how to start again\nMy hands and my head\nFriends in Leeds",
            )
        }
        val zone = ZoneId.systemDefault()
        repo.zone = zone
        val rnd = Random(42)
        val today = LocalDate.now(zone)
        fun at(d: LocalDate, h: Int, m: Int) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

        val mine = cat.worries.filter { it.sphere.name in SPHERES }.ifEmpty { cat.worries }
        val habits = mine.shuffled(rnd).take(3)
        repo.clock = { at(today.minusDays(60), 12, 0) }
        repo.engrave(Engraved(text = if (ru) "В 2019-м я уже думал, что всё рухнуло. Не рухнуло." else "In 2019 I thought everything had collapsed. It hadn't.", worry = null, sphere = null, at = 0))
        repo.engrave(Engraved(text = if (ru) "Я ни разу не остался без крыши и еды. Ни разу." else "I've never once been left without a roof or food. Not once.", worry = null, sphere = habits.first().sphere, at = 0))

        var d = today.minusDays(58)
        while (d.isBefore(today)) {
            if (rnd.nextFloat() < .45f) { d = d.plusDays(1); continue }
            val worry = if (rnd.nextFloat() < .55f) habits[rnd.nextInt(habits.size)] else mine[rnd.nextInt(mine.size)]
            val day = d
            val hour = if (rnd.nextFloat() < .15f) 2 else 18 + rnd.nextInt(4)
            repo.clock = { at(day, hour, rnd.nextInt(60)) }
            val heft = 4 + rnd.nextInt(6)
            val id = repo.put(worry.key, "", worry.sphere, heft)
            val s = repo.refresh()
            val q = Engine.queue(worry, worry.sphere, s, cat, hour < 5, rnd)
            var mass = 0
            for (k in q) {
                repo.lay(id, k)
                mass += Engine.mass(k, cat)
                if (mass > Rules.mass(heft) && rnd.nextFloat() < .6f) break
            }
            val due = if (rnd.nextFloat() < .75f) day.plusDays(listOf(1L, 3L, 7L, 7L, 30L)[rnd.nextInt(5)]).toEpochDay() else null
            repo.lifted(id, (heft - 2 - rnd.nextInt(4)).coerceAtLeast(1), due)
            if (due != null && due < today.toEpochDay()) {
                val r = rnd.nextFloat()
                repo.clock = { at(LocalDate.ofEpochDay(due), 9, 30) }
                repo.check(id, when { r < .72f -> Outcome.NOT; r < .86f -> Outcome.PARTLY; r < .96f -> Outcome.YES_COPED; else -> Outcome.YES_HARD })
            }
            if (rnd.nextFloat() < .2f) q.take(4).getOrNull(rnd.nextInt(4))?.let { repo.keep(it, true) }
            d = d.plusDays(1)
        }
        // A few old stones that didn't come true have gone to the river.
        val s = repo.refresh()
        for (st in s.stones.filter { it.outcome == Outcome.NOT }.take(5)) repo.toss(st.id)
        // One due today, one waiting on the shelf for the evening.
        repo.clock = { at(today.minusDays(3), 21, 5) }
        val w1 = mine[rnd.nextInt(mine.size)]
        val id1 = repo.put(w1.key, "", w1.sphere, 7)
        Engine.queue(w1, w1.sphere, repo.refresh(), cat, false, rnd).take(6).forEach { repo.lay(id1, it) }
        repo.lifted(id1, 4, today.toEpochDay())
        repo.clock = { at(today, 8, 40) }
        val w2 = habits.last()
        repo.put(w2.key, "", w2.sphere, 6, shelf = at(today, 18, 0))
        repo.clock = System::currentTimeMillis
        repo.refresh()
    }
}
