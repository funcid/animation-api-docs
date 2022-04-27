package me.func.store

import dev.xdark.feder.NetUtil
import me.func.store.signage.SignageScreen
import me.func.store.signage.button
import me.func.store.util.item
import ru.cristalix.clientapi.KotlinMod
import ru.cristalix.uiengine.UIEngine

const val MARGIN = 4.0

class App : KotlinMod() {

    lateinit var signageScreen: SignageScreen

    override fun onEnable() {
        UIEngine.initialize(this)

        // Создание магазина, в котором определённое количество кнопок.
        registerChannel("store:make") {
            signageScreen = SignageScreen(*MutableList(readInt()) {
                button {
                    title.content = NetUtil.readUtf8(this@registerChannel)
                    icon.stack = item(NetUtil.readUtf8(this@registerChannel))
                }
            }.toTypedArray())
        }
    }
}