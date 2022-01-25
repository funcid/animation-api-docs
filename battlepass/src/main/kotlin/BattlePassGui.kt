import dev.xdark.clientapi.event.render.ScaleChange
import dev.xdark.clientapi.event.window.WindowResize
import dev.xdark.clientapi.item.ItemStack
import dev.xdark.clientapi.nbt.NBTPrimitive
import dev.xdark.clientapi.nbt.NBTTagCompound
import dev.xdark.clientapi.nbt.NBTTagString
import dev.xdark.clientapi.resource.ResourceLocation
import io.netty.buffer.Unpooled
import me.func.protocol.DropRare
import org.lwjgl.input.Mouse
import ru.cristalix.clientapi.JavaMod
import ru.cristalix.clientapi.registerHandler
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.element.ContextGui
import ru.cristalix.uiengine.element.RectangleElement
import ru.cristalix.uiengine.eventloop.animate
import ru.cristalix.uiengine.utility.*
import kotlin.math.PI
import kotlin.math.roundToInt

class BattlePassGui(
    private val buyBlockText: String,
    private val price: Int,
    private val sale: Double,
    private val levelsCount: Int,
    val pages: List<BattlePage> = listOf(),
    var quests: List<String> = listOf(),
    private val items: List<ItemStack?> = listOf(),
    private val advancedItems: List<ItemStack?> = listOf()
) : ContextGui() {

    var isAdvanced: Boolean = false
    var level: Int = 1
    var exp: Int = 0
    var requiredExp: Int = 1
    var skipPrice: Int = 0

    private val guiSize = BattlePassGuiSize()

    private var battlepass: RectangleElement? = null
    private var moveLeftButton: RectangleElement? = null
    private var moveRightButton: RectangleElement? = null
    private var rewards: RectangleElement? = null

    private val arrowUpItem = loadItem("arrow_up")
    private val addItem = loadItem("add")

    private fun loadItem(tag: String): ItemStack = ItemStack.of(
        NBTTagCompound.of(
            mapOf(
                "id" to NBTTagString.of("clay_ball"),
                "Count" to NBTPrimitive.of(1),
                "Damage" to NBTPrimitive.of(0),
                "tag" to NBTTagCompound.of(
                    mapOf(
                        "other" to NBTTagString.of(tag)
                    )
                )
            )
        )
    )

    private val rewardsCount = 10

    init {
        color = Color(0, 0, 0, 0.86)

        update()

        registerHandler<ScaleChange>(UIEngine.listener) {
            update()
        }
        registerHandler<WindowResize>(UIEngine.listener) {
            update()
        }

        afterRender {
            val hoveredItem = hoveredReward ?: return@afterRender
            JavaMod.clientApi.resolution().run {
                val wholeDescription = hoveredItem.displayName
                screen.drawHoveringText(
                    wholeDescription, Mouse.getX() / 2,
                    (scaledHeight_double * scaleFactor / 2 - Mouse.getY() / 2).toInt()
                )
            }
        }
    }

    fun update() {
        if(battlepass != null) {
            removeChild(battlepass!!)
        }

        guiSize.calculate()
        battlepass = +rectangle {
            align = CENTER
            origin = CENTER
            size = V3(guiSize.totalWidth, guiSize.totalHeight)

            +rectangle {
                align = TOP
                origin = TOP
                size = V3(guiSize.totalWidth, guiSize.totalHeightPart * 22.2)
                color = Color(226, 145, 25, 0.28)

                val buyButtonNeed = !isAdvanced
                val skipButtonNeed = skipPrice != 0

                if (buyButtonNeed) {
                    +rectangle buy@{
                        origin = LEFT
                        align = LEFT
                        size = V3(guiSize.buyButtonWidth, guiSize.buyButtonHeight)
                        color = Color(226, 145, 25, 1.0)
                        offset.x += guiSize.buyButtonOffsetX

                        val buyText = +text {
                            align = CENTER
                            origin = CENTER
                            content = "Купить"
                        }

                        val priceText = +text {
                            enabled = false
                            align = CENTER
                            origin = CENTER
                            content = getPriceText(price)
                        }

                        onClick {
                            close()
                            JavaMod.clientApi.clientConnection().sendPayload("bp:buy-upgrade", Unpooled.buffer())
                        }
                        onHover {
                            animate(0.05) {
                                color = if (this@onHover.hovered) Color(244, 170, 61) else Color(226, 145, 25)
                            }
                            buyText.enabled = !hovered
                            priceText.enabled = !buyText.enabled
                        }
                    }
                }

                if(skipButtonNeed) {
                    +rectangle button@{
                        origin = LEFT
                        align = LEFT
                        size = V3(guiSize.buyButtonWidth, guiSize.buyButtonHeight)

                        color = Color(226, 145, 25, 1.0)
                        offset.x += if(buyButtonNeed) guiSize.totalWidthPart * 30 else guiSize.buyButtonOffsetX

                        val skipText = +text {
                            align = CENTER
                            origin = CENTER
                            content = "Пропустить"
                        }

                        val priceText = +text {
                            enabled = false
                            align = CENTER
                            origin = CENTER
                            content = getPriceText(skipPrice)
                        }

                        onHover {
                            animate(0.05) {
                                color = if (this@onHover.hovered) Color(244, 170, 61) else Color(226, 145, 25)
                            }

                            skipText.enabled = !hovered
                            priceText.enabled = !skipText.enabled
                        }

                        onClick {
                            if (!down) return@onClick
                            close()
                            JavaMod.clientApi.clientConnection().sendPayload("bp:buy-page", Unpooled.buffer())
                        }
                    }
                }

                +text {
                    origin = CENTER
                    align = CENTER
                    val buyBlockTextOffsetX = guiSize.totalWidthPart * 10.5
                    offset.x += if(buyButtonNeed) {
                        if(skipButtonNeed) buyBlockTextOffsetX else buyBlockTextOffsetX - guiSize.buyButtonWidth
                    } else {
                        if(skipButtonNeed) buyBlockTextOffsetX - guiSize.buyButtonWidth else buyBlockTextOffsetX - (guiSize.buyButtonWidth * 2)
                    }
                    content = " $buyBlockText"
                    lineHeight = 12.0
                }
            }

            +rectangle {
                align = CENTER
                origin = CENTER
                size = V3(guiSize.totalWidth, guiSize.totalHeightPart * 22.2)

                offset.y -= guiSize.totalHeightPart * 10.8

                +text {
                    lineHeight = 10.0
                    offset.x += guiSize.totalWidthPart * 1.8
                    content = "Уровень\n    $level"
                }

                val progressLineOffsetX = guiSize.totalWidthPart * 11.9
                +rectangle progress@{
                    color = Color(24, 57, 105)

                    size = V3(guiSize.totalWidthPart * 88.24, guiSize.totalHeightPart * 3.8)
                    offset.x += progressLineOffsetX
                    offset.y += 1.0

                    +rectangle {
                        val progress = if (requiredExp != 0) exp.toDouble() / requiredExp.toDouble() else 1.0
                        size = V3(this@progress.size.x * progress, this@progress.size.y)
                        color = Color(42, 102, 189, 1.0)
                    }
                }

                +text {
                    content = "Опыт: $exp из $requiredExp"
                    offset.x += progressLineOffsetX
                    offset.y += guiSize.totalHeightPart * 4.6
                }
            }

            +text {
                align = BOTTOM_LEFT
                origin = BOTTOM_LEFT

                offset.y -= guiSize.totalHeightPart * 4

                content = if (quests.isEmpty()) " Все задания на сегодня выполнены!" else {
                    " Задания:\n${quests.joinToString(separator = "\n")}"
                }
            }
        }

        rewards = addRewardRows()
        battlepass!! + rewards!!
        moveLeftButton = addMoveButton(true)
        moveRightButton = addMoveButton(false)
        updateMoveButtonsVisibility()
    }

    private fun updateMoveButtonsVisibility() {
        val maxPage = levelsCount / rewardsCount
        moveLeftButton!!.enabled = page > 0
        moveRightButton!!.enabled = page < maxPage
    }

    private fun addMoveButton(isToLeft: Boolean): RectangleElement = rectangle buttonMain@{
        size = V3(guiSize.totalWidthPart * 2.85, guiSize.rewardSizeY * 2.1)
        origin = if (isToLeft) TOP_LEFT else TOP_RIGHT
        align = if (isToLeft) TOP_LEFT else TOP_RIGHT
        offset.x = if (isToLeft) -20.0 else 23.0
        offset.y += guiSize.advancedOffsetY * 2.42
        color = Color(42, 102, 189, 0.62)

        +rectangle {
            size.x = this@buttonMain.size.x * 2 / 3
            size.y = size.x

            offset.y -= 1

            align = CENTER
            origin = CENTER
            color = Color(255, 255, 255)
            textureLocation = ResourceLocation.of("minecraft", "textures/gtm/arrow.png")

            if (isToLeft) rotation.degrees = -PI
        }

        onClick {
            if (enabled && down) {
                page += if (isToLeft) -1 else 1

                battlepass!!.removeChild(rewards!!)
                rewards = addRewardRows()
                battlepass!!.addChild(rewards!!)

                updateMoveButtonsVisibility()
            }
        }
    }.also { battlepass!!.addChild(it) }

    private var page = 0

    private fun addRewardRows(): RectangleElement = rectangle rewardMain@{
        val firstIndex = page * rewardsCount
        val lastIndex = (firstIndex + rewardsCount).coerceAtMost(levelsCount) - 1

        align = CENTER
        origin = CENTER
        size = V3(guiSize.totalWidth, guiSize.totalHeightPart * 45.5)
        offset.y += guiSize.totalHeightPart * 22

        +addRewardRow(
            firstIndex,
            lastIndex,
            false,
            Color(124, 124, 124, 0.62),
            Color(124, 124, 124, 0.28),
            0.0
        )
        +addRewardRow(
            firstIndex,
            lastIndex,
            true,
            Color(226, 132, 44, 0.62),
            Color(185, 122, 27, 0.28),
            guiSize.advancedOffsetY
        )

        val betweenX = guiSize.rewardBetweenX
        var offsetX = (guiSize.totalWidthPart * 11.45) + betweenX
        for (index in firstIndex..lastIndex) {
            +rectangle {
                size = V3(guiSize.totalWidthPart * 8.27, guiSize.totalHeightPart * 7.3)
                offset.x += offsetX
                offsetX += betweenX + (guiSize.totalWidthPart * 8.3)
                offset.y -= guiSize.totalHeightPart * 6.2

                +text {
                    origin = CENTER
                    align = CENTER
                    content = (index + 1).toString()
                }
            }
        }
    }

    private fun addRewardRow(
        firstIndex: Int,
        lastIndex: Int,
        isAdvanced: Boolean,
        firstBlockColor: Color,
        rewardBlockColor: Color,
        offsetY: Double
    ): RectangleElement = rectangle main@{
        color = firstBlockColor
        offset.y += offsetY
        size = V3(guiSize.rewardSizeX, guiSize.rewardSizeY)

        +rectangle {
            size.x = guiSize.totalWidthPart * 3.18
            size.y = size.x
            align = CENTER
            origin = CENTER
            offset.y -= guiSize.totalHeightPart * 2
            color = WHITE
            textureLocation = ResourceLocation.of("minecraft", "textures/gui/kirka.png")
        }
        +text {
            origin = BOTTOM
            align = BOTTOM
            scale = V3(0.9, 0.9)
            offset.y -= guiSize.totalHeightPart * 3.4
            content = if (isAdvanced) "Премиум" else "Базовый"
        }

        var offsetX = guiSize.rewardSizeX + guiSize.rewardBetweenX

        for (i in firstIndex until lastIndex + 1) {
            val item = (if (isAdvanced) advancedItems[i] else items[i]) ?: continue

            var rare = DropRare.COMMON
            var taken = false

            val tagCompound = item.tagCompound
            if (tagCompound != null) {
                val tag = tagCompound.getString("rarity")
                if (tag != null) {
                    rare = DropRare.valueOf(tag.uppercase())
                }

                taken = tagCompound.getBoolean("taken")
            }

            +rectangle rewardMain@{
                color = if(taken) Color(34, 174, 73, 0.72) else rewardBlockColor
                size = V3(guiSize.rewardBlockWidth, guiSize.totalHeightPart * 18.9)
                offset.x += offsetX
                offsetX += guiSize.rewardBetweenX + guiSize.rewardBlockWidth

                +item {
                    origin = CENTER
                    align = CENTER
                    stack = item
                    scale = V3(1.7, 1.7, 1.7)
                }
                +rectangle {
                    align = BOTTOM_LEFT
                    origin = BOTTOM_LEFT
                    size = V3(this@rewardMain.size.x, 2.0)
                    color = Color(rare.red, rare.green, rare.blue)
                }

                onHover {
                    if (hovered) {
                        hoveredReward = item
                    } else if (hoveredReward == item) {
                        hoveredReward = null
                    }
                }

                onClick {
                    if (!down || taken) return@onClick
                    close()
                    JavaMod.clientApi.clientConnection().sendPayload("bp:reward", Unpooled.buffer().apply {
                        writeInt(i)
                        writeBoolean(isAdvanced)
                    })
                }
            }
        }
    }

    private var hoveredReward: ItemStack? = null

    private fun getPriceText(price: Int): String {
        if (sale == 0.0) return "$price"
        return "§m$price§c§l ${(price * (100 - (sale * 100)) / 100.0).roundToInt()}"
    }

}