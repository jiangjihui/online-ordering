<template>
  <div class="admin-layout">
    <SideMenu />
    <div class="admin-content">
      <h2>套餐管理</h2>
      <div class="toolbar">
        <el-button type="primary" @click="openAddDialog">新增套餐</el-button>
      </div>
      <el-table :data="menuStore.combos" stripe>
        <el-table-column prop="name" label="套餐名" />
        <el-table-column prop="name_en" label="英文名" width="120" />
        <el-table-column prop="price" label="价格">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="items" label="包含菜品">
          <template #default="{ row }">
            <el-tag v-for="item in row.items" :key="item.dish_id" size="small" style="margin-right:4px">
              {{ item.dish_name }} x{{ item.quantity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="上架">
          <template #default="{ row }">
            <el-switch v-model="row.status" active-value="active" inactive-value="inactive" @change="menuStore.updateCombo(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="editCombo(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="menuStore.removeCombo(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="showAddDialog" :title="editingCombo ? '编辑套餐' : '新增套餐'" width="600px">
        <el-form :model="comboForm" label-width="80px">
          <el-form-item label="套餐名">
            <el-input v-model="comboForm.name" />
          </el-form-item>
          <el-form-item label="英文名">
            <el-input v-model="comboForm.name_en" />
          </el-form-item>
          <el-form-item label="价格">
            <el-input-number v-model="comboForm.price" :min="0" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="comboForm.description" />
          </el-form-item>
          <el-form-item label="包含菜品">
            <div v-for="(item, idx) in comboForm.items" :key="idx" class="combo-item-row">
              <el-select v-model="item.dish_id" placeholder="选菜品" style="width:200px">
                <el-option v-for="dish in menuStore.dishes" :key="dish.id" :label="dish.name" :value="dish.id" />
              </el-select>
              <el-input-number v-model="item.quantity" :min="1" :max="10" style="width:120px" />
              <el-button size="small" type="danger" @click="comboForm.items.splice(idx, 1)">删除</el-button>
            </div>
            <el-button size="small" @click="addItemRow">添加菜品</el-button>
          </el-form-item>
          <el-form-item label="上架">
            <el-switch v-model="comboForm.status" active-value="active" inactive-value="inactive" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="closeDialog">取消</el-button>
          <el-button type="primary" @click="saveCombo">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import type { Combo } from '@/types'
import { useMenuStore } from '@/stores/menu'
import SideMenu from '@/components/admin/SideMenu.vue'

const menuStore = useMenuStore()
const showAddDialog = ref(false)
const editingCombo = ref<Combo | null>(null)

onMounted(async () => { await menuStore.loadData() })

const defaultForm = (): Combo => ({
  id: Date.now(),
  name: '',
  name_en: '',
  price: 0,
  description: '',
  items: [],
  status: 'active',
})

const comboForm = reactive<Combo>(defaultForm())

// 自动填充 dish_name
watch(() => comboForm.items, () => {
  for (const item of comboForm.items) {
    const dish = menuStore.getDishById(item.dish_id)
    if (dish) item.dish_name = dish.name
  }
}, { deep: true })

function addItemRow() {
  comboForm.items.push({ dish_id: 0, dish_name: '', quantity: 1 })
}

function openAddDialog() {
  Object.assign(comboForm, defaultForm())
  editingCombo.value = null
  showAddDialog.value = true
}

function editCombo(combo: Combo) {
  editingCombo.value = combo
  Object.assign(comboForm, { ...combo, items: combo.items.map(i => ({ ...i })) })
  showAddDialog.value = true
}

function closeDialog() {
  showAddDialog.value = false
  editingCombo.value = null
  Object.assign(comboForm, defaultForm())
}

function saveCombo() {
  if (editingCombo.value) {
    menuStore.updateCombo({ ...comboForm })
  } else {
    menuStore.addCombo({ ...comboForm, id: Date.now() })
  }
  closeDialog()
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}
.admin-content {
  flex: 1;
  padding: var(--spacing-xl);
  background: var(--color-bg);
  overflow-y: auto;
}
.admin-content h2 { margin-bottom: var(--spacing-lg); }
.toolbar { margin-bottom: var(--spacing-lg); }
.combo-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
