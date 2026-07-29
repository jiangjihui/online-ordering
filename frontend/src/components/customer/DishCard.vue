<template>
  <div class="dish-card" :class="{ sold_out: dish.sold_out }">
    <img v-if="dish.image" :src="imageUrl(dish.image)" class="dish-image" />
    <div class="dish-info">
      <div class="dish-name">
        {{ menuStore.dishName(dish) }}
        <SpicyBadge :level="dish.is_spicy" />
        <span v-for="label in dish.labels" :key="label" class="label-badge">{{ labelIcons[label] }}</span>
      </div>
      <div class="dish-desc">{{ menuStore.dishDesc(dish) }}</div>
      <div class="dish-price">¥{{ dish.price }}</div>
    </div>
    <div v-if="dish.sold_out" class="sold-out-tag">{{ menuStore.lang === 'zh' ? '已售罄' : 'Sold Out' }}</div>
    <div v-else class="dish-actions">
      <template v-if="quantity > 0">
        <button class="qty-btn minus" @click="cartStore.removeItem(dish.id)">-</button>
        <span class="qty-num">{{ quantity }}</span>
        <button class="qty-btn plus" @click="cartStore.addItem(dish)">+</button>
      </template>
      <button v-else class="add-btn" @click="cartStore.addItem(dish)">+</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Dish, DishLabel } from '@/types'
import { useCartStore } from '@/stores/cart'
import { useMenuStore } from '@/stores/menu'
import { imageUrl } from '@/utils'
import SpicyBadge from './SpicyBadge.vue'

const props = defineProps<{ dish: Dish }>()
const cartStore = useCartStore()
const menuStore = useMenuStore()

const quantity = computed(() =>
  cartStore.items.filter(i => i.dish.id === props.dish.id)
    .reduce((sum, i) => sum + i.quantity, 0)
)

const labelIcons: Record<DishLabel, string> = {
  vegetarian: '🥬', nuts: '🥜', halal: '☪️', gluten_free: '🌾', dairy_free: '🥛',
}
</script>

<style scoped>
.dish-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-white);
  border-bottom: 1px solid var(--color-border);
}
.dish-image { width: 60px; height: 60px; object-fit: cover; border-radius: var(--radius-sm); margin-right: var(--spacing-sm); flex-shrink: 0; }
.dish-card.sold_out { opacity: 0.5; }
.dish-info { flex: 1; }
.dish-name {
  font-size: var(--font-size-lg);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}
.label-badge {
  font-size: 14px;
  line-height: 1;
}
.dish-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-light);
  margin-top: 4px;
}
.dish-price {
  font-size: var(--font-size-lg);
  color: var(--color-primary);
  font-weight: bold;
  margin-top: 4px;
}
.sold-out-tag {
  padding: 4px 12px;
  background: var(--color-border);
  color: var(--color-text-light);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: bold;
}
.dish-actions { display: flex; align-items: center; gap: 8px; }
.add-btn {
  width: 32px; height: 32px; border-radius: 50%; border: none;
  background: var(--color-primary); color: white; font-size: 20px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.qty-btn {
  width: 28px; height: 28px; border-radius: 50%; border: none;
  cursor: pointer; font-size: 16px;
  display: flex; align-items: center; justify-content: center;
}
.qty-btn.minus { background: var(--color-bg); color: var(--color-text); border: 1px solid var(--color-border); }
.qty-btn.plus { background: var(--color-primary); color: white; }
.qty-num { font-size: var(--font-size-lg); font-weight: bold; min-width: 20px; text-align: center; }
</style>
