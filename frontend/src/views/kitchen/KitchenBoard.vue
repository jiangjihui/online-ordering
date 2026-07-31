<template>
  <div class="kitchen-page">
    <header class="kitchen-header">
      <h2>后厨订单看板</h2>
      <div class="header-right">
        <span class="refresh-label">实时刷新</span>
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </header>

    <!-- 呼叫服务员提醒 -->
    <div v-if="pendingCalls.length > 0" class="call-alerts">
      <div class="call-alert-title">🔔 服务员呼叫</div>
      <div v-for="call in pendingCalls" :key="call.id" class="call-alert-item" @click="handleCall(call.id)">
        <span class="call-table">{{ call.table_number }}号桌</span>
        <span class="call-time">{{ call.created_at.slice(11, 16) }}</span>
        <button class="call-handle-btn">处理</button>
      </div>
    </div>

    <div v-if="pendingOrders.length > 0" class="pending-section">
      <h3>待处理订单</h3>
      <div class="orders-grid">
        <OrderCard v-for="order in pendingOrders" :key="order.id" :order="order" />
      </div>
    </div>
    <EmptyState v-if="pendingOrders.length === 0 && pendingCalls.length === 0" message="暂无待处理订单" />
    <div v-if="completedOrders.length > 0" class="completed-section">
      <h3>已完成订单</h3>
      <div class="orders-grid completed">
        <OrderCard v-for="order in completedOrders" :key="order.id" :order="order" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useOrderStore } from '@/stores/order'
import { useWaiterStore } from '@/stores/waiter'
import OrderCard from '@/components/kitchen/OrderCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useRouter } from 'vue-router'

const orderStore = useOrderStore()
const waiterStore = useWaiterStore()
const router = useRouter()

onMounted(async () => {
  await orderStore.loadData()
  await waiterStore.loadData()
  orderStore.subscribeWebSocket()
  waiterStore.subscribeWebSocket()
})
onUnmounted(() => {
  orderStore.unsubscribeWebSocket()
  waiterStore.unsubscribeWebSocket()
})

const pendingOrders = computed(() => orderStore.getPendingOrders())
const completedOrders = computed(() => orderStore.getCompletedOrders())
const pendingCalls = computed(() => waiterStore.pendingCalls)

async function handleCall(id: number) {
  await waiterStore.handleCall(id)
}

function logout() {
  sessionStorage.removeItem('auth')
  sessionStorage.removeItem('authUser')
  router.push('/login')
}
</script>

<style scoped>
.kitchen-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: var(--spacing-lg);
}
.kitchen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.kitchen-header h2 { margin: 0; }
.header-right { display: flex; align-items: center; gap: var(--spacing-md); }
.refresh-label {
  color: var(--color-success);
  font-size: var(--font-size-sm);
}

/* 呼叫提醒 */
.call-alerts {
  background: #fff3e0;
  border: 2px solid var(--color-warning);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}
.call-alert-title {
  font-weight: bold;
  font-size: var(--font-size-lg);
  margin-bottom: var(--spacing-sm);
}
.call-alert-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-sm) 0;
  gap: var(--spacing-md);
}
.call-table { font-weight: bold; }
.call-time { color: var(--color-text-light); font-size: var(--font-size-sm); }
.call-handle-btn {
  margin-left: auto;
  background: var(--color-success);
  color: white;
  border: none;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-sm);
}

.orders-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-lg);
}
.completed-section { margin-top: var(--spacing-xl); }
.completed .order-card { opacity: 0.6; }
</style>
