package me.dev.feature.item.data

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object ItemTable : IntIdTable(name = "items") {

    val name =                  varchar("name", 128)
    val description =           text("description").clientDefault { "" }
    val imageUrl =              text("image_url").clientDefault { "" }
    val price =                 float("price").clientDefault { 0f }
    val isActive =              bool("is_active").clientDefault{ false }
    val stock =                 integer("stock").clientDefault{ 0 }

}

class Item(id: EntityID<Int>): IntEntity(id) {

    companion object: IntEntityClass<Item>(ItemTable)

    var name by ItemTable.name
    var description by ItemTable.description
    var imageUrl by ItemTable.imageUrl
    var price by ItemTable.price
    var isActive by ItemTable.isActive
    var stock by ItemTable.stock
}