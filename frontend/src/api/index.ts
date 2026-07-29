import api from './base'

// Categories
export const getCategories = () => api.get('/categories')
export const createCategory = (data: any) => api.post('/categories', data)
export const updateCategory = (id: number, data: any) => api.put(`/categories/${id}`, data)
export const deleteCategory = (id: number) => api.delete(`/categories/${id}`)

// Dishes
export const getDishes = (params?: Record<string, any>) => api.get('/dishes', { params })
export const getDishById = (id: number) => api.get(`/dishes/${id}`)
export const createDish = (data: any) => api.post('/dishes', data)
export const updateDish = (id: number, data: any) => api.put(`/dishes/${id}`, data)
export const deleteDish = (id: number) => api.delete(`/dishes/${id}`)
export const uploadDishImage = (id: number, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(`/dishes/${id}/image`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// Combos
export const getCombos = (params?: Record<string, any>) => api.get('/combos', { params })
export const createCombo = (data: any) => api.post('/combos', data)
export const updateCombo = (id: number, data: any) => api.put(`/combos/${id}`, data)
export const deleteCombo = (id: number) => api.delete(`/combos/${id}`)

// Orders
export const getOrders = (params?: Record<string, any>) => api.get('/orders', { params })
export const getOrderById = (id: number) => api.get(`/orders/${id}`)
export const createOrder = (data: any) => api.post('/orders', data)
export const updateOrderItemStatus = (orderId: number, itemId: number, status: string) =>
  api.put(`/orders/${orderId}/items/${itemId}/status`, { status })
export const completeAllItems = (orderId: number) => api.put(`/orders/${orderId}/complete-all`)
export const getOrderStats = () => api.get('/orders/stats')

// Tables
export const getTables = () => api.get('/tables')
export const getTableById = (id: number) => api.get(`/tables/${id}`)
export const getTableByNumber = (number: string) => api.get(`/tables/number/${number}`)
export const createTable = (data: any) => api.post('/tables', data)
export const updateTable = (id: number, data: any) => api.put(`/tables/${id}`, data)
export const deleteTable = (id: number) => api.delete(`/tables/${id}`)
export const resetTable = (id: number) => api.put(`/tables/${id}/reset`)

// Waiter Calls
export const getWaiterCalls = (params?: Record<string, any>) => api.get('/waiter-calls', { params })
export const createWaiterCall = (tableId: number, tableNumber: string) =>
  api.post('/waiter-calls', { tableId, tableNumber })
export const handleWaiterCall = (id: number) => api.put(`/waiter-calls/${id}/handle`)
