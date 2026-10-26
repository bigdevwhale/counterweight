package app.counterweight.data

import app.counterweight.domain.Catalog
import app.counterweight.domain.Engraved
import app.counterweight.domain.Laid
import app.counterweight.domain.Mark
import app.counterweight.domain.MarkKind
import app.counterweight.domain.Outcome
import app.counterweight.domain.Sphere
import app.counterweight.domain.State
import app.counterweight.domain.Stone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

/** The chamber's ledger in Room: stones, the weights laid for them, the case and engraved weights. */
class Repo(private val db: ChamberDb, val catalog: Catalog) {
    private val dao = db.dao()
    private val mutex = Mutex()
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    /** The clock; the demo seed winds it back to replay months. */
    var clock: () -> Long = System::currentTimeMillis
    var zone: ZoneId = ZoneId.systemDefault()

    suspend fun refresh(): State = State(dao.stones(), dao.laid(), dao.marks(), dao.engraved()).also { _state.value = it }

    private suspend fun <T> op(block: suspend (State, Long) -> T): T = mutex.withLock {
        val s = refresh()
        block(s, clock()).also { refresh() }
    }

    /** A stone put on the pan, or on the shelf until [shelf]. */
    suspend fun put(worry: String?, words: String, sphere: Sphere, heft: Int, shelf: Long? = null): Long = op { _, now ->
        dao.insert(Stone(worry = worry, words = words.trim(), sphere = sphere, heft = heft.coerceIn(1, 10), at = now, shelf = shelf))
    }

    suspend fun lay(stone: Long, weight: String) = op { _, now -> dao.insert(Laid(stone = stone, weight = weight, at = now)) }

    /** The beam tipped: how heavy it feels now, and the day it will be known whether it came true. */
    suspend fun lifted(stone: Long, after: Int, due: Long?) = op { s, _ ->
        s.stone(stone)?.let { dao.update(it.copy(after = after.coerceIn(0, 10), due = due, shelf = null)) }
    }

    suspend fun shelve(stone: Long, until: Long) = op { s, _ -> s.stone(stone)?.let { dao.update(it.copy(shelf = until)) } }

    suspend fun due(stone: Long, day: Long?) = op { s, _ -> s.stone(stone)?.let { dao.update(it.copy(due = day)) } }

    suspend fun check(stone: Long, o: Outcome) = op { s, now -> s.stone(stone)?.let { dao.update(it.copy(outcome = o, checkedAt = now)) } }

    suspend fun toss(stone: Long) = op { s, _ -> s.stone(stone)?.let { dao.update(it.copy(tossed = true)) } }

    /** Forgets a stone altogether, with its weights. */
    suspend fun forget(stone: Long) = op { _, _ -> dao.deleteLaid(stone); dao.deleteStone(stone) }

    suspend fun keep(weight: String, on: Boolean) = op { _, now ->
        if (on) dao.mark(Mark(weight, MarkKind.KEEP, now)) else dao.unmark(weight, MarkKind.KEEP)
    }

    suspend fun notMine(weight: String, on: Boolean = true) = op { _, now ->
        if (on) dao.mark(Mark(weight, MarkKind.NOT_MINE, now)) else dao.unmark(weight, MarkKind.NOT_MINE)
    }

    suspend fun engrave(e: Engraved): Long = op { _, now ->
        if (e.id == 0L) dao.insert(e.copy(at = now)) else { dao.update(e); e.id }
    }

    suspend fun unengrave(id: Long) = op { _, _ -> dao.deleteEngraved(id); dao.unmark("own:$id", MarkKind.KEEP) }
}
