package app.counterweight

import androidx.test.core.app.ApplicationProvider
import app.counterweight.data.ChamberDb
import app.counterweight.data.Gender
import app.counterweight.data.Repo
import app.counterweight.domain.Catalog
import app.counterweight.domain.Engine
import app.counterweight.domain.Kind
import app.counterweight.domain.Outcome
import app.counterweight.domain.Rules
import app.counterweight.domain.Sphere
import app.counterweight.domain.State
import app.counterweight.ui.Words
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EngineTest {
    private val catalog = Catalog.load { name -> File("src/main/assets/content/$name").takeIf { it.exists() }?.inputStream() }
    private val zone = ZoneId.of("Europe/Berlin")

    private fun repo(): Repo = Repo(ChamberDb.memory(ApplicationProvider.getApplicationContext()), catalog).also { it.zone = zone }

    private fun at(d: LocalDate, h: Int) = d.atTime(h, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun catalogueIsWhole() {
        assertTrue(catalog.universal.size >= 300)
        assertTrue(catalog.night.isNotEmpty())
        assertEquals(catalog.worries.size, catalog.worries.map { it.key }.toSet().size)
        assertEquals(catalog.weights.size, catalog.weights.map { it.key }.toSet().size)
        for (w in catalog.worries) {
            assertTrue(w.key, w.key.startsWith(w.sphere.code + "_"))
            assertTrue(w.key, catalog.of(w.key).size >= 8)
            if (w.heavy) assertTrue(w.key, catalog.of(w.key).any { it.kind == Kind.TRUTH })
        }
        for (w in catalog.weights) {
            assertEquals(w.key, w.text.ru.count { it == '{' }, w.text.ru.count { it == '}' })
            if (w.kind == Kind.FACT || w.kind == Kind.VOICE) assertNotNull(w.key, w.source)
        }
    }

    @Test fun genderMarks() {
        assertEquals("справилась", Words(true, Gender.SHE).g("справил{ся|ась}"))
        assertEquals("справился", Words(true, Gender.HE).g("справил{ся|ась}"))
        assertEquals("сделал(а)", Words(true, null).g("сделал{|а}"))
    }

    @Test fun searchFindsByStems() {
        val w = catalog.worries.firstOrNull { it.sphere == Sphere.MONEY } ?: return
        val stem = w.stems.first { it.length >= 4 }
        assertTrue(catalog.search("боюсь что $stem").any { it.sphere == Sphere.MONEY })
        assertTrue(catalog.search("ab").isEmpty())
    }

    @Test fun crisisWords() {
        assertTrue(Engine.crisis("иногда не хочу жить"))
        assertTrue(Engine.crisis("I want to kill myself"))
        assertFalse(Engine.crisis("боюсь, что уволят"))
    }

    @Test fun queueTakesTurnsAndSkipsSetAside() {
        val w = catalog.worries.first()
        val q = Engine.queue(w, w.sphere, State.EMPTY, catalog, false, Random(1))
        assertEquals(q.size, q.toSet().size)
        val own = catalog.of(w.key).map { it.key }.toSet()
        // The worry's own weights come first, and not two of the same metal in a row at the start.
        val first = q.take(4).mapNotNull { catalog.weight(it) }
        assertTrue(first.all { it.key in own || w.heavy })
        assertEquals(first.size, first.map { it.kind }.toSet().size)
        assertTrue(q.size > own.size)
    }

    @Test fun weighingAndTheLedger() = runBlocking {
        val repo = repo()
        val w = catalog.worries.first()
        val day = LocalDate.of(2026, 10, 1)
        repo.clock = { at(day, 21) }
        val ids = (1..4).map { repo.put(w.key, "", w.sphere, 6) }
        for (id in ids) {
            val q = Engine.queue(w, w.sphere, repo.refresh(), catalog, false, Random(id))
            var mass = 0
            for (k in q) { repo.lay(id, k); mass += Engine.mass(k, catalog); if (mass > Rules.mass(6)) break }
            repo.lifted(id, 3, day.plusDays(7).toEpochDay())
        }
        assertEquals(4, repo.refresh().polish(w.key))
        assertEquals(4, repo.refresh().due(day.plusDays(7).toEpochDay()).size)
        assertNull(Engine.ledger(repo.refresh()))
        repo.check(ids[0], Outcome.NOT); repo.check(ids[1], Outcome.NOT); repo.check(ids[2], Outcome.YES_COPED)
        val rec = Engine.ledger(repo.refresh())!!
        assertEquals(3, rec.checked); assertEquals(2, rec.not); assertEquals(1, rec.survived)
        // The record is cast as a weight and goes on the pan early.
        val q = Engine.queue(w, w.sphere, repo.refresh(), catalog, false, Random(9))
        assertTrue(Rules.LEDGER_KEY in q.take(3))
        assertTrue(Words(true, Gender.SHE).ledger(rec).contains("проверила"))
        // A stone that didn't come true may go to the river.
        assertTrue(Engine.canToss(repo.refresh().stone(ids[0])!!, repo.refresh()))
    }

    @Test fun notMineNeverComesAgain() = runBlocking {
        val repo = repo()
        val w = catalog.worries.first()
        val first = Engine.queue(w, w.sphere, repo.refresh(), catalog, false, Random(2)).first()
        repo.notMine(first)
        assertFalse(first in Engine.queue(w, w.sphere, repo.refresh(), catalog, false, Random(2)))
    }

    @Test fun weightOfTheDayIsStable() {
        val d = LocalDate.of(2026, 10, 26).toEpochDay()
        val a = Engine.ofTheDay(State.EMPTY, catalog, d, 5)
        assertNotNull(a)
        assertEquals(a, Engine.ofTheDay(State.EMPTY, catalog, d, 5))
    }
}
