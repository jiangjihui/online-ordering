<template>
  <div class="dish-item" :class="item.status" @click="toggleStatus">
    <span class="dish-qty">{{ item.quantity }}x</span>
    <span class="dish-name">{{ item.dish_name }}</span>
    <span v-if="item.remark" class="dish-remark">{{ item.remark }}</span>
    <span class="dish-status">{{ statusLabels[item.status] }}</span>
  </div>
</template>

<script setup lang="ts">
import type { OrderItem } from '@/types'
import { useOrderStore } from '@/stores/order'

const props = defineProps<{ item: OrderItem; orderId: number }>()
const orderStore = useOrderStore()

const statusLabels: Record<string, string> = { pending: '待做', preparing: '制作中', completed: '完成 ✓' }

async function toggleStatus() {
  const nextStatus: Record<string, OrderItem['status']> = {
    pending: 'preparing',
    preparing: 'completed',
    completed: 'pending',
  }
  await orderStore.updateItemStatus(props.orderId, props.item.id, nextStatus[props.item.status])
}
</script>

<style scoped>
.dish-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  gap: 8px;
  transition: background 0.2s;
}
.dish-item.pending { background: #fff3e0; }
.dish-item.preparing { background: #ffebee; }
.dish-item.completed { background: #e8f5e9; opacity: 0.7; }
.dish-qty { font-weight: bold; min-width: 30px; }
.dish-name { flex: 1; }
.dish-remark { color: var(--color-primary); font-size: var(--font-size-sm); font-weight: 500; }
.dish-status { font-size: var(--font-size-sm); }
</style>
