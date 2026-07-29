export interface Table {
  id: number
  number: string
  area: string
  capacity: number
  status: 'idle' | 'dining' | 'reserved'
}

export interface Category {
  id: number
  name: string
  name_en: string
  sort_order: number
  icon: string
}

export type DishLabel = 'vegetarian' | 'nuts' | 'halal' | 'gluten_free' | 'dairy_free'

export interface Dish {
  id: number
  name: string
  name_en: string
  category_id: number
  price: number
  description: string
  description_en: string
  image: string
  status: 'active' | 'inactive'
  sold_out: boolean
  is_spicy: 0 | 1 | 2 | 3
  labels: DishLabel[]
  sort_order: number
}

export interface Combo {
  id: number
  name: string
  name_en: string
  price: number
  description: string
  items: ComboItem[]
  status: 'active' | 'inactive'
}

export interface ComboItem {
  dish_id: number
  dish_name: string
  quantity: number
}

export interface Order {
  id: number
  table_id: number
  order_type: 'scan' | 'server'
  status: 'pending' | 'preparing' | 'completed' | 'closed'
  total_amount: number
  remark: string
  created_at: string
  started_at?: string
  completed_at?: string
  items: OrderItem[]
}

export interface OrderItem {
  id: number
  order_id: number
  dish_id: number
  dish_name: string
  quantity: number
  remark: string
  status: 'pending' | 'preparing' | 'completed'
  started_at?: string
  completed_at?: string
}

export interface CartItem {
  dish: Dish
  quantity: number
  remark: string
}

export interface CartComboItem {
  combo: Combo
  quantity: number
  remark: string
}

export interface WaiterCall {
  id: number
  table_id: number
  table_number: string
  created_at: string
  status: 'pending' | 'handled'
}

export type Lang = 'zh' | 'en'
