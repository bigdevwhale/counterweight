package app.counterweight.domain

import java.io.InputStream

/**
 * The chamber's stock: worries by sphere and the weights cast for each, read from assets/content
 * (see docs/CONTENT.md). Weights get keys from their place: "<worry>#<n>", "@<sphere>#<n>", "u#<n>", "n#<n>".
 */
class Catalog(val worries: List<Worry>, val weights: List<Weight>) {
    private val worryBy = worries.associateBy { it.key }
    private val weightBy = weights.associateBy { it.key }
    private val forWorry = weights.filter { it.worry != null }.groupBy { it.worry!! }
    private val forSphere = weights.filter { it.worry == null && it.sphere != null }.groupBy { it.sphere!! }
    val universal = weights.filter { it.worry == null && it.sphere == null && !it.night }
    val night = weights.filter { it.night }

    fun worry(key: String?) = key?.let { worryBy[it] }
    fun weight(key: String) = weightBy[key]
    fun of(worry: String) = forWorry[worry].orEmpty()
    fun of(sphere: Sphere) = forSphere[sphere].orEmpty()
    fun worriesOf(sphere: Sphere) = worries.filter { it.sphere == sphere }

    /** Worries that match what someone typed, best first; matched on the search stems and the worry's own words. */
    fun search(query: String, n: Int = 8): List<Worry> {
        val q = norm(query)
        if (q.length < 3) return emptyList()
        val words = q.split(SPLIT).filter { it.length >= 3 }
        return worries.map { it to score(it, q, words) }.filter { it.second > 0 }.sortedByDescending { it.second }.take(n).map { it.first }
    }

    /** The sphere most of the typed words point to, for a worry that matches nothing exactly. */
    fun guess(query: String): Sphere? {
        val q = norm(query)
        val words = q.split(SPLIT).filter { it.length >= 3 }
        return worries.groupBy { it.sphere }.mapValues { (_, ws) -> ws.sumOf { score(it, q, words) } }.filterValues { it > 0 }.maxByOrNull { it.value }?.key
    }

    private fun score(w: Worry, q: String, words: List<String>): Int {
        var s = 0
        for (stem in w.stems) if (stem.length >= 3 && q.contains(stem)) s += if (stem.contains(' ')) 5 else 3
        val own = norm(w.text.ru + " " + w.text.en).split(SPLIT).filter { it.length >= 4 }.map { it.take(5) }.toSet()
        for (word in words) if (word.length >= 4 && word.take(5) in own) s += 1
        return s
    }

    companion object {
        private val SPLIT = Regex("[^\\p{L}\\p{N}]+")
        fun norm(s: String) = s.lowercase().replace('ё', 'е').trim()

        /** Reads a sphere's file, or the universal ("universal") and night ("night") ones. */
        fun read(name: String, input: InputStream, into: Builder) {
            val sphere = Sphere.entries.firstOrNull { it.code == name }
            var worry: String? = null
            var n = 0
            var wide = 0
            for (line in input.bufferedReader(Charsets.UTF_8).readLines()) {
                if (line.isBlank() || line.startsWith("#")) continue
                val c = line.split('\t')
                when (c.getOrNull(0)) {
                    "W" -> if (sphere != null && c.size >= 7) {
                        worry = c[1].trim()
                        n = 0
                        into.worries += Worry(
                            worry, sphere, Tx(c[2].trim(), c[3].trim()),
                            (c[4].split(',') + c[5].split(',')).map { norm(it) }.filter { it.isNotEmpty() }.distinct(),
                            c[6].trim() == "HEAVY",
                        )
                    }
                    "G", "@" -> if (c.size >= 5) {
                        val kind = runCatching { Kind.valueOf(c[1].trim()) }.getOrNull() ?: continue
                        val src = c[4].trim().takeUnless { it.isEmpty() || it == "-" }
                        val tx = Tx(c[2].trim(), c[3].trim())
                        into.weights += when {
                            c[0] == "G" && worry != null -> Weight("$worry#${n++}", kind, tx, src, worry, sphere)
                            sphere != null -> Weight("@${sphere.code}#${wide++}", kind, tx, src, null, sphere)
                            name == "night" -> Weight("n#${wide++}", kind, tx, src, night = true)
                            else -> Weight("u#${wide++}", kind, tx, src)
                        }
                    }
                }
            }
        }

        /** Reads every file under assets/content through [open], which may fail for a missing one. */
        fun load(open: (String) -> InputStream?): Catalog {
            val b = Builder()
            for (name in Sphere.entries.map { it.code } + listOf("universal", "night")) open("$name.tsv")?.use { read(name, it, b) }
            return Catalog(b.worries, b.weights)
        }
    }

    class Builder {
        val worries = mutableListOf<Worry>()
        val weights = mutableListOf<Weight>()
    }
}
