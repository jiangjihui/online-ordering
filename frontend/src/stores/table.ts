import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Table } from '@/types'
import * as api from '@/api'

/** Map a Table object from API camelCase to frontend snake_case */
function mapTableFromApi(raw: any): Table {
  return {
    id: raw.id,
    number: raw.number,
    area: raw.area,
    capacity: raw.capacity,
    status: raw.status,
  }
}

/** Map a frontend Table (snake_case) to API camelCase */
function mapTableToApi(table: Partial<Table>): any {
  return {
    number: table.number,
    area: table.area,
    capacity: table.capacity,
    status: table.status,
  }
}

export const useTableStore = defineStore('table', () => {
  const tables = ref<Table[]>([])
  const loaded = ref(false)

  async function loadData() {
    const data = await api.getTables()
    tables.value = (data as any[]).map(mapTableFromApi)
    loaded.value = true
  }

  function getTableById(id: number): Table | undefined {
    return tables.value.find(t => t.id === id)
  }

  function getTableByNumber(number: string): Table | undefined {
    return tables.value.find(t => t.number === number)
  }

  async function updateTable(table: Table) {
    const requestData = mapTableToApi(table)
    await api.updateTable(table.id, requestData)
    // Update local state after successful API call
    const idx = tables.value.findIndex(t => t.id === table.id)
    if (idx !== -1) tables.value[idx] = table
  }

  async function addTable(table: Omit<Table, 'id'>) {
    const requestData = mapTableToApi(table)
    const data = await api.createTable(requestData)
    const newTable = mapTableFromApi(data)
    tables.value.push(newTable)
    return newTable
  }

  async function removeTable(id: number) {
    await api.deleteTable(id)
    // Update local state after successful API call
    tables.value = tables.value.filter(t => t.id !== id)
  }

  async function resetTable(id: number) {
    await api.resetTable(id)
    const table = tables.value.find(t => t.id === id)
    if (table) table.status = 'idle'
  }

  return { tables, loaded, loadData, getTableById, getTableByNumber, updateTable, addTable, removeTable, resetTable }
})
