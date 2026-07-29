<template>
  <div class="order-card" :class="{ 'new-order': isNewOrder, 'urgent': isUrgent }">
    <div class="order-card-header">
      <span class="table-number">{{ tableName }}</span>
      <span class="order-type">{{ order.order_type === 'scan' ? '扫码下单' : '服务员下单' }}</span>
      <div class="time-info">
        <span class="wait-time" :class="{ 'urgent-text': isUrgent }">
          已等 {{ orderStore.getElapsedTime(order.created_at) }}
        </span>
      </div>
    </div>
    <div class="order-card-items">
      <DishItem
        v-for="item in order.items"
        :key="item.id"
        :item="item"
        :order-id="order.id"
      />
    </div>
    <div v-if="order.remark" class="order-card-remark">备注：{{ order.remark }}</div>
    <div class="order-card-timing">
      <span v-if="order.started_at">开始制作：{{ order.started_at.slice(11, 16) }}</span>
      <span v-if="order.started_at" class="duration">
        制作时长：{{ orderStore.getDuration(order.started_at, order.completed_at) }}
      </span>
      <span v-if="order.completed_at">完成时间：{{ order.completed_at.slice(11, 16) }}</span>
    </div>
    <button v-if="order.status !== 'completed'" class="complete-all-btn" @click="handleCompleteAll">
      全部完成
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Order } from '@/types'
import { useOrderStore } from '@/stores/order'
import { useTableStore } from '@/stores/table'
import DishItem from './DishItem.vue'

const props = defineProps<{ order: Order }>()
const orderStore = useOrderStore()

async function handleCompleteAll() {
  await orderStore.completeAllItems(props.order.id)
}
const tableStore = useTableStore()

const tableName = tableStore.getTableById(props.order.table_id)?.number ?? '未知'

const isNewOrder = computed(() => {
  const created = new Date(props.order.created_at.replace(' ', 'T'))
  return Date.now() - created.getTime() < 30000
})

const isUrgent = computed(() => {
  const created = new Date(props.order.created_at.replace(' ', 'T'))
  return Date.now() - created.getTime() > 15 * 60 * 1000
})
</script>

<style scoped>
.order-card {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: border-color 0.3s;
}
.order-card.new-order {
  animation: pulse-border 2s ease-in-out 3;
}
.order-card.urgent {
  border: 2px solid var(--color-primary);
}
@keyframes pulse-border {
  0%, 100% { border: 2px solid transparent; }
  50% { border: 2px solid var(--color-warning); }
}
.order-card-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}
.table-number {
  font-size: var(--font-size-xl);
  font-weight: bold;
  background: var(--color-primary);
  color: white;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
}
.order-type { font-size: var(--font-size-sm); color: var(--color-text-light); }
.time-info { margin-left: auto; text-align: right; }
.wait-time { font-size: var(--font-size-sm); color: var(--color-text-light); }
.wait-time.urgent-text { color: var(--color-primary); font-weight: bold; }
.order-card-remark {
  margin-top: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: #fff3e0;
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  font-weight: 500;
}
.order-card-timing {
  margin-top: var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-light);
  display: flex;
  gap: var(--spacing-md);
}
.duration { font-weight: 500; }
.complete-all-btn {
  width: 100%;
  margin-top: var(--spacing-md);
  padding: var(--spacing-sm);
  background: var(--color-success);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-base);
}
</style>
