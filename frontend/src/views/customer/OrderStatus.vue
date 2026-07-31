<template>
  <div class="customer-page">
    <header class="status-header">
      <span>{{ cartStore.tableNumber }}号桌 · {{ tableOrders.length }}笔订单</span>
      <span :class="`status-tag ${overallStatus}`">{{ statusLabels[overallStatus] }}</span>
    </header>
    <div v-if="tableOrders.length > 0" class="orders-list">
      <div v-for="order in tableOrders" :key="order.id" class="order-block">
        <div class="order-block-header">
          <span>订单 #{{ order.id }}</span>
          <span :class="`status-tag small ${order.status}`">{{ statusLabels[order.status] }}</span>
          <span class="order-time">{{ order.created_at.slice(11, 16) }}</span>
          <span v-if="order.remark" class="order-remark">备注：{{ order.remark }}</span>
        </div>
        <div v-for="item in order.items" :key="item.id" class="status-item">
          <span class="item-name">{{ item.dish_name }} x{{ item.quantity }}</span>
          <span v-if="item.remark" class="item-remark">（{{ item.remark }}）</span>
          <span :class="`item-status ${item.status}`">{{ itemStatusLabels[item.status] }}</span>
        </div>
      </div>
    </div>
    <EmptyState v-else message="订单不存在" />
    <div class="status-footer">
      <router-link to="/" class="back-link">返回首页</router-link>
      <button v-if="overallStatus !== 'completed'" class="add-more-btn" @click="router.push(`/order/${cartStore.tableNumber}`)">加菜</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { useCartStore } from '@/stores/cart'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const orderStore = useOrderStore()
const cartStore = useCartStore()

const tableOrders = computed(() =>
  orderStore.getOrdersByTable(cartStore.tableId)
    .filter(o => o.status !== 'completed' || tableOrdersAll.value.length <= 3)
)
const tableOrdersAll = computed(() => orderStore.getOrdersByTable(cartStore.tableId))

const overallStatus = computed(() => {
  const orders = tableOrdersAll.value
  if (orders.length === 0) return 'pending'
  if (orders.every(o => o.status === 'completed')) return 'completed'
  if (orders.some(o => o.status === 'preparing' || o.status === 'completed')) return 'preparing'
  return 'pending'
})

onMounted(async () => {
  await orderStore.loadData()
  orderStore.subscribeWebSocket(cartStore.tableId)
})
onUnmounted(() => {
  orderStore.unsubscribeWebSocket(cartStore.tableId)
})

const statusLabels: Record<string, string> = { pending: '待确认', preparing: '制作中', completed: '已完成' }
const itemStatusLabels: Record<string, string> = { pending: '待做', preparing: '制作中', completed: '已完成 ✓' }
</script>

<style scoped>
.customer-page {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--color-bg);
}
.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-white);
  font-weight: bold;
}
.status-tag {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
}
.status-tag.pending { background: var(--color-warning); color: white; }
.status-tag.preparing { background: var(--color-primary); color: white; }
.status-tag.completed { background: var(--color-success); color: white; }
.status-tag.small { font-size: 12px; padding: 2px 8px; }
.orders-list { padding: var(--spacing-lg); }
.order-block {
  margin-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border);
  padding-bottom: var(--spacing-md);
}
.order-block:last-child { border-bottom: none; }
.order-block-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-weight: bold;
  margin-bottom: var(--spacing-sm);
}
.order-time { color: var(--color-text-light); font-size: var(--font-size-sm); }
.order-remark { color: var(--color-text-light); font-size: var(--font-size-sm); margin-left: auto; }
.status-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-sm) 0;
  gap: var(--spacing-sm);
}
.item-name { font-weight: 500; }
.item-remark { color: var(--color-text-light); font-size: var(--font-size-sm); }
.item-status { margin-left: auto; font-size: var(--font-size-sm); }
.item-status.completed { color: var(--color-success); }
.item-status.preparing { color: var(--color-primary); }
.status-footer {
  display: flex;
  justify-content: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-xl);
}
.back-link { color: var(--color-text-light); }
.add-more-btn {
  background: var(--color-primary);
  color: white;
  border: none;
  padding: var(--spacing-sm) var(--spacing-xl);
  border-radius: var(--radius-md);
  cursor: pointer;
}
</style>
