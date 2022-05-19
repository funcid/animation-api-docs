package me.func.protocol.alert

import java.util.*

class NotificationData(
    var source: UUID?,
    var type: String?,
    var text: String?,
    var timeoutBarColor: Int,
    var backgroundColor: Int,
    var timeout: Long,
    var buttons: List<NotificationButton>?,
    var chatMessage: String?
) : Cloneable {
    private var original: Boolean = true

    public override fun clone() = if (original) NotificationData(
        source,
        type,
        text,
        timeoutBarColor,
        backgroundColor,
        timeout,
        buttons,
        chatMessage
    ).apply { original = true } else this
}
