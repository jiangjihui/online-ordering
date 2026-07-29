import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { CartItem, CartComboItem, Dish, Combo } from '@/types'

const STORAGE_KEY = 'ordering-cart'

function saveToStorage(state: { items: CartItem[]; comboItems: CartComboItem[]; tableId: number; tableNumber: string }) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

function loadFromStorage() {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try { return JSON.parse(raw) } catch { return null }
}

export const useCartStore = defineStore('cart', () => {
  const saved = loadFromStorage()
  const items = ref<CartItem[]>(saved?.items ?? [])
  const comboItems = ref<CartComboItem[]>(saved?.comboItems ?? [])
  const tableId = ref<number>(saved?.tableId ?? 0)
  const tableNumber = ref<string>(saved?.tableNumber ?? '')

  watch([items, comboItems, tableId, tableNumber], () => {
    saveToStorage({ items: items.value, comboItems: comboItems.value, tableId: tableId.value, tableNumber: tableNumber.value })
  }, { deep: true })

  const totalCount = computed(() =>
    items.value.reduce((sum, i) => sum + i.quantity, 0) +
    comboItems.value.reduce((sum, i) => sum + i.quantity, 0)
  )
  const totalAmount = computed(() =>
    items.value.reduce((sum, i) => sum + i.dish.price * i.quantity, 0) +
    comboItems.value.reduce((sum, i) => sum + i.combo.price * i.quantity, 0)
  )

  function setTable(id: number, number: string) {
    tableId.value = id
    tableNumber.value = number
  }

  function addItem(dish: Dish, remark: string = '') {
    const existing = items.value.find(i => i.dish.id === dish.id && i.remark === remark)
    if (existing) {
      existing.quantity++
    } else {
      items.value.push({ dish, quantity: 1, remark })
    }
  }

  function removeItem(dishId: number, remark: string = '') {
    const idx = items.value.findIndex(i => i.dish.id === dishId && i.remark === remark)
    if (idx !== -1) {
      if (items.value[idx].quantity > 1) {
        items.value[idx].quantity--
      } else {
        items.value.splice(idx, 1)
      }
    }
  }

  function deleteItem(dishId: number, remark: string = '') {
    items.value = items.value.filter(i => !(i.dish.id === dishId && i.remark === remark))
  }

  function addCombo(combo: Combo, remark: string = '') {
    const existing = comboItems.value.find(i => i.combo.id === combo.id && i.remark === remark)
    if (existing) {
      existing.quantity++
    } else {
      comboItems.value.push({ combo, quantity: 1, remark })
    }
  }

  function removeCombo(comboId: number, remark: string = '') {
    const idx = comboItems.value.findIndex(i => i.combo.id === comboId && i.remark === remark)
    if (idx !== -1) {
      if (comboItems.value[idx].quantity > 1) {
        comboItems.value[idx].quantity--
      } else {
        comboItems.value.splice(idx, 1)
      }
    }
  }

  function deleteCombo(comboId: number, remark: string = '') {
    comboItems.value = comboItems.value.filter(i => !(i.combo.id === comboId && i.remark === remark))
  }

  function clearCart() {
    items.value = []
    comboItems.value = []
  }

  return {
    items, comboItems, tableId, tableNumber, totalCount, totalAmount,
    setTable, addItem, removeItem, deleteItem,
    addCombo, removeCombo, deleteCombo, clearCart,
  }
})
