<template>
  <div class="cart-wrapper">
    <div class="cart-bar" @click="expanded = !expanded">
      <div class="cart-bar-info">
        <span class="cart-count">新增{{ cartStore.totalCount }}件 ¥{{ cartStore.totalAmount }}</span>
        <span v-if="orderedCount > 0" class="cart-total-total">合计 ¥{{ orderedAmount + cartStore.totalAmount }}</span>
      </div>
      <button class="cart-bar-btn" :disabled="cartStore.totalCount === 0" @click.stop="submitOrder">
        下单
      </button>
    </div>

    <div v-if="expanded" class="cart-detail">
      <div v-if="orderedCount > 0" class="cart-ordered-summary">
        <span>已点 {{ orderedCount }} 件 ¥{{ orderedAmount }}</span>
      </div>
      <div v-if="cartStore.totalCount > 0" class="cart-detail-header">
        <span>本次新增</span>
        <button class="clear-btn" @click="cartStore.clearCart()">清空</button>
      </div>
      <div v-if="cartStore.totalCount > 0" class="cart-items">
        <!-- 单品 -->
        <div v-for="item in cartStore.items" :key="`d-${item.dish.id}-${item.remark}`" class="cart-item">
          <img v-if="item.dish.image" :src="imageUrl(item.dish.image)" class="cart-item-thumb" />
          <span class="cart-item-name">{{ item.dish.name }}</span>
          <span v-if="item.remark" class="cart-item-remark">（{{ item.remark }}）</span>
          <div class="cart-item-qty">
            <button @click="cartStore.removeItem(item.dish.id, item.remark)">-</button>
            <span>{{ item.quantity }}</span>
            <button @click="cartStore.addItem(item.dish, item.remark)">+</button>
          </div>
          <span class="cart-item-price">¥{{ item.dish.price * item.quantity }}</span>
        </div>
        <!-- 套餐 -->
        <div v-for="item in cartStore.comboItems" :key="`c-${item.combo.id}-${item.remark}`" class="cart-item cart-combo-item">
          <div class="cart-combo-info">
            <span class="cart-item-name">🎁 {{ item.combo.name }}</span>
            <span v-if="item.remark" class="cart-item-remark">（{{ item.remark }}）</span>
            <div class="cart-combo-dishes">
              <span v-for="ci in item.combo.items" :key="ci.dish_id" class="combo-dish-tag">
                {{ ci.dish_name }}×{{ ci.quantity }}
              </span>
            </div>
          </div>
          <div class="cart-item-qty">
            <button @click="cartStore.removeCombo(item.combo.id, item.remark)">-</button>
            <span>{{ item.quantity }}</span>
            <button @click="cartStore.addCombo(item.combo, item.remark)">+</button>
          </div>
          <span class="cart-item-price combo-price">¥{{ item.combo.price * item.quantity }}</span>
        </div>
      </div>
      <EmptyState v-if="orderedCount === 0 && cartStore.totalCount === 0" message="还没有选择菜品" />
      <div class="cart-remark">
        <input v-model="orderRemark" placeholder="整单备注（如：少放辣、不要葱）" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { useOrderStore } from '@/stores/order'
import { useTableStore } from '@/stores/table'
import { imageUrl } from '@/utils'
import EmptyState from '@/components/common/EmptyState.vue'

const cartStore = useCartStore()
const orderStore = useOrderStore()
const tableStore = useTableStore()
const router = useRouter()

const expanded = ref(false)
const orderRemark = ref('')

const existingOrders = computed(() => orderStore.getOrdersByTable(cartStore.tableId))
const orderedCount = computed(() =>
  existingOrders.value.reduce((sum, o) => sum + o.items.reduce((s, i) => s + i.quantity, 0), 0)
)
const orderedAmount = computed(() =>
  existingOrders.value.reduce((sum, o) => sum + o.total_amount, 0)
)

