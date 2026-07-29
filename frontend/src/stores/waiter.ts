import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { WaiterCall } from '@/types'
import * as api from '@/api'
import { connectWebSocket, onAdminEvent, offAdminEvent } from '@/api/websocket'

/** Map a WaiterCall from API camelCase to frontend snake_case */
function mapCallFromApi(raw: any): WaiterCall {
  return {
    id: raw.id,
    table_id: raw.tableId,
    table_number: raw.tableNumber,
    created_at: raw.createdAt,
    status: raw.status,
  }
}

export const useWaiterStore = defineStore('waiter', () => {
  const calls = ref<WaiterCall[]>([])
  const loaded = ref(false)

  async function loadData() {
    const data = await api.getWaiterCalls()
    calls.value = (data as any[]).map(mapCallFromApi)
    loaded.value = true
  }

  async function callWaiter(tableId: number, tableNumber: string) {
    const data = await api.createWaiterCall(tableId, tableNumber)
    const newCall = mapCallFromApi(data)
    calls.value.push(newCall)
    return newCall
  }

  async function handleCall(id: number) {
    await api.handleWaiterCall(id)
    // Update local state after successful API call
    const call = calls.value.find(c => c.id === id)
    if (call) call.status = 'handled'
  }

  const pendingCalls = computed(() => calls.value.filter(c => c.status === 'pending'))

  // WebSocket handlers
  const onCallCreated = (data: any) => {
    const existing = calls.value.find(c => c.id === data.id)
    if (!existing) {
      calls.value.push(mapCallFromApi(data))
    }
  }

  const onCallHandled = (data: any) => {
    const call = calls.value.find(c => c.id === data.callId)
    if (call) call.status = 'handled'
  }

  function subscribeWebSocket() {
    connectWebSocket()
    onAdminEvent('waiter-call.created', onCallCreated)
    onAdminEvent('waiter-call.handled', onCallHandled)
  }

  function unsubscribeWebSocket() {
    offAdminEvent('waiter-call.created', onCallCreated)
    offAdminEvent('waiter-call.handled', onCallHandled)
  }

  return { calls, loaded, loadData, callWaiter, handleCall, pendingCalls, subscribeWebSocket, unsubscribeWebSocket }
})
