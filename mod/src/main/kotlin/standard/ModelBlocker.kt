package standard

import dev.xdark.clientapi.event.render.BlockLayerRender
import ru.cristalix.clientapi.JavaMod.clientApi
import ru.cristalix.clientapi.KotlinMod
import ru.cristalix.clientapi.KotlinModHolder.mod
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.element.TextElement
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.utility.CENTER
import ru.cristalix.uiengine.utility.Easings
import ru.cristalix.uiengine.utility.V3
import ru.cristalix.uiengine.utility.WHITE
import ru.cristalix.uiengine.utility.text

class ModelBlocker {
    private var locker: TextElement? = null

    private fun lazyAdd() {
        locker = text {
            color = WHITE
            shadow = true
            scale = V3(3.0, 3.0, 3.0)
            align = CENTER
            origin = CENTER
            enabled = false
        }
        locker!!.content = "Недоступно на\nданном режиме"
        UIEngine.overlayContext + locker!!

        mod.registerHandler<BlockLayerRender> {
            if (inModelMenu()) {
                UIEngine.clientApi.minecraft().displayScreen(null)
                locker!!.enabled = true
                isCancelled = true
                UIEngine.schedule(1.5) {
                    animate(0.5, Easings.BACK_BOTH) {
                        locker!!.scale.x = 0.6
                        locker!!.scale.y = 0.6
                    }
                }
                UIEngine.schedule(2.1) {
                    locker!!.enabled = false
                    locker!!.scale.x = 3.0
                    locker!!.scale.y = 3.0
                }
            }
        }
    }

    init {
        mod.registerChannel("func:break-ui") {
            if (locker == null) lazyAdd()
        }

        mod.registerChannel("func:return-ui") {
            locker = null
        }
    }


    private fun inModelMenu(): Boolean {
        if (locker == null) return false
        val minecraft = clientApi.minecraft()
        val screen = minecraft.currentScreen()
        return !minecraft.inGameHasFocus() && screen != null && screen::class.java.simpleName == "aqW"
    }
}
