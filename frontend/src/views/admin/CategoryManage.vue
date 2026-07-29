<template>
  <div class="admin-layout">
    <SideMenu />
    <div class="admin-content">
      <h2>分类管理</h2>
      <div class="toolbar">
        <el-button type="primary" @click="showAddDialog = true">新增分类</el-button>
      </div>
      <el-table :data="menuStore.categories" stripe>
        <el-table-column prop="icon" label="图标" width="80" />
        <el-table-column prop="name" label="分类名" />
        <el-table-column prop="name_en" label="英文名" width="120" />
        <el-table-column prop="sort_order" label="排序" />
        <el-table-column label="操作">
          <template #default="{ row, $index }">
            <el-button size="small" @click="editCategory(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteCategory(row, $index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="showAddDialog" :title="editingCategory ? '编辑分类' : '新增分类'" width="400px">
        <el-form :model="categoryForm" label-width="80px">
          <el-form-item label="分类名">
            <el-input v-model="categoryForm.name" />
          </el-form-item>
          <el-form-item label="英文名">
            <el-input v-model="categoryForm.name_en" />
          </el-form-item>
          <el-form-item label="图标">
            <el-input v-model="categoryForm.icon" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="categoryForm.sort_order" :min="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="saveCategory">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { Category } from '@/types'
import { useMenuStore } from '@/stores/menu'
import SideMenu from '@/components/admin/SideMenu.vue'

const menuStore = useMenuStore()
const showAddDialog = ref(false)
const editingCategory = ref<Category | null>(null)

onMounted(async () => { await menuStore.loadData() })

const defaultForm = (): Category => ({ id: Date.now(), name: '', name_en: '', sort_order: 0, icon: '' })
const categoryForm = reactive<Category>(defaultForm())

function editCategory(cat: Category) {
  editingCategory.value = cat
  Object.assign(categoryForm, cat)
  showAddDialog.value = true
}

function saveCategory() {
  if (editingCategory.value) {
    menuStore.updateCategory({ ...categoryForm })
  } else {
    menuStore.addCategory({ ...categoryForm, id: Date.now() })
  }
  showAddDialog.value = false
  editingCategory.value = null
  Object.assign(categoryForm, defaultForm())
}

async function deleteCategory(cat: Category, index: number) {
  await menuStore.removeCategory(cat.id)
}
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }
.admin-content { flex: 1; padding: var(--spacing-xl); background: var(--color-bg); overflow-y: auto; }
.admin-content h2 { margin-bottom: var(--spacing-lg); }
.toolbar { margin-bottom: var(--spacing-lg); }
</style>
