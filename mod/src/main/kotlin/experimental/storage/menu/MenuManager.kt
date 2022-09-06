package experimental.storage.menu

import Main.Companion.menuStack
import dev.xdark.clientapi.item.ItemTools
import dev.xdark.clientapi.resource.ResourceLocation
import dev.xdark.feder.NetUtil
import experimental.storage.AbstractMenu
import experimental.storage.button.StorageItemStack
import experimental.storage.button.StorageItemTexture
import experimental.storage.button.StorageNode
import experimental.storage.menu.selection.SelectionManager
import io.netty.buffer.ByteBuf
import org.lwjgl.input.Mouse
import ru.cristalix.clientapi.KotlinModHolder.mod
import ru.cristalix.uiengine.UIEngine
import java.util.*

class MenuManager {

    var openTimeAndDelayMillis = 0L // Время последнего открытия меню
    val beforeClickDelay = 200 // задержка перед кликом в миллисекундах
    val itemPadding = 4.0

    fun isMenuClickBlocked() = openTimeAndDelayMillis + beforeClickDelay > System.currentTimeMillis()

    fun readIcons(buffer: ByteBuf): MutableList<StorageNode<*>> = MutableList(buffer.readInt()) {
        if (buffer.readBoolean()) { // real item
            StorageItemStack(
                ItemTools.read(buffer), // item
                buffer.readLong(), // price
                NetUtil.readUtf8(buffer).replace("&", "§"), // item title
                NetUtil.readUtf8(buffer).replace("&", "§"), // item description
                NetUtil.readUtf8(buffer).replace("&", "§"), // item hint
                NetUtil.readUtf8(buffer).replace("&", "§"), // item hover desc
                buffer.readBoolean(), // special
            )
        } else { // texture
            StorageItemTexture(
                NetUtil.readUtf8(buffer), // texture
                buffer.readLong(), // price
                NetUtil.readUtf8(buffer).replace("&", "§"), // item title
                NetUtil.readUtf8(buffer).replace("&", "§"), // item description
                NetUtil.readUtf8(buffer).replace("&", "§"), // item hint
                NetUtil.readUtf8(buffer).replace("&", "§"), // item hover desc
                buffer.readBoolean(), // special
            )
        }
    }

    fun push(gui: AbstractMenu) {
        if (menuStack.size > 20)
            menuStack.clear()
        menuStack.push(gui)
        openTimeAndDelayMillis = System.currentTimeMillis()
        gui.open()
    }

    init {
        // Инициализируем менеджера для Selection
        SelectionManager()

        // Открыть меню подтверждения
        mod.registerChannel("func:accept") {
            menuStack.clear()
            Confirmation(UUID.fromString(NetUtil.readUtf8(this)), NetUtil.readUtf8(this)).open()
        }

        // Обновить кнопку
        mod.registerChannel("button:update") {
            val last = if (menuStack.size < 1) return@registerChannel else menuStack.peek()
            val index = readInt()
            if (index < 0 || index >= last.storage.size) return@registerChannel
            val node = last.storage[index]
            val inited = node.bundle != null

            when (readByte().toInt()) {
                0 -> {
                    if (!inited) return@registerChannel
                    val parts = NetUtil.readUtf8(this).split(":", limit = 2)
                    (node as StorageItemTexture).icon.textureLocation =
                        ResourceLocation.of(parts.first(), parts.last())
                }

                1 -> {
                    if (!inited) return@registerChannel
                    (node as StorageItemStack).icon.stack = ItemTools.read(this)
                }

                2 -> {
                    node.title = NetUtil.readUtf8(this).replace("&", "§")
                    if (!inited) return@registerChannel
                    node.titleElement?.content = node.title
                }

                3 -> {
                    node.description = NetUtil.readUtf8(this).replace("&", "§")
                    if (last is PlayChoice || !inited) return@registerChannel
                    node.optimizeSpace()
                }

                4 -> {
                    node.hint = NetUtil.readUtf8(this).replace("&", "§")
                    if (!inited) return@registerChannel
                    node.hintElement?.content = node.hint!!
                }

                5 -> {
                    if (!inited) return@registerChannel
                    node.hoverText = NetUtil.readUtf8(this).replace("&", "§")
                }

                else -> return@registerChannel
            }
        }

        // Открыть меню выбора режима
        mod.registerChannel("storage:choice") {
            push(
                PlayChoice(
                    uuid = UUID.fromString(NetUtil.readUtf8(this)),
                    title = NetUtil.readUtf8(this).replace("&", "§"),
                    description = NetUtil.readUtf8(this).replace("&", "§"),
                    allowClosing = readBoolean(),
                    storage = readIcons(this)
                )
            )
        }

        // Обновление места нахождения текста при наведении
        var mouseX = 0
        var mouseY = 0
        var lastText: String? = null
        UIEngine.postOverlayContext.afterRender {
            if (menuStack.isEmpty()) {
                return@afterRender
            }

            val currentMouseX = Mouse.getX()
            val currentMouseY = Mouse.getY()
            if (mouseX != currentMouseX || mouseY != currentMouseY) {
                mouseX = currentMouseX
                mouseY = currentMouseY

                lastText = null
                val stack = menuStack.peek() ?: return@afterRender
                for (node in stack.storage) {
                    if (node.bundle?.hovered == true) {
                        lastText = node.hoverText
                        break
                    }
                }
            }

            if (lastText.isNullOrEmpty()) {
                return@afterRender
            }

            val resolution = UIEngine.clientApi.resolution()
            val scaleFactor = resolution.scaleFactor

            val x = mouseX / scaleFactor
            val y = resolution.scaledHeight - mouseY / scaleFactor

            val screen = UIEngine.clientApi.minecraft().currentScreen()
            screen?.drawHoveringText(lastText!!.split("\n"), x, y)
        }
    }
}