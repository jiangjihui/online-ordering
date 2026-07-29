import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Order, OrderItem } from '@/types'
import * as api from '@/api'
import { connectWebSocket, onAdminEvent, offAdminEvent, onTableEvent, offTableEvent } from '@/api/websocket'

/** Map an Order object from API camelCase to frontend snake_case */
function mapOrderFromApi(raw: any): Order {
  return {
    id: raw.id,
    table_id: raw.tableId,
    order_type: raw.orderType,
    status: raw.status,
    total_amount: raw.totalAmount,
    remark: raw.remark,
    created_at: raw.createdAt,
    started_at: raw.startedAt ?? undefined,
    completed_at: raw.completedAt ?? undefined,
    items: (raw.items ?? []).map(mapItemFromApi),
  }
}

/** Map an OrderItem from API camelCase to frontend snake_case */
function mapItemFromApi(raw: any): OrderItem {
  return {
    id: raw.id,
    order_id: raw.orderId,
    dish_id: raw.dishId,
    dish_name: raw.dishName,
    quantity: raw.quantity,
    remark: raw.remark,
    status: raw.status,
    started_at: raw.startedAt ?? undefined,
    completed_at: raw.completedAt ?? undefined,
  }
}

export const useOrderStore = defineStore('order', () => {
  const orders = ref<Order[]>([])
  const loaded = ref(false)

  async function loadData() {
    const data = await api.getOrders()
    orders.value = (data as any[]).map(mapOrderFromApi)
    loaded.value = true
  }

  async function addOrder(request: any): Promise<Order> {
    const data = await api.createOrder(request)
    const newOrder = mapOrderFromApi(data)
    orders.value.push(newOrder)
    return newOrder
  }

  function getOrdersByTable(tableId: number): Order[] {
    return orders.value.filter(o => o.table_id === tableId && o.status !== 'closed')
  }

  function getPendingOrders(): Order[] {
    return orders.value.filter(o => o.status !== 'completed' && o.status !== 'closed')
  }

  function getCompletedOrders(): Order[] {
    return orders.value.filter(o => o.status === 'completed')
  }

  async function updateItemStatus(orderId: number, itemId: number, status: OrderItem['status']) {
    await api.updateOrderItemStatus(orderId, itemId, status)

    // Update local state after successful API call
    const order = orders.value.find(o => o.id === orderId)
    if (!order) return
    const item = order.items.find(i => i.id === itemId)
    if (!item) return

    const now = new Date().toISOString().replace('T', ' ').slice(0, 19)

    if (status === 'preparing' && item.status === 'pending') {
      item.started_at = now
    }
    if (status === 'completed') {
      item.completed_at = now
      if (!item.started_at) item.started_at = now
    }
    item.status = status

    // If any item is preparing -> order enters preparing
    if (order.items.some(i => i.status === 'preparing' || i.status === 'completed')) {
      if (order.status === 'pending') {
        order.status = 'preparing'
        order.started_at = now
      }
    }
    // If all items completed -> order completed
    if (order.items.every(i => i.status === 'completed')) {
      order.status = 'completed'
      order.completed_at = now
    }
  }

  async function completeAllItems(orderId: number) {
    await api.completeAllItems(orderId)

    // Update local state after successful API call
    const order = orders.value.find(o => o.id === orderId)
    if (!order) return
    const now = new Date().toISOString().replace('T', ' ').slice(0, 19)
    if (!order.started_at) order.started_at = now
    order.items.forEach(i => {
      i.status = 'completed'
      if (!i.started_at) i.started_at = now
      i.completed_at = now
    })
    order.status = 'completed'
    order.completed_at = now
  }

  function getElapsedTime(createdAt: string): string {
    const created = new Date(createdAt.replace(' ', 'T'))
    const diffMs = Date.now() - created.getTime()
    const mins = Math.floor(diffMs / 60000)
    if (mins < 1) return '刚刚'
    if (mins < 60) return `${mins}分钟前`
    const hours = Math.floor(mins / 60)
    return `${hours}小时${mins % 60}分钟前`
  }

  function getDuration(startedAt?: string, completedAt?: string): string {
    if (!startedAt) return '-'
    const start = new Date(startedAt.replace(' ', 'T'))
    const end = completedAt ? new Date(completedAt.replace(' ', 'T')) : new Date()
    const diffMs = end.getTime() - start.getTime()
    const mins = Math.floor(diffMs / 60000)
    const secs = Math.floor((diffMs % 60000) / 1000)
    if (mins < 1) return `${secs}秒`
    return `${mins}分${secs}秒`
  }

  // WebSocket handlers
  const onItemStatusUpdated = (data: any) => {
    const order = orders.value.find(o => o.id === data.orderId)
    if (!order) return
    const item = order.items.find(i => i.id === data.item.id)
    if (item) {
      item.status = data.item.status
      item.started_at = data.item.startedAt ?? undefined
      item.completed_at = data.item.completedAt ?? undefined
    }
  }

  const onOrderCompleted = (data: any) => {
    const order = orders.value.find(o => o.id === data.orderId)
    if (order) {
      order.status = 'completed'
      order.completed_at = new Date().toISOString().replace('T', ' ').slice(0, 19)
    }
  }

  function subscribeWebSocket(tableId?: number) {
    connectWebSocket()
    onAdminEvent('order.item-status-updated', onItemStatusUpdated)
    onAdminEvent('order.completed', onOrderCompleted)
    if (tableId) {
      onTableEvent(tableId, 'order.item-status-updated', onItemStatusUpdated)
      onTableEvent(tableId, 'order.completed', onOrderCompleted)
    }
  }

  function unsubscribeWebSocket(tableId?: number) {
    offAdminEvent('order.item-status-updated', onItemStatusUpdated)
    offAdminEvent('order.completed', onOrderCompleted)
    if (tableId) {
      offTableEvent(tableId, 'order.item-status-updated', onItemStatusUpdated)
      offTableEvent(tableId, 'order.completed', onOrderCompleted)
    }
  }

  return {
    orders, loaded, loadData, addOrder, getOrdersByTable, getPendingOrders, getCompletedOrders,
    updateItemStatus, completeAllItems, getElapsedTime, getDuration,
    subscribeWebSocket, unsubscribeWebSocket
  }
})
