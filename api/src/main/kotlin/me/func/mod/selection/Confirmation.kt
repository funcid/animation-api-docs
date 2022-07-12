package me.func.mod.selection

import me.func.mod.selection.MenuManager.bind
import org.bukkit.entity.Player
import java.util.UUID
import java.util.function.Consumer

class Confirmation private constructor(
    var text: String,
    var onAccept: Consumer<Player>,
    var onDeny: Consumer<Player>? = null,
) : Openable {
    override var uuid: UUID = UUID.randomUUID()

    constructor(vararg text: String, accept: Consumer<Player>) : this(text.toList(), accept)
    constructor(text: List<String>, accept: Consumer<Player>) : this(text.joinToString("\n"), accept)

    override fun open(player: Player) = apply { bind(player).string(text).send("func:accept", player) }
}
