<template>
  <div class="admin-layout">
    <SideMenu />
    <div class="admin-content">
      <h2>仪表盘</h2>

      <div class="toolbar">
        <el-button type="primary" @click="exportDailyReport">导出日报</el-button>
      </div>

      <!-- 呼叫服务员提醒 -->
      <div v-if="pendingCalls.length > 0" class="call-alerts">
        <div class="call-alert-title">🔔 服务员呼叫</div>
        <div v-for="call in pendingCalls" :key="call.id" class="call-alert-item" @click="waiterStore.handleCall(call.id)">
          <span class="call-table">{{ call.table_number }}号桌</span>
          <span class="call-time">{{ call.created_at.slice(11, 16) }}</span>
          <button class="call-handle-btn">处理</button>
        </div>
      </div>

      <div class="stats-grid">
        <div class="stat-card dining">
          <div class="stat-icon">🪑</div>
          <div class="stat-value">{{ diningTableCount }}</div>
          <div class="stat-label">就餐中</div>
          <div class="stat-sub">{{ idleTableCount }} 空闲 · {{ reservedTableCount }} 预留</div>
        </div>

        <div class="stat-card pending">
          <div class="stat-icon">⏳</div>
          <div class="stat-value">{{ pendingOrderCount }}</div>
          <div class="stat-label">待处理订单</div>
          <div class="stat-sub">制作中 {{ preparingOrderCount }} 项</div>
        </div>

        <div class="stat-card revenue">
          <div class="stat-icon">💰</div>
          <div class="stat-value">¥{{ todayRevenue }}</div>
          <div class="stat-label">今日营收</div>
          <div class="stat-sub">{{ completedOrderCount }} 笔订单</div>
        </div>

        <div class="stat-card dish-count">
          <div class="stat-icon">🍽️</div>
          <div class="stat-value">{{ activeDishCount }}</div>
          <div class="stat-label">上架菜品</div>
          <div class="stat-sub">{{ inactiveDishCount }} 个已下架 · {{ soldOutCount }} 个售罄</div>
        </div>
      </div>

      <div class="dashboard-sections">
        <div class="section">
          <h3>餐桌状态一览</h3>
          <div class="table-grid">
            <div
              v-for="table in tableStore.tables"
              :key="table.id"
              class="table-status-card"
              :class="table.status"
            >
              <div class="table-number">{{ table.number }}</div>
              <div class="table-area">{{ table.area }}</div>
              <div class="table-status-label">{{ tableStatusLabels[table.status] }}</div>
              <div class="table-capacity">{{ table.capacity }}人</div>
            </div>
          </div>
        </div>

        <div class="section">
          <h3>热门菜品 TOP5</h3>
          <div class="dish-ranking">
            <div v-for="(item, idx) in topDishes" :key="item.name" class="rank-item">
              <span class="rank-num">{{ idx + 1 }}</span>
              <span class="rank-name">{{ item.name }}</span>
              <span class="rank-count">{{ item.count }}份</span>
              <span class="rank-revenue">¥{{ item.revenue }}</span>
            </div>
          </div>
        </div>

        <div class="section">
          <h3>制作时长统计</h3>
          <div class="timing-stats">
            <div v-for="order in recentCompletedOrders" :key="order.id" class="timing-item">
              <span class="timing-table">{{ tableStore.getTableById(order.table_id)?.number ?? '-' }}</span>
              <span class="timing-duration">{{ orderStore.getDuration(order.started_at, order.completed_at) }}</span>
              <span class="timing-time">{{ order.completed_at?.slice(11, 16) ?? '-' }}</span>
            </div>
            <EmptyState v-if="recentCompletedOrders.length === 0" message="暂无已完成订单" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useTableStore } from '@/stores/table'
import { useOrderStore } from '@/stores/order'
import { useMenuStore } from '@/stores/menu'
import { useWaiterStore } from '@/stores/waiter'
import * as api from '@/api'
import SideMenu from '@/components/admin/SideMenu.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const tableStore = useTableStore()
const orderStore = useOrderStore()
const menuStore = useMenuStore()
const waiterStore = useWaiterStore()

const tableStatusLabels: Record<string, string> = { idle: '空闲', dining: '就餐中', reserved: '预留' }

const pendingCalls = computed(() => waiterStore.pendingCalls)

onMounted(async () => {
  await tableStore.loadData()
  await orderStore.loadData()
  await menuStore.loadData()
  await waiterStore.loadData()
  loadStats()
  orderStore.subscribeWebSocket()
  waiterStore.subscribeWebSocket()
})
onUnmounted(() => {
  orderStore.unsubscribeWebSocket()
  waiterStore.unsubscribeWebSocket()
})

const diningTableCount = computed(() => stats.value?.diningTableCount ?? 0)
const idleTableCount = computed(() => tableStore.tables.filter(t => t.status === 'idle').length)
const reservedTableCount = computed(() => tableStore.tables.filter(t => t.status === 'reserved').length)

const pendingOrderCount = computed(() => stats.value?.pendingOrderCount ?? 0)
const preparingOrderCount = computed(() => stats.value?.preparingOrderCount ?? 0)
const completedOrderCount = computed(() => orderStore.orders.filter(o => o.status === 'completed').length)

const todayRevenue = computed(() => stats.value?.todayRevenue ?? 0)

const activeDishCount = computed(() => stats.value?.activeDishCount ?? 0)
const inactiveDishCount = computed(() => menuStore.dishes.filter(d => d.status === 'inactive').length)
const soldOutCount = computed(() => menuStore.dishes.filter(d => d.status === 'active' && d.sold_out).length)

