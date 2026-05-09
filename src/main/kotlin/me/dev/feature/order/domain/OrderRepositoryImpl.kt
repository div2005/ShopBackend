package me.dev.feature.order.domain

import me.dev.feature.item.data.Item
import me.dev.feature.item.data.ItemTable
import me.dev.feature.order.data.Order
import me.dev.feature.order.data.OrderItem
import me.dev.feature.order.data.OrderItemTable
import me.dev.feature.order.domain.mapper.toDTO
import me.dev.feature.order.domain.model.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.time.LocalDateTime
import java.util.*

class OrderRepositoryImpl : OrderRepository {
    override suspend fun getAllOrders(): List<OrderDTO> {
        val orders = suspendTransaction {
            return@suspendTransaction Order.all().sortedBy { i -> i.purchaseDate }.toList()
        }
        return orders.map { o -> o.toDTO() }
    }

    override suspend fun getOrder(uuid: String): OrderDTO? {
        return suspendTransaction {
            return@suspendTransaction Order.findById(UUID.fromString(uuid))?.toDTO()
        }
    }

    override suspend fun createOrder(request: OrderCreateRequest): OrderCreateResponse {
        return suspendTransaction {

            val cart = request.cart

            var totalPrice = 0f

            if (cart.isEmpty()) return@suspendTransaction OrderCreateResponse(status = OrderCreateResponse.StatusType.CART_IS_EMPTY)

            for (item in cart) {
                val foundItem = Item.findById(item.key) ?: return@suspendTransaction OrderCreateResponse(status = OrderCreateResponse.StatusType.ITEM_NOT_FOUND)

                if (!foundItem.isActive) return@suspendTransaction OrderCreateResponse(status = OrderCreateResponse.StatusType.ITEM_NOT_FOUND)

                if (foundItem.stock < item.value || item.value < 1) return@suspendTransaction OrderCreateResponse(status = OrderCreateResponse.StatusType.NO_ENOUGH_ITEMS)

                totalPrice += item.value * foundItem.price
            }

            val order = Order.new {
                this.purchaseDate = LocalDateTime.now()
                this.firstName = request.firstName
                this.email = request.email
                this.phone = request.phone
                this.city = request.city
                this.firstAddress = request.firstAddress
                this.totalPrice = totalPrice
                this.status = OrderStatusType.UNPAID.toString()
            }

            for (item in cart) {
                val _item = Item.findById(item.key)!!
                item.apply { _item.stock -= item.value }
                OrderItem.new {
                    this.item = _item
                    this.order = order
                    this.itemAmount = item.value
                }
            }

            return@suspendTransaction OrderCreateResponse(orderDTO = order.toDTO())
        }
    }

    override suspend fun updateOrder(uuid: String, request: OrderUpdateRequest): OrderDTO? {
        val order = suspendTransaction {
            return@suspendTransaction Order.findById(UUID.fromString(uuid))?.apply {
                this.firstName = request.firstName
                this.email = request.email
                this.phone = request.phone
                this.city = request.city
                this.firstAddress = request.firstAddress
                this.status = request.status
            }
        }
        return order?.toDTO()
    }

    override suspend fun updateOrderStatus(uuid: String, request: OrderUpdateStatusRequest): OrderDTO? {
        val order = suspendTransaction {

            try {
                OrderStatusType.valueOf(request.status.uppercase())
            } catch (e: IllegalArgumentException) {
                return@suspendTransaction null
            }

            return@suspendTransaction Order.findById(UUID.fromString(uuid))?.apply {
                this.status = request.status
            }
        }
        return order?.toDTO()
    }

    override suspend fun getAllOrderItems(uuid: String): List<OrderItemInfo> {
        val orderItems = suspendTransaction {
            val order = Order.findById(UUID.fromString(uuid)) ?: return@suspendTransaction null
            val orderItems = OrderItem.find { OrderItemTable.order eq order.id }
            val items = Item.find { ItemTable.id inList orderItems.map { it.item.id } }.toList()

            val resList = emptyList<OrderItemInfo>().toMutableList()

            orderItems.forEach { orderItem ->
                resList += OrderItemInfo(
                    itemId = orderItem.item.id.value,
                    itemAmount = orderItem.itemAmount,
                    name = items.find { it.id == orderItem.item.id }?.name ?: "Item Name",
                    imageUrl = items.find { it.id == orderItem.item.id }?.imageUrl ?: "",
                )
            }

            return@suspendTransaction resList
        } ?: return emptyList()

        return orderItems
    }
}