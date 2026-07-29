import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Category, Dish, Combo, DishLabel, Lang } from '@/types'
import * as api from '@/api'

function mapDish(d: any): Dish {
  return {
    ...d,
    category_id: d.categoryId,
    name_en: d.nameEn,
    description_en: d.descriptionEn,
    sold_out: d.soldOut,
    is_spicy: d.isSpicy,
    sort_order: d.sortOrder,
  }
}

function mapCategory(c: any): Category {
  return {
    ...c,
    name_en: c.nameEn,
    sort_order: c.sortOrder,
  }
}

function mapCombo(c: any): Combo {
  return {
    ...c,
    name_en: c.nameEn,
    items: c.items?.map((i: any) => ({
      dish_id: i.dishId,
      dish_name: i.dishName,
      quantity: i.quantity,
    })) ?? [],
  }
}

export const useMenuStore = defineStore('menu', () => {
  const categories = ref<Category[]>([])
  const dishes = ref<Dish[]>([])
  const combos = ref<Combo[]>([])
  const activeCategoryId = ref<number>(1)
  const searchQuery = ref<string>('')
  const activeLabels = ref<DishLabel[]>([])
  const lang = ref<Lang>('zh')
  const loaded = ref(false)

  const activeCombos = computed(() =>
    combos.value.filter(c => c.status === 'active')
  )

  const displayedDishes = computed(() => {
    let result = dishes.value.filter(d => d.category_id === activeCategoryId.value)
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase()
      result = result.filter(d =>
        d.name.toLowerCase().includes(q) ||
        d.name_en.toLowerCase().includes(q)
      )
    }
    if (activeLabels.value.length > 0) {
      result = result.filter(d =>
        activeLabels.value.every(l => d.labels.includes(l))
      )
    }
    return result
      .filter(d => d.status === 'active')
      .sort((a, b) => a.sort_order - b.sort_order)
  })

  async function loadData() {
    if (loaded.value) return
    const [cats, dishList, comboList] = await Promise.all([
      api.getCategories(),
      api.getDishes(),
      api.getCombos(),
    ])
    categories.value = (cats as any[]).map(mapCategory)
    dishes.value = (dishList as any[]).map(mapDish)
    combos.value = (comboList as any[]).map(mapCombo)
    if (categories.value.length > 0) {
      activeCategoryId.value = categories.value[0].id
    }
    loaded.value = true
  }

  function getDishesByCategory(catId: number): Dish[] {
    return dishes.value
      .filter(d => d.category_id === catId && d.status === 'active')
      .sort((a, b) => a.sort_order - b.sort_order)
  }

  function setActiveCategory(id: number) {
    activeCategoryId.value = id
  }

  function getDishById(id: number): Dish | undefined {
    return dishes.value.find(d => d.id === id)
  }

  async function toggleSoldOut(id: number) {
    const dish = dishes.value.find(d => d.id === id)
    if (!dish) return
    const updated = { ...dish, sold_out: !dish.sold_out }
    await api.updateDish(id, updated)
    dish.sold_out = !dish.sold_out
  }

  function toggleLabel(label: DishLabel) {
    const idx = activeLabels.value.indexOf(label)
    if (idx !== -1) {
      activeLabels.value.splice(idx, 1)
    } else {
      activeLabels.value.push(label)
    }
  }

  function setLang(l: Lang) {
    lang.value = l
  }

  function dishName(dish: Dish): string {
    return lang.value === 'en' && dish.name_en ? dish.name_en : dish.name
  }

  function dishDesc(dish: Dish): string {
    return lang.value === 'en' && dish.description_en ? dish.description_en : dish.description
  }

  function categoryName(cat: Category): string {
    return lang.value === 'en' && cat.name_en ? cat.name_en : cat.name
  }

  function comboName(combo: Combo): string {
    return lang.value === 'en' && combo.name_en ? combo.name_en : combo.name
  }

  async function updateDish(dish: Dish) {
    await api.updateDish(dish.id, dish)
    const idx = dishes.value.findIndex(d => d.id === dish.id)
    if (idx !== -1) dishes.value[idx] = dish
  }

  async function addDish(dish: Dish) {
    const created = await api.createDish(dish) as any
    dishes.value.push(mapDish(created))
  }

  async function removeDish(id: number) {
    await api.deleteDish(id)
    dishes.value = dishes.value.filter(d => d.id !== id)
  }

  async function updateCombo(combo: Combo) {
    await api.updateCombo(combo.id, combo)
    const idx = combos.value.findIndex(c => c.id === combo.id)
    if (idx !== -1) combos.value[idx] = combo
  }

  async function addCombo(combo: Combo) {
    const created = await api.createCombo(combo) as any
    combos.value.push(mapCombo(created))
  }

  async function removeCombo(id: number) {
    await api.deleteCombo(id)
    combos.value = combos.value.filter(c => c.id !== id)
  }

  async function addCategory(cat: Category) {
    const created = await api.createCategory(cat) as any
    categories.value.push(mapCategory(created))
  }

  async function updateCategory(cat: Category) {
    await api.updateCategory(cat.id, cat)
    const idx = categories.value.findIndex(c => c.id === cat.id)
    if (idx !== -1) categories.value[idx] = cat
  }

  async function removeCategory(id: number) {
    await api.deleteCategory(id)
    categories.value = categories.value.filter(c => c.id !== id)
  }

  return {
    categories, dishes, combos, activeCategoryId, searchQuery, activeLabels, lang, loaded,
    activeCombos, displayedDishes,
    loadData, getDishesByCategory, setActiveCategory, getDishById,
    toggleSoldOut, toggleLabel, setLang,
    dishName, dishDesc, categoryName, comboName,
    updateDish, addDish, removeDish,
    updateCombo, addCombo, removeCombo,
    addCategory, updateCategory, removeCategory,
  }
})
