<template>
  <div class="customer-page">
    <header class="order-header">
      <span class="table-label">桌号 {{ tableNumber }}</span>
      <div class="header-actions">
        <button class="lang-btn" @click="menuStore.setLang(menuStore.lang === 'zh' ? 'en' : 'zh')">
          {{ menuStore.lang === 'zh' ? 'EN' : '中' }}
        </button>
        <button class="call-btn" @click="callWaiter">🔔</button>
      </div>
    </header>

    <div v-if="tableStatus === 'dining'" class="table-notice">
      该桌正在就餐中，您可以加菜
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input v-model="menuStore.searchQuery" :placeholder="menuStore.lang === 'zh' ? '搜索菜品...' : 'Search dishes...'" class="search-input" />
    </div>

    <!-- 标签筛选 -->
    <div v-if="!menuStore.searchQuery" class="label-filters">
      <span
        v-for="label in allLabels"
        :key="label"
        class="label-chip"
        :class="{ active: menuStore.activeLabels.includes(label) }"
        @click="menuStore.toggleLabel(label)"
      >
        {{ labelNames[label] }}
      </span>
    </div>

    <!-- 套餐区 -->
    <div v-if="!menuStore.searchQuery && menuStore.activeCombos.length > 0" class="combo-section">
      <div class="section-title">{{ menuStore.lang === 'zh' ? '🎁 套餐' : '🎁 Set Meals' }}</div>
      <div v-for="combo in menuStore.activeCombos" :key="combo.id" class="combo-card">
        <div class="combo-info">
          <div class="combo-name">{{ menuStore.comboName(combo) }}</div>
          <div class="combo-desc">{{ combo.description }}</div>
          <div class="combo-items">
            <span v-for="item in combo.items" :key="item.dish_id" class="combo-item-tag">
              {{ item.dish_name }} x{{ item.quantity }}
            </span>
          </div>
          <div class="combo-price">¥{{ combo.price }}</div>
        </div>
        <button class="add-btn" @click="addCombo(combo)">+</button>
      </div>
    </div>

    <CategoryTabs @select="scrollToCategory" />
    <div ref="dishListRef" class="dish-list" @scroll="onDishListScroll">
      <div v-for="cat in menuStore.categories" :key="cat.id" :id="`cat-section-${cat.id}`" class="category-section">
        <div class="section-title">{{ cat.icon }} {{ menuStore.categoryName(cat) }}</div>
        <DishCard v-for="dish in getDishesByCategory(cat.id)" :key="dish.id" :dish="dish" />
        <EmptyState v-if="getDishesByCategory(cat.id).length === 0" :message="menuStore.lang === 'zh' ? '该分类暂无菜品' : 'No dishes in this category'" />
      </div>
    </div>

    <!-- 左下角已点图标 -->
    <div v-if="orderedItemCount > 0" class="ordered-badge" @click="showOrderedPanel = true">
      <div class="badge-icon">📋</div>
      <div class="badge-count">{{ orderedItemCount }}</div>
    </div>

    <!-- 已点菜品底部弹出面板 -->
    <div v-if="showOrderedPanel" class="ordered-overlay" @click="showOrderedPanel = false" />
    <div v-if="showOrderedPanel" class="ordered-panel">
      <div class="ordered-panel-header">
        <span>{{ menuStore.lang === 'zh' ? '已点菜品' : 'Ordered Items' }}</span>
        <button class="panel-close" @click="showOrderedPanel = false">×</button>
      </div>
      <div class="ordered-panel-body">
        <div v-for="order in existingOrders" :key="order.id" class="ordered-group">
          <div v-if="order.remark" class="ordered-remark">{{ menuStore.lang === 'zh' ? '备注' : 'Note' }}：{{ order.remark }}</div>
          <div v-for="item in order.items" :key="item.id" class="ordered-item">
            <span class="ordered-name">{{ item.dish_name }}</span>
            <span v-if="item.remark" class="ordered-item-remark">（{{ item.remark }}）</span>
            <span class="ordered-qty">x{{ item.quantity }}</span>
            <span :class="`ordered-status ${item.status}`">{{ itemStatusLabels[item.status] }}</span>
          </div>
        </div>
      </div>
      <div class="ordered-panel-footer">
        {{ menuStore.lang === 'zh' ? '共' : 'Total' }} {{ orderedItemCount }} {{ menuStore.lang === 'zh' ? '件' : 'items' }} · ¥{{ orderedTotalAmount }}
      </div>
    </div>

    <ShoppingCart />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Combo, DishLabel, Dish } from '@/types'
