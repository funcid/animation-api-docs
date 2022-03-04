package me.func.protocol.personalization

import me.func.protocol.Unique
import java.util.*

data class GraffitiPack(
    override var uuid: UUID,
    var graffiti: MutableList<Graffiti>,
    var title: String,
    var creator: String,
    var price: Int,
    var rare: Int,
    var available: Boolean
) : Unique, Cloneable {
    public override fun clone() =
        GraffitiPack(uuid, graffiti.map { it.clone() }.toMutableList(), title, creator, price, rare, available)
}
