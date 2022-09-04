package standard

import Main.Companion.externalManager
import dev.xdark.clientapi.event.gui.ScreenDisplay
import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.clientapi.gui.ingame.ChatScreen
import org.lwjgl.input.Keyboard
import ru.cristalix.clientapi.KotlinModHolder.mod
import ru.cristalix.clientapi.readUtf8
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.utility.*
import kotlin.math.pow
import kotlin.properties.Delegates.notNull

class Boosters {

    private var boosters: Flex by notNull()

    private fun booster(name: String, factor: Double) = carved {
        size = V3(62.5 * 1.055.pow(name.length.toDouble()), 18.0)
        color = Color(0, 0, 0, .62)
        carveSize = 2.0

        +text {
            content = name
            size = V3(16.0, 16.0)
            color = WHITE
            shadow = true
            origin = CENTER
            align = CENTER
            offset.y = 3.5
            offset.x -= 12.0 - (-.3 * name.length)
        }

        +carved {
            size = V3(32.0, 18.0)
            color = Color(40, 180, 0, .62)
            carveSize = 2.0
            origin = RIGHT
            align = RIGHT

            +text {
                content = "x$factor".replace(".0", "")
                size = V3(16.0, 16.0)
                color = WHITE
                origin = CENTER
                align = CENTER
                shadow = true
                offset.y = 3.5
            }
        }
    }

    init {
        val container = flex {
            origin = TOP_RIGHT
            align = TOP_RIGHT
            flexSpacing = 4.0
            size = V3(80.0, 80.0)
            offset.y = 12.0
            offset.x -= 12.0

            +carved {
                size = V3(18.0, 18.0)
                color = Color(0, 0, 0, .62)
                carveSize = 2.0

                +text {
                    content = "+"
                    size = V3(16.0, 16.0)
                    color = WHITE
                    shadow = true
                    origin = CENTER
                    align = CENTER
                    offset.y = 3.5
                    offset.x = 0.5
                }
            }

            boosters = +flex {
                flexSpacing = 4.0
            }
            enabled = false
        }

        mod.registerHandler<ScreenDisplay> {
            if (screen is ChatScreen && container.enabled) {
                container.enabled = false
            } else if (screen !is ChatScreen && boosters.children.isNotEmpty() && !container.enabled) {
                container.enabled = true
            }
        }

        mod.registerHandler<GameLoop> {
           if (Keyboard.isKeyDown(Keyboard.KEY_TAB) && container.enabled) {
               container.enabled = false
           } else if (!Keyboard.isKeyDown(Keyboard.KEY_TAB) && !container.enabled) {
               container.enabled = true
           }
        }

        mod.registerChannel("mid:boost") {
            val count = readInt()
            boosters.enabled = readBoolean()

            boosters.children.forEach {
                boosters.removeChild(it)
            }

            repeat(count) {
                boosters.addChild(booster(readUtf8(), readDouble()))
            }
        }

        UIEngine.overlayContext.addChild(container)
    }
}