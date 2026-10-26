package app.counterweight.ui.scale

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import app.counterweight.domain.Engine
import app.counterweight.domain.Kind
import app.counterweight.domain.Rules
import app.counterweight.ui.Home
import app.counterweight.ui.art.Art.weight

/** A weight ready to read: whatever made it, its metal, words and source. */
data class Shown(val key: String, val kind: Kind, val text: String, val source: String?)

/** Finds any weight by key: the catalogue's, an engraved one, or the person's own record. */
fun Home.shown(key: String): Shown? = when {
    key == Rules.LEDGER_KEY -> Engine.ledger(s)?.let { Shown(key, Kind.LEDGER, w.ledger(it), null) }
    key.startsWith("own:") -> s.engraved.firstOrNull { it.key == key }?.let { Shown(key, Kind.OWN, it.text, null) }
    else -> catalog.weight(key)?.let { Shown(key, it.kind, w.t(it.text), it.source) }
}

/** One weight drawn in a box of [size], standing on its base. */
@Composable
fun WeightIcon(kind: Kind, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { weight(kind, Offset(this.size.width / 2, this.size.height * .9f), this.size.height * .78f) }
}
