<template>
  <div class="admin-layout">
    <SideMenu />
    <div class="admin-content">
      <h2>菜品管理</h2>
      <div class="toolbar">
        <el-button type="primary" @click="openAddDialog">新增菜品</el-button>
        <el-button @click="printMenu">打印菜单</el-button>
      </div>
      <el-table :data="menuStore.dishes" stripe>
        <el-table-column prop="image" label="图片" width="80">
          <template #default="{ row }">
            <img v-if="row.image" :src="imageUrl(row.image)" class="dish-thumb" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="菜名" />
        <el-table-column prop="name_en" label="英文名" width="120" />
        <el-table-column prop="category_id" label="分类">
          <template #default="{ row }">
            {{ menuStore.categories.find(c => c.id === row.category_id)?.name ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="is_spicy" label="辣度">
          <template #default="{ row }">
            {{ ['不辣','微辣','中辣','特辣'][row.is_spicy] }}
          </template>
        </el-table-column>
        <el-table-column prop="labels" label="标签">
          <template #default="{ row }">
            <el-tag v-for="label in (row.labels as DishLabel[])" :key="label" size="small" style="margin-right:4px">
              {{ labelNames[label] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sold_out" label="售罄">
          <template #default="{ row }">
            <el-switch v-model="row.sold_out" @change="menuStore.updateDish(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="上架">
          <template #default="{ row }">
            <el-switch v-model="row.status" active-value="active" inactive-value="inactive" @change="menuStore.updateDish(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="editDish(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="menuStore.removeDish(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="showAddDialog" :title="editingDish ? '编辑菜品' : '新增菜品'" width="600px">
        <el-form :model="dishForm" label-width="80px">
          <el-form-item label="菜名">
            <el-input v-model="dishForm.name" />
          </el-form-item>
          <el-form-item label="英文名">
            <el-input v-model="dishForm.name_en" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="dishForm.category_id">
              <el-option v-for="cat in menuStore.categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="价格">
            <el-input-number v-model="dishForm.price" :min="0" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="dishForm.description" />
          </el-form-item>
          <el-form-item label="英文描述">
            <el-input v-model="dishForm.description_en" />
          </el-form-item>
          <el-form-item label="图片">
            <div v-if="dishForm.image" class="image-preview">
              <img :src="imageUrl(dishForm.image)" class="image-preview-img" />
              <el-button size="small" type="danger" @click="dishForm.image = ''">删除图片</el-button>
            </div>
            <el-upload
              v-else
              :http-request="handleImageUpload"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small">上传图片</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="辣度">
            <el-select v-model="dishForm.is_spicy">
              <el-option label="不辣" :value="0" />
              <el-option label="微辣" :value="1" />
              <el-option label="中辣" :value="2" />
              <el-option label="特辣" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-checkbox-group v-model="dishForm.labels">
              <el-checkbox v-for="label in allLabels" :key="label" :label="label">{{ labelNames[label] }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="dishForm.sort_order" :min="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="closeDialog">取消</el-button>
          <el-button type="primary" @click="saveDish">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { Dish, DishLabel } from '@/types'
import { useMenuStore } from '@/stores/menu'
import { uploadDishImage } from '@/api'
import { ElMessage } from 'element-plus'
import SideMenu from '@/components/admin/SideMenu.vue'

const menuStore = useMenuStore()
const showAddDialog = ref(false)
const editingDish = ref<Dish | null>(null)

onMounted(async () => { await menuStore.loadData() })

const allLabels: DishLabel[] = ['vegetarian', 'nuts', 'halal', 'gluten_free', 'dairy_free']
const labelNames: Record<DishLabel, string> = {
  vegetarian: '素食', nuts: '含坚果', halal: '清真',
  gluten_free: '无麸质', dairy_free: '无乳制品',
}

const defaultForm = (): Dish => ({
  id: Date.now(),
  name: '',
  name_en: '',
  category_id: 1,
  price: 0,
  description: '',
  description_en: '',
  image: '',
  status: 'active',
  sold_out: false,
  is_spicy: 0,
  labels: [],
  sort_order: 0,
})

const dishForm = reactive<Dish>(defaultForm())

function openAddDialog() {
  Object.assign(dishForm, defaultForm())
  editingDish.value = null
  showAddDialog.value = true
}

function editDish(dish: Dish) {
  editingDish.value = dish
  Object.assign(dishForm, { ...dish, labels: [...dish.labels] })
  showAddDialog.value = true
}

function closeDialog() {
  showAddDialog.value = false
  editingDish.value = null
  Object.assign(dishForm, defaultForm())
}

function saveDish() {
  if (editingDish.value) {
    menuStore.updateDish({ ...dishForm })
  } else {
    menuStore.addDish({ ...dishForm, id: Date.now() })
  }
  closeDialog()
}

function imageUrl(filename: string) {
  return `${import.meta.env.VITE_IMAGE_BASE_URL}/${filename}`
}

async function handleImageUpload(options: any) {
  if (!editingDish.value) {
    ElMessage({ message: '请先保存菜品再上传图片', type: 'warning' })
    return
  }
  try {
    const filename = await uploadDishImage(editingDish.value.id, options.file)
    dishForm.image = filename
    editingDish.value.image = filename
    ElMessage({ message: '图片上传成功', type: 'success', duration: 2000 })
  } catch {
    ElMessage({ message: '图片上传失败', type: 'error', duration: 2000 })
  }
}

function printMenu() {
  const categories = menuStore.categories
  const dishes = menuStore.dishes.filter(d => d.status === 'active')
  let html = '<html><head><title>菜单</title><style>'
  html += 'body{font-family:sans-serif;padding:20px;} h1{text-align:center;} h2{margin-top:20px;}'
  html += 'table{width:100%;border-collapse:collapse;margin-bottom:20px;} th,td{border:1px solid #ccc;padding:6px;text-align:left;}'
  html += '.spicy{color:#e74c3c;font-weight:bold;} .sold-out{color:#999;}'
  html += '</style></head><body><h1>餐厅菜单</h1>'
  for (const cat of categories) {
    const catDishes = dishes.filter(d => d.category_id === cat.id)
    if (catDishes.length === 0) continue
    html += `<h2>${cat.icon} ${cat.name}</h2><table><tr><th>菜名</th><th>英文名</th><th>价格</th><th>辣度</th><th>标签</th></tr>`
    for (const d of catDishes) {
      const spicy = ['','微辣','中辣','特辣'][d.is_spicy]
      const labels = d.labels.map(l => labelNames[l]).join('、')
      html += `<tr${d.sold_out ? ' class="sold-out"' : ''}><td>${d.sold_out ? '(售罄) ' : ''}${d.name}</td><td>${d.name_en}</td><td>¥${d.price}</td><td class="spicy">${spicy}</td><td>${labels}</td></tr>`
    }
    html += '</table>'
  }
  html += '</body></html>'
  const w = window.open('', '_blank')
  if (w) { w.document.write(html); w.document.close(); w.print() }
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
.dish-thumb { width: 40px; height: 40px; object-fit: cover; border-radius: var(--radius-sm); }
.image-preview { display: flex; align-items: center; gap: var(--spacing-sm); }
.image-preview-img { width: 80px; height: 80px; object-fit: cover; border-radius: var(--radius-sm); border: 1px solid var(--color-border); }
</style>
