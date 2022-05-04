package me.func.protocol.dialog

class Screen(val text: List<String>) {

    constructor(vararg lines: String) : this(lines.toList())

    var buttons: List<Button>? = null

    fun buttons(vararg buttons: Button?): Screen {
        this.buttons = buttons.filterNotNull().toList()
        return this
    }
}