const topDishes = computed(() => {
  const raw = stats.value?.topDishes ?? []
  return raw.map((d: any) => ({
    name: d.dishName ?? d.dish_name,
    count: d.totalQuantity ?? 0,
    revenue: 0,
  }))
})

const recentCompletedOrders = computed(() =>
  orderStore.getCompletedOrders().slice(-10)
)

const stats = ref<any>(null)
async function loadStats() {
  stats.value = await api.getOrderStats()
}

function exportDailyReport() {
  const today = new Date().toISOString().slice(0, 10)
  let html = '<html><head><title>日报</title><style>'
  html += 'body{font-family:sans-serif;padding:20px;} h1{text-align:center;} h2{margin-top:20px;}'
  html += 'table{width:100%;border-collapse:collapse;margin-bottom:20px;} th,td{border:1px solid #ccc;padding:6px;text-align:left;}'
  html += '.summary{text-align:center;font-size:18px;margin:20px 0;} .stat{display:inline-block;margin:10px 20px;}'
  html += '</style></head><body>'
  html += `<h1>餐厅日报 - ${today}</h1>`
  html += `<div class="summary"><span class="stat">营收 ¥${todayRevenue.value}</span><span class="stat">订单 ${orderStore.orders.length} 笔</span><span class="stat">就餐 ${diningTableCount.value} 桌</span></div>`
  // 菜品统计
  html += '<h2>菜品销量统计</h2><table><tr><th>菜名</th><th>销量</th><th>营收</th></tr>'
  for (const d of topDishes.value) {
    html += `<tr><td>${d.name}</td><td>${d.count}份</td><td>¥${d.revenue}</td></tr>`
  }
  html += '</table>'
  // 订单列表
  html += '<h2>订单明细</h2><table><tr><th>订单号</th><th>桌号</th><th>方式</th><th>金额</th><th>状态</th><th>下单时间</th><th>制作时长</th></tr>'
  for (const o of orderStore.orders) {
    const tn = tableStore.getTableById(o.table_id)?.number ?? '-'
    const dur = orderStore.getDuration(o.started_at, o.completed_at)
    html += `<tr><td>#${o.id}</td><td>${tn}</td><td>${o.order_type === 'scan' ? '扫码' : '服务员'}</td><td>¥${o.total_amount}</td><td>${statusLabelMap[o.status]}</td><td>${o.created_at}</td><td>${dur}</td></tr>`
  }
  html += '</table>'
  // 餐桌状态
  html += '<h2>餐桌状态</h2><table><tr><th>桌号</th><th>区域</th><th>容量</th><th>状态</th></tr>'
  for (const t of tableStore.tables) {
    html += `<tr><td>${t.number}</td><td>${t.area}</td><td>${t.capacity}人</td><td>${tableStatusLabels[t.status]}</td></tr>`
  }
  html += '</table></body></html>'
  const w = window.open('', '_blank')
  if (w) { w.document.write(html); w.document.close(); w.print() }
}

const statusLabelMap: Record<string, string> = { pending: '待确认', preparing: '制作中', completed: '已完成', closed: '已清台' }
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }
.admin-content { flex: 1; padding: var(--spacing-xl); background: var(--color-bg); overflow-y: auto; }
.admin-content h2 { margin-bottom: var(--spacing-lg); }
.toolbar { margin-bottom: var(--spacing-lg); }

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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}
.stat-card {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  text-align: center;
  border-top: 4px solid;
}
.stat-card.dining { border-top-color: var(--color-primary); }
.stat-card.pending { border-top-color: var(--color-warning); }
.stat-card.revenue { border-top-color: var(--color-success); }
.stat-card.dish-count { border-top-color: #3b82f6; }
.stat-icon { font-size: 32px; margin-bottom: var(--spacing-sm); }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-label { font-size: var(--font-size-base); color: var(--color-text-light); margin-top: 4px; }
.stat-sub { font-size: var(--font-size-sm); color: var(--color-text-light); margin-top: var(--spacing-xs); }

.dashboard-sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-lg);
}
.section {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
}
.section h3 { margin-bottom: var(--spacing-md); }

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: var(--spacing-sm);
}
.table-status-card {
  border-radius: var(--radius-md);
  padding: var(--spacing-sm);
  text-align: center;
  background: var(--color-bg);
  font-size: var(--font-size-sm);
}
.table-status-card.idle { background: #e8f5e9; }
.table-status-card.dining { background: #ffebee; }
.table-status-card.reserved { background: #fff3e0; }
.table-number { font-size: var(--font-size-lg); font-weight: bold; }
.table-area { color: var(--color-text-light); }
.table-status-label { font-weight: 500; margin-top: 4px; }
.table-capacity { color: var(--color-text-light); }

.rank-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-sm) 0;
  gap: var(--spacing-sm);
}
.rank-num {
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--color-primary); color: white;
  display: flex; align-items: center; justify-content: center;
  font-size: var(--font-size-sm); font-weight: bold;
}
.rank-name { flex: 1; }
.rank-count { color: var(--color-text-light); }
.rank-revenue { font-weight: 500; color: var(--color-success); }

.timing-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-sm) 0;
  gap: var(--spacing-md);
  font-size: var(--font-size-sm);
}
.timing-table { font-weight: bold; }
.timing-duration { color: var(--color-primary); }
.timing-time { color: var(--color-text-light); }
</style>
