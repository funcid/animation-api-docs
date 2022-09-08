import battlepass.BattlePass
import chat.MultiChat
import com.google.gson.Gson
import dev.xdark.clientapi.event.chat.ChatSend
import dev.xdark.clientapi.event.network.PluginMessage
import dev.xdark.clientapi.gui.ingame.AdvancementsScreen
import dev.xdark.clientapi.gui.ingame.OptionsScreen
import dev.xdark.clientapi.opengl.OpenGlHelper
import dev.xdark.feder.NetUtil
import dialog.DialogMod
import experimental.Experimental
import experimental.storage.AbstractMenu
import experimental.storage.menu.MenuManager
import healthbar.Healthbar
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import lootbox.LootboxMod
import me.func.protocol.Mod
import npc.NPC
import org.lwjgl.opengl.GL20
import ru.cristalix.clientapi.KotlinMod
import ru.cristalix.uiengine.UIEngine
import standard.ExternalManager
import standard.Standard
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.util.Stack

class Main : KotlinMod() {
    companion object {
        lateinit var externalManager: ExternalManager
        var menuStack: Stack<AbstractMenu> = Stack()
        val gson = Gson()
    }

    override fun onEnable() {
        UIEngine.initialize(this)

        registerChannel("anime:loadmod") {
            repeat(readInt() /* Count */) {
                when (Mod.values()[readInt() /* Mod Ordinal */]) {
                    Mod.STANDARD -> Standard()
                    Mod.EXPERIMENTAL -> Experimental.load()
                    Mod.NPC -> NPC()
                    Mod.HEALTHBAR -> Healthbar()
                    Mod.BATTLEPASS -> BattlePass()
                    Mod.LOOTBOX -> LootboxMod()
                    Mod.DIALOG -> DialogMod()
                    Mod.CHAT -> MultiChat()
                    else -> return@repeat
                }
            }
        }

        registerChannel("fiwka:shaders") {
            val shader = NetUtil.readUtf8(this)
            val type = readInt()

            val id = OpenGlHelper.glCreateShader(type)

            GL20.glShaderSource(id, shader)

            OpenGlHelper.glCompileShader(id)

            val program = OpenGlHelper.glCreateProgram()
            OpenGlHelper.glAttachShader(program, id)
            OpenGlHelper.glLinkProgram(program)

            OpenGlHelper.glUseProgram(program)

            GL20.glDetachShader(program, id)
        }

        externalManager = ExternalManager()

        val apiPrefix = "§a§lAPI §7"

        fun message(message: String) = clientApi.chat().printChatMessage(apiPrefix + message)

        registerChannel("anime:debug") {
            val fields = readInt()
            repeat(6) { if (fields > it) message(NetUtil.readUtf8(this).replace(": ", ": §b")) }
        }

        registerChannel("func:close") {
            val mc = UIEngine.clientApi.minecraft()
            val screen = mc.currentScreen()
            if (screen is AdvancementsScreen || screen is OptionsScreen)
                return@registerChannel
            menuStack.clear()
            UIEngine.clientApi.minecraft().displayScreen(null)
        }

        var debugChannels = false

        registerHandler<ChatSend> {
            if (message.startsWith("/func:debug")) {
                clientApi.clientConnection().sendPayload("anime:debug", Unpooled.EMPTY_BUFFER)
                isCancelled = true
            } else if (message.startsWith("/func:channels")) {
                debugChannels = !debugChannels
                message("Статус отладки каналов: $debugChannels")
                isCancelled = true
            }
        }

        registerHandler<PluginMessage> {
            if (debugChannels) message("Канал: §b$channel§7, размер сообщения §b${data.readableBytes()} §7байт.")
        }
    }
}
