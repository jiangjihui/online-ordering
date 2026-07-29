<template>
  <el-menu :default-active="activeRoute" router class="side-menu">
    <div class="menu-logo">点餐管理</div>
    <el-menu-item index="/admin">
      <el-icon><DataBoard /></el-icon>
      <span>仪表盘</span>
    </el-menu-item>
    <el-menu-item index="/admin/dishes">
      <el-icon><Food /></el-icon>
      <span>菜品管理</span>
    </el-menu-item>
    <el-menu-item index="/admin/categories">
      <el-icon><Grid /></el-icon>
      <span>分类管理</span>
    </el-menu-item>
    <el-menu-item index="/admin/tables">
      <el-icon><House /></el-icon>
      <span>餐桌管理</span>
    </el-menu-item>
    <el-menu-item index="/admin/orders">
      <el-icon><List /></el-icon>
      <span>订单查看</span>
    </el-menu-item>
    <el-menu-item index="/admin/combos">
      <el-icon><Coin /></el-icon>
      <span>套餐管理</span>
    </el-menu-item>
    <div class="menu-footer">
      <div class="menu-user">{{ currentUser }}</div>
      <el-button size="small" @click="logout">退出登录</el-button>
    </div>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Food, Grid, House, List, DataBoard, Coin } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const activeRoute = computed(() => route.path)

const currentUser = computed(() => sessionStorage.getItem('authUser') || '')

function logout() {
  sessionStorage.removeItem('auth')
  sessionStorage.removeItem('authUser')
  router.push('/login')
}
</script>

<style scoped>
.side-menu {
  height: 100vh;
  border-right: 1px solid var(--color-border);
}
.menu-logo {
  padding: var(--spacing-lg);
  font-size: var(--font-size-xl);
  font-weight: bold;
  color: var(--color-primary);
  text-align: center;
  border-bottom: 1px solid var(--color-border);
}
.menu-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--spacing-md);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.menu-user { font-size: var(--font-size-sm); color: var(--color-text-light); }
</style>
