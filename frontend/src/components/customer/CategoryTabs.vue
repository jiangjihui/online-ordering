<template>
  <div class="category-tabs">
    <div
      v-for="cat in categories"
      :key="cat.id"
      class="tab"
      :class="{ active: cat.id === activeId }"
      @click="menuStore.setActiveCategory(cat.id)"
    >
      <span class="tab-icon">{{ cat.icon }}</span>
      <span class="tab-name">{{ menuStore.categoryName(cat) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useMenuStore } from '@/stores/menu'

const menuStore = useMenuStore()
const categories = computed(() => menuStore.categories)
const activeId = computed(() => menuStore.activeCategoryId)
</script>

<style scoped>
.category-tabs {
  display: flex;
  overflow-x: auto;
  background: var(--color-white);
  padding: var(--spacing-sm) 0;
  border-bottom: 1px solid var(--color-border);
  -webkit-overflow-scrolling: touch;
}
.category-tabs::-webkit-scrollbar { display: none; }
.tab {
  flex-shrink: 0;
  padding: var(--spacing-sm) var(--spacing-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: color 0.2s;
}
.tab.active {
  color: var(--color-primary);
  font-weight: bold;
}
.tab-icon { font-size: 20px; }
.tab-name { font-size: var(--font-size-sm); }
</style>