async function submitOrder() {
  if (cartStore.totalCount === 0) return
  const table = tableStore.getTableById(cartStore.tableId)
  const isFirstOrder = existingOrders.value.length === 0
  try {
    // 单品 + 套餐展开为单品（后厨按单品制作）
    const orderItems = [
      ...cartStore.items.map((item) => ({
        dishId: item.dish.id,
        dishName: item.dish.name,
        quantity: item.quantity,
        remark: item.remark,
      })),
      ...cartStore.comboItems.flatMap((ci) =>
        ci.combo.items.map((dish) => ({
          dishId: dish.dish_id,
          dishName: `${dish.dish_name}[${ci.combo.name}]`,
          quantity: dish.quantity * ci.quantity,
          remark: ci.remark,
        }))
      ),
    ]
    const order = await orderStore.addOrder({
      tableId: cartStore.tableId,
      orderType: 'scan',
      status: 'pending',
      totalAmount: cartStore.totalAmount,
      remark: orderRemark.value,
      items: orderItems,
    })
    if (table) {
      await tableStore.updateTable({ ...table, status: 'dining' })
    }
    cartStore.clearCart()
    ElMessage({
      message: isFirstOrder ? '下单成功，正在为您准备' : '加菜成功',
      type: 'success',
      duration: 2000,
    })
    router.push(`/order-status/${order.id}`)
  } catch (e: any) {
    ElMessage({ message: e.message || '下单失败', type: 'error', duration: 2000 })
  }
}
</script>

<style scoped>
.cart-wrapper { position: fixed; bottom: 0; left: 0; right: 0; z-index: 100; max-width: 480px; margin: 0 auto; }
.cart-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-text);
  color: white;
}
.cart-bar-info { display: flex; gap: var(--spacing-md); }
.cart-count { font-size: var(--font-size-base); }
.cart-total-total { font-size: var(--font-size-lg); font-weight: bold; color: var(--color-text-light); margin-left: var(--spacing-sm); }
.cart-bar-btn {
  background: var(--color-primary);
  color: white;
  border: none;
  padding: var(--spacing-sm) var(--spacing-xl);
  border-radius: var(--radius-md);
  font-size: var(--font-size-lg);
  cursor: pointer;
}
.cart-bar-btn:disabled { opacity: 0.5; cursor: default; }
.cart-detail {
  background: var(--color-white);
  padding: var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}
.cart-detail-header {
  display: flex;
  justify-content: space-between;
  font-weight: bold;
  margin-bottom: var(--spacing-md);
}
.clear-btn { color: var(--color-text-light); background: none; border: none; cursor: pointer; }
.cart-ordered-summary {
  padding: var(--spacing-sm) 0;
  color: var(--color-text-light);
  font-size: var(--font-size-sm);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--spacing-md);
}
.cart-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-sm) 0;
  gap: var(--spacing-sm);
}
.cart-item-thumb { width: 32px; height: 32px; object-fit: cover; border-radius: var(--radius-sm); flex-shrink: 0; }
.cart-item-name { flex: 0 0 auto; }
.cart-item-remark { color: var(--color-text-light); font-size: var(--font-size-sm); }
.cart-item-qty {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-left: auto;
}
.cart-item-qty button {
  width: 24px; height: 24px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-white);
  cursor: pointer;
  font-size: 16px;
}
.cart-item-price { font-weight: bold; min-width: 50px; text-align: right; }

/* 套餐样式 */
.cart-combo-item {
  flex-wrap: wrap;
  padding: var(--spacing-sm) 0;
  border-bottom: 1px dashed var(--color-border);
}
.cart-combo-info { flex: 1; min-width: 0; }
.cart-combo-dishes {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}
.combo-dish-tag {
  font-size: 11px;
  padding: 2px 6px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  color: var(--color-text-light);
}
.combo-price { color: var(--color-primary); }

.cart-remark input {
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-top: var(--spacing-md);
  font-size: var(--font-size-base);
}
</style>
