package me.dev.feature.order.data

import me.dev.feature.item.data.Item
import me.dev.feature.item.data.ItemTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object OrderItemTable: IntIdTable(name = "order_items"){

    val order = reference("order", OrderTable)
    val item = reference("item", ItemTable)
    val itemAmount = integer("item_amount")

    init{
        index(true, order, item)
    }
}

class OrderItem(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<OrderItem>(OrderItemTable)
    var order by Order referencedOn OrderItemTable.order
    var item by Item referencedOn OrderItemTable.item
    var itemAmount by OrderItemTable.itemAmount
}