import { useMenuStore } from '@/stores/menu'
import { useCartStore } from '@/stores/cart'
import { useTableStore } from '@/stores/table'
import { useOrderStore } from '@/stores/order'
import { useWaiterStore } from '@/stores/waiter'
import CategoryTabs from '@/components/customer/CategoryTabs.vue'
import DishCard from '@/components/customer/DishCard.vue'
import ShoppingCart from '@/components/customer/ShoppingCart.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const menuStore = useMenuStore()
const cartStore = useCartStore()
const tableStore = useTableStore()
const orderStore = useOrderStore()
const waiterStore = useWaiterStore()

const tableNumber = route.params.tableNumber as string
const showOrderedPanel = ref(false)
const dishListRef = ref<HTMLElement | null>(null)

const tableStatus = computed(() => {
  const table = tableStore.getTableByNumber(tableNumber)
  return table?.status ?? 'idle'
})

const allLabels: DishLabel[] = ['vegetarian', 'nuts', 'halal', 'gluten_free', 'dairy_free']
const labelNames: Record<DishLabel, string> = {
  vegetarian: '🥬 素食', nuts: '🥜 含坚果', halal: '清真',
  gluten_free: '无麸质', dairy_free: '无乳制品',
}

const existingOrders = computed(() => orderStore.getOrdersByTable(cartStore.tableId))
const orderedItemCount = computed(() =>
  existingOrders.value.reduce((sum, o) => sum + o.items.reduce((s, i) => s + i.quantity, 0), 0)
)
const orderedTotalAmount = computed(() =>
  existingOrders.value.reduce((sum, o) => sum + o.total_amount, 0)
)
const itemStatusLabels: Record<string, string> = { pending: '待做', preparing: '制作中', completed: '已完成 ✓' }

function getDishesByCategory(catId: number): Dish[] {
  let result = menuStore.dishes.filter(d => d.category_id === catId && d.status === 'active')
  if (menuStore.searchQuery) {
    const q = menuStore.searchQuery.toLowerCase()
    result = result.filter(d => d.name.toLowerCase().includes(q) || d.name_en.toLowerCase().includes(q))
  }
  if (menuStore.activeLabels.length > 0) {
    result = result.filter(d => menuStore.activeLabels.every(l => d.labels.includes(l)))
  }
  return result.sort((a, b) => a.sort_order - b.sort_order)
}

function scrollToCategory(catId: number) {
  const el = document.getElementById(`cat-section-${catId}`)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function onDishListScroll() {
  if (!dishListRef.value) return
  const scrollTop = dishListRef.value.scrollTop
  for (const cat of menuStore.categories) {
    const el = document.getElementById(`cat-section-${cat.id}`)
    if (!el) continue
    if (el.offsetTop - 10 <= scrollTop) menuStore.setActiveCategory(cat.id)
  }
}

async function callWaiter() {
  try {
    await waiterStore.callWaiter(cartStore.tableId, cartStore.tableNumber)
    ElMessage({ message: menuStore.lang === 'zh' ? '已呼叫服务员，请稍候' : 'Waiter called, please wait', type: 'success', duration: 2000 })
  } catch {
    ElMessage({ message: menuStore.lang === 'zh' ? '呼叫失败，请重试' : 'Call failed, please retry', type: 'error', duration: 2000 })
  }
}

function addCombo(combo: Combo) {
  cartStore.addCombo(combo)
}

onMounted(async () => {
  await menuStore.loadData()
  await tableStore.loadData()
  await orderStore.loadData()
  const table = tableStore.getTableByNumber(tableNumber)
  if (table) cartStore.setTable(table.id, table.number)
})
</script>

<style scoped>
.customer-page {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--color-bg);
  position: relative;
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-white);
  font-size: var(--font-size-lg);
  font-weight: bold;
  border-bottom: 1px solid var(--color-border);
}
.header-actions { display: flex; gap: var(--spacing-sm); }
.lang-btn, .call-btn {
  background: none; border: 1px solid var(--color-border);
  border-radius: var(--radius-sm); padding: 4px 10px;
  cursor: pointer; font-size: var(--font-size-sm);
}
.call-btn { color: var(--color-primary); }

/* 餐桌状态提示 */
.table-notice {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: #fff3e0;
  color: #e65100;
  font-size: var(--font-size-sm);
  text-align: center;
  border-bottom: 1px solid var(--color-border);
}

