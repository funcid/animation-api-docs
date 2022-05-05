package store

import Mod
import dev.xdark.feder.NetUtil
import ru.cristalix.clientapi.KotlinMod
import ru.cristalix.uiengine.UIEngine
import store.signage.SignageScreen
import store.signage.button
import store.util.item

const val MARGIN = 4.0

context(KotlinMod)
class Store : Mod {
    lateinit var signageScreen: SignageScreen

    override fun load() {
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
