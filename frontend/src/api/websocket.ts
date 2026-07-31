import { Client } from '@stomp/stompjs'
import type { IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

type EventHandler = (data: any) => void

const stompClient = new Client({
  webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_BASE_URL),
  reconnectDelay: 5000,
  debug: (str) => console.log('[STOMP]', str),
})

const adminHandlers: Map<string, EventHandler[]> = new Map()
const tableHandlers: Map<string, Map<string, EventHandler[]>> = new Map()
let connected = false

function processMessage(handlers: Map<string, EventHandler[]>, msg: IMessage) {
  const body = JSON.parse(msg.body)
  const event = body.event
  const cbs = handlers.get(event)
  if (cbs) cbs.forEach((cb) => cb(body.data))
}

stompClient.onConnect = () => {
  connected = true
  console.log('[STOMP] Connected')

  stompClient.subscribe('/topic/admin', (msg) => processMessage(adminHandlers, msg))

  // Re-subscribe existing table topics
  for (const [tableId, handlersMap] of tableHandlers) {
    stompClient.subscribe(`/topic/table/${tableId}`, (msg) => processMessage(handlersMap, msg))
  }
}

stompClient.onDisconnect = () => {
  connected = false
  console.log('[STOMP] Disconnected')
}

stompClient.onStompError = (frame) => {
  console.error('[STOMP] Error', frame.headers['message'], frame.body)
}

export function connectWebSocket() {
  if (!stompClient.active) {
    stompClient.activate()
  }
}

export function disconnectWebSocket() {
  if (stompClient.active) {
    stompClient.deactivate()
  }
}

export function onAdminEvent(event: string, handler: EventHandler) {
  if (!adminHandlers.has(event)) adminHandlers.set(event, [])
  adminHandlers.get(event)!.push(handler)
}

export function offAdminEvent(event: string, handler: EventHandler) {
  const cbs = adminHandlers.get(event)
  if (cbs) {
    const idx = cbs.indexOf(handler)
    if (idx >= 0) cbs.splice(idx, 1)
  }
}

export function onTableEvent(tableId: number, event: string, handler: EventHandler) {
  const key = String(tableId)
  if (!tableHandlers.has(key)) {
    tableHandlers.set(key, new Map())
    // Subscribe if already connected
    if (connected) {
      stompClient.subscribe(`/topic/table/${key}`, (msg) => processMessage(tableHandlers.get(key)!, msg))
    }
  }
  const handlersMap = tableHandlers.get(key)!
  if (!handlersMap.has(event)) handlersMap.set(event, [])
  handlersMap.get(event)!.push(handler)
}

export function offTableEvent(tableId: number, event: string, handler: EventHandler) {
  const key = String(tableId)
  const handlersMap = tableHandlers.get(key)
  if (handlersMap) {
    const cbs = handlersMap.get(event)
    if (cbs) {
      const idx = cbs.indexOf(handler)
      if (idx >= 0) cbs.splice(idx, 1)
    }
  }
}
