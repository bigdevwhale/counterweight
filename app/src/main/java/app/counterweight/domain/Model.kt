package app.counterweight.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The shelves of the chamber: what a worry is about. The file of each is assets/content/<code>.tsv. */
enum class Sphere(val ru: String, val en: String) {
    MONEY("Деньги", "Money"),
    WORK("На работе", "At work"),
    CAREER("Увольнение и карьера", "Jobs & career"),
    STUDY("Учёба и экзамены", "Study & exams"),
    HEALTH("Здоровье", "Health"),
    MIND("Тревога и сон", "Anxiety & sleep"),
    DEATH("Смерть", "Death"),
    AGE("Возраст и время", "Age & time"),
    LOVE("Любовь и пара", "Love"),
    SINGLE("Одиночество", "Loneliness"),
    FAMILY("Родители и родня", "Family"),
    KIDS("Дети", "Children"),
    FRIENDS("Друзья и люди", "Friends"),
    SHAME("Что подумают люди", "What people think"),
    SELF("Я не справлюсь", "Not good enough"),
    HOME("Жильё и быт", "Home"),
    SAFETY("Безопасность", "Safety"),
    MOVE("Переезд и эмиграция", "Moving abroad"),
    TRAVEL("Дороги и полёты", "Travel & flying"),
    WORLD("Мир и новости", "The world & news"),
    FUTURE("Будущее и выбор", "Future & choices"),
    TECH("Технологии и ИИ", "Tech & AI"),
    LAW("Документы и закон", "Papers & the law"),
    PETS("Питомцы", "Pets");

    val code get() = name.lowercase()
}

/**
 * What a weight is cast from, and so what it carries and how much it weighs on the pan. [OWN] are
 * engraved by the person; [LEDGER] is cast from their own record of worries that did and didn't come true.
 */
enum class Kind(val mass: Int) {
    FACT(3), AFTER(3), POWER(2), BRIGHT(2), QUESTION(2), VOICE(2), FEATHER(1), TRUTH(2), OWN(4), LEDGER(4);
}

/** Text in both languages; what the person writes carries the same words in both. */
data class Tx(val ru: String, val en: String) {
    companion object { fun same(s: String) = Tx(s, s) }
}

/** A worry from the catalogue, as someone would say it to themselves. */
data class Worry(
    val key: String,
    val sphere: Sphere,
    val text: Tx,
    /** Search stems, lower case. */
    val stems: List<String>,
    /** A fear that can truly come true and be terrible: it gets honest weights, not only bright ones. */
    val heavy: Boolean,
)

/** One weight with one argument on it. [worry] is null for weights of a whole sphere, and [sphere] for universal ones. */
data class Weight(
    val key: String,
    val kind: Kind,
    val text: Tx,
    val source: String?,
    val worry: String? = null,
    val sphere: Sphere? = null,
    /** Universal weights meant for the small hours. */
    val night: Boolean = false,
)

/** How it went, when the day of finding out came. */
enum class Outcome { NOT, PARTLY, YES_COPED, YES_HARD }

/** A worry put on the scale: what it was, how heavy, what it was weighed against and how it turned out. */
@Entity(tableName = "stone")
data class Stone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The catalogue worry, or null for one in the person's own words only. */
    val worry: String?,
    /** The person's own words, if they typed any. */
    val words: String,
    val sphere: Sphere,
    /** How heavy it felt, 1–10. */
    val heft: Int,
    val at: Long,
    /** How heavy it felt after weighing, or null if never weighed through. */
    val after: Int? = null,
    /** On the shelf until this moment (the hour for worries), not weighed yet. */
    val shelf: Long? = null,
    /** The day it will be clear whether it came true (epoch day). */
    val due: Long? = null,
    val outcome: Outcome? = null,
    val checkedAt: Long? = null,
    /** A pebble thrown into the river: gone from the shelves. */
    val tossed: Boolean = false,
) {
    val lifted get() = after != null
}

/** A weight laid on the pan for a stone. */
@Entity(tableName = "laid")
data class Laid(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stone: Long,
    val weight: String,
    val at: Long,
)

enum class MarkKind { KEEP, NOT_MINE }

/** A weight kept in the case, or set aside as not one's own. */
@Entity(tableName = "mark", primaryKeys = ["weight", "kind"])
data class Mark(val weight: String, val kind: MarkKind, val at: Long)

/** A weight engraved by the person; weighed like any other under the key "own:<id>". */
@Entity(tableName = "engraved")
data class Engraved(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    /** The worry it was made for, or null for any. */
    val worry: String?,
    val sphere: Sphere?,
    val at: Long,
) {
    val key get() = "own:$id"
}

object Rules {
    /** Smoothness steps: each stone of the same worry outweighed polishes it; at [PEBBLE] it is a pebble. */
    const val PEBBLE = 6
    /** Records needed before the person's own record is cast as a weight. */
    const val LEDGER_MIN = 3
    const val LEDGER_KEY = "ledger"

    /** What a stone weighs on the pan: a little more than its heft, so a light one still needs two weights. */
    fun mass(heft: Int) = heft * 2 + 1

    /** Beam tilt for a difference of mass, in degrees; positive is the stone's side down. */
    fun tilt(stone: Int, weights: Int): Float = ((stone - weights) * 2.2f).coerceIn(-14f, 14f)
}

class State(val stones: List<Stone>, val laid: List<Laid>, val marks: List<Mark>, val engraved: List<Engraved>) {
    val kept: List<String> = marks.filter { it.kind == MarkKind.KEEP }.sortedByDescending { it.at }.map { it.weight }
    val notMine: Set<String> = marks.filter { it.kind == MarkKind.NOT_MINE }.map { it.weight }.toSet()
    val seen: Set<String> = laid.map { it.weight }.toSet()
    fun stone(id: Long) = stones.firstOrNull { it.id == id }
    fun laidOn(id: Long) = laid.filter { it.stone == id }
    fun isKept(key: String) = marks.any { it.weight == key && it.kind == MarkKind.KEEP }

    /** Stones still lying about: not thrown away. */
    val shelved get() = stones.filter { !it.tossed }
    /** Stones waiting on the shelf for the hour of worries. */
    val waiting get() = stones.filter { it.shelf != null && !it.lifted && !it.tossed }
    /** Stones whose day of finding out has come and which have not been checked. */
    fun due(today: Long) = stones.filter { it.due != null && it.due <= today && it.outcome == null }
    val checked get() = stones.filter { it.outcome != null }

    /** How smooth a worry has been polished: how many of its stones were outweighed. */
    fun polish(worry: String?): Int = if (worry == null) 0 else stones.count { it.worry == worry && it.lifted }

    companion object { val EMPTY = State(emptyList(), emptyList(), emptyList(), emptyList()) }
}

fun Long.day(zone: ZoneId): Long = Instant.ofEpochMilli(this).atZone(zone).toLocalDate().toEpochDay()
fun Long.date(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
fun Long.hour(zone: ZoneId): Int = Instant.ofEpochMilli(this).atZone(zone).hour
