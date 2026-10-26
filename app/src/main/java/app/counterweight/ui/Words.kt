package app.counterweight.ui

import app.counterweight.data.Gender
import app.counterweight.domain.Engine
import app.counterweight.domain.Kind
import app.counterweight.domain.Outcome
import app.counterweight.domain.Sphere
import app.counterweight.domain.Tx
import java.time.LocalDate
import kotlin.math.abs

/** The chamber's words that come from data rather than resources, in both languages. */
class Words(val ru: Boolean, val gender: Gender?) {
    fun t(r: String, e: String) = if (ru) g(r) else e
    fun t(x: Tx) = t(x.ru, x.en)

    /** Picks the form in Russian {he|she} marks; without a known gender both show, as «справился(ась)». */
    fun g(s: String): String = MARK.replace(s) { m ->
        val (he, she) = m.destructured
        when (gender) {
            Gender.HE -> he
            Gender.SHE -> she
            null -> if (she.startsWith(he)) "$he(${she.removePrefix(he)})" else if (he.isEmpty()) "($she)" else "$he/$she"
        }
    }

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n) % 100; val b = a % 10
        return when { a in 11..19 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    fun n(n: Int, one: String, few: String, many: String, en1: String, enN: String) = if (ru) "$n ${plural(n, one, few, many)}" else "$n ${if (n == 1) en1 else enN}"

    fun sphere(s: Sphere) = if (ru) s.ru else s.en

    /** The metal of a kind, as stamped on its label. */
    fun metal(k: Kind) = when (k) {
        Kind.FACT -> t("чугун · факт", "cast iron · fact")
        Kind.AFTER -> t("латунь · и что тогда?", "brass · and then what?")
        Kind.POWER -> t("медь · в твоих руках", "copper · in your hands")
        Kind.BRIGHT -> t("серебро · другая сторона", "silver · the other side")
        Kind.QUESTION -> t("стекло · вопрос", "glass · a question")
        Kind.VOICE -> t("бронза · голос", "bronze · a voice")
        Kind.FEATHER -> t("пёрышко", "a feather")
        Kind.TRUTH -> t("сталь · честно", "steel · honestly")
        Kind.OWN -> t("золото · твоя гравировка", "gold · your engraving")
        Kind.LEDGER -> t("сургуч · твоя поверка", "sealed · your own record")
    }

    fun kindName(k: Kind) = when (k) {
        Kind.FACT -> t("Факты", "Facts"); Kind.AFTER -> t("И что тогда?", "And then what?"); Kind.POWER -> t("В твоих руках", "In your hands")
        Kind.BRIGHT -> t("Другая сторона", "The other side"); Kind.QUESTION -> t("Вопросы", "Questions"); Kind.VOICE -> t("Голоса", "Voices")
        Kind.FEATHER -> t("Пёрышки", "Feathers"); Kind.TRUTH -> t("Честно", "Honestly"); Kind.OWN -> t("Твои", "Yours"); Kind.LEDGER -> t("Поверка", "Your record")
    }

    /** The weight cast from the person's own record. */
    fun ledger(r: Engine.Record): String = if (ru) {
        val base = "Ты провер{ил|ила} ${n(r.checked, "тревогу", "тревоги", "тревог", "", "")}. Не сбыл${plural(r.not, "ась", "ись", "ись")} — ${r.not}."
        g(if (r.came == 0) "$base Ни одна." else "$base Из сбывшихся с ${r.survived} из ${r.came} ты справил{ся|ась}. Это твоя статистика, не чужая.")
    } else {
        val base = "Of the ${r.checked} worries you checked, ${r.not} didn't come true."
        if (r.came == 0) "$base Not one." else "$base Of those that did, you got through ${r.survived} of ${r.came}. These are your numbers, nobody else's."
    }

    fun outcome(o: Outcome) = when (o) {
        Outcome.NOT -> t("Не сбылось", "Didn't happen")
        Outcome.PARTLY -> t("Сбылось отчасти", "Partly happened")
        Outcome.YES_COPED -> t("Сбылось — и я справил{ся|ась}", "Happened, and I coped")
        Outcome.YES_HARD -> t("Сбылось, и это тяжело", "Happened, and it's hard")
    }

    fun heft(n: Int) = when (n) {
        in 1..2 -> t("песчинка", "a grain of sand")
        in 3..4 -> t("камешек", "a pebble in the shoe")
        in 5..6 -> t("булыжник", "a cobblestone")
        in 7..8 -> t("валун", "a boulder")
        else -> t("скала", "a whole rock")
    }

    fun date(d: LocalDate) = if (ru) "${d.dayOfMonth} ${MONTHS_GEN[d.monthValue - 1]}" else "${MONTHS_EN[d.monthValue - 1]} ${d.dayOfMonth}"
    fun dateY(d: LocalDate) = if (ru) "${d.dayOfMonth} ${MONTHS_GEN[d.monthValue - 1]} ${d.year}" else "${MONTHS_EN[d.monthValue - 1]} ${d.dayOfMonth}, ${d.year}"

    /** «сегодня», «завтра», «через 3 дня», «3 дня назад» for a day relative to today. */
    fun rel(day: Long, today: Long): String {
        val d = (day - today).toInt()
        return when {
            d == 0 -> t("сегодня", "today")
            d == 1 -> t("завтра", "tomorrow")
            d == -1 -> t("вчера", "yesterday")
            d > 1 -> if (ru) "через ${n(d, "день", "дня", "дней", "", "")}" else "in $d days"
            else -> if (ru) "${n(-d, "день", "дня", "дней", "", "")} назад" else "${-d} days ago"
        }
    }

    private companion object {
        val MARK = Regex("\\{([^|{}]*)\\|([^|{}]*)\\}")
        val MONTHS_GEN = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
        val MONTHS_EN = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }
}
