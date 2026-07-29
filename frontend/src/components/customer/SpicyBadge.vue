<template>
  <span v-if="level > 0" class="spicy-badge" :class="`spicy-${level}`">
    {{ labels[level] }}
  </span>
</template>

<script setup lang="ts">
defineProps<{ level: 0 | 1 | 2 | 3 }>()

const labels: Record<number, string> = { 1: '微辣', 2: '中辣', 3: '特辣' }
</script>

<style scoped>
.spicy-badge {
  font-size: var(--font-size-sm);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  color: #fff;
}
.spicy-1 { background: var(--color-spicy-1); }
.spicy-2 { background: var(--color-spicy-2); }
.spicy-3 { background: var(--color-spicy-3); }
</style>
