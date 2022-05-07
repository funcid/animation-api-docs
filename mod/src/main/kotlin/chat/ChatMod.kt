package chat

import dev.xdark.clientapi.event.chat.ChatReceive
import dev.xdark.clientapi.gui.ChatOverlay
import dev.xdark.clientapi.text.Text
import dev.xdark.clientapi.text.TextJSON
import io.netty.buffer.Unpooled
import ru.cristalix.clientapi.KotlinMod
import ru.cristalix.clientapi.readUtf8
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.element.RectangleElement
import ru.cristalix.uiengine.element.TextElement
import ru.cristalix.uiengine.onMouseDown
import ru.cristalix.uiengine.utility.BOTTOM_LEFT
import ru.cristalix.uiengine.utility.CENTER
import ru.cristalix.uiengine.utility.Color
import ru.cristalix.uiengine.utility.V3
import ru.cristalix.uiengine.utility.rectangle
import ru.cristalix.uiengine.utility.text

context(KotlinMod)
class ChatMod {
    private var currentChat = 1

    private val chats: MutableMap<Int, Chat> = LinkedHashMap(4)

    init {
        // Общий межсерверный чат
        chats[1] = createChat("О")
        // Системный чат
        chats[2] = createChat("С")
        // Боевой чат
        chats[3] = createChat("Б")
        // Групповой чат
        chats[4] = createChat("Г")
        // Торговый чат
        chats[5] = createChat("Т")

        val activeColor = Color(42, 102, 189, 1.0)
        val nonActiveColor = Color(alpha = 0.68)

        UIEngine.clientApi.minecraft().ingameUI.chatOverlay = chats[currentChat]!!.overlay

        var offsetX = 5.0
        chats.forEach { (id, chat) ->
            val button = chat.button
            button.color = if (currentChat == id) activeColor else nonActiveColor
            button.offset.x = offsetX
            offsetX += button.size.x + 5.0

            val name = text {
                origin = CENTER
                align = CENTER
                content = chat.name
                shadow = true
            }

            button.onMouseDown {
                chats[currentChat]?.button?.color = nonActiveColor
                currentChat = id
                chats[id]?.button?.color = activeColor
                UIEngine.clientApi.minecraft().ingameUI.chatOverlay = chat.overlay
                chat.unseen = 0
                name.content = chat.name
                button.size.x = 15.0

                var offsetX = 5.0
                chats.values.forEach {
                    val button = it.button
                    button.offset.x = offsetX
                    offsetX += button.size.x + 5.0
                }

                UIEngine.clientApi.clientConnection().sendPayload("zabelix:select_chat", Unpooled.copyInt(currentChat))
            }
            button.addChild(name)
            UIEngine.overlayContext.addChild(button)
        }

        registerChannel("delete-chat") {
            val index = readInt()
            chats[index]!!.button.enabled = false
            chats.remove(index)
        }

        registerChannel("zabelix:chat_message") {
            handleMessage(readInt(), TextJSON.jsonToText(readUtf8()))
        }

        registerHandler<ChatReceive> {
            val raw = text.unformattedText
            if (currentChat != 1 && raw.startsWith("[VC]")) {
                isCancelled = true
                chats[1]!!.overlay.printText(text)
            }
        }
    }

    private fun handleMessage(chatId: Int, message: Text) {
        chats[chatId]?.apply {
            if (currentChat != chatId) {
                unseen++
                val content = "$name §c+$unseen"
                (button.children[0] as TextElement).content = content
                button.size.x = UIEngine.clientApi.fontRenderer().getStringWidth(content) * 1.3
            }

            overlay.printText(message)
        }

        var offsetX = 5.0
        chats.values.forEach {
            val button = it.button
            button.offset.x = offsetX
            offsetX += button.size.x + 5.0
        }

        if (chatId == 1) {
            UIEngine.clientApi.minecraft().ingameUI.defaultChatOverlay.printText(message)
        }
    }

    private fun createChat(name: String): Chat =
        Chat(
            name,
            ChatOverlay.Builder.builder().minecraft(UIEngine.clientApi.minecraft()).build(),
            rectangle {
                align = BOTTOM_LEFT
                origin = BOTTOM_LEFT
                offset.y = -20.0
                size = V3(15.0, 15.0)
            }
        )

    class Chat(
        val name: String,
        val overlay: ChatOverlay,
        var button: RectangleElement,
        var unseen: Int = 0
    )
}