/* 搜索栏 */
.search-bar {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-white);
  border-bottom: 1px solid var(--color-border);
}
.search-input {
  width: 100%; padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-border); border-radius: var(--radius-md);
  font-size: var(--font-size-base); outline: none;
}
.search-input:focus { border-color: var(--color-primary); }

/* 标签筛选 */
.label-filters {
  display: flex; gap: var(--spacing-sm); padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-white); border-bottom: 1px solid var(--color-border);
  overflow-x: auto;
}
.label-filters::-webkit-scrollbar { display: none; }
.label-chip {
  flex-shrink: 0; padding: 4px 12px; border-radius: var(--radius-md);
  font-size: var(--font-size-sm); cursor: pointer;
  background: var(--color-bg); color: var(--color-text-light);
  border: 1px solid var(--color-border);
}
.label-chip.active { background: var(--color-primary); color: white; border-color: var(--color-primary); }

/* 套餐 */
.combo-section { margin-bottom: var(--spacing-sm); }
.combo-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-white); border-bottom: 1px solid var(--color-border);
}
.combo-info { flex: 1; }
.combo-name { font-size: var(--font-size-lg); font-weight: 500; }
.combo-desc { font-size: var(--font-size-sm); color: var(--color-text-light); margin-top: 4px; }
.combo-items { display: flex; gap: var(--spacing-xs); margin-top: 4px; }
.combo-item-tag {
  font-size: 11px; padding: 2px 6px; background: var(--color-bg);
  border-radius: var(--radius-sm);
}
.combo-price { font-size: var(--font-size-lg); color: var(--color-primary); font-weight: bold; margin-top: 4px; }
.add-btn {
  width: 32px; height: 32px; border-radius: 50%; border: none;
  background: var(--color-primary); color: white; font-size: 20px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
}

.dish-list { padding-bottom: 70px; overflow-y: auto; height: calc(100vh - 170px); }
.category-section { margin-bottom: var(--spacing-sm); }
.section-title {
  padding: var(--spacing-sm) var(--spacing-lg); background: var(--color-white);
  font-size: var(--font-size-base); font-weight: bold; color: var(--color-text-light);
  border-bottom: 1px solid var(--color-border);
}

/* 已点图标 */
.ordered-badge {
  position: fixed; bottom: 60px; left: max(calc(50% - 240px), 16px);
  width: 48px; height: 48px; background: var(--color-text);
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  cursor: pointer; z-index: 100; box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  transition: transform 0.2s;
}
.ordered-badge:hover { transform: scale(1.1); }
.badge-icon { font-size: 22px; }
.badge-count {
  position: absolute; top: -6px; right: -6px;
  background: var(--color-primary); color: white; font-size: 12px;
  min-width: 18px; height: 18px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center; font-weight: bold;
}

/* 已点面板 */
.ordered-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 150;
}
.ordered-panel {
  position: fixed; bottom: 0; left: 0; right: 0; max-width: 480px;
  margin: 0 auto; background: var(--color-white); z-index: 200;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  max-height: 60vh; display: flex; flex-direction: column;
}
.ordered-panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--spacing-lg); font-weight: bold; font-size: var(--font-size-lg);
  border-bottom: 1px solid var(--color-border);
}
.panel-close { background: none; border: none; font-size: 24px; cursor: pointer; color: var(--color-text-light); }
.ordered-panel-body { overflow-y: auto; padding: var(--spacing-lg); flex: 1; }
.ordered-group { margin-bottom: var(--spacing-md); }
.ordered-group:last-child { margin-bottom: 0; }
.ordered-remark { font-size: var(--font-size-sm); color: var(--color-primary); font-weight: 500; margin-bottom: var(--spacing-xs); }
.ordered-item { display: flex; align-items: center; padding: 6px 0; gap: var(--spacing-sm); }
.ordered-name { flex: 1; }
.ordered-item-remark { color: var(--color-text-light); font-size: var(--font-size-sm); }
.ordered-qty { color: var(--color-text-light); }
.ordered-status { font-size: var(--font-size-sm); }
.ordered-status.completed { color: var(--color-success); }
.ordered-status.preparing { color: var(--color-primary); }
.ordered-panel-footer {
  padding: var(--spacing-md) var(--spacing-lg); background: var(--color-bg);
  text-align: center; font-weight: bold; color: var(--color-text-light);
  border-top: 1px solid var(--color-border);
}
</style>
