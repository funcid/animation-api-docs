package store.util

import dev.xdark.clientapi.item.Item
import dev.xdark.clientapi.item.ItemStack
import dev.xdark.clientapi.nbt.NBTBase
import dev.xdark.clientapi.nbt.NBTTagCompound
import dev.xdark.clientapi.nbt.NBTTagString

// Функция, которая помогает создать предмет id=айди в майнкрафте, item("chest")
fun item(id: String, builder: ItemStack.() -> Unit = {}): ItemStack =
    ItemStack.of(Item.of("minecraft:$id"), 1, 0).also(builder)

var ItemStack.nbt: Pair<String, Any>?
    set(value) {
        val obj = value!!.second
        tagCompound = NBTTagCompound.of(
            // не совать ключ-значение в ( сюда )
            hashMapOf<String?, NBTBase?>().also {
                it[value.first] = when (obj) {
                    is String -> NBTTagString.of(obj)
                    is Map<*, *> -> NBTTagCompound.of(obj as MutableMap<String, NBTBase>?)
                    else -> null
                }
            }
        )
    }
    get() = null
