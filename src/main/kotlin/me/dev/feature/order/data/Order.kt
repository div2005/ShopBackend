package me.dev.feature.order.data

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.javatime.datetime
import java.util.UUID

object OrderTable : UUIDTable(name = "orders") {

    val purchaseDate        = datetime("purchase_date")
    val firstName           = varchar("first_name", 256)
    val email               = varchar("email", 256)
    val phone               = varchar("phone", 9)
    val city                = varchar("city", 256)
    val firstAddress        = text("first_address")
    val totalPrice          = float("total_price")
    val status              = varchar("status", 24)
}

class Order(id: EntityID<UUID>): Entity<UUID>(id){
    companion object: EntityClass<UUID, Order>(OrderTable)

    var purchaseDate by OrderTable.purchaseDate
    var firstName by OrderTable.firstName
    var email by OrderTable.email
    var phone by OrderTable.phone
    var city by OrderTable.city
    var firstAddress by OrderTable.firstAddress
    var totalPrice by OrderTable.totalPrice
    var status by OrderTable.status
}