package experimental.storage.button

import dev.xdark.clientapi.item.ItemStack
import ru.cristalix.uiengine.element.ItemElement
import ru.cristalix.uiengine.utility.V3
import ru.cristalix.uiengine.utility.item

class StorageItemStack(
    icon: ItemStack,
    price: Long,
    vault: String,
    title: String,
    description: String,
    hint: String,
    hover: String,
    special: Boolean
) : StorageNode<ItemElement>(
    price,
    vault,
    title,
    description,
    hint,
    hover,
    item { stack = icon },
    special
) {
    override fun scaling(scale: Double) = icon.apply {
        this.scale = V3(scale / 16.0, scale / 16.0, scale / 16.0)
    }
}
