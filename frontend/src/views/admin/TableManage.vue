<template>
  <div class="admin-layout">
    <SideMenu />
    <div class="admin-content">
      <h2>餐桌管理</h2>
      <div class="toolbar">
        <el-button type="primary" @click="showAddDialog = true">新增餐桌</el-button>
      </div>
      <el-table :data="tableStore.tables" stripe>
        <el-table-column prop="number" label="桌号" />
        <el-table-column prop="area" label="区域" />
        <el-table-column prop="capacity" label="容纳人数" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-select v-model="row.status" size="small" @change="changeStatus(row)">
              <el-option label="空闲" value="idle" />
              <el-option label="就餐中" value="dining" />
              <el-option label="预留" value="reserved" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="editTable(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeTable(row.id)">删除</el-button>
            <el-button v-if="row.status === 'dining'" size="small" type="success" @click="clearTable(row)">清台</el-button>
            <el-button size="small" type="primary" @click="showQRCode(row)">二维码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="showAddDialog" :title="editingTable ? '编辑餐桌' : '新增餐桌'" width="400px">
        <el-form :model="tableForm" label-width="80px">
          <el-form-item label="桌号">
            <el-input v-model="tableForm.number" />
          </el-form-item>
          <el-form-item label="区域">
            <el-select v-model="tableForm.area">
              <el-option label="大厅" value="大厅" />
              <el-option label="包间" value="包间" />
              <el-option label="露天" value="露天" />
            </el-select>
          </el-form-item>
          <el-form-item label="容纳人数">
            <el-input-number v-model="tableForm.capacity" :min="1" />
          </el-form-item>
          <el-form-item label="初始状态">
            <el-select v-model="tableForm.status">
              <el-option label="空闲" value="idle" />
              <el-option label="预留" value="reserved" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="saveTable">保存</el-button>
        </template>
      </el-dialog>

      <!-- 二维码弹窗 -->
      <el-dialog v-model="showQRDialog" title="餐桌二维码" width="320px">
        <div class="qr-container">
          <div class="qr-label">桌号：{{ qrTable?.number }}</div>
          <div class="qr-area">{{ qrTable?.area }}</div>
          <div ref="qrCanvasRef" class="qr-canvas qr-print-area">
            <QrcodeVue v-if="qrUrl" :value="qrUrl" :size="200" level="M" />
          </div>
          <div class="qr-url">{{ qrUrl }}</div>
        </div>
        <template #footer>
          <el-button @click="showQRDialog = false">关闭</el-button>
          <el-button type="primary" @click="printQRCode">打印</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { Table } from '@/types'
import { useTableStore } from '@/stores/table'
import { ElMessage, ElMessageBox } from 'element-plus'
import QrcodeVue from 'qrcode.vue'
import SideMenu from '@/components/admin/SideMenu.vue'

const tableStore = useTableStore()
const showAddDialog = ref(false)
const editingTable = ref<Table | null>(null)

onMounted(async () => { await tableStore.loadData() })

const showQRDialog = ref(false)
const qrTable = ref<Table | null>(null)
const qrUrl = computed(() => {
  if (!qrTable.value) return ''
  const host = window.location.host
  return `${window.location.protocol}//${host}/order/${qrTable.value.number}`
})

const defaultForm = (): Table => ({ id: Date.now(), number: '', area: '大厅', capacity: 4, status: 'idle' })
const tableForm = reactive<Table>(defaultForm())

function editTable(table: Table) {
  editingTable.value = table
  Object.assign(tableForm, table)
  showAddDialog.value = true
}

async function saveTable() {
  if (editingTable.value) {
    await tableStore.updateTable({ ...tableForm })
  } else {
    await tableStore.addTable({ ...tableForm, id: Date.now() })
  }
  showAddDialog.value = false
  editingTable.value = null
  Object.assign(tableForm, defaultForm())
}

async function removeTable(id: number) {
  await tableStore.removeTable(id)
}

async function clearTable(table: Table) {
  try {
    await ElMessageBox.confirm(`确认清台？${table.number}号桌将恢复为空闲状态`, '清台确认')
    await tableStore.resetTable(table.id)
    ElMessage({ message: `${table.number}号桌已清台`, type: 'success', duration: 2000 })
  } catch { /* user cancelled */ }
}

async function changeStatus(table: Table) {
  await tableStore.updateTable({ ...table })
}

function showQRCode(table: Table) {
  qrTable.value = table
  showQRDialog.value = true
}

function printQRCode() {
  const container = document.querySelector('.qr-print-area')
  if (!container) return
  const printWindow = window.open('', '_blank')
  if (!printWindow) return
  printWindow.document.write(`
    <html>
    <head><title>餐桌二维码 - ${qrTable.value?.number}</title>
    <style>
      body { display: flex; justify-content: center; align-items: center; min-height: 100vh; font-family: sans-serif; }
      .print-card { border: 2px solid #333; border-radius: 8px; padding: 24px; text-align: center; width: 200px; }
      .print-label { font-size: 24px; font-weight: bold; margin-bottom: 8px; }
      .print-area { font-size: 14px; color: #666; margin-bottom: 16px; }
      .print-qr { margin-bottom: 16px; }
      .print-url { font-size: 10px; color: #999; }
    </style></head>
    <body>
      <div class="print-card">
        <div class="print-label">${qrTable.value?.number}</div>
        <div class="print-area">${qrTable.value?.area}</div>
        <div class="print-qr">${document.querySelector('.qr-canvas')?.innerHTML || ''}</div>
        <div class="print-url">扫码点餐</div>
      </div>
    </body></html>
  `)
  printWindow.document.close()
  printWindow.print()
}
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }
.admin-content { flex: 1; padding: var(--spacing-xl); background: var(--color-bg); overflow-y: auto; }
.admin-content h2 { margin-bottom: var(--spacing-lg); }
.toolbar { margin-bottom: var(--spacing-lg); }
.qr-container { text-align: center; }
.qr-label { font-size: 24px; font-weight: bold; }
.qr-area { font-size: 14px; color: var(--color-text-light); margin-bottom: var(--spacing-md); }
.qr-canvas { display: flex; justify-content: center; margin: var(--spacing-md) 0; }
.qr-url { font-size: 12px; color: var(--color-text-light); }
</style>
