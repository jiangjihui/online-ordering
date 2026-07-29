<template>
  <div class="admin-layout">
    <SideMenu />
    <div class="admin-content">
      <h2>订单查看</h2>
      <div class="toolbar">
        <el-select v-model="filterTable" placeholder="按桌号筛选" clearable style="width: 150px">
          <el-option v-for="t in tableStore.tables" :key="t.id" :label="t.number" :value="t.id" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="按状态筛选" clearable style="width: 150px; margin-left: 12px">
          <el-option label="待确认" value="pending" />
          <el-option label="制作中" value="preparing" />
          <el-option label="已完成" value="completed" />
          <el-option label="已清台" value="closed" />
        </el-select>
      </div>
      <el-table :data="filteredOrders" stripe>
        <el-table-column prop="id" label="订单号" width="80" />
        <el-table-column prop="table_id" label="桌号">
          <template #default="{ row }">
            {{ tableStore.getTableById(row.table_id)?.number ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="order_type" label="下单方式">
          <template #default="{ row }">{{ row.order_type === 'scan' ? '扫码' : '服务员' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="tagTypeMap[row.status as keyof typeof tagTypeMap]">
              {{ statusLabelMap[row.status as keyof typeof statusLabelMap] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="total_amount" label="金额">
          <template #default="{ row }">¥{{ row.total_amount }}</template>
        </el-table-column>
        <el-table-column prop="created_at" label="下单时间" width="180" />
        <el-table-column label="等待/制作时长" width="140">
          <template #default="{ row }">
            <span v-if="row.status === 'completed'">
              {{ orderStore.getDuration(row.started_at, row.completed_at) }}
            </span>
            <span v-else-if="row.started_at">
              {{ orderStore.getDuration(row.started_at) }}
            </span>
            <span v-else>
              {{ orderStore.getElapsedTime(row.created_at) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
            <el-button size="small" @click="printKitchenTicket(row)">出单</el-button>
            <el-button size="small" @click="printBill(row)">账单</el-button>
            <el-button v-if="row.status === 'completed'" size="small" type="success" @click="clearTable(row)">清台</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="showDetailDialog" title="订单详情" width="600px">
        <div v-if="detailOrder">
          <p><strong>桌号：</strong>{{ tableStore.getTableById(detailOrder.table_id)?.number }}</p>
          <p><strong>下单方式：</strong>{{ detailOrder.order_type === 'scan' ? '扫码' : '服务员' }}</p>
          <p><strong>备注：</strong>{{ detailOrder.remark || '无' }}</p>
          <p><strong>下单时间：</strong>{{ detailOrder.created_at }}</p>
          <p v-if="detailOrder.started_at"><strong>开始制作：</strong>{{ detailOrder.started_at }}</p>
          <p v-if="detailOrder.completed_at"><strong>完成时间：</strong>{{ detailOrder.completed_at }}</p>
          <p v-if="detailOrder.started_at"><strong>制作时长：</strong>{{ orderStore.getDuration(detailOrder.started_at, detailOrder.completed_at) }}</p>
          <el-table :data="detailOrder.items" stripe size="small">
            <el-table-column prop="dish_name" label="菜品" />
            <el-table-column prop="quantity" label="数量" />
            <el-table-column prop="remark" label="备注" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                {{ itemStatusLabelMap[row.status as keyof typeof itemStatusLabelMap] }}
              </template>
            </el-table-column>
            <el-table-column label="制作时长" width="120">
              <template #default="{ row }">
                {{ row.started_at ? orderStore.getDuration(row.started_at, row.completed_at) : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { Order } from '@/types'
import { useOrderStore } from '@/stores/order'
import { useTableStore } from '@/stores/table'
import { ElMessage, ElMessageBox } from 'element-plus'
import SideMenu from '@/components/admin/SideMenu.vue'

const orderStore = useOrderStore()
const tableStore = useTableStore()

onMounted(async () => {
  await orderStore.loadData()
  await tableStore.loadData()
  orderStore.subscribeWebSocket()
})
onUnmounted(() => {
  orderStore.unsubscribeWebSocket()
})

const filterTable = ref<number | undefined>(undefined)
const filterStatus = ref<string | undefined>(undefined)
const showDetailDialog = ref(false)
const detailOrder = ref<Order | null>(null)

const tagTypeMap: Record<string, string> = { pending: 'warning', preparing: 'danger', completed: 'success', closed: 'info' }
const statusLabelMap: Record<string, string> = { pending: '待确认', preparing: '制作中', completed: '已完成', closed: '已清台' }
const itemStatusLabelMap: Record<string, string> = { pending: '待做', preparing: '制作中', completed: '已完成' }

const filteredOrders = computed(() => {
  return orderStore.orders.filter(o => {
    if (filterTable.value !== undefined && o.table_id !== filterTable.value) return false
    if (filterStatus.value !== undefined && o.status !== filterStatus.value) return false
    return true
  })
})

function viewDetail(order: Order) {
  detailOrder.value = order
  showDetailDialog.value = true
}

async function clearTable(order: Order) {
  const table = tableStore.getTableById(order.table_id)
  if (!table) return
  try {
    await ElMessageBox.confirm(`确认清台？${table.number}号桌将恢复为空闲状态`, '清台确认')
    await tableStore.resetTable(table.id)
    ElMessage({ message: `${table.number}号桌已清台`, type: 'success', duration: 2000 })
  } catch { /* user cancelled */ }
}

function printKitchenTicket(order: Order) {
  const tableName = tableStore.getTableById(order.table_id)?.number ?? '-'
  let html = '<html><head><title>厨房出单</title><style>'
  html += 'body{font-family:sans-serif;padding:10px;width:80mm;} h2{margin:0;} .item{padding:4px 0;}'
  html += '.remark{color:#e74c3c;font-weight:bold;} hr{border:1px dashed #000;}'
  html += '</style></head><body>'
  html += `<h2>桌号：${tableName}</h2>`
  html += `<p>下单：${order.created_at.slice(11, 16)} · ${order.order_type === 'scan' ? '扫码' : '服务员'}</p><hr>`
  for (const item of order.items) {
    html += `<div class="item"><strong>${item.quantity}x ${item.dish_name}</strong>`
    if (item.remark) html += `<span class="remark"> (${item.remark})</span>`
    html += '</div>'
  }
  if (order.remark) html += `<hr><p class="remark">整单备注：${order.remark}</p>`
  html += '</body></html>'
  const w = window.open('', '_blank')
  if (w) { w.document.write(html); w.document.close(); w.print() }
}

function printBill(order: Order) {
  const tableName = tableStore.getTableById(order.table_id)?.number ?? '-'
  let html = '<html><head><title>账单</title><style>'
  html += 'body{font-family:sans-serif;padding:20px;} h1{text-align:center;} h2{margin-top:16px;}'
  html += 'table{width:100%;border-collapse:collapse;} th,td{border:1px solid #ccc;padding:6px;text-align:left;}'
  html += '.total{text-align:right;font-size:20px;font-weight:bold;margin-top:16px;}'
  html += '</style></head><body>'
  html += `<h1>账单</h1><p>桌号：${tableName} · 订单号：#${order.id}</p>`
  html += `<p>下单时间：${order.created_at}</p>`
  html += '<h2>菜品明细</h2><table><tr><th>菜品</th><th>数量</th><th>备注</th><th>单价</th><th>小计</th></tr>'
  for (const item of order.items) {
    html += `<tr><td>${item.dish_name}</td><td>${item.quantity}</td><td>${item.remark || '-'}</td><td>-</td><td>-</td></tr>`
  }
  html += '</table>'
  if (order.remark) html += `<p>备注：${order.remark}</p>`
  html += `<div class="total">合计：¥${order.total_amount}</div>`
  html += '</body></html>'
  const w = window.open('', '_blank')
  if (w) { w.document.write(html); w.document.close(); w.print() }
}
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }
.admin-content { flex: 1; padding: var(--spacing-xl); background: var(--color-bg); overflow-y: auto; }
.admin-content h2 { margin-bottom: var(--spacing-lg); }
.toolbar { margin-bottom: var(--spacing-lg); display: flex; gap: var(--spacing-md); align-items: center; }
</style>
